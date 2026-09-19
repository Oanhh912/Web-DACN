package com.bookstore.servlet;

import com.bookstore.data.DataStore;
import com.bookstore.model.Book;
import com.bookstore.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Servlet xử lý hiển thị Trang chủ bán sách sau khi đăng nhập thành công.
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"/home", "/index"})
public class HomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Cho phép khách vãng lai (currentUser == null) xem trang chủ và danh mục sách

        // Lấy từ khóa tìm kiếm & danh mục (nếu có)
        String keyword = request.getParameter("q");
        String category = request.getParameter("category");

        List<Book> books;
        if ((keyword != null && !keyword.trim().isEmpty()) || (category != null && !category.trim().isEmpty())) {
            books = DataStore.searchBooks(keyword, category);
        } else {
            books = DataStore.getAllBooks();
        }

        List<String> categories = DataStore.getCategories();

        // Đưa dữ liệu vào request attributes
        request.setAttribute("currentUser", currentUser);
        request.setAttribute("books", books);
        request.setAttribute("categories", categories);
        request.setAttribute("currentCategory", (category != null) ? category : "Tất cả");
        request.setAttribute("searchKeyword", (keyword != null) ? keyword : "");

        // Chuyển tiếp tới trang chủ
        request.getRequestDispatcher("/home.jsp").forward(request, response);
    }
}
