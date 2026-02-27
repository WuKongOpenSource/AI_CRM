package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.*;
import com.kakarote.ai_crm.entity.PO.*;
import com.kakarote.ai_crm.entity.VO.*;
import com.kakarote.ai_crm.mapper.*;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.ICustomerService;
import com.kakarote.ai_crm.utils.UserUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 客户服务实现
 */
@Slf4j
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements ICustomerService {

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private CustomerTagMapper customerTagMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ICustomFieldService customFieldService;

    @Autowired
    private CustomerTeamMapper customerTeamMapper;

    @Autowired
    private ManageUserMapper manageUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addCustomer(CustomerAddBO customerAddBO) {
        // Create customer
        Customer customer = BeanUtil.copyProperties(customerAddBO, Customer.class);
        customer.setOwnerId(UserUtil.getUserId());
        customer.setStatus(1);
        if (StrUtil.isEmpty(customer.getStage())) {
            customer.setStage("lead");
        }
        if (StrUtil.isEmpty(customer.getLevel())) {
            customer.setLevel("B");
        }
        save(customer);

        // Create primary contact if provided
        if (StrUtil.isNotEmpty(customerAddBO.getContactName())) {
            Contact contact = new Contact();
            contact.setCustomerId(customer.getCustomerId());
            contact.setName(customerAddBO.getContactName());
            contact.setPhone(customerAddBO.getContactPhone());
            contact.setEmail(customerAddBO.getContactEmail());
            contact.setPosition(customerAddBO.getContactPosition());
            contact.setIsPrimary(1);
            contact.setStatus(1);
            contactMapper.insert(contact);
            // 同步冗余字段
            syncContactCache(customer.getCustomerId());
        }

        // Save custom fields
        if (customerAddBO.getCustomFields() != null && !customerAddBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("customer", customer.getCustomerId(), customerAddBO.getCustomFields());
        }

        return customer.getCustomerId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(CustomerUpdateBO customerUpdateBO) {
        Customer customer = getById(customerUpdateBO.getCustomerId());
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        BeanUtil.copyProperties(customerUpdateBO, customer, "customerId", "createUserId", "createTime", "customFields");
        updateById(customer);

        // 更新自定义字段
        if (customerUpdateBO.getCustomFields() != null && !customerUpdateBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("customer", customerUpdateBO.getCustomerId(), customerUpdateBO.getCustomFields());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long customerId) {
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        removeById(customerId);
        // Delete related contacts
        contactMapper.delete(new LambdaQueryWrapper<Contact>().eq(Contact::getCustomerId, customerId));
        // Delete related tags
        customerTagMapper.delete(new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getCustomerId, customerId));
    }

    @Override
    public BasePage<CustomerListVO> queryPageList(CustomerQueryBO queryBO) {
        BasePage<CustomerListVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);

        List<CustomerListVO> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return page;
        }

        List<Long> customerIds = records.stream().map(CustomerListVO::getCustomerId).toList();

        // 1. tagNames -> tags 列表
        for (CustomerListVO record : records) {
            if (StrUtil.isNotEmpty(record.getTagNames())) {
                record.setTags(Arrays.asList(record.getTagNames().split(",")));
            } else {
                record.setTags(Collections.emptyList());
            }
        }

        // 2. 批量加载自定义字段（1-2条SQL代替N+1）
        try {
            Map<Long, Map<String, Object>> cfMap = customFieldService.getBatchCustomFieldValues("customer", customerIds);
            for (CustomerListVO record : records) {
                record.setCustomFields(cfMap.getOrDefault(record.getCustomerId(), Collections.emptyMap()));
            }
        } catch (Exception e) {
            log.warn("批量加载自定义字段失败: {}", e.getMessage());
        }

        // 3. 批量加载团队成员姓名
        try {
            List<CustomerTeam> teamList = customerTeamMapper.selectList(
                new LambdaQueryWrapper<CustomerTeam>()
                    .in(CustomerTeam::getCustomerId, customerIds)
                    .eq(CustomerTeam::getRole, "member")
            );
            if (!teamList.isEmpty()) {
                Set<Long> userIds = teamList.stream().map(CustomerTeam::getUserId).collect(Collectors.toSet());
                List<ManagerUser> users = manageUserMapper.selectBatchIds(userIds);
                Map<Long, String> userNameMap = users.stream()
                    .collect(Collectors.toMap(ManagerUser::getUserId, ManagerUser::getRealname, (a, b) -> a));

                Map<Long, List<String>> teamMap = teamList.stream()
                    .collect(Collectors.groupingBy(
                        CustomerTeam::getCustomerId,
                        Collectors.mapping(t -> userNameMap.getOrDefault(t.getUserId(), ""), Collectors.toList())
                    ));

                for (CustomerListVO record : records) {
                    record.setTeamMemberNames(teamMap.getOrDefault(record.getCustomerId(), Collections.emptyList()));
                }
            }
        } catch (Exception e) {
            log.warn("批量加载团队成员失败: {}", e.getMessage());
        }

        return page;
    }

    @Override
    public CustomerDetailVO getCustomerDetail(Long customerId) {
        CustomerListVO customer = baseMapper.getCustomerById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }

        CustomerDetailVO detail = BeanUtil.copyProperties(customer, CustomerDetailVO.class);

        // Get contacts - wrapped in try-catch to prevent failures from breaking the entire API
        try {
            List<Contact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                    .eq(Contact::getCustomerId, customerId)
                    .eq(Contact::getStatus, 1)
            );
            log.info("查询客户[{}]的联系人数量: {}", customerId, contacts.size());
            detail.setContacts(BeanUtil.copyToList(contacts, ContactVO.class));
        } catch (Exception e) {
            log.error("查询客户[{}]的联系人失败: {}", customerId, e.getMessage(), e);
            detail.setContacts(new java.util.ArrayList<>());
        }

        // Get tags - wrapped in try-catch
        try {
            List<CustomerTag> tags = customerTagMapper.selectList(
                new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getCustomerId, customerId)
            );
            log.info("查询客户[{}]的标签数量: {}", customerId, tags.size());
            detail.setTags(tags);
        } catch (Exception e) {
            log.error("查询客户[{}]的标签失败: {}", customerId, e.getMessage(), e);
            detail.setTags(new java.util.ArrayList<>());
        }

        // Get custom fields - wrapped in try-catch to prevent failures from breaking the entire API
        try {
            Map<String, Object> customFields = customFieldService.getCustomFieldValues("customer", customerId);
            log.info("查询客户[{}]的自定义字段: {}", customerId, customFields);
            detail.setCustomFields(customFields);
        } catch (Exception e) {
            log.error("查询客户[{}]的自定义字段失败: {}", customerId, e.getMessage(), e);
            detail.setCustomFields(new java.util.HashMap<>());
        }

        // Get related tasks - wrapped in try-catch
        try {
            List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                    .eq(Task::getCustomerId, customerId)
                    .orderByDesc(Task::getCreateTime)
                    .last("LIMIT 10")
            );
            log.info("查询客户[{}]的相关任务数量: {}", customerId, tasks.size());
            detail.setTasks(tasks);
        } catch (Exception e) {
            log.error("查询客户[{}]的相关任务失败: {}", customerId, e.getMessage(), e);
            detail.setTasks(new java.util.ArrayList<>());
        }

        return detail;
    }

    @Override
    public void updateStage(Long customerId, String stage) {
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        customer.setStage(stage);
        updateById(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTag(Long customerId, String tagName, String color) {
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }

        // Check if tag already exists
        Long count = customerTagMapper.selectCount(
            new LambdaQueryWrapper<CustomerTag>()
                .eq(CustomerTag::getCustomerId, customerId)
                .eq(CustomerTag::getTagName, tagName)
        );
        if (count > 0) {
            return; // Tag already exists
        }

        CustomerTag tag = new CustomerTag();
        tag.setCustomerId(customerId);
        tag.setTagName(tagName);
        tag.setColor(StrUtil.isEmpty(color) ? "#3b82f6" : color);
        customerTagMapper.insert(tag);
        // 同步冗余字段
        syncTagCache(customerId);
    }

    @Override
    public void removeTag(Long customerId, Long tagId) {
        customerTagMapper.deleteById(tagId);
        // 同步冗余字段
        syncTagCache(customerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferCustomer(CustomerTransferBO transferBO) {
        lambdaUpdate()
            .in(Customer::getCustomerId, transferBO.getCustomerIds())
            .set(Customer::getOwnerId, transferBO.getNewOwnerId())
            .update();
    }

    // ==================== 阶段/级别标签映射 ====================

    private static final Map<String, String> STAGE_LABEL_MAP = new LinkedHashMap<>();
    private static final Map<String, String> LABEL_STAGE_MAP = new LinkedHashMap<>();
    private static final Set<String> VALID_LEVELS = Set.of("A", "B", "C");

    static {
        STAGE_LABEL_MAP.put("lead", "线索");
        STAGE_LABEL_MAP.put("qualified", "资格审查");
        STAGE_LABEL_MAP.put("proposal", "方案报价");
        STAGE_LABEL_MAP.put("negotiation", "谈判中");
        STAGE_LABEL_MAP.put("closed", "已成交");
        STAGE_LABEL_MAP.put("lost", "已流失");
        STAGE_LABEL_MAP.forEach((k, v) -> LABEL_STAGE_MAP.put(v, k));
    }

    // ==================== 导出 ====================

    @Override
    public void exportCustomers(CustomerExportBO exportBO, HttpServletResponse response) {
        // 1. 查询客户列表
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getStatus, 1);
        if (exportBO.getCustomerIds() != null && !exportBO.getCustomerIds().isEmpty()) {
            wrapper.in(Customer::getCustomerId, exportBO.getCustomerIds());
        } else {
            if (StrUtil.isNotEmpty(exportBO.getKeyword())) {
                wrapper.like(Customer::getCompanyName, exportBO.getKeyword());
            }
            if (StrUtil.isNotEmpty(exportBO.getStage())) {
                wrapper.eq(Customer::getStage, exportBO.getStage());
            }
            if (StrUtil.isNotEmpty(exportBO.getLevel())) {
                wrapper.eq(Customer::getLevel, exportBO.getLevel());
            }
        }
        wrapper.orderByDesc(Customer::getCreateTime);
        List<Customer> customers = list(wrapper);

        if (customers.isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "没有符合条件的客户数据");
        }

        // 2. 批量查联系人
        List<Long> customerIds = customers.stream().map(Customer::getCustomerId).collect(Collectors.toList());
        List<Contact> allContacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                        .in(Contact::getCustomerId, customerIds)
                        .eq(Contact::getStatus, 1)
                        .orderByDesc(Contact::getIsPrimary)
        );
        Map<Long, List<Contact>> contactMap = allContacts.stream()
                .collect(Collectors.groupingBy(Contact::getCustomerId));

        // 3. 查自定义字段定义
        List<CustomFieldVO> customFields = customFieldService.getEnabledFieldsByEntity("customer");

        // 4. 构建Excel
        ExcelWriter writer = ExcelUtil.getWriter(true);
        // 固定表头
        List<String> headers = new ArrayList<>(Arrays.asList(
                "公司名称", "行业", "商机阶段", "客户级别", "来源", "地址", "网站",
                "报价金额", "合同金额", "收入金额", "备注",
                "联系人姓名", "联系人职位", "联系人电话", "联系人邮箱", "联系人微信"
        ));
        // 追加自定义字段表头
        for (CustomFieldVO cf : customFields) {
            headers.add(cf.getFieldLabel());
        }
        writer.writeHeadRow(headers);

        // 5. 写数据行
        for (Customer c : customers) {
            List<Contact> contacts = contactMap.getOrDefault(c.getCustomerId(), Collections.emptyList());
            Map<String, Object> cfValues = customFieldService.getCustomFieldValues("customer", c.getCustomerId());

            if (contacts.isEmpty()) {
                // 无联系人，联系人列留空
                List<Object> row = buildExportRow(c, null, customFields, cfValues);
                writer.writeRow(row);
            } else {
                for (Contact contact : contacts) {
                    List<Object> row = buildExportRow(c, contact, customFields, cfValues);
                    writer.writeRow(row);
                }
            }
        }

        // 6. 设置列宽
        Sheet exportSheet = writer.getSheet();
        int defaultExportWidth = 18 * 256;
        int wideExportWidth = 24 * 256;
        Set<String> wideColumns = Set.of("公司名称", "地址", "网站", "备注");
        for (int i = 0; i < headers.size(); i++) {
            exportSheet.setColumnWidth(i, wideColumns.contains(headers.get(i)) ? wideExportWidth : defaultExportWidth);
        }

        // 7. 输出
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("客户数据.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
        } catch (Exception e) {
            log.error("导出客户Excel失败", e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "导出失败: " + e.getMessage());
        }
    }

    private List<Object> buildExportRow(Customer c, Contact contact, List<CustomFieldVO> customFields, Map<String, Object> cfValues) {
        List<Object> row = new ArrayList<>();
        row.add(c.getCompanyName());
        row.add(c.getIndustry());
        row.add(STAGE_LABEL_MAP.getOrDefault(c.getStage(), c.getStage()));
        row.add(c.getLevel());
        row.add(c.getSource());
        row.add(c.getAddress());
        row.add(c.getWebsite());
        row.add(c.getQuotation());
        row.add(c.getContractAmount());
        row.add(c.getRevenue());
        row.add(c.getRemark());
        // 联系人
        if (contact != null) {
            row.add(contact.getName());
            row.add(contact.getPosition());
            row.add(contact.getPhone());
            row.add(contact.getEmail());
            row.add(contact.getWechat());
        } else {
            row.add(null);
            row.add(null);
            row.add(null);
            row.add(null);
            row.add(null);
        }
        // 自定义字段
        for (CustomFieldVO cf : customFields) {
            row.add(cfValues != null ? cfValues.get(cf.getFieldName()) : null);
        }
        return row;
    }

    // ==================== 导入模板 ====================

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        List<CustomFieldVO> customFields = customFieldService.getEnabledFieldsByEntity("customer");

        ExcelWriter writer = ExcelUtil.getWriter(true);
        Sheet sheet = writer.getSheet();
        Workbook wb = writer.getWorkbook();

        // -- 构建表头列表 --
        List<String> headers = new ArrayList<>(Arrays.asList(
                "公司名称", "行业", "商机阶段", "客户级别", "来源", "地址", "网站",
                "报价金额", "备注",
                "联系人姓名", "联系人职位", "联系人电话", "联系人邮箱", "联系人微信"
        ));
        for (CustomFieldVO cf : customFields) {
            headers.add(cf.getFieldLabel());
        }
        int totalCols = headers.size();
        Set<String> requiredFields = Set.of("公司名称");

        // -- Row 0: 注意事项（合并单元格） --
        String notes = "注意事项：\n"
                + "1. 表头标\"*\"的红色字体为必填项\n"
                + "2. 商机阶段可选值：线索、资格审查、方案报价、谈判中、已成交、已流失\n"
                + "3. 客户级别可选值：A、B、C\n"
                + "4. 报价金额：数字，支持小数\n"
                + "5. 同一公司多个联系人时，重复填写公司名称，每行一个联系人";

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));
        Row noteRow = sheet.createRow(0);
        noteRow.setHeightInPoints(110);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue(notes);

        // 注意事项样式：自动换行 + 顶部对齐
        CellStyle noteStyle = wb.createCellStyle();
        noteStyle.setWrapText(true);
        noteStyle.setVerticalAlignment(VerticalAlignment.TOP);
        Font noteFont = wb.createFont();
        noteFont.setFontHeightInPoints((short) 11);
        noteStyle.setFont(noteFont);
        noteCell.setCellStyle(noteStyle);

        // -- Row 1: 表头 --
        // 必填字段样式（红色 + 加粗 + 背景色）
        CellStyle requiredStyle = wb.createCellStyle();
        Font redFont = wb.createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);
        redFont.setFontHeightInPoints((short) 11);
        requiredStyle.setFont(redFont);
        requiredStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        requiredStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        requiredStyle.setBorderBottom(BorderStyle.THIN);
        requiredStyle.setBorderTop(BorderStyle.THIN);

        // 普通表头样式（加粗 + 背景色）
        CellStyle normalHeaderStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        normalHeaderStyle.setFont(headerFont);
        normalHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        normalHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        normalHeaderStyle.setBorderBottom(BorderStyle.THIN);
        normalHeaderStyle.setBorderTop(BorderStyle.THIN);

        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            String name = headers.get(i);
            if (requiredFields.contains(name)) {
                cell.setCellValue("*" + name);
                cell.setCellStyle(requiredStyle);
            } else {
                cell.setCellValue(name);
                cell.setCellStyle(normalHeaderStyle);
            }
        }

        // -- 列宽 --
        int defaultWidth = 18 * 256;
        for (int i = 0; i < totalCols; i++) {
            sheet.setColumnWidth(i, defaultWidth);
        }

        // -- 下拉选择验证（数据行从第3行开始，预留200行） --
        DataValidationHelper dvHelper = sheet.getDataValidationHelper();
        int dataStartRow = 2; // 0-based，Row 2 = Excel第3行
        int dataEndRow = 201; // 预留200行数据

        // 商机阶段下拉
        int stageColIdx = headers.indexOf("商机阶段");
        if (stageColIdx >= 0) {
            String[] stageOptions = {"线索", "资格审查", "方案报价", "谈判中", "已成交", "已流失"};
            DataValidationConstraint stageConstraint = dvHelper.createExplicitListConstraint(stageOptions);
            CellRangeAddressList stageRange = new CellRangeAddressList(dataStartRow, dataEndRow, stageColIdx, stageColIdx);
            DataValidation stageValidation = dvHelper.createValidation(stageConstraint, stageRange);
            stageValidation.setShowErrorBox(true);
            stageValidation.createErrorBox("输入错误", "请从下拉列表中选择商机阶段");
            sheet.addValidationData(stageValidation);
        }

        // 客户级别下拉
        int levelColIdx = headers.indexOf("客户级别");
        if (levelColIdx >= 0) {
            String[] levelOptions = {"A", "B", "C"};
            DataValidationConstraint levelConstraint = dvHelper.createExplicitListConstraint(levelOptions);
            CellRangeAddressList levelRange = new CellRangeAddressList(dataStartRow, dataEndRow, levelColIdx, levelColIdx);
            DataValidation levelValidation = dvHelper.createValidation(levelConstraint, levelRange);
            levelValidation.setShowErrorBox(true);
            levelValidation.createErrorBox("输入错误", "请从下拉列表中选择客户级别（A/B/C）");
            sheet.addValidationData(levelValidation);
        }

        // 输出
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("客户导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
        } catch (Exception e) {
            log.error("生成导入模板失败", e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "生成模板失败: " + e.getMessage());
        }
    }

    // ==================== 导入预览 ====================

    @Override
    public CustomerImportPreviewVO importPreview(MultipartFile file) {
        CustomerImportPreviewVO preview = new CustomerImportPreviewVO();
        List<String> globalErrors = new ArrayList<>();
        List<CustomerImportBO> rows = new ArrayList<>();

        // 查自定义字段定义
        List<CustomFieldVO> customFields = customFieldService.getEnabledFieldsByEntity("customer");
        Map<String, CustomFieldVO> cfLabelMap = customFields.stream()
                .collect(Collectors.toMap(CustomFieldVO::getFieldLabel, f -> f, (a, b) -> a));

        try {
            ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
            List<List<Object>> sheetData = reader.read();
            reader.close();

            if (sheetData.size() < 2) {
                globalErrors.add("Excel文件为空或只有表头行");
                preview.setErrors(globalErrors);
                preview.setRows(rows);
                return preview;
            }

            // 动态定位表头行（兼容首行有注意事项的模板，以及 *前缀）
            int headerRowIndex = -1;
            for (int i = 0; i < sheetData.size(); i++) {
                List<Object> row = sheetData.get(i);
                for (Object cell : row) {
                    if (cell != null && cell.toString().contains("*公司名称")) {
                        headerRowIndex = i;
                        break;
                    }
                }
                if (headerRowIndex >= 0) break;
            }

            if (headerRowIndex < 0) {
                globalErrors.add("缺少必需列「公司名称」");
                preview.setErrors(globalErrors);
                preview.setRows(rows);
                return preview;
            }

            // 解析表头，strip "*" 前缀
            List<Object> headerRowData = sheetData.get(headerRowIndex);
            List<String> headers = headerRowData.stream()
                    .map(h -> h == null ? "" : h.toString().trim().replaceFirst("^\\*", ""))
                    .collect(Collectors.toList());

            // 固定列索引
            int colCompanyName = headers.indexOf("公司名称");
            int colIndustry = headers.indexOf("行业");
            int colStage = headers.indexOf("商机阶段");
            int colLevel = headers.indexOf("客户级别");
            int colSource = headers.indexOf("来源");
            int colAddress = headers.indexOf("地址");
            int colWebsite = headers.indexOf("网站");
            int colQuotation = headers.indexOf("报价金额");
            int colRemark = headers.indexOf("备注");
            int colContactName = headers.indexOf("联系人姓名");
            int colContactPosition = headers.indexOf("联系人职位");
            int colContactPhone = headers.indexOf("联系人电话");
            int colContactEmail = headers.indexOf("联系人邮箱");
            int colContactWechat = headers.indexOf("联系人微信");

            if (colCompanyName < 0) {
                globalErrors.add("缺少必需列「公司名称」");
                preview.setErrors(globalErrors);
                preview.setRows(rows);
                return preview;
            }

            // 逐行解析（从表头行的下一行开始）
            for (int i = headerRowIndex + 1; i < sheetData.size(); i++) {
                List<Object> rowData = sheetData.get(i);
                CustomerImportBO bo = new CustomerImportBO();
                bo.setRowNum(i + 1); // Excel行号（1-based）
                List<String> rowErrors = new ArrayList<>();

                bo.setCompanyName(getCellStr(rowData, colCompanyName));
                bo.setIndustry(getCellStr(rowData, colIndustry));
                bo.setSource(getCellStr(rowData, colSource));
                bo.setAddress(getCellStr(rowData, colAddress));
                bo.setWebsite(getCellStr(rowData, colWebsite));
                bo.setRemark(getCellStr(rowData, colRemark));
                bo.setContactName(getCellStr(rowData, colContactName));
                bo.setContactPosition(getCellStr(rowData, colContactPosition));
                bo.setContactPhone(getCellStr(rowData, colContactPhone));
                bo.setContactEmail(getCellStr(rowData, colContactEmail));
                bo.setContactWechat(getCellStr(rowData, colContactWechat));

                // 阶段：中文标签 → 英文code
                String stageStr = getCellStr(rowData, colStage);
                if (StrUtil.isNotEmpty(stageStr)) {
                    String stageCode = LABEL_STAGE_MAP.get(stageStr);
                    if (stageCode == null && STAGE_LABEL_MAP.containsKey(stageStr)) {
                        stageCode = stageStr; // 允许直接写英文code
                    }
                    if (stageCode != null) {
                        bo.setStage(stageCode);
                    } else {
                        rowErrors.add("商机阶段「" + stageStr + "」无效");
                    }
                }

                // 级别
                String levelStr = getCellStr(rowData, colLevel);
                if (StrUtil.isNotEmpty(levelStr)) {
                    String upperLevel = levelStr.toUpperCase();
                    if (VALID_LEVELS.contains(upperLevel)) {
                        bo.setLevel(upperLevel);
                    } else {
                        rowErrors.add("客户级别「" + levelStr + "」无效，可选 A/B/C");
                    }
                }

                // 报价金额
                String quotationStr = getCellStr(rowData, colQuotation);
                if (StrUtil.isNotEmpty(quotationStr)) {
                    try {
                        bo.setQuotation(new BigDecimal(quotationStr.replace(",", "")));
                    } catch (NumberFormatException e) {
                        rowErrors.add("报价金额格式无效");
                    }
                }

                // 自定义字段
                Map<String, Object> cfMap = new HashMap<>();
                for (int col = 0; col < headers.size(); col++) {
                    String header = headers.get(col);
                    CustomFieldVO cfDef = cfLabelMap.get(header);
                    if (cfDef != null) {
                        String val = getCellStr(rowData, col);
                        if (StrUtil.isNotEmpty(val)) {
                            cfMap.put(cfDef.getFieldName(), val);
                        }
                    }
                }
                if (!cfMap.isEmpty()) {
                    bo.setCustomFields(cfMap);
                }

                // 必填验证
                if (StrUtil.isEmpty(bo.getCompanyName())) {
                    rowErrors.add("公司名称不能为空");
                }

                bo.setErrors(rowErrors);
                rows.add(bo);
            }

            // 批量检测重复
            Set<String> companyNames = rows.stream()
                    .map(CustomerImportBO::getCompanyName)
                    .filter(StrUtil::isNotEmpty)
                    .collect(Collectors.toSet());
            if (!companyNames.isEmpty()) {
                List<Customer> existing = list(new LambdaQueryWrapper<Customer>()
                        .in(Customer::getCompanyName, companyNames)
                        .eq(Customer::getStatus, 1));
                Map<String, Long> existingMap = existing.stream()
                        .collect(Collectors.toMap(Customer::getCompanyName, Customer::getCustomerId, (a, b) -> a));

                for (CustomerImportBO bo : rows) {
                    if (StrUtil.isNotEmpty(bo.getCompanyName()) && existingMap.containsKey(bo.getCompanyName())) {
                        bo.setDuplicate(true);
                        bo.setExistingCustomerId(existingMap.get(bo.getCompanyName()));
                    }
                }
            }

        } catch (Exception e) {
            log.error("解析导入Excel失败", e);
            globalErrors.add("解析文件失败: " + e.getMessage());
        }

        // 统计
        int errorRows = (int) rows.stream().filter(r -> r.getErrors() != null && !r.getErrors().isEmpty()).count();
        int duplicateRows = (int) rows.stream().filter(CustomerImportBO::isDuplicate).count();
        preview.setTotalRows(rows.size());
        preview.setErrorRows(errorRows);
        preview.setDuplicateRows(duplicateRows);
        preview.setValidRows(rows.size() - errorRows);
        preview.setRows(rows);
        preview.setErrors(globalErrors);
        return preview;
    }

    private String getCellStr(List<Object> row, int index) {
        if (index < 0 || index >= row.size() || row.get(index) == null) {
            return null;
        }
        String val = row.get(index).toString().trim();
        return val.isEmpty() ? null : val;
    }

    // ==================== 确认导入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerImportResultVO confirmImport(List<CustomerImportBO> rows) {
        CustomerImportResultVO result = new CustomerImportResultVO();
        int imported = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        // 按公司名称分组，合并同公司多联系人
        Map<String, List<CustomerImportBO>> grouped = new LinkedHashMap<>();
        for (CustomerImportBO row : rows) {
            if (row.getErrors() != null && !row.getErrors().isEmpty()) {
                skipped++;
                continue;
            }
            if (StrUtil.isEmpty(row.getCompanyName())) {
                skipped++;
                continue;
            }
            grouped.computeIfAbsent(row.getCompanyName(), k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<CustomerImportBO>> entry : grouped.entrySet()) {
            List<CustomerImportBO> group = entry.getValue();
            CustomerImportBO first = group.get(0);

            try {
                if (first.isDuplicate()) {
                    String handleMode = first.getHandleMode();
                    if ("skip".equals(handleMode)) {
                        skipped += group.size();
                        continue;
                    }
                    if ("overwrite".equals(handleMode)) {
                        // 更新现有客户
                        Customer existing = getById(first.getExistingCustomerId());
                        if (existing != null) {
                            if (StrUtil.isNotEmpty(first.getIndustry())) existing.setIndustry(first.getIndustry());
                            if (StrUtil.isNotEmpty(first.getStage())) existing.setStage(first.getStage());
                            if (StrUtil.isNotEmpty(first.getLevel())) existing.setLevel(first.getLevel());
                            if (StrUtil.isNotEmpty(first.getSource())) existing.setSource(first.getSource());
                            if (StrUtil.isNotEmpty(first.getAddress())) existing.setAddress(first.getAddress());
                            if (StrUtil.isNotEmpty(first.getWebsite())) existing.setWebsite(first.getWebsite());
                            if (first.getQuotation() != null) existing.setQuotation(first.getQuotation());
                            if (StrUtil.isNotEmpty(first.getRemark())) existing.setRemark(first.getRemark());
                            updateById(existing);

                            // 更新自定义字段
                            if (first.getCustomFields() != null && !first.getCustomFields().isEmpty()) {
                                customFieldService.updateCustomFieldValues("customer", existing.getCustomerId(), first.getCustomFields());
                            }

                            // 导入联系人
                            for (CustomerImportBO r : group) {
                                insertContactFromImport(existing.getCustomerId(), r);
                            }
                            updated++;
                        }
                        continue;
                    }
                    // handleMode未设置，按跳过处理
                    skipped += group.size();
                    continue;
                }

                // 新增客户
                Customer customer = new Customer();
                customer.setCompanyName(first.getCompanyName());
                customer.setIndustry(first.getIndustry());
                customer.setStage(StrUtil.isNotEmpty(first.getStage()) ? first.getStage() : "lead");
                customer.setLevel(StrUtil.isNotEmpty(first.getLevel()) ? first.getLevel() : "B");
                customer.setSource(first.getSource());
                customer.setAddress(first.getAddress());
                customer.setWebsite(first.getWebsite());
                customer.setQuotation(first.getQuotation());
                customer.setRemark(first.getRemark());
                customer.setOwnerId(UserUtil.getUserId());
                customer.setStatus(1);
                save(customer);

                // 保存自定义字段
                if (first.getCustomFields() != null && !first.getCustomFields().isEmpty()) {
                    customFieldService.updateCustomFieldValues("customer", customer.getCustomerId(), first.getCustomFields());
                }

                // 导入联系人
                boolean firstContact = true;
                for (CustomerImportBO r : group) {
                    if (StrUtil.isNotEmpty(r.getContactName())) {
                        Contact contact = new Contact();
                        contact.setCustomerId(customer.getCustomerId());
                        contact.setName(r.getContactName());
                        contact.setPosition(r.getContactPosition());
                        contact.setPhone(r.getContactPhone());
                        contact.setEmail(r.getContactEmail());
                        contact.setWechat(r.getContactWechat());
                        contact.setIsPrimary(firstContact ? 1 : 0);
                        contact.setStatus(1);
                        contactMapper.insert(contact);
                        firstContact = false;
                    }
                }
                imported++;
            } catch (Exception e) {
                log.error("导入客户[{}]失败: {}", first.getCompanyName(), e.getMessage());
                errors.add("第" + first.getRowNum() + "行「" + first.getCompanyName() + "」导入失败: " + e.getMessage());
            }
        }

        result.setImported(imported);
        result.setUpdated(updated);
        result.setSkipped(skipped);
        result.setErrors(errors);
        return result;
    }

    private void insertContactFromImport(Long customerId, CustomerImportBO row) {
        if (StrUtil.isEmpty(row.getContactName())) {
            return;
        }
        // 检查是否已存在同名联系人
        Long existCount = contactMapper.selectCount(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getCustomerId, customerId)
                        .eq(Contact::getName, row.getContactName())
                        .eq(Contact::getStatus, 1));
        if (existCount > 0) {
            return;
        }
        Contact contact = new Contact();
        contact.setCustomerId(customerId);
        contact.setName(row.getContactName());
        contact.setPosition(row.getContactPosition());
        contact.setPhone(row.getContactPhone());
        contact.setEmail(row.getContactEmail());
        contact.setWechat(row.getContactWechat());
        contact.setIsPrimary(0);
        contact.setStatus(1);
        contactMapper.insert(contact);
    }

    @Override
    public DashboardStatsVO getStatistics() {
        DashboardStatsVO stats = new DashboardStatsVO();

        // Total customers
        stats.setTotalCustomers(count());

        // Count by stage
        List<DashboardStatsVO.StageCountVO> stageStats = new java.util.ArrayList<>();
        String[] stages = {"lead", "qualified", "proposal", "negotiation", "closed", "lost"};
        String[] stageNames = {"线索", "已验证", "方案阶段", "商务谈判", "已成交", "已流失"};
        for (int i = 0; i < stages.length; i++) {
            DashboardStatsVO.StageCountVO stageCount = new DashboardStatsVO.StageCountVO();
            stageCount.setStage(stages[i]);
            stageCount.setStageName(stageNames[i]);
            stageCount.setCount(lambdaQuery().eq(Customer::getStage, stages[i]).count());
            stageStats.add(stageCount);
        }
        stats.setCustomersByStage(stageStats);

        // Count by level
        List<DashboardStatsVO.LevelCountVO> levelStats = new java.util.ArrayList<>();
        String[] levels = {"A", "B", "C"};
        for (String level : levels) {
            DashboardStatsVO.LevelCountVO levelCount = new DashboardStatsVO.LevelCountVO();
            levelCount.setLevel(level);
            levelCount.setCount(lambdaQuery().eq(Customer::getLevel, level).count());
            levelStats.add(levelCount);
        }
        stats.setCustomersByLevel(levelStats);

        // Active deals (not closed or lost)
        stats.setActiveDeals(lambdaQuery()
            .notIn(Customer::getStage, "closed", "lost")
            .count());

        return stats;
    }

    // ==================== 冗余字段同步 ====================

    /**
     * 同步客户的主联系人信息和联系人数量到客户表
     */
    public void syncContactCache(Long customerId) {
        Contact primary = contactMapper.selectOne(
            new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId)
                .eq(Contact::getIsPrimary, 1)
                .eq(Contact::getStatus, 1)
                .last("LIMIT 1")
        );
        Long count = contactMapper.selectCount(
            new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId)
                .eq(Contact::getStatus, 1)
        );
        lambdaUpdate()
            .eq(Customer::getCustomerId, customerId)
            .set(Customer::getPrimaryContactName, primary != null ? primary.getName() : null)
            .set(Customer::getPrimaryContactPhone, primary != null ? primary.getPhone() : null)
            .set(Customer::getPrimaryContactPosition, primary != null ? primary.getPosition() : null)
            .set(Customer::getContactCount, count.intValue())
            .update();
    }

    /**
     * 同步客户的标签名称到客户表
     */
    public void syncTagCache(Long customerId) {
        List<CustomerTag> tags = customerTagMapper.selectList(
            new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getCustomerId, customerId)
        );
        String tagNames = tags.stream()
            .map(CustomerTag::getTagName)
            .collect(Collectors.joining(","));
        lambdaUpdate()
            .eq(Customer::getCustomerId, customerId)
            .set(Customer::getTagNames, tagNames)
            .update();
    }
}
