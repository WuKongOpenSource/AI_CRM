package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.PO.CustomFieldPool;

/**
 * 自定义字段池服务接口
 */
public interface ICustomFieldPoolService extends IService<CustomFieldPool> {

    /**
     * 获取一个可用的字段池槽位。
     * 优先复用池中已有的、当前实体未占用的同类型列；
     * 若没有可用的，生成新列名并执行 ALTER TABLE。
     *
     * @param entityType 实体类型（customer / contact）
     * @param fieldType  逻辑字段类型（text / number / date 等）
     * @return 字段池记录
     */
    CustomFieldPool acquireSlot(String entityType, String fieldType);
}
