package com.kakarote.ai_crm.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业微信「代开发自建应用」(代开发模板) 的 suite 凭证配置。
 *
 * <p>用于在收到代开发模板回调的授权事件时，调用 get_permanent_code 拿到企业 permanent_code，
 * 之后用自建式 gettoken(corpid, permanent_code) 取企业 access_token（区别于第三方应用的 get_corp_token）。</p>
 *
 * <p>注意：代开发模板的<b>回调 Token/EncodingAESKey</b> 复用 {@code wecom.dev-callback.*}（回调端点是
 * /wecom/dev/callback）；这里只配代开发模板自己的 <b>suite_id / suite_secret</b>（服务商后台「代开发应用」详情页）。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "wecom.agency-dev")
public class WecomAgencyDevProperties {

    /** 是否启用代开发集成。关闭时 /wecom/dev/callback 仍只做 URL 验证+应答（不处理授权事件）。 */
    private Boolean enabled = Boolean.FALSE;

    /** 代开发模板的 suite_id（服务商后台「代开发应用」）。 */
    private String suiteId;

    /** 代开发模板的 suite_secret。 */
    private String suiteSecret;

    public boolean isUsable() {
        return Boolean.TRUE.equals(enabled)
                && StrUtil.isNotBlank(suiteId)
                && StrUtil.isNotBlank(suiteSecret);
    }
}
