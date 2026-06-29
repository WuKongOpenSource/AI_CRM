package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.CandidateAddBO;
import com.kakarote.ai_crm.entity.BO.CandidateFieldUpdateBO;
import com.kakarote.ai_crm.entity.BO.CandidateQueryBO;
import com.kakarote.ai_crm.entity.BO.CandidateResumeParseBO;
import com.kakarote.ai_crm.entity.BO.CandidateUpdateBO;
import com.kakarote.ai_crm.entity.PO.Candidate;
import com.kakarote.ai_crm.entity.VO.CandidateDetailVO;
import com.kakarote.ai_crm.entity.VO.CandidateListVO;
import com.kakarote.ai_crm.entity.VO.CandidateResumeParseVO;

import java.util.List;

public interface ICandidateService extends IService<Candidate> {

    Long addCandidate(CandidateAddBO bo);

    void updateCandidate(CandidateUpdateBO bo);

    CandidateDetailVO updateCandidateField(CandidateFieldUpdateBO bo);

    void deleteCandidate(Long candidateId);

    BasePage<CandidateListVO> queryPageList(CandidateQueryBO queryBO);

    CandidateDetailVO getCandidateDetail(Long candidateId);

    void updateStage(Long candidateId, String stage);

    Candidate getVisibleCandidate(Long candidateId);

    CandidateDetailVO getVisibleCandidateVO(Long candidateId);

    List<Candidate> findByPhoneOrEmail(String phone, String email);

    CandidateResumeParseVO aiParseResume(CandidateResumeParseBO bo);

    CandidateResumeParseVO parseAndUpsertResumeAttachment(String fileName, String filePath,
                                                          Long fileSize, String mimeType,
                                                          Long candidateId);
}
