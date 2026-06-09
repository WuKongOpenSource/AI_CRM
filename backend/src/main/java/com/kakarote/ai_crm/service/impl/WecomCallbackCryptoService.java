package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.WecomOpenPlatformProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Service
public class WecomCallbackCryptoService {

    @Autowired
    private WecomOpenPlatformProperties properties;

    /**
     * 使用第三方应用回调凭证(Token/EncodingAESKey)验签并解密。
     */
    public String decrypt(String msgSignature, String timestamp, String nonce, String encryptedText) {
        return decrypt(properties.getToken(), properties.getEncodingAesKey(), msgSignature, timestamp, nonce, encryptedText);
    }

    /**
     * 使用指定的回调凭证(Token/EncodingAESKey)验签并解密。
     * 供多套回调(第三方应用 / 代开发模板)复用同一 WXBizMsgCrypt 加解密实现。
     */
    public String decrypt(String token,
                          String encodingAesKey,
                          String msgSignature,
                          String timestamp,
                          String nonce,
                          String encryptedText) {
        if (StrUtil.isBlank(encryptedText)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom callback encrypt text is empty");
        }
        verifySignature(token, msgSignature, timestamp, nonce, encryptedText);
        byte[] aesKey = aesKey(encodingAesKey);
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(aesKey, 0, 16));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            byte[] unpadded = removePkcs7Padding(decrypted);
            if (unpadded.length < 20) {
                throw new IllegalArgumentException("plain text is too short");
            }
            int xmlLength = ByteBuffer.wrap(unpadded, 16, 4).getInt();
            if (xmlLength < 0 || 20 + xmlLength > unpadded.length) {
                throw new IllegalArgumentException("plain text length is invalid");
            }
            return new String(unpadded, 20, xmlLength, StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom callback decrypt failed");
        }
    }

    private void verifySignature(String token, String msgSignature, String timestamp, String nonce, String encryptedText) {
        if (StrUtil.isBlank(msgSignature)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom callback signature is empty");
        }
        String[] parts = {
                StrUtil.nullToEmpty(token),
                StrUtil.nullToEmpty(timestamp),
                StrUtil.nullToEmpty(nonce),
                StrUtil.nullToEmpty(encryptedText)
        };
        Arrays.sort(parts);
        String joined = String.join("", parts);
        try {
            String actual = bytesToHex(MessageDigest.getInstance("SHA-1").digest(joined.getBytes(StandardCharsets.UTF_8)));
            if (!actual.equalsIgnoreCase(msgSignature)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom callback signature mismatch");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom callback signature verify failed");
        }
    }

    private byte[] aesKey(String encodingAesKey) {
        String key = StrUtil.trim(encodingAesKey);
        if (key.length() != 43) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom EncodingAESKey must be 43 characters");
        }
        return Base64.getDecoder().decode(key + "=");
    }

    private byte[] removePkcs7Padding(byte[] decrypted) {
        int padding = decrypted[decrypted.length - 1] & 0xFF;
        if (padding < 1 || padding > 32) {
            padding = 0;
        }
        return Arrays.copyOf(decrypted, decrypted.length - padding);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
