package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置Mapper
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT config_value
            FROM crm_system_config
            WHERE tenant_id = #{tenantId}
              AND config_key = #{configKey}
            LIMIT 1
            """)
    String selectValueByTenantId(@Param("tenantId") Long tenantId, @Param("configKey") String configKey);
}
