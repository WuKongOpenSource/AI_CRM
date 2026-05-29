package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.AccessLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    @Delete("""
            DELETE FROM crm_access_log
            WHERE log_id IN (
                SELECT log_id
                FROM crm_access_log
                WHERE create_time < #{cutoff}
                ORDER BY create_time
                LIMIT #{limit}
            )
            """)
    int deleteExpiredBefore(@Param("cutoff") Date cutoff, @Param("limit") int limit);
}
