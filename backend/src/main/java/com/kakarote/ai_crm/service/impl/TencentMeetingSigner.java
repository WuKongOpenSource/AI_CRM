package com.kakarote.ai_crm.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TencentMeetingSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private TencentMeetingSigner() {
    }

    public static String sign(String secretId, String secretKey, String httpMethod,
                              String nonce, String timestamp, String requestUri, String requestBody) {
        try {
            String body = requestBody == null ? "" : requestBody;
            String toBeSigned = httpMethod.toUpperCase()
                    + "\nX-TC-Key=" + secretId
                    + "&X-TC-Nonce=" + nonce
                    + "&X-TC-Timestamp=" + timestamp
                    + "\n" + requestUri
                    + "\n" + body;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            String hex = bytesToHex(mac.doFinal(toBeSigned.getBytes(StandardCharsets.UTF_8)));
            return Base64.getEncoder().encodeToString(hex.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign Tencent Meeting request", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        int index = 0;
        for (byte value : bytes) {
            out[index++] = HEX[(value >>> 4) & 0x0f];
            out[index++] = HEX[value & 0x0f];
        }
        return new String(out);
    }
}
