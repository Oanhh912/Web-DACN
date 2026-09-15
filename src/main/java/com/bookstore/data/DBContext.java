package com.bookstore.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Lớp kết nối cơ sở dữ liệu MySQL theo thông tin từ MySQL Workbench:
 * - Host: 127.0.0.1
 * - Port: 3306
 * - User: root
 * - Password: (trống)
 * - Database: web_bookora
 */
public class DBContext {
    private static final String HOST = "127.0.0.1";
    private static final String PORT = "3306";
    private static final String DB_NAME = "web_bookora";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Mặc định trên XAMPP / Local MySQL

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME 
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";

    static {
        try {
            // Nạp MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("⚠️ Chưa tìm thấy MySQL JDBC Driver (com.mysql.cj.jdbc.Driver): " + e.getMessage());
        }
    }

    /**
     * Mở kết nối mới tới cơ sở dữ liệu MySQL
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Kiểm tra trạng thái kết nối MySQL
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("⚠️ Kết nối MySQL thất bại: " + e.getMessage());
            return false;
        }
    }
}
