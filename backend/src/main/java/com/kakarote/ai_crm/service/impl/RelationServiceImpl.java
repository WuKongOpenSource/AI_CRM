package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.FollowUpQueryBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.BO.RelationAddBO;
import com.kakarote.ai_crm.entity.BO.RelationQueryBO;
import com.kakarote.ai_crm.entity.BO.RelationUpdateBO;
import com.kakarote.ai_crm.entity.BO.ScheduleQueryBO;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.VO.RelationDetailVO;
import com.kakarote.ai_crm.entity.VO.RelationVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.FollowUpMapper;
import com.kakarote.ai_crm.mapper.KnowledgeMapper;
import com.kakarote.ai_crm.mapper.RelationMapper;
import com.kakarote.ai_crm.mapper.ScheduleMapper;
import com.kakarote.ai_crm.mapper.TaskMapper;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.IRelationService;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 关系人服务实现。
 */
@Service
public class RelationServiceImpl extends ServiceImpl<RelationMapper, Relation> implements IRelationService {

    private static final String ENTITY_RELATION = "relation";
    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_CUSTOMER_CONTACT = "customer_contact";
    private static final String RELATION_TYPE_OTHER = "other";

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerLogoService customerLogoService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private FollowUpMapper followUpMapper;

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    private ICustomFieldService customFieldService;

    @Autowired
    private IGlobalSearchIndexService globalSearchIndexService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addRelation(RelationAddBO relationAddBO) {
        Relation relation = BeanUtil.copyProperties(relationAddBO, Relation.class);
        relation.setStatus(1);
        relation.setSource(SOURCE_MANUAL);
        if (StrUtil.isBlank(relation.getRelationType())) {
            relation.setRelationType(RELATION_TYPE_OTHER);
        }
        normalizeLinkedCustomerFields(relation);
        customFieldService.validateUniqueCustomFieldValues(ENTITY_RELATION, null,
                buildRelationUniqueFieldValues(relation, relationAddBO.getCustomFields()));
        save(relation);
        updateCustomFields(relation.getRelationId(), relationAddBO.getCustomFields());
        refreshRelationIndex(relation.getRelationId());
        return relation.getRelationId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRelation(RelationUpdateBO relationUpdateBO) {
        Relation relation = getOwnedRelation(relationUpdateBO.getRelationId());
        BeanUtil.copyProperties(relationUpdateBO, relation, "relationId", "source",
                "sourceCustomerId", "sourceContactId", "createUserId", "createTime", "customFields");
        normalizeLinkedCustomerFields(relation);
        customFieldService.validateUniqueCustomFieldValues(ENTITY_RELATION, relation.getRelationId(),
                buildRelationUniqueFieldValues(relation, relationUpdateBO.getCustomFields()));
        updateById(relation);
        updateCustomFields(relation.getRelationId(), relationUpdateBO.getCustomFields());
        refreshRelationIndex(relation.getRelationId());
    }

    @Override
    public void deleteRelation(Long relationId) {
        Relation relation = getOwnedRelation(relationId);
        relation.setStatus(0);
        updateById(relation);
        refreshRelationIndex(relation.getRelationId());
    }

    @Override
    public BasePage<RelationVO> queryPageList(RelationQueryBO queryBO) {
        BasePage<RelationVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO, UserUtil.getUserId());
        page.getRecords().forEach(this::completeRelationVO);
        fillCustomFields(page.getRecords());
        return page;
    }

