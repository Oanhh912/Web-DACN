package com.bookstore.server;

import com.bookstore.data.DataStore;
import com.bookstore.model.Book;
import com.bookstore.model.User;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Máy chủ Java độc lập (Standalone Java Web Server).
 * Cho phép chạy trực tiếp ứng dụng web bán sách mà không cần cấu hình Tomcat hay cài đặt thêm phần mềm.
 */
public class BookstoreApp {
    private static final int DEFAULT_PORT = 8080;
    private static final Path WEBAPP_DIR = Paths.get("src", "main", "webapp");
    // Bộ nhớ quản lý session: Token -> Username
    private static final Map<String, String> activeSessions = new HashMap<>();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            try {
                port = 8082;
                server = HttpServer.create(new InetSocketAddress(port), 0);
            } catch (IOException ex) {
                System.err.println("Không thể khởi động server: " + ex.getMessage());
                return;
            }
        }

        // Định tuyến các đường dẫn
        server.createContext("/", new RootHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/home", new HomeHandler());
        server.createContext("/logout", new LogoutHandler());
        server.createContext("/api/books", new ApiBooksHandler());
        server.createContext("/api/me", new ApiMeHandler());
        server.createContext("/api/send-otp", new SendOtpHandler());
        server.createContext("/api/verify-otp", new VerifyOtpHandler());
        server.createContext("/db", new DatabaseViewerHandler());
        server.createContext("/css/", new StaticFileHandler());
        server.createContext("/js/", new StaticFileHandler());
        server.createContext("/images/", new StaticFileHandler());

        server.setExecutor(null); // Sử dụng executor mặc định
        server.start();

        System.out.println("===============================================================");
        System.out.println("🚀 MÁY CHỦ JAVA BOOKSTORE ĐÃ KHỞI CHẠY THÀNH CÔNG!");
        System.out.println("🌐 Địa chỉ truy cập: http://localhost:" + port + "/login");
        System.out.println("📌 Tài khoản mẫu:");
        System.out.println("   - Admin:     admin     / 123456");
        System.out.println("   - Người mua: oanh      / 123456");
        System.out.println("   - Khách:     khachhang / 123456");
        System.out.println("===============================================================");
    }

    /**
     * Điều hướng trang gốc /
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                redirect(exchange, "/home");
            } else {
                new StaticFileHandler().handle(exchange);
            }
        }
    }

    /**
     * Xử lý GET/POST cho trang Đăng nhập (/login)
     */
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                User user = getAuthenticatedUser(exchange);
                if (user != null) {
                    redirect(exchange, "/home");
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                String message = "";
                if (query != null && query.contains("logged_out")) {
                    message = "Bạn đã đăng xuất an toàn khỏi hệ thống!";
                } else if (query != null && query.contains("require_login")) {
                    message = "Vui lòng đăng nhập để tiếp tục truy cập trang chủ!";
                } else if (query != null && query.contains("register_success")) {
                    message = "Đăng ký tài khoản thành công! Vui lòng đăng nhập để tiếp tục.";
                }

                String html = renderLoginPage("", message, "");
                sendResponse(exchange, 200, "text/html; charset=UTF-8", html);

            } else if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> params = parseFormData(exchange);
                String username = params.getOrDefault("username", "").trim();
                String password = params.getOrDefault("password", "").trim();
                String remember = params.getOrDefault("remember", "");

                if (username.isEmpty() || password.isEmpty()) {
                    String html = renderLoginPage("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!", "", username);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                    return;
                }

                if (DataStore.validateUser(username, password)) {
                    // Đăng nhập thành công -> Tạo session token
                    String token = UUID.randomUUID().toString();
                    activeSessions.put(token, username.toLowerCase());

                    String cookieHeader = "BOOKSTORE_SESSION=" + token + "; Path=/; HttpOnly";
                    if ("on".equals(remember) || "true".equals(remember)) {
                        cookieHeader += "; Max-Age=" + (7 * 24 * 3600); // 7 ngày
                    }
                    exchange.getResponseHeaders().add("Set-Cookie", cookieHeader);
                    redirect(exchange, "/home");
                } else {
                    String html = renderLoginPage("Tên đăng nhập hoặc mật khẩu không chính xác!", "", username);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                }
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    /**
     * Xử lý GET/POST cho trang Đăng ký (/register)
     */
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                User user = getAuthenticatedUser(exchange);
                if (user != null) {
                    redirect(exchange, "/home");
                    return;
                }

                String html = renderRegisterPage("", "", "", "", "");
                sendResponse(exchange, 200, "text/html; charset=UTF-8", html);

            } else if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> params = parseFormData(exchange);
                String fullName = params.getOrDefault("fullName", "").trim();
                String username = params.getOrDefault("username", "").trim();
                String email = params.getOrDefault("email", "").trim();
                String phone = params.getOrDefault("phone", "").trim();
                String password = params.getOrDefault("password", "").trim();
                String confirmPassword = params.getOrDefault("confirmPassword", "").trim();

                if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    String html = renderRegisterPage("Vui lòng nhập đầy đủ các trường thông tin bắt buộc!", fullName, username, email, phone);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                    return;
                }

                if (!isStrongPassword(password)) {
                    String html = renderRegisterPage("Mật khẩu phải có ít nhất 8 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt!", fullName, username, email, phone);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    String html = renderRegisterPage("Mật khẩu xác nhận không khớp! Vui lòng thử lại.", fullName, username, email, phone);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                    return;
                }

                if (DataStore.findUser(username) != null) {
                    String html = renderRegisterPage("Tên đăng nhập '" + username + "' đã được sử dụng! Vui lòng chọn tên khác.", fullName, "", email, phone);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                    return;
                }

                User newUser = new User(
                        username,
                        password,
                        fullName,
                        email,
                        phone,
                        "CUSTOMER",
                        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                );

                boolean success = DataStore.registerUser(newUser);
                if (success) {
                    redirect(exchange, "/login?message=register_success");
                } else {
                    String html = renderRegisterPage("Có lỗi khi tạo tài khoản trong cơ sở dữ liệu. Vui lòng thử lại!", fullName, username, email, phone);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                }
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    /**
     * API gửi mã xác thực OTP đăng ký (/api/send-otp)
     */
    static class SendOtpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, String> params = parseFormData(exchange);
            String fullName = params.getOrDefault("fullName", "").trim();
            String username = params.getOrDefault("username", "").trim();
            String email = params.getOrDefault("email", "").trim();
            String phone = params.getOrDefault("phone", "").trim();
            String password = params.getOrDefault("password", "").trim();
            String confirmPassword = params.getOrDefault("confirmPassword", "").trim();
            String channel = params.getOrDefault("otpChannel", "").trim();

            if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng điền đầy đủ các thông tin bắt buộc!\"}");
                return;
            }

            if (email.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng nhập địa chỉ Email để nhận mã xác thực OTP!\"}");
                return;
            }

            if (!isStrongPassword(password)) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mật khẩu phải có ít nhất 8 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt!\"}");
                return;
            }

            if (!password.equals(confirmPassword)) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mật khẩu xác nhận không trùng khớp!\"}");
                return;
            }

            channel = "email";

            if (DataStore.findUser(username) != null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tên đăng nhập '" + escapeJson(username).replace("\"", "") + "' đã được sử dụng! Vui lòng chọn tên khác.\"}");
                return;
            }

            User pendingUser = new User(
                    username,
                    password,
                    fullName,
                    email,
                    phone,
                    "CUSTOMER",
                    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            );

            DataStore.OtpSession session = DataStore.createOtpSession(pendingUser, channel);
            boolean realSmsConfigured = com.bookstore.service.OtpSenderService.isConfigured();
            boolean realEmailConfigured = com.bookstore.service.EmailService.isConfigured();

            String jsonResponse = String.format(
                    "{\"success\":true,\"message\":\"Mã xác thực OTP đã được gửi thành công!\",\"channel\":%s,\"target\":%s,\"isRealSms\":%b,\"isRealEmail\":%b}",
                    escapeJson(session.getChannel()),
                    escapeJson(session.getTarget()),
                    realSmsConfigured,
                    realEmailConfigured
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", jsonResponse);
        }
    }

    /**
     * API xác thực mã OTP và hoàn tất đăng ký (/api/verify-otp)
     */
    static class VerifyOtpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, String> params = parseFormData(exchange);
            String username = params.getOrDefault("username", "").trim();
            String otp = params.getOrDefault("otp", "").trim();

            if (username.isEmpty() || otp.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng nhập mã xác thực OTP 6 số!\"}");
                return;
            }

            DataStore.OtpSession session = DataStore.getOtpSession(username);
            if (session == null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Phiên xác thực không tồn tại hoặc đã hết hạn. Vui lòng thử lại!\"}");
                return;
            }

            if (session.isExpired()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã OTP đã hết hạn (sau 2 phút). Vui lòng bấm gửi lại mã mới!\"}");
                return;
            }

            if (!session.getCode().equals(otp)) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã OTP không chính xác! Vui lòng kiểm tra lại.\"}");
                return;
            }

            boolean registered = DataStore.verifyAndRegister(username, otp);
            if (registered) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Đăng ký tài khoản thành công!\",\"redirect\":\"login?message=register_success\"}");
            } else {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Có lỗi khi lưu vào cơ sở dữ liệu MySQL. Vui lòng thử lại!\"}");
            }
        }
    }

    /**
     * Xử lý GET cho trang Chủ (/home)
     */
    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);

            // Đọc query parameters nếu có
            String query = exchange.getRequestURI().getQuery();
            String keyword = "";
            String category = "Tất cả";
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("q")) keyword = qp.get("q");
                if (qp.containsKey("category")) category = qp.get("category");
            }

            List<Book> books = DataStore.searchBooks(keyword, category);
            List<String> categories = DataStore.getCategories();

            String html = renderHomePage(user, books, categories, category, keyword);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * Xử lý Đăng xuất (/logout)
     */
    static class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String token = getSessionToken(exchange);
            if (token != null) {
                activeSessions.remove(token);
            }
            exchange.getResponseHeaders().add("Set-Cookie", "BOOKSTORE_SESSION=; Path=/; Max-Age=0");
            redirect(exchange, "/login?message=logged_out");
        }
    }

    /**
     * API trả về danh sách sách dưới dạng JSON
     */
    static class ApiBooksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String keyword = "";
            String category = "Tất cả";
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("q")) keyword = qp.get("q");
                if (qp.containsKey("category")) category = qp.get("category");
            }

            List<Book> books = DataStore.searchBooks(keyword, category);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                        "{\"id\":%d,\"code\":%s,\"title\":%s,\"author\":%s,\"price\":%.0f,\"originalPrice\":%.0f,\"formattedPrice\":%s,\"category\":%s,\"rating\":%.1f,\"reviewCount\":%d,\"image\":%s,\"description\":%s,\"isBestSeller\":%b}",
                        b.getId(),
                        escapeJson(b.getCode()),
                        escapeJson(b.getTitle()),
                        escapeJson(b.getAuthor()),
                        b.getPrice(),
                        b.getOriginalPrice(),
                        escapeJson(b.getFormattedPrice()),
                        escapeJson(b.getCategory()),
                        b.getRating(),
                        b.getReviewCount(),
                        escapeJson(b.getImage()),
                        escapeJson(b.getDescription()),
                        b.isBestSeller()
                ));
            }
            sb.append("]");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", sb.toString());
        }
    }

    /**
     * API trả về thông tin người dùng hiện tại
     */
    static class ApiMeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                sendResponse(exchange, 401, "application/json", "{\"error\":\"Unauthorized\"}");
                return;
            }
            String json = String.format(
                    "{\"username\":%s,\"fullName\":%s,\"email\":%s,\"role\":%s,\"avatar\":%s}",
                    escapeJson(user.getUsername()),
                    escapeJson(user.getFullName()),
                    escapeJson(user.getEmail()),
                    escapeJson(user.getRole()),
                    escapeJson(user.getAvatar())
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    /**
     * Phục vụ các file tĩnh (CSS, JS, Hình ảnh)
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/")) path = path.substring(1);
            Path filePath = WEBAPP_DIR.resolve(path);

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String mime = getMimeType(filePath.toString());
                byte[] bytes = Files.readAllBytes(filePath);
                exchange.getResponseHeaders().set("Content-Type", mime);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } else {
                String msg = "404 Not Found: " + path;
                exchange.sendResponseHeaders(404, msg.length());
                OutputStream os = exchange.getResponseBody();
                os.write(msg.getBytes());
                os.close();
            }
        }
    }

    // ====================== TEMPLATE RENDERING ======================

    private static String renderLoginPage(String error, String info, String username) {
        Path templatePath = WEBAPP_DIR.resolve("login.html");
        String content = "";
        try {
            content = Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            content = "<h1>Login Template Missing</h1>";
        }

        String errorHtml = (error != null && !error.isEmpty()) 
                ? "<div class=\"alert alert-danger\"><i class=\"fas fa-exclamation-circle\"></i> " + error + "</div>" 
                : "";
        String infoHtml = (info != null && !info.isEmpty()) 
                ? "<div class=\"alert alert-success\"><i class=\"fas fa-check-circle\"></i> " + info + "</div>" 
                : "";

        content = content.replace("<!-- ${ALERT_MESSAGE} -->", errorHtml + infoHtml);
        content = content.replace("${username}", (username != null) ? username : "");
        return content;
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

    private static String renderRegisterPage(String error, String fullName, String username, String email, String phone) {
        Path templatePath = WEBAPP_DIR.resolve("register.html");
        String content = "";
        try {
            content = Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            content = "<h1>Register Template Missing</h1>";
        }

        String errorHtml = (error != null && !error.isEmpty()) 
                ? "<div class=\"alert alert-danger\"><i class=\"fas fa-exclamation-circle\"></i> " + error + "</div>" 
                : "";

        content = content.replace("<!-- ${ALERT_MESSAGE} -->", errorHtml);
        content = content.replace("${fullName}", (fullName != null) ? escapeAttr(fullName) : "");
        content = content.replace("${username}", (username != null) ? escapeAttr(username) : "");
        content = content.replace("${email}", (email != null) ? escapeAttr(email) : "");
        content = content.replace("${phone}", (phone != null) ? escapeAttr(phone) : "");
        return content;
    }

    private static String renderHomePage(User user, List<Book> books, List<String> categories, String selectedCat, String keyword) {
        Path templatePath = WEBAPP_DIR.resolve("home.html");
        String content = "";
        try {
            content = Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            content = "<h1>Home Template Missing</h1>";
        }

        // Top-bar Auth & Main Header Auth (Khách vãng lai vs Đã đăng nhập)
        if (user != null) {
            String topbarAuth = String.format(
                    "<div class=\"topbar-auth-links\">" +
                    "    <span class=\"topbar-welcome\"><i class=\"fas fa-circle-user\"></i> Xin chào, <strong>%s</strong></span>" +
                    "    <span class=\"topbar-divider\">|</span>" +
                    "    <a href=\"logout\" class=\"topbar-auth-btn\"><i class=\"fas fa-arrow-right-from-bracket\"></i> ĐĂNG XUẤT</a>" +
                    "</div>",
                    escapeAttr(user.getFullName())
            );

            String headerAuth = String.format(
                    "<div class=\"user-dropdown\">\n" +
                    "    <button type=\"button\" class=\"user-profile-trigger\" id=\"userMenuTrigger\">\n" +
                    "        <img src=\"%s\" alt=\"Avatar\" class=\"user-avatar-img\" onerror=\"this.src='https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150';\">\n" +
                    "        <div class=\"user-meta\">\n" +
                    "            <div class=\"user-greeting\">Xin chào,</div>\n" +
                    "            <div class=\"user-fullname\">%s</div>\n" +
                    "        </div>\n" +
                    "        <span class=\"user-role-tag\">%s</span>\n" +
                    "        <i class=\"fas fa-chevron-down\" style=\"font-size: 11px; color: var(--text-muted); margin-left: 4px;\"></i>\n" +
                    "    </button>\n" +
                    "    <div class=\"user-menu-dropdown\" id=\"userMenuDropdown\">\n" +
                    "        <div class=\"dropdown-header-info\">\n" +
                    "            <div style=\"font-weight: 700; font-size: 13px; color: var(--primary);\">%s</div>\n" +
                    "            <div class=\"dropdown-email\">%s</div>\n" +
                    "        </div>\n" +
                    "        <a href=\"javascript:void(0)\" class=\"dropdown-item\" onclick=\"alert('Trang hồ sơ cá nhân của %s')\">\n" +
                    "            <i class=\"fas fa-user-circle\"></i> Hồ sơ tài khoản\n" +
                    "        </a>\n" +
                    "        <a href=\"javascript:void(0)\" class=\"dropdown-item\" onclick=\"toggleCartDrawer()\">\n" +
                    "            <i class=\"fas fa-box-archive\"></i> Đơn hàng của tôi\n" +
                    "        </a>\n" +
                    "        <a href=\"javascript:void(0)\" class=\"dropdown-item\" onclick=\"alert('Danh sách yêu thích đang được đồng bộ!')\">\n" +
                    "            <i class=\"fas fa-heart\"></i> Sách yêu thích\n" +
                    "        </a>\n" +
                    "        <a href=\"logout\" class=\"dropdown-item logout\">\n" +
                    "            <i class=\"fas fa-arrow-right-from-bracket\"></i> Đăng xuất\n" +
                    "        </a>\n" +
                    "    </div>\n" +
                    "</div>",
                    escapeAttr(user.getAvatar() != null ? user.getAvatar() : ""),
                    escapeAttr(user.getFullName()),
                    escapeAttr(user.getRole()),
                    escapeAttr(user.getFullName()),
                    escapeAttr(user.getEmail() != null ? user.getEmail() : ""),
                    escapeAttr(user.getFullName())
            );

            content = content.replace("<!-- ${TOPBAR_AUTH} -->", topbarAuth);
            content = content.replace("<!-- ${HEADER_AUTH} -->", headerAuth);
            content = content.replace("<!-- ${HERO_GREETING} -->", "thành viên <strong>" + escapeAttr(user.getFullName()) + "</strong>");
            content = content.replace("${user.fullName}", escapeAttr(user.getFullName()));
            content = content.replace("${user.username}", escapeAttr(user.getUsername()));
            content = content.replace("${user.role}", escapeAttr(user.getRole()));
            content = content.replace("${user.avatar}", escapeAttr(user.getAvatar() != null ? user.getAvatar() : ""));
            content = content.replace("${user.email}", escapeAttr(user.getEmail() != null ? user.getEmail() : ""));
        } else {
            String topbarAuth =
                    "<div class=\"topbar-auth-links\">" +
                    "    <span style=\"color: #cbd5e1;\"><i class=\"fas fa-truck-fast\"></i> Miễn phí vận chuyển từ 250.000 đ</span>" +
                    "</div>";

            String headerAuth =
                    "<div class=\"guest-auth-buttons\">\n" +
                    "    <a href=\"login\" class=\"btn-guest btn-guest-login\">\n" +
                    "        <i class=\"fas fa-arrow-right-to-bracket\"></i>\n" +
                    "        <span>Đăng Nhập</span>\n" +
                    "    </a>\n" +
                    "    <a href=\"register\" class=\"btn-guest btn-guest-register\">\n" +
                    "        <i class=\"fas fa-user-plus\"></i>\n" +
                    "        <span>Đăng Ký</span>\n" +
                    "    </a>\n" +
                    "</div>";

            content = content.replace("<!-- ${TOPBAR_AUTH} -->", topbarAuth);
            content = content.replace("<!-- ${HEADER_AUTH} -->", headerAuth);
            content = content.replace("<!-- ${HERO_GREETING} -->", "quý độc giả và thành viên mới");
            content = content.replace("${user.fullName}", "Khách");
            content = content.replace("${user.username}", "guest");
            content = content.replace("${user.role}", "GUEST");
            content = content.replace("${user.avatar}", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
            content = content.replace("${user.email}", "");
        }

        content = content.replace("${searchKeyword}", (keyword != null) ? keyword : "");

        // Render Categories
        StringBuilder catHtml = new StringBuilder();
        for (String cat : categories) {
            boolean active = cat.equalsIgnoreCase(selectedCat);
            catHtml.append(String.format(
                    "<button class=\"category-pill %s\" data-category=\"%s\">%s</button>",
                    active ? "active" : "",
                    cat,
                    cat
            ));
        }
        content = content.replace("<!-- ${CATEGORY_PILLS} -->", catHtml.toString());

        // Render Book Cards
        StringBuilder booksHtml = new StringBuilder();
        if (books.isEmpty()) {
            booksHtml.append("<div class=\"no-books-found\"><i class=\"fas fa-book-open\"></i><p>Không tìm thấy cuốn sách nào phù hợp.</p></div>");
        } else {
            for (Book b : books) {
                String badge = b.isBestSeller() ? "<span class=\"badge-tag badge-bestseller\">Bán chạy</span>" : "";
                String discountBadge = (b.getDiscountPercent() > 0) 
                        ? "<span class=\"badge-tag badge-discount\">-" + b.getDiscountPercent() + "%</span>" 
                        : "";
                String originalPriceHtml = (b.getOriginalPrice() > 0) 
                        ? "<span class=\"price-original\">" + b.getFormattedOriginalPrice() + "</span>" 
                        : "";

                booksHtml.append(String.format(
                        "<div class=\"book-card\" data-id=\"%d\" data-code=\"%s\" data-title=\"%s\" data-author=\"%s\" data-price=\"%.0f\" data-formatted-price=\"%s\" data-category=\"%s\" data-image=\"%s\" data-desc=\"%s\" data-rating=\"%.1f\">\n" +
                        "    <div class=\"book-card-inner\">\n" +
                        "        <div class=\"book-cover-wrap\">\n" +
                        "            <img src=\"%s\" alt=\"%s\" class=\"book-cover\" loading=\"lazy\" onerror=\"this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';\">\n" +
                        "            <div class=\"badge-container\">%s%s</div>\n" +
                        "            <div class=\"book-actions-overlay\">\n" +
                        "                <button type=\"button\" class=\"btn-quickview\" onclick=\"openQuickView(%d)\"><i class=\"fas fa-eye\"></i> Xem nhanh</button>\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "        <div class=\"book-info\">\n" +
                        "            <div class=\"book-meta-top\">\n" +
                        "                <span class=\"book-category\">%s</span>\n" +
                        "                <span class=\"book-code\" title=\"Mã sách: %s\"><i class=\"fas fa-barcode\"></i> %s</span>\n" +
                        "            </div>\n" +
                        "            <h3 class=\"book-title\" title=\"%s\">%s</h3>\n" +
                        "            <p class=\"book-author\"><i class=\"fas fa-feather-alt\"></i> %s</p>\n" +
                        "            <div class=\"book-rating\">\n" +
                        "                <div class=\"stars\"><i class=\"fas fa-star\"></i> <span>%.1f</span></div>\n" +
                        "                <span class=\"review-count\">(%d đánh giá)</span>\n" +
                        "            </div>\n" +
                        "            <div class=\"book-price-row\">\n" +
                        "                <div class=\"price-box\">\n" +
                        "                    <span class=\"price-current\">%s</span>\n" +
                        "                    %s\n" +
                        "                </div>\n" +
                        "                <button type=\"button\" class=\"btn-add-cart\" onclick=\"addToCart(%d)\" title=\"Thêm vào giỏ\">\n" +
                        "                    <i class=\"fas fa-cart-plus\"></i>\n" +
                        "                </button>\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "</div>\n",
                        b.getId(),
                        escapeAttr(b.getCode()),
                        escapeAttr(b.getTitle()),
                        escapeAttr(b.getAuthor()),
                        b.getPrice(),
                        escapeAttr(b.getFormattedPrice()),
                        escapeAttr(b.getCategory()),
                        escapeAttr(b.getImage()),
                        escapeAttr(b.getDescription()),
                        b.getRating(),
                        b.getImage(),
                        escapeAttr(b.getTitle()),
                        badge,
                        discountBadge,
                        b.getId(),
                        escapeHtml(b.getCategory()),
                        escapeAttr(b.getCode()),
                        escapeHtml(b.getCode()),
                        escapeAttr(b.getTitle()),
                        escapeHtml(b.getTitle()),
                        escapeHtml(b.getAuthor()),
                        b.getRating(),
                        b.getReviewCount(),
                        b.getFormattedPrice(),
                        originalPriceHtml,
                        b.getId()
                ));
            }
        }
        content = content.replace("<!-- ${BOOK_GRID} -->", booksHtml.toString());
        content = content.replace("${bookCount}", String.valueOf(books.size()));

        return content;
    }

    // ====================== UTILITY FUNCTIONS ======================

    private static User getAuthenticatedUser(HttpExchange exchange) {
        String token = getSessionToken(exchange);
        if (token != null && activeSessions.containsKey(token)) {
            String username = activeSessions.get(token);
            return DataStore.findUser(username);
        }
        return null;
    }

    private static String getSessionToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String cookieHeader : cookies) {
                String[] pairs = cookieHeader.split(";");
                for (String pair : pairs) {
                    String[] kv = pair.trim().split("=", 2);
                    if (kv.length == 2 && "BOOKSTORE_SESSION".equals(kv[0].trim())) {
                        return kv[1].trim();
                    }
                }
            }
        }
        return null;
    }

    private static Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return parseQueryString(body);
    }

    private static Map<String, String> parseQueryString(String qs) {
        Map<String, String> map = new HashMap<>();
        if (qs == null || qs.isEmpty()) return map;
        String[] pairs = qs.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, val);
        }
        return map;
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    private static void sendResponse(HttpExchange exchange, int status, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        return "text/html; charset=UTF-8";
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    private static String escapeAttr(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    private static String escapeJson(String input) {
        if (input == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    /**
     * Trang xem trực quan dữ liệu MySQL trực tiếp trên trình duyệt
     */
    static class DatabaseViewerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\">");
            html.append("<title>Cơ Sở Dữ Liệu MySQL - Bookora</title>");
            html.append("<style>");
            html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f8fafc; color: #0f172a; padding: 30px; }");
            html.append(".container { max-width: 1000px; margin: 0 auto; }");
            html.append(".header { background: #1e1b4b; color: white; padding: 24px; border-radius: 12px; margin-bottom: 24px; }");
            html.append(".badge { display: inline-block; padding: 4px 10px; border-radius: 99px; font-weight: bold; font-size: 12px; background: #10b981; color: white; margin-left: 10px; }");
            html.append(".card { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); margin-bottom: 24px; border: 1px solid #e2e8f0; }");
            html.append("h2 { color: #1e1b4b; font-size: 20px; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; }");
            html.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 14px; }");
            html.append("th, td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #e2e8f0; }");
            html.append("th { background: #f1f5f9; color: #475569; font-weight: 700; }");
            html.append("tr:hover { background: #f8fafc; }");
            html.append(".role-admin { background: #fee2e2; color: #b91c1c; padding: 2px 8px; border-radius: 6px; font-weight: 700; font-size: 11px; }");
            html.append(".role-cust { background: #e0e7ff; color: #4338ca; padding: 2px 8px; border-radius: 6px; font-weight: 700; font-size: 11px; }");
            html.append(".btn { display: inline-block; background: #d97706; color: white; text-decoration: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; margin-top: 10px; }");
            html.append("</style></head><body><div class=\"container\">");

            html.append("<div class=\"header\">");
            html.append("<h1>🗄️ Dữ Liệu Cơ Sở Dữ Liệu MySQL: web_bookora <span class=\"badge\">ĐÃ KẾT NỐI</span></h1>");
            html.append("<p style=\"color: #cbd5e1; margin-top: 6px;\">Host: 127.0.0.1:3306 | User: root | Database: web_bookora</p>");
            html.append("<div style=\"margin-top: 14px;\"><a href=\"/login\" class=\"btn\">⬅️ Quay lại trang Đăng nhập</a></div>");
            html.append("</div>");

            // Bảng Users
            html.append("<div class=\"card\">");
            html.append("<h2>👥 Bảng `users` (Danh sách tài khoản)</h2>");
            html.append("<table><thead><tr><th>ID</th><th>Tên đăng nhập (username)</th><th>Mật khẩu (password)</th><th>Họ và tên</th><th>Email</th><th>Vai trò (role)</th></tr></thead><tbody>");
            
            try (java.sql.Connection conn = com.bookstore.data.DBContext.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id, username, password, full_name, email, role FROM users ORDER BY id ASC");
                 java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String roleClass = "ADMIN".equalsIgnoreCase(rs.getString("role")) ? "role-admin" : "role-cust";
                    html.append(String.format("<tr><td>%d</td><td><strong>%s</strong></td><td><code>%s</code></td><td>%s</td><td>%s</td><td><span class=\"%s\">%s</span></td></tr>",
                            rs.getInt("id"),
                            escapeHtml(rs.getString("username")),
                            escapeHtml(rs.getString("password")),
                            escapeHtml(rs.getString("full_name")),
                            escapeHtml(rs.getString("email")),
                            roleClass,
                            escapeHtml(rs.getString("role"))
                    ));
                }
            } catch (java.sql.SQLException e) {
                html.append("<tr><td colspan=\"6\" style=\"color: red;\">Lỗi truy vấn users: ").append(e.getMessage()).append("</td></tr>");
            }
            html.append("</tbody></table></div>");

            // Bảng Books
            html.append("<div class=\"card\">");
            html.append("<h2>📚 Bảng `books` (Danh mục sách)</h2>");
            html.append("<table><thead><tr><th>ID</th><th>Tên sách</th><th>Tác giả</th><th>Thể loại</th><th>Giá bán</th><th>Đánh giá</th></tr></thead><tbody>");
            List<Book> books = DataStore.getAllBooks();
            for (Book b : books) {
                html.append(String.format("<tr><td>%d</td><td><strong>%s</strong></td><td>%s</td><td>%s</td><td style=\"color: #d97706; font-weight: bold;\">%s</td><td>★ %.1f</td></tr>",
                        b.getId(),
                        escapeHtml(b.getTitle()),
                        escapeHtml(b.getAuthor()),
                        escapeHtml(b.getCategory()),
                        escapeHtml(b.getFormattedPrice()),
                        b.getRating()
                ));
            }
            html.append("</tbody></table></div>");

            html.append("</div></body></html>");
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html.toString());
        }
    }
}
