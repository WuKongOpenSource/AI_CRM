package com.kakarote.ai_crm.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.PostgreDialect;
import com.kakarote.ai_crm.config.auth.GlobalDataPermissionHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mybatis-plus配置
 *
 * @author hmb
 */
@Configuration
@MapperScan(basePackages = {"com.kakarote.ai_crm.mapper"})
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(GlobalDataPermissionHandler dataPermissionHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataPermissionHandler));

        PaginationInnerInterceptor innerInterceptor = new PaginationInnerInterceptor();
        innerInterceptor.setDbType(DbType.POSTGRE_SQL);
        innerInterceptor.setDialect(new PostgreDialect());
        innerInterceptor.setOptimizeJoin(true);
        interceptor.addInnerInterceptor(innerInterceptor);
        return interceptor;
    }
}
