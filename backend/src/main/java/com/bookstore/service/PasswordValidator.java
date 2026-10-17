package com.bookstore.service;

/**
 * Lớp kiểm tra ràng buộc mật khẩu tập trung cho toàn bộ hệ thống Bookora.
 * 
 * Quy tắc:
 * - Tối thiểu 8 ký tự.
 * - Có ít nhất một chữ cái viết hoa (A-Z).
 * - Có ít nhất một chữ cái viết thường (a-z).
 * - Có ít nhất một chữ số (0-9).
 * - Có ít nhất một ký tự đặc biệt (!@#$%^&*...).
 * - Không chấp nhận mật khẩu rỗng hoặc chứa khoảng trắng.
 */
public class PasswordValidator {

    public static final String PASSWORD_REQUIREMENT_MESSAGE = 
            "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.";

    public static final String PASSWORD_MISMATCH_MESSAGE = 
            "Mật khẩu xác nhận không trùng khớp! Vui lòng kiểm tra lại.";

    /**
     * Kiểm tra tính hợp lệ của mật khẩu theo chuẩn an toàn
     */
    public static boolean isValid(String password) {
        if (password == null || password.trim().isEmpty() || password.length() < 8) {
            return false;
        }

        // Không chấp nhận mật khẩu chứa khoảng trắng
        if (password.contains(" ") || password.contains("\t") || password.contains("\n") || password.contains("\r")) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(c)) {
                hasSpecial = true;
            }
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
