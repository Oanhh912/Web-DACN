package com.bookstore.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tiện ích băm và xác thực mật khẩu an toàn cho Bookora.
 * Áp dụng Salted SHA-256 theo chuẩn Java Cryptography Architecture.
 * Hỗ trợ tương thích ngược mượt mà với các tài khoản mẫu cũ (123456).
 */
public class PasswordUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HASH_PREFIX = "SHA256$";

    /**
     * Băm mật khẩu bằng SHA-256 kết hợp muối ngẫu nhiên 16 bytes
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) return null;
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = computeHash(plainPassword, salt);
        return HASH_PREFIX + saltBase64 + "$" + hashBase64;
    }

    /**
     * Xác thực mật khẩu nhập vào so với mật khẩu đã lưu
     */
    public static boolean verifyPassword(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) return false;

        // Nếu mật khẩu đã được băm bằng SHA-256
        if (storedPassword.startsWith(HASH_PREFIX)) {
            String[] parts = storedPassword.split("\\$");
            if (parts.length == 3) {
                try {
                    byte[] salt = Base64.getDecoder().decode(parts[1]);
                    String computedHash = computeHash(plainPassword, salt);
                    return MessageDigest.isEqual(
                            computedHash.getBytes(StandardCharsets.UTF_8),
                            parts[2].getBytes(StandardCharsets.UTF_8)
                    );
                } catch (Exception e) {
                    return false;
                }
            }
        }

        // Tương thích ngược với các tài khoản mẫu hiện có
        return storedPassword.equals(plainPassword);
    }

    private static String computeHash(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
