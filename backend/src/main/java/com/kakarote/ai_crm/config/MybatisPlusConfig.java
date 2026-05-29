package com.kakarote.ai_crm.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.PostgreDialect;
import com.kakarote.ai_crm.config.auth.GlobalDataPermissionHandler;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus plugin configuration.
 */
@Configuration
@MapperScan(basePackages = {"com.kakarote.ai_crm.mapper"})
public class MybatisPlusConfig {

    private static final Set<String> IGNORE_TENANT_TABLES = Set.of(
            "manager_menu",
            "manager_role_menu",
            "crm_tenant",
            "crm_custom_field_pool",
            "crm_ai_model_pricing",
            "crm_ai_billing_config",
            "crm_access_log",
            "crm_error_log",
            "crm_external_auth_identity",
            "crm_external_tenant_binding"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(GlobalDataPermissionHandler dataPermissionHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.getTenantId();
                if (tenantId == null) {
                    return new LongValue(0);
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TENANT_TABLES.contains(tableName);
            }
        });
        interceptor.addInnerInterceptor(tenantInterceptor);
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataPermissionHandler));

        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(DbType.POSTGRE_SQL);
        paginationInterceptor.setDialect(new PostgreDialect());
        paginationInterceptor.setOptimizeJoin(true);
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}
