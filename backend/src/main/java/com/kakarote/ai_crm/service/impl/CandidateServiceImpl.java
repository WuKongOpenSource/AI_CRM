package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.CandidateAddBO;
import com.kakarote.ai_crm.entity.BO.CandidateFieldUpdateBO;
import com.kakarote.ai_crm.entity.BO.CandidateQueryBO;
import com.kakarote.ai_crm.entity.BO.CandidateResumeParseBO;
import com.kakarote.ai_crm.entity.BO.CandidateUpdateBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.BO.ScheduleQueryBO;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.BO.FieldOption;
import com.kakarote.ai_crm.entity.PO.Candidate;
import com.kakarote.ai_crm.entity.PO.RecruitmentJob;
import com.kakarote.ai_crm.entity.VO.CandidateDetailVO;
import com.kakarote.ai_crm.entity.VO.CandidateListVO;
import com.kakarote.ai_crm.entity.VO.CandidateResumeParseVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.entity.VO.RecruitmentJobVO;
import com.kakarote.ai_crm.entity.VO.ScheduleVO;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import com.kakarote.ai_crm.mapper.CandidateMapper;
import com.kakarote.ai_crm.mapper.KnowledgeMapper;
import com.kakarote.ai_crm.mapper.ScheduleMapper;
import com.kakarote.ai_crm.mapper.TaskMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ICandidateService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IRecruitmentJobService;
import com.kakarote.ai_crm.utils.DocumentTextExtractor;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CandidateServiceImpl extends ServiceImpl<CandidateMapper, Candidate> implements ICandidateService {

    private static final String ENTITY_CANDIDATE = "candidate";
    private static final String DEFAULT_STAGE = "new";
    private static final String DEFAULT_SOURCE = "manual";
    private static final int MAX_RESUME_TEXT_LENGTH = 12000;
    private static final int SNAPSHOT_TEXT_LENGTH = 4000;

    private static final Map<String, String> STAGE_LABEL_FALLBACK = Map.of(
            "new", "新候选人",
            "screening", "初筛",
            "interview_scheduled", "安排面试",
            "interviewing", "面试中",
            "interview_passed", "面试通过",
            "offer", "Offer",
            "hired", "已入职",
            "rejected", "已淘汰"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?86[-\\s]?)?(1[3-9]\\d{9})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern YEARS_PATTERN = Pattern.compile("(\\d{1,2}(?:\\.\\d)?)\\s*(?:年|years?)\\s*(?:工作|经验|experience)?", Pattern.CASE_INSENSITIVE);

    @Autowired
    private ICustomFieldService customFieldService;

    @Autowired
    private IRecruitmentJobService recruitmentJobService;

    @Autowired
    private DataPermissionService dataPermissionService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Lazy
    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addCandidate(CandidateAddBO bo) {
        Candidate candidate = new Candidate();
        applyAddFields(candidate, bo);
        candidate.setStatus(1);
        candidate.setStage(normalizeStage(candidate.getStage()));
        candidate.setSource(StrUtil.blankToDefault(normalizeText(candidate.getSource()), DEFAULT_SOURCE));
        candidate.setOwnerId(resolveOwnerId(candidate.getOwnerId()));
        customFieldService.validateUniqueCustomFieldValues(ENTITY_CANDIDATE, null,
                buildUniqueFieldValues(candidate, bo.getCustomFields()));
        save(candidate);
        updateCustomFields(candidate.getCandidateId(), bo.getCustomFields());
        return candidate.getCandidateId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCandidate(CandidateUpdateBO bo) {
        Candidate candidate = getVisibleCandidate(bo.getCandidateId());
        applyUpdateFields(candidate, bo);
        candidate.setStage(normalizeStage(candidate.getStage()));
        candidate.setOwnerId(resolveOwnerId(candidate.getOwnerId()));
        customFieldService.validateUniqueCustomFieldValues(ENTITY_CANDIDATE, candidate.getCandidateId(),
                buildUniqueFieldValues(candidate, bo.getCustomFields()));
        updateById(candidate);
        updateCustomFields(candidate.getCandidateId(), bo.getCustomFields());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CandidateDetailVO updateCandidateField(CandidateFieldUpdateBO bo) {
        Candidate candidate = getVisibleCandidate(bo.getCandidateId());
        String fieldName = StrUtil.trim(bo.getFieldName());
        Object value = bo.getValue();
        if (isCoreField(fieldName)) {
            applyCoreField(candidate, fieldName, value);
            candidate.setStage(normalizeStage(candidate.getStage()));
            candidate.setOwnerId(resolveOwnerId(candidate.getOwnerId()));
            customFieldService.validateUniqueCustomFieldValues(ENTITY_CANDIDATE, candidate.getCandidateId(),
                    buildUniqueFieldValues(candidate, null));
            updateById(candidate);
        } else {
            customFieldService.updateCustomFieldValue(ENTITY_CANDIDATE, candidate.getCandidateId(), fieldName, value);
        }
        return getCandidateDetail(candidate.getCandidateId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCandidate(Long candidateId) {
        Candidate candidate = getVisibleCandidate(candidateId);
        candidate.setStatus(0);
        updateById(candidate);
    }

    @Override
    public BasePage<CandidateListVO> queryPageList(CandidateQueryBO queryBO) {
        if (queryBO == null) {
            queryBO = new CandidateQueryBO();
        }
        applyDataScope(queryBO);
        BasePage<CandidateListVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        page.getRecords().forEach(this::completeCandidateListVO);
        fillCustomFields(page.getRecords());
        return page;
    }

    @Override
    public CandidateDetailVO getCandidateDetail(Long candidateId) {
        Candidate candidate = getVisibleCandidate(candidateId);
        CandidateDetailVO detail = baseMapper.getCandidateById(candidate.getCandidateId());
        if (detail == null) {
            detail = BeanUtil.copyProperties(candidate, CandidateDetailVO.class);
        }
        completeCandidateDetailVO(detail);
        detail.setCustomFields(customFieldService.getCustomFieldValues(ENTITY_CANDIDATE, candidateId));
        detail.setTasks(queryCandidateTasks(candidateId));
        detail.setSchedules(queryCandidateSchedules(candidateId));
        detail.setResumes(queryCandidateResumes(candidateId));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStage(Long candidateId, String stage) {
        Candidate candidate = getVisibleCandidate(candidateId);
        candidate.setStage(normalizeStage(stage));
        updateById(candidate);
    }

    @Override
    public Candidate getVisibleCandidate(Long candidateId) {
        Candidate candidate = getById(candidateId);
        if (ObjectUtil.isNull(candidate) || Objects.equals(candidate.getStatus(), 0)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "候选人不存在");
        }
        dataPermissionService.assertUserDataAccessByPermission("candidate:view", candidate.getOwnerId());
        return candidate;
    }

    @Override
    public CandidateDetailVO getVisibleCandidateVO(Long candidateId) {
        return getCandidateDetail(candidateId);
    }

    @Override
    public List<Candidate> findByPhoneOrEmail(String phone, String email) {
        return baseMapper.selectByPhoneOrEmailIgnoreDataPermission(normalizeText(phone), normalizeText(email));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CandidateResumeParseVO aiParseResume(CandidateResumeParseBO bo) {
        String text = resolveResumeText(bo);
        Map<String, Object> parsed = parseResumeFields(text, bo);
        CandidateResumeParseVO vo = new CandidateResumeParseVO();
        vo.setParsedFields(parsed);
        vo.setResumeSummary(asText(parsed.get("resumeSummary")));
        vo.setAiEvaluation(asText(parsed.get("aiEvaluation")));
        vo.setRawText(abbreviate(text, SNAPSHOT_TEXT_LENGTH));

        if (Boolean.TRUE.equals(bo.getAutoSave())) {
            upsertParsedCandidate(bo, parsed, vo);
        }
        return vo;
    }

    @Override
    public CandidateResumeParseVO parseAndUpsertResumeAttachment(String fileName, String filePath,
                                                                 Long fileSize, String mimeType,
                                                                 Long candidateId) {
        CandidateResumeParseBO bo = new CandidateResumeParseBO();
        bo.setCandidateId(candidateId);
        bo.setFileName(fileName);
        bo.setFilePath(filePath);
        bo.setFileSize(fileSize);
        bo.setMimeType(mimeType);
        bo.setSource("resume_upload");
        bo.setAutoSave(true);
        return aiParseResume(bo);
    }

    private void applyAddFields(Candidate candidate, CandidateAddBO bo) {
        candidate.setName(requireName(bo.getName()));
        candidate.setAvatar(normalizeText(bo.getAvatar()));
        candidate.setPhone(normalizeText(bo.getPhone()));
        candidate.setEmail(normalizeEmail(bo.getEmail()));
        candidate.setWechat(normalizeText(bo.getWechat()));
        candidate.setCurrentCompany(normalizeText(bo.getCurrentCompany()));
        candidate.setCurrentPosition(normalizeText(bo.getCurrentPosition()));
        candidate.setAppliedPosition(normalizeText(bo.getAppliedPosition()));
        candidate.setJobRequirements(normalizeLongText(bo.getJobRequirements()));
        Long recruitmentJobId = bo.getRecruitmentJobId() != null
                ? bo.getRecruitmentJobId()
                : findRecruitmentJobIdByName(candidate.getAppliedPosition());
        applyRecruitmentJob(candidate, recruitmentJobId);
        candidate.setStage(normalizeText(bo.getStage()));
        candidate.setSource(normalizeText(bo.getSource()));
        candidate.setEducation(normalizeText(bo.getEducation()));
        candidate.setSchool(normalizeText(bo.getSchool()));
        candidate.setMajor(normalizeText(bo.getMajor()));
        candidate.setWorkYears(bo.getWorkYears());
        candidate.setExpectedCity(normalizeText(bo.getExpectedCity()));
        candidate.setExpectedSalary(normalizeText(bo.getExpectedSalary()));
        candidate.setSkillTags(normalizeText(bo.getSkillTags()));
        candidate.setResumeSummary(normalizeLongText(bo.getResumeSummary()));
        candidate.setAiEvaluation(normalizeLongText(bo.getAiEvaluation()));
        candidate.setOwnerId(bo.getOwnerId());
        candidate.setRemark(normalizeLongText(bo.getRemark()));
        candidate.setLastContactTime(bo.getLastContactTime());
        candidate.setNextStepTime(bo.getNextStepTime());
    }

    private void applyUpdateFields(Candidate candidate, CandidateUpdateBO bo) {
        if (bo.getName() != null) candidate.setName(requireName(bo.getName()));
        if (bo.getAvatar() != null) candidate.setAvatar(normalizeText(bo.getAvatar()));
        if (bo.getPhone() != null) candidate.setPhone(normalizeText(bo.getPhone()));
        if (bo.getEmail() != null) candidate.setEmail(normalizeEmail(bo.getEmail()));
        if (bo.getWechat() != null) candidate.setWechat(normalizeText(bo.getWechat()));
        if (bo.getCurrentCompany() != null) candidate.setCurrentCompany(normalizeText(bo.getCurrentCompany()));
        if (bo.getCurrentPosition() != null) candidate.setCurrentPosition(normalizeText(bo.getCurrentPosition()));
        if (bo.getAppliedPosition() != null) candidate.setAppliedPosition(normalizeText(bo.getAppliedPosition()));
        if (bo.getJobRequirements() != null) candidate.setJobRequirements(normalizeLongText(bo.getJobRequirements()));
        if (bo.getRecruitmentJobId() != null) applyRecruitmentJob(candidate, bo.getRecruitmentJobId());
        if (bo.getStage() != null) candidate.setStage(normalizeText(bo.getStage()));
        if (bo.getSource() != null) candidate.setSource(normalizeText(bo.getSource()));
        if (bo.getEducation() != null) candidate.setEducation(normalizeText(bo.getEducation()));
        if (bo.getSchool() != null) candidate.setSchool(normalizeText(bo.getSchool()));
        if (bo.getMajor() != null) candidate.setMajor(normalizeText(bo.getMajor()));
        if (bo.getWorkYears() != null) candidate.setWorkYears(bo.getWorkYears());
        if (bo.getExpectedCity() != null) candidate.setExpectedCity(normalizeText(bo.getExpectedCity()));
        if (bo.getExpectedSalary() != null) candidate.setExpectedSalary(normalizeText(bo.getExpectedSalary()));
        if (bo.getSkillTags() != null) candidate.setSkillTags(normalizeText(bo.getSkillTags()));
        if (bo.getResumeSummary() != null) candidate.setResumeSummary(normalizeLongText(bo.getResumeSummary()));
        if (bo.getAiEvaluation() != null) candidate.setAiEvaluation(normalizeLongText(bo.getAiEvaluation()));
        if (bo.getOwnerId() != null) candidate.setOwnerId(bo.getOwnerId());
        if (bo.getRemark() != null) candidate.setRemark(normalizeLongText(bo.getRemark()));
        if (bo.getLastContactTime() != null) candidate.setLastContactTime(bo.getLastContactTime());
        if (bo.getNextStepTime() != null) candidate.setNextStepTime(bo.getNextStepTime());
    }

    private void applyDataScope(CandidateQueryBO queryBO) {
        DataPermissionContext context = dataPermissionService.createContextByPermission("candidate:view");
        queryBO.setAllData(context.isAllData());
        queryBO.setUserIds(context.getUserIds() == null ? Collections.emptyList() : new ArrayList<>(context.getUserIds()));
    }

    private void updateCustomFields(Long candidateId, Map<String, Object> customFields) {
        if (customFields != null && !customFields.isEmpty()) {
            customFieldService.updateCustomFieldValues(ENTITY_CANDIDATE, candidateId, customFields);
        }
    }

    private void fillCustomFields(List<CandidateListVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream()
                .map(CandidateListVO::getCandidateId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Map<String, Object>> values = customFieldService.getBatchCustomFieldValues(ENTITY_CANDIDATE, ids);
        records.forEach(record -> record.setCustomFields(values.getOrDefault(record.getCandidateId(), Collections.emptyMap())));
    }

    private List<TaskVO> queryCandidateTasks(Long candidateId) {
        TaskQueryBO queryBO = new TaskQueryBO();
        queryBO.setCandidateId(candidateId);
        queryBO.setPage(1);
        queryBO.setLimit(20);
        return taskMapper.queryPageList(queryBO.parse(), queryBO).getRecords();
    }

    private List<ScheduleVO> queryCandidateSchedules(Long candidateId) {
        ScheduleQueryBO queryBO = new ScheduleQueryBO();
        queryBO.setCandidateId(candidateId);
        queryBO.setType("interview");
        queryBO.setPage(1);
        queryBO.setLimit(20);
        return scheduleMapper.queryPageList(queryBO.parse(), queryBO).getRecords();
    }

    private List<KnowledgeVO> queryCandidateResumes(Long candidateId) {
        KnowledgeQueryBO queryBO = new KnowledgeQueryBO();
        queryBO.setCandidateId(candidateId);
        queryBO.setPage(1);
        queryBO.setLimit(20);
        return knowledgeMapper.queryPageList(queryBO.parse(), queryBO).getRecords();
    }

    private void completeCandidateListVO(CandidateListVO vo) {
        if (vo != null) {
            vo.setAvatarUrl(resolveFileUrl(vo.getAvatar()));
            vo.setStageName(stageName(vo.getStage()));
            if (StrUtil.isBlank(vo.getRecruitmentJobName())) {
                vo.setRecruitmentJobName(vo.getAppliedPosition());
            }
        }
    }

    private void completeCandidateDetailVO(CandidateDetailVO vo) {
        if (vo != null) {
            vo.setAvatarUrl(resolveFileUrl(vo.getAvatar()));
            vo.setStageName(stageName(vo.getStage()));
            if (vo.getRecruitmentJobId() != null) {
                try {
                    RecruitmentJobVO job = recruitmentJobService.getRecruitmentJobDetail(vo.getRecruitmentJobId());
                    vo.setRecruitmentJob(job);
                    if (StrUtil.isBlank(vo.getRecruitmentJobName())) {
                        vo.setRecruitmentJobName(job.getJobName());
                    }
                } catch (BusinessException ignored) {
                    // Keep candidate detail available even if a historical job was deleted.
                }
            }
            if (StrUtil.isBlank(vo.getRecruitmentJobName())) {
                vo.setRecruitmentJobName(vo.getAppliedPosition());
            }
        }
    }

    private Map<String, Object> buildUniqueFieldValues(Candidate candidate, Map<String, Object> customFields) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (candidate != null) {
            values.put("phone", candidate.getPhone());
            values.put("email", candidate.getEmail());
            values.put("name", candidate.getName());
        }
        if (customFields != null && !customFields.isEmpty()) {
            values.putAll(customFields);
        }
        return values;
    }

    private String resolveResumeText(CandidateResumeParseBO bo) {
        if (bo == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "简历内容不能为空");
        }
        if (StrUtil.isNotBlank(bo.getContent())) {
            return abbreviate(bo.getContent(), MAX_RESUME_TEXT_LENGTH);
        }
        if (StrUtil.isBlank(bo.getFilePath())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "简历内容不能为空");
        }
        try (InputStream inputStream = fileStorageService.getFileStream(bo.getFilePath())) {
            String text = DocumentTextExtractor.parseToString(inputStream, bo.getMimeType(), bo.getFileName());
            if (StrUtil.isBlank(text)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "无法提取简历文本");
            }
            return abbreviate(text, MAX_RESUME_TEXT_LENGTH);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("候选人简历文本提取失败: {}", bo.getFileName(), exception);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "无法提取简历文本");
        }
    }

    private Map<String, Object> parseResumeFields(String text, CandidateResumeParseBO bo) {
        Map<String, Object> fallback = fallbackParse(text, bo);
        try {
            String response = chatClientProvider.getChatClient()
                    .prompt()
                    .user(buildResumeParsePrompt(text, bo))
                    .call()
                    .content();
            Map<String, Object> aiFields = parseAiJson(response);
            fallback.putAll(removeBlankValues(aiFields));
        } catch (Exception exception) {
            log.info("AI 简历解析不可用，使用规则解析结果: {}", exception.getMessage());
        }
        fallback.putIfAbsent("resumeSummary", buildFallbackSummary(fallback));
        if (StrUtil.isBlank(asText(fallback.get("aiEvaluation")))) {
            fallback.put("aiEvaluation", buildFallbackEvaluation(fallback));
        }
        return fallback;
    }

    private String buildResumeParsePrompt(String text, CandidateResumeParseBO bo) {
        return """
                你是一名专业招聘助理。请从候选人简历文本中抽取结构化信息，只返回 JSON，不要返回解释。
                字段名固定为：
                name, phone, email, wechat, currentCompany, currentPosition, appliedPosition, education, school, major,
                workYears, expectedCity, expectedSalary, skillTags, resumeSummary, aiEvaluation。
                workYears 返回数字，skillTags 返回逗号分隔字符串。缺失字段返回空字符串。
                resumeSummary 概括简历事实，aiEvaluation 需要评估候选人与应聘岗位的匹配优势、风险点和建议面试验证方向。
                默认应聘岗位: %s

                简历文本:
                %s
                """.formatted(StrUtil.blankToDefault(bo.getAppliedPosition(), ""), abbreviate(text, SNAPSHOT_TEXT_LENGTH));
    }

    private Map<String, Object> parseAiJson(String response) throws Exception {
        String json = extractJsonObject(response);
        JsonNode root = objectMapper.readTree(json);
        Map<String, Object> values = new LinkedHashMap<>();
        root.fields().forEachRemaining(entry -> {
            JsonNode node = entry.getValue();
            if (node == null || node.isNull()) {
                return;
            }
            if (node.isNumber()) {
                values.put(entry.getKey(), node.decimalValue());
            } else if (node.isArray()) {
                List<String> items = new ArrayList<>();
                node.forEach(item -> {
                    if (item != null && !item.isNull() && StrUtil.isNotBlank(item.asText())) {
                        items.add(item.asText());
                    }
                });
                values.put(entry.getKey(), String.join(", ", items));
            } else {
                values.put(entry.getKey(), node.asText());
            }
        });
        return values;
    }

    private Map<String, Object> fallbackParse(String text, CandidateResumeParseBO bo) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("phone", findFirst(PHONE_PATTERN, text));
        values.put("email", findFirst(EMAIL_PATTERN, text));
        values.put("name", guessName(text, bo.getFileName()));
        values.put("appliedPosition", normalizeText(bo.getAppliedPosition()));
        values.put("source", StrUtil.blankToDefault(normalizeText(bo.getSource()), "resume_upload"));
        Matcher yearsMatcher = YEARS_PATTERN.matcher(StrUtil.blankToDefault(text, ""));
        if (yearsMatcher.find()) {
            values.put("workYears", new BigDecimal(yearsMatcher.group(1)));
        }
        values.put("resumeSummary", buildFallbackSummary(values));
        return removeBlankValues(values);
    }

    private void upsertParsedCandidate(CandidateResumeParseBO bo, Map<String, Object> parsed, CandidateResumeParseVO vo) {
        Candidate candidate = bo.getCandidateId() == null
                ? findMatchedCandidate(parsed)
                : getVisibleCandidate(bo.getCandidateId());
        Map<String, Object> conflicts = new LinkedHashMap<>();
        if (candidate == null) {
            candidate = createCandidateFromParsed(bo, parsed);
            vo.setCreated(true);
        } else {
            mergeParsedFields(candidate, parsed, conflicts);
            candidate.setAiParseSnapshot(toJson(parsed));
            candidate.setConflictSnapshot(conflicts.isEmpty() ? null : toJson(conflicts));
            updateById(candidate);
            vo.setUpdated(true);
        }
        vo.setCandidateId(candidate.getCandidateId());
        vo.setCandidateName(candidate.getName());
        vo.setConflicts(conflicts);
    }

    private Candidate findMatchedCandidate(Map<String, Object> parsed) {
        String phone = asText(parsed.get("phone"));
        String email = asText(parsed.get("email"));
        List<Candidate> matches = findByPhoneOrEmail(phone, email);
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        for (Candidate candidate : matches) {
            try {
                dataPermissionService.assertUserDataAccessByPermission("candidate:view", candidate.getOwnerId());
                return candidate;
            } catch (BusinessException ignored) {
                // Keep looking for a visible match.
            }
        }
        return null;
    }

    private Candidate createCandidateFromParsed(CandidateResumeParseBO bo, Map<String, Object> parsed) {
        CandidateAddBO addBO = new CandidateAddBO();
        addBO.setName(StrUtil.blankToDefault(asText(parsed.get("name")), guessName(bo.getContent(), bo.getFileName())));
        addBO.setPhone(asText(parsed.get("phone")));
        addBO.setEmail(asText(parsed.get("email")));
        addBO.setWechat(asText(parsed.get("wechat")));
        addBO.setCurrentCompany(asText(parsed.get("currentCompany")));
        addBO.setCurrentPosition(asText(parsed.get("currentPosition")));
        addBO.setAppliedPosition(firstNonBlank(asText(parsed.get("appliedPosition")), bo.getAppliedPosition()));
        addBO.setJobRequirements(asText(parsed.get("jobRequirements")));
        addBO.setSource(firstNonBlank(asText(parsed.get("source")), bo.getSource(), "resume_upload"));
        addBO.setEducation(asText(parsed.get("education")));
        addBO.setSchool(asText(parsed.get("school")));
        addBO.setMajor(asText(parsed.get("major")));
        addBO.setWorkYears(asDecimal(parsed.get("workYears")));
        addBO.setExpectedCity(asText(parsed.get("expectedCity")));
        addBO.setExpectedSalary(asText(parsed.get("expectedSalary")));
        addBO.setSkillTags(asText(parsed.get("skillTags")));
        addBO.setResumeSummary(asText(parsed.get("resumeSummary")));
        addBO.setAiEvaluation(asText(parsed.get("aiEvaluation")));
        Long candidateId = addCandidate(addBO);
        Candidate candidate = getById(candidateId);
        candidate.setAiParseSnapshot(toJson(parsed));
        updateById(candidate);
        return candidate;
    }

    private void mergeParsedFields(Candidate candidate, Map<String, Object> parsed, Map<String, Object> conflicts) {
        mergeText(candidate::getName, candidate::setName, "name", parsed, conflicts);
        mergeText(candidate::getPhone, candidate::setPhone, "phone", parsed, conflicts);
        mergeText(candidate::getEmail, candidate::setEmail, "email", parsed, conflicts);
        mergeText(candidate::getWechat, candidate::setWechat, "wechat", parsed, conflicts);
        mergeText(candidate::getCurrentCompany, candidate::setCurrentCompany, "currentCompany", parsed, conflicts);
        mergeText(candidate::getCurrentPosition, candidate::setCurrentPosition, "currentPosition", parsed, conflicts);
        mergeText(candidate::getAppliedPosition, candidate::setAppliedPosition, "appliedPosition", parsed, conflicts);
        mergeText(candidate::getJobRequirements, candidate::setJobRequirements, "jobRequirements", parsed, conflicts);
        mergeText(candidate::getSource, candidate::setSource, "source", parsed, conflicts);
        mergeText(candidate::getEducation, candidate::setEducation, "education", parsed, conflicts);
        mergeText(candidate::getSchool, candidate::setSchool, "school", parsed, conflicts);
        mergeText(candidate::getMajor, candidate::setMajor, "major", parsed, conflicts);
        mergeText(candidate::getExpectedCity, candidate::setExpectedCity, "expectedCity", parsed, conflicts);
        mergeText(candidate::getExpectedSalary, candidate::setExpectedSalary, "expectedSalary", parsed, conflicts);
        mergeText(candidate::getSkillTags, candidate::setSkillTags, "skillTags", parsed, conflicts);
        mergeText(candidate::getResumeSummary, candidate::setResumeSummary, "resumeSummary", parsed, conflicts);
        mergeText(candidate::getAiEvaluation, candidate::setAiEvaluation, "aiEvaluation", parsed, conflicts);
        if (candidate.getRecruitmentJobId() == null) {
            Long recruitmentJobId = findRecruitmentJobIdByName(candidate.getAppliedPosition());
            if (recruitmentJobId != null) {
                applyRecruitmentJob(candidate, recruitmentJobId);
            }
        }
        BigDecimal workYears = asDecimal(parsed.get("workYears"));
        if (workYears != null) {
            if (candidate.getWorkYears() == null) {
                candidate.setWorkYears(workYears);
            } else if (candidate.getWorkYears().compareTo(workYears) != 0) {
                conflicts.put("workYears", conflict(candidate.getWorkYears(), workYears));
            }
        }
    }

    private void mergeText(java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter,
                           String fieldName, Map<String, Object> parsed, Map<String, Object> conflicts) {
        String next = normalizeLongText(asText(parsed.get(fieldName)));
        if (StrUtil.isBlank(next)) {
            return;
        }
        String current = getter.get();
        if (StrUtil.isBlank(current)) {
            setter.accept(next);
        } else if (!Objects.equals(StrUtil.trim(current), next)) {
            conflicts.put(fieldName, conflict(current, next));
        }
    }

    private Map<String, Object> conflict(Object currentValue, Object parsedValue) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("current", currentValue);
        map.put("parsed", parsedValue);
        return map;
    }

    private boolean isCoreField(String fieldName) {
        return Set.of("name", "avatar", "phone", "email", "wechat", "currentCompany", "currentPosition",
                "appliedPosition", "jobRequirements", "recruitmentJobId", "stage", "source", "education", "school", "major", "workYears",
                "expectedCity", "expectedSalary", "skillTags", "resumeSummary", "aiEvaluation",
                "ownerId", "remark", "lastContactTime", "nextStepTime").contains(fieldName);
    }

    private void applyCoreField(Candidate candidate, String fieldName, Object value) {
        switch (fieldName) {
            case "name" -> candidate.setName(requireName(asText(value)));
            case "avatar" -> candidate.setAvatar(normalizeText(asText(value)));
            case "phone" -> candidate.setPhone(normalizeText(asText(value)));
            case "email" -> candidate.setEmail(normalizeEmail(asText(value)));
            case "wechat" -> candidate.setWechat(normalizeText(asText(value)));
            case "currentCompany" -> candidate.setCurrentCompany(normalizeText(asText(value)));
            case "currentPosition" -> candidate.setCurrentPosition(normalizeText(asText(value)));
            case "appliedPosition" -> candidate.setAppliedPosition(normalizeText(asText(value)));
            case "jobRequirements" -> candidate.setJobRequirements(normalizeLongText(asText(value)));
            case "recruitmentJobId" -> applyRecruitmentJob(candidate, parseLong(value));
            case "stage" -> candidate.setStage(normalizeStage(asText(value)));
            case "source" -> candidate.setSource(normalizeText(asText(value)));
            case "education" -> candidate.setEducation(normalizeText(asText(value)));
            case "school" -> candidate.setSchool(normalizeText(asText(value)));
            case "major" -> candidate.setMajor(normalizeText(asText(value)));
            case "workYears" -> candidate.setWorkYears(asDecimal(value));
            case "expectedCity" -> candidate.setExpectedCity(normalizeText(asText(value)));
            case "expectedSalary" -> candidate.setExpectedSalary(normalizeText(asText(value)));
            case "skillTags" -> candidate.setSkillTags(normalizeText(asText(value)));
            case "resumeSummary" -> candidate.setResumeSummary(normalizeLongText(asText(value)));
            case "aiEvaluation" -> candidate.setAiEvaluation(normalizeLongText(asText(value)));
            case "ownerId" -> candidate.setOwnerId(parseLong(value));
            case "remark" -> candidate.setRemark(normalizeLongText(asText(value)));
            default -> {
            }
        }
    }

    private void applyRecruitmentJob(Candidate candidate, Long recruitmentJobId) {
        if (recruitmentJobId == null) {
            candidate.setRecruitmentJobId(null);
            return;
        }
        RecruitmentJob job = recruitmentJobService.getVisibleRecruitmentJob(recruitmentJobId);
        candidate.setRecruitmentJobId(job.getRecruitmentJobId());
        candidate.setAppliedPosition(job.getJobName());
        candidate.setJobRequirements(job.getRequirements());
    }

    private Long findRecruitmentJobIdByName(String jobName) {
        String normalized = normalizeText(jobName);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        RecruitmentJob job = recruitmentJobService.getOne(new LambdaQueryWrapper<RecruitmentJob>()
                .eq(RecruitmentJob::getDelFlag, 0)
                .eq(RecruitmentJob::getStatus, "open")
                .eq(RecruitmentJob::getJobName, normalized)
                .last("LIMIT 1"), false);
        return job == null ? null : job.getRecruitmentJobId();
    }

    private Long resolveOwnerId(Long ownerId) {
        return ownerId == null ? UserUtil.getUserId() : ownerId;
    }

    private String normalizeStage(String stage) {
        String normalized = normalizeStageAlias(stage);
        List<FieldOption> options = customFieldService.getFieldOptions(ENTITY_CANDIDATE, "stage");
        boolean allowed = options == null || options.isEmpty() || options.stream()
                .filter(Objects::nonNull)
                .map(FieldOption::getValue)
                .filter(StrUtil::isNotBlank)
                .anyMatch(value -> StrUtil.equals(value, normalized));
        if (!allowed) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "无效的候选人阶段: " + stage);
        }
        return normalized;
    }

    private String normalizeStageAlias(String stage) {
        String normalized = StrUtil.blankToDefault(StrUtil.trim(stage), DEFAULT_STAGE);
        String compact = normalized.replaceAll("[\\s,，。.!！;；:：\"“”'‘’、]", "").toLowerCase(Locale.ROOT);
        if (Set.of("new", "新候选人", "新建候选人").contains(compact)) {
            return "new";
        }
        if (Set.of("screening", "初筛", "筛选").contains(compact)) {
            return "screening";
        }
        if (Set.of("interview_scheduled", "安排面试", "待面试", "约面试", "面试中", "进入面试").contains(compact)) {
            return "interview_scheduled";
        }
        if (Set.of("interview_passed", "面试通过", "通过面试").contains(compact)) {
            return "interview_passed";
        }
        if (Set.of("offer", "发offer", "给offer").contains(compact)) {
            return "offer";
        }
        if (Set.of("hired", "已入职", "入职", "入职了", "录用").contains(compact)) {
            return "hired";
        }
        if (Set.of("rejected", "已淘汰", "淘汰", "淘汰了", "拒绝", "不合适", "面试不通过", "未通过", "pass掉").contains(compact)) {
            return "rejected";
        }
        List<FieldOption> options = customFieldService.getFieldOptions(ENTITY_CANDIDATE, "stage");
        if (options != null) {
            for (FieldOption option : options) {
                if (option == null) {
                    continue;
                }
                if (StrUtil.equals(option.getValue(), normalized) || StrUtil.equals(option.getLabel(), normalized)) {
                    return option.getValue();
                }
            }
        }
        return normalized;
    }

    private String stageName(String stage) {
        if (stage == null) {
            return "新候选人";
        }
        String label = customFieldService.resolveOptionLabel(ENTITY_CANDIDATE, "stage", stage);
        return StrUtil.isNotBlank(label) ? label : STAGE_LABEL_FALLBACK.getOrDefault(stage, stage);
    }

    private String requireName(String name) {
        String normalized = normalizeText(name);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "候选人姓名不能为空");
        }
        return normalized;
    }

    private String normalizeText(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

    private String resolveFileUrl(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        if (isExternalUrl(filePath)) {
            return StrUtil.trim(filePath);
        }
        try {
            return fileStorageService.getUrl(filePath);
        } catch (Exception e) {
            log.warn("Resolve candidate avatar URL failed: {}", e.getMessage());
            return null;
        }
    }

    private boolean isExternalUrl(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.startWithIgnoreCase(normalized, "http://")
                || StrUtil.startWithIgnoreCase(normalized, "https://");
    }

    private String normalizeLongText(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeText(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String asText(Object value) {
        return value == null ? null : StrUtil.trim(String.valueOf(value));
    }

    private BigDecimal asDecimal(Object value) {
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception exception) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return null;
        }
        return Long.parseLong(String.valueOf(value).trim());
    }

    private Map<String, Object> removeBlankValues(Map<String, Object> values) {
        Map<String, Object> cleaned = new LinkedHashMap<>();
        if (values == null) {
            return cleaned;
        }
        values.forEach((key, value) -> {
            if (value instanceof String text) {
                if (StrUtil.isNotBlank(text)) {
                    cleaned.put(key, StrUtil.trim(text));
                }
            } else if (value != null) {
                cleaned.put(key, value);
            }
        });
        return cleaned;
    }

    private String guessName(String text, String fileName) {
        if (StrUtil.isNotBlank(text)) {
            for (String line : text.split("\\R")) {
                String candidate = StrUtil.trim(line);
                if (StrUtil.isBlank(candidate) || candidate.length() > 20 || candidate.contains("@")
                        || candidate.contains(":") || candidate.contains("：")) {
                    continue;
                }
                if (candidate.matches("[\\u4E00-\\u9FA5A-Za-z·]{2,20}")) {
                    return candidate;
                }
            }
        }
        String baseName = StrUtil.blankToDefault(FileUtil.mainName(fileName), "未命名候选人");
        return baseName.length() > 30 ? baseName.substring(0, 30) : baseName;
    }

    private String findFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(StrUtil.blankToDefault(text, ""));
        if (!matcher.find()) {
            return null;
        }
        return matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
    }

    private String buildFallbackSummary(Map<String, Object> values) {
        List<String> parts = new ArrayList<>();
        addPart(parts, "姓名", asText(values.get("name")));
        addPart(parts, "当前职位", asText(values.get("currentPosition")));
        addPart(parts, "应聘岗位", asText(values.get("appliedPosition")));
        addPart(parts, "学校", asText(values.get("school")));
        addPart(parts, "技能", asText(values.get("skillTags")));
        return parts.isEmpty() ? "已解析简历，等待补充候选人信息。" : String.join("；", parts);
    }

    private String buildFallbackEvaluation(Map<String, Object> values) {
        List<String> parts = new ArrayList<>();
        String appliedPosition = asText(values.get("appliedPosition"));
        String currentPosition = asText(values.get("currentPosition"));
        String skillTags = asText(values.get("skillTags"));
        String education = firstNonBlank(asText(values.get("education")), asText(values.get("school")));
        BigDecimal workYears = asDecimal(values.get("workYears"));
        if (StrUtil.isNotBlank(appliedPosition) || StrUtil.isNotBlank(currentPosition)) {
            parts.add("候选人当前背景与" + StrUtil.blankToDefault(appliedPosition, "目标岗位") + "存在初步匹配基础。");
        }
        if (workYears != null) {
            parts.add("已识别约 " + workYears.stripTrailingZeros().toPlainString() + " 年相关经验，可结合项目深度进一步确认胜任力。");
        }
        if (StrUtil.isNotBlank(skillTags)) {
            parts.add("技能关键词：" + skillTags + "。");
        }
        if (StrUtil.isNotBlank(education)) {
            parts.add("教育背景：" + education + "。");
        }
        if (parts.isEmpty()) {
            parts.add("已完成简历文本解析，建议补充岗位要求后继续评估匹配度、风险点和面试重点。");
        } else {
            parts.add("建议下一步围绕岗位硬性要求、项目真实性、沟通表达和到岗意愿进行面试验证。");
        }
        return String.join("\n", parts);
    }

    private void addPart(List<String> parts, String label, String value) {
        if (StrUtil.isNotBlank(value)) {
            parts.add(label + ": " + value);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return null;
    }

    private String extractJsonObject(String text) {
        String normalized = StrUtil.blankToDefault(text, "{}").trim();
        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return normalized.substring(start, end + 1);
        }
        return "{}";
    }

    private String abbreviate(String text, int maxLength) {
        if (StrUtil.isBlank(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n...(内容过长已截断)";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return null;
        }
    }
}
