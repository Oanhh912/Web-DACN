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
 * Servlet xử lý hiển thị trang Hồ sơ tài khoản và Đổi mật khẩu (Jakarta EE).
 */
@WebServlet(name = "ProfileServlet", urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=require_login");
            return;
        }

        // Lấy thông tin người dùng mới nhất từ DataStore
        User freshUser = DataStore.findUser(currentUser.getUsername());
        if (freshUser != null) {
            session.setAttribute("currentUser", freshUser);
            request.setAttribute("currentUser", freshUser);
        } else {
            request.setAttribute("currentUser", currentUser);
        }

        request.setAttribute("activeTab", "profile");
        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=require_login");
            return;
        }

        String action = request.getParameter("action");
        String activeTab = "profile";
        String successMsg = null;
        String errorMsg = null;

        if ("updateProfile".equalsIgnoreCase(action)) {
            String fullName = request.getParameter("fullName");
            String email = request.getParameter("email");
            String phone = request.getParameter("phone");
            String avatar = request.getParameter("avatar");

            if (fullName == null || fullName.trim().isEmpty()) {
                errorMsg = "Họ và tên không được để trống!";
            } else {
                boolean updated = DataStore.updateUserProfile(currentUser.getUsername(), fullName, email, phone, avatar);
                if (updated) {
                    successMsg = "Cập nhật thông tin hồ sơ tài khoản thành công!";
                    User freshUser = DataStore.findUser(currentUser.getUsername());
                    session.setAttribute("currentUser", freshUser);
                    currentUser = freshUser;
                } else {
                    errorMsg = "Có lỗi xảy ra khi lưu thông tin. Vui lòng thử lại!";
                }
            }
            activeTab = "profile";

        } else if ("changePassword".equalsIgnoreCase(action)) {
            activeTab = "password";
            String oldPassword = request.getParameter("oldPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");

            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                errorMsg = "Vui lòng nhập mật khẩu hiện tại!";
            } else if (newPassword == null || newPassword.trim().isEmpty()) {
                errorMsg = "Vui lòng nhập mật khẩu mới!";
            } else if (newPassword.length() < 8) {
                errorMsg = "Mật khẩu mới phải có ít nhất 8 ký tự!";
            } else if (!isStrongPassword(newPassword)) {
                errorMsg = "Mật khẩu mới phải bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt!";
            } else if (!newPassword.equals(confirmPassword)) {
                errorMsg = "Mật khẩu xác nhận không trùng khớp với mật khẩu mới!";
            } else {
                String changeResult = DataStore.changePassword(currentUser.getUsername(), oldPassword, newPassword);
                if (changeResult == null) {
                    successMsg = "Đổi mật khẩu thành công! Hãy ghi nhớ mật khẩu mới của bạn.";
                } else {
                    errorMsg = changeResult;
                }
            }
        }

        User freshUser = DataStore.findUser(currentUser.getUsername());
        request.setAttribute("currentUser", freshUser != null ? freshUser : currentUser);
        request.setAttribute("activeTab", activeTab);
        request.setAttribute("successMessage", successMsg);
        request.setAttribute("errorMessage", errorMsg);

        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
