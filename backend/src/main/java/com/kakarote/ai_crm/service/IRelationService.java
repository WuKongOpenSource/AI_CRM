package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.RelationAddBO;
import com.kakarote.ai_crm.entity.BO.RelationQueryBO;
import com.kakarote.ai_crm.entity.BO.RelationUpdateBO;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.VO.RelationDetailVO;
import com.kakarote.ai_crm.entity.VO.RelationVO;

/**
 * 关系人服务接口。
 */
public interface IRelationService extends IService<Relation> {

    Long addRelation(RelationAddBO relationAddBO);

    void updateRelation(RelationUpdateBO relationUpdateBO);

    void deleteRelation(Long relationId);

    BasePage<RelationVO> queryPageList(RelationQueryBO queryBO);

    RelationDetailVO detail(Long relationId);

    Long addFromContact(Long contactId);

    Relation getOwnedRelation(Long relationId);

    RelationVO getOwnedRelationVO(Long relationId);
}