    @Override
    public RelationDetailVO detail(Long relationId) {
        getOwnedRelation(relationId);
        RelationDetailVO detailVO = new RelationDetailVO();
        detailVO.setRelation(getOwnedRelationVO(relationId));
        detailVO.setTasks(queryRelationTasks(relationId));
        detailVO.setSchedules(queryRelationSchedules(relationId));
        detailVO.setAttachments(queryRelationAttachments(relationId));
        detailVO.setHistories(queryRelationHistories(relationId));
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFromContact(Long contactId) {
        Long currentUserId = UserUtil.getUserId();
        Relation existing = baseMapper.selectOne(Wrappers.<Relation>lambdaQuery()
                .eq(Relation::getSourceContactId, contactId)
                .eq(Relation::getCreateUserId, currentUserId)
                .eq(Relation::getStatus, 1));
        if (existing != null) {
            return existing.getRelationId();
        }

        Contact contact = contactMapper.selectById(contactId);
        if (ObjectUtil.isNull(contact)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "联系人不存在");
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
        normalizeLinkedCustomerFields(relation);
        relation.setStatus(1);
        save(relation);
        refreshRelationIndex(relation.getRelationId());
        return relation.getRelationId();
    }

    @Override
    public Relation getOwnedRelation(Long relationId) {
        Relation relation = getById(relationId);
        if (ObjectUtil.isNull(relation) || Objects.equals(relation.getStatus(), 0)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "关系人不存在");
        }
        Long currentUserId = UserUtil.getUserId();
        if (relation.getCreateUserId() != null && !Objects.equals(relation.getCreateUserId(), currentUserId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "关系人不存在或无权限访问");
        }
        return relation;
    }

    @Override
    public RelationVO getOwnedRelationVO(Long relationId) {
        Relation relation = getOwnedRelation(relationId);
        RelationVO relationVO = baseMapper.getRelationById(relationId, UserUtil.getUserId());
        if (relationVO == null) {
            relationVO = toRelationVO(relation);
        } else {
            completeRelationVO(relationVO);
        }
        relationVO.setCustomFields(customFieldService.getCustomFieldValues(ENTITY_RELATION, relationId));
        return relationVO;
    }

    private RelationVO toRelationVO(Relation relation) {
        RelationVO vo = BeanUtil.copyProperties(relation, RelationVO.class);
        completeRelationVO(vo);
        return vo;
    }

    private void completeRelationVO(RelationVO vo) {
        if (vo == null) {
            return;
        }
        vo.setCompany(null);
        vo.setAvatarUrl(resolveCustomerLogoUrl(vo.getAvatar()));
        vo.setRelationTypeName(resolveRelationTypeName(vo.getRelationType()));
        vo.setSourceName(resolveSourceName(vo.getSource()));
        enrichRelationCustomerFields(vo, vo.getCustomerId());
    }

    private void fillCustomFields(List<RelationVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> relationIds = records.stream()
                .map(RelationVO::getRelationId)
                .filter(Objects::nonNull)
                .toList();
        if (relationIds.isEmpty()) {
            return;
        }
        Map<Long, Map<String, Object>> customFieldMap = customFieldService.getBatchCustomFieldValues(ENTITY_RELATION, relationIds);
        if (customFieldMap == null) {
            customFieldMap = Collections.emptyMap();
        }
        for (RelationVO record : records) {
            record.setCustomFields(customFieldMap.getOrDefault(record.getRelationId(), Collections.emptyMap()));
        }
    }

    private List<com.kakarote.ai_crm.entity.VO.TaskVO> queryRelationTasks(Long relationId) {
        if (taskMapper == null) {
            return Collections.emptyList();
        }
        TaskQueryBO queryBO = new TaskQueryBO();
        queryBO.setRelationId(relationId);
        queryBO.setPage(1);
        queryBO.setLimit(20);
        return taskMapper.queryPageList(queryBO.parse(), queryBO).getRecords();
    }

    private List<com.kakarote.ai_crm.entity.VO.ScheduleVO> queryRelationSchedules(Long relationId) {
        if (scheduleMapper == null) {
            return Collections.emptyList();
        }
        ScheduleQueryBO queryBO = new ScheduleQueryBO();
        queryBO.setRelationId(relationId);
        queryBO.setPage(1);
        queryBO.setLimit(20);
        return scheduleMapper.queryPageList(queryBO.parse(), queryBO).getRecords();
    }

