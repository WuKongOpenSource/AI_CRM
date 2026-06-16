package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 知识库Mapper
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {

    /**
     * 分页查询知识库
     */
    IPage<KnowledgeVO> queryPageList(IPage<KnowledgeVO> page, @Param("query") KnowledgeQueryBO query);

    /**
     * 查询知识详情
     */
    KnowledgeVO getKnowledgeById(@Param("knowledgeId") Long knowledgeId);

    /**
     * 查询按ID忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Select("SELECT * FROM crm_knowledge WHERE knowledge_id = #{knowledgeId}")
    Knowledge selectByIdIgnoreDataPermission(@Param("knowledgeId") Long knowledgeId);

    /**
     * 查询按客户ID忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Select("SELECT * FROM crm_knowledge WHERE customer_id = #{customerId}")
    List<Knowledge> selectByCustomerIdIgnoreDataPermission(@Param("customerId") Long customerId);

    /**
     * 更新WEKnora信息忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Update("""
            UPDATE crm_knowledge
            SET weknora_knowledge_id = #{weKnoraKnowledgeId},
                weknora_parse_status = #{parseStatus}
            WHERE knowledge_id = #{knowledgeId}
            """)
    int updateWeKnoraInfoIgnoreDataPermission(@Param("knowledgeId") Long knowledgeId,
                                              @Param("weKnoraKnowledgeId") String weKnoraKnowledgeId,
                                              @Param("parseStatus") String parseStatus);

    /**
     * 更新解析状态忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Update("""
            UPDATE crm_knowledge
            SET weknora_parse_status = #{parseStatus}
            WHERE knowledge_id = #{knowledgeId}
            """)
    int updateParseStatusIgnoreDataPermission(@Param("knowledgeId") Long knowledgeId,
                                              @Param("parseStatus") String parseStatus);
}
