package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.RelationQueryBO;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.VO.RelationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RelationMapper extends BaseMapper<Relation> {

    IPage<RelationVO> queryPageList(IPage<RelationVO> page,
                                    @Param("query") RelationQueryBO query,
                                    @Param("ownerUserId") Long ownerUserId);

    RelationVO getRelationById(@Param("relationId") Long relationId,
                               @Param("ownerUserId") Long ownerUserId);
}
