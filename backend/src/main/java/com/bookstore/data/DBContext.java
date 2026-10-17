package com.bookstore.data;

import com.bookstore.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Lớp kết nối cơ sở dữ liệu MySQL.
 *
 * Thông tin kết nối được lấy từ AppConfig (config tập trung):
 *   - Host: 127.0.0.1
 *   - Port: 3306
 *   - Database: web_bookora
 *   - User: root
 *   - Password: (mặc định trống trên XAMPP/Local MySQL)
 *
 * Để thay đổi thông tin kết nối, chỉnh sửa AppConfig.java
 * hoặc tạo file config/db_config.properties (xem .env.example).
 */
public class DBContext {
    private static final String URL =
            "jdbc:mysql://"
            + AppConfig.DB_HOST + ":" + AppConfig.DB_PORT + "/" + AppConfig.DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("⚠️ Chưa tìm thấy MySQL JDBC Driver (com.mysql.cj.jdbc.Driver): " + e.getMessage());
        }
    }

    /**
     * Mở kết nối mới tới cơ sở dữ liệu MySQL.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, AppConfig.DB_USER, AppConfig.DB_PASSWORD);
    }

    /**
     * Kiểm tra trạng thái kết nối MySQL.
     *
     * @return true nếu kết nối thành công, false nếu thất bại
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
