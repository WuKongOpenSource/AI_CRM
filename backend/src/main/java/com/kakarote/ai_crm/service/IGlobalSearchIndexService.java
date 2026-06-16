package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.entity.PO.GlobalSearchIndex;
import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;

public interface IGlobalSearchIndexService extends IService<GlobalSearchIndex> {

    /**
     * 分页查询全局搜索索引列表。
     */
    BasePage<GlobalSearchResultVO> queryPageList(GlobalSearchQueryBO queryBO);

    /**
     * 刷新客户索引。
     */
    void refreshCustomerIndex(Long customerId);

    /**
     * 刷新客户关联索引。
     */
    void refreshCustomerRelatedIndexes(Long customerId);

    /**
     * 刷新联系人索引。
     */
    void refreshContactIndex(Long contactId);

    /**
     * 刷新关系人索引。
     */
    void refreshRelationIndex(Long relationId);

    void refreshProductIndex(Long productId);

    /**
     * 刷新任务索引。
     */
    void refreshTaskIndex(Long taskId);

    /**
     * 刷新日程索引。
     */
    void refreshScheduleIndex(Long scheduleId);

    /**
     * 刷新知识索引。
     */
    void refreshKnowledgeIndex(Long knowledgeId);

    /**
     * 删除按Entity。
     */
    void deleteByEntity(String entityType, Long entityId);

    /**
     * 删除联系人索引按客户ID。
     */
    void deleteContactIndexesByCustomerId(Long customerId);
}
