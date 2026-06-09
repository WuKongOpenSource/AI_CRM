package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.WecomDevCallbackProperties;
import com.kakarote.ai_crm.service.impl.WecomAgencyDevService;
import com.kakarote.ai_crm.service.impl.WecomCallbackCryptoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业微信「代开发模板」回调端点（{@code /wecom/dev/callback}）。
 *
 * <p>会话存档功能本身不依赖回调事件（存档走 WeWorkFinanceSDK），但创建代开发模板时企微会对
 * 回调URL做有效性验证（GET 带 echostr），并随后持续推送 suite_ticket / 授权变更等事件（POST）。
 * 本端点只做两件事：</p>
 * <ul>
 *   <li>GET：用代开发模板的 Token/EncodingAESKey 验签并解密 echostr，原样返回明文 → 通过URL验证；</li>
 *   <li>POST：统一应答 {@code success}，事件暂不处理（存档无需）。</li>
 * </ul>
 *
 * <p>与第三方应用回调 {@code /wecom/open/callback} 相互独立，复用同一 WXBizMsgCrypt 实现
 * （{@link WecomCallbackCryptoService}），仅换用代开发这一套密钥。需在 SecurityConfig 放行本路径。</p>
 */
@RestController
@RequestMapping("/wecom/dev")
@Tag(name = "WeCom Agency Dev Callback")
public class WecomDevCallbackController {

    @Autowired
    private WecomDevCallbackProperties properties;

    @Autowired
    private WecomCallbackCryptoService cryptoService;

    @Autowired
    private WecomAgencyDevService agencyDevService;

    @GetMapping("/callback")
    @Operation(summary = "Verify WeCom agency-dev template callback URL")
    public ResponseEntity<String> verify(@RequestParam(value = "msg_signature", required = false) String msgSignature,
                                         @RequestParam(required = false) String timestamp,
                                         @RequestParam(required = false) String nonce,
                                         @RequestParam(value = "echostr", required = false) String echoStr) {
        if (!properties.isUsable()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "代开发回调未配置，请设置 wecom.dev-callback.token 与 wecom.dev-callback.encoding-aes-key");
        }
        String plain = cryptoService.decrypt(properties.getToken(), properties.getEncodingAesKey(),
                msgSignature, timestamp, nonce, echoStr);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(plain);
    }

    @PostMapping("/callback")
    @Operation(summary = "Receive WeCom agency-dev template callback event")
    public ResponseEntity<String> callback(@RequestBody(required = false) String body,
                                           @RequestParam(value = "msg_signature", required = false) String msgSignature,
                                           @RequestParam(required = false) String timestamp,
                                           @RequestParam(required = false) String nonce) {
        // 代开发集成启用时处理授权事件（suite_ticket / create_auth → 捕获 permanent_code）；未启用则仅应答 success。
        agencyDevService.handleEvent(body, msgSignature, timestamp, nonce);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("success");
    }
}
