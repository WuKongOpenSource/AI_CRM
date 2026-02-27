package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.CustomField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 自定义字段Mapper
 */
@Mapper
public interface CustomFieldMapper extends BaseMapper<CustomField> {

    /**
     * 查询实体的自定义字段值
     *
     * @param tableName  表名
     * @param idColumn   ID列名
     * @param entityId   实体ID
     * @param columns    自定义字段列名列表
     * @return 自定义字段值Map
     */
    Map<String, Object> getCustomFieldValues(
            @Param("tableName") String tableName,
            @Param("idColumn") String idColumn,
            @Param("entityId") Long entityId,
            @Param("columns") List<String> columns
    );

    /**
     * 更新实体的自定义字段值
     *
     * @param tableName  表名
     * @param idColumn   ID列名
     * @param entityId   实体ID
     * @param values     自定义字段值Map
     */
    void updateCustomFieldValues(
            @Param("tableName") String tableName,
            @Param("idColumn") String idColumn,
            @Param("entityId") Long entityId,
            @Param("values") Map<String, Object> values
    );

    /**
     * 批量查询实体的自定义字段值
     *
     * @param tableName  表名
     * @param idColumn   ID列名
     * @param entityIds  实体ID列表
     * @param columns    自定义字段列名列表
     * @return 每行一条Map，含entity_id字段
     */
    List<Map<String, Object>> getBatchCustomFieldValues(
            @Param("tableName") String tableName,
            @Param("idColumn") String idColumn,
            @Param("entityIds") List<Long> entityIds,
            @Param("columns") List<String> columns
    );
}
