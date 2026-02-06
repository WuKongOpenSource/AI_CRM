package com.kakarote.ai_crm.config;

import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * OIDC Provider 配置
 * 用于 MinIO SSO 登录
 *
 * 注意：issuer、redirectUri、frontendLoginUrl 已改为动态获取
 * 通过 RequestContextUtil 从当前请求中提取访问地址
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "oidc")
public class OidcConfig {

    /**
     * OIDC 客户端 ID
     */
    private String clientId = "minio-console";

    /**
     * OIDC 客户端密钥
     */
    private String clientSecret = "minio-console-secret-key-2024";

    /**
     * MinIO Console 端口（用于动态生成 redirect_uri）
     */
    private int minioConsolePort = 9001;

    /**
     * 授权码有效期（秒）
     */
    private int codeExpiry = 300;

    /**
     * Access Token 有效期（秒）
     */
    private int tokenExpiry = 3600;

    /**
     * Session Cookie 名称
     */
    private String sessionCookie = "CRM_SESSION";

    /**
     * RSA 密钥对（用于签署 id_token）
     */
    private RSAKey rsaKey;

    /**
     * RSA Key ID
     */
    private String keyId;

    /**
     * 应用启动时生成 RSA 密钥对
     */
    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            this.keyId = UUID.randomUUID().toString();
            this.rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .build();

            log.info("OIDC RSA 密钥对已生成，Key ID: {}", keyId);
            log.info("OIDC Provider 配置: clientId={}, minioConsolePort={}", clientId, minioConsolePort);
        } catch (Exception e) {
            log.error("生成 RSA 密钥对失败", e);
            throw new RuntimeException("无法初始化 OIDC 配置", e);
        }
    }

    /**
     * 获取 RSA 公钥
     */
    public RSAPublicKey getPublicKey() {
        try {
            return rsaKey.toRSAPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("获取 RSA 公钥失败", e);
        }
    }

    /**
     * 获取 RSA 私钥
     */
    public RSAPrivateKey getPrivateKey() {
        try {
            return rsaKey.toRSAPrivateKey();
        } catch (Exception e) {
            throw new RuntimeException("获取 RSA 私钥失败", e);
        }
    }
}
