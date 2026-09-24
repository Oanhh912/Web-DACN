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

        // Lấy các tham số tìm kiếm & bộ lọc đa tiêu chí theo Use Case của BA
        String keyword = request.getParameter("q");
        String category = request.getParameter("category");
        String author = request.getParameter("author");
        String publisher = request.getParameter("publisher");
        String stockStatus = request.getParameter("stockStatus");
        
        Double minPrice = null;
        Double maxPrice = null;
        try {
            String minP = request.getParameter("minPrice");
            if (minP != null && !minP.trim().isEmpty()) {
                minPrice = Double.parseDouble(minP.trim());
            }
        } catch (NumberFormatException ignored) {}

        try {
            String maxP = request.getParameter("maxPrice");
            if (maxP != null && !maxP.trim().isEmpty()) {
                maxPrice = Double.parseDouble(maxP.trim());
            }
        } catch (NumberFormatException ignored) {}

        List<Book> books = DataStore.searchBooks(keyword, category, author, publisher, minPrice, maxPrice, stockStatus);

        List<String> categories = DataStore.getCategories();
        List<String> authors = DataStore.getAuthors();
        List<String> publishers = DataStore.getPublishers();
        List<DataStore.PromotionItem> promotions = DataStore.getPromotions();

        // Đưa dữ liệu vào request attributes
        request.setAttribute("currentUser", currentUser);
        request.setAttribute("books", books);
        request.setAttribute("categories", categories);
        request.setAttribute("authors", authors);
        request.setAttribute("publishers", publishers);
        request.setAttribute("promotions", promotions);
        request.setAttribute("currentCategory", (category != null) ? category : "Tất cả");
        request.setAttribute("currentAuthor", (author != null) ? author : "Tất cả");
        request.setAttribute("currentPublisher", (publisher != null) ? publisher : "Tất cả");
        request.setAttribute("currentStockStatus", (stockStatus != null) ? stockStatus : "all");
        request.setAttribute("currentMinPrice", minPrice);
        request.setAttribute("currentMaxPrice", maxPrice);
        request.setAttribute("searchKeyword", (keyword != null) ? keyword : "");

        // Chuyển tiếp tới trang chủ
        request.getRequestDispatcher("/home.jsp").forward(request, response);
    }
}
