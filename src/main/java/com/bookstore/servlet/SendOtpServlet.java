package com.bookstore.servlet;

import com.bookstore.data.DataStore;
import com.bookstore.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet xử lý gửi mã OTP đăng ký tài khoản (Tomcat 10.1 / Jakarta EE 10)
 */
@WebServlet(name = "SendOtpServlet", urlPatterns = {"/api/send-otp"})
public class SendOtpServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        String fullName = request.getParameter("fullName");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String channel = request.getParameter("otpChannel");

        if (fullName == null) fullName = "";
        if (username == null) username = "";
        if (email == null) email = "";
        if (phone == null) phone = "";
        if (password == null) password = "";
        if (confirmPassword == null) confirmPassword = "";
        if (channel == null) channel = "";

        fullName = fullName.trim();
        username = username.trim();
        email = email.trim();
        phone = phone.trim();
        channel = channel.trim();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng điền đầy đủ các thông tin bắt buộc!\"}");
            return;
        }

        if (email.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng nhập địa chỉ Email để nhận mã xác thực OTP!\"}");
            return;
        }

        if (!isStrongPassword(password)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Mật khẩu phải có ít nhất 8 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt!\"}");
            return;
        }

        if (!password.equals(confirmPassword)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Mật khẩu xác nhận không trùng khớp!\"}");
            return;
        }

        channel = "email";

        if (DataStore.findUser(username) != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Tên đăng nhập '" + escapeJson(username) + "' đã được sử dụng! Vui lòng chọn tên khác.\"}");
            return;
        }

        User pendingUser = new User(
                username,
                password,
                fullName,
                email,
                phone,
                "CUSTOMER",
                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
        );

        DataStore.OtpSession session = DataStore.createOtpSession(pendingUser, channel);
        boolean realSmsConfigured = com.bookstore.service.OtpSenderService.isConfigured();
        boolean realEmailConfigured = com.bookstore.service.EmailService.isConfigured();

        String jsonResponse = String.format(
                "{\"success\":true,\"message\":\"Mã xác thực OTP đã được gửi thành công!\",\"channel\":%s,\"target\":%s,\"isRealSms\":%b,\"isRealEmail\":%b}",
                escapeJson(session.getChannel()),
                escapeJson(session.getTarget()),
                realSmsConfigured,
                realEmailConfigured
        );
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(jsonResponse);
    }

    private String escapeJson(String input) {
        if (input == null) return "\"\"";
        return "\"" + input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
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
            } else {
                hasSpecial = true;
            }
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
