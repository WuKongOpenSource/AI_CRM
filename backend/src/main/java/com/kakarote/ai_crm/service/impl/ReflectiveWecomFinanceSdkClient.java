package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class ReflectiveWecomFinanceSdkClient implements WecomFinanceSdkClient {

    private static final String FINANCE_CLASS_NAME = "com.tencent.wework.Finance";
    private static final int DEFAULT_TIMEOUT_SECONDS = 3;

    @Override
    public List<JSONObject> fetchMessages(FetchRequest request) {
        try {
            Class<?> financeClass = Class.forName(FINANCE_CLASS_NAME);
            long sdk = number(invoke(financeClass, "NewSdk")).longValue();
            try {
                long initRet = number(invoke(financeClass, "Init", sdk, request.corpId(), request.secret())).longValue();
                assertSdkOk(initRet, "Initialize WeCom Finance SDK failed");
                JSONObject encryptedData = fetchEncryptedChatData(financeClass, sdk, request.startSeq(), request.limit());
                return decryptChatData(financeClass, sdk, encryptedData, request.privateKey(), request.publicKeyVersion());
            } finally {
                invokeQuietly(financeClass, "DestroySdk", sdk);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "企业微信会话存档 SDK 未加载，请添加 java_sdk.jar 和原生动态库");
        } catch (LinkageError e) {
            // ExceptionInInitializerError / UnsatisfiedLinkError / NoClassDefFoundError：
            // SDK jar 在类路径上但当前平台的原生库(.so/.dll)缺失或不兼容（如缺少 OpenSSL 依赖）。
            // 转为业务异常，保证应用在缺库时仍可正常启动、仅在调用会话存档时报错。
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "企业微信会话存档原生库加载失败，请确认对应平台的 .so/.dll 已就位: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "拉取企业微信会话存档失败: " + e.getMessage());
        }
    }

    private JSONObject fetchEncryptedChatData(Class<?> financeClass, long sdk, long startSeq, int limit) throws Exception {
        long slice = number(invoke(financeClass, "NewSlice")).longValue();
        try {
            long ret = number(invoke(financeClass, "GetChatData",
                    sdk,
                    startSeq,
                    limit,
                    "",
                    "",
                    DEFAULT_TIMEOUT_SECONDS,
                    slice)).longValue();
            assertSdkOk(ret, "Fetch WeCom archive encrypted chat data failed");
            String raw = string(invoke(financeClass, "GetContentFromSlice", slice));
            JSONObject json = JSON.parseObject(raw);
            assertApiOk(json, "Fetch WeCom archive encrypted chat data failed");
            return json;
        } finally {
            invokeQuietly(financeClass, "FreeSlice", slice);
        }
    }

    private List<JSONObject> decryptChatData(Class<?> financeClass,
                                             long sdk,
                                             JSONObject encryptedData,
                                             String privateKey,
                                             String publicKeyVersion) throws Exception {
        JSONArray chatData = encryptedData == null ? null : encryptedData.getJSONArray("chatdata");
        if (chatData == null || chatData.isEmpty()) {
            return List.of();
        }
        List<JSONObject> result = new ArrayList<>();
        for (Object item : chatData) {
            JSONObject encryptedMessage = (JSONObject) item;
            String encryptedChatMsg = encryptedMessage.getString("encrypt_chat_msg");
            if (StrUtil.isBlank(encryptedChatMsg)) {
                result.add(encryptedMessage);
                continue;
            }
            String messagePublicKeyVersion = encryptedMessage.getString("publickey_ver");
            if (StrUtil.isNotBlank(publicKeyVersion) && StrUtil.isNotBlank(messagePublicKeyVersion)
                    && !publicKeyVersion.equals(messagePublicKeyVersion)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                        "企业微信会话存档公钥版本与消息版本不匹配");
            }
            String decryptedRandomKey = decryptRandomKey(encryptedMessage.getString("encrypt_random_key"), privateKey);
            long messageSlice = number(invoke(financeClass, "NewSlice")).longValue();
            try {
                long ret = number(invoke(financeClass, "DecryptData", sdk, decryptedRandomKey, encryptedChatMsg, messageSlice)).longValue();
                assertSdkOk(ret, "Decrypt WeCom archive message failed");
                String plaintext = string(invoke(financeClass, "GetContentFromSlice", messageSlice));
                JSONObject message = JSON.parseObject(plaintext);
                if (message.getLong("seq") == null) {
                    message.put("seq", encryptedMessage.getLong("seq"));
                }
                result.add(message);
            } finally {
                invokeQuietly(financeClass, "FreeSlice", messageSlice);
            }
        }
        return result;
    }

    private String decryptRandomKey(String encryptedRandomKey, String privateKeyPem) {
        if (StrUtil.isBlank(encryptedRandomKey)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "企业微信会话存档加密随机密钥为空");
        }
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedRandomKey);
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "解密企业微信会话存档随机密钥失败: " + e.getMessage());
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        String normalized = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        if (privateKeyPem.contains("BEGIN RSA PRIVATE KEY")) {
            keyBytes = wrapPkcs1PrivateKey(keyBytes);
        }
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private byte[] wrapPkcs1PrivateKey(byte[] pkcs1) {
        byte[] version = new byte[]{0x02, 0x01, 0x00};
        byte[] algorithmIdentifier = new byte[]{
                0x30, 0x0D,
                0x06, 0x09,
                0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        byte[] privateKey = concat(new byte[]{0x04}, derLength(pkcs1.length), pkcs1);
        byte[] body = concat(version, algorithmIdentifier, privateKey);
        return concat(new byte[]{0x30}, derLength(body.length), body);
    }

    private Object invoke(Class<?> type, String methodName, Object... args) throws Exception {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            Object[] converted = convertArgs(method.getParameterTypes(), args);
            if (converted != null) {
                return method.invoke(null, converted);
            }
        }
        throw new IllegalStateException("Finance method not found: " + methodName);
    }

    private void invokeQuietly(Class<?> type, String methodName, Object... args) {
        try {
            invoke(type, methodName, args);
        } catch (Exception ignored) {
            // Best-effort cleanup for native SDK slices/handles.
        }
    }

    private Object[] convertArgs(Class<?>[] parameterTypes, Object[] args) {
        Object[] converted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object value = args[i];
            Class<?> target = parameterTypes[i];
            if (target == long.class || target == Long.class) {
                converted[i] = number(value).longValue();
            } else if (target == int.class || target == Integer.class) {
                converted[i] = number(value).intValue();
            } else if (target == String.class) {
                converted[i] = value == null ? "" : value.toString();
            } else {
                return null;
            }
        }
        return converted;
    }

    private void assertSdkOk(long ret, String message) {
        if (ret != 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, message + ": " + ret);
        }
    }

    private void assertApiOk(JSONObject json, String message) {
        Integer errCode = json == null ? null : json.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    message + ": " + errCode + " " + json.getString("errmsg"));
        }
    }

    private Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private byte[] derLength(int length) {
        if (length < 128) {
            return new byte[]{(byte) length};
        }
        int value = length;
        int bytes = 0;
        while (value > 0) {
            bytes++;
            value >>= 8;
        }
        byte[] encoded = new byte[bytes + 1];
        encoded[0] = (byte) (0x80 | bytes);
        for (int i = bytes; i > 0; i--) {
            encoded[i] = (byte) (length & 0xFF);
            length >>= 8;
        }
        return encoded;
    }

    private byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
