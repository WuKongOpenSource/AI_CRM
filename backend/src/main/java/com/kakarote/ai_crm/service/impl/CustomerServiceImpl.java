package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.entity.BO.*;
import com.kakarote.ai_crm.entity.PO.*;
import com.kakarote.ai_crm.entity.VO.*;
import com.kakarote.ai_crm.mapper.*;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.ICustomerService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.ITaskService;
import com.kakarote.ai_crm.utils.UserUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private com.kakarote.ai_crm.service.IDynamicSchemaService dynamicSchemaService;

    @Autowired
    private CustomerTeamMapper customerTeamMapper;

    @Autowired
    private ManageUserMapper manageUserMapper;

    @Autowired
    @Lazy
    private DynamicChatClientProvider chatClientProvider;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private IGlobalSearchIndexService globalSearchIndexService;

    @Autowired
    private ITaskService taskService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CUSTOMER_TABLE_NAME = "crm_customer";
    private static final Set<String> CUSTOMER_LOGO_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");
    private static final Set<String> BLANK_EMPTY_FIELD_TYPES = Set.of("text", "textarea", "select");
    private static final String FIELD_SOURCE_SYSTEM = "system";
    private static final String FIELD_SOURCE_CUSTOM = "custom";
    private static final String FILTER_OPERATOR_IS_EMPTY = "isEmpty";
    private static final String FILTER_OPERATOR_IS_NOT_EMPTY = "isNotEmpty";
    private static final String EMPTY_MODE_NULL_ONLY = "nullOnly";
    private static final String EMPTY_MODE_BLANK = "blank";
    private static final String EMPTY_MODE_JSON_ARRAY = "jsonArray";
    private static final Pattern SAFE_SQL_COLUMN_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final Map<String, CustomerFilterFieldDefinition> CUSTOMER_SYSTEM_FILTER_FIELDS = buildCustomerSystemFilterFields();

    private record CustomerFilterFieldDefinition(
            String fieldName,
            String fieldSource,
            String label,
            String columnName,
            String fieldType
    ) {
    }

    private static Map<String, CustomerFilterFieldDefinition> buildCustomerSystemFilterFields() {
        Map<String, CustomerFilterFieldDefinition> fields = new LinkedHashMap<>();
        addCustomerSystemFilterField(fields, "companyName", "公司名称", "company_name", "text");
        addCustomerSystemFilterField(fields, "industry", "所属行业", "industry", "text");
        addCustomerSystemFilterField(fields, "stage", "商机阶段", "stage", "select");
        addCustomerSystemFilterField(fields, "level", "客户级别", "level", "select");
        addCustomerSystemFilterField(fields, "source", "来源", "source", "text");
        addCustomerSystemFilterField(fields, "address", "地址", "address", "textarea");
        addCustomerSystemFilterField(fields, "website", "网站", "website", "text");
        addCustomerSystemFilterField(fields, "logo", "公司 Logo", "logo", "text");
        addCustomerSystemFilterField(fields, "quotation", "预计成交金额", "quotation", "number");
        addCustomerSystemFilterField(fields, "lastContactTime", "最后跟进", "last_contact_time", "datetime");
        addCustomerSystemFilterField(fields, "nextFollowTime", "下次跟进时间", "next_follow_time", "datetime");
        addCustomerSystemFilterField(fields, "remark", "备注", "remark", "textarea");
        addCustomerSystemFilterField(fields, "ownerId", "负责人", "owner_id", "number");
        addCustomerSystemFilterField(fields, "primaryContactName", "主联系人姓名", "primary_contact_name", "text");
        addCustomerSystemFilterField(fields, "primaryContactPhone", "主联系人电话", "primary_contact_phone", "text");
        addCustomerSystemFilterField(fields, "primaryContactPosition", "主联系人职位", "primary_contact_position", "text");
        addCustomerSystemFilterField(fields, "contactCount", "联系人数量", "contact_count", "number");
        addCustomerSystemFilterField(fields, "tagNames", "标签", "tag_names", "text");
        addCustomerSystemFilterField(fields, "createTime", "创建时间", "create_time", "datetime");
        addCustomerSystemFilterField(fields, "updateTime", "更新时间", "update_time", "datetime");
        return Collections.unmodifiableMap(fields);
    }

    private static void addCustomerSystemFilterField(Map<String, CustomerFilterFieldDefinition> fields,
                                                     String fieldName,
                                                     String label,
                                                     String columnName,
                                                     String fieldType) {
        fields.put(fieldName, new CustomerFilterFieldDefinition(
                fieldName,
                FIELD_SOURCE_SYSTEM,
                label,
                columnName,
                fieldType
        ));
    }

    private static final String AI_CUSTOMER_PARSE_PROMPT = """
        你是一个专业的 CRM 助手。请从以下输入（文字描述、名片信息、邮件内容等）中提取客户信息，并进行智能分析。

        如果提供了图片（如名片），请结合图片内容和文字进行分析。

        输入内容:
        %s

        请严格按以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
        {
          "companyName": "公司名称",
          "industry": "行业（如：互联网、金融、制造业等）",
          "level": "客户级别，只能是 A、B 或 C（根据公司规模和潜力判断，大公司或明确需求为A，中等为B，其他为C）",
          "stage": "商机阶段，只能是以下之一: lead, qualified, proposal, negotiation, closed（默认 lead）",
          "source": "客户来源（如：线上广告、朋友介绍、展会等，若无法判断则留空）",
          "remark": "备注信息（补充描述）",
          "contactName": "联系人姓名",
          "contactPhone": "联系电话",
          "contactEmail": "电子邮箱",
          "contactPosition": "联系人职位",
          "score": 潜力评分(0-100的整数，根据公司规模、需求匹配度、行业前景综合评估),
          "tags": ["标签1", "标签2", "标签3"],
          "summary": "客户分析摘要（1-2句话，分析该客户的价值和特点）",
          "nextStep": "建议的下一步行动（具体可执行的建议）",
          "keyPoints": ["关键要点1", "关键要点2"]
        }

        注意：
        1. 无法从输入中提取的字段请留空字符串或空数组
        2. tags 请生成3-5个关键词标签
        3. score 请根据信息丰富度和客户质量给出合理评分
        """;

    private static final DateTimeFormatter AI_SEARCH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern DAYS_WITHOUT_CONTACT_PATTERN = Pattern.compile("(\\d{1,3})\\s*天(?:未跟进|未联系|未互动)");
    private static final Pattern RECENT_NEW_CUSTOMERS_PATTERN = Pattern.compile("(?:最近|近)(\\d{1,3})\\s*天新增");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("([ABC])\\s*(?:类|级)?客户", Pattern.CASE_INSENSITIVE);
    private static final List<String> COMMON_INDUSTRIES = List.of(
        "制造业", "互联网", "金融", "教育", "医疗", "零售", "物流", "房地产", "SaaS", "政府", "能源"
    );
    private static final List<String> ACTIVE_STAGE_CODES = List.of("qualified", "proposal", "negotiation");
    private static final String AI_CUSTOMER_SEARCH_PARSE_PROMPT = """
        你是 CRM 客户列表的 AI 搜索解析器。你的任务是把自然语言搜索语句，转换为结构化的客户筛选条件。
        当前时间：%s
        用户输入：%s

        只允许输出以下字段，不要输出未定义字段：
        {
          "parsedQuery": {
            "keyword": "无法映射到结构化字段时使用的关键词",
            "stage": "lead/qualified/proposal/negotiation/closed/lost 之一",
            "stages": ["lead/qualified/proposal/negotiation/closed/lost"],
            "level": "A/B/C",
            "industry": "行业名称",
            "tag": "标签名称",
            "source": "客户来源",
            "quotationMin": 0,
            "quotationMax": 0,
            "lastContactStart": "yyyy-MM-dd HH:mm:ss",
            "lastContactEnd": "yyyy-MM-dd HH:mm:ss",
            "includeNoLastContact": true,
            "nextFollowStart": "yyyy-MM-dd HH:mm:ss",
            "nextFollowEnd": "yyyy-MM-dd HH:mm:ss",
            "createTimeStart": "yyyy-MM-dd HH:mm:ss",
            "createTimeEnd": "yyyy-MM-dd HH:mm:ss",
            "contactCountMin": 0,
            "contactCountMax": 0,
            "filters": [
              {"fieldName": "lastContactTime", "fieldSource": "system/custom", "operator": "isEmpty/isNotEmpty"}
            ],
            "sortBy": "createTime/quotation/lastContactTime/nextFollowTime/contactCount",
            "sortOrder": "asc/desc"
          },
          "explanation": "一句话说明解析依据",
          "confidence": 0.0
        }

        解析规则：
        1. 能结构化就结构化，不能结构化的词放到 keyword。
        2. 所有时间必须输出绝对时间，不允许输出“最近30天”“上周”这种相对表达。
        3. “高价值客户”默认表示 quotationMin = 500000，建议 sortBy=quotation, sortOrder=desc。
        4. “活跃客户”默认表示 stages=["qualified","proposal","negotiation"]。
        5. “30天未跟进”这类表达，转成 lastContactEnd，并将 includeNoLastContact 设为 true。
        6. “最近一周新增客户”转成 createTimeStart，并建议按 createTime 倒序。
        7. 阶段若用户说的是中文，请映射成英文 code。
        8. level 只能是 A/B/C；sortOrder 只能是 asc/desc。
        9. 未提及的字段留空，不要猜测。

        只返回 JSON，不要返回解释性文字，不要使用 Markdown 代码块。
        """;

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
        customer.setUpdateTime(new Date());
        Long updateUserId = getCurrentUserIdOrNull();
        if (updateUserId != null) {
            customer.setUpdateUserId(updateUserId);
        }
        updateById(customer);

        // 更新自定义字段
        if (customerUpdateBO.getCustomFields() != null && !customerUpdateBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("customer", customerUpdateBO.getCustomerId(), customerUpdateBO.getCustomFields());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerDetailVO updateCustomerField(CustomerFieldUpdateBO fieldUpdateBO) {
        Customer customer = getById(fieldUpdateBO.getCustomerId());
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Customer does not exist");
        }

        String fieldName = StrUtil.trim(fieldUpdateBO.getFieldName());
        String fieldSource = normalizeFieldSource(fieldUpdateBO.getFieldSource());
        Object value = fieldUpdateBO.getValue();
        if (FIELD_SOURCE_CUSTOM.equals(fieldSource) || !CUSTOMER_SYSTEM_FILTER_FIELDS.containsKey(fieldName)) {
            customFieldService.updateCustomFieldValue("customer", fieldUpdateBO.getCustomerId(), fieldName, value);
            return getCustomerDetail(fieldUpdateBO.getCustomerId());
        }

        customFieldService.validateUniqueFieldValue("customer", fieldUpdateBO.getCustomerId(), fieldName, value);
        switch (fieldName) {
            case "companyName" -> customer.setCompanyName(toStr(value));
            case "industry" -> customer.setIndustry(toStr(value));
            case "stage" -> customer.setStage(toStr(value));
            case "level" -> customer.setLevel(toStr(value));
            case "source" -> customer.setSource(toStr(value));
            case "address" -> customer.setAddress(toStr(value));
            case "website" -> customer.setWebsite(toStr(value));
            case "logo" -> customer.setLogo(toStr(value));
            case "quotation" -> customer.setQuotation(toBigDecimal(value));
            case "lastContactTime" -> customer.setLastContactTime(toDateValue(value));
            case "nextFollowTime" -> customer.setNextFollowTime(toDateValue(value));
            case "remark" -> customer.setRemark(toStr(value));
            case "ownerId" -> customer.setOwnerId(toLong(value));
            default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Unsupported customer field: " + fieldName);
        }
        customer.setUpdateTime(new Date());
        Long updateUserId = getCurrentUserIdOrNull();
        if (updateUserId != null) {
            customer.setUpdateUserId(updateUserId);
        }
        updateById(customer);
        refreshCustomerActivity(fieldUpdateBO.getCustomerId());
        return getCustomerDetail(fieldUpdateBO.getCustomerId());
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
        resolveCustomerFieldFilters(queryBO);
        // 1. 获取启用的自定义字段，构建动态列列表
        List<CustomFieldVO> enabledFields = customFieldService.getEnabledFieldsByEntity("customer");
        List<String> cfColumns = enabledFields.stream()
                .map(CustomFieldVO::getColumnName)
                .filter(col -> dynamicSchemaService.columnExists("crm_customer", col))
                .toList();
        Map<String, String> colToFieldName = enabledFields.stream()
                .filter(f -> cfColumns.contains(f.getColumnName()))
                .collect(Collectors.toMap(CustomFieldVO::getColumnName, CustomFieldVO::getFieldName));

        // 2. 单次查询：标准字段 + 自定义字段列一起查出
        BasePage<Map<String, Object>> rawPage = queryBO.parse();
        baseMapper.queryPageListWithCf(rawPage, queryBO, cfColumns);

        List<Map<String, Object>> rawRecords = rawPage.getRecords();
        if (rawRecords == null || rawRecords.isEmpty()) {
            BasePage<CustomerListVO> emptyPage = new BasePage<>(rawPage.getCurrent(), rawPage.getSize());
            emptyPage.setTotal(rawPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 3. Map → CustomerListVO 转换，同时提取自定义字段
        List<CustomerListVO> records = rawRecords.stream()
                .map(row -> {
                    CustomerListVO vo = mapToCustomerListVO(row);
                    Map<String, Object> customFields = new HashMap<>();
                    for (String col : cfColumns) {
                        Object val = row.get(col);
                        if (val != null) {
                            customFields.put(colToFieldName.get(col), val);
                        }
                    }
                    vo.setCustomFields(customFields);
                    fillCustomerLogoUrl(vo);
                    return vo;
                })
                .collect(Collectors.toList());

        List<Long> customerIds = records.stream().map(CustomerListVO::getCustomerId).toList();

        // 4. tagNames -> tags 列表
        for (CustomerListVO record : records) {
            if (StrUtil.isNotEmpty(record.getTagNames())) {
                record.setTags(Arrays.asList(record.getTagNames().split(",")));
            } else {
                record.setTags(Collections.emptyList());
            }
        }

        // 5. 联系人 fallback
        try {
            List<Contact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                    .in(Contact::getCustomerId, customerIds)
                    .eq(Contact::getStatus, 1)
                    .orderByDesc(Contact::getIsPrimary)
                    .orderByAsc(Contact::getCreateTime)
                    .orderByAsc(Contact::getContactId)
            );
            Map<Long, List<Contact>> contactMap = contacts.stream()
                .collect(Collectors.groupingBy(Contact::getCustomerId));

            for (CustomerListVO record : records) {
                List<Contact> customerContacts = contactMap.getOrDefault(record.getCustomerId(), Collections.emptyList());
                if (customerContacts.isEmpty()) {
                    continue;
                }

                Contact fallbackContact = customerContacts.get(0);
                if (StrUtil.isBlank(record.getPrimaryContactName())) {
                    record.setPrimaryContactName(fallbackContact.getName());
                }
                if (StrUtil.isBlank(record.getPrimaryContactPhone())) {
                    record.setPrimaryContactPhone(fallbackContact.getPhone());
                }
                if (StrUtil.isBlank(record.getPrimaryContactPosition())) {
                    record.setPrimaryContactPosition(fallbackContact.getPosition());
                }
                if (record.getContactCount() == null || record.getContactCount() <= 0) {
                    record.setContactCount(customerContacts.size());
                }
            }
        } catch (Exception e) {
            log.warn("批量加载联系人兜底信息失败: {}", e.getMessage());
        }

        // 6. 批量加载团队成员姓名
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

        // 7. 组装分页结果
        BasePage<CustomerListVO> resultPage = new BasePage<>(rawPage.getCurrent(), rawPage.getSize());
        resultPage.setTotal(rawPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    /**
     * 解析客户字段筛选条件。
     */
    private void resolveCustomerFieldFilters(CustomerQueryBO queryBO) {
        if (queryBO == null || queryBO.getFilters() == null || queryBO.getFilters().isEmpty()) {
            if (queryBO != null) {
                queryBO.setResolvedFieldFilters(null);
            }
            return;
        }

        List<CustomerResolvedFieldFilterBO> resolvedFilters = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CustomerFieldFilterBO filter : queryBO.getFilters()) {
            CustomerResolvedFieldFilterBO resolvedFilter = resolveCustomerFieldFilter(filter);
            if (resolvedFilter == null) {
                continue;
            }
            String dedupeKey = resolvedFilter.getColumnName() + ":" + resolvedFilter.getOperator();
            if (seen.add(dedupeKey)) {
                resolvedFilters.add(resolvedFilter);
            }
        }
        queryBO.setResolvedFieldFilters(resolvedFilters.isEmpty() ? null : resolvedFilters);
    }

    private CustomerResolvedFieldFilterBO resolveCustomerFieldFilter(CustomerFieldFilterBO filter) {
        if (filter == null) {
            return null;
        }

        String fieldSource = normalizeFieldSource(filter.getFieldSource());
        String fieldName = normalizeCustomerFilterFieldName(filter.getFieldName(), fieldSource);
        String operator = normalizeEmptyFilterOperator(filter.getOperator());
        if (StrUtil.isBlank(fieldName) || StrUtil.isBlank(operator)) {
            return null;
        }

        String columnName;
        String fieldType;
        if (FIELD_SOURCE_CUSTOM.equals(fieldSource)) {
            CustomFieldVO customField = getCustomerCustomFilterFieldMap().get(fieldName);
            if (customField == null) {
                return null;
            }
            columnName = StrUtil.trim(customField.getColumnName());
            fieldType = customField.getFieldType();
        } else {
            CustomerFilterFieldDefinition definition = CUSTOMER_SYSTEM_FILTER_FIELDS.get(fieldName);
            if (definition == null) {
                return null;
            }
            columnName = definition.columnName();
            fieldType = definition.fieldType();
        }

        if (!isSafeCustomerColumn(columnName) || !dynamicSchemaService.columnExists(CUSTOMER_TABLE_NAME, columnName)) {
            return null;
        }
        return new CustomerResolvedFieldFilterBO(columnName, operator, resolveFilterEmptyMode(fieldType));
    }

    private boolean isSafeCustomerColumn(String columnName) {
        return StrUtil.isNotBlank(columnName) && SAFE_SQL_COLUMN_PATTERN.matcher(columnName).matches();
    }

    private Map<String, CustomFieldVO> getCustomerCustomFilterFieldMap() {
        Map<String, CustomFieldVO> fieldMap = new LinkedHashMap<>();
        List<CustomFieldVO> customFields = customFieldService.getEnabledFieldsByEntity("customer");
        for (CustomFieldVO field : customFields) {
            if (field == null || !isSafeCustomerColumn(field.getColumnName())) {
                continue;
            }
            if (!dynamicSchemaService.columnExists(CUSTOMER_TABLE_NAME, field.getColumnName())) {
                continue;
            }
            putCustomFilterAlias(fieldMap, field.getFieldName(), field);
            putCustomFilterAlias(fieldMap, field.getFieldLabel(), field);
            putCustomFilterAlias(fieldMap, field.getColumnName(), field);
        }
        return fieldMap;
    }

    private void putCustomFilterAlias(Map<String, CustomFieldVO> fieldMap, String alias, CustomFieldVO field) {
        String normalizedAlias = normalizeOptionalText(alias);
        if (StrUtil.isNotBlank(normalizedAlias)) {
            fieldMap.putIfAbsent(normalizedAlias, field);
        }
    }

    private String normalizeFieldSource(String fieldSource) {
        String normalized = normalizeOptionalText(fieldSource);
        if (FIELD_SOURCE_CUSTOM.equalsIgnoreCase(normalized)) {
            return FIELD_SOURCE_CUSTOM;
        }
        return FIELD_SOURCE_SYSTEM;
    }

    private String normalizeEmptyFilterOperator(String operator) {
        String normalized = normalizeOptionalText(operator);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        if (FILTER_OPERATOR_IS_EMPTY.equals(normalized)
                || "empty".equalsIgnoreCase(normalized)
                || "blank".equalsIgnoreCase(normalized)
                || "null".equalsIgnoreCase(normalized)
                || "为空".equals(normalized)
                || "未填写".equals(normalized)
                || "未设置".equals(normalized)
                || "无".equals(normalized)) {
            return FILTER_OPERATOR_IS_EMPTY;
        }
        if (FILTER_OPERATOR_IS_NOT_EMPTY.equals(normalized)
                || "notEmpty".equalsIgnoreCase(normalized)
                || "not_blank".equalsIgnoreCase(normalized)
                || "notNull".equalsIgnoreCase(normalized)
                || "不为空".equals(normalized)
                || "非空".equals(normalized)
                || "已填写".equals(normalized)
                || "已设置".equals(normalized)
                || "有".equals(normalized)) {
            return FILTER_OPERATOR_IS_NOT_EMPTY;
        }
        return null;
    }

    private String resolveFilterEmptyMode(String fieldType) {
        String normalizedType = StrUtil.blankToDefault(fieldType, "").trim().toLowerCase(Locale.ROOT);
        if ("multiselect".equals(normalizedType)) {
            return EMPTY_MODE_JSON_ARRAY;
        }
        if (BLANK_EMPTY_FIELD_TYPES.contains(normalizedType)) {
            return EMPTY_MODE_BLANK;
        }
        return EMPTY_MODE_NULL_ONLY;
    }

    /**
     * 将 Map 行数据映射为 CustomerListVO（标准字段）。
     */
    private CustomerListVO mapToCustomerListVO(Map<String, Object> row) {
        CustomerListVO vo = new CustomerListVO();
        vo.setCustomerId(toLong(row.get("customer_id")));
        vo.setCompanyName(toStr(row.get("company_name")));
        vo.setIndustry(toStr(row.get("industry")));
        vo.setStage(toStr(row.get("stage")));
        vo.setLevel(toStr(row.get("level")));
        vo.setSource(toStr(row.get("source")));
        vo.setLogo(toStr(row.get("logo")));
        vo.setQuotation(toBigDecimal(row.get("quotation")));
        vo.setLastContactTime(toDate(row.get("last_contact_time")));
        vo.setNextFollowTime(toDate(row.get("next_follow_time")));
        vo.setOwnerId(toLong(row.get("owner_id")));
        vo.setOwnerName(toStr(row.get("owner_name")));
        vo.setCreateTime(toDate(row.get("create_time")));
        vo.setPrimaryContactName(toStr(row.get("primary_contact_name")));
        vo.setPrimaryContactPhone(toStr(row.get("primary_contact_phone")));
        vo.setPrimaryContactPosition(toStr(row.get("primary_contact_position")));
        vo.setContactCount(toInt(row.get("contact_count")));
        vo.setTagNames(toStr(row.get("tag_names")));
        return vo;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.valueOf(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String toStr(Object val) {
        return val != null ? val.toString() : null;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Date toDate(Object val) {
        if (val == null) return null;
        if (val instanceof Date d) return d;
        if (val instanceof java.sql.Timestamp ts) return new Date(ts.getTime());
        return null;
    }

    private Date toDateValue(Object val) {
        Date date = toDate(val);
        if (date != null || val == null) {
            return date;
        }
        String text = StrUtil.trimToNull(val.toString());
        return text == null ? null : DateUtil.parse(text);
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.valueOf(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private void fillCustomerLogoUrl(CustomerListVO vo) {
        if (vo == null) {
            return;
        }
        vo.setLogoUrl(resolveFileUrl(vo.getLogo()));
    }

    private String resolveFileUrl(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        if (isExternalUrl(filePath)) {
            return filePath.trim();
        }
        try {
            return fileStorageService.getUrl(filePath);
        } catch (Exception e) {
            log.warn("Resolve customer logo URL failed: {}", e.getMessage());
            return null;
        }
    }

    private boolean isExternalUrl(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.startWithIgnoreCase(normalized, "http://")
            || StrUtil.startWithIgnoreCase(normalized, "https://");
    }

    @Override
    public List<Customer> findCustomersByExactCompanyName(String companyName) {
        String normalizedCompanyName = StrUtil.trim(companyName);
        if (StrUtil.isBlank(normalizedCompanyName)) {
            return Collections.emptyList();
        }
        return lambdaQuery()
            .eq(Customer::getCompanyName, normalizedCompanyName)
            .eq(Customer::getStatus, 1)
            .orderByDesc(Customer::getCreateTime)
            .list();
    }

    @Override
    public List<Customer> findCustomersByExactCompanyNameIgnoreDataPermission(String companyName) {
        String normalizedCompanyName = StrUtil.trim(companyName);
        if (StrUtil.isBlank(normalizedCompanyName)) {
            return Collections.emptyList();
        }
        return baseMapper.selectByExactCompanyNameIgnoreDataPermission(normalizedCompanyName);
    }

    @Override
    public List<Customer> findCustomersByCompanyNameLikeIgnoreDataPermission(String keyword, int limit) {
        String normalizedKeyword = StrUtil.trim(keyword);
        if (StrUtil.isBlank(normalizedKeyword) || normalizedKeyword.length() < 2) {
            return Collections.emptyList();
        }
        int actualLimit = Math.max(1, Math.min(limit, 50));
        return baseMapper.selectByCompanyNameLikeIgnoreDataPermission(normalizedKeyword, actualLimit);
    }

    @Override
    public Customer findCustomerByIdIgnoreDataPermission(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return baseMapper.selectByIdIgnoreDataPermission(customerId);
    }

    @Override
    public CustomerDetailVO getCustomerDetail(Long customerId) {
        CustomerDetailVO detail = baseMapper.getCustomerById(customerId);
        if (ObjectUtil.isNull(detail)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        detail.setLogoUrl(resolveFileUrl(detail.getLogo()));

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
    private static final Map<String, String> SEARCH_EXPLANATION_FIELD_LABEL_MAP = new LinkedHashMap<>();

    static {
        STAGE_LABEL_MAP.put("lead", "线索");
        STAGE_LABEL_MAP.put("qualified", "资格审查");
        STAGE_LABEL_MAP.put("proposal", "方案报价");
        STAGE_LABEL_MAP.put("negotiation", "谈判中");
        STAGE_LABEL_MAP.put("closed", "已成交");
        STAGE_LABEL_MAP.put("lost", "已流失");
        STAGE_LABEL_MAP.forEach((k, v) -> LABEL_STAGE_MAP.put(v, k));
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("includeNoLastContact", "包含未跟进客户");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("lastContactStart", "最后跟进时间");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("lastContactEnd", "最后跟进时间");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("nextFollowStart", "下次跟进时间");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("nextFollowEnd", "下次跟进时间");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("createTimeStart", "创建时间");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("createTimeEnd", "创建时间");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("contactCountMin", "联系人数量");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("contactCountMax", "联系人数量");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("filters", "字段筛选");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("fieldName", "字段");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("fieldSource", "字段来源");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("operator", "条件");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("isEmpty", "为空");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("isNotEmpty", "不为空");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("lastContactTime", "最后跟进时间");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("nextFollowTime", "下次跟进时间");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("parsedQuery", "筛选条件");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("sortOrder", "排序方向");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("sortBy", "排序规则");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("quotationMin", "预计成交金额");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("quotationMax", "预计成交金额");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("quotation", "预计成交金额");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("createTime", "创建时间");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("contactCount", "联系人数量");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("industry", "行业");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("keyword", "关键词");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("stages", "阶段");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("stage", "阶段");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("level", "客户级别");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("source", "来源");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("tag", "标签");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("asc", "升序");
        SEARCH_EXPLANATION_FIELD_LABEL_MAP.put("desc", "降序");
    }

    // ==================== 导出 ====================

    @Override
    public void exportCustomers(CustomerExportBO exportBO, HttpServletResponse response) {
        // 1. 查询客户列表
        LambdaQueryWrapper<Customer> wrapper = buildExportCustomerWrapper(exportBO);
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
                "预计成交金额", "备注",
                "联系人姓名", "联系人职位", "联系人电话", "联系人邮箱", "联系人微信"
        ));
        // 追加自定义字段表头
        for (CustomFieldVO cf : customFields) {
            headers.add(cf.getFieldLabel());
        }
        writer.writeHeadRow(headers);

        // 列索引常量
        final int CONTACT_COL_START = headers.indexOf("联系人姓名");
        final int CONTACT_COL_END = headers.indexOf("联系人微信");
        final int CUSTOMER_COL_END = CONTACT_COL_START - 1;
        final int CUSTOM_FIELD_COL_START = CONTACT_COL_END + 1;
        final int CUSTOM_FIELD_COL_END = headers.size() - 1;

        // 5. 写数据行，记录多联系人客户的行范围
        List<int[]> mergeRanges = new ArrayList<>(); // [startRow, rowCount]
        int dataRowIndex = 1; // row 0 = header

        for (Customer c : customers) {
            List<Contact> contacts = contactMap.getOrDefault(c.getCustomerId(), Collections.emptyList());
            Map<String, Object> cfValues = customFieldService.getCustomFieldValues("customer", c.getCustomerId());
            int startRow = dataRowIndex;

            if (contacts.isEmpty()) {
                writer.writeRow(buildExportRow(c, null, customFields, cfValues));
                dataRowIndex++;
            } else {
                for (Contact contact : contacts) {
                    writer.writeRow(buildExportRow(c, contact, customFields, cfValues));
                    dataRowIndex++;
                }
            }

            int rowsWritten = dataRowIndex - startRow;
            if (rowsWritten > 1) {
                mergeRanges.add(new int[]{startRow, rowsWritten});
            }
        }

        // 5b. 合并多联系人客户的客户列单元格
        Sheet exportSheet = writer.getSheet();
        Workbook wb = writer.getWorkbook();
        if (!mergeRanges.isEmpty()) {
            for (int[] range : mergeRanges) {
                int firstRow = range[0];
                int lastRow = firstRow + range[1] - 1;

                // 合并客户列
                for (int col = 0; col <= CUSTOMER_COL_END; col++) {
                    exportSheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, col, col));
                    applyCenteredStyle(exportSheet, wb, firstRow, col);
                }
                // 合并自定义字段列，联系人列保持逐行展示
                for (int col = CUSTOM_FIELD_COL_START; col <= CUSTOM_FIELD_COL_END; col++) {
                    exportSheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, col, col));
                    applyCenteredStyle(exportSheet, wb, firstRow, col);
                }
            }
        }

        // 6. 设置列宽
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

    private LambdaQueryWrapper<Customer> buildExportCustomerWrapper(CustomerExportBO exportBO) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getStatus, 1);

        if (exportBO.getCustomerIds() != null && !exportBO.getCustomerIds().isEmpty()) {
            wrapper.in(Customer::getCustomerId, exportBO.getCustomerIds());
            wrapper.orderByDesc(Customer::getCreateTime);
            return wrapper;
        }

        if (StrUtil.isNotEmpty(exportBO.getKeyword())) {
            wrapper.and(w -> w.like(Customer::getCompanyName, exportBO.getKeyword())
                .or()
                .like(Customer::getPrimaryContactName, exportBO.getKeyword())
                .or()
                .like(Customer::getPrimaryContactPhone, exportBO.getKeyword())
                .or()
                .like(Customer::getTagNames, exportBO.getKeyword()));
        }
        if (StrUtil.isNotEmpty(exportBO.getStage())) {
            wrapper.eq(Customer::getStage, exportBO.getStage());
        }
        if (exportBO.getStages() != null && !exportBO.getStages().isEmpty()) {
            wrapper.in(Customer::getStage, exportBO.getStages());
        }
        if (StrUtil.isNotEmpty(exportBO.getLevel())) {
            wrapper.eq(Customer::getLevel, exportBO.getLevel());
        }
        if (exportBO.getOwnerId() != null) {
            wrapper.eq(Customer::getOwnerId, exportBO.getOwnerId());
        }
        if (StrUtil.isNotEmpty(exportBO.getIndustry())) {
            wrapper.eq(Customer::getIndustry, exportBO.getIndustry());
        }
        if (StrUtil.isNotEmpty(exportBO.getSource())) {
            wrapper.eq(Customer::getSource, exportBO.getSource());
        }
        if (StrUtil.isNotEmpty(exportBO.getTag())) {
            List<Long> tagCustomerIds = customerTagMapper.selectList(
                    new LambdaQueryWrapper<CustomerTag>()
                        .eq(CustomerTag::getTagName, exportBO.getTag()))
                .stream()
                .map(CustomerTag::getCustomerId)
                .distinct()
                .toList();
            if (tagCustomerIds.isEmpty()) {
                wrapper.eq(Customer::getCustomerId, -1L);
            } else {
                wrapper.in(Customer::getCustomerId, tagCustomerIds);
            }
        }
        if (exportBO.getQuotationMin() != null) {
            wrapper.ge(Customer::getQuotation, exportBO.getQuotationMin());
        }
        if (exportBO.getQuotationMax() != null) {
            wrapper.le(Customer::getQuotation, exportBO.getQuotationMax());
        }
        if (exportBO.getLastContactStart() != null) {
            wrapper.ge(Customer::getLastContactTime, exportBO.getLastContactStart());
        }
        if (exportBO.getLastContactEnd() != null) {
            if (Boolean.TRUE.equals(exportBO.getIncludeNoLastContact())) {
                wrapper.and(w -> w.le(Customer::getLastContactTime, exportBO.getLastContactEnd())
                    .or()
                    .isNull(Customer::getLastContactTime));
            } else {
                wrapper.le(Customer::getLastContactTime, exportBO.getLastContactEnd());
            }
        }
        if (exportBO.getNextFollowStart() != null) {
            wrapper.ge(Customer::getNextFollowTime, exportBO.getNextFollowStart());
        }
        if (exportBO.getNextFollowEnd() != null) {
            wrapper.le(Customer::getNextFollowTime, exportBO.getNextFollowEnd());
        }
        if (exportBO.getCreateTimeStart() != null) {
            wrapper.ge(Customer::getCreateTime, exportBO.getCreateTimeStart());
        }
        if (exportBO.getCreateTimeEnd() != null) {
            wrapper.le(Customer::getCreateTime, exportBO.getCreateTimeEnd());
        }
        if (exportBO.getContactCountMin() != null) {
            wrapper.ge(Customer::getContactCount, exportBO.getContactCountMin());
        }
        if (exportBO.getContactCountMax() != null) {
            wrapper.le(Customer::getContactCount, exportBO.getContactCountMax());
        }

        applyExportSort(wrapper, exportBO.getSortBy(), exportBO.getSortOrder());
        return wrapper;
    }

    private void applyExportSort(LambdaQueryWrapper<Customer> wrapper, String sortBy, String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if (StrUtil.isBlank(sortBy) || "createTime".equals(sortBy)) {
            if (asc) {
                wrapper.orderByAsc(Customer::getCreateTime);
            } else {
                wrapper.orderByDesc(Customer::getCreateTime);
            }
            return;
        }

        switch (sortBy) {
            case "quotation" -> wrapper.orderBy(true, asc, Customer::getQuotation);
            case "lastContactTime" -> wrapper.orderBy(true, asc, Customer::getLastContactTime);
            case "nextFollowTime" -> wrapper.orderBy(true, asc, Customer::getNextFollowTime);
            case "contactCount" -> wrapper.orderBy(true, asc, Customer::getContactCount);
            default -> {
                if (asc) {
                    wrapper.orderByAsc(Customer::getCreateTime);
                } else {
                    wrapper.orderByDesc(Customer::getCreateTime);
                }
                return;
            }
        }
        wrapper.orderByDesc(Customer::getCreateTime);
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

    private void applyCenteredStyle(Sheet sheet, Workbook wb, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) return;
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        CellStyle newStyle = wb.createCellStyle();
        newStyle.cloneStyleFrom(cell.getCellStyle());
        newStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        newStyle.setAlignment(HorizontalAlignment.CENTER);
        cell.setCellStyle(newStyle);
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
                "预计成交金额", "备注",
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
                + "4. 预计成交金额：数字，支持小数\n"
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
            int colQuotation = headers.indexOf("预计成交金额");
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

                // 预计成交金额
                String quotationStr = getCellStr(rowData, colQuotation);
                if (StrUtil.isNotEmpty(quotationStr)) {
                    try {
                        bo.setQuotation(new BigDecimal(quotationStr.replace(",", "")));
                    } catch (NumberFormatException e) {
                        rowErrors.add("预计成交金额格式无效");
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
                            syncContactCache(existing.getCustomerId());
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
                syncContactCache(customer.getCustomerId());
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
    public void refreshCustomerActivity(Long customerId) {
        if (customerId == null) {
            return;
        }
        Customer customer = baseMapper.selectByIdIgnoreDataPermission(customerId);
        if (customer == null || !Objects.equals(customer.getStatus(), 1)) {
            return;
        }
        globalSearchIndexService.refreshCustomerIndex(customerId);
        globalSearchIndexService.refreshCustomerRelatedIndexes(customerId);
        taskService.refreshValuePriorityByCustomerId(customerId);
    }

    @Override
    public CustomerLogoUploadVO uploadCustomerLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Logo image is required");
        }

        String extension = resolveCustomerLogoExtension(file);
        if (extension == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Only png, jpg, jpeg, webp and gif images are supported");
        }

        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String path = "customer/logo/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
        String logo = fileStorageService.upload(file, path);

        CustomerLogoUploadVO vo = new CustomerLogoUploadVO();
        vo.setLogo(logo);
        vo.setLogoUrl(resolveFileUrl(logo));
        return vo;
    }

    private String resolveCustomerLogoExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (StrUtil.isNotBlank(filename)) {
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
                String extension = filename.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (CUSTOMER_LOGO_EXTENSIONS.contains(extension)) {
                    return extension;
                }
            }
        }

        String contentType = StrUtil.trim(file.getContentType());
        if (StrUtil.isBlank(contentType)) {
            return null;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> null;
        };
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
                .eq(Contact::getStatus, 1)
                .orderByDesc(Contact::getIsPrimary)
                .orderByAsc(Contact::getCreateTime)
                .orderByAsc(Contact::getContactId)
                .last("LIMIT 1")
        );
        Long count = contactMapper.selectCount(
            new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId)
                .eq(Contact::getStatus, 1)
        );
        Date updateTime = new Date();
        var update = lambdaUpdate()
            .eq(Customer::getCustomerId, customerId)
            .set(Customer::getPrimaryContactName, primary != null ? primary.getName() : null)
            .set(Customer::getPrimaryContactPhone, primary != null ? primary.getPhone() : null)
            .set(Customer::getPrimaryContactPosition, primary != null ? primary.getPosition() : null)
            .set(Customer::getContactCount, count.intValue())
            .set(Customer::getUpdateTime, updateTime);
        Long updateUserId = getCurrentUserIdOrNull();
        if (updateUserId != null) {
            update.set(Customer::getUpdateUserId, updateUserId);
        }
        update.update();
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
        Date updateTime = new Date();
        var update = lambdaUpdate()
            .eq(Customer::getCustomerId, customerId)
            .set(Customer::getTagNames, tagNames)
            .set(Customer::getUpdateTime, updateTime);
        Long updateUserId = getCurrentUserIdOrNull();
        if (updateUserId != null) {
            update.set(Customer::getUpdateUserId, updateUserId);
        }
        update.update();
    }

    // ==================== AI 智能录入 ====================

    private Long getCurrentUserIdOrNull() {
        try {
            return UserUtil.getUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerAiReportVO generateAiReport(Long customerId) {
        Customer customer = getById(customerId);
        if (customer == null || Objects.equals(customer.getStatus(), 0)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Customer does not exist");
        }

        CustomerAiReportVO report = new CustomerAiReportVO();
        report.setCustomerId(customerId);
        report.setAiStatusDetection(StrUtil.blankToDefault(customer.getAiStatusDetection(), "pending"));
        report.setAiInsight(StrUtil.blankToDefault(customer.getAiInsight(), "No AI analysis has been generated yet."));

        String prompt = """
                Analyze this CRM customer and return compact JSON only:
                {
                  "aiStatusDetection": "short status diagnosis",
                  "aiInsight": "business insight",
                  "aiDeepInsight": "deeper opportunity/risk analysis",
                  "aiNextStep": "recommended next action"
                }
                Customer:
                name=%s
                industry=%s
                stage=%s
                level=%s
                source=%s
                quotation=%s
                lastContactTime=%s
                nextFollowTime=%s
                remark=%s
                """.formatted(
                StrUtil.blankToDefault(customer.getCompanyName(), ""),
                StrUtil.blankToDefault(customer.getIndustry(), ""),
                StrUtil.blankToDefault(customer.getStage(), ""),
                StrUtil.blankToDefault(customer.getLevel(), ""),
                StrUtil.blankToDefault(customer.getSource(), ""),
                customer.getQuotation(),
                customer.getLastContactTime(),
                customer.getNextFollowTime(),
                StrUtil.blankToDefault(customer.getRemark(), "")
        );

        try {
            String response = chatClientProvider.getChatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();
            String json = StrUtil.trimToEmpty(response);
            if (json.startsWith("```")) {
                json = json.replaceFirst("```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }
            JsonNode root = objectMapper.readTree(json);
            report.setAiStatusDetection(StrUtil.blankToDefault(getJsonText(root, "aiStatusDetection"), report.getAiStatusDetection()));
            report.setAiInsight(StrUtil.blankToDefault(getJsonText(root, "aiInsight"), report.getAiInsight()));
            report.setAiDeepInsight(getJsonText(root, "aiDeepInsight"));
            report.setAiNextStep(getJsonText(root, "aiNextStep"));

            customer.setAiStatusDetection(report.getAiStatusDetection());
            customer.setAiInsight(report.getAiInsight());
            customer.setAiParseSnapshot(json);
            customer.setAiAnalysisStatus("completed");
            customer.setAiAnalysisRequestedAt(new Date());
            updateById(customer);
        } catch (Exception e) {
            log.warn("Customer AI report generation failed, customerId={}, error={}", customerId, e.getMessage());
            customer.setAiAnalysisStatus("failed");
            customer.setAiAnalysisRequestedAt(new Date());
            updateById(customer);
        }
        return report;
    }

    @Override
    public CustomerAiParseVO aiParseCustomer(CustomerAiParseBO parseBO) {
        String prompt = String.format(AI_CUSTOMER_PARSE_PROMPT, parseBO.getContent());

        try {
            String response;

            if (StrUtil.isNotEmpty(parseBO.getImageObjectKey())) {
                // Multimodal: text + image
                String imageUrl = fileStorageService.getUrl(parseBO.getImageObjectKey());
                String mimeTypeStr = StrUtil.blankToDefault(parseBO.getImageMimeType(), "image/png");
                MimeType mimeType = MimeType.valueOf(mimeTypeStr);
                Media media = Media.builder()
                        .mimeType(mimeType)
                        .data(URI.create(imageUrl).toURL())
                        .build();

                response = chatClientProvider.getChatClient()
                        .prompt()
                        .user(u -> u.text(prompt).media(media))
                        .call()
                        .content();
            } else {
                // Text only
                response = chatClientProvider.getChatClient()
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();
            }

            log.info("AI 客户录入解析原始响应: {}", response);
            return parseCustomerAiResponse(response);
        } catch (Exception e) {
            log.error("AI 客户录入解析失败", e);
            return buildFallbackCustomerResult();
        }
    }

    @Override
    public CustomerAiSearchParseVO aiParseSearch(CustomerAiSearchParseBO parseBO) {
        String normalizedQuery = StrUtil.trim(parseBO.getQuery());
        if (StrUtil.isBlank(normalizedQuery)) {
            return buildFallbackSearchResult("");
        }

        String now = LocalDateTime.now().format(AI_SEARCH_TIME_FORMATTER);
        String prompt = String.format(AI_CUSTOMER_SEARCH_PARSE_PROMPT, now, normalizedQuery);

        try {
            String response = chatClientProvider.getChatClient()
                .prompt()
                .user(prompt)
                .call()
                .content();

            log.info("AI 客户搜索解析原始响应: {}", response);
            return parseCustomerAiSearchResponse(response, normalizedQuery);
        } catch (Exception e) {
            log.error("AI 客户搜索解析失败", e);
            CustomerAiSearchQueryVO heuristicQuery = buildHeuristicSearchQuery(normalizedQuery);
            if (!isSearchQueryEmpty(heuristicQuery)) {
                return buildSearchResult(normalizedQuery, heuristicQuery, "已使用内置规则解析搜索条件", 0.65, false);
            }
            return buildFallbackSearchResult(normalizedQuery);
        }
    }

    private CustomerAiParseVO parseCustomerAiResponse(String response) {
        try {
            // Strip markdown code block markers if present
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(json);
            CustomerAiParseVO vo = new CustomerAiParseVO();
            vo.setCompanyName(getJsonText(root, "companyName"));
            vo.setIndustry(getJsonText(root, "industry"));
            vo.setLevel(getJsonText(root, "level"));
            vo.setStage(getJsonText(root, "stage"));
            vo.setSource(getJsonText(root, "source"));
            vo.setRemark(getJsonText(root, "remark"));
            vo.setContactName(getJsonText(root, "contactName"));
            vo.setContactPhone(getJsonText(root, "contactPhone"));
            vo.setContactEmail(getJsonText(root, "contactEmail"));
            vo.setContactPosition(getJsonText(root, "contactPosition"));
            vo.setSummary(getJsonText(root, "summary"));
            vo.setNextStep(getJsonText(root, "nextStep"));

            if (root.has("score") && !root.get("score").isNull()) {
                vo.setScore(root.get("score").asInt(50));
            }

            vo.setTags(getJsonStringList(root, "tags"));
            vo.setKeyPoints(getJsonStringList(root, "keyPoints"));

            return vo;
        } catch (Exception e) {
            log.warn("AI 客户录入响应 JSON 解析失败: {}", e.getMessage());
            return buildFallbackCustomerResult();
        }
    }

    private CustomerAiSearchParseVO parseCustomerAiSearchResponse(String response, String normalizedQuery) {
        try {
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode queryNode = root.has("parsedQuery") ? root.get("parsedQuery") : root;
            CustomerAiSearchQueryVO query = toCustomerAiSearchQuery(queryNode);
            enrichSearchQueryWithHeuristics(query, normalizedQuery);

            if (isSearchQueryEmpty(query)) {
                return buildFallbackSearchResult(normalizedQuery);
            }

            String explanation = getJsonText(root, "explanation");
            if (StrUtil.isBlank(explanation)) {
                explanation = "已将自然语言搜索解析为结构化筛选条件";
            }
            Double confidence = parseJsonConfidence(root.get("confidence"));
            return buildSearchResult(normalizedQuery, query, explanation, confidence, false);
        } catch (Exception e) {
            log.warn("AI 客户搜索响应 JSON 解析失败: {}", e.getMessage());
            CustomerAiSearchQueryVO heuristicQuery = buildHeuristicSearchQuery(normalizedQuery);
            if (!isSearchQueryEmpty(heuristicQuery)) {
                return buildSearchResult(normalizedQuery, heuristicQuery, "已使用内置规则解析搜索条件", 0.65, false);
            }
            return buildFallbackSearchResult(normalizedQuery);
        }
    }

    private CustomerAiSearchQueryVO toCustomerAiSearchQuery(JsonNode queryNode) {
        CustomerAiSearchQueryVO query = new CustomerAiSearchQueryVO();
        if (queryNode == null || queryNode.isNull()) {
            return query;
        }

        query.setKeyword(normalizeOptionalText(getJsonText(queryNode, "keyword")));

        String stage = normalizeStageCode(getJsonText(queryNode, "stage"));
        query.setStage(stage);

        List<String> stages = getJsonStringList(queryNode, "stages").stream()
            .map(this::normalizeStageCode)
            .filter(StrUtil::isNotBlank)
            .distinct()
            .toList();
        if (!stages.isEmpty()) {
            query.setStages(stages);
            if (stages.size() == 1 && StrUtil.isBlank(query.getStage())) {
                query.setStage(stages.get(0));
            }
        }

        String level = normalizeOptionalText(getJsonText(queryNode, "level"));
        if (StrUtil.isNotBlank(level)) {
            level = level.toUpperCase(Locale.ROOT);
            if (VALID_LEVELS.contains(level)) {
                query.setLevel(level);
            }
        }

        query.setIndustry(normalizeOptionalText(getJsonText(queryNode, "industry")));
        query.setTag(normalizeOptionalText(getJsonText(queryNode, "tag")));
        query.setSource(normalizeOptionalText(getJsonText(queryNode, "source")));
        query.setQuotationMin(parseJsonBigDecimal(queryNode.get("quotationMin")));
        query.setQuotationMax(parseJsonBigDecimal(queryNode.get("quotationMax")));
        query.setLastContactStart(parseJsonDate(queryNode.get("lastContactStart")));
        query.setLastContactEnd(parseJsonDate(queryNode.get("lastContactEnd")));
        query.setIncludeNoLastContact(parseJsonBoolean(queryNode.get("includeNoLastContact")));
        query.setNextFollowStart(parseJsonDate(queryNode.get("nextFollowStart")));
        query.setNextFollowEnd(parseJsonDate(queryNode.get("nextFollowEnd")));
        query.setCreateTimeStart(parseJsonDate(queryNode.get("createTimeStart")));
        query.setCreateTimeEnd(parseJsonDate(queryNode.get("createTimeEnd")));
        query.setContactCountMin(parseJsonInteger(queryNode.get("contactCountMin")));
        query.setContactCountMax(parseJsonInteger(queryNode.get("contactCountMax")));
        query.setFilters(parseJsonCustomerFieldFilters(queryNode.get("filters")));
        normalizeAiSearchFieldFilters(query);
        query.setSortBy(normalizeSortBy(getJsonText(queryNode, "sortBy")));
        query.setSortOrder(normalizeSortOrder(getJsonText(queryNode, "sortOrder")));
        return query;
    }

    private CustomerAiSearchParseVO buildSearchResult(String originalQuery,
                                                      CustomerAiSearchQueryVO query,
                                                      String explanation,
                                                      Double confidence,
                                                      boolean fallbackKeywordSearch) {
        CustomerAiSearchParseVO vo = new CustomerAiSearchParseVO();
        vo.setOriginalQuery(originalQuery);
        vo.setNormalizedQuery(originalQuery);
        vo.setParsedQuery(query);
        List<CustomerAiSearchDisplayChipVO> displayChips = buildSearchChips(query);
        vo.setDisplayChips(displayChips);
        vo.setExplanation(resolveSearchExplanation(originalQuery, displayChips, explanation, fallbackKeywordSearch));
        vo.setConfidence(confidence != null ? confidence : 0.8);
        vo.setFallbackKeywordSearch(fallbackKeywordSearch);
        return vo;
    }

    private CustomerAiSearchParseVO buildFallbackSearchResult(String normalizedQuery) {
        CustomerAiSearchQueryVO query = new CustomerAiSearchQueryVO();
        query.setKeyword(normalizedQuery);
        return buildSearchResult(
            normalizedQuery,
            query,
            StrUtil.isBlank(normalizedQuery) ? "未识别到有效搜索内容" : "已回退为关键词搜索",
            StrUtil.isBlank(normalizedQuery) ? 0.0 : 0.35,
            true
        );
    }

    private CustomerAiSearchQueryVO buildHeuristicSearchQuery(String normalizedQuery) {
        CustomerAiSearchQueryVO query = new CustomerAiSearchQueryVO();
        enrichSearchQueryWithHeuristics(query, normalizedQuery);
        return query;
    }

    private void normalizeAiSearchFieldFilters(CustomerAiSearchQueryVO query) {
        if (query == null || query.getFilters() == null || query.getFilters().isEmpty()) {
            return;
        }

        List<CustomerFieldFilterBO> normalizedFilters = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CustomerFieldFilterBO filter : query.getFilters()) {
            if (filter == null) {
                continue;
            }
            String fieldSource = normalizeFieldSource(filter.getFieldSource());
            String fieldName = normalizeCustomerFilterFieldName(filter.getFieldName(), fieldSource);
            String operator = normalizeEmptyFilterOperator(filter.getOperator());
            if (StrUtil.isBlank(fieldName) || StrUtil.isBlank(operator) || !isKnownCustomerFilterField(fieldName, fieldSource)) {
                continue;
            }
            String dedupeKey = fieldSource + ":" + fieldName + ":" + operator;
            if (!seen.add(dedupeKey)) {
                continue;
            }
            CustomerFieldFilterBO normalizedFilter = new CustomerFieldFilterBO();
            normalizedFilter.setFieldName(fieldName);
            normalizedFilter.setFieldSource(fieldSource);
            normalizedFilter.setOperator(operator);
            normalizedFilters.add(normalizedFilter);
        }
        query.setFilters(normalizedFilters.isEmpty() ? null : normalizedFilters);
    }

    private String normalizeCustomerFilterFieldName(String fieldName, String fieldSource) {
        String normalized = normalizeOptionalText(fieldName);
        if (StrUtil.isBlank(normalized) || FIELD_SOURCE_CUSTOM.equals(normalizeFieldSource(fieldSource))) {
            return normalized;
        }
        if (CUSTOMER_SYSTEM_FILTER_FIELDS.containsKey(normalized)) {
            return normalized;
        }
        return switch (normalized) {
            case "company_name", "公司", "公司名称", "客户名称", "客户名" -> "companyName";
            case "行业", "所属行业" -> "industry";
            case "商机阶段", "客户阶段", "阶段" -> "stage";
            case "客户级别", "级别" -> "level";
            case "客户来源", "来源" -> "source";
            case "地址", "客户地址" -> "address";
            case "官网", "网站", "网址", "website" -> "website";
            case "logo", "Logo", "公司Logo", "公司 Logo" -> "logo";
            case "金额", "报价", "预计成交金额", "quotation" -> "quotation";
            case "last_contact_time", "最后跟进", "最后跟进时间", "跟进时间" -> "lastContactTime";
            case "next_follow_time", "下次跟进", "下次跟进时间" -> "nextFollowTime";
            case "备注", "客户备注" -> "remark";
            case "负责人", "owner_id" -> "ownerId";
            case "主联系人", "联系人姓名", "主联系人姓名", "primary_contact_name" -> "primaryContactName";
            case "联系人电话", "主联系人电话", "电话", "手机号", "手机", "primary_contact_phone" -> "primaryContactPhone";
            case "联系人职位", "主联系人职位", "职位", "primary_contact_position" -> "primaryContactPosition";
            case "联系人数量", "联系人数", "contact_count" -> "contactCount";
            case "标签", "tag_names" -> "tagNames";
            case "创建时间", "create_time" -> "createTime";
            case "更新时间", "update_time" -> "updateTime";
            default -> null;
        };
    }

    private boolean isKnownCustomerFilterField(String fieldName, String fieldSource) {
        if (StrUtil.isBlank(fieldName)) {
            return false;
        }
        if (FIELD_SOURCE_CUSTOM.equals(normalizeFieldSource(fieldSource))) {
            return getCustomerCustomFilterFieldMap().containsKey(fieldName);
        }
        return CUSTOMER_SYSTEM_FILTER_FIELDS.containsKey(fieldName);
    }

    private void addAiSearchFieldFilter(CustomerAiSearchQueryVO query,
                                        String fieldName,
                                        String fieldSource,
                                        String operator) {
        if (query == null || StrUtil.isBlank(fieldName) || StrUtil.isBlank(operator)) {
            return;
        }
        List<CustomerFieldFilterBO> filters = query.getFilters() == null
                ? new ArrayList<>()
                : new ArrayList<>(query.getFilters());
        String normalizedFieldSource = normalizeFieldSource(fieldSource);
        String normalizedOperator = normalizeEmptyFilterOperator(operator);
        String normalizedFieldName = normalizeCustomerFilterFieldName(fieldName, normalizedFieldSource);
        if (StrUtil.isBlank(normalizedOperator)
                || StrUtil.isBlank(normalizedFieldName)
                || !isKnownCustomerFilterField(normalizedFieldName, normalizedFieldSource)) {
            return;
        }
        boolean exists = filters.stream().anyMatch(filter ->
                filter != null
                        && normalizedFieldName.equals(normalizeCustomerFilterFieldName(filter.getFieldName(), filter.getFieldSource()))
                        && normalizedFieldSource.equals(normalizeFieldSource(filter.getFieldSource()))
                        && normalizedOperator.equals(normalizeEmptyFilterOperator(filter.getOperator()))
        );
        if (exists) {
            query.setFilters(filters);
            return;
        }
        CustomerFieldFilterBO filter = new CustomerFieldFilterBO();
        filter.setFieldName(normalizedFieldName);
        filter.setFieldSource(normalizedFieldSource);
        filter.setOperator(normalizedOperator);
        filters.add(filter);
        query.setFilters(filters);
    }

    private void applyPresenceFiltersFromQuery(CustomerAiSearchQueryVO query, String normalizedQuery) {
        if (StrUtil.isBlank(normalizedQuery)) {
            return;
        }
        if (isFollowedCustomerQuery(normalizedQuery)) {
            query.setLastContactStart(null);
            query.setLastContactEnd(null);
            query.setIncludeNoLastContact(null);
            addAiSearchFieldFilter(query, "lastContactTime", FIELD_SOURCE_SYSTEM, FILTER_OPERATOR_IS_NOT_EMPTY);
        }
        applyPresenceFilter(query, normalizedQuery, "website",
                new String[]{"官网", "网站", "网址"},
                FIELD_SOURCE_SYSTEM);
        applyPresenceFilter(query, normalizedQuery, "primaryContactPhone",
                new String[]{"电话", "手机号", "手机", "联系方式"},
                FIELD_SOURCE_SYSTEM);
        applyPresenceFilter(query, normalizedQuery, "primaryContactName",
                new String[]{"联系人", "主联系人"},
                FIELD_SOURCE_SYSTEM);
        applyPresenceFilter(query, normalizedQuery, "logo",
                new String[]{"logo", "Logo", "公司Logo", "公司 Logo"},
                FIELD_SOURCE_SYSTEM);
        applyPresenceFilter(query, normalizedQuery, "remark",
                new String[]{"备注"},
                FIELD_SOURCE_SYSTEM);
        applyPresenceFilter(query, normalizedQuery, "tagNames",
                new String[]{"标签"},
                FIELD_SOURCE_SYSTEM);
        normalizeAiSearchFieldFilters(query);
    }

    private void applyPresenceFilter(CustomerAiSearchQueryVO query,
                                     String normalizedQuery,
                                     String fieldName,
                                     String[] keywords,
                                     String fieldSource) {
        if (!containsAny(normalizedQuery, keywords)) {
            return;
        }
        if (containsAny(normalizedQuery, "为空", "空的", "未填写", "没填写", "未设置", "没设置", "没有", "无")) {
            addAiSearchFieldFilter(query, fieldName, fieldSource, FILTER_OPERATOR_IS_EMPTY);
            return;
        }
        if (containsAny(normalizedQuery, "不为空", "非空", "已填写", "有值", "有")) {
            addAiSearchFieldFilter(query, fieldName, fieldSource, FILTER_OPERATOR_IS_NOT_EMPTY);
        }
    }

    private boolean isFollowedCustomerQuery(String normalizedQuery) {
        if (StrUtil.isBlank(normalizedQuery)) {
            return false;
        }
        if (containsAny(normalizedQuery, "未跟进", "无跟进", "没有跟进", "从未跟进")) {
            return false;
        }
        return containsAny(
                normalizedQuery,
                "已跟进",
                "已经跟进",
                "有跟进记录",
                "有跟进的客户",
                "写过跟进",
                "已写跟进",
                "跟进不为空",
                "跟进非空"
        );
    }

    private boolean containsAny(String value, String... candidates) {
        if (StrUtil.isBlank(value) || candidates == null || candidates.length == 0) {
            return false;
        }
        for (String candidate : candidates) {
            if (StrUtil.isNotBlank(candidate) && value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void enrichSearchQueryWithHeuristics(CustomerAiSearchQueryVO query, String normalizedQuery) {
        if (query == null || StrUtil.isBlank(normalizedQuery)) {
            return;
        }

        applyPresenceFiltersFromQuery(query, normalizedQuery);

        if (StrUtil.isBlank(query.getIndustry())) {
            for (String industry : COMMON_INDUSTRIES) {
                if (normalizedQuery.contains(industry)) {
                    query.setIndustry(industry);
                    break;
                }
            }
        }

        if (query.getQuotationMin() == null && normalizedQuery.contains("高价值")) {
            query.setQuotationMin(BigDecimal.valueOf(500_000));
            if (StrUtil.isBlank(query.getSortBy())) {
                query.setSortBy("quotation");
            }
            if (StrUtil.isBlank(query.getSortOrder())) {
                query.setSortOrder("desc");
            }
        }

        if ((query.getStages() == null || query.getStages().isEmpty())
            && StrUtil.isBlank(query.getStage())
            && normalizedQuery.contains("活跃")) {
            query.setStages(new ArrayList<>(ACTIVE_STAGE_CODES));
        }

        if (query.getLastContactEnd() == null) {
            Matcher daysMatcher = DAYS_WITHOUT_CONTACT_PATTERN.matcher(normalizedQuery);
            if (daysMatcher.find()) {
                int days = Integer.parseInt(daysMatcher.group(1));
                query.setLastContactEnd(DateUtil.offsetDay(new Date(), -days));
                query.setIncludeNoLastContact(Boolean.TRUE);
                if (StrUtil.isBlank(query.getSortBy())) {
                    query.setSortBy("lastContactTime");
                }
                if (StrUtil.isBlank(query.getSortOrder())) {
                    query.setSortOrder("asc");
                }
            }
        }

        if (query.getCreateTimeStart() == null) {
            Matcher recentNewMatcher = RECENT_NEW_CUSTOMERS_PATTERN.matcher(normalizedQuery);
            if (recentNewMatcher.find()) {
                int days = Integer.parseInt(recentNewMatcher.group(1));
                query.setCreateTimeStart(DateUtil.offsetDay(new Date(), -days));
                query.setSortBy("createTime");
                query.setSortOrder("desc");
            } else if (normalizedQuery.contains("最近一周新增") || normalizedQuery.contains("近一周新增")) {
                query.setCreateTimeStart(DateUtil.offsetDay(new Date(), -7));
                query.setSortBy("createTime");
                query.setSortOrder("desc");
            }
        }

        if (StrUtil.isBlank(query.getLevel())) {
            Matcher levelMatcher = LEVEL_PATTERN.matcher(normalizedQuery);
            if (levelMatcher.find()) {
                String level = levelMatcher.group(1).toUpperCase(Locale.ROOT);
                if (VALID_LEVELS.contains(level)) {
                    query.setLevel(level);
                }
            }
        }
    }

    private boolean isSearchQueryEmpty(CustomerAiSearchQueryVO query) {
        if (query == null) {
            return true;
        }
        return StrUtil.isBlank(query.getKeyword())
            && StrUtil.isBlank(query.getStage())
            && (query.getStages() == null || query.getStages().isEmpty())
            && StrUtil.isBlank(query.getLevel())
            && StrUtil.isBlank(query.getIndustry())
            && StrUtil.isBlank(query.getTag())
            && StrUtil.isBlank(query.getSource())
            && query.getQuotationMin() == null
            && query.getQuotationMax() == null
            && query.getLastContactStart() == null
            && query.getLastContactEnd() == null
            && !Boolean.TRUE.equals(query.getIncludeNoLastContact())
            && query.getNextFollowStart() == null
            && query.getNextFollowEnd() == null
            && query.getCreateTimeStart() == null
            && query.getCreateTimeEnd() == null
            && query.getContactCountMin() == null
            && query.getContactCountMax() == null
            && (query.getFilters() == null || query.getFilters().isEmpty());
    }

    private List<CustomerAiSearchDisplayChipVO> buildSearchChips(CustomerAiSearchQueryVO query) {
        List<CustomerAiSearchDisplayChipVO> chips = new ArrayList<>();
        if (query == null) {
            return chips;
        }

        if (StrUtil.isNotBlank(query.getKeyword())) {
            chips.add(new CustomerAiSearchDisplayChipVO("keyword", "关键词: " + query.getKeyword()));
        }
        if (StrUtil.isNotBlank(query.getIndustry())) {
            chips.add(new CustomerAiSearchDisplayChipVO("industry", "行业: " + query.getIndustry()));
        }
        if (StrUtil.isNotBlank(query.getLevel())) {
            chips.add(new CustomerAiSearchDisplayChipVO("level", "级别: " + query.getLevel()));
        }
        if (StrUtil.isNotBlank(query.getStage())) {
            chips.add(new CustomerAiSearchDisplayChipVO("stage", "阶段: " + getStageLabel(query.getStage())));
        } else if (query.getStages() != null && !query.getStages().isEmpty()) {
            String labels = query.getStages().stream().map(this::getStageLabel).collect(Collectors.joining(" / "));
            chips.add(new CustomerAiSearchDisplayChipVO("stages", "阶段: " + labels));
        }
        if (StrUtil.isNotBlank(query.getTag())) {
            chips.add(new CustomerAiSearchDisplayChipVO("tag", "标签: " + query.getTag()));
        }
        if (StrUtil.isNotBlank(query.getSource())) {
            chips.add(new CustomerAiSearchDisplayChipVO("source", "来源: " + query.getSource()));
        }
        if (query.getQuotationMin() != null || query.getQuotationMax() != null) {
            chips.add(new CustomerAiSearchDisplayChipVO("quotation", buildAmountRangeLabel("预计成交金额", query.getQuotationMin(), query.getQuotationMax())));
        }
        if (query.getLastContactStart() != null || query.getLastContactEnd() != null || Boolean.TRUE.equals(query.getIncludeNoLastContact())) {
            chips.add(new CustomerAiSearchDisplayChipVO("lastContact", buildDateRangeLabel("最后跟进", query.getLastContactStart(), query.getLastContactEnd(), Boolean.TRUE.equals(query.getIncludeNoLastContact()))));
        }
        if (query.getNextFollowStart() != null || query.getNextFollowEnd() != null) {
            chips.add(new CustomerAiSearchDisplayChipVO("nextFollow", buildDateRangeLabel("下次跟进", query.getNextFollowStart(), query.getNextFollowEnd(), false)));
        }
        if (query.getCreateTimeStart() != null || query.getCreateTimeEnd() != null) {
            chips.add(new CustomerAiSearchDisplayChipVO("createTime", buildDateRangeLabel("创建时间", query.getCreateTimeStart(), query.getCreateTimeEnd(), false)));
        }
        if (query.getContactCountMin() != null || query.getContactCountMax() != null) {
            String label = "联系人";
            if (query.getContactCountMin() != null && query.getContactCountMax() != null) {
                label += ": " + query.getContactCountMin() + "-" + query.getContactCountMax();
            } else if (query.getContactCountMin() != null) {
                label += ">=" + query.getContactCountMin();
            } else {
                label += "<=" + query.getContactCountMax();
            }
            chips.add(new CustomerAiSearchDisplayChipVO("contactCount", label));
        }
        if (query.getFilters() != null && !query.getFilters().isEmpty()) {
            for (CustomerFieldFilterBO filter : query.getFilters()) {
                String key = buildFieldFilterChipKey(filter);
                String label = buildFieldFilterChipLabel(filter);
                if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(label)) {
                    chips.add(new CustomerAiSearchDisplayChipVO(key, label));
                }
            }
        }
        if (StrUtil.isNotBlank(query.getSortBy())) {
            chips.add(new CustomerAiSearchDisplayChipVO("sort", buildSortLabel(query.getSortBy(), query.getSortOrder())));
        }
        return chips;
    }

    private String buildFieldFilterChipKey(CustomerFieldFilterBO filter) {
        if (filter == null || StrUtil.isBlank(filter.getFieldName())) {
            return null;
        }
        String operator = normalizeEmptyFilterOperator(filter.getOperator());
        if (StrUtil.isBlank(operator)) {
            return null;
        }
        return "filter:" + normalizeFieldSource(filter.getFieldSource())
                + ":" + filter.getFieldName()
                + ":" + operator;
    }

    private String buildFieldFilterChipLabel(CustomerFieldFilterBO filter) {
        if (filter == null) {
            return null;
        }
        String operator = normalizeEmptyFilterOperator(filter.getOperator());
        String fieldName = normalizeCustomerFilterFieldName(filter.getFieldName(), filter.getFieldSource());
        if (StrUtil.isBlank(operator) || StrUtil.isBlank(fieldName)) {
            return null;
        }

        String fieldLabel = resolveFilterFieldLabel(fieldName, filter.getFieldSource());
        String operatorLabel = FILTER_OPERATOR_IS_EMPTY.equals(operator) ? "为空" : "不为空";
        if (FILTER_OPERATOR_IS_NOT_EMPTY.equals(operator)
                && FIELD_SOURCE_SYSTEM.equals(normalizeFieldSource(filter.getFieldSource()))
                && "lastContactTime".equals(fieldName)) {
            operatorLabel = "已跟进";
        }
        return fieldLabel + ": " + operatorLabel;
    }

    private String resolveFilterFieldLabel(String fieldName, String fieldSource) {
        if (FIELD_SOURCE_CUSTOM.equals(normalizeFieldSource(fieldSource))) {
            CustomFieldVO customField = getCustomerCustomFilterFieldMap().get(fieldName);
            return customField == null ? fieldName : StrUtil.blankToDefault(customField.getFieldLabel(), customField.getFieldName());
        }
        CustomerFilterFieldDefinition definition = CUSTOMER_SYSTEM_FILTER_FIELDS.get(fieldName);
        return definition != null ? definition.label() : fieldName;
    }

    private String getStageLabel(String stage) {
        if (StrUtil.isBlank(stage)) {
            return "未知";
        }
        return STAGE_LABEL_MAP.getOrDefault(stage, stage);
    }

    private String buildAmountRangeLabel(String prefix, BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return prefix + ": " + formatCompactAmount(min) + " - " + formatCompactAmount(max);
        }
        if (min != null) {
            return prefix + ">=" + formatCompactAmount(min);
        }
        return prefix + "<=" + formatCompactAmount(max);
    }

    private String buildDateRangeLabel(String prefix, Date start, Date end, boolean includeNoValue) {
        List<String> parts = new ArrayList<>();
        if (start != null) {
            parts.add("从 " + DateUtil.formatDateTime(start));
        }
        if (end != null) {
            parts.add("到 " + DateUtil.formatDateTime(end));
        }
        if (parts.isEmpty() && includeNoValue) {
            return prefix + ": 包含未跟进";
        }
        if (includeNoValue) {
            parts.add("包含未跟进");
        }
        return prefix + ": " + String.join(" ", parts);
    }

    private String buildSortLabel(String sortBy, String sortOrder) {
        String label = switch (sortBy) {
            case "quotation" -> "预计成交金额";
            case "lastContactTime" -> "最后跟进";
            case "nextFollowTime" -> "下次跟进";
            case "contactCount" -> "联系人数量";
            default -> "创建时间";
        };
        return "排序: 按" + label + ("asc".equalsIgnoreCase(sortOrder) ? "升序" : "降序");
    }

    private String resolveSearchExplanation(String originalQuery,
                                            List<CustomerAiSearchDisplayChipVO> displayChips,
                                            String rawExplanation,
                                            boolean fallbackKeywordSearch) {
        if (fallbackKeywordSearch) {
            return StrUtil.isBlank(originalQuery) ? "未识别到有效搜索内容" : "未识别出明确的结构化筛选条件，已回退为关键词搜索";
        }

        String sanitizedExplanation = sanitizeSearchExplanation(rawExplanation);
        String chipExplanation = buildSearchExplanationFromChips(displayChips);

        if (containsInternalSearchField(rawExplanation) && StrUtil.isNotBlank(chipExplanation)) {
            return chipExplanation;
        }
        if (StrUtil.isNotBlank(sanitizedExplanation)) {
            return sanitizedExplanation;
        }
        if (StrUtil.isNotBlank(chipExplanation)) {
            return chipExplanation;
        }
        return "已将自然语言搜索解析为结构化筛选条件";
    }

    private String buildSearchExplanationFromChips(List<CustomerAiSearchDisplayChipVO> displayChips) {
        if (displayChips == null || displayChips.isEmpty()) {
            return null;
        }

        List<String> filterLabels = new ArrayList<>();
        String sortLabel = null;
        for (CustomerAiSearchDisplayChipVO chip : displayChips) {
            if (chip == null || StrUtil.isBlank(chip.getLabel())) {
                continue;
            }
            if ("sort".equals(chip.getKey())) {
                sortLabel = chip.getLabel().trim();
            } else {
                filterLabels.add(chip.getLabel().trim());
            }
        }

        List<String> parts = new ArrayList<>();
        if (!filterLabels.isEmpty()) {
            parts.add("已识别筛选条件：" + String.join("，", filterLabels));
        }
        if (StrUtil.isNotBlank(sortLabel)) {
            parts.add("已识别" + sortLabel.replaceFirst("^排序[:：]\\s*", "排序规则："));
        }
        return parts.isEmpty() ? null : String.join("；", parts);
    }

    private String sanitizeSearchExplanation(String explanation) {
        String sanitized = StrUtil.trim(explanation);
        if (StrUtil.isBlank(sanitized)) {
            return null;
        }

        sanitized = sanitized.replace("`", "").replace("\"", "");
        for (Map.Entry<String, String> entry : SEARCH_EXPLANATION_FIELD_LABEL_MAP.entrySet()) {
            sanitized = sanitized.replace(entry.getKey(), entry.getValue());
        }
        sanitized = sanitized.replaceAll("\\s+", " ");
        return sanitized.trim();
    }

    private boolean containsInternalSearchField(String explanation) {
        if (StrUtil.isBlank(explanation)) {
            return false;
        }

        String normalized = explanation.toLowerCase(Locale.ROOT);
        if (normalized.contains("{") || normalized.contains("}") || normalized.contains("parsedquery")) {
            return true;
        }

        for (String field : SEARCH_EXPLANATION_FIELD_LABEL_MAP.keySet()) {
            if (normalized.contains(field.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String formatCompactAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        BigDecimal tenThousand = BigDecimal.valueOf(10_000);
        if (amount.abs().compareTo(tenThousand) >= 0) {
            BigDecimal wan = amount.divide(tenThousand, 1, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
            return wan.toPlainString() + "万";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    private BigDecimal parseJsonBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = StrUtil.trim(node.asText());
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseJsonInteger(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = StrUtil.trim(node.asText());
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseJsonBoolean(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = StrUtil.trim(node.asText());
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    private List<CustomerFieldFilterBO> parseJsonCustomerFieldFilters(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return Collections.emptyList();
        }
        List<CustomerFieldFilterBO> filters = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) {
                continue;
            }
            CustomerFieldFilterBO filter = new CustomerFieldFilterBO();
            filter.setFieldName(normalizeOptionalText(getJsonText(item, "fieldName")));
            filter.setFieldSource(normalizeFieldSource(getJsonText(item, "fieldSource")));
            filter.setOperator(normalizeEmptyFilterOperator(getJsonText(item, "operator")));
            if (StrUtil.isBlank(filter.getFieldName()) || StrUtil.isBlank(filter.getOperator())) {
                continue;
            }
            filters.add(filter);
        }
        return filters;
    }

    private Date parseJsonDate(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = StrUtil.trim(node.asText());
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return DateUtil.parse(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseJsonConfidence(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = StrUtil.trim(node.asText());
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            double value = Double.parseDouble(text);
            if (value > 1) {
                value = value / 100D;
            }
            return Math.max(0D, Math.min(1D, value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeStageCode(String stage) {
        String normalized = normalizeOptionalText(stage);
        if (normalized == null) {
            return null;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (STAGE_LABEL_MAP.containsKey(lower)) {
            return lower;
        }
        return LABEL_STAGE_MAP.getOrDefault(normalized, null);
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = normalizeOptionalText(sortBy);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "createTime", "create_time", "创建时间", "新增时间" -> "createTime";
            case "quotation", "报价", "报价金额", "预计成交金额" -> "quotation";
            case "lastContactTime", "last_contact_time", "最后联系", "最后跟进" -> "lastContactTime";
            case "nextFollowTime", "next_follow_time", "下次跟进" -> "nextFollowTime";
            case "contactCount", "contact_count", "联系人数", "联系人数量" -> "contactCount";
            default -> null;
        };
    }

    private String normalizeSortOrder(String sortOrder) {
        String normalized = normalizeOptionalText(sortOrder);
        if (normalized == null) {
            return null;
        }
        if ("asc".equalsIgnoreCase(normalized) || "升序".equals(normalized)) {
            return "asc";
        }
        if ("desc".equalsIgnoreCase(normalized) || "降序".equals(normalized)) {
            return "desc";
        }
        return null;
    }

    private String normalizeOptionalText(String value) {
        String normalized = StrUtil.trim(value);
        if (StrUtil.isBlank(normalized) || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String getJsonText(JsonNode root, String field) {
        if (root.has(field) && !root.get(field).isNull()) {
            return root.get(field).asText();
        }
        return null;
    }

    private List<String> getJsonStringList(JsonNode root, String field) {
        List<String> result = new ArrayList<>();
        if (root.has(field) && root.get(field).isArray()) {
            root.get(field).forEach(n -> result.add(n.asText()));
        }
        return result;
    }

    private CustomerAiParseVO buildFallbackCustomerResult() {
        CustomerAiParseVO vo = new CustomerAiParseVO();
        vo.setTags(List.of());
        vo.setKeyPoints(List.of());
        return vo;
    }
}
