package com.bookstore.servlet;

import com.bookstore.data.DataStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet xác thực mã OTP đăng ký tài khoản (Tomcat 10.1 / Jakarta EE 10)
 */
@WebServlet(name = "VerifyOtpServlet", urlPatterns = {"/api/verify-otp"})
public class VerifyOtpServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        String username = request.getParameter("username");
        String otp = request.getParameter("otp");

        if (username == null) username = "";
        if (otp == null) otp = "";

        username = username.trim();
        otp = otp.trim();

        if (username.isEmpty() || otp.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng nhập mã xác thực OTP 6 số!\"}");
            return;
        }

        DataStore.OtpSession session = DataStore.getOtpSession(username);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Phiên xác thực không tồn tại hoặc đã hết hạn. Vui lòng thử lại!\"}");
            return;
        }

        if (session.isExpired()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Mã OTP đã hết hạn (sau 2 phút). Vui lòng bấm gửi lại mã mới!\"}");
            return;
        }

        if (!session.getCode().equals(otp)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Mã OTP không chính xác! Vui lòng kiểm tra lại.\"}");
            return;
        }

        boolean registered = DataStore.verifyAndRegister(username, otp);
        if (registered) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":true,\"message\":\"Đăng ký tài khoản thành công!\",\"redirect\":\"login?message=register_success\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"message\":\"Có lỗi khi lưu vào cơ sở dữ liệu MySQL. Vui lòng thử lại!\"}");
        }
    }
}
