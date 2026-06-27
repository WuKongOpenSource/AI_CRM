package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.RecruitmentJobAddBO;
import com.kakarote.ai_crm.entity.BO.RecruitmentJobQueryBO;
import com.kakarote.ai_crm.entity.BO.RecruitmentJobUpdateBO;
import com.kakarote.ai_crm.entity.PO.RecruitmentJob;
import com.kakarote.ai_crm.entity.VO.RecruitmentJobVO;
import com.kakarote.ai_crm.mapper.RecruitmentJobMapper;
import com.kakarote.ai_crm.service.IRecruitmentJobService;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class RecruitmentJobServiceImpl extends ServiceImpl<RecruitmentJobMapper, RecruitmentJob> implements IRecruitmentJobService {

    private static final String STATUS_OPEN = "open";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addRecruitmentJob(RecruitmentJobAddBO bo) {
        RecruitmentJob job = new RecruitmentJob();
        job.setJobName(requireJobName(bo.getJobName()));
        job.setDepartment(normalizeText(bo.getDepartment()));
        job.setHeadcount(bo.getHeadcount());
        job.setWorkYears(bo.getWorkYears());
        job.setEducation(normalizeText(bo.getEducation()));
        job.setCity(normalizeText(bo.getCity()));
        job.setSalaryRange(normalizeText(bo.getSalaryRange()));
        job.setSkillTags(normalizeText(bo.getSkillTags()));
        job.setResponsibilities(normalizeLongText(bo.getResponsibilities()));
        job.setRequirements(normalizeLongText(bo.getRequirements()));
        job.setStatus(normalizeStatus(bo.getStatus()));
        job.setOwnerId(resolveOwnerId(bo.getOwnerId()));
        job.setRemark(normalizeLongText(bo.getRemark()));
        job.setDelFlag(0);
        save(job);
        return job.getRecruitmentJobId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecruitmentJob(RecruitmentJobUpdateBO bo) {
        RecruitmentJob job = getVisibleRecruitmentJob(bo.getRecruitmentJobId());
        if (bo.getJobName() != null) job.setJobName(requireJobName(bo.getJobName()));
        if (bo.getDepartment() != null) job.setDepartment(normalizeText(bo.getDepartment()));
        if (bo.getHeadcount() != null) job.setHeadcount(bo.getHeadcount());
        if (bo.getWorkYears() != null) job.setWorkYears(bo.getWorkYears());
        if (bo.getEducation() != null) job.setEducation(normalizeText(bo.getEducation()));
        if (bo.getCity() != null) job.setCity(normalizeText(bo.getCity()));
        if (bo.getSalaryRange() != null) job.setSalaryRange(normalizeText(bo.getSalaryRange()));
        if (bo.getSkillTags() != null) job.setSkillTags(normalizeText(bo.getSkillTags()));
        if (bo.getResponsibilities() != null) job.setResponsibilities(normalizeLongText(bo.getResponsibilities()));
        if (bo.getRequirements() != null) job.setRequirements(normalizeLongText(bo.getRequirements()));
        if (bo.getStatus() != null) job.setStatus(normalizeStatus(bo.getStatus()));
        if (bo.getOwnerId() != null) job.setOwnerId(resolveOwnerId(bo.getOwnerId()));
        if (bo.getRemark() != null) job.setRemark(normalizeLongText(bo.getRemark()));
        updateById(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecruitmentJob(Long recruitmentJobId) {
        RecruitmentJob job = getVisibleRecruitmentJob(recruitmentJobId);
        job.setDelFlag(1);
        updateById(job);
    }

    @Override
    public BasePage<RecruitmentJobVO> queryPageList(RecruitmentJobQueryBO queryBO) {
        if (queryBO == null) {
            queryBO = new RecruitmentJobQueryBO();
        }
        BasePage<RecruitmentJob> page = queryBO.parse();
        page(page, buildQueryWrapper(queryBO));
        return page.copy(RecruitmentJobVO.class);
    }

    @Override
    public List<RecruitmentJobVO> listOptions(RecruitmentJobQueryBO queryBO) {
        if (queryBO == null) {
            queryBO = new RecruitmentJobQueryBO();
        }
        queryBO.setPage(1);
        queryBO.setLimit(100);
        return queryPageList(queryBO).getRecords();
    }

    @Override
    public RecruitmentJobVO getRecruitmentJobDetail(Long recruitmentJobId) {
        return BeanUtil.copyProperties(getVisibleRecruitmentJob(recruitmentJobId), RecruitmentJobVO.class);
    }

    @Override
    public RecruitmentJob getVisibleRecruitmentJob(Long recruitmentJobId) {
        RecruitmentJob job = getById(recruitmentJobId);
        if (job == null || Objects.equals(job.getDelFlag(), 1)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "招聘岗位不存在");
        }
        return job;
    }

    private LambdaQueryWrapper<RecruitmentJob> buildQueryWrapper(RecruitmentJobQueryBO queryBO) {
        LambdaQueryWrapper<RecruitmentJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecruitmentJob::getDelFlag, 0);
        if (StrUtil.isNotBlank(queryBO.getStatus())) {
            wrapper.eq(RecruitmentJob::getStatus, normalizeStatus(queryBO.getStatus()));
        }
        if (queryBO.getOwnerId() != null) {
            wrapper.eq(RecruitmentJob::getOwnerId, queryBO.getOwnerId());
        }
        if (StrUtil.isNotBlank(queryBO.getKeyword())) {
            String keyword = StrUtil.trim(queryBO.getKeyword());
            wrapper.and(item -> item
                    .like(RecruitmentJob::getJobName, keyword)
                    .or().like(RecruitmentJob::getDepartment, keyword)
                    .or().like(RecruitmentJob::getCity, keyword)
                    .or().like(RecruitmentJob::getSkillTags, keyword)
                    .or().like(RecruitmentJob::getRequirements, keyword));
        }
        wrapper.orderByDesc(RecruitmentJob::getUpdateTime)
                .orderByDesc(RecruitmentJob::getCreateTime)
                .orderByDesc(RecruitmentJob::getRecruitmentJobId);
        return wrapper;
    }

    private String requireJobName(String value) {
        String normalized = normalizeText(value);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "岗位名称不能为空");
        }
        return StrUtil.maxLength(normalized, 150);
    }

    private String normalizeStatus(String status) {
        String value = StrUtil.blankToDefault(normalizeText(status), STATUS_OPEN);
        if (!List.of("open", "paused", "closed").contains(value)) {
            return STATUS_OPEN;
        }
        return value;
    }

    private Long resolveOwnerId(Long ownerId) {
        return ownerId != null ? ownerId : UserUtil.getUserIdOrNull();
    }

    private String normalizeText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private String normalizeLongText(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : StrUtil.maxLength(normalized, 5000);
    }
}
