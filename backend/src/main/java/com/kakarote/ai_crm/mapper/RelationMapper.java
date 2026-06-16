package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.RelationQueryBO;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.VO.RelationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 关系人Mapper。
 */
@Mapper
public interface RelationMapper extends BaseMapper<Relation> {

    /**
     * 分页查询关系人。
     */
    IPage<RelationVO> queryPageList(IPage<RelationVO> page,
                                    @Param("query") RelationQueryBO query,
                                    @Param("ownerUserId") Long ownerUserId);

    /**
     * 查询关系人详情基础信息。
     */
    RelationVO getRelationById(@Param("relationId") Long relationId,
                               @Param("ownerUserId") Long ownerUserId);
}
