package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.RelationAddBO;
import com.kakarote.ai_crm.entity.BO.RelationQueryBO;
import com.kakarote.ai_crm.entity.BO.RelationUpdateBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.VO.RelationDetailVO;
import com.kakarote.ai_crm.entity.VO.RelationVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.RelationMapper;
import com.kakarote.ai_crm.service.IRelationService;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Service
public class RelationServiceImpl extends ServiceImpl<RelationMapper, Relation> implements IRelationService {

    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_CUSTOMER_CONTACT = "customer_contact";
    private static final String RELATION_TYPE_OTHER = "other";

    private final RelationMapper relationMapper;
    private final ContactMapper contactMapper;
    private final CustomerMapper customerMapper;

    public RelationServiceImpl(RelationMapper relationMapper,
                               ContactMapper contactMapper,
                               CustomerMapper customerMapper) {
        this.relationMapper = relationMapper;
        this.contactMapper = contactMapper;
        this.customerMapper = customerMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addRelation(RelationAddBO relationAddBO) {
        Relation relation = BeanUtil.copyProperties(relationAddBO, Relation.class);
        Long currentUserId = UserUtil.getUserId();
        relation.setStatus(1);
        relation.setSource(SOURCE_MANUAL);
        relation.setCreateUserId(currentUserId);
        relation.setUpdateUserId(currentUserId);
        if (StrUtil.isBlank(relation.getRelationType())) {
            relation.setRelationType(RELATION_TYPE_OTHER);
        }
        normalizeLinkedCustomer(relation);
        relationMapper.insert(relation);
        return relation.getRelationId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRelation(RelationUpdateBO relationUpdateBO) {
        Relation relation = getOwnedRelation(relationUpdateBO.getRelationId());
        BeanUtil.copyProperties(relationUpdateBO, relation, "relationId", "source",
                "sourceCustomerId", "sourceContactId", "createUserId", "createTime", "customFields");
        if (StrUtil.isBlank(relation.getRelationType())) {
            relation.setRelationType(RELATION_TYPE_OTHER);
        }
        relation.setUpdateUserId(UserUtil.getUserId());
        normalizeLinkedCustomer(relation);
        relationMapper.updateById(relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRelation(Long relationId) {
        Relation relation = getOwnedRelation(relationId);
        relation.setStatus(0);
        relation.setUpdateUserId(UserUtil.getUserId());
        relationMapper.updateById(relation);
    }

    @Override
    public BasePage<RelationVO> queryPageList(RelationQueryBO queryBO) {
        BasePage<RelationVO> page = queryBO.parse();
        relationMapper.queryPageList(page, queryBO, UserUtil.getUserId());
        page.getRecords().forEach(this::completeRelationVO);
        return page;
    }

    @Override
    public RelationDetailVO detail(Long relationId) {
        RelationDetailVO detailVO = new RelationDetailVO();
        detailVO.setRelation(getOwnedRelationVO(relationId));
        detailVO.setTasks(Collections.emptyList());
        detailVO.setSchedules(Collections.emptyList());
        detailVO.setAttachments(Collections.emptyList());
        detailVO.setHistories(Collections.emptyList());
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFromContact(Long contactId) {
        Long currentUserId = UserUtil.getUserId();
        Relation existing = relationMapper.selectOne(Wrappers.<Relation>lambdaQuery()
                .eq(Relation::getSourceContactId, contactId)
                .eq(Relation::getCreateUserId, currentUserId)
                .eq(Relation::getStatus, 1)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getRelationId();
        }

        Contact contact = contactMapper.selectById(contactId);
        if (ObjectUtil.isNull(contact) || Objects.equals(contact.getStatus(), 0)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Contact does not exist");
        }

        Relation relation = new Relation();
        relation.setName(contact.getName());
        relation.setPhone(contact.getPhone());
        relation.setEmail(contact.getEmail());
        relation.setWechat(contact.getWechat());
        relation.setRemark(contact.getNotes());
        relation.setRelationType(SOURCE_CUSTOMER_CONTACT);
        relation.setSource(SOURCE_CUSTOMER_CONTACT);
        relation.setCustomerId(contact.getCustomerId());
        relation.setSourceCustomerId(contact.getCustomerId());
        relation.setSourceContactId(contact.getContactId());
        relation.setStatus(1);
        relation.setCreateUserId(currentUserId);
        relation.setUpdateUserId(currentUserId);
        normalizeLinkedCustomer(relation);
        relationMapper.insert(relation);
        return relation.getRelationId();
    }

    @Override
    public Relation getOwnedRelation(Long relationId) {
        Relation relation = relationMapper.selectById(relationId);
        if (ObjectUtil.isNull(relation) || Objects.equals(relation.getStatus(), 0)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Relation does not exist");
        }
        Long currentUserId = UserUtil.getUserId();
        if (relation.getCreateUserId() != null && !Objects.equals(relation.getCreateUserId(), currentUserId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Relation does not exist");
        }
        return relation;
    }

    @Override
    public RelationVO getOwnedRelationVO(Long relationId) {
        Relation relation = getOwnedRelation(relationId);
        RelationVO relationVO = relationMapper.getRelationById(relationId, UserUtil.getUserId());
        if (relationVO == null) {
            relationVO = BeanUtil.copyProperties(relation, RelationVO.class);
        }
        completeRelationVO(relationVO);
        return relationVO;
    }

    private void completeRelationVO(RelationVO vo) {
        if (vo == null) {
            return;
        }
        vo.setRelationTypeName(resolveRelationTypeName(vo.getRelationType()));
        vo.setSourceName(resolveSourceName(vo.getSource()));
        vo.setCustomFields(Collections.emptyMap());
        fillCustomerName(vo);
    }

    private void fillCustomerName(RelationVO vo) {
        if (vo.getCustomerId() == null || StrUtil.isNotBlank(vo.getCustomerName())) {
            return;
        }
        Customer customer = customerMapper.selectById(vo.getCustomerId());
        if (customer != null && !Objects.equals(customer.getStatus(), 0)) {
            vo.setCustomerName(customer.getCompanyName());
        }
    }

    private String resolveRelationTypeName(String relationType) {
        if (StrUtil.isBlank(relationType)) {
            return "Other";
        }
        Map<String, String> labels = Map.of(
                SOURCE_CUSTOMER_CONTACT, "Customer contact",
                "decision_maker", "Decision maker",
                "influencer", "Influencer",
                "partner", "Partner",
                RELATION_TYPE_OTHER, "Other"
        );
        return labels.getOrDefault(relationType, relationType);
    }

    private String resolveSourceName(String source) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        Map<String, String> labels = Map.of(
                SOURCE_MANUAL, "Manual",
                SOURCE_CUSTOMER_CONTACT, "Customer contact"
        );
        return labels.getOrDefault(source, source);
    }

    private void normalizeLinkedCustomer(Relation relation) {
        relation.setCompany(null);
        if (relation.getCustomerId() == null) {
            return;
        }
        Customer customer = customerMapper.selectById(relation.getCustomerId());
        if (customer == null || Objects.equals(customer.getStatus(), 0)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Customer does not exist");
        }
    }
}
