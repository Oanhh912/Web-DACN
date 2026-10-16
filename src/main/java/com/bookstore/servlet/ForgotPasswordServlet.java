package com.bookstore.servlet;

import com.bookstore.data.DataStore;
import com.bookstore.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Servlet xử lý Quên mật khẩu & Đặt lại mật khẩu (Tomcat 10.1 / Jakarta EE 10)
 */
@WebServlet(name = "ForgotPasswordServlet", urlPatterns = {
        "/forgot-password",
        "/api/forgot-password/send-otp",
        "/api/forgot-password/verify-otp",
        "/api/forgot-password/reset-password"
})
public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/forgot-password".equals(path)) {
            request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        String path = request.getServletPath();

        if ("/api/forgot-password/send-otp".equals(path)) {
            String email = request.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng nhập địa chỉ email của bạn!\"}");
                return;
            }

            DataStore.PasswordResetResult result = DataStore.requestPasswordResetOtp(email.trim());
            response.setStatus(result.success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(String.format("{\"success\":%b,\"message\":\"%s\",\"cooldown\":%d}",
                    result.success, escapeJson(result.message), result.cooldownSeconds));

        } else if ("/api/forgot-password/verify-otp".equals(path)) {
            String email = request.getParameter("email");
            String otp = request.getParameter("otp");

            if (email == null || email.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng nhập đầy đủ email và mã OTP!\"}");
                return;
            }

            DataStore.PasswordResetVerifyResult result = DataStore.verifyPasswordResetOtp(email.trim(), otp.trim());
            response.setStatus(result.success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(String.format("{\"success\":%b,\"message\":\"%s\",\"resetToken\":\"%s\"}",
                    result.success, escapeJson(result.message), result.resetToken != null ? escapeJson(result.resetToken) : ""));

        } else if ("/api/forgot-password/reset-password".equals(path)) {
            String email = request.getParameter("email");
            String resetToken = request.getParameter("resetToken");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");

            if (email == null || email.trim().isEmpty() || resetToken == null || resetToken.trim().isEmpty()
                    || newPassword == null || newPassword.isEmpty() || confirmPassword == null || confirmPassword.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng điền đầy đủ các thông tin bắt buộc!\"}");
                return;
            }

            DataStore.PasswordResetResult result = DataStore.executePasswordReset(email.trim(), resetToken.trim(), newPassword, confirmPassword);
            if (result.success) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            }

            response.setStatus(result.success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(String.format("{\"success\":%b,\"message\":\"%s\"}",
                    result.success, escapeJson(result.message)));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
