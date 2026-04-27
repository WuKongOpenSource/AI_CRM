package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.auth.DataPermissionHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.entity.BO.*;
import com.kakarote.ai_crm.entity.PO.*;
import com.kakarote.ai_crm.entity.VO.*;
import com.kakarote.ai_crm.mapper.*;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.ICustomerService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.ITaskService;
import com.kakarote.ai_crm.utils.AiMediaUtil;
import com.kakarote.ai_crm.utils.UserUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 客户服务实现
 */
@Slf4j
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements ICustomerService {

    private static final String CUSTOMER_TABLE_NAME = "crm_customer";
    private static final String CUSTOMER_SEARCH_TEXT_COLUMN = "search_text";
    private static final Set<String> SEARCHABLE_CUSTOM_FIELD_TYPES = Set.of("text", "textarea", "select", "multiselect");
    private static final String AI_ANALYSIS_STATUS_PENDING = "pending";
    private static final String AI_ANALYSIS_STATUS_RUNNING = "running";
    private static final String AI_ANALYSIS_STATUS_SUCCESS = "success";
    private static final String AI_ANALYSIS_STATUS_FAILED = "failed";
    private static final Set<String> INLINE_EDITABLE_SYSTEM_FIELDS = Set.of(
            "companyName",
            "industry",
            "level",
            "source",
            "address",
            "website",
            "quotation",
            "nextFollowTime",
            "remark"
    );
    private static final Set<String> INLINE_EDITABLE_CONTACT_FIELDS = Set.of(
            "primaryContactName",
            "primaryContactPhone",
            "primaryContactEmail",
            "primaryContactPosition",
            "contactName",
            "contactPhone",
            "contactEmail",
            "contactPosition"
    );

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private CustomerTagMapper customerTagMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private FollowUpMapper followUpMapper;

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
    private CustomerLogoService customerLogoService;

    @Autowired
    private DataPermissionService dataPermissionService;

    @Autowired
    private IGlobalSearchIndexService globalSearchIndexService;

    @Autowired
    private ITaskService taskService;

    @Autowired
    private AiQuotaService aiQuotaService;

    @Autowired
    @Qualifier("customerAiAnalysisExecutor")
    private Executor customerAiAnalysisExecutor;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern WEBSITE_URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)[^\\s，。；;、)）\\]】>]+");

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
          "website": "公司官网或网站 URL（如：https://example.com；若输入包含 URL 请提取到该字段）",
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

    private static final String AI_CUSTOMER_REPORT_PROMPT = """
        你是一位资深 CRM 销售分析师。请基于下面的客户资料，生成客户 AI 分析报告，并严格返回 JSON：
        {
          "aiStatusDetection": "30-80字，概括客户当前状态、推进信号与主要风险",
          "aiInsight": "120-300字，输出客户需求判断、决策链判断、推进建议和风险提醒"
        }

        要求：
        1. 使用中文。
        2. aiStatusDetection 要简洁明确，适合在客户列表中直接浏览。
        3. aiInsight 要具体、可执行，适合在客户详情中长期保存。
        4. 只返回 JSON，不要返回 Markdown、代码块或额外说明。

        客户资料：
        %s
        """;

    private static final Set<String> AI_STATUS_DETECTION_OPTIONS = Set.of("高意向", "活跃状态", "需跟进", "休眠");
    private static final String AI_CUSTOMER_REPORT_PROMPT_V2 = """
        你是一位资深 CRM 销售分析师。请基于下面的客户资料，生成客户 AI 分析报告，并严格返回 JSON：
        {
          "aiStatusDetection": "只能是 高意向、活跃状态、需跟进、休眠 之一",
          "aiInsight": "40-70字，输出一句短摘要：阶段/沟通状态/需求/联系人 + 建议 + 风险提醒",
          "aiDeepInsight": "120-300字，输出3-5条深度分析要点，用\\n分隔，覆盖阶段、需求、决策链、风险，不要重复aiInsight",
          "aiNextStep": "40-100字，输出一个具体可执行的下一步行动建议"
        }

        要求：
        1. 使用中文。
        2. aiStatusDetection 只能返回以下四个值之一：高意向、活跃状态、需跟进、休眠。
        3. aiInsight 是列表和客户基本信息中的摘要，不要写长段落；参考格式：线索阶段，未沟通，需求不明，无联系人。建议尽快触达并完善信息，避免流失。
        4. aiDeepInsight 用于客户详情页，不需要像列表摘要一样精简；每条独立成句，便于前端分段展示。
        5. 只返回 JSON，不要返回 Markdown、代码块或额外说明。

        客户资料：
        %s
        """;

    private static final DateTimeFormatter AI_SEARCH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern DAYS_WITHOUT_CONTACT_PATTERN = Pattern.compile("(\\d{1,3})\\s*天(?:未跟进|未联系|未互动)");
    private static final Pattern RECENT_NEW_CUSTOMERS_PATTERN = Pattern.compile("(?:最近|近)(\\d{1,3})\\s*天新增");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("([ABC])\\s*(?:类|级)?客户", Pattern.CASE_INSENSITIVE);
    private static final String SEARCH_NUMBER_WITH_UNIT_PATTERN = "(\\d+(?:\\.\\d+)?)\\s*(万|w|W|千|k|K|亿)?\\s*(?:元|块|人民币)?";
    private static final Pattern EXPLICIT_ZERO_PATTERN = Pattern.compile("(?<!\\d)0+(?:\\.0+)?(?!\\d)|零");
    private static final Pattern QUOTATION_COMPARE_PATTERN = buildAmountComparePattern("报价(?:金额)?");
    private static final Pattern QUOTATION_RANGE_PATTERN = buildAmountRangePattern("报价(?:金额)?");
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
            "quotationMin": null,
            "quotationMax": null,
            "lastContactStart": "yyyy-MM-dd HH:mm:ss",
            "lastContactEnd": "yyyy-MM-dd HH:mm:ss",
            "includeNoLastContact": true,
            "nextFollowStart": "yyyy-MM-dd HH:mm:ss",
            "nextFollowEnd": "yyyy-MM-dd HH:mm:ss",
            "createTimeStart": "yyyy-MM-dd HH:mm:ss",
            "createTimeEnd": "yyyy-MM-dd HH:mm:ss",
            "contactCountMin": null,
            "contactCountMax": null,
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

    /**
     * 新增客户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addCustomer(CustomerAddBO customerAddBO) {
        // Create customer
        Date analysisRequestedAt = new Date();
        Long currentUserId = UserUtil.getUserId();
        Long currentTenantId = UserUtil.getTenantId();
        Customer customer = BeanUtil.copyProperties(customerAddBO, Customer.class);
        customer.setOwnerId(currentUserId);
        customer.setWebsite(customerLogoService.normalizeWebsite(customer.getWebsite()));
        customer.setLogo("");
        customer.setStatus(1);
        customer.setAiAnalysisStatus(AI_ANALYSIS_STATUS_PENDING);
        customer.setAiAnalysisRequestedAt(analysisRequestedAt);
        if (StrUtil.isEmpty(customer.getStage())) {
            customer.setStage("lead");
        }
        if (StrUtil.isBlank(customer.getLevel())) {
            customer.setLevel(null);
        }
        customFieldService.validateUniqueCustomFieldValues("customer", null,
                buildCustomerUniqueFieldValues(customer, customerAddBO.getCustomFields()));
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
            customFieldService.validateUniqueCustomFieldValues("contact", null,
                    buildContactUniqueFieldValues(contact, null));
            contactMapper.insert(contact);
            // 同步冗余字段
            syncContactCache(customer.getCustomerId());
        }

        // Save custom fields
        if (customerAddBO.getCustomFields() != null && !customerAddBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("customer", customer.getCustomerId(), customerAddBO.getCustomFields());
        }

        refreshCustomerSearchText(customer.getCustomerId());
        globalSearchIndexService.refreshCustomerIndex(customer.getCustomerId());
        scheduleCustomerAiAnalysis(customer.getCustomerId(),
                currentUserId,
                ObjectUtil.defaultIfNull(customer.getTenantId(), currentTenantId),
                analysisRequestedAt);

        return customer.getCustomerId();
    }

    /**
     * 更新客户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(CustomerUpdateBO customerUpdateBO) {
        Date analysisRequestedAt = new Date();
        Long currentUserId = UserUtil.getUserId();
        Long currentTenantId = UserUtil.getTenantId();
        Customer customer = getById(customerUpdateBO.getCustomerId());
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        boolean websiteChanged = !Objects.equals(customerUpdateBO.getWebsite(), customer.getWebsite());
        String previousLogo = customer.getLogo();
        BeanUtil.copyProperties(customerUpdateBO, customer, "customerId", "createUserId", "createTime", "customFields");
        String normalizedWebsite = customerLogoService.normalizeWebsite(customerUpdateBO.getWebsite());
        customer.setWebsite(normalizedWebsite);
        if (websiteChanged) {
            customer.setLogo("");
        }
        customer.setAiAnalysisStatus(AI_ANALYSIS_STATUS_PENDING);
        customer.setAiAnalysisRequestedAt(analysisRequestedAt);
        customFieldService.validateUniqueCustomFieldValues("customer", customer.getCustomerId(),
                buildCustomerUniqueFieldValues(customer, customerUpdateBO.getCustomFields()));
        updateById(customer);
        if (websiteChanged) {
            deleteCustomerLogoAfterCommit(previousLogo);
        }

        // 更新自定义字段
        if (customerUpdateBO.getCustomFields() != null && !customerUpdateBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("customer", customerUpdateBO.getCustomerId(), customerUpdateBO.getCustomFields());
        }

        refreshCustomerSearchText(customer.getCustomerId());
        globalSearchIndexService.refreshCustomerIndex(customer.getCustomerId());
        globalSearchIndexService.refreshCustomerRelatedIndexes(customer.getCustomerId());
        taskService.refreshValuePriorityByCustomerId(customer.getCustomerId());
        scheduleCustomerAiAnalysis(customer.getCustomerId(),
                currentUserId,
                ObjectUtil.defaultIfNull(customer.getTenantId(), currentTenantId),
                analysisRequestedAt);
    }

    /**
     * 更新客户字段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerDetailVO updateCustomerField(CustomerFieldUpdateBO fieldUpdateBO) {
        Long customerId = fieldUpdateBO.getCustomerId();
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Customer not found");
        }

        String fieldName = StrUtil.trim(fieldUpdateBO.getFieldName());
        String fieldSource = StrUtil.trimToEmpty(fieldUpdateBO.getFieldSource()).toLowerCase(Locale.ROOT);
        if (StrUtil.isBlank(fieldName)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Field name cannot be blank");
        }

        if ("contact".equals(fieldSource) || INLINE_EDITABLE_CONTACT_FIELDS.contains(fieldName)) {
            updatePrimaryContactField(customer, fieldName, fieldUpdateBO.getValue());
        } else if ("custom".equals(fieldSource) || isEnabledCustomCustomerField(fieldName)) {
            customFieldService.updateCustomFieldValue("customer", customerId, fieldName, normalizeBlankToNull(fieldUpdateBO.getValue()));
            refreshCustomerAfterInlineUpdate(customerId, true);
        } else {
            updateSystemCustomerField(customer, fieldName, fieldUpdateBO.getValue());
        }

        return getCustomerDetail(customerId);
    }

    /**
     * 删除客户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long customerId) {
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        removeById(customerId);
        deleteCustomerLogoAfterCommit(customer.getLogo());
        // Delete related contacts
        contactMapper.delete(new LambdaQueryWrapper<Contact>().eq(Contact::getCustomerId, customerId));
        // Delete related tags
        customerTagMapper.delete(new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getCustomerId, customerId));
        globalSearchIndexService.deleteByEntity("customer", customerId);
        globalSearchIndexService.deleteContactIndexesByCustomerId(customerId);
        globalSearchIndexService.refreshCustomerRelatedIndexes(customerId);
        taskService.refreshValuePriorityByCustomerId(customerId);
    }

    /**
     * 分页查询客户列表。
     */
    @Override
    public BasePage<CustomerListVO> queryPageList(CustomerQueryBO queryBO) {
        queryBO.setKeyword(normalizeSearchTextFragment(queryBO.getKeyword()));
        applyCustomerQueryScope(queryBO);
        if (queryBO.getAuthorizedOwnerIds() != null && queryBO.getAuthorizedOwnerIds().isEmpty()) {
            BasePage<CustomerListVO> emptyPage = new BasePage<>(queryBO.getPage(), queryBO.getLimit());
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }
        // 1. 获取启用的自定义字段，构建动态列列表
        List<CustomFieldVO> enabledFields = getEnabledCustomOnlyFields("customer");
        List<String> cfColumns = enabledFields.stream()
                .map(CustomFieldVO::getColumnName)
                .filter(col -> dynamicSchemaService.columnExists("crm_customer", col))
                .toList();
        Map<String, String> colToFieldName = enabledFields.stream()
                .filter(f -> cfColumns.contains(f.getColumnName()))
                .collect(Collectors.toMap(CustomFieldVO::getColumnName, CustomFieldVO::getFieldName));

        // 2. 单次查询：标准字段 + 自定义字段列一起查出
        BasePage<Map<String, Object>> rawPage = queryBO.parse();
        rawPage.setSearchCount(false);
        rawPage.setOptimizeCountSql(false);
        rawPage.setOptimizeJoinOfCountSql(false);
        Long total = baseMapper.queryPageListCount(queryBO);
        rawPage.setTotal(total == null ? 0L : total);
        if (rawPage.getTotal() <= 0) {
            BasePage<CustomerListVO> emptyPage = new BasePage<>(rawPage.getCurrent(), rawPage.getSize());
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }
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
        } catch (BusinessException e) {
            throw e;
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
     * 将 Map 行数据映射为 CustomerListVO（标准字段）
     */
    private CustomerListVO mapToCustomerListVO(Map<String, Object> row) {
        CustomerListVO vo = new CustomerListVO();
        vo.setCustomerId(toLong(row.get("customer_id")));
        vo.setCompanyName(toStr(row.get("company_name")));
        vo.setIndustry(toStr(row.get("industry")));
        vo.setStage(toStr(row.get("stage")));
        vo.setLevel(toStr(row.get("level")));
        vo.setSource(toStr(row.get("source")));
        vo.setWebsite(toStr(row.get("website")));
        vo.setLogo(toStr(row.get("logo")));
        vo.setLogoUrl(customerLogoService.resolveLogoUrl(vo.getLogo()));
        vo.setAddress(toStr(row.get("address")));
        vo.setQuotation(toBigDecimal(row.get("quotation")));
        vo.setLastContactTime(toDate(row.get("last_contact_time")));
        vo.setNextFollowTime(toDate(row.get("next_follow_time")));
        vo.setRemark(toStr(row.get("remark")));
        vo.setAiStatusDetection(toStr(row.get("ai_status_detection")));
        vo.setAiInsight(toStr(row.get("ai_insight")));
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

    /**
     * 转换为Long。
     */
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.valueOf(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    /**
     * 转换为STR。
     */
    private String toStr(Object val) {
        return val != null ? val.toString() : null;
    }

    /**
     * 转换为BIGDecimal。
     */
    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    /**
     * 转换为日期。
     */
    private Date toDate(Object val) {
        if (val == null) return null;
        if (val instanceof Date d) return d;
        if (val instanceof java.sql.Timestamp ts) return new Date(ts.getTime());
        return null;
    }

    /**
     * 转换为Int。
     */
    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.valueOf(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    /**
     * 标准化BlankTO空值。
     */
    private Object normalizeBlankToNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text && StrUtil.isBlank(text)) {
            return null;
        }
        return value;
    }

    /**
     * 标准化String值。
     */
    private String normalizeStringValue(Object value) {
        Object normalized = normalizeBlankToNull(value);
        return normalized == null ? null : normalized.toString().trim();
    }

    /**
     * 解析字段BIGDecimal。
     */
    private BigDecimal parseFieldBigDecimal(Object value) {
        Object normalized = normalizeBlankToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (normalized instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(normalized.toString().replace(",", ""));
        } catch (NumberFormatException exception) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Invalid number value");
        }
    }

    /**
     * 解析字段日期。
     */
    private Date parseFieldDate(Object value) {
        Object normalized = normalizeBlankToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized instanceof Date date) {
            return date;
        }
        if (normalized instanceof java.sql.Timestamp timestamp) {
            return new Date(timestamp.getTime());
        }
        try {
            return DateUtil.parse(normalized.toString());
        } catch (Exception exception) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Invalid date value");
        }
    }

    /**
     * 构建客户Unique字段值。
     */
    private Map<String, Object> buildCustomerUniqueFieldValues(Customer customer, Map<String, Object> customFields) {
        Map<String, Object> values = new HashMap<>();
        if (customer != null) {
            values.put("companyName", customer.getCompanyName());
            values.put("industry", customer.getIndustry());
            values.put("stage", customer.getStage());
            values.put("level", customer.getLevel());
            values.put("source", customer.getSource());
            values.put("website", customer.getWebsite());
            values.put("quotation", customer.getQuotation());
            values.put("address", customer.getAddress());
            values.put("nextFollowTime", customer.getNextFollowTime());
            values.put("remark", customer.getRemark());
        }
        if (customFields != null && !customFields.isEmpty()) {
            values.putAll(customFields);
        }
        return values;
    }

    /**
     * 构建联系人Unique字段值。
     */
    private Map<String, Object> buildContactUniqueFieldValues(Contact contact, Map<String, Object> customFields) {
        Map<String, Object> values = new HashMap<>();
        if (contact != null) {
            values.put("name", contact.getName());
            values.put("position", contact.getPosition());
            values.put("phone", contact.getPhone());
            values.put("email", contact.getEmail());
            values.put("wechat", contact.getWechat());
            values.put("isPrimary", contact.getIsPrimary());
            values.put("notes", contact.getNotes());
        }
        if (customFields != null && !customFields.isEmpty()) {
            values.putAll(customFields);
        }
        return values;
    }

    /**
     * 判断是否启用项自定义客户字段。
     */
    private boolean isEnabledCustomCustomerField(String fieldName) {
        return customFieldService.getEnabledFieldsByEntity("customer").stream()
                .anyMatch(field -> "custom".equalsIgnoreCase(field.getFieldSource())
                        && fieldName.equals(field.getFieldName()));
    }

    /**
     * 更新系统客户字段。
     */
    private void updateSystemCustomerField(Customer customer, String fieldName, Object value) {
        if (!INLINE_EDITABLE_SYSTEM_FIELDS.contains(fieldName)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Field is not editable");
        }

        if ("companyName".equals(fieldName) && StrUtil.isBlank(normalizeStringValue(value))) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Company name cannot be blank");
        }

        Long customerId = customer.getCustomerId();
        Date updateTime = new Date();
        LambdaUpdateWrapper<Customer> wrapper = Wrappers.lambdaUpdate(Customer.class)
                .eq(Customer::getCustomerId, customerId)
                .set(Customer::getUpdateTime, updateTime)
                .set(Customer::getUpdateUserId, UserUtil.getUserId());

        boolean websiteChanged = false;
        String previousLogo = customer.getLogo();
        Object uniqueValue;

        switch (fieldName) {
            case "companyName" -> {
                uniqueValue = normalizeStringValue(value);
                wrapper.set(Customer::getCompanyName, uniqueValue);
            }
            case "industry" -> {
                uniqueValue = normalizeStringValue(value);
                wrapper.set(Customer::getIndustry, uniqueValue);
            }
            case "level" -> {
                uniqueValue = normalizeStringValue(value);
                wrapper.set(Customer::getLevel, uniqueValue);
            }
            case "source" -> {
                uniqueValue = normalizeStringValue(value);
                wrapper.set(Customer::getSource, uniqueValue);
            }
            case "address" -> {
                uniqueValue = normalizeStringValue(value);
                wrapper.set(Customer::getAddress, uniqueValue);
            }
            case "website" -> {
                String normalizedWebsite = customerLogoService.normalizeWebsite(normalizeStringValue(value));
                uniqueValue = normalizedWebsite;
                websiteChanged = !Objects.equals(normalizedWebsite, customer.getWebsite());
                wrapper.set(Customer::getWebsite, normalizedWebsite);
                if (websiteChanged) {
                    wrapper.set(Customer::getLogo, "");
                }
            }
            case "quotation" -> {
                uniqueValue = parseFieldBigDecimal(value);
                wrapper.set(Customer::getQuotation, uniqueValue);
            }
            case "nextFollowTime" -> {
                uniqueValue = parseFieldDate(value);
                wrapper.set(Customer::getNextFollowTime, uniqueValue);
            }
            case "remark" -> {
                uniqueValue = normalizeStringValue(value);
                wrapper.set(Customer::getRemark, uniqueValue);
            }
            default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Field is not editable");
        }

        Map<String, Object> uniqueValues = new HashMap<>();
        uniqueValues.put(fieldName, uniqueValue);
        customFieldService.validateUniqueCustomFieldValues("customer", customerId, uniqueValues);
        update(wrapper);
        if (websiteChanged) {
            deleteCustomerLogoAfterCommit(previousLogo);
        }
        refreshCustomerAfterInlineUpdate(customerId, "quotation".equals(fieldName));
    }

    /**
     * 更新主联系人字段。
     */
    private void updatePrimaryContactField(Customer customer, String fieldName, Object value) {
        String normalizedFieldName = normalizePrimaryContactFieldName(fieldName);
        String textValue = normalizeStringValue(value);
        Contact contact = findPrimaryContactEntity(customer.getCustomerId());

        if (contact == null) {
            if (StrUtil.isBlank(textValue)) {
                return;
            }
            contact = new Contact();
            contact.setCustomerId(customer.getCustomerId());
            contact.setName("name".equals(normalizedFieldName)
                    ? textValue
                    : StrUtil.blankToDefault(customer.getPrimaryContactName(), "Primary Contact"));
            contact.setIsPrimary(1);
            contact.setStatus(1);
            applyPrimaryContactValue(contact, normalizedFieldName, textValue);
            Map<String, Object> uniqueValues = new HashMap<>();
            uniqueValues.put(normalizedFieldName, textValue);
            customFieldService.validateUniqueCustomFieldValues("contact", null, uniqueValues);
            contactMapper.insert(contact);
        } else {
            LambdaUpdateWrapper<Contact> wrapper = Wrappers.lambdaUpdate(Contact.class)
                    .eq(Contact::getContactId, contact.getContactId())
                    .set(Contact::getUpdateTime, new Date())
                    .set(Contact::getUpdateUserId, UserUtil.getUserId());
            switch (normalizedFieldName) {
                case "name" -> wrapper.set(Contact::getName, textValue);
                case "phone" -> wrapper.set(Contact::getPhone, textValue);
                case "email" -> wrapper.set(Contact::getEmail, textValue);
                case "position" -> wrapper.set(Contact::getPosition, textValue);
                default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Contact field is not editable");
            }
            Map<String, Object> uniqueValues = new HashMap<>();
            uniqueValues.put(normalizedFieldName, textValue);
            customFieldService.validateUniqueCustomFieldValues("contact", contact.getContactId(), uniqueValues);
            contactMapper.update(null, wrapper);
        }

        syncContactCache(customer.getCustomerId());
        refreshCustomerAfterInlineUpdate(customer.getCustomerId(), false);
        if (contact.getContactId() != null) {
            globalSearchIndexService.refreshContactIndex(contact.getContactId());
        }
    }

    /**
     * 查找主联系人Entity。
     */
    private Contact findPrimaryContactEntity(Long customerId) {
        List<Contact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getCustomerId, customerId)
                        .eq(Contact::getStatus, 1)
                        .orderByDesc(Contact::getIsPrimary)
                        .orderByAsc(Contact::getCreateTime)
                        .orderByAsc(Contact::getContactId)
                        .last("LIMIT 1")
        );
        return contacts.isEmpty() ? null : contacts.get(0);
    }

    /**
     * 标准化主联系人字段名称。
     */
    private String normalizePrimaryContactFieldName(String fieldName) {
        return switch (fieldName) {
            case "primaryContactName", "contactName" -> "name";
            case "primaryContactPhone", "contactPhone" -> "phone";
            case "primaryContactEmail", "contactEmail" -> "email";
            case "primaryContactPosition", "contactPosition" -> "position";
            default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Contact field is not editable");
        };
    }

    /**
     * 处理applyPrimaryContactValue方法逻辑。
     */
    private void applyPrimaryContactValue(Contact contact, String normalizedFieldName, String value) {
        switch (normalizedFieldName) {
            case "name" -> contact.setName(value);
            case "phone" -> contact.setPhone(value);
            case "email" -> contact.setEmail(value);
            case "position" -> contact.setPosition(value);
            default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Contact field is not editable");
        }
    }

    /**
     * 刷新客户AfterInlineUpdate。
     */
    private void refreshCustomerAfterInlineUpdate(Long customerId, boolean refreshTaskPriority) {
        refreshCustomerSearchText(customerId);
        globalSearchIndexService.refreshCustomerIndex(customerId);
        globalSearchIndexService.refreshCustomerRelatedIndexes(customerId);
        if (refreshTaskPriority) {
            taskService.refreshValuePriorityByCustomerId(customerId);
        }
    }

    /**
     * 查找客户按精确公司名称。
     */
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

    /**
     * 查找客户按精确公司名称忽略数据权限。
     */
    @Override
    public List<Customer> findCustomersByExactCompanyNameIgnoreDataPermission(String companyName) {
        String normalizedCompanyName = StrUtil.trim(companyName);
        if (StrUtil.isBlank(normalizedCompanyName)) {
            return Collections.emptyList();
        }
        return baseMapper.selectByExactCompanyNameIgnoreDataPermission(normalizedCompanyName);
    }

    /**
     * 查找客户按公司名称模糊忽略数据权限。
     */
    @Override
    public List<Customer> findCustomersByCompanyNameLikeIgnoreDataPermission(String keyword, int limit) {
        String normalizedKeyword = StrUtil.trim(keyword);
        if (StrUtil.isBlank(normalizedKeyword) || normalizedKeyword.length() < 2) {
            return Collections.emptyList();
        }
        int actualLimit = Math.max(1, Math.min(limit, 50));
        return baseMapper.selectByCompanyNameLikeIgnoreDataPermission(normalizedKeyword, actualLimit);
    }

    /**
     * 查找客户按ID忽略数据权限。
     */
    @Override
    public Customer findCustomerByIdIgnoreDataPermission(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return baseMapper.selectByIdIgnoreDataPermission(customerId);
    }

    /**
     * 获取客户详情。
     */
    @Override
    public CustomerDetailVO getCustomerDetail(Long customerId) {
        CustomerDetailVO detail = baseMapper.getCustomerById(customerId);
        if (detail != null) {
            detail.setLogoUrl(customerLogoService.resolveLogoUrl(detail.getLogo()));
        }
        if (ObjectUtil.isNull(detail)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }


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

    /**
     * 生成AI报告。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerAiReportVO generateAiReport(Long customerId) {
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "瀹㈡埛涓嶅瓨鍦?");
        }

        Date analysisRequestedAt = new Date();
        updateCustomerAiAnalysisState(customerId, null, AI_ANALYSIS_STATUS_RUNNING, analysisRequestedAt);
        try {
            return generateAiReportInternal(customerId, analysisRequestedAt, true);
        } catch (Exception exception) {
            updateCustomerAiAnalysisState(customerId, analysisRequestedAt, AI_ANALYSIS_STATUS_FAILED, null);
            throw exception;
        }
    }

    /**
     * 生成AI报告内部。
     */
    private CustomerAiReportVO generateAiReportInternal(Long customerId,
                                                        Date expectedRequestedAt,
                                                        boolean persistCompletedStatus) {
        try {
            customerLogoService.populateCustomerLogo(customerId, expectedRequestedAt);
        } catch (Exception exception) {
            log.warn("populate customer logo failed, customerId={}", customerId, exception);
        }
        CustomerDetailVO detail = getCustomerDetail(customerId);
        List<FollowUpVO> recentFollowUps = followUpMapper.getRecentByCustomerId(customerId, 5);

        CustomerAiReportVO report;
        String prompt = String.format(AI_CUSTOMER_REPORT_PROMPT_V2, buildCustomerAiReportContext(detail, recentFollowUps));
        try {
            aiQuotaService.ensureQuotaAvailable("customer_report", null, null, prompt);
            var chatResponse = chatClientProvider.getChatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();
            String response = chatResponse.getResult().getOutput().getText();
            aiQuotaService.consumeResolvedTokens(
                "customer_report",
                aiQuotaService.resolveTokenUsage(chatResponse, null, null, prompt, response)
            );
            report = parseCustomerAiReportResponse(response, detail, recentFollowUps);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("generate customer ai report failed, customerId={}", customerId, exception);
            report = buildFallbackCustomerAiReportV2(detail, recentFollowUps);
        }

        report.setCustomerId(customerId);
        String aiParseSnapshot = serializeCustomerAiParseSnapshot(buildCustomerAiParseSnapshot(detail, recentFollowUps, report));

        LambdaUpdateWrapper<Customer> updateWrapper = Wrappers.lambdaUpdate(Customer.class)
                .eq(Customer::getCustomerId, customerId)
                .set(Customer::getAiStatusDetection, StrUtil.nullToEmpty(report.getAiStatusDetection()))
                .set(Customer::getAiInsight, StrUtil.nullToEmpty(report.getAiInsight()));
        if (expectedRequestedAt != null) {
            updateWrapper.eq(Customer::getAiAnalysisRequestedAt, expectedRequestedAt);
        }
        if (StrUtil.isNotBlank(aiParseSnapshot)) {
            updateWrapper.set(Customer::getAiParseSnapshot, aiParseSnapshot);
        }
        if (persistCompletedStatus) {
            updateWrapper.set(Customer::getAiAnalysisStatus, AI_ANALYSIS_STATUS_SUCCESS);
        }
        int updated = baseMapper.update(null, updateWrapper);
        if (updated <= 0) {
            log.info("skip stale customer ai analysis result, customerId={}", customerId);
            return report;
        }

        refreshCustomerSearchText(customerId);
        globalSearchIndexService.refreshCustomerIndex(customerId);
        taskService.refreshValuePriorityByCustomerId(customerId);
        return report;
    }

    /**
     * 删除客户LogoAfterCommit。
     */
    private void deleteCustomerLogoAfterCommit(String logo) {
        String normalizedLogo = StrUtil.trimToNull(logo);
        if (normalizedLogo == null) {
            return;
        }

        Runnable deleteTask = () -> customerLogoService.deleteStoredLogoQuietly(normalizedLogo);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /**
                 * 处理afterCommit方法逻辑。
                 */
                @Override
                public void afterCommit() {
                    deleteTask.run();
                }
            });
            return;
        }

        deleteTask.run();
    }

    /**
     * 处理scheduleCustomerAiAnalysis方法逻辑。
     */
    private void scheduleCustomerAiAnalysis(Long customerId, Long userId, Long tenantId, Date analysisRequestedAt) {
        if (customerId == null || userId == null || analysisRequestedAt == null) {
            return;
        }

        Runnable triggerTask = () -> {
            try {
                customerAiAnalysisExecutor.execute(() -> runCustomerAiAnalysisAsync(customerId, userId, tenantId, analysisRequestedAt));
            } catch (Exception exception) {
                log.error("dispatch customer ai analysis failed, customerId={}", customerId, exception);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /**
                 * 处理afterCommit方法逻辑。
                 */
                @Override
                public void afterCommit() {
                    triggerTask.run();
                }
            });
            return;
        }

        triggerTask.run();
    }

    /**
     * 处理runCustomerAiAnalysisAsync方法逻辑。
     */
    private void runCustomerAiAnalysisAsync(Long customerId, Long userId, Long tenantId, Date analysisRequestedAt) {
        if (tenantId == null) {
            log.warn("skip customer ai analysis because tenant context is missing, customerId={}", customerId);
            return;
        }

        bindCustomerAiAnalysisContext(userId, tenantId);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (!updateCustomerAiAnalysisState(customerId, analysisRequestedAt, AI_ANALYSIS_STATUS_RUNNING, null)) {
                    log.info("skip stale customer ai analysis before execution, customerId={}", customerId);
                    return;
                }
                try {
                    generateAiReportInternal(customerId, analysisRequestedAt, true);
                } catch (Exception exception) {
                    updateCustomerAiAnalysisState(customerId, analysisRequestedAt, AI_ANALYSIS_STATUS_FAILED, null);
                    log.error("auto generate customer ai report failed, customerId={}", customerId, exception);
                }
            });
        } finally {
            clearCustomerAiAnalysisContext();
        }
    }

    /**
     * 绑定客户AI分析上下文。
     */
    private void bindCustomerAiAnalysisContext(Long userId, Long tenantId) {
        DataPermissionHolder.clear();
        AiContextHolder.bindThreadContext(userId, tenantId);
    }

    /**
     * 清理客户AI分析上下文。
     */
    private void clearCustomerAiAnalysisContext() {
        DataPermissionHolder.clear();
        AiContextHolder.clearThreadContext();
    }

    /**
     * 更新客户AI分析状态。
     */
    private boolean updateCustomerAiAnalysisState(Long customerId,
                                                  Date expectedRequestedAt,
                                                  String aiAnalysisStatus,
                                                  Date nextRequestedAt) {
        LambdaUpdateWrapper<Customer> updateWrapper = Wrappers.lambdaUpdate(Customer.class)
                .eq(Customer::getCustomerId, customerId);
        if (expectedRequestedAt != null) {
            updateWrapper.eq(Customer::getAiAnalysisRequestedAt, expectedRequestedAt);
        }
        if (StrUtil.isNotBlank(aiAnalysisStatus)) {
            updateWrapper.set(Customer::getAiAnalysisStatus, aiAnalysisStatus);
        }
        if (nextRequestedAt != null) {
            updateWrapper.set(Customer::getAiAnalysisRequestedAt, nextRequestedAt);
        }
        return baseMapper.update(null, updateWrapper) > 0;
    }

    /**
     * 处理serializeCustomerAiParseSnapshot方法逻辑。
     */
    private String serializeCustomerAiParseSnapshot(CustomerAiParseVO snapshot) {
        if (isEmptyCustomerAiParseResult(snapshot)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            log.warn("serialize customer ai parse snapshot failed", exception);
            return null;
        }
    }

    /**
     * 构建客户AI解析快照。
     */
    private CustomerAiParseVO buildCustomerAiParseSnapshot(CustomerDetailVO detail,
                                                           List<FollowUpVO> recentFollowUps,
                                                           CustomerAiReportVO report) {
        CustomerAiParseVO snapshot = new CustomerAiParseVO();
        ContactVO primaryContact = findPrimaryContact(detail.getContacts());

        snapshot.setCompanyName(detail.getCompanyName());
        snapshot.setIndustry(detail.getIndustry());
        snapshot.setLevel(detail.getLevel());
        snapshot.setStage(detail.getStage());
        snapshot.setSource(detail.getSource());
        snapshot.setWebsite(detail.getWebsite());
        snapshot.setRemark(detail.getRemark());

        if (primaryContact != null) {
            snapshot.setContactName(primaryContact.getName());
            snapshot.setContactPhone(primaryContact.getPhone());
            snapshot.setContactEmail(primaryContact.getEmail());
            snapshot.setContactPosition(primaryContact.getPosition());
        }

        snapshot.setScore(inferCustomerAiParseScore(detail, recentFollowUps, report.getAiStatusDetection()));
        snapshot.setTags(buildCustomerAiParseTags(detail, report.getAiStatusDetection()));
        snapshot.setSummary(buildCustomerAiParseSummary(detail, recentFollowUps, report));
        snapshot.setNextStep(StrUtil.blankToDefault(report.getAiNextStep(), buildCustomerAiParseNextStep(detail, report.getAiStatusDetection())));
        snapshot.setKeyPoints(buildCustomerAiParseKeyPoints(detail, primaryContact, recentFollowUps));
        return snapshot;
    }

    /**
     * 查找主联系人。
     */
    private ContactVO findPrimaryContact(List<ContactVO> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return null;
        }
        return contacts.stream()
                .filter(contact -> contact.getIsPrimary() != null && contact.getIsPrimary() == 1)
                .findFirst()
                .orElse(contacts.get(0));
    }

    /**
     * 处理inferCustomerAiParseScore方法逻辑。
     */
    private Integer inferCustomerAiParseScore(CustomerDetailVO detail,
                                              List<FollowUpVO> recentFollowUps,
                                              String aiStatus) {
        int score = 45;

        if ("A".equalsIgnoreCase(detail.getLevel())) {
            score += 20;
        } else if ("B".equalsIgnoreCase(detail.getLevel())) {
            score += 12;
        } else if ("C".equalsIgnoreCase(detail.getLevel())) {
            score += 6;
        }

        switch (StrUtil.blankToDefault(detail.getStage(), "")) {
            case "qualified" -> score += 8;
            case "proposal" -> score += 12;
            case "negotiation" -> score += 16;
            case "closed" -> score += 18;
            case "lost" -> score -= 20;
            default -> score += 3;
        }

        if (detail.getQuotation() != null) {
            score += 6;
        }
        if (detail.getContacts() != null && !detail.getContacts().isEmpty()) {
            score += 5;
        }

        FollowUpVO latestFollowUp = recentFollowUps == null || recentFollowUps.isEmpty() ? null : recentFollowUps.get(0);
        if (latestFollowUp != null && latestFollowUp.getFollowTime() != null && getDaysSinceNow(latestFollowUp.getFollowTime()) <= 7) {
            score += 8;
        }
        if (detail.getNextFollowTime() != null && !detail.getNextFollowTime().before(new Date())) {
            score += 5;
        }

        switch (StrUtil.blankToDefault(aiStatus, "")) {
            case "高意向" -> score += 10;
            case "活跃状态" -> score += 6;
            case "需跟进" -> score += 2;
            case "休眠" -> score -= 15;
            default -> {
            }
        }

        return Math.max(20, Math.min(score, 98));
    }

    /**
     * 构建客户AI解析标签。
     */
    private List<String> buildCustomerAiParseTags(CustomerDetailVO detail, String aiStatus) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (detail.getTags() != null) {
            detail.getTags().stream()
                    .map(CustomerTag::getTagName)
                    .filter(StrUtil::isNotBlank)
                    .forEach(tags::add);
        }
        if (StrUtil.isNotBlank(detail.getIndustry())) {
            tags.add(detail.getIndustry());
        }
        if (StrUtil.isNotBlank(aiStatus)) {
            tags.add(aiStatus);
        }
        if (StrUtil.isNotBlank(detail.getLevel())) {
            tags.add(getLevelDisplayLabel(detail.getLevel()) + "客户");
        }
        if (StrUtil.isNotBlank(detail.getStage())) {
            tags.add(getStageLabel(detail.getStage()));
        }
        return tags.stream().filter(StrUtil::isNotBlank).limit(5).collect(Collectors.toList());
    }

    /**
     * 构建客户AI解析摘要。
     */
    private String buildCustomerAiParseSummary(CustomerDetailVO detail,
                                               List<FollowUpVO> recentFollowUps,
                                               CustomerAiReportVO report) {
        if (StrUtil.isNotBlank(report.getAiDeepInsight())) {
            return normalizeAiDeepInsightText(report.getAiDeepInsight());
        }
        return buildDeepAiInsight(detail, recentFollowUps, report.getAiStatusDetection());
    }

    /**
     * 构建客户AI解析下次步骤。
     */
    private String buildCustomerAiParseNextStep(CustomerDetailVO detail, String aiStatus) {
        if (detail.getNextFollowTime() != null && !detail.getNextFollowTime().before(new Date())) {
            return "优先围绕已安排的下次跟进时间推进沟通，确认客户反馈、决策节点和下一步动作。";
        }
        return switch (StrUtil.blankToDefault(aiStatus, "")) {
            case "高意向" -> "优先推进预算确认、方案收口和关键决策人同步，尽快约定下一次商务沟通。";
            case "活跃状态" -> "保持当前沟通频率，继续围绕需求澄清、方案演示和关键角色对齐推进。";
            case "休眠" -> "先确认客户沉默原因，再设计重新激活的话术或触达动作，寻找新的沟通窗口。";
            default -> "尽快发起一次明确触达，确认客户当前反馈、阻塞点和下一次跟进时间。";
        };
    }

    /**
     * 构建客户AI解析键要点。
     */
    private List<String> buildCustomerAiParseKeyPoints(CustomerDetailVO detail,
                                                       ContactVO primaryContact,
                                                       List<FollowUpVO> recentFollowUps) {
        List<String> keyPoints = new ArrayList<>();

        if (primaryContact != null && StrUtil.isNotBlank(primaryContact.getName())) {
            StringBuilder contactPoint = new StringBuilder("关键联系人：" + primaryContact.getName());
            if (StrUtil.isNotBlank(primaryContact.getPosition())) {
                contactPoint.append(" / ").append(primaryContact.getPosition());
            }
            if (StrUtil.isNotBlank(primaryContact.getPhone())) {
                contactPoint.append(" / ").append(primaryContact.getPhone());
            }
            keyPoints.add(contactPoint.toString());
        }

        FollowUpVO latestFollowUp = recentFollowUps == null || recentFollowUps.isEmpty() ? null : recentFollowUps.get(0);
        if (latestFollowUp != null && StrUtil.isNotBlank(latestFollowUp.getContent())) {
            keyPoints.add("最近跟进：" + truncateText(latestFollowUp.getContent(), 60));
        } else if (StrUtil.isNotBlank(detail.getRemark())) {
            keyPoints.add("客户备注：" + truncateText(detail.getRemark(), 60));
        }

        if (detail.getNextFollowTime() != null) {
            keyPoints.add("下次跟进：" + DateUtil.formatDateTime(detail.getNextFollowTime()));
        }

        if (detail.getQuotation() != null) {
            keyPoints.add("预计成交金额：" + formatNullableAmount(detail.getQuotation()));
        }

        if (keyPoints.isEmpty()) {
            keyPoints.add("当前阶段：" + getStageLabel(detail.getStage()));
        }

        return keyPoints.stream().filter(StrUtil::isNotBlank).limit(4).collect(Collectors.toList());
    }

    /**
     * 构建客户AI报告上下文。
     */
    private String buildCustomerAiReportContext(CustomerDetailVO detail, List<FollowUpVO> recentFollowUps) {
        StringBuilder builder = new StringBuilder();
        builder.append("客户ID: ").append(detail.getCustomerId()).append('\n');
        builder.append("公司名称: ").append(StrUtil.blankToDefault(detail.getCompanyName(), "未知")).append('\n');
        builder.append("行业: ").append(StrUtil.blankToDefault(detail.getIndustry(), "未填写")).append('\n');
        builder.append("阶段: ").append(getStageLabel(detail.getStage())).append('\n');
        builder.append("客户级别: ").append(getLevelDisplayLabel(detail.getLevel())).append('\n');
        builder.append("来源: ").append(StrUtil.blankToDefault(detail.getSource(), "未填写")).append('\n');
        builder.append("负责人: ").append(StrUtil.blankToDefault(detail.getOwnerName(), "未分配")).append('\n');
        builder.append("预计成交金额: ").append(formatNullableAmount(detail.getQuotation())).append('\n');
        builder.append("最后联系时间: ").append(formatNullableDateTime(detail.getLastContactTime())).append('\n');
        builder.append("下次跟进时间: ").append(formatNullableDateTime(detail.getNextFollowTime())).append('\n');
        builder.append("备注: ").append(StrUtil.blankToDefault(detail.getRemark(), "无")).append('\n');
        builder.append("标签: ").append(formatTagNames(detail.getTags())).append('\n');
        builder.append("联系人: ").append(formatContactSummary(detail.getContacts())).append('\n');
        builder.append("相关任务: ").append(formatTaskSummary(detail.getTasks())).append('\n');
        builder.append("最近跟进: ").append(formatFollowUpSummary(recentFollowUps)).append('\n');

        if (detail.getCustomFields() != null && !detail.getCustomFields().isEmpty()) {
            builder.append("自定义字段: ").append(formatCustomFieldSummary(detail.getCustomFields())).append('\n');
        }
        if (StrUtil.isNotBlank(detail.getAiStatusDetection())) {
            builder.append("历史AI状态探测: ").append(detail.getAiStatusDetection()).append('\n');
        }
        if (StrUtil.isNotBlank(detail.getAiInsight())) {
            builder.append("历史AI洞察: ").append(detail.getAiInsight()).append('\n');
        }
        return builder.toString();
    }

    /**
     * 解析客户AI报告响应。
     */
    private CustomerAiReportVO parseCustomerAiReportResponse(String response,
                                                             CustomerDetailVO detail,
                                                             List<FollowUpVO> recentFollowUps) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonPayload(response));
            CustomerAiReportVO report = new CustomerAiReportVO();
            report.setAiStatusDetection(normalizeAiStatusDetection(getJsonText(root, "aiStatusDetection"), detail, recentFollowUps));
            report.setAiInsight(normalizeAiReportText(getJsonText(root, "aiInsight")));
            report.setAiDeepInsight(normalizeAiDeepInsightText(getJsonText(root, "aiDeepInsight")));
            report.setAiNextStep(normalizeAiDeepInsightText(getJsonText(root, "aiNextStep")));
            if (StrUtil.isAllBlank(report.getAiStatusDetection(), report.getAiInsight(), report.getAiDeepInsight(), report.getAiNextStep())) {
                return buildFallbackCustomerAiReportV2(detail, recentFollowUps);
            }
            if (StrUtil.isBlank(report.getAiStatusDetection())
                    || StrUtil.isBlank(report.getAiInsight())
                    || StrUtil.isBlank(report.getAiDeepInsight())
                    || StrUtil.isBlank(report.getAiNextStep())) {
                CustomerAiReportVO fallback = buildFallbackCustomerAiReportV2(detail, recentFollowUps);
                if (StrUtil.isBlank(report.getAiStatusDetection())) {
                    report.setAiStatusDetection(fallback.getAiStatusDetection());
                }
                if (StrUtil.isBlank(report.getAiInsight())) {
                    report.setAiInsight(fallback.getAiInsight());
                }
                if (StrUtil.isBlank(report.getAiDeepInsight())) {
                    report.setAiDeepInsight(fallback.getAiDeepInsight());
                }
                if (StrUtil.isBlank(report.getAiNextStep())) {
                    report.setAiNextStep(fallback.getAiNextStep());
                }
            }
            return report;
        } catch (Exception exception) {
            log.warn("parse customer ai report failed: {}", exception.getMessage());
            return buildFallbackCustomerAiReportV2(detail, recentFollowUps);
        }
    }

    /**
     * 构建兜底客户AI报告。
     */
    private CustomerAiReportVO buildFallbackCustomerAiReport(CustomerDetailVO detail, List<FollowUpVO> recentFollowUps) {
        CustomerAiReportVO report = new CustomerAiReportVO();
        String stageLabel = getStageLabel(detail.getStage());
        String levelLabel = getLevelDisplayLabel(detail.getLevel());
        FollowUpVO latestFollowUp = recentFollowUps == null || recentFollowUps.isEmpty() ? null : recentFollowUps.get(0);

        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("当前处于").append(stageLabel).append("阶段");
        if (StrUtil.isNotBlank(levelLabel)) {
            statusBuilder.append("，客户等级").append(levelLabel);
        }
        if (latestFollowUp != null && latestFollowUp.getFollowTime() != null) {
            statusBuilder.append("，最近跟进于").append(DateUtil.formatDateTime(latestFollowUp.getFollowTime()));
        } else {
            statusBuilder.append("，近期缺少有效跟进记录");
        }
        if (detail.getNextFollowTime() != null) {
            statusBuilder.append("，已安排下次跟进");
        } else {
            statusBuilder.append("，下次跟进时间待明确");
        }
        report.setAiStatusDetection(statusBuilder.toString());

        List<String> insightParts = new ArrayList<>();
        insightParts.add("客户行业为" + StrUtil.blankToDefault(detail.getIndustry(), "未明确") + "，当前处于" + stageLabel + "阶段。");
        if (detail.getContacts() != null && !detail.getContacts().isEmpty()) {
            ContactVO primaryContact = detail.getContacts().stream()
                    .filter(contact -> contact.getIsPrimary() != null && contact.getIsPrimary() == 1)
                    .findFirst()
                    .orElse(detail.getContacts().get(0));
            insightParts.add("当前主要联系人为" + StrUtil.blankToDefault(primaryContact.getName(), "未明确")
                    + "，建议围绕其职责进一步确认需求与决策节奏。");
        } else {
            insightParts.add("当前联系人信息较少，建议优先补齐关键决策人和业务使用方。");
        }
        if (latestFollowUp != null && StrUtil.isNotBlank(latestFollowUp.getContent())) {
            insightParts.add("最近一次跟进显示：" + truncateText(latestFollowUp.getContent(), 70) + "。");
        } else if (StrUtil.isNotBlank(detail.getRemark())) {
            insightParts.add("现有备注显示：" + truncateText(detail.getRemark(), 70) + "。");
        }
        if (detail.getNextFollowTime() != null) {
            insightParts.add("建议围绕已排定的下次跟进节点推进方案确认、预算校验和成交阻碍排查。");
        } else {
            insightParts.add("建议尽快明确下一次跟进动作和时间，避免推进节奏中断。");
        }
        report.setAiInsight(String.join("", insightParts));
        return report;
    }

    /**
     * 构建兜底客户AI报告V2。
     */
    private CustomerAiReportVO buildFallbackCustomerAiReportV2(CustomerDetailVO detail, List<FollowUpVO> recentFollowUps) {
        CustomerAiReportVO report = new CustomerAiReportVO();
        String aiStatus = inferAiStatusDetection(detail, recentFollowUps);

        report.setAiStatusDetection(aiStatus);
        report.setAiInsight(buildCompactAiInsight(detail, recentFollowUps, aiStatus));
        report.setAiDeepInsight(buildDeepAiInsight(detail, recentFollowUps, aiStatus));
        report.setAiNextStep(buildCustomerAiParseNextStep(detail, aiStatus));
        return report;
    }

    /**
     * 构建DeepAIInsight。
     */
    private String buildDeepAiInsight(CustomerDetailVO detail, List<FollowUpVO> recentFollowUps, String aiStatus) {
        ContactVO primaryContact = findPrimaryContact(detail.getContacts());
        FollowUpVO latestFollowUp = recentFollowUps == null || recentFollowUps.isEmpty() ? null : recentFollowUps.get(0);
        List<String> points = new ArrayList<>();

        StringBuilder stagePoint = new StringBuilder();
        stagePoint.append("客户当前处于").append(getStageLabel(detail.getStage())).append("阶段");
        if (StrUtil.isNotBlank(detail.getIndustry())) {
            stagePoint.append("，所属行业为").append(detail.getIndustry());
        }
        if (detail.getQuotation() != null) {
            stagePoint.append("，已有预计成交金额").append(detail.getQuotation().stripTrailingZeros().toPlainString());
        } else {
            stagePoint.append("，预算和成交金额仍需进一步确认");
        }
        stagePoint.append("。");
        points.add(stagePoint.toString());

        if (primaryContact != null && StrUtil.isNotBlank(primaryContact.getName())) {
            StringBuilder contactPoint = new StringBuilder("关键联系人已记录为").append(primaryContact.getName());
            if (StrUtil.isNotBlank(primaryContact.getPosition())) {
                contactPoint.append("（").append(primaryContact.getPosition()).append("）");
            }
            contactPoint.append("，建议继续补充其决策权限、业务痛点和内部协同角色。");
            points.add(contactPoint.toString());
        } else {
            points.add("决策链信息尚未建立，需优先补齐关键联系人、使用部门和最终拍板人，避免后续推进缺少明确对象。");
        }

        if (latestFollowUp != null && StrUtil.isNotBlank(latestFollowUp.getContent())) {
            points.add("最近跟进记录显示：" + truncateText(latestFollowUp.getContent(), 80) + "，后续应围绕客户反馈继续验证需求真实性和紧急程度。");
        } else if (StrUtil.isNotBlank(detail.getRemark())) {
            points.add("现有备注显示：" + truncateText(detail.getRemark(), 80) + "，仍需通过下一次沟通补齐可执行的推进依据。");
        } else {
            points.add("当前缺少有效跟进记录，需求、预算、时间表和竞争情况都不够清晰，需要尽快通过一次结构化沟通补齐。");
        }

        String riskPoint = switch (StrUtil.blankToDefault(aiStatus, "")) {
            case "高意向" -> "风险点在于成交条件未及时收口，若预算、方案和关键决策人不同步，可能错过最佳推进窗口。";
            case "活跃状态" -> "风险点在于沟通热度可能回落，需要把持续互动转化为明确的下一步节点和客户侧承诺。";
            case "休眠" -> "风险点在于客户已进入低响应状态，需要先判断沉默原因，再决定重新激活或阶段性搁置。";
            default -> "风险点在于跟进节奏和客户反馈不够明确，若持续缺少下一步安排，商机容易被动降温。";
        };
        points.add(riskPoint);

        return points.stream()
                .filter(StrUtil::isNotBlank)
                .map(point -> "- " + point)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 构建CompactAIInsight。
     */
    private String buildCompactAiInsight(CustomerDetailVO detail, List<FollowUpVO> recentFollowUps, String aiStatus) {
        FollowUpVO latestFollowUp = recentFollowUps == null || recentFollowUps.isEmpty() ? null : recentFollowUps.get(0);
        boolean hasContact = detail.getContacts() != null && !detail.getContacts().isEmpty();
        boolean demandUnknown = detail.getQuotation() == null
                && (latestFollowUp == null || StrUtil.isBlank(latestFollowUp.getContent()))
                && StrUtil.isBlank(detail.getRemark());

        String summary = buildStagePhrase(detail.getStage()) + "，"
                + buildCommunicationPhrase(detail, latestFollowUp) + "，"
                + buildDemandPhrase(detail, latestFollowUp) + "，"
                + (hasContact ? "联系人已明确" : "无联系人") + "。"
                + buildCompactAiInsightSuggestion(aiStatus, hasContact, demandUnknown);
        return compactAiReportText(summary);
    }

    /**
     * 构建阶段Phrase。
     */
    private String buildStagePhrase(String stage) {
        String stageLabel = getStageLabel(stage);
        return stageLabel.endsWith("阶段") ? stageLabel : stageLabel + "阶段";
    }

    /**
     * 构建CommunicationPhrase。
     */
    private String buildCommunicationPhrase(CustomerDetailVO detail, FollowUpVO latestFollowUp) {
        long daysSinceLastContact = getDaysSinceNow(detail.getLastContactTime());
        long daysSinceLatestFollowUp = latestFollowUp != null && latestFollowUp.getFollowTime() != null
                ? getDaysSinceNow(latestFollowUp.getFollowTime())
                : Long.MAX_VALUE;
        long daysSinceCommunication = Math.min(daysSinceLastContact, daysSinceLatestFollowUp);
        if (daysSinceCommunication <= 7) {
            return "近期已沟通";
        }
        if (daysSinceCommunication <= 30 || latestFollowUp != null) {
            return "已沟通";
        }
        if (daysSinceCommunication < Long.MAX_VALUE) {
            return "沟通间隔较长";
        }
        return "未沟通";
    }

    /**
     * 构建DemandPhrase。
     */
    private String buildDemandPhrase(CustomerDetailVO detail, FollowUpVO latestFollowUp) {
        if (detail.getQuotation() != null) {
            return "预算初步明确";
        }
        if ((latestFollowUp != null && StrUtil.isNotBlank(latestFollowUp.getContent()))
                || StrUtil.isNotBlank(detail.getRemark())) {
            return "需求待确认";
        }
        return "需求不明";
    }

    /**
     * 构建CompactAIInsightSuggestion。
     */
    private String buildCompactAiInsightSuggestion(String aiStatus, boolean hasContact, boolean demandUnknown) {
        if (!hasContact || demandUnknown) {
            return "建议尽快触达并完善信息，避免流失。";
        }
        return switch (aiStatus) {
            case "高意向" -> "建议尽快确认成交条件，推动下一步商务动作。";
            case "活跃状态" -> "建议保持沟通节奏并明确下次行动，避免热度回落。";
            case "休眠" -> "建议评估沉默原因并重新激活，避免继续流失。";
            default -> "建议尽快安排明确跟进，避免商机降温。";
        };
    }

    /**
     * 标准化AI状态检测。
     */
    private String normalizeAiStatusDetection(String value, CustomerDetailVO detail, List<FollowUpVO> recentFollowUps) {
        String normalized = StrUtil.trimToEmpty(value);
        if (AI_STATUS_DETECTION_OPTIONS.contains(normalized)) {
            return normalized;
        }

        String compact = normalized.replaceAll("\\s+", "");
        if (StrUtil.isNotBlank(compact)) {
            if (compact.contains("休眠") || compact.contains("沉睡") || compact.contains("长期未跟进")
                    || compact.contains("长期未联系") || compact.contains("长期未互动")
                    || compact.contains("超30天") || compact.contains("超过30天") || compact.contains("流失风险")) {
                return "休眠";
            }
            if (compact.contains("高意向") || compact.contains("强意向") || compact.contains("高潜")
                    || compact.contains("高潜力") || compact.contains("签约") || compact.contains("成交")) {
                return "高意向";
            }
            if (compact.contains("活跃状态") || compact.contains("活跃") || compact.contains("持续沟通")
                    || compact.contains("频繁互动") || compact.contains("跟进中") || compact.contains("推进中")) {
                return "活跃状态";
            }
            if (compact.contains("需跟进") || compact.contains("待跟进") || compact.contains("需要跟进")
                    || compact.contains("待联系") || compact.contains("待推进") || compact.contains("待回访")) {
                return "需跟进";
            }
        }

        return inferAiStatusDetection(detail, recentFollowUps);
    }

    /**
     * 处理inferAiStatus检测方法逻辑。
     */
    private String inferAiStatusDetection(CustomerDetailVO detail, List<FollowUpVO> recentFollowUps) {
        FollowUpVO latestFollowUp = recentFollowUps == null || recentFollowUps.isEmpty() ? null : recentFollowUps.get(0);
        long daysSinceLastContact = getDaysSinceNow(detail.getLastContactTime());
        long daysSinceCreate = getDaysSinceNow(detail.getCreateTime());
        long daysSinceLatestFollowUp = latestFollowUp != null && latestFollowUp.getFollowTime() != null
                ? getDaysSinceNow(latestFollowUp.getFollowTime())
                : Long.MAX_VALUE;

        Date now = new Date();
        boolean hasUpcomingFollow = detail.getNextFollowTime() != null && !detail.getNextFollowTime().before(now);
        boolean hasOverdueFollow = detail.getNextFollowTime() != null && detail.getNextFollowTime().before(now);
        boolean hasRecentActivity = daysSinceLastContact <= 7 || daysSinceLatestFollowUp <= 7;
        boolean hasCommercialSignal = detail.getQuotation() != null;
        boolean isHighValue = "A".equalsIgnoreCase(StrUtil.blankToDefault(detail.getLevel(), ""));
        boolean isDormant = "lost".equals(detail.getStage())
                || (!hasUpcomingFollow && daysSinceCreate >= 30 && daysSinceLastContact >= 30 && daysSinceLatestFollowUp >= 30);

        if (isDormant) {
            return "休眠";
        }
        if (("negotiation".equals(detail.getStage()) || "proposal".equals(detail.getStage()) || "closed".equals(detail.getStage()))
                && (hasRecentActivity || hasUpcomingFollow || hasCommercialSignal || isHighValue)) {
            return "高意向";
        }
        if (isHighValue && (hasRecentActivity || hasUpcomingFollow || hasCommercialSignal)) {
            return "高意向";
        }
        if (hasRecentActivity || hasUpcomingFollow) {
            return "活跃状态";
        }
        if (hasOverdueFollow || daysSinceLastContact >= 14 || daysSinceLatestFollowUp >= 14 || detail.getNextFollowTime() == null) {
            return "需跟进";
        }
        return "活跃状态";
    }

    /**
     * 构建AIInsightSuggestion。
     */
    private String buildAiInsightSuggestion(String aiStatus) {
        return switch (aiStatus) {
            case "高意向" -> "建议优先围绕成交条件、预算确认和决策推进节奏展开深度跟进，尽快推动明确下一步商务动作。";
            case "活跃状态" -> "建议保持当前互动频率，持续推进需求澄清、方案演示和关键角色沟通，避免热度回落。";
            case "需跟进" -> "建议尽快安排一次明确触达，确认当前阻塞点、负责人反馈和下一次跟进时间，避免商机继续降温。";
            case "休眠" -> "建议先评估客户沉默原因，再设计重新激活话术或活动触达策略，必要时重新识别需求窗口。";
            default -> "建议结合当前阶段补齐信息、明确下一步动作，并持续更新跟进记录。";
        };
    }

    /**
     * 获取DaysSince编号W。
     */
    private long getDaysSinceNow(Date value) {
        if (value == null) {
            return Long.MAX_VALUE;
        }
        long diff = System.currentTimeMillis() - value.getTime();
        if (diff <= 0) {
            return 0L;
        }
        return diff / (24L * 60L * 60L * 1000L);
    }

    /**
     * 处理extractJsonPayload方法逻辑。
     */
    private String extractJsonPayload(String response) {
        String json = StrUtil.blankToDefault(response, "").trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "");
            json = json.replaceFirst("\\s*```$", "");
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        return json;
    }

    /**
     * 标准化AI报告文本。
     */
    private String normalizeAiReportText(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return compactAiReportText(value);
    }

    /**
     * 标准化AIDeepInsight文本。
     */
    private String normalizeAiDeepInsightText(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return StrUtil.trimToEmpty(value)
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("(?m)^\\s*[-*•·]\\s*", "- ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * 处理compactAiReportText方法逻辑。
     */
    private String compactAiReportText(String value) {
        String normalized = StrUtil.trimToEmpty(value)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ");
        int maxLength = 76;
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        String[] sentences = normalized.split("(?<=[。！？!?；;])");
        StringBuilder builder = new StringBuilder();
        for (String sentence : sentences) {
            String part = StrUtil.trim(sentence);
            if (StrUtil.isBlank(part)) {
                continue;
            }
            if (builder.length() > 0 && builder.length() + part.length() > maxLength) {
                break;
            }
            if (builder.length() == 0 && part.length() > maxLength) {
                return part.substring(0, maxLength) + "...";
            }
            builder.append(part);
        }
        if (builder.length() > 0) {
            return builder.toString();
        }
        return normalized.substring(0, maxLength) + "...";
    }

    /**
     * 格式化Nullable日期时间。
     */
    private String formatNullableDateTime(Date value) {
        return value == null ? "无" : DateUtil.formatDateTime(value);
    }

    /**
     * 格式化Nullable金额。
     */
    private String formatNullableAmount(BigDecimal value) {
        return value == null ? "无" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * 格式化标签名称。
     */
    private String formatTagNames(List<CustomerTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return "无";
        }
        return tags.stream()
                .map(CustomerTag::getTagName)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("、"));
    }

    /**
     * 格式化联系人摘要。
     */
    private String formatContactSummary(List<ContactVO> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return "无";
        }
        return contacts.stream()
                .limit(5)
                .map(contact -> {
                    StringBuilder builder = new StringBuilder(StrUtil.blankToDefault(contact.getName(), "未命名联系人"));
                    if (StrUtil.isNotBlank(contact.getPosition())) {
                        builder.append('/').append(contact.getPosition());
                    }
                    if (StrUtil.isNotBlank(contact.getPhone())) {
                        builder.append('/').append(contact.getPhone());
                    }
                    return builder.toString();
                })
                .collect(Collectors.joining("；"));
    }

    /**
     * 格式化任务摘要。
     */
    private String formatTaskSummary(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "无";
        }
        return tasks.stream()
                .limit(5)
                .map(task -> {
                    StringBuilder builder = new StringBuilder(StrUtil.blankToDefault(task.getTitle(), "未命名任务"));
                    if (StrUtil.isNotBlank(task.getStatus())) {
                        builder.append(" [").append(task.getStatus()).append(']');
                    }
                    if (task.getDueDate() != null) {
                        builder.append(" 截止").append(DateUtil.formatDateTime(task.getDueDate()));
                    }
                    return builder.toString();
                })
                .collect(Collectors.joining("；"));
    }

    /**
     * 格式化跟进摘要。
     */
    private String formatFollowUpSummary(List<FollowUpVO> followUps) {
        if (followUps == null || followUps.isEmpty()) {
            return "无";
        }
        return followUps.stream()
                .limit(5)
                .map(followUp -> {
                    StringBuilder builder = new StringBuilder();
                    if (followUp.getFollowTime() != null) {
                        builder.append(DateUtil.formatDateTime(followUp.getFollowTime())).append(' ');
                    }
                    if (StrUtil.isNotBlank(followUp.getType())) {
                        builder.append('[').append(followUp.getType()).append("] ");
                    }
                    builder.append(truncateText(followUp.getContent(), 80));
                    return builder.toString().trim();
                })
                .collect(Collectors.joining("；"));
    }

    /**
     * 格式化自定义字段摘要。
     */
    private String formatCustomFieldSummary(Map<String, Object> customFields) {
        return customFields.entrySet().stream()
                .filter(entry -> entry.getValue() != null && StrUtil.isNotBlank(String.valueOf(entry.getValue())))
                .limit(10)
                .map(entry -> entry.getKey() + "=" + truncateText(String.valueOf(entry.getValue()), 40))
                .collect(Collectors.joining("；"));
    }

    /**
     * 获取LevelDisplayLabel。
     */
    private String getLevelDisplayLabel(String level) {
        return switch (StrUtil.blankToDefault(level, "")) {
            case "A" -> "A级";
            case "B" -> "B级";
            case "C" -> "C级";
            default -> StrUtil.blankToDefault(level, "未评级");
        };
    }

    /**
     * 处理truncateText方法逻辑。
     */
    private String truncateText(String value, int maxLength) {
        if (StrUtil.isBlank(value)) {
            return "无";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    /**
     * 更新阶段。
     */
    @Override
    public void updateStage(Long customerId, String stage) {
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        customer.setStage(stage);
        updateById(customer);
        globalSearchIndexService.refreshCustomerIndex(customerId);
        taskService.refreshValuePriorityByCustomerId(customerId);
    }

    /**
     * 新增标签。
     */
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

    /**
     * 移除标签。
     */
    @Override
    public void removeTag(Long customerId, Long tagId) {
        customerTagMapper.deleteById(tagId);
        // 同步冗余字段
        syncTagCache(customerId);
    }

    /**
     * 转移客户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferCustomer(CustomerTransferBO transferBO) {
        lambdaUpdate()
            .in(Customer::getCustomerId, transferBO.getCustomerIds())
            .set(Customer::getOwnerId, transferBO.getNewOwnerId())
            .update();
        if (transferBO.getCustomerIds() != null) {
            transferBO.getCustomerIds().forEach(customerId -> {
                globalSearchIndexService.refreshCustomerIndex(customerId);
                globalSearchIndexService.refreshCustomerRelatedIndexes(customerId);
            });
        }
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
        List<CustomFieldVO> customFields = getEnabledCustomOnlyFields("customer");

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
        final int CUSTOMER_COL_END = 10;
        final int CUSTOM_FIELD_COL_START = 16;
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

                // 合并客户列 (0–10)
                for (int col = 0; col <= CUSTOMER_COL_END; col++) {
                    exportSheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, col, col));
                    applyCenteredStyle(exportSheet, wb, firstRow, col);
                }
                // 合并自定义字段列 (16+)
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

    /**
     * 构建导出客户Wrapper。
     */
    private LambdaQueryWrapper<Customer> buildExportCustomerWrapper(CustomerExportBO exportBO) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getStatus, 1);

        if (exportBO.getCustomerIds() != null && !exportBO.getCustomerIds().isEmpty()) {
            wrapper.in(Customer::getCustomerId, exportBO.getCustomerIds());
            wrapper.orderByDesc(Customer::getCreateTime);
            return wrapper;
        }

        String normalizedKeyword = normalizeSearchTextFragment(exportBO.getKeyword());
        if (StrUtil.isNotEmpty(normalizedKeyword)) {
            wrapper.like(Customer::getSearchText, normalizedKeyword);
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

    /**
     * 处理applyExportSort方法逻辑。
     */
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

    /**
     * 构建导出ROW。
     */
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

    /**
     * 处理applyCenteredStyle方法逻辑。
     */
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
        List<CustomFieldVO> customFields = getEnabledCustomOnlyFields("customer");

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
        List<CustomFieldVO> customFields = getEnabledCustomOnlyFields("customer");
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

    /**
     * 获取CellSTR。
     */
    private String getCellStr(List<Object> row, int index) {
        if (index < 0 || index >= row.size() || row.get(index) == null) {
            return null;
        }
        String val = row.get(index).toString().trim();
        return val.isEmpty() ? null : val;
    }

    /**
     * 构建客户导入Unique字段值。
     */
    private Map<String, Object> buildCustomerImportUniqueFieldValues(CustomerImportBO row) {
        Map<String, Object> values = new HashMap<>();
        if (row != null) {
            values.put("companyName", row.getCompanyName());
            values.put("industry", row.getIndustry());
            values.put("stage", row.getStage());
            values.put("level", row.getLevel());
            values.put("source", row.getSource());
            values.put("website", row.getWebsite());
            values.put("quotation", row.getQuotation());
            values.put("address", row.getAddress());
            values.put("remark", row.getRemark());
            if (row.getCustomFields() != null && !row.getCustomFields().isEmpty()) {
                values.putAll(row.getCustomFields());
            }
        }
        return values;
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
                            customFieldService.validateUniqueCustomFieldValues("customer", existing.getCustomerId(),
                                    buildCustomerImportUniqueFieldValues(first));
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
                customFieldService.validateUniqueCustomFieldValues("customer", null,
                        buildCustomerUniqueFieldValues(customer, first.getCustomFields()));
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
                        customFieldService.validateUniqueCustomFieldValues("contact", null,
                                buildContactUniqueFieldValues(contact, null));
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

    /**
     * 插入联系人导入。
     */
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
        customFieldService.validateUniqueCustomFieldValues("contact", null,
                buildContactUniqueFieldValues(contact, null));
        contactMapper.insert(contact);
    }

    /**
     * 获取统计信息。
     */
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
        lambdaUpdate()
            .eq(Customer::getCustomerId, customerId)
            .set(Customer::getPrimaryContactName, primary != null ? primary.getName() : null)
            .set(Customer::getPrimaryContactPhone, primary != null ? primary.getPhone() : null)
            .set(Customer::getPrimaryContactPosition, primary != null ? primary.getPosition() : null)
            .set(Customer::getContactCount, count.intValue())
            .update();
        refreshCustomerSearchText(customerId);
        globalSearchIndexService.refreshCustomerIndex(customerId);
        taskService.refreshValuePriorityByCustomerId(customerId);
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
        refreshCustomerSearchText(customerId);
        globalSearchIndexService.refreshCustomerIndex(customerId);
    }

    // ==================== AI 智能录入 ====================

    public int refreshAllCustomerSearchText() {
        if (!dynamicSchemaService.columnExists(CUSTOMER_TABLE_NAME, CUSTOMER_SEARCH_TEXT_COLUMN)) {
            log.info("skip refresh customer search text because {}.{} does not exist", CUSTOMER_TABLE_NAME, CUSTOMER_SEARCH_TEXT_COLUMN);
            return 0;
        }

        List<Customer> customers = lambdaQuery().list();
        if (customers.isEmpty()) {
            return 0;
        }

        List<CustomFieldVO> searchableFields = getSearchableCustomerTextFields();
        List<Long> customerIds = customers.stream().map(Customer::getCustomerId).toList();
        Map<Long, Map<String, Object>> customFieldMap = searchableFields.isEmpty()
            ? Collections.emptyMap()
            : customFieldService.getBatchCustomFieldValues("customer", customerIds);

        int refreshed = 0;
        for (Customer customer : customers) {
            Map<String, Object> customFields = customFieldMap.getOrDefault(customer.getCustomerId(), Collections.emptyMap());
            String searchText = buildCustomerSearchText(customer, searchableFields, customFields);
            lambdaUpdate()
                .eq(Customer::getCustomerId, customer.getCustomerId())
                .set(Customer::getSearchText, searchText)
                .update();
            refreshed++;
        }
        return refreshed;
    }

    /**
     * 刷新客户搜索文本。
     */
    public void refreshCustomerSearchText(Long customerId) {
        if (customerId == null || !dynamicSchemaService.columnExists(CUSTOMER_TABLE_NAME, CUSTOMER_SEARCH_TEXT_COLUMN)) {
            return;
        }

        Customer customer = getById(customerId);
        if (customer == null) {
            return;
        }

        List<CustomFieldVO> searchableFields = getSearchableCustomerTextFields();
        Map<String, Object> customFields = searchableFields.isEmpty()
            ? Collections.emptyMap()
            : customFieldService.getCustomFieldValues("customer", customerId);
        String searchText = buildCustomerSearchText(customer, searchableFields, customFields);

        lambdaUpdate()
            .eq(Customer::getCustomerId, customerId)
            .set(Customer::getSearchText, searchText)
            .update();
    }

    /**
     * 获取Searchable客户文本字段。
     */
    private List<CustomFieldVO> getSearchableCustomerTextFields() {
        return getEnabledCustomOnlyFields("customer").stream()
            .filter(field -> Boolean.TRUE.equals(field.getIsSearchable()))
            .filter(field -> SEARCHABLE_CUSTOM_FIELD_TYPES.contains(field.getFieldType()))
            .toList();
    }

    /**
     * 获取启用项自定义Only字段。
     */
    private List<CustomFieldVO> getEnabledCustomOnlyFields(String entityType) {
        return customFieldService.getEnabledFieldsByEntity(entityType).stream()
            .filter(field -> !"system".equalsIgnoreCase(field.getFieldSource()))
            .toList();
    }

    /**
     * 处理applyCustomerQueryScope方法逻辑。
     */
    private void applyCustomerQueryScope(CustomerQueryBO queryBO) {
        queryBO.setTenantId(TenantContextHolder.getTenantId());

        DataPermissionContext context = dataPermissionService.createContext("customer");
        if (context == null || context.isAllData()) {
            queryBO.setAuthorizedOwnerIds(null);
            return;
        }

        if (context.getUserIds() == null || context.getUserIds().isEmpty()) {
            queryBO.setAuthorizedOwnerIds(Collections.emptyList());
            return;
        }

        queryBO.setAuthorizedOwnerIds(new ArrayList<>(context.getUserIds()));
    }

    /**
     * 构建客户搜索文本。
     */
    private String buildCustomerSearchText(Customer customer,
                                           List<CustomFieldVO> searchableFields,
                                           Map<String, Object> customFields) {
        LinkedHashSet<String> fragments = new LinkedHashSet<>();

        if (customer != null) {
            appendSearchText(fragments, customer.getCompanyName());
            appendSearchText(fragments, customer.getIndustry());
            appendSearchText(fragments, customer.getStage());
            appendSearchText(fragments, getStageLabel(customer.getStage()));
            appendSearchText(fragments, customer.getLevel());
            appendSearchText(fragments, getLevelDisplayLabel(customer.getLevel()));
            appendSearchText(fragments, customer.getSource());
            appendSearchText(fragments, customer.getAddress());
            appendSearchText(fragments, customer.getWebsite());
            appendSearchText(fragments, customer.getPrimaryContactName());
            appendSearchText(fragments, customer.getPrimaryContactPhone());
            appendSearchText(fragments, customer.getPrimaryContactPosition());
            appendSearchText(fragments, customer.getTagNames());
            appendSearchText(fragments, customer.getRemark());
            appendSearchText(fragments, customer.getAiStatusDetection());
            appendSearchText(fragments, customer.getAiInsight());
        }

        if (searchableFields != null && !searchableFields.isEmpty() && customFields != null && !customFields.isEmpty()) {
            for (CustomFieldVO field : searchableFields) {
                for (String value : resolveCustomFieldSearchValues(field, resolveSearchFieldRawValue(field, customFields))) {
                    appendSearchText(fragments, value);
                }
            }
        }

        return String.join(" ", fragments);
    }

    /**
     * 解析搜索字段RAW值。
     */
    private Object resolveSearchFieldRawValue(CustomFieldVO field, Map<String, Object> customFields) {
        if (field == null || customFields == null || customFields.isEmpty()) {
            return null;
        }

        String columnName = StrUtil.trim(field.getColumnName());
        if (StrUtil.isNotBlank(columnName) && customFields.containsKey(columnName)) {
            return customFields.get(columnName);
        }

        String fieldName = StrUtil.trim(field.getFieldName());
        if (StrUtil.isNotBlank(fieldName) && customFields.containsKey(fieldName)) {
            return customFields.get(fieldName);
        }

        return null;
    }

    /**
     * 解析自定义字段搜索值。
     */
    private List<String> resolveCustomFieldSearchValues(CustomFieldVO field, Object rawValue) {
        if (field == null || rawValue == null) {
            return Collections.emptyList();
        }

        return switch (field.getFieldType()) {
            case "select" -> buildSelectSearchValues(field, rawValue);
            case "multiselect" -> buildMultiselectSearchValues(field, rawValue);
            default -> Collections.singletonList(String.valueOf(rawValue));
        };
    }

    /**
     * 构建Select搜索值。
     */
    private List<String> buildSelectSearchValues(CustomFieldVO field, Object rawValue) {
        String raw = StrUtil.trim(String.valueOf(rawValue));
        if (StrUtil.isBlank(raw)) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        values.add(raw);
        String label = resolveFieldOptionLabel(field, raw);
        if (StrUtil.isNotBlank(label) && !StrUtil.equals(label, raw)) {
            values.add(label);
        }
        return values;
    }

    /**
     * 构建Multiselect搜索值。
     */
    private List<String> buildMultiselectSearchValues(CustomFieldVO field, Object rawValue) {
        List<String> values = new ArrayList<>();
        for (String item : parseMultiValueSafe(rawValue)) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            values.add(item);
            String label = resolveFieldOptionLabel(field, item);
            if (StrUtil.isNotBlank(label) && !StrUtil.equals(label, item)) {
                values.add(label);
            }
        }
        return values;
    }

    /**
     * 解析Multi值Safe。
     */
    private List<String> parseMultiValueSafe(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }
        if (rawValue instanceof Collection<?> collection) {
            return collection.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(StrUtil::isNotBlank)
                .toList();
        }

        String text = StrUtil.trim(String.valueOf(rawValue));
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }

        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                JsonNode array = objectMapper.readTree(text);
                if (array.isArray()) {
                    List<String> values = new ArrayList<>();
                    array.forEach(node -> {
                        String value = StrUtil.trim(node.asText());
                        if (StrUtil.isNotBlank(value)) {
                            values.add(value);
                        }
                    });
                    return values;
                }
            } catch (Exception e) {
                log.warn("parse multiselect custom field value failed: {}", e.getMessage());
            }
        }

        return Arrays.stream(text.split("[,\\uFF0C]"))
            .map(StrUtil::trim)
            .filter(StrUtil::isNotBlank)
            .toList();
    }

    /**
     * 解析字段选项Label。
     */
    private String resolveFieldOptionLabel(CustomFieldVO field, String rawValue) {
        if (field == null || field.getOptions() == null || field.getOptions().isEmpty() || StrUtil.isBlank(rawValue)) {
            return rawValue;
        }

        return field.getOptions().stream()
            .filter(option -> StrUtil.equals(option.getValue(), rawValue))
            .map(FieldOption::getLabel)
            .filter(StrUtil::isNotBlank)
            .findFirst()
            .orElse(rawValue);
    }

    /**
     * 处理appendSearchText方法逻辑。
     */
    private void appendSearchText(Set<String> fragments, String value) {
        String normalized = normalizeSearchTextFragment(value);
        if (StrUtil.isNotBlank(normalized)) {
            fragments.add(normalized);
        }
    }

    /**
     * 标准化搜索文本Fragment。
     */
    private String normalizeSearchTextFragment(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        String normalized = value
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace('\t', ' ')
            .replace('，', ' ')
            .replace(',', ' ')
            .replace('；', ' ')
            .replace(';', ' ');
        normalized = normalized.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * 使用 AI 解析客户。
     */
    @Override
    public CustomerAiParseVO aiParseCustomer(CustomerAiParseBO parseBO) {
        String prompt = String.format(AI_CUSTOMER_PARSE_PROMPT, parseBO.getContent());

        try {
            aiQuotaService.ensureQuotaAvailable("customer_parse", null, null, prompt);
            String response;

            if (StrUtil.isNotEmpty(parseBO.getImageObjectKey())) {
                if (!chatClientProvider.getCurrentCapabilities().isSupportsVision()) {
                    throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                            "当前 AI 模型不支持图片识别，请在系统设置中切换到支持视觉输入的模型后重试");
                }
                // Multimodal: text + image
                String mimeTypeStr = StrUtil.blankToDefault(parseBO.getImageMimeType(), "image/png");
                MimeType mimeType = MimeType.valueOf(mimeTypeStr);
                Media media = AiMediaUtil.buildMedia(fileStorageService, parseBO.getImageObjectKey(), mimeType);

                var chatResponse = chatClientProvider.getChatClient()
                        .prompt()
                        .user(u -> u.text(prompt).media(media))
                        .call()
                        .chatResponse();
                response = chatResponse.getResult().getOutput().getText();
                aiQuotaService.consumeResolvedTokens(
                    "customer_parse",
                    aiQuotaService.resolveTokenUsage(chatResponse, null, null, prompt, response)
                );
            } else {
                // Text only
                var chatResponse = chatClientProvider.getChatClient()
                        .prompt()
                        .user(prompt)
                        .call()
                        .chatResponse();
                response = chatResponse.getResult().getOutput().getText();
                aiQuotaService.consumeResolvedTokens(
                    "customer_parse",
                    aiQuotaService.resolveTokenUsage(chatResponse, null, null, prompt, response)
                );
            }

            log.info("AI 客户录入解析原始响应: {}", response);
            CustomerAiParseVO result = parseCustomerAiResponse(response, parseBO.getContent());
            if (isEmptyCustomerAiParseResult(result)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR,
                        "AI 未提取到可填充的信息，请确认图片清晰或补充文字描述后重试");
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 客户录入解析失败", e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR,
                    "AI 提取失败，请检查图片格式、模型配置后重试");
        }
    }

    /**
     * 使用 AI 解析搜索。
     */
    @Override
    public CustomerAiSearchParseVO aiParseSearch(CustomerAiSearchParseBO parseBO) {
        String normalizedQuery = StrUtil.trim(parseBO.getQuery());
        if (StrUtil.isBlank(normalizedQuery)) {
            return buildFallbackSearchResult("");
        }

        String now = LocalDateTime.now().format(AI_SEARCH_TIME_FORMATTER);
        String prompt = String.format(AI_CUSTOMER_SEARCH_PARSE_PROMPT, now, normalizedQuery);

        try {
            aiQuotaService.ensureQuotaAvailable("customer_search_parse", null, null, prompt);
            var chatResponse = chatClientProvider.getChatClient()
                .prompt()
                .user(prompt)
                .call()
                .chatResponse();
            String response = chatResponse.getResult().getOutput().getText();
            aiQuotaService.consumeResolvedTokens(
                "customer_search_parse",
                aiQuotaService.resolveTokenUsage(chatResponse, null, null, prompt, response)
            );

            log.info("AI 客户搜索解析原始响应: {}", response);
            return parseCustomerAiSearchResponse(response, normalizedQuery);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 客户搜索解析失败", e);
            CustomerAiSearchQueryVO heuristicQuery = buildHeuristicSearchQuery(normalizedQuery);
            if (!isSearchQueryEmpty(heuristicQuery)) {
                return buildSearchResult(normalizedQuery, heuristicQuery, "已使用内置规则解析搜索条件", 0.65, false);
            }
            return buildFallbackSearchResult(normalizedQuery);
        }
    }

    /**
     * 解析客户AI响应。
     */
    private CustomerAiParseVO parseCustomerAiResponse(String response, String sourceContent) {
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
            vo.setWebsite(resolveAiParsedWebsite(getJsonText(root, "website"), sourceContent));
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
            return buildFallbackCustomerResult(sourceContent);
        }
    }

    /**
     * 解析客户AI搜索响应。
     */
    private CustomerAiSearchParseVO parseCustomerAiSearchResponse(String response, String normalizedQuery) {
        try {
            String json = extractJsonPayload(response);
            JsonNode root = objectMapper.readTree(json);
            JsonNode queryNode = root.has("parsedQuery") ? root.get("parsedQuery") : root;
            CustomerAiSearchQueryVO query = toCustomerAiSearchQuery(queryNode);
            normalizeAiSearchQuery(query, normalizedQuery);
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

    /**
     * 转换为客户AI搜索查询。
     */
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
        query.setSortBy(normalizeSortBy(getJsonText(queryNode, "sortBy")));
        query.setSortOrder(normalizeSortOrder(getJsonText(queryNode, "sortOrder")));
        return query;
    }

    /**
     * 标准化AI搜索查询。
     */
    private void normalizeAiSearchQuery(CustomerAiSearchQueryVO query, String normalizedQuery) {
        if (query == null) {
            return;
        }

        applyAmountFilterFromQuery(query, normalizedQuery, QUOTATION_COMPARE_PATTERN, QUOTATION_RANGE_PATTERN, "quotation");

        boolean keepZeroQuotation = shouldKeepZeroAmountFilter(normalizedQuery, "报价", "报价金额", "预计成交", "预计成交金额");
        boolean keepZeroContactCount = shouldKeepZeroContactCountFilter(normalizedQuery);

        if (!keepZeroQuotation) {
            if (isZero(query.getQuotationMin())) {
                query.setQuotationMin(null);
            }
            if (isZero(query.getQuotationMax())) {
                query.setQuotationMax(null);
            }
        }
        if (!keepZeroContactCount) {
            if (Integer.valueOf(0).equals(query.getContactCountMin())) {
                query.setContactCountMin(null);
            }
            if (Integer.valueOf(0).equals(query.getContactCountMax())) {
                query.setContactCountMax(null);
            }
        }

        normalizeAmountRange(query, "quotation", keepZeroQuotation);
        normalizeContactCountRange(query, keepZeroContactCount);
    }

    /**
     * 处理applyAmountFilterFromQuery方法逻辑。
     */
    private void applyAmountFilterFromQuery(CustomerAiSearchQueryVO query,
                                            String normalizedQuery,
                                            Pattern comparePattern,
                                            Pattern rangePattern,
                                            String fieldKey) {
        if (query == null || StrUtil.isBlank(normalizedQuery)) {
            return;
        }

        Matcher rangeMatcher = rangePattern.matcher(normalizedQuery);
        if (rangeMatcher.find()) {
            BigDecimal min = parseSearchAmount(rangeMatcher.group(2), rangeMatcher.group(3));
            BigDecimal max = parseSearchAmount(rangeMatcher.group(4), rangeMatcher.group(5));
            if (min != null && max != null) {
                if (min.compareTo(max) > 0) {
                    BigDecimal temp = min;
                    min = max;
                    max = temp;
                }
                setAmountRange(query, fieldKey, min, max);
                return;
            }
        }

        Matcher compareMatcher = comparePattern.matcher(normalizedQuery);
        if (!compareMatcher.find()) {
            return;
        }

        BigDecimal value = parseSearchAmount(compareMatcher.group(3), compareMatcher.group(4));
        if (value == null) {
            return;
        }
        applyAmountOperator(query, fieldKey, compareMatcher.group(2), value);
    }

    /**
     * 处理applyAmountOperator方法逻辑。
     */
    private void applyAmountOperator(CustomerAiSearchQueryVO query, String fieldKey, String operator, BigDecimal value) {
        if (query == null || value == null) {
            return;
        }

        String normalizedOperator = normalizeSearchOperator(operator);
        if (isEqualityOperator(normalizedOperator)) {
            setAmountRange(query, fieldKey, value, value);
            return;
        }
        if (isUpperBoundOperator(normalizedOperator)) {
            setAmountRange(query, fieldKey, null, value);
            return;
        }
        setAmountRange(query, fieldKey, value, null);
    }

    /**
     * 设置金额范围。
     */
    private void setAmountRange(CustomerAiSearchQueryVO query, String fieldKey, BigDecimal min, BigDecimal max) {
        if (query == null || StrUtil.isBlank(fieldKey)) {
            return;
        }
        switch (fieldKey) {
            case "quotation" -> {
                query.setQuotationMin(min);
                query.setQuotationMax(max);
            }
            default -> {
                return;
            }
        }
    }

    /**
     * 标准化金额范围。
     */
    private void normalizeAmountRange(CustomerAiSearchQueryVO query, String fieldKey, boolean keepZero) {
        if (query == null || StrUtil.isBlank(fieldKey)) {
            return;
        }

        BigDecimal min;
        BigDecimal max;
        switch (fieldKey) {
            case "quotation" -> {
                min = query.getQuotationMin();
                max = query.getQuotationMax();
            }
            default -> {
                return;
            }
        }

        if (min == null || max == null || min.compareTo(max) <= 0) {
            return;
        }

        if (!keepZero && isZero(max)) {
            setAmountRange(query, fieldKey, min, null);
            return;
        }
        if (!keepZero && isZero(min)) {
            setAmountRange(query, fieldKey, null, max);
            return;
        }
        setAmountRange(query, fieldKey, max, min);
    }

    /**
     * 标准化联系人Count范围。
     */
    private void normalizeContactCountRange(CustomerAiSearchQueryVO query, boolean keepZero) {
        if (query == null) {
            return;
        }
        Integer min = query.getContactCountMin();
        Integer max = query.getContactCountMax();
        if (min == null || max == null || min <= max) {
            return;
        }
        if (!keepZero && max == 0) {
            query.setContactCountMax(null);
            return;
        }
        if (!keepZero && min == 0) {
            query.setContactCountMin(null);
            return;
        }
        query.setContactCountMin(max);
        query.setContactCountMax(min);
    }

    /**
     * 判断是否KeepZero金额筛选。
     */
    private boolean shouldKeepZeroAmountFilter(String normalizedQuery, String... fieldKeywords) {
        return mentionsExplicitZero(normalizedQuery) && containsAny(normalizedQuery, fieldKeywords);
    }

    /**
     * 判断是否KeepZero联系人Count筛选。
     */
    private boolean shouldKeepZeroContactCountFilter(String normalizedQuery) {
        if (StrUtil.isBlank(normalizedQuery)) {
            return false;
        }
        if (containsAny(normalizedQuery, "无联系人", "没有联系人", "未添加联系人", "联系人为空")) {
            return true;
        }
        return mentionsExplicitZero(normalizedQuery) && containsAny(normalizedQuery, "联系人", "联系人数", "联系人数量");
    }

    /**
     * 处理mentionsExplicitZero方法逻辑。
     */
    private boolean mentionsExplicitZero(String normalizedQuery) {
        return StrUtil.isNotBlank(normalizedQuery) && EXPLICIT_ZERO_PATTERN.matcher(normalizedQuery).find();
    }

    /**
     * 处理containsAny方法逻辑。
     */
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

    /**
     * 判断是否Zero。
     */
    private boolean isZero(BigDecimal value) {
        return value != null && BigDecimal.ZERO.compareTo(value) == 0;
    }

    /**
     * 构建搜索结果。
     */
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

    /**
     * 构建兜底搜索结果。
     */
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

    /**
     * 构建Heuristic搜索查询。
     */
    private CustomerAiSearchQueryVO buildHeuristicSearchQuery(String normalizedQuery) {
        CustomerAiSearchQueryVO query = new CustomerAiSearchQueryVO();
        normalizeAiSearchQuery(query, normalizedQuery);
        enrichSearchQueryWithHeuristics(query, normalizedQuery);
        return query;
    }

    /**
     * 处理enrichSearchQuery包含Heuristics方法逻辑。
     */
    private void enrichSearchQueryWithHeuristics(CustomerAiSearchQueryVO query, String normalizedQuery) {
        if (query == null || StrUtil.isBlank(normalizedQuery)) {
            return;
        }

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

    /**
     * 判断是否搜索查询空值。
     */
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
            && query.getContactCountMax() == null;
    }

    /**
     * 构建搜索Chips。
     */
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
        if (StrUtil.isNotBlank(query.getSortBy())) {
            chips.add(new CustomerAiSearchDisplayChipVO("sort", buildSortLabel(query.getSortBy(), query.getSortOrder())));
        }
        return chips;
    }

    /**
     * 获取阶段Label。
     */
    private String getStageLabel(String stage) {
        if (StrUtil.isBlank(stage)) {
            return "未知";
        }
        return STAGE_LABEL_MAP.getOrDefault(stage, stage);
    }

    /**
     * 构建金额范围Label。
     */
    private String buildAmountRangeLabel(String prefix, BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return prefix + ": " + formatCompactAmount(min) + " - " + formatCompactAmount(max);
        }
        if (min != null) {
            return prefix + ">=" + formatCompactAmount(min);
        }
        return prefix + "<=" + formatCompactAmount(max);
    }

    /**
     * 构建日期范围Label。
     */
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

    /**
     * 构建排序Label。
     */
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

    /**
     * 解析搜索Explanation。
     */
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

    /**
     * 构建搜索ExplanationChips。
     */
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

    /**
     * 处理sanitizeSearchExplanation方法逻辑。
     */
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

    /**
     * 处理contains内部SearchField方法逻辑。
     */
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

    /**
     * 格式化Compact金额。
     */
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

    /**
     * 构建金额ComparePattern。
     */
    private static Pattern buildAmountComparePattern(String fieldRegex) {
        return Pattern.compile("(" + fieldRegex + ")\\s*(?:在)?\\s*(大于等于|大于或等于|不少于|不低于|高于或等于|>=|＞=|≥|大于|高于|超过|多于|>|＞|小于等于|小于或等于|不超过|不高于|低于或等于|<=|＜=|≤|小于|低于|少于|以内|以下|<|＜|等于|为|=)\\s*" + SEARCH_NUMBER_WITH_UNIT_PATTERN);
    }

    /**
     * 构建金额范围Pattern。
     */
    private static Pattern buildAmountRangePattern(String fieldRegex) {
        return Pattern.compile("(" + fieldRegex + ")\\s*" + SEARCH_NUMBER_WITH_UNIT_PATTERN + "\\s*(?:到|至|~|～|-|—)\\s*" + SEARCH_NUMBER_WITH_UNIT_PATTERN);
    }

    /**
     * 解析搜索金额。
     */
    private BigDecimal parseSearchAmount(String numberText, String unitText) {
        if (StrUtil.isBlank(numberText)) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(numberText.replace(",", ""));
            String normalizedUnit = StrUtil.blankToDefault(unitText, "").trim().toLowerCase(Locale.ROOT);
            return switch (normalizedUnit) {
                case "万", "w" -> amount.multiply(BigDecimal.valueOf(10_000L));
                case "千", "k" -> amount.multiply(BigDecimal.valueOf(1_000L));
                case "亿" -> amount.multiply(BigDecimal.valueOf(100_000_000L));
                default -> amount;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析JSONBIGDecimal。
     */
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

    /**
     * 解析JSONInteger。
     */
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

    /**
     * 解析JSONBoolean。
     */
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

    /**
     * 解析JSON日期。
     */
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

    /**
     * 解析JSONConfidence。
     */
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

    /**
     * 标准化阶段验证码。
     */
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

    /**
     * 标准化排序按。
     */
    private String normalizeSortBy(String sortBy) {
        String normalized = normalizeOptionalText(sortBy);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "createTime", "create_time", "创建时间", "新增时间" -> "createTime";
            case "quotation", "报价", "报价金额", "预计成交", "预计成交金额" -> "quotation";
            case "lastContactTime", "last_contact_time", "最后联系", "最后跟进" -> "lastContactTime";
            case "nextFollowTime", "next_follow_time", "下次跟进" -> "nextFollowTime";
            case "contactCount", "contact_count", "联系人数", "联系人数量" -> "contactCount";
            default -> null;
        };
    }

    /**
     * 标准化排序订单。
     */
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

    /**
     * 标准化搜索Operator。
     */
    private String normalizeSearchOperator(String operator) {
        if (operator == null) {
            return "";
        }
        return operator.replaceAll("\\s+", "");
    }

    /**
     * 判断是否UpperBoundOperator。
     */
    private boolean isUpperBoundOperator(String operator) {
        return switch (operator) {
            case "小于等于", "小于或等于", "不超过", "不高于", "低于或等于", "<=", "＜=", "≤", "小于", "低于", "少于", "以内", "以下", "<", "＜" -> true;
            default -> false;
        };
    }

    /**
     * 判断是否EqualityOperator。
     */
    private boolean isEqualityOperator(String operator) {
        return switch (operator) {
            case "等于", "为", "=" -> true;
            default -> false;
        };
    }

    /**
     * 标准化Optional文本。
     */
    private String normalizeOptionalText(String value) {
        String normalized = StrUtil.trim(value);
        if (StrUtil.isBlank(normalized) || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    /**
     * 解析AIParsed网站。
     */
    private String resolveAiParsedWebsite(String parsedWebsite, String sourceContent) {
        String normalizedWebsite = normalizeWebsiteForAiParse(parsedWebsite);
        if (StrUtil.isNotBlank(normalizedWebsite)) {
            return normalizedWebsite;
        }
        return extractWebsiteFromContent(sourceContent);
    }

    /**
     * 处理extract网站FromContent方法逻辑。
     */
    private String extractWebsiteFromContent(String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        Matcher matcher = WEBSITE_URL_PATTERN.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return normalizeWebsiteForAiParse(matcher.group());
    }

    /**
     * 标准化网站用于AI解析。
     */
    private String normalizeWebsiteForAiParse(String website) {
        String normalized = StrUtil.trim(website);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        while (normalized.length() > 0 && ".,;:，。；、)）]】>》".indexOf(normalized.charAt(normalized.length() - 1)) >= 0) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        if (normalized.toLowerCase(Locale.ROOT).startsWith("www.")) {
            normalized = "https://" + normalized;
        }
        return customerLogoService.normalizeWebsite(normalized);
    }

    /**
     * 获取JSON文本。
     */
    private String getJsonText(JsonNode root, String field) {
        if (root.has(field) && !root.get(field).isNull()) {
            return root.get(field).asText();
        }
        return null;
    }

    /**
     * 获取JSONString列表。
     */
    private List<String> getJsonStringList(JsonNode root, String field) {
        List<String> result = new ArrayList<>();
        if (root.has(field) && root.get(field).isArray()) {
            root.get(field).forEach(n -> result.add(n.asText()));
        }
        return result;
    }

    /**
     * 构建兜底客户结果。
     */
    private CustomerAiParseVO buildFallbackCustomerResult(String sourceContent) {
        CustomerAiParseVO vo = new CustomerAiParseVO();
        vo.setWebsite(extractWebsiteFromContent(sourceContent));
        vo.setTags(List.of());
        vo.setKeyPoints(List.of());
        return vo;
    }

    /**
     * 判断是否空值客户AI解析结果。
     */
    private boolean isEmptyCustomerAiParseResult(CustomerAiParseVO result) {
        if (result == null) {
            return true;
        }
        return StrUtil.isAllBlank(
                result.getCompanyName(),
                result.getIndustry(),
                result.getLevel(),
                result.getStage(),
                result.getSource(),
                result.getWebsite(),
                result.getRemark(),
                result.getContactName(),
                result.getContactPhone(),
                result.getContactEmail(),
                result.getContactPosition(),
                result.getSummary(),
                result.getNextStep()
        ) && (result.getScore() == null || result.getScore() <= 0)
                && (result.getTags() == null || result.getTags().isEmpty())
                && (result.getKeyPoints() == null || result.getKeyPoints().isEmpty());
    }
}
