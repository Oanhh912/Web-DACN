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
 * Servlet xử lý đăng ký tài khoản thành viên mới.
 * Tương thích chuẩn Jakarta EE 10 (Tomcat 10.1).
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String fullName = request.getParameter("fullName");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // Lưu lại dữ liệu người dùng đã nhập để điền lại vào form nếu có lỗi
        request.setAttribute("fullName", fullName);
        request.setAttribute("username", username);
        request.setAttribute("email", email);
        request.setAttribute("phone", phone);

        // 1. Kiểm tra các trường bắt buộc
        boolean hasEmail = email != null && !email.trim().isEmpty();

        if (fullName == null || fullName.trim().isEmpty() ||
            username == null || username.trim().isEmpty() ||
            !hasEmail ||
            password == null || password.trim().isEmpty() ||
            confirmPassword == null || confirmPassword.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Vui lòng nhập đầy đủ họ tên, tên đăng nhập, địa chỉ email và mật khẩu!");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        username = username.trim();
        fullName = fullName.trim();
        email = email.trim();

        // 2. Kiểm tra mật khẩu an toàn
        if (!isStrongPassword(password)) {
            request.setAttribute("errorMessage", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt!");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        // 3. Kiểm tra mật khẩu xác nhận trùng khớp
        if (!password.equals(confirmPassword)) {
            request.setAttribute("errorMessage", "Mật khẩu xác nhận không khớp! Vui lòng kiểm tra lại.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        // 4. Kiểm tra tên đăng nhập đã tồn tại trong MySQL chưa
        if (DataStore.findUser(username) != null) {
            request.setAttribute("errorMessage", "Tên đăng nhập '" + username + "' đã được sử dụng! Vui lòng chọn tên khác.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        // 5. Khởi tạo đối tượng User mới với vai trò mặc định là CUSTOMER
        String defaultAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150";
        User newUser = new User(username, password, fullName, email, phone, "CUSTOMER", defaultAvatar);

        boolean success = DataStore.registerUser(newUser);
        if (success) {
            // Đăng ký thành công -> Chuyển hướng về trang đăng nhập với thông báo thành công
            response.sendRedirect(request.getContextPath() + "/login?message=register_success");
        } else {
            request.setAttribute("errorMessage", "Không thể hoàn tất đăng ký do lỗi cơ sở dữ liệu. Vui lòng thử lại sau!");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
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
