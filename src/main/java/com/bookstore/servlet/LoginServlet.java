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
 * Servlet xử lý đăng nhập người dùng.
 * Áp dụng chuẩn Jakarta EE 10 (Tomcat 10.1).
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        // Nếu đã đăng nhập rồi thì chuyển thẳng vào trang chủ
        if (session != null && session.getAttribute("currentUser") != null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        // Chuyển tiếp tới giao diện đăng nhập
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Thiết lập mã hóa UTF-8
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String remember = request.getParameter("remember");

        // Kiểm tra hợp lệ
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        username = username.trim();
        if (DataStore.validateUser(username, password)) {
            User user = DataStore.findUser(username);
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user);

            // Cấu hình thời gian sống của session (30 phút hoặc lâu hơn nếu ghi nhớ)
            if ("on".equalsIgnoreCase(remember) || "true".equalsIgnoreCase(remember)) {
                session.setMaxInactiveInterval(7 * 24 * 60 * 60); // 7 ngày
            } else {
                session.setMaxInactiveInterval(30 * 60); // 30 phút
            }

            // Chuyển hướng thành công vào trang chủ
            response.sendRedirect(request.getContextPath() + "/home");
        } else {
            request.setAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác! Vui lòng thử lại.");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }
}
