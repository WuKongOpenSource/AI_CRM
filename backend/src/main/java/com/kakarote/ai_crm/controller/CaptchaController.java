package com.kakarote.ai_crm.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.alibaba.fastjson.util.TypeUtils;
import com.anji.captcha.model.common.CaptchaTypeEnum;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.util.AESUtil;
import com.anji.captcha.util.StringUtils;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.CaptchaCheckBO;
import com.kakarote.ai_crm.entity.VO.CaptchaCheckVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/captcha")
@Tag(name = "验证码接口")
public class CaptchaController {

    private static final String DEFAULT_CAPTCHA_TYPE = CaptchaTypeEnum.BLOCKPUZZLE.getCodeValue();
    private static final int DEFAULT_POINT_Y = 5;

    @Autowired
    private CaptchaService captchaService;

    @PostMapping("/get")
    @Operation(summary = "获取滑块验证码")
    public Result<Object> get(@RequestBody(required = false) CaptchaVO data, HttpServletRequest request) {
        if (data == null) {
            data = new CaptchaVO();
        }
        if (StrUtil.isBlank(data.getCaptchaType())) {
            data.setCaptchaType(DEFAULT_CAPTCHA_TYPE);
        }
        data.setBrowserInfo(getRemoteId(request));
        ResponseModel model = captchaService.get(data);
        Result<Object> result = new Result<>(TypeUtils.castToInt(model.getRepCode()), model.getRepMsg());
        result.setData(model.getRepData());
        return result;
    }

    @PostMapping("/check")
    @Operation(summary = "校验滑块验证码")
    public Result<CaptchaCheckVO> check(@Valid @RequestBody CaptchaCheckBO data, HttpServletRequest request) {
        String pointJson = buildPointJson(data.getPointX(), data.getPointY());

        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType(StrUtil.blankToDefault(data.getCaptchaType(), DEFAULT_CAPTCHA_TYPE));
        captchaVO.setToken(data.getToken());
        captchaVO.setPointJson(encryptIfNecessary(pointJson, data.getSecretKey()));
        captchaVO.setBrowserInfo(getRemoteId(request));

        ResponseModel model = captchaService.check(captchaVO);
        if (!model.isSuccess()) {
            return new Result<>(TypeUtils.castToInt(model.getRepCode()), model.getRepMsg());
        }
        return Result.ok(new CaptchaCheckVO(buildCaptchaVerification(data.getToken(), pointJson, data.getSecretKey())));
    }

    public static String getRemoteId(HttpServletRequest request) {
        String clientIP = JakartaServletUtil.getClientIP(request);
        String ua = request.getHeader("user-agent");
        if (StringUtils.isNotBlank(clientIP)) {
            return clientIP + ua;
        }
        return request.getRemoteAddr() + ua;
    }

    private String buildPointJson(Integer pointX, Integer pointY) {
        int y = pointY == null ? DEFAULT_POINT_Y : pointY;
        return "{\"x\":%d,\"y\":%d}".formatted(pointX, y);
    }

    private String buildCaptchaVerification(String token, String pointJson, String secretKey) {
        return encryptIfNecessary(token + "---" + pointJson, secretKey);
    }

    private String encryptIfNecessary(String rawValue, String secretKey) {
        if (StrUtil.isBlank(secretKey)) {
            return rawValue;
        }
        try {
            return AESUtil.aesEncrypt(rawValue, secretKey);
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "验证码加密失败");
        }
    }
}
