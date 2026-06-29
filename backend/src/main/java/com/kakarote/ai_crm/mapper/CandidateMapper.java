package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.CandidateQueryBO;
import com.kakarote.ai_crm.entity.PO.Candidate;
import com.kakarote.ai_crm.entity.VO.CandidateDetailVO;
import com.kakarote.ai_crm.entity.VO.CandidateListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CandidateMapper extends BaseMapper<Candidate> {

    IPage<CandidateListVO> queryPageList(IPage<CandidateListVO> page, @Param("query") CandidateQueryBO query);

    CandidateDetailVO getCandidateById(@Param("candidateId") Long candidateId);

    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT *
            FROM crm_candidate
            WHERE candidate_id = #{candidateId}
            """)
    Candidate selectByIdIgnoreDataPermission(@Param("candidateId") Long candidateId);

    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT *
            FROM crm_candidate
            WHERE status = 1
              AND (
                  (#{phone} IS NOT NULL AND #{phone} <> '' AND phone = #{phone})
                  OR (#{email} IS NOT NULL AND #{email} <> '' AND LOWER(email) = LOWER(#{email}))
              )
            ORDER BY update_time DESC NULLS LAST, create_time DESC
            LIMIT 5
            """)
    List<Candidate> selectByPhoneOrEmailIgnoreDataPermission(@Param("phone") String phone,
                                                             @Param("email") String email);
}
