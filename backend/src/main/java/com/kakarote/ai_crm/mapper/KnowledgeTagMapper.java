package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.KnowledgeTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库标签Mapper
 */
@Mapper
public interface KnowledgeTagMapper extends BaseMapper<KnowledgeTag> {

    /**
     * 查询知识库标签列表
     */
    List<String> getTagsByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    /**
     * 删除知识库标签
     */
    int deleteByKnowledgeId(@Param("knowledgeId") Long knowledgeId);
}
