package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
