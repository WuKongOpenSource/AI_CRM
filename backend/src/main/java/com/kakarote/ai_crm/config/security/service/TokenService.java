package com.kakarote.ai_crm.config.security.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.alibaba.fastjson.JSON;
import com.kakarote.ai_crm.common.Const;
import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Handles JWT and Redis-backed login sessions.
 */
@Component
@Slf4j
public class TokenService {
    private static final DateTimeFormatter KICKOUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long MILLIS_SECOND = 1000;
    private static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;
    private static final long MILLIS_MINUTE_TEN = 1400 * 60 * 1000L;

    @Value("${token.header}")
    private String header;

    @Value("${token.secret}")
    private String secret;

    @Value("${token.expireTime}")
    private int expireTime;

    @Value("${token.multi-login-enabled:false}")
    private boolean multiLoginEnabled;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 获取Login用户。
     */
    public LoginUser getLoginUser(HttpServletRequest request) {
        String token = getToken(request);
        if (StrUtil.isEmpty(token)) {
            return null;
        }

        try {
            Claims claims = parseToken(token);
            String uuid = (String) claims.get(Const.LOGIN_USER_KEY);
            String loginUserJson = redisTemplate.opsForValue().get(getTokenKey(uuid));
            if (StrUtil.isBlank(loginUserJson)) {
                attachKickoutMessage(request, uuid);
                return null;
            }

            LoginUser loginUser = JSON.parseObject(loginUserJson, LoginUser.class);
            if (loginUser == null) {
                attachKickoutMessage(request, uuid);
                return null;
            }

            if (isTokenReplaced(loginUser, uuid)) {
                attachKickoutMessage(request, uuid);
                return null;
            }
            return loginUser;
        } catch (Exception e) {
            log.debug("Failed to resolve login user from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 通过原始 token 字符串解析登录用户（用于 WebSocket 等无 HttpServletRequest 的场景）。
     */
    public LoginUser getLoginUser(String token) {
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        try {
            Claims claims = parseToken(token);
            String uuid = (String) claims.get(Const.LOGIN_USER_KEY);
            String loginUserJson = redisTemplate.opsForValue().get(getTokenKey(uuid));
            if (StrUtil.isBlank(loginUserJson)) {
                return null;
            }
            LoginUser loginUser = JSON.parseObject(loginUserJson, LoginUser.class);
            if (loginUser == null) {
                return null;
            }
            if (isTokenReplaced(loginUser, uuid)) {
                return null;
            }
            return loginUser;
        } catch (Exception e) {
            log.debug("Failed to resolve login user from token string: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 设置Login用户。
     */
    public void setLoginUser(LoginUser loginUser) {
        if (ObjectUtil.isNotNull(loginUser) && StrUtil.isNotEmpty(loginUser.getToken())) {
            refreshToken(loginUser);
        }
    }

    /**
     * 处理delLoginUser方法逻辑。
     */
    public void delLoginUser(String token) {
        if (StrUtil.isEmpty(token)) {
            return;
        }

        String tokenKey = getTokenKey(token);
        String loginUserJson = redisTemplate.opsForValue().get(tokenKey);
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(getKickoutKey(token));

        if (StrUtil.isBlank(loginUserJson)) {
            return;
        }

        LoginUser loginUser = JSON.parseObject(loginUserJson, LoginUser.class);
        clearUserTokenMapping(loginUser, token);
    }

    /**
     * 创建Token。
     */
    public String createToken(LoginUser loginUser) {
        return createToken(loginUser, null);
    }

    /**
     * 创建Token。
     */
    public String createToken(LoginUser loginUser, String loginIp) {
        String token = IdUtil.fastUUID();
        loginUser.setToken(token);
        LoginTypeEnum loginType = resolveLoginType(loginUser);
        loginUser.setLoginType(loginType);
        handleSingleLogin(loginUser, token, loginIp);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Const.LOGIN_USER_KEY, token);
        claims.put("username", loginUser.getUsername());
        claims.put("loginType", loginType.name());
        return createToken(claims);
    }

    /**
     * 校验Token。
     */
    public void verifyToken(LoginUser loginUser) {
        long expireAt = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireAt - currentTime <= MILLIS_MINUTE_TEN) {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新Token。
     */
    public void refreshToken(LoginUser loginUser) {
        LoginTypeEnum loginType = resolveLoginType(loginUser);
        loginUser.setLoginType(loginType);
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        redisTemplate.opsForValue().set(
                getTokenKey(loginUser.getToken()),
                JSON.toJSONString(loginUser),
                expireTime,
                TimeUnit.MINUTES
        );

        if (!multiLoginEnabled && loginUser.getUser() != null && loginUser.getUser().getUserId() != null) {
            redisTemplate.opsForValue().set(
                    getUserTokenKey(loginUser.getUser().getUserId(), loginType),
                    loginUser.getToken(),
                    expireTime,
                    TimeUnit.MINUTES
            );
            clearMatchingLegacyUserTokenMapping(loginUser.getUser().getUserId(), loginUser.getToken(), loginType);
        }
    }

    /**
     * 获取UsernameToken。
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 解析LoginIP。
     */
    public String resolveLoginIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return StrUtil.nullToEmpty(JakartaServletUtil.getClientIP(request)).trim();
    }

    /**
     * 创建Token。
     */
    private String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 解析Token。
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取Token。
     */
    private String getToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader(header);
        if (StrUtil.isNotEmpty(token) && token.startsWith(Const.TOKEN_PREFIX)) {
            token = token.replace(Const.TOKEN_PREFIX, "");
        }
        return token;
    }

    /**
     * 获取Token键。
     */
    private String getTokenKey(String uuid) {
        return Const.LOGIN_TOKEN_KEY + uuid;
    }

    /**
     * 获取用户Token键。
     */
    private String getUserTokenKey(Long userId, LoginTypeEnum loginType) {
        return Const.LOGIN_USER_TOKEN_KEY + userId + ":" + LoginTypeEnum.resolve(loginType).name();
    }

    private String getLegacyUserTokenKey(Long userId) {
        return Const.LOGIN_USER_TOKEN_KEY + userId;
    }

    /**
     * 获取Kickout键。
     */
    private String getKickoutKey(String token) {
        return Const.LOGIN_KICKOUT_KEY + token;
    }

    private LoginTypeEnum resolveLoginType(LoginUser loginUser) {
        return loginUser == null ? LoginTypeEnum.PC : LoginTypeEnum.resolve(loginUser.getLoginType());
    }

    private String getLatestUserToken(Long userId, LoginTypeEnum loginType) {
        LoginTypeEnum resolvedLoginType = LoginTypeEnum.resolve(loginType);
        String latestToken = redisTemplate.opsForValue().get(getUserTokenKey(userId, resolvedLoginType));
        if (StrUtil.isBlank(latestToken) && resolvedLoginType == LoginTypeEnum.PC) {
            return redisTemplate.opsForValue().get(getLegacyUserTokenKey(userId));
        }
        return latestToken;
    }

    private void clearMatchingLegacyUserTokenMapping(Long userId, String token, LoginTypeEnum loginType) {
        if (userId == null || StrUtil.isBlank(token) || LoginTypeEnum.resolve(loginType) != LoginTypeEnum.PC) {
            return;
        }

        String legacyUserTokenKey = getLegacyUserTokenKey(userId);
        String legacyToken = redisTemplate.opsForValue().get(legacyUserTokenKey);
        if (StrUtil.equals(token, legacyToken)) {
            redisTemplate.delete(legacyUserTokenKey);
        }
    }

    /**
     * 判断是否TokenReplaced。
     */
    private boolean isTokenReplaced(LoginUser loginUser, String token) {
        if (multiLoginEnabled || loginUser == null || loginUser.getUser() == null || loginUser.getUser().getUserId() == null) {
            return false;
        }

        String latestToken = getLatestUserToken(loginUser.getUser().getUserId(), resolveLoginType(loginUser));
        return StrUtil.isNotBlank(latestToken) && !StrUtil.equals(latestToken, token);
    }

    /**
     * 处理SingleLogin。
     */
    private void handleSingleLogin(LoginUser loginUser, String newToken, String loginIp) {
        if (multiLoginEnabled || loginUser == null || loginUser.getUser() == null || loginUser.getUser().getUserId() == null) {
            return;
        }

        LoginTypeEnum loginType = resolveLoginType(loginUser);
        Long userId = loginUser.getUser().getUserId();
        String oldToken = getLatestUserToken(userId, loginType);
        if (StrUtil.isBlank(oldToken) || StrUtil.equals(oldToken, newToken)) {
            return;
        }

        redisTemplate.opsForValue().set(
                getKickoutKey(oldToken),
                buildKickoutMessage(loginIp),
                expireTime,
                TimeUnit.MINUTES
        );
        redisTemplate.delete(getTokenKey(oldToken));
        clearMatchingLegacyUserTokenMapping(userId, oldToken, loginType);
    }

    /**
     * 清理用户Token映射。
     */
    private void clearUserTokenMapping(LoginUser loginUser, String token) {
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getUserId() == null) {
            return;
        }

        LoginTypeEnum loginType = resolveLoginType(loginUser);
        String userTokenKey = getUserTokenKey(loginUser.getUser().getUserId(), loginType);
        String currentToken = redisTemplate.opsForValue().get(userTokenKey);
        if (StrUtil.equals(token, currentToken)) {
            redisTemplate.delete(userTokenKey);
        }
        clearMatchingLegacyUserTokenMapping(loginUser.getUser().getUserId(), token, loginType);
    }

    /**
     * 处理attachKickoutMessage方法逻辑。
     */
    private void attachKickoutMessage(HttpServletRequest request, String token) {
        if (request == null || StrUtil.isBlank(token)) {
            return;
        }

        String kickoutMessage = redisTemplate.opsForValue().get(getKickoutKey(token));
        if (StrUtil.isNotBlank(kickoutMessage)) {
            request.setAttribute(Const.LOGIN_KICKOUT_MESSAGE_ATTR, kickoutMessage);
        }
    }

    /**
     * 构建Kickout消息。
     */
    private String buildKickoutMessage(String loginIp) {
        String time = LocalDateTime.now().format(KICKOUT_TIME_FORMATTER);
        if (StrUtil.isBlank(loginIp)) {
            return "\u5f53\u524d\u7528\u6237\u4e8e" + time + "\u5728\u5176\u4ed6IP\u767b\u5f55\uff0c\u5f53\u524d\u767b\u5f55\u5df2\u88ab\u9000\u51fa";
        }
        return "\u5f53\u524d\u7528\u6237\u4e8e" + time + "\u5728\u5176\u4ed6IP(" + loginIp
                + ")\u767b\u5f55\uff0c\u5f53\u524d\u767b\u5f55\u5df2\u88ab\u9000\u51fa";
    }
}
