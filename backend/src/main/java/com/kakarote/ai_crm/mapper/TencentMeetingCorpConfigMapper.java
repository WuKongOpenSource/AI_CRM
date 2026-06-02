package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TencentMeetingCorpConfigMapper extends BaseMapper<TencentMeetingCorpConfig> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM crm_tencent_meeting_corp_config
            WHERE webhook_secret_encrypted IS NOT NULL
            """)
    List<TencentMeetingCorpConfig> selectWebhookConfigsIgnoreTenant();
}
