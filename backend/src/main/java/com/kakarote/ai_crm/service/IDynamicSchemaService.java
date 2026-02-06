package com.kakarote.ai_crm.service;

/**
 * 动态Schema服务接口
 * 用于执行数据库表结构变更
 */
public interface IDynamicSchemaService {

    /**
     * 添加列
     *
     * @param tableName  表名
     * @param columnName 列名
     * @param columnType 列类型
     * @param comment    列注释
     */
    void addColumn(String tableName, String columnName, String columnType, String comment);

    /**
     * 删除列
     *
     * @param tableName  表名
     * @param columnName 列名
     */
    void dropColumn(String tableName, String columnName);

    /**
     * 检查列是否存在
     *
     * @param tableName  表名
     * @param columnName 列名
     * @return 是否存在
     */
    boolean columnExists(String tableName, String columnName);

    /**
     * 获取实体对应的表名
     *
     * @param entityType 实体类型
     * @return 表名
     */
    String getTableName(String entityType);

    /**
     * 获取实体的主键列名
     *
     * @param entityType 实体类型
     * @return 主键列名
     */
    String getIdColumnName(String entityType);
}
