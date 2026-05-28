package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ErrorLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface ErrorLogMapper extends BaseMapper<ErrorLog> {

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    @Delete("""
            DELETE FROM crm_error_log
            WHERE error_id IN (
                SELECT error_id
                FROM crm_error_log
                WHERE create_time < #{cutoff}
                ORDER BY create_time
                LIMIT #{limit}
            )
            """)
    int deleteExpiredBefore(@Param("cutoff") Date cutoff, @Param("limit") int limit);
}
