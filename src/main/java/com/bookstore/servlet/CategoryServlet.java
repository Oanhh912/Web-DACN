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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Servlet xử lý hiển thị Trang Danh Mục Sách riêng biệt theo Ảnh mẫu 2.
 * Điều hướng theo URL: /category?name=Văn học hoặc /category?name=Tất cả
 */
@WebServlet(name = "CategoryServlet", urlPatterns = {"/category"})
public class CategoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        String categoryName = request.getParameter("name");
        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = request.getParameter("category");
        }
        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = "Tất cả";
        }
        categoryName = categoryName.trim();

        String sort = request.getParameter("sort");
        if (sort == null || sort.trim().isEmpty()) {
            sort = "newest";
        }

        List<Book> books;
        if ("Tất cả".equalsIgnoreCase(categoryName) || "Tất cả sách".equalsIgnoreCase(categoryName)) {
            books = DataStore.getAllBooks();
        } else {
            books = DataStore.searchBooks("", categoryName, null, null, null, null, "all");
        }

        // Sắp xếp danh sách sách
        if ("bestseller".equalsIgnoreCase(sort)) {
            books.sort((a, b) -> Boolean.compare(b.isBestSeller(), a.isBestSeller()));
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            books.sort(Comparator.comparingDouble(Book::getPrice));
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            books.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
        } else if ("rating".equalsIgnoreCase(sort)) {
            books.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        } else {
            // newest
            books.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        }

        request.setAttribute("currentUser", currentUser);
        request.setAttribute("categoryTitle", categoryName);
        request.setAttribute("books", books);
        request.setAttribute("bookCount", books.size());
        request.setAttribute("currentSort", sort);

        request.getRequestDispatcher("/category.jsp").forward(request, response);
    }
}
