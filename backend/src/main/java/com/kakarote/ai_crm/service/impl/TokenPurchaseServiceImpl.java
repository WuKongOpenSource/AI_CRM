package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse;
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
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;

/**
 * 积分购买服务。
 * 这里统一编排购买选项、订单创建、第三方下单、回调处理和到账入账逻辑，
 * 让前端购买弹窗只围绕订单状态做展示和轮询。
 */
@Slf4j
@Service
public class TokenPurchaseServiceImpl extends ServiceImpl<TokenPurchaseOrderMapper, TokenPurchaseOrder>
    implements ITokenPurchaseService {

    private static final String CHANNEL_WECHAT = "wechat";
    private static final String CHANNEL_ALIPAY = "alipay";
    private static final String ALIPAY_PRODUCT_CODE_FAST_INSTANT_TRADE_PAY = "FAST_INSTANT_TRADE_PAY";
    private static final String PAYMENT_MODE_QR_CODE = "qrcode";
    private static final String PAYMENT_MODE_PAGE = "page";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private final TokenPurchaseProperties properties;
    private final ICrmTenantService tenantService;

    /**
     * 初始化Token 购买实例。
     */
    public TokenPurchaseServiceImpl(TokenPurchaseProperties properties, ICrmTenantService tenantService) {
        this.properties = properties;
        this.tenantService = tenantService;
    }

    /**
     * 返回购买弹窗初始化所需的完整信息，
     * 包括当前租户剩余额度、可购买套餐以及各支付渠道是否可用。
     */
    @Override
    public TokenPurchaseOptionVO getOptions() {
        Long tenantId = UserUtil.getTenantId();
        TokenPurchaseOptionVO vo = new TokenPurchaseOptionVO();
        vo.setEnabled(properties.isEnabled());
        vo.setOrderExpireMinutes(properties.getOrderExpireMinutes());
        vo.setGiftCreditRemaining(tenantService.getGiftCreditRemaining(tenantId));
        vo.setPurchasedCreditRemaining(tenantService.getPurchasedCreditRemaining(tenantId));
        vo.setCreditRemaining(tenantService.getTotalCreditRemaining(tenantId));

        List<TokenPurchaseOptionVO.PlanVO> plans = new ArrayList<>();
        for (Plan plan : properties.getResolvedPlans()) {
            TokenPurchaseOptionVO.PlanVO item = new TokenPurchaseOptionVO.PlanVO();
            item.setId(plan.getId());
            item.setName(plan.getName());
            item.setDescription(plan.getDescription());
            item.setCreditAmount(plan.getCreditAmount());
            item.setPriceFen(plan.getPriceFen());
            plans.add(item);
        }
        vo.setPlans(plans);

        List<TokenPurchaseOptionVO.ChannelVO> channels = new ArrayList<>();
        channels.add(buildChannel(CHANNEL_WECHAT, "微信支付", isWechatReady(), "微信支付商户参数未配置完整"));
        channels.add(buildChannel(CHANNEL_ALIPAY, "支付宝", properties.getAlipay().isReady(), "支付宝商户参数未配置完整"));
        vo.setChannels(channels);
        return vo;
    }

    /**
     * 先创建本地订单，再生成第三方支付载荷。
     * 这样前端无论是立刻轮询，还是后续刷新页面，都能拿到稳定的订单号。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenPurchaseOrderVO createOrder(TokenPurchaseCreateBO createBO) {
        ensurePurchaseEnabled();
        Long tenantId = UserUtil.getTenantId();
        Long userId = UserUtil.getUserId();
        if (tenantId == null || userId == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH, "当前登录信息失效，请重新登录");
        }

        TokenPurchaseOrder reusableOrder = findReusablePendingOrder(tenantId);
        if (reusableOrder != null) {
            return toOrderVO(reusableOrder);
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
        order.setCreditAmount(plan.getCreditAmount());
        order.setAmountFen(plan.getPriceFen());
        order.setPaymentChannel(channel);
        order.setStatus(STATUS_PENDING);
        order.setExpireTime(new Date(System.currentTimeMillis() + properties.getOrderExpireMinutes() * 60_000L));
        save(order);

        order.setPaymentQrCode(createPaymentPayload(order));
        updateById(order);
        return toOrderVO(order);
    }

    /**
     * 购买额度归属租户；同一租户已有待支付订单时，复用原订单和原二维码，避免重复向支付平台下单。
     */
    private TokenPurchaseOrder findReusablePendingOrder(Long tenantId) {
        List<TokenPurchaseOrder> pendingOrders = lambdaQuery()
            .eq(TokenPurchaseOrder::getTenantId, tenantId)
            .eq(TokenPurchaseOrder::getStatus, STATUS_PENDING)
            .orderByDesc(TokenPurchaseOrder::getCreateTime)
            .list();
        for (TokenPurchaseOrder order : pendingOrders) {
            refreshOrderStatusIfExpired(order);
            if (STATUS_PENDING.equals(order.getStatus())) {
                ensurePaymentPayload(order);
                return order;
            }
        }
        return null;
    }

    /**
     * 查询单个订单给前端轮询使用。
     * 如果待支付订单缺少支付载荷，这里会兜底补生成一次。
     */
    @Override
    public TokenPurchaseOrderVO getOrder(String orderNo) {
        TokenPurchaseOrder order = lambdaQuery()
            .eq(TokenPurchaseOrder::getOrderNo, orderNo)
            .one();
        if (order == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "订单不存在");
        }
        refreshOrderStatusIfExpired(order);
        ensurePaymentPayload(order);
        return toOrderVO(order);
    }

    /**
     * 查询最近购买订单，用于账单抽屉展示。
     */
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

    /**
     * 处理微信支付异步回调。
     * 当前按项目现状仅保留 APIv3 解密和订单金额/状态校验，不再走平台证书验签。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleWechatNotify(String body) {
        try {
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
            log.debug("微信回调: {}", body);
            if ("SUCCESS".equalsIgnoreCase(tradeState)) {
                markOrderPaid(order, transactionId, body);
            } else if ("CLOSED".equalsIgnoreCase(tradeState)) {
                markOrderStatus(order, STATUS_CLOSED, transactionId, body);
            } else {
                markOrderStatus(order, STATUS_FAILED, transactionId, body);
            }
            return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            return "{\"code\":\"FAIL\",\"message\":\"失败\"}";
        }
    }

    /**
     * 处理支付宝异步回调。
     * 支付宝仍然保留官方 EasySDK 验签，然后再按交易状态更新本地订单。
     */
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
            log.error("处理支付宝回调失败", e);
            return "failure";
        }
    }

    /**
     * 组装支付渠道展示项。
     */
    private TokenPurchaseOptionVO.ChannelVO buildChannel(String code, String label, boolean enabled, String reason) {
        TokenPurchaseOptionVO.ChannelVO channel = new TokenPurchaseOptionVO.ChannelVO();
        channel.setCode(code);
        channel.setLabel(label);
        channel.setEnabled(enabled);
        channel.setUnavailableReason(enabled ? null : reason);
        return channel;
    }

    /**
     * 总开关控制。购买功能被禁用时直接拒绝下单。
     */
    private void ensurePurchaseEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "当前环境未启用积分购买");
        }
    }

    /**
     * 按套餐 ID 查找购买套餐。
     */
    private Plan findPlan(String planId) {
        return properties.getResolvedPlans().stream()
            .filter(plan -> StrUtil.equals(plan.getId(), planId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "购买套餐不存在"));
    }

    /**
     * 归一化支付渠道，只允许微信和支付宝两个固定值。
     */
    private String normalizeChannel(String paymentChannel) {
        String channel = StrUtil.nullToEmpty(paymentChannel).trim().toLowerCase(Locale.ROOT);
        if (!CHANNEL_WECHAT.equals(channel) && !CHANNEL_ALIPAY.equals(channel)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付方式不支持");
        }
        return channel;
    }

    /**
     * 下单前校验支付渠道是否已完成必要配置。
     */
    private void validateChannelReady(String channel) {
        if (CHANNEL_WECHAT.equals(channel) && !isWechatReady()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "微信支付尚未完成配置");
        }
        if (CHANNEL_ALIPAY.equals(channel) && !properties.getAlipay().isReady()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付宝尚未完成配置");
        }
    }

    /**
     * 调用微信 Native Pay 下单并返回原始 code_url。
     * 前端再基于这个地址生成二维码图片。
     */
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
            log.debug("微信支付: {}", body);
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

    /**
     * 使用支付宝 Page Pay 生成一段可直接放进 iframe 的表单页面。
     * 这样前端无需再自己拼接支付宝网关参数。
     */
    private String createAlipayPayment(TokenPurchaseOrder order) {
        try {
            initAlipayFactory();
            int qrCodeWidth = properties.getAlipay().getQrcodeWidth() == null
                ? 200
                : Math.max(100, properties.getAlipay().getQrcodeWidth());
            var page = Factory.Payment.Page()
                .optional("qr_pay_mode", 4)
                .optional("qrcode_width", qrCodeWidth)
                .optional("integration_type", "PCWEB")
                .optional("product_code", ALIPAY_PRODUCT_CODE_FAST_INSTANT_TRADE_PAY)
                .optional("body", buildOrderBody(order));
            if (StrUtil.isNotBlank(properties.getAlipay().getSellerId())) {
                page = page.optional("seller_id", properties.getAlipay().getSellerId().trim());
            }
            AlipayTradePagePayResponse pay = page
                .asyncNotify(resolveNotifyUrl(CHANNEL_ALIPAY))
                .pay(buildOrderSubject(order), order.getOrderNo(), fenToAmount(order.getAmountFen()).toPlainString(), "");
            return pay.getBody();
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付宝下单失败: " + e.getMessage());
        }
    }

    /**
     * EasySDK 使用静态 Factory 持有配置。
     * 每次调用前重新初始化，避免不同请求之间串用旧配置。
     */
    private void initAlipayFactory() {
        Config config = new Config();
        config.protocol = "https";
        config.signType = "RSA2";
        config.gatewayHost = normalizeAlipayGatewayHost(properties.getAlipay().getGateway());
        config.appId = properties.getAlipay().getAppId();
        config.merchantPrivateKey = normalizeMultiline(properties.getAlipay().getPrivateKey());
        config.alipayPublicKey = normalizeMultiline(properties.getAlipay().getAlipayPublicKey());
        Factory.setOptions(config);
    }

    /**
     * 将完整网关地址整理成 EasySDK 需要的 host 形式。
     */
    private String normalizeAlipayGatewayHost(String gateway) {
        String normalizedGateway = StrUtil.blankToDefault(gateway, "https://openapi.alipay.com/gateway.do").trim();
        normalizedGateway = StrUtil.removePrefixIgnoreCase(normalizedGateway, "https://");
        normalizedGateway = StrUtil.removePrefixIgnoreCase(normalizedGateway, "http://");
        return StrUtil.subBefore(normalizedGateway, "/", false);
    }

    /**
     * 优先使用显式配置的回调地址。
     * 本地开发时如果没配，则尝试根据当前请求上下文动态拼回调地址。
     */
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

    /**
     * 待支付订单超过有效期后，查询时会被自动标记为已过期。
     */
    private void refreshOrderStatusIfExpired(TokenPurchaseOrder order) {
        if (order == null || !STATUS_PENDING.equals(order.getStatus()) || order.getExpireTime() == null) {
            return;
        }
        if (order.getExpireTime().before(new Date())) {
            order.setStatus(STATUS_EXPIRED);
            updateById(order);
        }
    }

    /**
     * 支付平台回调可能重复投递，所以成功入账必须保持幂等。
     * 已支付订单再次回调时直接返回，不重复增加积分。
     */
    private void markOrderPaid(TokenPurchaseOrder order, String providerOrderNo, String payload) {
        if (STATUS_PAID.equals(order.getStatus())) {
            return;
        }
        markOrderStatus(order, STATUS_PAID, providerOrderNo, payload);
        tenantService.addPurchasedCredits(order.getTenantId(), order.getCreditAmount());
    }

    /**
     * 支付回调通常发生在匿名请求下，没有当前租户上下文。
     * 这里临时切换到订单所属租户，确保更新和入账都落到正确租户。
     */
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

    /**
     * 将数据库订单转换成前端弹窗需要的展示结构。
     * 微信走二维码模式，支付宝走 page pay 表单模式。
     */
    private TokenPurchaseOrderVO toOrderVO(TokenPurchaseOrder order) {
        TokenPurchaseOrderVO vo = BeanUtil.copyProperties(order, TokenPurchaseOrderVO.class);
        vo.setAmountDisplay("¥" + fenToAmount(order.getAmountFen()).stripTrailingZeros().toPlainString());
        vo.setPaymentChannelLabel(CHANNEL_WECHAT.equals(order.getPaymentChannel()) ? "微信支付" : "支付宝");
        if (CHANNEL_WECHAT.equals(order.getPaymentChannel())) {
            vo.setPaymentMode(PAYMENT_MODE_QR_CODE);
            vo.setQrCodeContent(order.getPaymentQrCode());
            if (STATUS_PENDING.equals(order.getStatus()) && StrUtil.isNotBlank(order.getPaymentQrCode())) {
                vo.setQrCodeImage(QrCodeUtil.toDataUri(order.getPaymentQrCode(), 280));
            }
        } else {
            vo.setPaymentMode(PAYMENT_MODE_PAGE);
            if (STATUS_PENDING.equals(order.getStatus()) && StrUtil.isNotBlank(order.getPaymentQrCode())) {
                vo.setPaymentFormHtml(order.getPaymentQrCode());
            }
        }
        return vo;
    }

    /**
     * 生成支付标题，直接展示给第三方支付平台。
     */
    private String buildOrderSubject(TokenPurchaseOrder order) {
        return "AI CRM 积分充值 - " + order.getCreditAmount() + " 积分";
    }

    /**
     * 生成支付补充说明，用在支付宝等支持 body 的场景。
     */
    private String buildOrderBody(TokenPurchaseOrder order) {
        return "AI CRM 积分套餐充值: " + StrUtil.blankToDefault(order.getPlanName(), order.getPlanId());
    }

    /**
     * 统一封装支付载荷的创建入口，避免首次下单和补生成走两套逻辑。
     */
    private String createPaymentPayload(TokenPurchaseOrder order) {
        return CHANNEL_WECHAT.equals(order.getPaymentChannel())
            ? createWechatPayment(order)
            : createAlipayPayment(order);
    }

    /**
     * 待支付订单如果缺少支付载荷，在查询时补一遍，避免用户刷新后必须重新下单。
     */
    private void ensurePaymentPayload(TokenPurchaseOrder order) {
        if (order == null || !STATUS_PENDING.equals(order.getStatus()) || StrUtil.isNotBlank(order.getPaymentQrCode())) {
            return;
        }
        order.setPaymentQrCode(createPaymentPayload(order));
        updateById(order);
    }

    /**
     * 分转元。
     */
    private BigDecimal fenToAmount(Integer amountFen) {
        if (amountFen == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amountFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * 拼接第三方接口完整地址。
     */
    private String normalizeUrl(String gateway, String path) {
        return StrUtil.removeSuffix(gateway, "/") + path;
    }

    /**
     * 控制回调原文保存长度，避免数据库字段被超长载荷撑爆。
     */
    private String limitPayload(String payload) {
        if (payload == null) {
            return null;
        }
        return payload.length() > 4000 ? payload.substring(0, 4000) : payload;
    }

    /**
     * 构造微信 APIv3 商户请求头。
     * 这里的签名串换行格式必须严格符合微信文档要求。
     */
    private String buildWechatAuthorization(String method, String path, String body) throws Exception {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String sign = Base64.getEncoder().encodeToString(signRsaSha256(
            loadPrivateKey(resolveWechatPrivateKey()),
            message.getBytes(StandardCharsets.UTF_8)
        ));
        return "WECHATPAY2-SHA256-RSA2048 "
            + "mchid=\"" + properties.getWechat().getMerchantId() + "\","
            + "nonce_str=\"" + nonce + "\","
            + "signature=\"" + sign + "\","
            + "timestamp=\"" + timestamp + "\","
            + "serial_no=\"" + properties.getWechat().getMerchantSerialNo() + "\"";
    }

    /**
     * 微信回调里的 resource 使用商户 APIv3 Key 做 AES-GCM 解密。
     */
    private String decryptWechatResource(JsonNode resource) throws Exception {
        String associatedData = resource.path("associated_data").asText("");
        String nonce = resource.path("nonce").asText();
        String ciphertext = resource.path("ciphertext").asText();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(
            resolveWechatApiV3Key().getBytes(StandardCharsets.UTF_8),
            "AES"
        );
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
    }

    /**
     * 支付宝回调验签继续沿用 EasySDK 官方实现。
     */
    private void verifyAlipaySignature(Map<String, String> params) throws Exception {
        initAlipayFactory();
        if (!Factory.Payment.Common().verifyNotify(params)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "支付宝回调缺少签名");
        }
    }

    /**
     * 微信支付可用性只校验商户基础参数、APIv3 Key 和商户私钥是否齐备。
     * 按当前需求，不再要求配置平台证书或微信支付公钥。
     */
    private boolean isWechatReady() {
        TokenPurchaseProperties.Wechat wechat = properties.getWechat();
        return wechat.isEnabled()
            && StrUtil.isNotBlank(wechat.getAppId())
            && StrUtil.isNotBlank(wechat.getMerchantId())
            && StrUtil.isNotBlank(wechat.getMerchantSerialNo())
            && isValidWechatApiV3Key(wechat.getApiV3Key())
            && StrUtil.isNotBlank(resolveWechatPrivateKeyQuietly());
    }

    /**
     * 显式获取微信商户私钥，缺失时抛出可读错误。
     */
    private String resolveWechatPrivateKey() {
        String privateKey = resolveWechatPrivateKeyQuietly();
        if (StrUtil.isBlank(privateKey)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                "微信支付私钥未配置");
        }
        return privateKey;
    }

    /**
     * 显式获取 APIv3 Key。
     * 微信支付回调解密必须使用 32 字节密钥，这里提前拦截配置错误。
     */
    private String resolveWechatApiV3Key() {
        String apiV3Key = normalizeMultiline(properties.getWechat().getApiV3Key());
        if (StrUtil.isBlank(apiV3Key)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "微信支付 APIv3 Key 未配置");
        }
        if (!isValidWechatApiV3Key(apiV3Key)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "微信支付 APIv3 Key 必须为 32 字节");
        }
        return apiV3Key;
    }

    /**
     * 安静模式解析商户私钥。
     * 支持直接填 PEM，也支持按路径读取，最后再回退到默认证书目录。
     */
    private String resolveWechatPrivateKeyQuietly() {
        return resolveWechatPemContent(
            properties.getWechat().getPrivateKey(),
            properties.getWechat().getPrivateKeyPath(),
            List.of("apiclient_key.pem")
        );
    }

    /**
     * 微信 APIv3 Key 固定要求 32 字节，长度不对时不允许继续下单或解密。
     */
    private boolean isValidWechatApiV3Key(String apiV3Key) {
        String normalizedKey = normalizeMultiline(apiV3Key);
        return StrUtil.isNotBlank(normalizedKey)
            && normalizedKey.getBytes(StandardCharsets.UTF_8).length == 32;
    }

    /**
     * 通用 PEM 解析逻辑。
     * 按“内联内容 -> 显式路径 -> 默认证书文件”的顺序查找。
     */
    private String resolveWechatPemContent(String inlineValue, String pathValue, List<String> defaultFileNames) {
        Path keyDirectory = resolveWechatKeyDirectory();
        String inlineContent = resolvePemValueOrPath(inlineValue, keyDirectory);
        if (StrUtil.isNotBlank(inlineContent)) {
            return inlineContent;
        }

        String pathContent = resolvePemValueOrPath(pathValue, keyDirectory);
        if (StrUtil.isNotBlank(pathContent)) {
            return pathContent;
        }

        for (String fileName : defaultFileNames) {
            String defaultContent = readFileContent(resolveChildPath(keyDirectory, fileName));
            if (StrUtil.isNotBlank(defaultContent)) {
                return defaultContent;
            }
        }
        return null;
    }

    /**
     * 解析微信证书目录。默认取 src/main/resources/cert。
     */
    private Path resolveWechatKeyDirectory() {
        String configuredKeyPath = StrUtil.blankToDefault(properties.getWechat().getKeyPath(), "src/main/resources/cert");
        return resolveFilePath(configuredKeyPath, null);
    }

    /**
     * 一个值既可能是 PEM 原文，也可能是文件路径，这里统一兼容。
     */
    private String resolvePemValueOrPath(String rawValue, Path baseDirectory) {
        String normalizedValue = normalizeMultiline(rawValue);
        if (StrUtil.isBlank(normalizedValue)) {
            return null;
        }
        if (looksLikePem(normalizedValue)) {
            return normalizedValue;
        }
        return readFileContent(resolveFilePath(normalizedValue, baseDirectory));
    }

    /**
     * 粗略判断字符串是否已经是 PEM 内容。
     */
    private boolean looksLikePem(String value) {
        return StrUtil.containsAnyIgnoreCase(value,
            "-----BEGIN PRIVATE KEY-----",
            "-----BEGIN PUBLIC KEY-----",
            "-----BEGIN CERTIFICATE-----");
    }

    /**
     * 解析文件路径。
     * 既兼容绝对路径，也兼容相对工作目录和默认证书目录。
     */
    private Path resolveFilePath(String rawPath, Path baseDirectory) {
        if (StrUtil.isBlank(rawPath)) {
            return null;
        }

        Path raw = Paths.get(rawPath.trim());
        if (raw.isAbsolute()) {
            return raw.normalize();
        }

        Path workingDirectory = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path workingCandidate = workingDirectory.resolve(raw).normalize();
        if (Files.exists(workingCandidate)) {
            return workingCandidate;
        }

        if (baseDirectory != null) {
            Path baseCandidate = baseDirectory.resolve(raw).normalize();
            if (Files.exists(baseCandidate)) {
                return baseCandidate;
            }
        }

        if ("src/main/resources/cert".equals(rawPath.trim().replace("\\", "/"))) {
            Path backendCandidate = workingDirectory.resolve("backend").resolve("src/main/resources/cert").normalize();
            if (Files.exists(backendCandidate)) {
                return backendCandidate;
            }
        }

        return workingCandidate;
    }

    /**
     * 在证书目录下拼接子文件路径。
     */
    private Path resolveChildPath(Path directory, String fileName) {
        if (directory == null || StrUtil.isBlank(fileName)) {
            return null;
        }
        return directory.resolve(fileName).normalize();
    }

    /**
     * 读取 PEM 文件内容。
     * 读取失败时只记日志，方便做“quietly”判断。
     */
    private String readFileContent(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.warn("读取 PEM 文件失败, path={}", path, e);
            return null;
        }
    }

    /**
     * 将 PEM 私钥解析成 RSA PrivateKey。
     */
    private PrivateKey loadPrivateKey(String pem) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(normalizePemBody(pem));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    /**
     * 使用 SHA256withRSA 生成签名。
     */
    private byte[] signRsaSha256(PrivateKey privateKey, byte[] content) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(content);
        return signature.sign();
    }

    /**
     * 去掉 PEM 头尾和空白，得到可供 Base64 解码的主体内容。
     */
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

    /**
     * 把环境变量里常见的 \\n 还原成真实换行。
     */
    private String normalizeMultiline(String value) {
        return StrUtil.nullToEmpty(value).replace("\\n", "\n").trim();
    }
}
