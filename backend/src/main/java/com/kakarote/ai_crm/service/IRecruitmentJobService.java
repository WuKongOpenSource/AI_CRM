package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.RecruitmentJobAddBO;
import com.kakarote.ai_crm.entity.BO.RecruitmentJobQueryBO;
import com.kakarote.ai_crm.entity.BO.RecruitmentJobUpdateBO;
import com.kakarote.ai_crm.entity.PO.RecruitmentJob;
import com.kakarote.ai_crm.entity.VO.RecruitmentJobVO;

import java.util.List;

public interface IRecruitmentJobService extends IService<RecruitmentJob> {

    Long addRecruitmentJob(RecruitmentJobAddBO bo);

    void updateRecruitmentJob(RecruitmentJobUpdateBO bo);

    void deleteRecruitmentJob(Long recruitmentJobId);

    BasePage<RecruitmentJobVO> queryPageList(RecruitmentJobQueryBO queryBO);

    List<RecruitmentJobVO> listOptions(RecruitmentJobQueryBO queryBO);

    RecruitmentJobVO getRecruitmentJobDetail(Long recruitmentJobId);

    RecruitmentJob getVisibleRecruitmentJob(Long recruitmentJobId);
}
