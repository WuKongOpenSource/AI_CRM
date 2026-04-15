package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.TokenPurchaseProperties;
import com.kakarote.ai_crm.config.TokenPurchaseProperties.Plan;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.TokenPurchaseCreateBO;
import com.kakarote.ai_crm.entity.PO.TokenPurchaseOrder;
import com.kakarote.ai_crm.entity.VO.TokenPurchaseOptionVO;
import com.kakarote.ai_crm.entity.VO.TokenPurchaseOrderVO;
import com.kakarote.ai_crm.mapper.TokenPurchaseOrderMapper;
import com.kakarote.ai_crm.service.ICrmTenantService;
import com.kakarote.ai_crm.service.ITokenPurchaseService;
import com.kakarote.ai_crm.utils.QrCodeUtil;
import com.kakarote.ai_crm.utils.RequestContextUtil;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@Service
public class TokenPurchaseServiceImpl extends ServiceImpl<TokenPurchaseOrderMapper, TokenPurchaseOrder>
        implements ITokenPurchaseService {

    private static final String CHANNEL_WECHAT = "wechat";
    private static final String CHANNEL_ALIPAY = "alipay";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private final TokenPurchaseProperties properties;
    private final ICrmTenantService tenantService;

    public TokenPurchaseServiceImpl(TokenPurchaseProperties properties, ICrmTenantService tenantService) {
        this.properties = properties;
        this.tenantService = tenantService;
    }

    @Override
    public TokenPurchaseOptionVO getOptions() {
        Long tenantId = UserUtil.getTenantId();
        TokenPurchaseOptionVO vo = new TokenPurchaseOptionVO();
        vo.setEnabled(properties.isEnabled());
        vo.setOrderExpireMinutes(properties.getOrderExpireMinutes());
        vo.setGiftTokenRemaining(tenantService.getGiftTokenRemaining(tenantId));
        vo.setPurchasedTokenRemaining(tenantService.getPurchasedTokenRemaining(tenantId));
        vo.setTokenRemaining(tenantService.getTotalTokenRemaining(tenantId));

        List<TokenPurchaseOptionVO.PlanVO> plans = new ArrayList<>();
        for (Plan plan : properties.getResolvedPlans()) {
            TokenPurchaseOptionVO.PlanVO item = new TokenPurchaseOptionVO.PlanVO();
            item.setId(plan.getId());
            item.setName(plan.getName());
            item.setDescription(plan.getDescription());
            item.setTokenAmount(plan.getTokenAmount());
            item.setPriceFen(plan.getPriceFen());
            plans.add(item);
        }
        vo.setPlans(plans);

        List<TokenPurchaseOptionVO.ChannelVO> channels = new ArrayList<>();
        channels.add(buildChannel(CHANNEL_WECHAT, "微信支付", properties.getWechat().isReady(), "微信支付商户参数未配置完整"));
        channels.add(buildChannel(CHANNEL_ALIPAY, "支付宝", properties.getAlipay().isReady(), "支付宝商户参数未配置完整"));
        vo.setChannels(channels);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenPurchaseOrderVO createOrder(TokenPurchaseCreateBO createBO) {
        ensurePurchaseEnabled();
        Long tenantId = UserUtil.getTenantId();
        Long userId = UserUtil.getUserId();
        if (tenantId == null || userId == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH, "当前登录信息失效，请重新登录");
        }

        Plan plan = findPlan(createBO.getPlanId());
        String channel = normalizeChannel(createBO.getPaymentChannel());
        validateChannelReady(channel);

        TokenPurchaseOrder order = new TokenPurchaseOrder();
        order.setTenantId(tenantId);
        order.setUserId(userId);
        order.setOrderNo("TP" + IdUtil.getSnowflakeNextIdStr());
        order.setPlanId(plan.getId());
        order.setPlanName(plan.getName());
        order.setTokenAmount(plan.getTokenAmount());
        order.setAmountFen(plan.getPriceFen());
        order.setPaymentChannel(channel);
        order.setStatus(STATUS_PENDING);
        order.setExpireTime(new Date(System.currentTimeMillis() + properties.getOrderExpireMinutes() * 60_000L));
        order.setPaymentQrCode(CHANNEL_WECHAT.equals(channel) ? createWechatPayment(order) : createAlipayPayment(order));
        save(order);
        return toOrderVO(order);
    }

    @Override
    public TokenPurchaseOrderVO getOrder(String orderNo) {
        TokenPurchaseOrder order = lambdaQuery()
                .eq(TokenPurchaseOrder::getOrderNo, orderNo)
                .one();
        if (order == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "订单不存在");
        }
        refreshOrderStatusIfExpired(order);
        return toOrderVO(order);
    }

    @Override
    public List<TokenPurchaseOrderVO> listRecentOrders(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<TokenPurchaseOrder> orders = lambdaQuery()
                .orderByDesc(TokenPurchaseOrder::getCreateTime)
                .last("LIMIT " + safeLimit)
                .list();
        for (TokenPurchaseOrder order : orders) {
            refreshOrderStatusIfExpired(order);
        }
        return orders.stream().map(this::toOrderVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleWechatNotify(String timestamp, String nonce, String signature, String serial, String body) {
        try {
            verifyWechatSignature(timestamp, nonce, signature, serial, body);
            JsonNode notifyNode = objectMapper.readTree(body);
            JsonNode resource = notifyNode.path("resource");
            JsonNode transaction = objectMapper.readTree(decryptWechatResource(resource));

            String orderNo = transaction.path("out_trade_no").asText();
            String transactionId = transaction.path("transaction_id").asText(null);
            String tradeState = transaction.path("trade_state").asText();
            int amountFen = transaction.path("amount").path("total").asInt();

            TokenPurchaseOrder order = baseMapper.selectGlobalByOrderNoForUpdate(orderNo);
            if (order == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "订单不存在");
            }
            if (order.getAmountFen() != null && order.getAmountFen() != amountFen) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "微信回调金额校验失败");
            }

            if ("SUCCESS".equalsIgnoreCase(tradeState)) {
                markOrderPaid(order, transactionId, body);
            } else if ("CLOSED".equalsIgnoreCase(tradeState)) {
                markOrderStatus(order, STATUS_CLOSED, transactionId, body);
            } else {
                markOrderStatus(order, STATUS_FAILED, transactionId, body);
            }
            return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
        } catch (Exception e) {
            log.error("Handle wechat notify failed", e);
            return "{\"code\":\"FAIL\",\"message\":\"失败\"}";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            verifyAlipaySignature(params);
            String orderNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");
            BigDecimal totalAmount = new BigDecimal(params.getOrDefault("total_amount", "0"));

            TokenPurchaseOrder order = baseMapper.selectGlobalByOrderNoForUpdate(orderNo);
            if (order == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "订单不存在");
            }
            if (fenToAmount(order.getAmountFen()).compareTo(totalAmount) != 0) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付宝回调金额校验失败");
            }

            String payload = objectMapper.writeValueAsString(params);
            if ("TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) || "TRADE_FINISHED".equalsIgnoreCase(tradeStatus)) {
                markOrderPaid(order, tradeNo, payload);
            } else if ("TRADE_CLOSED".equalsIgnoreCase(tradeStatus)) {
                markOrderStatus(order, STATUS_CLOSED, tradeNo, payload);
            } else {
                markOrderStatus(order, STATUS_FAILED, tradeNo, payload);
            }
            return "success";
        } catch (Exception e) {
            log.error("Handle alipay notify failed", e);
            return "failure";
        }
    }

    private TokenPurchaseOptionVO.ChannelVO buildChannel(String code, String label, boolean enabled, String reason) {
        TokenPurchaseOptionVO.ChannelVO channel = new TokenPurchaseOptionVO.ChannelVO();
        channel.setCode(code);
        channel.setLabel(label);
        channel.setEnabled(enabled);
        channel.setUnavailableReason(enabled ? null : reason);
        return channel;
    }

    private void ensurePurchaseEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "当前环境未启用 Token 购买");
        }
    }

    private Plan findPlan(String planId) {
        return properties.getResolvedPlans().stream()
                .filter(plan -> StrUtil.equals(plan.getId(), planId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "购买套餐不存在"));
    }

    private String normalizeChannel(String paymentChannel) {
        String channel = StrUtil.nullToEmpty(paymentChannel).trim().toLowerCase(Locale.ROOT);
        if (!CHANNEL_WECHAT.equals(channel) && !CHANNEL_ALIPAY.equals(channel)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付方式不支持");
        }
        return channel;
    }

    private void validateChannelReady(String channel) {
        if (CHANNEL_WECHAT.equals(channel) && !properties.getWechat().isReady()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "微信支付尚未完成配置");
        }
        if (CHANNEL_ALIPAY.equals(channel) && !properties.getAlipay().isReady()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付宝尚未完成配置");
        }
    }

    private String createWechatPayment(TokenPurchaseOrder order) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("appid", properties.getWechat().getAppId());
            payload.put("mchid", properties.getWechat().getMerchantId());
            payload.put("description", buildOrderSubject(order));
            payload.put("out_trade_no", order.getOrderNo());
            payload.put("notify_url", resolveNotifyUrl(CHANNEL_WECHAT));
            payload.put("amount", Map.of("total", order.getAmountFen(), "currency", "CNY"));

            String path = "/v3/pay/transactions/native";
            String body = objectMapper.writeValueAsString(payload);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("Authorization", buildWechatAuthorization("POST", path, body));
            headers.set("User-Agent", "AI-CRM/TokenPurchase");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    normalizeUrl(properties.getWechat().getGateway(), path),
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode responseNode = objectMapper.readTree(response.getBody());
            String codeUrl = responseNode.path("code_url").asText(null);
            if (StrUtil.isBlank(codeUrl)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                        responseNode.path("message").asText("微信支付下单失败"));
            }
            return codeUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "微信支付下单失败: " + e.getMessage());
        }
    }

    private String createAlipayPayment(TokenPurchaseOrder order) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("app_id", properties.getAlipay().getAppId());
            params.put("method", "alipay.trade.precreate");
            params.put("format", "JSON");
            params.put("charset", "UTF-8");
            params.put("sign_type", "RSA2");
            params.put("timestamp", formatAlipayTimestamp(new Date()));
            params.put("version", "1.0");
            params.put("notify_url", resolveNotifyUrl(CHANNEL_ALIPAY));
            params.put("biz_content", objectMapper.writeValueAsString(Map.of(
                    "out_trade_no", order.getOrderNo(),
                    "total_amount", fenToAmount(order.getAmountFen()).toPlainString(),
                    "subject", buildOrderSubject(order),
                    "timeout_express", properties.getOrderExpireMinutes() + "m"
            )));
            params.put("sign", buildAlipaySign(params));

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            params.forEach(form::add);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    properties.getAlipay().getGateway(),
                    new HttpEntity<>(form, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode responseNode = root.path("alipay_trade_precreate_response");
            if (!"10000".equals(responseNode.path("code").asText())) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                        responseNode.path("sub_msg").asText(responseNode.path("msg").asText("支付宝下单失败")));
            }
            String qrCode = responseNode.path("qr_code").asText(null);
            if (StrUtil.isBlank(qrCode)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付宝下单失败，未返回二维码");
            }
            return qrCode;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付宝下单失败: " + e.getMessage());
        }
    }

    private String resolveNotifyUrl(String channel) {
        String configured = CHANNEL_WECHAT.equals(channel)
                ? properties.getWechat().getNotifyUrl()
                : properties.getAlipay().getNotifyUrl();
        if (StrUtil.isNotBlank(configured)) {
            return configured.trim();
        }
        String baseUrl = RequestContextUtil.getBaseUrl();
        if (StrUtil.isBlank(baseUrl)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付回调地址未配置，请先补充 notify-url");
        }
        return baseUrl + "/tokenPurchase/notify/" + channel;
    }

    private void refreshOrderStatusIfExpired(TokenPurchaseOrder order) {
        if (order == null || !STATUS_PENDING.equals(order.getStatus()) || order.getExpireTime() == null) {
            return;
        }
        if (order.getExpireTime().before(new Date())) {
            order.setStatus(STATUS_EXPIRED);
            updateById(order);
        }
    }

    private void markOrderPaid(TokenPurchaseOrder order, String providerOrderNo, String payload) {
        if (STATUS_PAID.equals(order.getStatus())) {
            return;
        }
        markOrderStatus(order, STATUS_PAID, providerOrderNo, payload);
        tenantService.addPurchasedTokens(order.getTenantId(), order.getTokenAmount());
    }

    private void markOrderStatus(TokenPurchaseOrder order, String status, String providerOrderNo, String payload) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(order.getTenantId());
            order.setStatus(status);
            order.setPaymentProviderOrderNo(StrUtil.blankToDefault(providerOrderNo, order.getPaymentProviderOrderNo()));
            order.setNotifyPayload(limitPayload(payload));
            if (STATUS_PAID.equals(status)) {
                order.setPaidTime(new Date());
            }
            updateById(order);
        } finally {
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            } else {
                TenantContextHolder.clear();
            }
        }
    }

    private TokenPurchaseOrderVO toOrderVO(TokenPurchaseOrder order) {
        TokenPurchaseOrderVO vo = BeanUtil.copyProperties(order, TokenPurchaseOrderVO.class);
        vo.setAmountDisplay("¥" + fenToAmount(order.getAmountFen()).stripTrailingZeros().toPlainString());
        vo.setPaymentChannelLabel(CHANNEL_WECHAT.equals(order.getPaymentChannel()) ? "微信支付" : "支付宝");
        vo.setQrCodeContent(order.getPaymentQrCode());
        if (STATUS_PENDING.equals(order.getStatus()) && StrUtil.isNotBlank(order.getPaymentQrCode())) {
            vo.setQrCodeImage(QrCodeUtil.toDataUri(order.getPaymentQrCode(), 280));
        }
        return vo;
    }

    private String buildOrderSubject(TokenPurchaseOrder order) {
        return "AI CRM Token 充值 - " + order.getTokenAmount() + " Token";
    }

    private BigDecimal fenToAmount(Integer amountFen) {
        if (amountFen == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amountFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String formatAlipayTimestamp(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return format.format(date);
    }

    private String normalizeUrl(String gateway, String path) {
        return StrUtil.removeSuffix(gateway, "/") + path;
    }

    private String limitPayload(String payload) {
        if (payload == null) {
            return null;
        }
        return payload.length() > 4000 ? payload.substring(0, 4000) : payload;
    }

    private String buildWechatAuthorization(String method, String path, String body) throws Exception {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String sign = Base64.getEncoder().encodeToString(signRsaSha256(
                loadPrivateKey(properties.getWechat().getPrivateKey()),
                message.getBytes(StandardCharsets.UTF_8)
        ));
        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + properties.getWechat().getMerchantId() + "\","
                + "nonce_str=\"" + nonce + "\","
                + "signature=\"" + sign + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + properties.getWechat().getMerchantSerialNo() + "\"";
    }

    private void verifyWechatSignature(String timestamp, String nonce, String signature, String serial, String body) throws Exception {
        X509Certificate certificate = loadCertificate(properties.getWechat().getPlatformCertificate());
        if (StrUtil.isBlank(timestamp) || StrUtil.isBlank(nonce) || StrUtil.isBlank(signature)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "微信支付回调头不完整");
        }
        if (StrUtil.isNotBlank(serial) && !StrUtil.equalsIgnoreCase(serial, certificate.getSerialNumber().toString(16))) {
            log.warn("Wechat pay serial mismatch, header={}, cert={}", serial, certificate.getSerialNumber().toString(16));
        }
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        if (!verifyRsaSha256(
                certificate.getPublicKey(),
                message.getBytes(StandardCharsets.UTF_8),
                Base64.getDecoder().decode(signature)
        )) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "微信支付回调验签失败");
        }
    }

    private String decryptWechatResource(JsonNode resource) throws Exception {
        String associatedData = resource.path("associated_data").asText("");
        String nonce = resource.path("nonce").asText();
        String ciphertext = resource.path("ciphertext").asText();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(normalizeMultiline(properties.getWechat().getApiV3Key())
                .getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
    }

    private String buildAlipaySign(Map<String, String> params) throws Exception {
        return Base64.getEncoder().encodeToString(signRsaSha256(
                loadPrivateKey(properties.getAlipay().getPrivateKey()),
                buildAlipaySignContent(params, true).getBytes(StandardCharsets.UTF_8)
        ));
    }

    private void verifyAlipaySignature(Map<String, String> params) throws Exception {
        String sign = params.get("sign");
        if (StrUtil.isBlank(sign)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付宝回调缺少签名");
        }
        if (!verifyRsaSha256(
                loadPublicKey(properties.getAlipay().getAlipayPublicKey()),
                buildAlipaySignContent(params, false).getBytes(StandardCharsets.UTF_8),
                Base64.getDecoder().decode(sign)
        )) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付宝回调验签失败");
        }
    }

    private String buildAlipaySignContent(Map<String, String> params, boolean includeSignType) {
        return params.entrySet().stream()
                .filter(entry -> StrUtil.isNotBlank(entry.getValue()))
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> includeSignType || !"sign_type".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(normalizePemBody(pem));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private PublicKey loadPublicKey(String pem) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(normalizePemBody(pem));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private X509Certificate loadCertificate(String pem) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(normalizeMultiline(pem).getBytes(StandardCharsets.UTF_8))
        );
    }

    private byte[] signRsaSha256(PrivateKey privateKey, byte[] content) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(content);
        return signature.sign();
    }

    private boolean verifyRsaSha256(PublicKey publicKey, byte[] content, byte[] sign) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(content);
        return signature.verify(sign);
    }

    private String normalizePemBody(String pem) {
        return normalizeMultiline(pem)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
    }

    private String normalizeMultiline(String value) {
        return StrUtil.nullToEmpty(value).replace("\\n", "\n").trim();
    }
}