    private List<com.kakarote.ai_crm.entity.VO.KnowledgeVO> queryRelationAttachments(Long relationId) {
        if (knowledgeMapper == null) {
            return Collections.emptyList();
        }
        KnowledgeQueryBO queryBO = new KnowledgeQueryBO();
        queryBO.setRelationId(relationId);
        queryBO.setPage(1);
        queryBO.setLimit(20);
        return knowledgeMapper.queryPageList(queryBO.parse(), queryBO).getRecords();
    }

    private List<com.kakarote.ai_crm.entity.VO.FollowUpVO> queryRelationHistories(Long relationId) {
        if (followUpMapper == null) {
            return Collections.emptyList();
        }
        FollowUpQueryBO queryBO = new FollowUpQueryBO();
        queryBO.setRelationId(relationId);
        queryBO.setPage(1);
        queryBO.setLimit(20);
        return followUpMapper.queryPageList(queryBO.parse(), queryBO).getRecords();
    }

    private void updateCustomFields(Long relationId, Map<String, Object> customFields) {
        if (customFields != null && !customFields.isEmpty()) {
            customFieldService.updateCustomFieldValues(ENTITY_RELATION, relationId, customFields);
        }
    }

    private void refreshRelationIndex(Long relationId) {
        if (globalSearchIndexService != null) {
            globalSearchIndexService.refreshRelationIndex(relationId);
        }
    }

    private Map<String, Object> buildRelationUniqueFieldValues(Relation relation, Map<String, Object> customFields) {
        Map<String, Object> values = new HashMap<>();
        if (relation != null) {
            values.put("name", relation.getName());
            values.put("phone", relation.getPhone());
            values.put("wechat", relation.getWechat());
            values.put("email", relation.getEmail());
            values.put("relationType", relation.getRelationType());
            values.put("customerId", relation.getCustomerId());
        }
        if (customFields != null && !customFields.isEmpty()) {
            values.putAll(customFields);
        }
        return values;
    }

    private String resolveRelationTypeName(String relationType) {
        if (StrUtil.isBlank(relationType)) {
            return "其他";
        }
        // 真相源：crm_custom_field.options（relation/relationType）
        return customFieldService.resolveOptionLabel("relation", "relationType", relationType);
    }

    private String resolveSourceName(String source) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        // 真相源：crm_custom_field.options（relation/source）
        return customFieldService.resolveOptionLabel("relation", "source", source);
    }

    private void normalizeLinkedCustomerFields(Relation relation) {
        if (relation == null) {
            return;
        }
        relation.setCompany(null);
        if (relation.getCustomerId() == null) {
            return;
        }
        Customer customer = customerMapper.selectById(relation.getCustomerId());
        if (customer == null || Objects.equals(customer.getStatus(), 0)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
        }
    }

    private void enrichRelationCustomerFields(RelationVO vo, Long customerId) {
        if (vo == null || customerId == null) {
            return;
        }
        String customerName = vo.getCustomerName();
        String logo = vo.getCustomerLogo();
        if (StrUtil.isBlank(customerName) || StrUtil.isBlank(logo)) {
            Customer customer = customerMapper.selectByIdIgnoreDataPermission(customerId);
            if (customer != null && !Objects.equals(customer.getStatus(), 0)) {
                if (StrUtil.isBlank(customerName)) {
                    customerName = customer.getCompanyName();
                }
                if (StrUtil.isBlank(logo)) {
                    logo = customer.getLogo();
                }
            }
        }
        String logoUrl = resolveCustomerLogoUrl(logo);
        vo.setCustomerId(customerId);
        vo.setCustomerName(customerName);
        vo.setCustomerLogo(logo);
        vo.setCustomerLogoUrl(logoUrl);
    }

    private String resolveCustomerLogoUrl(String logo) {
        if (StrUtil.isBlank(logo) || customerLogoService == null) {
            return null;
        }
        try {
            return customerLogoService.resolveLogoUrl(logo);
        } catch (Exception ignored) {
            return null;
        }
    }
}
