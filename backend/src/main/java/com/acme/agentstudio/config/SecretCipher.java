package com.acme.agentstudio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 敏感凭证对称加解密组件。
 * 基于 AES/GCM/NoPadding 算法实现数据库中大模型 API Key、第三方 Webhook Token 等敏感凭证的落地密文存储与内存解密。
 */
@Component
public class SecretCipher {

    /** AES-256 密钥字节数组（由凭证密钥计算 SHA-256 得到） */
    private final byte[] key;

    /** 随机数生成器（用于生成 AES-GCM 初始向量 IV） */
    private final SecureRandom random = new SecureRandom();

    /**
     * 构造函数：初始化加密密钥。
     *
     * @param secret    专用加解密密钥（配置文件中 `app.security.secret-key`）
     * @param jwtSecret 兜底 JWT 密钥（未单独配置加解密密钥时复用）
     */
    public SecretCipher(
            @Value("${app.security.secret-key:}") String secret,
            @Value("${app.security.jwt-secret:}") String jwtSecret
    ) {
        String effectiveSecret = (secret == null || secret.isBlank()) ? jwtSecret : secret;
        if (effectiveSecret == null || effectiveSecret.isBlank()) {
            throw new IllegalStateException("未配置凭证加密密钥。");
        }
        try {
            key = MessageDigest.getInstance("SHA-256").digest(effectiveSecret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("凭证加密密钥初始化失败。", e);
        }
    }

    /**
     * 将敏感明文转换为带前缀的 ENC:base64(iv):base64(ciphertext) 密文串。
     *
     * @param value 敏感明文
     * @return 包含 ENC: 前缀的密文字符串
     */
    public String encrypt(String value) {
        if (value == null || value.isBlank() || value.startsWith("ENC:")) {
            return value;
        }
        try {
            // 1. 生成 12 字节的随机 IV
            byte[] iv = new byte[12];
            random.nextBytes(iv);

            // 2. 初始化 AES-GCM 加密器
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));

            // 3. 执行加密并组装为前缀格式
            byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return "ENC:" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("凭证加密失败。", e);
        }
    }

    /**
     * 将带有 ENC: 前缀的密文解密回敏感明文。
     *
     * @param value 带前缀的密文字符串
     * @return 解密后的原始明文
     */
    public String decrypt(String value) {
        if (value == null || !value.startsWith("ENC:")) {
            return value;
        }
        try {
            // 1. 解析密文结构 [0] -> "ENC", [1] -> base64(iv), [2] -> base64(ciphertext)
            String[] parts = value.split(":", 3);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherText = Base64.getDecoder().decode(parts[2]);

            // 2. 初始化 AES-GCM 解密器
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));

            // 3. 执行解密
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("凭证解密失败，请检查加密密钥是否一致。", e);
        }
    }
}

