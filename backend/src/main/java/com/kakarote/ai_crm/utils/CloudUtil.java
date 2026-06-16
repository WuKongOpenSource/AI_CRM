package com.kakarote.ai_crm.utils;

import cn.hutool.core.util.StrUtil;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.kakarote.ai_crm.config.CloudMailProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SaaS 云能力工具类。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudUtil {

    private final CloudMailProperties cloudMailProperties;

    private IAcsClient client;

    /**
     * 初始化客户端。
     */
    @PostConstruct
    public void initClient() {
        if (!cloudMailProperties.hasRequiredConfig()) {
            return;
        }
        DefaultProfile profile = DefaultProfile.getProfile(
                cloudMailProperties.getRegionId(),
                cloudMailProperties.getAccessKeyId(),
                cloudMailProperties.getAccessKeySecret()
        );
        this.client = new DefaultAcsClient(profile);
    }

    /**
     * 发送邮箱验证码。
     */
    public boolean sendVerificationCodeEmail(String email, String subject, String code, String sceneName) {
        if (!cloudMailProperties.isEnabled()) {
            log.info("邮件服务未启用，使用日志模式输出验证码: email={}, scene={}, code={}", email, sceneName, code);
            return true;
        }
        if (!cloudMailProperties.hasRequiredConfig() || client == null) {
            log.error("邮件服务已启用但配置不完整，无法发送验证码邮件");
            return false;
        }

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dm.aliyuncs.com");
        request.setSysVersion("2015-11-23");
        request.setSysAction("SingleSendMail");
        request.putQueryParameter("AddressType", "1");
        request.putQueryParameter("AccountName", cloudMailProperties.getAccountName());
        request.putQueryParameter("ReplyToAddress", String.valueOf(cloudMailProperties.isReplyToAddress()));
        request.putQueryParameter("ToAddress", email);
        request.putQueryParameter("Subject", subject);
        if (StrUtil.isNotBlank(cloudMailProperties.getFromAlias())) {
            request.putQueryParameter("FromAlias", cloudMailProperties.getFromAlias());
        }
        request.putQueryParameter("HtmlBody", buildVerificationEmailBody(code, sceneName));

        try {
            CommonResponse response = client.getCommonResponse(request);
            if (response.getHttpStatus() == 200) {
                return true;
            }
            log.error("邮件发送失败: status={}, response={}", response.getHttpStatus(), response.getData());
        } catch (ClientException e) {
            log.error("邮件发送失败", e);
        }
        return false;
    }

    /**
     * 构建校验邮箱内容。
     */
    private String buildVerificationEmailBody(String code, String sceneName) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>邮箱验证码</title>
                </head>
                <body style="margin:0;padding:24px;background:#f3f6fb;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;color:#0f172a;">
                  <div style="max-width:620px;margin:0 auto;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 12px 40px rgba(15,23,42,0.08);">
                    <div style="padding:28px 32px;background:linear-gradient(135deg,#0f172a,#1d4ed8);color:#ffffff;">
                      <div style="font-size:14px;letter-spacing:1px;opacity:0.85;">AI CRM</div>
                      <h1 style="margin:12px 0 0;font-size:24px;line-height:1.3;">邮箱验证码</h1>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.8;">您正在进行<span style="font-weight:600;color:#1d4ed8;">%s</span>操作，本次验证码如下：</p>
                      <div style="margin:24px 0;padding:18px 20px;border-radius:16px;background:#eff6ff;border:1px solid #bfdbfe;text-align:center;">
                        <span style="display:inline-block;font-size:34px;font-weight:700;letter-spacing:8px;color:#1d4ed8;">%s</span>
                      </div>
                      <p style="margin:0 0 12px;font-size:14px;line-height:1.8;color:#475569;">验证码 10 分钟内有效，请勿泄露给他人。</p>
                      <p style="margin:0;font-size:14px;line-height:1.8;color:#64748b;">如果这不是您的操作，请忽略本邮件。</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(sceneName, code);
    }
}
