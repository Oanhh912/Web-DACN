package com.bookstore.servlet;

import com.bookstore.data.DataStore;
import com.bookstore.model.User;
import com.bookstore.service.PasswordValidator;
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

        String agreeTerms = request.getParameter("agreeTerms");
        if (agreeTerms == null || (!"true".equalsIgnoreCase(agreeTerms) && !"on".equalsIgnoreCase(agreeTerms) && !"1".equalsIgnoreCase(agreeTerms))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"code\":\"TERMS_NOT_AGREED\",\"message\":\"Bạn chưa đồng ý điều khoản và yêu cầu phải đồng ý điều khoản mới được đăng ký tài khoản.\"}");
            return;
        }

        if (email.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng nhập địa chỉ Email để nhận mã xác thực OTP!\"}");
            return;
        }

        if (!com.bookstore.service.EmailValidator.isValid(email)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"code\":\"INVALID_EMAIL\",\"message\":\"Bạn đang nhập sai định dạng email! Vui lòng nhập đúng định dạng email chuẩn (ví dụ: yourname@gmail.com).\"}");
            return;
        }

        if (DataStore.isEmailExists(email)) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write("{\"success\":false,\"code\":\"EMAIL_EXISTS\",\"message\":\"Email đã tồn tại! Mỗi email chỉ được đăng ký 1 tài khoản duy nhất. Vui lòng sử dụng email khác hoặc đăng nhập bằng tài khoản hiện có.\"}");
            return;
        }

        if (!isStrongPassword(password)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":" + escapeJson(PasswordValidator.PASSWORD_REQUIREMENT_MESSAGE) + "}");
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
        return PasswordValidator.isValid(password);
    }
}
