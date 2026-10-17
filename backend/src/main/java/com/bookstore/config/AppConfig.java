package com.bookstore.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Cấu hình tập trung cho ứng dụng Bookora.
 *
 * Ưu tiên đọc từ file cấu hình bên ngoài (config/) trước,
 * sau đó fallback về giá trị mặc định trong code.
 *
 * Cách sử dụng:
 *   int port = AppConfig.getPort();
 *   Path webappDir = AppConfig.getWebappDir();
 */
public class AppConfig {

    // ─── Server ───────────────────────────────────────────────────────────────
    /** Cổng mặc định của máy chủ */
    public static final int DEFAULT_PORT = 8080;

    /** Cổng dự phòng nếu cổng mặc định đang bận */
    public static final int FALLBACK_PORT = 8082;

    // ─── Database ─────────────────────────────────────────────────────────────
    public static final String DB_HOST     = "127.0.0.1";
    public static final String DB_PORT     = "3306";
    public static final String DB_NAME     = "web_bookora";
    public static final String DB_USER     = "root";
    public static final String DB_PASSWORD = "";

    // ─── Session ──────────────────────────────────────────────────────────────
    /** Tên cookie phiên đăng nhập */
    public static final String SESSION_COOKIE_NAME    = "BOOKSTORE_SESSION";
    /** Thời gian sống phiên thường (giây) */
    public static final int    SESSION_NORMAL_SECONDS  = 30 * 60;        // 30 phút
    /** Thời gian sống phiên "ghi nhớ đăng nhập" (giây) */
    public static final int    SESSION_REMEMBER_SECONDS = 7 * 24 * 3600; // 7 ngày

    // ─── Đường dẫn ────────────────────────────────────────────────────────────
    /**
     * Thư mục chứa toàn bộ file tĩnh Frontend (HTML, CSS, JS, images...).
     *
     * Khi chạy từ thư mục gốc dự án:
     *   - Cấu trúc MỚI  → frontend/public  (ưu tiên)
     *   - Cấu trúc CŨ   → src/main/webapp  (fallback tương thích)
     */
    public static Path getWebappDir() {
        // Kiểm tra cấu trúc mới trước
        Path newPath = Paths.get("frontend", "public");
        if (newPath.toFile().exists()) {
            return newPath;
        }
        // Fallback về cấu trúc cũ để tương thích
        return Paths.get("src", "main", "webapp");
    }

    /**
     * Thư mục chứa các trang HTML phân nhóm (pages/).
     * Trả về null nếu không dùng cấu trúc phân nhóm pages.
     */
    public static Path getPagesDir() {
        Path pagesPath = Paths.get("frontend", "pages");
        return pagesPath.toFile().exists() ? pagesPath : null;
    }

    /**
     * Thư mục chứa các file cấu hình bí mật (email, gemini key...).
     * Ưu tiên: config/ → thư mục gốc
     */
    public static Path getConfigDir() {
        Path configPath = Paths.get("config");
        return configPath.toFile().exists() ? configPath : Paths.get(".");
    }

    /**
     * Tìm file cấu hình: kiểm tra trong config/ trước, sau đó tìm ở thư mục gốc.
     */
    public static File getConfigFile(String filename) {
        File f = new File("config", filename);
        if (f.exists()) return f;
        return new File(filename);
    }

    /**
     * Tìm Path của file cấu hình: kiểm tra trong config/ trước, sau đó tìm ở thư mục gốc.
     */
    public static Path getConfigPath(String filename) {
        Path p = Paths.get("config", filename);
        if (java.nio.file.Files.exists(p)) return p;
        return Paths.get(filename);
    }

    // ─── Tải cấu hình từ file ─────────────────────────────────────────────────
    private static final Properties appProps = new Properties();

    static {
        loadProperties("app.properties");
    }

    private static void loadProperties(String filename) {
        // Tìm file trong config/ trước, sau đó thư mục gốc
        File[] candidates = {
            new File("config", filename),
            new File(filename)
        };
        for (File f : candidates) {
            if (f.exists()) {
                try (InputStream is = new FileInputStream(f)) {
                    appProps.load(is);
                    System.out.println("📋 Đã tải cấu hình từ: " + f.getAbsolutePath());
                    return;
                } catch (Exception e) {
                    System.err.println("⚠️ Không thể đọc " + f + ": " + e.getMessage());
                }
            }
        }
    }

    /** Lấy cổng máy chủ (có thể override bằng system property -Dserver.port=...) */
    public static int getPort() {
        String sysProp = System.getProperty("server.port");
        if (sysProp != null) {
            try { return Integer.parseInt(sysProp.trim()); } catch (NumberFormatException ignored) {}
        }
        String prop = appProps.getProperty("server.port");
        if (prop != null) {
            try { return Integer.parseInt(prop.trim()); } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_PORT;
    }

    /** Lấy giá trị cấu hình tùy chỉnh */
    public static String get(String key, String defaultValue) {
        return appProps.getProperty(key, defaultValue);
    }

    // ─── Đường dẫn XAMPP MySQL ────────────────────────────────────────────────
    public static final String XAMPP_MYSQLD_PATH    = "C:\\xampp\\mysql\\bin\\mysqld.exe";
    public static final String XAMPP_MYSQL_INI_PATH = "C:\\xampp\\mysql\\bin\\my.ini";
}
