package com.bookstore.server;

import com.bookstore.data.DataStore;
import com.bookstore.model.Address;
import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.bookstore.model.ChatbotContent;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.PaymentMethod;
import com.bookstore.model.User;
import com.bookstore.model.Voucher;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Máy chủ Java độc lập (Standalone Java Web Server).
 * Cho phép chạy trực tiếp ứng dụng web bán sách mà không cần cấu hình Tomcat hay cài đặt thêm phần mềm.
 */
public class BookstoreApp {
    private static final int DEFAULT_PORT = 8080;
    private static final Path WEBAPP_DIR = Paths.get("src", "main", "webapp");
    // Bộ nhớ quản lý session: Token -> Username
    private static final Map<String, String> activeSessions = new HashMap<>();
    // Chống đặt trùng đơn khi nhấn nhiều lần (Idempotency / Anti-spam)
    private static final Set<String> processedRequestIds = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Long> userLastOrderTime = new ConcurrentHashMap<>();

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
        server.createContext("/category", new CategoryHandler());
        server.createContext("/category.html", new CategoryHandler());
        server.createContext("/book", new BookDetailHandler());
        server.createContext("/cart", new CartHandler());
        server.createContext("/checkout", new CheckoutHandler());
        server.createContext("/order-success", new OrderSuccessHandler());
        server.createContext("/about", new AboutHandler());
        server.createContext("/contact", new ContactHandler());
        server.createContext("/promotions", new PromotionsHandler());
        server.createContext("/profile", new ProfileHandler());
        server.createContext("/logout", new LogoutHandler());
        server.createContext("/api/books", new ApiBooksHandler());
        server.createContext("/api/suggestions", new ApiSuggestionsHandler());
        server.createContext("/api/chatbot", new ApiChatbotHandler());
        server.createContext("/api/me", new ApiMeHandler());
        server.createContext("/api/send-otp", new SendOtpHandler());
        server.createContext("/api/verify-otp", new VerifyOtpHandler());
        server.createContext("/api/checkout/info", new ApiCheckoutInfoHandler());
        server.createContext("/api/checkout/address", new ApiCheckoutAddressHandler());
        server.createContext("/api/checkout/validate-voucher", new ApiValidateVoucherHandler());
        server.createContext("/api/checkout/place-order", new ApiPlaceOrderHandler());
        server.createContext("/api/payment/confirm-online", new ApiConfirmOnlinePaymentHandler());
        server.createContext("/api/orders/detail", new ApiGetOrderDetailHandler());
        server.createContext("/api/orders/my-orders", new ApiMyOrdersHandler());
        server.createContext("/api/upload-avatar", new ApiUploadAvatarHandler());
        server.createContext("/api/admin/books", new ApiAdminBooksHandler());
        server.createContext("/api/admin/books/detail", new ApiAdminBookDetailHandler());
        server.createContext("/api/admin/books/add", new ApiAdminBookAddHandler());
        server.createContext("/api/admin/books/edit", new ApiAdminBookEditHandler());
        server.createContext("/api/admin/books/update-stock", new ApiAdminStockUpdateHandler());
        server.createContext("/api/admin/books/delete", new ApiAdminBookDeleteHandler());
        server.createContext("/api/admin/books/upload-cover", new ApiAdminBookUploadCoverHandler());
        server.createContext("/api/admin/customers", new ApiAdminCustomersHandler());
        server.createContext("/api/admin/customers/detail", new ApiAdminCustomerDetailHandler());
        server.createContext("/api/admin/customers/status", new ApiAdminCustomerStatusHandler());
        server.createContext("/api/admin/customers/order-detail", new ApiAdminCustomerOrderDetailHandler());
        server.createContext("/api/admin/categories", new ApiAdminCategoriesHandler());
        server.createContext("/api/admin/categories/detail", new ApiAdminCategoryDetailHandler());
        server.createContext("/api/admin/categories/add", new ApiAdminCategoryAddHandler());
        server.createContext("/api/admin/categories/edit", new ApiAdminCategoryEditHandler());
        server.createContext("/api/admin/categories/delete", new ApiAdminCategoryDeleteHandler());
        server.createContext("/api/admin/promotions", new ApiAdminPromotionsHandler());
        server.createContext("/api/admin/promotions/detail", new ApiAdminPromotionDetailHandler());
        server.createContext("/api/admin/promotions/add", new ApiAdminPromotionAddHandler());
        server.createContext("/api/admin/promotions/edit", new ApiAdminPromotionEditHandler());
        server.createContext("/api/admin/promotions/delete", new ApiAdminPromotionDeleteHandler());
        server.createContext("/api/admin/promotions/status", new ApiAdminPromotionStatusHandler());
        server.createContext("/api/admin/reports", new ApiAdminReportsHandler());
        server.createContext("/api/admin/reports/export", new ApiAdminReportsExportHandler());
        server.createContext("/api/admin/chatbot/content", new ApiAdminChatbotContentHandler());
        server.createContext("/api/admin/chatbot/content/detail", new ApiAdminChatbotDetailHandler());
        server.createContext("/api/admin/chatbot/content/add", new ApiAdminChatbotAddHandler());
        server.createContext("/api/admin/chatbot/content/edit", new ApiAdminChatbotEditHandler());
        server.createContext("/api/admin/chatbot/content/delete", new ApiAdminChatbotDeleteHandler());
        server.createContext("/api/admin/chatbot/content/sync", new ApiAdminChatbotSyncHandler());
        server.createContext("/db", new DatabaseViewerHandler());
        server.createContext("/admin", new AdminHandler());
        server.createContext("/admin/books", new AdminHandler());
        server.createContext("/admin/customers", new AdminHandler());
        server.createContext("/admin/categories", new AdminHandler());
        server.createContext("/admin/promotions", new AdminHandler());
        server.createContext("/admin/reports", new AdminHandler());
        server.createContext("/admin/chatbot", new AdminHandler());
        server.createContext("/admin/css/", new StaticFileHandler());
        server.createContext("/admin/js/", new StaticFileHandler());
        server.createContext("/css/", new StaticFileHandler());
        server.createContext("/js/", new StaticFileHandler());
        server.createContext("/images/", new StaticFileHandler());
        server.createContext("/uploads/", new StaticFileHandler());


        // Tự động kiểm tra và khởi động MySQL nếu chưa chạy
        boolean dbConnected = com.bookstore.data.DBContext.testConnection();
        if (!dbConnected) {
            System.out.println("⚠️ Chưa kết nối MySQL. Đang thử tự động khởi động MySQL từ XAMPP...");
            try {
                java.io.File xamppMysql = new java.io.File("C:\\xampp\\mysql\\bin\\mysqld.exe");
                if (xamppMysql.exists()) {
                    new ProcessBuilder("C:\\xampp\\mysql\\bin\\mysqld.exe", "--defaults-file=C:\\xampp\\mysql\\bin\\my.ini", "--standalone").start();
                    Thread.sleep(2000);
                    dbConnected = com.bookstore.data.DBContext.testConnection();
                }
            } catch (Exception ignored) {}
        }

        server.setExecutor(null); // Sử dụng executor mặc định
        server.start();

        System.out.println("===============================================================");
        System.out.println("🚀 MÁY CHỦ JAVA BOOKSTORE ĐÃ KHỞI CHẠY THÀNH CÔNG!");
        System.out.println("🗄️  Trạng thái MySQL Database: " + (dbConnected ? "✅ ĐÃ KẾT NỐI (web_bookora)" : "⚠️ CHƯA KẾT NỐI (Đang dùng bộ nhớ dự phòng)"));
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
            } else if (path.equals("/category") || path.equals("/category.html")) {
                new CategoryHandler().handle(exchange);
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
                    if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                        redirect(exchange, "/admin");
                    } else {
                        redirect(exchange, "/home");
                    }
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
                String redirectUrl = params.getOrDefault("redirect", "");
                if (redirectUrl.isEmpty()) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null) {
                        Map<String, String> qp = parseQueryString(query);
                        redirectUrl = qp.getOrDefault("redirect", "");
                    }
                }

                if (username.isEmpty() || password.isEmpty()) {
                    String html = renderLoginPage("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!", "", username);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                    return;
                }

                if (DataStore.validateUser(username, password)) {
                    User loggedInUser = DataStore.findUser(username);
                    if (loggedInUser != null && "LOCKED".equalsIgnoreCase(loggedInUser.getStatus())) {
                        String html = renderLoginPage("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.", "", username);
                        sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
                        return;
                    }

                    // Đăng nhập thành công -> Tạo session token
                    String token = UUID.randomUUID().toString();
                    activeSessions.put(token, username.toLowerCase());

                    String cookieHeader = "BOOKSTORE_SESSION=" + token + "; Path=/; HttpOnly";
                    if ("on".equals(remember) || "true".equals(remember)) {
                        cookieHeader += "; Max-Age=" + (7 * 24 * 3600); // 7 ngày
                    }
                    exchange.getResponseHeaders().add("Set-Cookie", cookieHeader);

                    if (loggedInUser != null && "ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
                        if (redirectUrl != null && !redirectUrl.isEmpty() && redirectUrl.startsWith("/admin")) {
                            redirect(exchange, redirectUrl);
                        } else {
                            redirect(exchange, "/admin");
                        }
                    } else {
                        if (redirectUrl != null && !redirectUrl.isEmpty() && redirectUrl.startsWith("/") && !redirectUrl.startsWith("/admin")) {
                            redirect(exchange, redirectUrl);
                        } else {
                            redirect(exchange, "/home");
                        }
                    }
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
     * Xử lý giao diện và phân quyền cho khu vực Quản trị viên (/admin/*)
     */
    static class AdminHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            // 1. Kiểm tra xác thực (Authentication)
            if (user == null) {
                String reqUri = exchange.getRequestURI().toString();
                redirect(exchange, "/login?require_login=true&redirect=" + reqUri);
                return;
            }

            // 2. Kiểm tra phân quyền (Authorization - RBAC)
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                // Khách hàng (CUSTOMER) cố tình truy cập trực tiếp URL Admin -> Chặn và chuyển hướng về trang chủ khách hàng
                redirect(exchange, "/home");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String fileName = "index.html";
            if (path.equals("/admin/books")) {
                fileName = "books.html";
            } else if (path.equals("/admin/customers")) {
                fileName = "customers.html";
            } else if (path.equals("/admin/categories")) {
                fileName = "categories.html";
            } else if (path.equals("/admin/promotions")) {
                fileName = "promotions.html";
            } else if (path.equals("/admin/reports")) {
                fileName = "reports.html";
            } else if (path.equals("/admin/chatbot")) {
                fileName = "chatbot.html";
            } else if (path.startsWith("/admin/css/") || path.startsWith("/admin/js/")) {
                new StaticFileHandler().handle(exchange);
                return;
            }

            Path filePath = WEBAPP_DIR.resolve("admin").resolve(fileName);
            if (!Files.exists(filePath)) {
                filePath = WEBAPP_DIR.resolve("admin").resolve("index.html");
            }

            String content = Files.readString(filePath, StandardCharsets.UTF_8);

            // Thay thế thông tin Admin cá nhân
            String adminFullName = (user.getFullName() != null && !user.getFullName().isEmpty()) ? user.getFullName() : user.getUsername();
            String adminAvatar = (user.getAvatar() != null && !user.getAvatar().isEmpty()) ? user.getAvatar() : "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150";

            content = content.replace("${adminFullName}", escapeHtml(adminFullName));
            content = content.replace("${adminUsername}", escapeHtml(user.getUsername()));
            content = content.replace("${adminAvatar}", escapeAttr(adminAvatar));

            // Thay thế dữ liệu thống kê tổng quan (Dashboard stats)
            content = content.replace("${totalBooks}", String.valueOf(DataStore.getAllBooks().size()));
            content = content.replace("${totalCustomers}", String.valueOf(DataStore.getAllUsers().size()));
            content = content.replace("${totalOrders}", String.valueOf(DataStore.getAllOrders().size()));
            content = content.replace("${totalRevenue}", "587,000 đ");
            
            // Tính số lượng sách sắp hết hàng (stock <= 5)
            long lowStock = DataStore.getAllBooks().stream().filter(b -> b.getStock() <= 5).count();
            content = content.replace("${lowStockCount}", String.valueOf(lowStock));

            sendResponse(exchange, 200, "text/html; charset=UTF-8", content);
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
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/home/css/") || path.startsWith("/home/js/") || path.startsWith("/home/images/")) {
                new StaticFileHandler().handle(exchange);
                return;
            }

            User user = getAuthenticatedUser(exchange);

            // Đọc các tham số tìm kiếm & lọc đa tiêu chí
            String query = exchange.getRequestURI().getQuery();
            String keyword = "";
            String category = "Tất cả";
            String author = "Tất cả";
            String publisher = "Tất cả";
            String stockStatus = "all";
            Double minPrice = null;
            Double maxPrice = null;

            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("q")) keyword = qp.get("q");
                if (qp.containsKey("category")) category = qp.get("category");
                if (qp.containsKey("author")) author = qp.get("author");
                if (qp.containsKey("publisher")) publisher = qp.get("publisher");
                if (qp.containsKey("stockStatus")) stockStatus = qp.get("stockStatus");
                if (qp.containsKey("minPrice")) {
                    try { minPrice = Double.parseDouble(qp.get("minPrice")); } catch (NumberFormatException ignored) {}
                }
                if (qp.containsKey("maxPrice")) {
                    try { maxPrice = Double.parseDouble(qp.get("maxPrice")); } catch (NumberFormatException ignored) {}
                }
            }

            List<Book> books = DataStore.searchBooks(keyword, category, author, publisher, minPrice, maxPrice, stockStatus);
            List<String> categories = DataStore.getCategories();
            List<String> authors = DataStore.getAuthors();
            List<String> publishers = DataStore.getPublishers();
            List<DataStore.PromotionItem> promotions = DataStore.getPromotions();

            String html = renderHomePage(user, books, categories, authors, publishers, promotions,
                                         category, author, publisher, minPrice, maxPrice, stockStatus, keyword);
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
     * Xử lý GET/POST cho trang Hồ sơ tài khoản (/profile)
     */
    static class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                redirect(exchange, "/login?require_login=true");
                return;
            }

            User freshUser = DataStore.findUser(user.getUsername());
            if (freshUser != null) {
                user = freshUser;
            }

            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                String html = renderProfilePage(user, "profile", "", "");
                sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
            } else if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> params = parseFormData(exchange);
                String action = params.getOrDefault("action", "").trim();
                String activeTab = "profile";
                String successMsg = "";
                String errorMsg = "";

                if ("updateProfile".equalsIgnoreCase(action)) {
                    String fullName = params.getOrDefault("fullName", "").trim();
                    String email = params.getOrDefault("email", "").trim();
                    String phone = params.getOrDefault("phone", "").trim();
                    String avatar = params.getOrDefault("avatar", "").trim();

                    if (fullName.isEmpty()) {
                        errorMsg = "Họ và tên không được để trống!";
                    } else {
                        boolean ok = DataStore.updateUserProfile(user.getUsername(), fullName, email, phone, avatar);
                        if (ok) {
                            successMsg = "Cập nhật hồ sơ tài khoản thành công!";
                            User updated = DataStore.findUser(user.getUsername());
                            if (updated != null) user = updated;
                        } else {
                            errorMsg = "Có lỗi xảy ra khi lưu thông tin vào cơ sở dữ liệu!";
                        }
                    }
                    activeTab = "profile";

                } else if ("changePassword".equalsIgnoreCase(action)) {
                    activeTab = "password";
                    String oldPassword = params.getOrDefault("oldPassword", "").trim();
                    String newPassword = params.getOrDefault("newPassword", "").trim();
                    String confirmPassword = params.getOrDefault("confirmPassword", "").trim();

                    if (oldPassword.isEmpty()) {
                        errorMsg = "Vui lòng nhập mật khẩu hiện tại!";
                    } else if (newPassword.isEmpty()) {
                        errorMsg = "Vui lòng nhập mật khẩu mới!";
                    } else if (newPassword.length() < 8) {
                        errorMsg = "Mật khẩu mới phải có tối thiểu 8 ký tự!";
                    } else if (!isStrongPassword(newPassword)) {
                        errorMsg = "Mật khẩu mới phải gồm chữ hoa, chữ thường, số và ký tự đặc biệt!";
                    } else if (!newPassword.equals(confirmPassword)) {
                        errorMsg = "Mật khẩu xác nhận không trùng khớp!";
                    } else {
                        String result = DataStore.changePassword(user.getUsername(), oldPassword, newPassword);
                        if (result == null) {
                            successMsg = "Đổi mật khẩu thành công! Hãy ghi nhớ mật khẩu mới của bạn.";
                        } else {
                            errorMsg = result;
                        }
                    }
                }

                // Hỗ trợ phản hồi AJAX JSON nếu gọi qua fetch/XMLHttpRequest
                String accept = exchange.getRequestHeaders().getFirst("Accept");
                String requestedWith = exchange.getRequestHeaders().getFirst("X-Requested-With");
                if ((accept != null && accept.contains("application/json")) || "XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                    boolean isSuccess = errorMsg.isEmpty();
                    String json = String.format("{\"success\":%b,\"message\":%s,\"activeTab\":%s}",
                            isSuccess,
                            escapeJson(isSuccess ? successMsg : errorMsg),
                            escapeJson(activeTab));
                    sendResponse(exchange, isSuccess ? 200 : 400, "application/json; charset=UTF-8", json);
                    return;
                }

                String html = renderProfilePage(user, activeTab, successMsg, errorMsg);
                sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    /**
     * Xử lý điều hướng và hiển thị trang Giỏ hàng (/cart)
     */
    /**
     * Xử lý điều hướng và hiển thị trang Giỏ hàng (/cart)
     */
    static class CartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            String html = renderCartPage(user);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * Xử lý hiển thị trang Đặt hàng & Thanh toán (/checkout)
     * Nghiệp vụ: Kiểm tra đã đăng nhập chưa, nếu chưa yêu cầu đăng nhập.
     */
    static class CheckoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                redirect(exchange, "/login?redirect=/checkout&require_login=true");
                return;
            }
            String html = renderContentPage("checkout.html", user);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * Xử lý hiển thị trang Xác nhận đơn hàng thành công (/order-success)
     */
    static class OrderSuccessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            String html = renderContentPage("order-success.html", user);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * API lấy thông tin người dùng, danh sách địa chỉ, voucher và phương thức thanh toán
     */
    static class ApiCheckoutInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                sendResponse(exchange, 401, "application/json; charset=UTF-8", "{\"success\":false,\"needLogin\":true,\"message\":\"Vui lòng đăng nhập để tiếp tục đặt hàng!\"}");
                return;
            }

            List<Address> addresses = DataStore.getAddressesByUsername(user.getUsername());
            List<Voucher> vouchers = DataStore.getActiveVouchers();
            List<PaymentMethod> pms = DataStore.getPaymentMethods();

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"success\":true,");
            sb.append("\"user\":{")
              .append("\"username\":").append(escapeJson(user.getUsername())).append(",")
              .append("\"fullName\":").append(escapeJson(user.getFullName())).append(",")
              .append("\"phone\":").append(escapeJson(user.getPhone() != null ? user.getPhone() : "")).append(",")
              .append("\"email\":").append(escapeJson(user.getEmail() != null ? user.getEmail() : "")).append("},");

            // Danh sách địa chỉ (DIA_CHI)
            sb.append("\"addresses\":[");
            for (int i = 0; i < addresses.size(); i++) {
                Address a = addresses.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format("{\"id\":%d,\"recipientName\":%s,\"phone\":%s,\"addressDetail\":%s,\"province\":%s,\"district\":%s,\"ward\":%s,\"fullAddress\":%s,\"isDefault\":%b}",
                        a.getId(),
                        escapeJson(a.getRecipientName()),
                        escapeJson(a.getPhone()),
                        escapeJson(a.getAddressDetail()),
                        escapeJson(a.getProvince()),
                        escapeJson(a.getDistrict()),
                        escapeJson(a.getWard()),
                        escapeJson(a.getFullAddress()),
                        a.isDefault()
                ));
            }
            sb.append("],");

            // Danh sách mã giảm giá (MA_GIAM_GIA)
            sb.append("\"vouchers\":[");
            for (int i = 0; i < vouchers.size(); i++) {
                Voucher v = vouchers.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format("{\"id\":%d,\"code\":%s,\"title\":%s,\"description\":%s,\"discountType\":%s,\"discountValue\":%.0f,\"minOrderAmount\":%.0f,\"maxDiscountAmount\":%.0f}",
                        v.getId(),
                        escapeJson(v.getCode()),
                        escapeJson(v.getTitle()),
                        escapeJson(v.getDescription()),
                        escapeJson(v.getDiscountType()),
                        v.getDiscountValue(),
                        v.getMinOrderAmount(),
                        v.getMaxDiscountAmount()
                ));
            }
            sb.append("],");

            // Danh sách phương thức thanh toán (PHUONG_THUC_TT)
            sb.append("\"paymentMethods\":[");
            for (int i = 0; i < pms.size(); i++) {
                PaymentMethod pm = pms.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format("{\"id\":%d,\"code\":%s,\"name\":%s,\"description\":%s}",
                        pm.getId(),
                        escapeJson(pm.getCode()),
                        escapeJson(pm.getName()),
                        escapeJson(pm.getDescription())
                ));
            }
            sb.append("]}");

            sendResponse(exchange, 200, "application/json; charset=UTF-8", sb.toString());
        }
    }

    /**
     * API thêm địa chỉ nhận hàng mới cho tài khoản đang đăng nhập (Bảng DIA_CHI)
     */
    static class ApiCheckoutAddressHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                sendResponse(exchange, 401, "application/json; charset=UTF-8", "{\"success\":false,\"needLogin\":true,\"message\":\"Vui lòng đăng nhập!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String recipientName = extractJsonString(body, "recipientName");
            String phone = extractJsonString(body, "phone");
            String addressDetail = extractJsonString(body, "addressDetail");
            String province = extractJsonString(body, "province");
            String district = extractJsonString(body, "district");
            String ward = extractJsonString(body, "ward");
            boolean isDefault = extractJsonBoolean(body, "isDefault", false);

            if (recipientName == null || recipientName.trim().isEmpty() ||
                phone == null || phone.trim().isEmpty() ||
                addressDetail == null || addressDetail.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng nhập đầy đủ tên người nhận, số điện thoại và địa chỉ cụ thể!\"}");
                return;
            }

            Address addr = DataStore.addAddress(user.getUsername(), recipientName, phone, addressDetail, province, district, ward, isDefault);
            if (addr == null) {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không thể lưu địa chỉ nhận hàng!\"}");
                return;
            }

            String json = String.format("{\"success\":true,\"message\":\"Đã thêm địa chỉ nhận hàng thành công!\",\"address\":{\"id\":%d,\"recipientName\":%s,\"phone\":%s,\"addressDetail\":%s,\"province\":%s,\"district\":%s,\"ward\":%s,\"fullAddress\":%s,\"isDefault\":%b}}",
                    addr.getId(),
                    escapeJson(addr.getRecipientName()),
                    escapeJson(addr.getPhone()),
                    escapeJson(addr.getAddressDetail()),
                    escapeJson(addr.getProvince()),
                    escapeJson(addr.getDistrict()),
                    escapeJson(addr.getWard()),
                    escapeJson(addr.getFullAddress()),
                    addr.isDefault()
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    /**
     * API kiểm tra mã giảm giá (Bước 4 - Backend tính lại toàn bộ giá)
     */
    static class ApiValidateVoucherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String code = extractJsonString(body, "code");
            if (code == null || code.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng nhập mã giảm giá!\"}");
                return;
            }

            List<OrderItemReq> items = extractOrderItems(body);
            if (items.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Đơn hàng chưa có sản phẩm nào để tính giảm giá!\"}");
                return;
            }

            // Tự tính lại tổng tiền từ DB/DataStore - Không tin giá frontend gửi lên
            double subtotal = 0;
            for (OrderItemReq req : items) {
                Book b = DataStore.getBookById(req.bookId);
                if (b != null) {
                    subtotal += b.getPrice() * req.quantity;
                }
            }

            Voucher v = DataStore.findVoucher(code);
            if (v == null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã giảm giá '" + escapeJson(code.trim().toUpperCase()) + "' không tồn tại trên hệ thống!\"}");
                return;
            }

            String validationError = v.validate(subtotal);
            if (validationError != null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":" + escapeJson(validationError) + "}");
                return;
            }

            double discountAmount = v.calculateDiscount(subtotal);
            double shippingFee = subtotal >= 250000 ? 0 : 30000;
            double finalTotal = Math.max(0, subtotal - discountAmount + shippingFee);

            String json = String.format("{\"success\":true,\"message\":\"Áp dụng mã giảm giá thành công!\",\"code\":%s,\"title\":%s,\"discountAmount\":%.0f,\"subtotal\":%.0f,\"shippingFee\":%.0f,\"finalTotal\":%.0f}",
                    escapeJson(v.getCode()),
                    escapeJson(v.getTitle()),
                    discountAmount,
                    subtotal,
                    shippingFee,
                    finalTotal
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    /**
     * API Đặt hàng chính (Bước 5 & 6 - Kiểm tra tồn kho KHO, tự tính giá, tạo đơn, trừ kho)
     */
    static class ApiPlaceOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            // Kiểm tra đăng nhập
            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                sendResponse(exchange, 401, "application/json; charset=UTF-8", "{\"success\":false,\"needLogin\":true,\"message\":\"Vui lòng đăng nhập để tiếp tục đặt hàng!\"}");
                return;
            }

            String body = readRequestBody(exchange);

            // Kiểm tra chống nhấn Đặt hàng nhiều lần (Anti-duplicate / Debouncing)
            String clientRequestId = extractJsonString(body, "clientRequestId");
            if (clientRequestId != null && !clientRequestId.trim().isEmpty()) {
                if (processedRequestIds.contains(clientRequestId)) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Yêu cầu đặt hàng đang được xử lý, vui lòng không nhấn gửi lại nhiều lần!\"}");
                    return;
                }
                processedRequestIds.add(clientRequestId);
            }

            Long lastOrderTime = userLastOrderTime.get(user.getUsername().toLowerCase());
            if (lastOrderTime != null && System.currentTimeMillis() - lastOrderTime < 1500) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Thao tác đặt hàng quá nhanh, vui lòng chờ trong giây lát!\"}");
                return;
            }
            userLastOrderTime.put(user.getUsername().toLowerCase(), System.currentTimeMillis());

            int addressId = extractJsonInt(body, "addressId", 0);
            String paymentMethod = extractJsonString(body, "paymentMethod");
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) paymentMethod = "COD";
            paymentMethod = paymentMethod.trim().toUpperCase();

            String voucherCode = extractJsonString(body, "voucherCode");
            String note = extractJsonString(body, "note");
            if (note == null) note = "";

            List<OrderItemReq> reqItems = extractOrderItems(body);
            if (reqItems.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Giỏ hàng đang trống! Vui lòng chọn ít nhất một cuốn sách để đặt hàng.\"}");
                return;
            }

            // Kiểm tra quyền sở hữu địa chỉ (Chỉ cho phép địa chỉ thuộc tài khoản đang đăng nhập)
            Address address = DataStore.getAddressById(addressId, user.getUsername());
            if (address == null) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Địa chỉ nhận hàng không hợp lệ hoặc không thuộc tài khoản của bạn!\"}");
                return;
            }

            if (address.getPhone() == null || address.getPhone().trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng cập nhật số điện thoại người nhận hàng!\"}");
                return;
            }

            // Xây dựng danh sách chi tiết đơn hàng và TỰ TÍNH LẠI GIÁ BẰNG BACKEND
            List<OrderItem> orderItems = new ArrayList<>();
            double subtotal = 0;
            for (OrderItemReq req : reqItems) {
                Book b = DataStore.getBookById(req.bookId);
                if (b == null) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Sản phẩm có mã ID " + req.bookId + " không tồn tại trên hệ thống!\"}");
                    return;
                }
                OrderItem oi = new OrderItem(b.getId(), b.getCode(), b.getTitle(), b.getImage(), b.getPrice(), req.quantity);
                orderItems.add(oi);
                subtotal += b.getPrice() * req.quantity;
            }

            // Bước 2: KIỂM TRA TỒN KHO (KHO)
            StringBuilder stockErrorMsg = new StringBuilder();
            if (!DataStore.checkStockAvailable(orderItems, stockErrorMsg)) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"stockError\":true,\"message\":" + escapeJson(stockErrorMsg.toString()) + "}");
                return;
            }

            // Bước 4: Kiểm tra và tính mã giảm giá
            double discountAmount = 0;
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                Voucher v = DataStore.findVoucher(voucherCode.trim());
                if (v == null) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã giảm giá '" + escapeJson(voucherCode) + "' không tồn tại!\"}");
                    return;
                }
                String vErr = v.validate(subtotal);
                if (vErr != null) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":" + escapeJson(vErr) + "}");
                    return;
                }
                discountAmount = v.calculateDiscount(subtotal);
            }

            // Phí vận chuyển: Miễn phí nếu subtotal >= 250.000đ, ngược lại 30.000đ
            double shippingFee = subtotal >= 250000 ? 0 : 30000;
            double totalAmount = Math.max(0, subtotal - discountAmount + shippingFee);

            // BƯỚC 6: XỬ LÝ PHƯƠNG THỨC THANH TOÁN
            if ("ONLINE".equalsIgnoreCase(paymentMethod)) {
                // Thanh toán trực tuyến: Trả về thông tin chuyển khoản / cổng thanh toán (CHƯA TRỪ KHO)
                StringBuilder itemsJson = new StringBuilder("[");
                for (int i = 0; i < orderItems.size(); i++) {
                    OrderItem oi = orderItems.get(i);
                    if (i > 0) itemsJson.append(",");
                    itemsJson.append(String.format("{\"bookId\":%d,\"bookTitle\":%s,\"quantity\":%d,\"price\":%.0f,\"subtotal\":%.0f}",
                            oi.getBookId(),
                            escapeJson(oi.getBookTitle()),
                            oi.getQuantity(),
                            oi.getPrice(),
                            oi.getSubtotal()
                    ));
                }
                itemsJson.append("]");

                String json = String.format("{\"success\":true,\"needsOnlineModal\":true,\"subtotal\":%.0f,\"discountAmount\":%.0f,\"shippingFee\":%.0f,\"totalAmount\":%.0f,\"recipientName\":%s,\"recipientPhone\":%s,\"deliveryAddress\":%s,\"voucherCode\":%s,\"addressId\":%d,\"note\":%s,\"items\":%s}",
                        subtotal,
                        discountAmount,
                        shippingFee,
                        totalAmount,
                        escapeJson(address.getRecipientName()),
                        escapeJson(address.getPhone()),
                        escapeJson(address.getFullAddress()),
                        escapeJson(voucherCode != null ? voucherCode : ""),
                        address.getId(),
                        escapeJson(note),
                        itemsJson.toString()
                );
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
                return;
            }

            // Thanh toán COD (Tiền mặt khi nhận hàng):
            // 1. Trừ kho nguyên tử (Atomic deduction)
            boolean deducted = DataStore.deductStock(orderItems);
            if (!deducted) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Số lượng sản phẩm trong kho vừa thay đổi, không đủ để hoàn tất đơn hàng!\"}");
                return;
            }

            // 2. Tạo đơn hàng và chi tiết đơn hàng (DON_HANG, CT_DON_HANG)
            Order order = DataStore.createOrder(
                    user.getUsername(),
                    address,
                    orderItems,
                    voucherCode,
                    subtotal,
                    discountAmount,
                    shippingFee,
                    totalAmount,
                    "COD",
                    "PENDING",
                    "CHO_XAC_NHAN",
                    note,
                    null
            );

            // 3. Xóa giỏ hàng trên DB & Client
            DataStore.syncCartToDb(user.getUsername(), Collections.emptyList());

            String json = String.format("{\"success\":true,\"message\":\"Đặt hàng thành công!\",\"orderCode\":%s,\"totalAmount\":%.0f,\"paymentMethod\":\"COD\",\"redirectUrl\":%s}",
                    escapeJson(order.getOrderCode()),
                    totalAmount,
                    escapeJson("/order-success?code=" + order.getOrderCode())
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    /**
     * API xác nhận thanh toán trực tuyến mô phỏng (ONLINE)
     * - Nếu action = "SUCCESS": Trừ kho, tạo DON_HANG, CT_DON_HANG, cập nhật trạng thái PAID, xóa giỏ hàng
     * - Nếu action = "FAILED": KHÔNG TRỪ KHO, thông báo "Thanh toán thất bại", cho phép thanh toán lại hoặc chọn COD
     */
    static class ApiConfirmOnlinePaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                sendResponse(exchange, 401, "application/json; charset=UTF-8", "{\"success\":false,\"needLogin\":true,\"message\":\"Vui lòng đăng nhập!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String action = extractJsonString(body, "action"); // "SUCCESS" hoặc "FAILED"
            int addressId = extractJsonInt(body, "addressId", 0);
            String voucherCode = extractJsonString(body, "voucherCode");
            String note = extractJsonString(body, "note");
            if (note == null) note = "";

            List<OrderItemReq> reqItems = extractOrderItems(body);

            // TRƯỜNG HỢP: THANH TOÁN THẤT BẠI (Bắt buộc theo Nghiệp vụ mục 3 & 4)
            if ("FAILED".equalsIgnoreCase(action)) {
                // Tuyệt đối KHÔNG trừ kho, giữ nguyên giỏ hàng, cho phép thử lại hoặc đổi COD
                sendResponse(exchange, 200, "application/json; charset=UTF-8",
                        "{\"success\":false,\"paymentFailed\":true,\"message\":\"Thanh toán thất bại! Giao dịch trực tuyến đã bị từ chối hoặc do quý khách hủy bỏ. Kho hàng không bị trừ. Bạn có thể thử thanh toán lại hoặc chọn phương thức Thanh toán khi nhận hàng (COD).\",\"canRetry\":true,\"allowCodSwitch\":true}");
                return;
            }

            // TRƯỜNG HỢP: THANH TOÁN THÀNH CÔNG
            Address address = DataStore.getAddressById(addressId, user.getUsername());
            if (address == null) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Địa chỉ nhận hàng không hợp lệ!\"}");
                return;
            }

            List<OrderItem> orderItems = new ArrayList<>();
            double subtotal = 0;
            for (OrderItemReq req : reqItems) {
                Book b = DataStore.getBookById(req.bookId);
                if (b == null) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Sách không tồn tại!\"}");
                    return;
                }
                OrderItem oi = new OrderItem(b.getId(), b.getCode(), b.getTitle(), b.getImage(), b.getPrice(), req.quantity);
                orderItems.add(oi);
                subtotal += b.getPrice() * req.quantity;
            }

            // Kiểm tra tồn kho
            StringBuilder stockErr = new StringBuilder();
            if (!DataStore.checkStockAvailable(orderItems, stockErr)) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"stockError\":true,\"message\":" + escapeJson(stockErr.toString()) + "}");
                return;
            }

            // Trừ tồn kho
            boolean deducted = DataStore.deductStock(orderItems);
            if (!deducted) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tồn kho không đủ để hoàn tất thanh toán!\"}");
                return;
            }

            double discountAmount = 0;
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                Voucher v = DataStore.findVoucher(voucherCode.trim());
                if (v != null && v.validate(subtotal) == null) {
                    discountAmount = v.calculateDiscount(subtotal);
                }
            }
            double shippingFee = subtotal >= 250000 ? 0 : 30000;
            double totalAmount = Math.max(0, subtotal - discountAmount + shippingFee);

            String transactionId = "VNQR" + System.currentTimeMillis();
            Order order = DataStore.createOrder(
                    user.getUsername(),
                    address,
                    orderItems,
                    voucherCode,
                    subtotal,
                    discountAmount,
                    shippingFee,
                    totalAmount,
                    "ONLINE",
                    "PAID",
                    "DANG_XU_LY",
                    note,
                    transactionId
            );

            // Xóa giỏ hàng
            DataStore.syncCartToDb(user.getUsername(), Collections.emptyList());

            String json = String.format("{\"success\":true,\"message\":\"Thanh toán trực tuyến thành công!\",\"orderCode\":%s,\"totalAmount\":%.0f,\"paymentMethod\":\"ONLINE\",\"transactionId\":%s,\"redirectUrl\":%s}",
                    escapeJson(order.getOrderCode()),
                    totalAmount,
                    escapeJson(transactionId),
                    escapeJson("/order-success?code=" + order.getOrderCode())
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    /**
     * API tra cứu chi tiết đơn hàng (Dành cho trang /order-success và tra cứu)
     */
    static class ApiGetOrderDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            String query = exchange.getRequestURI().getQuery();
            String code = "";
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("code")) code = qp.get("code");
            }

            if (code.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Thiếu mã đơn hàng!\"}");
                return;
            }

            Order order = DataStore.getOrderByCode(code, user != null ? user.getUsername() : null);
            if (order == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không tìm thấy đơn hàng hoặc đơn hàng không thuộc quyền xem của bạn!\"}");
                return;
            }

            StringBuilder itemsJson = new StringBuilder("[");
            for (int i = 0; i < order.getItems().size(); i++) {
                OrderItem it = order.getItems().get(i);
                if (i > 0) itemsJson.append(",");
                itemsJson.append(String.format("{\"bookId\":%d,\"bookCode\":%s,\"bookTitle\":%s,\"bookImage\":%s,\"price\":%.0f,\"quantity\":%d,\"subtotal\":%.0f,\"formattedPrice\":%s,\"formattedSubtotal\":%s}",
                        it.getBookId(),
                        escapeJson(it.getBookCode()),
                        escapeJson(it.getBookTitle()),
                        escapeJson(it.getBookImage()),
                        it.getPrice(),
                        it.getQuantity(),
                        it.getSubtotal(),
                        escapeJson(it.getFormattedPrice()),
                        escapeJson(it.getFormattedSubtotal())
                ));
            }
            itemsJson.append("]");

            String json = String.format("{\"success\":true,\"order\":{\"id\":%d,\"orderCode\":%s,\"recipientName\":%s,\"recipientPhone\":%s,\"deliveryAddress\":%s,\"voucherCode\":%s,\"subtotal\":%.0f,\"discountAmount\":%.0f,\"shippingFee\":%.0f,\"totalAmount\":%.0f,\"formattedSubtotal\":%s,\"formattedDiscount\":%s,\"formattedShipping\":%s,\"formattedTotal\":%s,\"paymentMethodCode\":%s,\"paymentMethodName\":%s,\"paymentStatus\":%s,\"orderStatus\":%s,\"note\":%s,\"transactionId\":%s,\"createdAt\":%s,\"items\":%s}}",
                    order.getId(),
                    escapeJson(order.getOrderCode()),
                    escapeJson(order.getRecipientName()),
                    escapeJson(order.getRecipientPhone()),
                    escapeJson(order.getDeliveryAddress()),
                    escapeJson(order.getVoucherCode() != null ? order.getVoucherCode() : ""),
                    order.getSubtotal(),
                    order.getDiscountAmount(),
                    order.getShippingFee(),
                    order.getTotalAmount(),
                    escapeJson(order.getFormattedSubtotal()),
                    escapeJson(order.getFormattedDiscount()),
                    escapeJson(order.getFormattedShipping()),
                    escapeJson(order.getFormattedTotal()),
                    escapeJson(order.getPaymentMethodCode()),
                    escapeJson(order.getPaymentMethodName()),
                    escapeJson(order.getPaymentStatus()),
                    escapeJson(order.getOrderStatus()),
                    escapeJson(order.getNote() != null ? order.getNote() : ""),
                    escapeJson(order.getTransactionId() != null ? order.getTransactionId() : ""),
                    escapeJson(order.getCreatedAt()),
                    itemsJson.toString()
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    /**
     * API: GET /api/orders/my-orders
     * Trả về danh sách đơn hàng của người dùng đang đăng nhập (DON_HANG)
     */
    static class ApiMyOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                sendResponse(exchange, 401, "application/json; charset=UTF-8",
                        "{\"success\":false,\"message\":\"Vui lòng đăng nhập!\",\"needLogin\":true}");
                return;
            }

            List<Order> orders = DataStore.getOrdersByUsername(user.getUsername());

            StringBuilder sb = new StringBuilder("{\"success\":true,\"orders\":[");
            for (int i = 0; i < orders.size(); i++) {
                Order o = orders.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                    "{\"id\":%d,\"orderCode\":%s,\"recipientName\":%s,\"recipientPhone\":%s," +
                    "\"deliveryAddress\":%s,\"subtotal\":%.0f,\"discountAmount\":%.0f,\"shippingFee\":%.0f,\"totalAmount\":%.0f,\"formattedTotal\":%s," +
                    "\"paymentMethodName\":%s,\"paymentStatus\":%s,\"orderStatus\":%s,\"createdAt\":%s,\"items\":[",
                    o.getId(),
                    escapeJson(o.getOrderCode()),
                    escapeJson(o.getRecipientName()),
                    escapeJson(o.getRecipientPhone()),
                    escapeJson(o.getDeliveryAddress()),
                    o.getSubtotal(),
                    o.getDiscountAmount(),
                    o.getShippingFee(),
                    o.getTotalAmount(),
                    escapeJson(o.getFormattedTotal()),
                    escapeJson(o.getPaymentMethodName()),
                    escapeJson(o.getPaymentStatus()),
                    escapeJson(o.getOrderStatus()),
                    escapeJson(o.getCreatedAt())
                ));
                List<OrderItem> items = o.getItems();
                if (items != null) {
                    for (int j = 0; j < items.size(); j++) {
                        OrderItem item = items.get(j);
                        if (j > 0) sb.append(",");
                        sb.append(String.format(
                            "{\"id\":%d,\"bookId\":%d,\"bookCode\":%s,\"bookTitle\":%s,\"bookImage\":%s,\"price\":%.0f,\"quantity\":%d,\"subtotal\":%.0f}",
                            item.getId(),
                            item.getBookId(),
                            escapeJson(item.getBookCode()),
                            escapeJson(item.getBookTitle()),
                            escapeJson(item.getBookImage()),
                            item.getPrice(),
                            item.getQuantity(),
                            item.getSubtotal()
                        ));
                    }
                }
                sb.append("]}");
            }
            sb.append("]}");

            sendResponse(exchange, 200, "application/json; charset=UTF-8", sb.toString());
        }
    }

    /**
     * API Admin: GET /api/admin/books
     * Lấy danh sách sách, hỗ trợ tìm kiếm (q) và bộ lọc (category, author, publisher, stockStatus)
     */
    static class ApiAdminBooksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Từ chối truy cập! Quyền ADMIN là bắt buộc.\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String keyword = "";
            String category = "Tất cả";
            String author = "Tất cả";
            String publisher = "Tất cả";
            String stockStatus = "all";

            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("q")) keyword = qp.get("q");
                if (qp.containsKey("category")) category = qp.get("category");
                if (qp.containsKey("author")) author = qp.get("author");
                if (qp.containsKey("publisher")) publisher = qp.get("publisher");
                if (qp.containsKey("stockStatus")) stockStatus = qp.get("stockStatus");
            }

            List<Book> books = DataStore.searchBooks(keyword, category, author, publisher, null, null, stockStatus);
            List<String> categories = DataStore.getCategories();
            List<String> authors = DataStore.getAuthors();
            List<String> publishers = DataStore.getPublishers();

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"success\":true,");
            sb.append("\"total\":").append(books.size()).append(",");

            // Books JSON array
            sb.append("\"books\":[");
            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                        "{\"id\":%d,\"code\":%s,\"title\":%s,\"author\":%s,\"publisher\":%s,\"price\":%.0f,\"originalPrice\":%.0f,\"formattedPrice\":%s,\"category\":%s,\"stock\":%d,\"rating\":%.1f,\"reviewCount\":%d,\"image\":%s,\"description\":%s,\"promotion\":%s,\"isBestSeller\":%b,\"stockStatusText\":%s}",
                        b.getId(),
                        escapeJson(b.getCode()),
                        escapeJson(b.getTitle()),
                        escapeJson(b.getAuthor()),
                        escapeJson(b.getPublisher()),
                        b.getPrice(),
                        b.getOriginalPrice(),
                        escapeJson(b.getFormattedPrice()),
                        escapeJson(b.getCategory()),
                        b.getStock(),
                        b.getRating(),
                        b.getReviewCount(),
                        escapeJson(b.getImage()),
                        escapeJson(b.getDescription()),
                        escapeJson(b.getPromotion()),
                        b.isBestSeller(),
                        escapeJson(b.getStockStatusText())
                ));
            }
            sb.append("],");

            // Categories
            sb.append("\"categories\":[");
            for (int i = 0; i < categories.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(escapeJson(categories.get(i)));
            }
            sb.append("],");

            // Authors
            sb.append("\"authors\":[");
            for (int i = 0; i < authors.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(escapeJson(authors.get(i)));
            }
            sb.append("],");

            // Publishers
            sb.append("\"publishers\":[");
            for (int i = 0; i < publishers.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(escapeJson(publishers.get(i)));
            }
            sb.append("]}");

            sendResponse(exchange, 200, "application/json; charset=UTF-8", sb.toString());
        }
    }

    /**
     * API Admin: GET /api/admin/books/detail?id=...
     */
    static class ApiAdminBookDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Từ chối truy cập! Quyền ADMIN là bắt buộc.\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            int id = 0;
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("id")) {
                    try { id = Integer.parseInt(qp.get("id")); } catch (Exception ignored) {}
                }
            }

            Book b = DataStore.getBookById(id);
            if (b == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không tìm thấy sách phù hợp!\"}");
                return;
            }

            String json = String.format(
                    "{\"success\":true,\"book\":{\"id\":%d,\"code\":%s,\"title\":%s,\"author\":%s,\"publisher\":%s,\"price\":%.0f,\"originalPrice\":%.0f,\"formattedPrice\":%s,\"category\":%s,\"stock\":%d,\"rating\":%.1f,\"reviewCount\":%d,\"image\":%s,\"description\":%s,\"promotion\":%s,\"isBestSeller\":%b,\"stockStatusText\":%s,\"publishYear\":%d,\"pageCount\":%d,\"weight\":%d,\"dimensions\":%s,\"coverFormat\":%s}}",
                    b.getId(),
                    escapeJson(b.getCode()),
                    escapeJson(b.getTitle()),
                    escapeJson(b.getAuthor()),
                    escapeJson(b.getPublisher()),
                    b.getPrice(),
                    b.getOriginalPrice(),
                    escapeJson(b.getFormattedPrice()),
                    escapeJson(b.getCategory()),
                    b.getStock(),
                    b.getRating(),
                    b.getReviewCount(),
                    escapeJson(b.getImage()),
                    escapeJson(b.getDescription()),
                    escapeJson(b.getPromotion()),
                    b.isBestSeller(),
                    escapeJson(b.getStockStatusText()),
                    b.getPublishYear(),
                    b.getPageCount(),
                    b.getWeight(),
                    escapeJson(b.getDimensions()),
                    escapeJson(b.getCoverFormat())
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    /**
     * API Admin: POST /api/admin/books/add
     */
    static class ApiAdminBookAddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Từ chối truy cập! Quyền ADMIN là bắt buộc.\"}");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            String title = extractJsonString(body, "title");
            if (title == null) title = params.getOrDefault("title", "").trim();

            String code = extractJsonString(body, "code");
            if (code == null) code = params.getOrDefault("code", "").trim();

            String author = extractJsonString(body, "author");
            if (author == null) author = params.getOrDefault("author", "").trim();

            String publisher = extractJsonString(body, "publisher");
            if (publisher == null) publisher = params.getOrDefault("publisher", "").trim();

            String category = extractJsonString(body, "category");
            if (category == null) category = params.getOrDefault("category", "").trim();

            String desc = extractJsonString(body, "description");
            if (desc == null) desc = params.getOrDefault("description", "").trim();

            String image = extractJsonString(body, "image");
            if (image == null) image = params.getOrDefault("image", "").trim();

            String promotion = extractJsonString(body, "promotion");
            if (promotion == null) promotion = params.getOrDefault("promotion", "").trim();

            int stock = extractJsonInt(body, "stock", -999);
            if (stock == -999 && params.containsKey("stock")) {
                try { stock = Integer.parseInt(params.get("stock")); } catch (Exception ignored) {}
            }

            double price = -1;
            String priceStr = extractJsonString(body, "price");
            if (priceStr == null) priceStr = params.get("price");
            if (priceStr != null) {
                try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}
            }

            double origPrice = 0;
            String origPriceStr = extractJsonString(body, "originalPrice");
            if (origPriceStr == null) origPriceStr = params.get("originalPrice");
            if (origPriceStr != null) {
                try { origPrice = Double.parseDouble(origPriceStr); } catch (Exception ignored) {}
            }

            boolean isBestSeller = extractJsonBoolean(body, "isBestSeller", false);

            int publishYear = extractJsonInt(body, "publishYear", 2024);
            if (publishYear <= 0 && params.containsKey("publishYear")) {
                try { publishYear = Integer.parseInt(params.get("publishYear")); } catch (Exception ignored) {}
            }
            int pageCount = extractJsonInt(body, "pageCount", 250);
            if (pageCount <= 0 && params.containsKey("pageCount")) {
                try { pageCount = Integer.parseInt(params.get("pageCount")); } catch (Exception ignored) {}
            }
            int weight = extractJsonInt(body, "weight", 350);
            if (weight <= 0 && params.containsKey("weight")) {
                try { weight = Integer.parseInt(params.get("weight")); } catch (Exception ignored) {}
            }
            String dimensions = extractJsonString(body, "dimensions");
            if (dimensions == null) dimensions = params.getOrDefault("dimensions", "21 x 13.5 x 1.2 cm");

            String coverFormat = extractJsonString(body, "coverFormat");
            if (coverFormat == null) coverFormat = params.getOrDefault("coverFormat", "Bìa Mềm");

            // Validation
            if (title == null || title.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tên sách không được để trống.\"}");
                return;
            }
            if (author == null || author.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tác giả không hợp lệ.\"}");
                return;
            }
            if (publisher == null || publisher.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Nhà xuất bản không hợp lệ.\"}");
                return;
            }
            if (category == null || category.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Danh mục không hợp lệ.\"}");
                return;
            }
            if (price < 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Giá sách không hợp lệ.\"}");
                return;
            }
            if (stock < 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Số lượng tồn kho không được nhỏ hơn 0.\"}");
                return;
            }

            if (code == null || code.trim().isEmpty()) {
                code = "MS" + (System.currentTimeMillis() % 100000);
            }
            if (image == null || image.trim().isEmpty()) {
                image = "/images/default-book.jpg";
            }
            if (promotion == null || promotion.trim().isEmpty()) {
                promotion = "Sản phẩm chính hãng Bookora";
            }

            Book newBook = new Book(
                    0, code.trim(), title.trim(), author.trim(), publisher.trim(),
                    price, origPrice > 0 ? origPrice : price, category.trim(), stock,
                    5.0, 0, image.trim(), desc != null ? desc.trim() : "", promotion.trim(), isBestSeller,
                    publishYear, pageCount, weight, dimensions, coverFormat
            );

            boolean added = DataStore.addBook(newBook);
            if (added) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Thêm sách thành công.\"}");
            } else {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Có lỗi khi lưu sách vào cơ sở dữ liệu!\"}");
            }
        }
    }

    /**
     * API Admin: POST /api/admin/books/edit
     */
    static class ApiAdminBookEditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Từ chối truy cập! Quyền ADMIN là bắt buộc.\"}");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            int id = extractJsonInt(body, "id", 0);
            if (id <= 0 && params.containsKey("id")) {
                try { id = Integer.parseInt(params.get("id")); } catch (Exception ignored) {}
            }

            Book existing = DataStore.getBookById(id);
            if (existing == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Sách không tồn tại để chỉnh sửa!\"}");
                return;
            }

            String title = extractJsonString(body, "title");
            if (title == null) title = params.getOrDefault("title", "").trim();

            String code = extractJsonString(body, "code");
            if (code == null) code = params.getOrDefault("code", "").trim();

            String author = extractJsonString(body, "author");
            if (author == null) author = params.getOrDefault("author", "").trim();

            String publisher = extractJsonString(body, "publisher");
            if (publisher == null) publisher = params.getOrDefault("publisher", "").trim();

            String category = extractJsonString(body, "category");
            if (category == null) category = params.getOrDefault("category", "").trim();

            String desc = extractJsonString(body, "description");
            if (desc == null) desc = params.getOrDefault("description", "").trim();

            String image = extractJsonString(body, "image");
            if (image == null) image = params.getOrDefault("image", "").trim();

            String promotion = extractJsonString(body, "promotion");
            if (promotion == null) promotion = params.getOrDefault("promotion", "").trim();

            int stock = extractJsonInt(body, "stock", -999);
            if (stock == -999 && params.containsKey("stock")) {
                try { stock = Integer.parseInt(params.get("stock")); } catch (Exception ignored) {}
            }

            double price = -1;
            String priceStr = extractJsonString(body, "price");
            if (priceStr == null) priceStr = params.get("price");
            if (priceStr != null) {
                try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}
            }

            double origPrice = 0;
            String origPriceStr = extractJsonString(body, "originalPrice");
            if (origPriceStr == null) origPriceStr = params.get("originalPrice");
            if (origPriceStr != null) {
                try { origPrice = Double.parseDouble(origPriceStr); } catch (Exception ignored) {}
            }

            boolean isBestSeller = extractJsonBoolean(body, "isBestSeller", existing.isBestSeller());

            int publishYear = extractJsonInt(body, "publishYear", existing.getPublishYear());
            if (params.containsKey("publishYear")) {
                try { publishYear = Integer.parseInt(params.get("publishYear")); } catch (Exception ignored) {}
            }
            int pageCount = extractJsonInt(body, "pageCount", existing.getPageCount());
            if (params.containsKey("pageCount")) {
                try { pageCount = Integer.parseInt(params.get("pageCount")); } catch (Exception ignored) {}
            }
            int weight = extractJsonInt(body, "weight", existing.getWeight());
            if (params.containsKey("weight")) {
                try { weight = Integer.parseInt(params.get("weight")); } catch (Exception ignored) {}
            }
            String dimensions = extractJsonString(body, "dimensions");
            if (dimensions == null && params.containsKey("dimensions")) dimensions = params.get("dimensions");
            if (dimensions == null) dimensions = existing.getDimensions();

            String coverFormat = extractJsonString(body, "coverFormat");
            if (coverFormat == null && params.containsKey("coverFormat")) coverFormat = params.get("coverFormat");
            if (coverFormat == null) coverFormat = existing.getCoverFormat();

            // Validation
            if (title == null || title.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tên sách không được để trống.\"}");
                return;
            }
            if (author == null || author.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tác giả không hợp lệ.\"}");
                return;
            }
            if (publisher == null || publisher.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Nhà xuất bản không hợp lệ.\"}");
                return;
            }
            if (category == null || category.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Danh mục không hợp lệ.\"}");
                return;
            }
            if (price < 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Giá sách không hợp lệ.\"}");
                return;
            }
            if (stock < 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Số lượng tồn kho không được nhỏ hơn 0.\"}");
                return;
            }

            existing.setCode(code != null && !code.trim().isEmpty() ? code.trim() : existing.getCode());
            existing.setTitle(title.trim());
            existing.setAuthor(author.trim());
            existing.setPublisher(publisher.trim());
            existing.setCategory(category.trim());
            existing.setPrice(price);
            existing.setOriginalPrice(origPrice > 0 ? origPrice : price);
            existing.setStock(stock);
            if (image != null && !image.trim().isEmpty()) existing.setImage(image.trim());
            existing.setDescription(desc != null ? desc.trim() : "");
            existing.setPromotion(promotion != null ? promotion.trim() : "");
            existing.setBestSeller(isBestSeller);
            existing.setPublishYear(publishYear > 0 ? publishYear : 2024);
            existing.setPageCount(pageCount > 0 ? pageCount : 250);
            existing.setWeight(weight > 0 ? weight : 350);
            existing.setDimensions(dimensions != null && !dimensions.trim().isEmpty() ? dimensions.trim() : "21 x 13.5 x 1.2 cm");
            existing.setCoverFormat(coverFormat != null && !coverFormat.trim().isEmpty() ? coverFormat.trim() : "Bìa Mềm");

            boolean updated = DataStore.updateBook(existing);
            if (updated) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Cập nhật sách thành công.\"}");
            } else {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Có lỗi khi cập nhật cơ sở dữ liệu!\"}");
            }
        }
    }

    /**
     * API Admin: POST /api/admin/books/update-stock
     */
    static class ApiAdminStockUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Từ chối truy cập! Quyền ADMIN là bắt buộc.\"}");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            int id = extractJsonInt(body, "id", 0);
            if (id <= 0 && params.containsKey("id")) {
                try { id = Integer.parseInt(params.get("id")); } catch (Exception ignored) {}
            }

            Book existing = DataStore.getBookById(id);
            if (existing == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Sách không tồn tại!\"}");
                return;
            }

            int newStock = extractJsonInt(body, "stock", -999);
            if (newStock == -999 && params.containsKey("stock")) {
                try { newStock = Integer.parseInt(params.get("stock")); } catch (Exception ignored) {}
            }
            if (newStock == -999 && params.containsKey("newStock")) {
                try { newStock = Integer.parseInt(params.get("newStock")); } catch (Exception ignored) {}
            }

            if (newStock < 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Số lượng tồn kho không được nhỏ hơn 0.\"}");
                return;
            }

            boolean updated = DataStore.updateStock(id, newStock);
            if (updated) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", String.format("{\"success\":true,\"message\":\"Cập nhật tồn kho thành công.\",\"newStock\":%d}", newStock));
            } else {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Có lỗi khi cập nhật kho!\"}");
            }
        }
    }

    /**
     * API Admin: POST /api/admin/books/delete
     */
    static class ApiAdminBookDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Từ chối truy cập! Quyền ADMIN là bắt buộc.\"}");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            int id = extractJsonInt(body, "id", 0);
            if (id <= 0 && params.containsKey("id")) {
                try { id = Integer.parseInt(params.get("id")); } catch (Exception ignored) {}
            }

            String errorMsg = DataStore.deleteBook(id);
            if (errorMsg != null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", String.format("{\"success\":false,\"message\":%s}", escapeJson(errorMsg)));
            } else {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Xóa sách thành công.\"}");
            }
        }
    }

    /**
     * API: POST /api/upload-avatar
     * Nhận file ảnh tải lên từ máy tính (Base64 data URL), lưu vào thư mục /uploads/avatars/
     * và cập nhật trực tiếp vào cơ sở dữ liệu MySQL.
     */
    static class ApiUploadAvatarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                sendResponse(exchange, 401, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng đăng nhập để tải ảnh đại diện!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String avatarData = extractJsonString(body, "avatarData");
            if (avatarData == null || avatarData.trim().isEmpty()) {
                Map<String, String> formData = parseFormData(exchange);
                avatarData = formData.getOrDefault("avatarData", "");
            }

            if (avatarData == null || avatarData.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng chọn tệp hình ảnh từ máy tính!\"}");
                return;
            }

            try {
                String base64Content = avatarData.trim();
                String ext = ".jpg";
                if (base64Content.contains("data:image/png;base64,")) {
                    ext = ".png";
                    base64Content = base64Content.replace("data:image/png;base64,", "");
                } else if (base64Content.contains("data:image/jpeg;base64,")) {
                    ext = ".jpg";
                    base64Content = base64Content.replace("data:image/jpeg;base64,", "");
                } else if (base64Content.contains("data:image/webp;base64,")) {
                    ext = ".webp";
                    base64Content = base64Content.replace("data:image/webp;base64,", "");
                } else if (base64Content.contains("data:image/gif;base64,")) {
                    ext = ".gif";
                    base64Content = base64Content.replace("data:image/gif;base64,", "");
                } else if (base64Content.contains(";base64,")) {
                    base64Content = base64Content.substring(base64Content.indexOf(";base64,") + 8);
                }

                byte[] imageBytes = Base64.getDecoder().decode(base64Content.replaceAll("\\s+", ""));
                if (imageBytes.length > 5 * 1024 * 1024) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Kích thước hình ảnh quá lớn! Vui lòng chọn ảnh nhỏ hơn 5MB.\"}");
                    return;
                }

                String filename = "avatar_" + user.getUsername().toLowerCase() + "_" + System.currentTimeMillis() + ext;
                Path uploadDir = WEBAPP_DIR.resolve("uploads").resolve("avatars");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                Path filePath = uploadDir.resolve(filename);
                Files.write(filePath, imageBytes);

                String avatarUrl = "/uploads/avatars/" + filename;

                // Cập nhật thông tin avatar vào MySQL và memory
                boolean updated = DataStore.updateUserProfile(user.getUsername(), user.getFullName(), user.getEmail(), user.getPhone(), avatarUrl);

                if (updated) {
                    sendResponse(exchange, 200, "application/json; charset=UTF-8",
                            String.format("{\"success\":true,\"message\":\"Tải lên và cập nhật ảnh đại diện thành công!\",\"avatarUrl\":%s}", escapeJson(avatarUrl)));
                } else {
                    sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không thể cập nhật ảnh đại diện vào cơ sở dữ liệu!\"}");
                }

            } catch (Exception e) {
                System.err.println("⚠️ Lỗi xử lý tải ảnh đại diện: " + e.getMessage());
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Định dạng hình ảnh không hợp lệ hoặc bị lỗi mã hóa!\"}");
            }
        }
    }

    /**
     * API Admin: POST /api/admin/books/upload-cover
     * Nhận file ảnh tải lên từ máy tính (Base64 data URL), lưu vào thư mục /uploads/books/
     */
    static class ApiAdminBookUploadCoverHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Từ chối truy cập! Quyền ADMIN là bắt buộc.\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String coverData = extractJsonString(body, "coverData");
            if (coverData == null || coverData.trim().isEmpty()) {
                Map<String, String> formData = parseFormData(exchange);
                coverData = formData.getOrDefault("coverData", "");
            }

            if (coverData == null || coverData.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng chọn tệp hình ảnh từ máy tính!\"}");
                return;
            }

            try {
                String base64Content = coverData.trim();
                String ext = ".jpg";
                if (base64Content.contains("data:image/png;base64,")) {
                    ext = ".png";
                    base64Content = base64Content.replace("data:image/png;base64,", "");
                } else if (base64Content.contains("data:image/jpeg;base64,")) {
                    ext = ".jpg";
                    base64Content = base64Content.replace("data:image/jpeg;base64,", "");
                } else if (base64Content.contains("data:image/webp;base64,")) {
                    ext = ".webp";
                    base64Content = base64Content.replace("data:image/webp;base64,", "");
                } else if (base64Content.contains("data:image/gif;base64,")) {
                    ext = ".gif";
                    base64Content = base64Content.replace("data:image/gif;base64,", "");
                } else if (base64Content.contains(";base64,")) {
                    base64Content = base64Content.substring(base64Content.indexOf(";base64,") + 8);
                }

                byte[] imageBytes = Base64.getDecoder().decode(base64Content.replaceAll("\\s+", ""));
                if (imageBytes.length > 5 * 1024 * 1024) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Kích thước hình ảnh quá lớn! Vui lòng chọn ảnh nhỏ hơn 5MB.\"}");
                    return;
                }

                String filename = "book_cover_" + System.currentTimeMillis() + ext;
                Path uploadDir = WEBAPP_DIR.resolve("uploads").resolve("books");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                Path filePath = uploadDir.resolve(filename);
                Files.write(filePath, imageBytes);

                String imageUrl = "/uploads/books/" + filename;

                sendResponse(exchange, 200, "application/json; charset=UTF-8",
                        String.format("{\"success\":true,\"message\":\"Tải ảnh bìa sách thành công!\",\"imageUrl\":%s}", escapeJson(imageUrl)));

            } catch (Exception e) {
                System.err.println("⚠️ Lỗi xử lý tải ảnh bìa sách: " + e.getMessage());
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Định dạng hình ảnh không hợp lệ hoặc bị lỗi mã hóa!\"}");
            }
        }
    }

    // =========================================================================
    // API QUẢN LÝ KHÁCH HÀNG DÀNH CHO ADMIN (/api/admin/customers/*)
    // =========================================================================

    /**
     * GET /api/admin/customers?search=...
     * Tra cứu & lấy danh sách khách hàng
     */
    static class ApiAdminCustomersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String search = "";
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                search = qp.getOrDefault("search", "").trim().toLowerCase();
            }

            List<User> allUsers = DataStore.getAllUsers();
            List<Order> allOrders = DataStore.getAllOrders();

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"customers\":[");
            boolean first = true;

            for (User u : allUsers) {
                // Chỉ xử lý các tài khoản khách hàng (không bao gồm ADMIN)
                if ("ADMIN".equalsIgnoreCase(u.getRole())) {
                    continue;
                }

                // Lọc theo từ khóa tìm kiếm nếu có
                if (!search.isEmpty()) {
                    boolean match = (u.getUsername() != null && u.getUsername().toLowerCase().contains(search))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(search))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(search))
                            || (u.getPhone() != null && u.getPhone().toLowerCase().contains(search));
                    if (!match) {
                        continue;
                    }
                }

                // Tính số đơn hàng của khách hàng này
                long orderCount = allOrders.stream()
                        .filter(o -> o.getUsername() != null && o.getUsername().equalsIgnoreCase(u.getUsername()))
                        .count();

                if (!first) json.append(",");
                first = false;

                json.append(String.format(
                    "{\"username\":%s,\"fullName\":%s,\"email\":%s,\"phone\":%s,\"role\":%s,\"avatar\":%s,\"status\":%s,\"orderCount\":%d}",
                    escapeJson(u.getUsername()),
                    escapeJson(u.getFullName() != null ? u.getFullName() : ""),
                    escapeJson(u.getEmail() != null ? u.getEmail() : ""),
                    escapeJson(u.getPhone() != null ? u.getPhone() : ""),
                    escapeJson(u.getRole()),
                    escapeJson(u.getAvatar() != null ? u.getAvatar() : ""),
                    escapeJson(u.getStatus()),
                    orderCount
                ));
            }

            json.append("]}");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }
    }

    /**
     * GET /api/admin/customers/detail?username=...
     * Lấy thông tin chi tiết khách hàng và lịch sử mua hàng
     */
    static class ApiAdminCustomerDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String targetUsername = "";
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                targetUsername = qp.getOrDefault("username", "").trim();
            }

            if (targetUsername.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Thiếu mã khách hàng (username)\"}");
                return;
            }

            User customer = DataStore.findUser(targetUsername);
            if (customer == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không tìm thấy thông tin khách hàng.\"}");
                return;
            }

            List<Order> customerOrders = DataStore.getOrdersByUsername(targetUsername);

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,");
            json.append("\"customer\":{");
            json.append(String.format(
                "\"username\":%s,\"fullName\":%s,\"email\":%s,\"phone\":%s,\"role\":%s,\"avatar\":%s,\"status\":%s",
                escapeJson(customer.getUsername()),
                escapeJson(customer.getFullName() != null ? customer.getFullName() : ""),
                escapeJson(customer.getEmail() != null ? customer.getEmail() : ""),
                escapeJson(customer.getPhone() != null ? customer.getPhone() : ""),
                escapeJson(customer.getRole()),
                escapeJson(customer.getAvatar() != null ? customer.getAvatar() : ""),
                escapeJson(customer.getStatus())
            ));
            json.append("},\"orders\":[");

            boolean first = true;
            for (Order o : customerOrders) {
                if (!first) json.append(",");
                first = false;

                json.append(String.format(
                    "{\"id\":%d,\"orderCode\":%s,\"createdAt\":%s,\"totalAmount\":%.0f,\"formattedTotalAmount\":%s,\"orderStatus\":%s,\"orderStatusText\":%s,\"paymentMethodName\":%s}",
                    o.getId(),
                    escapeJson(o.getOrderCode()),
                    escapeJson(o.getCreatedAt() != null ? o.getCreatedAt() : ""),
                    o.getTotalAmount(),
                    escapeJson(o.getFormattedTotalAmount()),
                    escapeJson(o.getOrderStatus()),
                    escapeJson(o.getOrderStatusText()),
                    escapeJson(o.getPaymentMethodName() != null ? o.getPaymentMethodName() : "COD")
                ));
            }

            json.append("]}");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }
    }

    /**
     * POST /api/admin/customers/status
     * Khóa hoặc mở khóa tài khoản khách hàng
     */
    static class ApiAdminCustomerStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String targetUsername = extractJsonString(body, "username");
            if (targetUsername == null || targetUsername.trim().isEmpty()) {
                Map<String, String> params = parseQueryString(body);
                targetUsername = params.getOrDefault("username", "").trim();
            }
            targetUsername = targetUsername.trim();

            String newStatus = extractJsonString(body, "status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                Map<String, String> params = parseQueryString(body);
                newStatus = params.getOrDefault("status", "").trim();
            }
            newStatus = newStatus.trim().toUpperCase();

            if (targetUsername.isEmpty() || (!"ACTIVE".equals(newStatus) && !"LOCKED".equals(newStatus))) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Dữ liệu không hợp lệ.\"}");
                return;
            }

            User targetUser = DataStore.findUser(targetUsername);
            if (targetUser == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không tìm thấy tài khoản khách hàng.\"}");
                return;
            }

            if ("ADMIN".equalsIgnoreCase(targetUser.getRole())) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không thể khóa tài khoản Quản trị viên!\"}");
                return;
            }

            boolean ok = DataStore.updateUserStatus(targetUsername, newStatus);
            if (ok) {
                String msg = "LOCKED".equals(newStatus) ? "Khóa tài khoản thành công." : "Mở khóa tài khoản thành công.";
                sendResponse(exchange, 200, "application/json; charset=UTF-8", String.format("{\"success\":true,\"message\":%s,\"newStatus\":%s}", escapeJson(msg), escapeJson(newStatus)));
            } else {
                String msg = "LOCKED".equals(newStatus) ? "Khóa tài khoản thất bại." : "Mở khóa tài khoản thất bại.";
                sendResponse(exchange, 500, "application/json; charset=UTF-8", String.format("{\"success\":false,\"message\":%s}", escapeJson(msg)));
            }
        }
    }

    /**
     * GET /api/admin/customers/order-detail?orderCode=...
     * Chi tiết đơn hàng trong lịch sử mua hàng của khách hàng
     */
    static class ApiAdminCustomerOrderDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String orderCode = "";
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                orderCode = qp.getOrDefault("orderCode", "").trim();
            }

            if (orderCode.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Thiếu mã đơn hàng.\"}");
                return;
            }

            Order order = DataStore.getOrderByCode(orderCode);
            if (order == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không tìm thấy đơn hàng.\"}");
                return;
            }

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,");
            json.append("\"order\":{");
            json.append(String.format(
                "\"id\":%d,\"orderCode\":%s,\"username\":%s,\"recipientName\":%s,\"recipientPhone\":%s,\"deliveryAddress\":%s,\"voucherCode\":%s,\"subtotal\":%.0f,\"discountAmount\":%.0f,\"shippingFee\":%.0f,\"totalAmount\":%.0f,\"formattedTotalAmount\":%s,\"paymentMethodCode\":%s,\"paymentMethodName\":%s,\"paymentStatus\":%s,\"orderStatus\":%s,\"orderStatusText\":%s,\"note\":%s,\"createdAt\":%s",
                order.getId(),
                escapeJson(order.getOrderCode()),
                escapeJson(order.getUsername()),
                escapeJson(order.getRecipientName() != null ? order.getRecipientName() : ""),
                escapeJson(order.getRecipientPhone() != null ? order.getRecipientPhone() : ""),
                escapeJson(order.getDeliveryAddress() != null ? order.getDeliveryAddress() : ""),
                escapeJson(order.getVoucherCode() != null ? order.getVoucherCode() : ""),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getTotalAmount(),
                escapeJson(order.getFormattedTotalAmount()),
                escapeJson(order.getPaymentMethodCode() != null ? order.getPaymentMethodCode() : "COD"),
                escapeJson(order.getPaymentMethodName() != null ? order.getPaymentMethodName() : ""),
                escapeJson(order.getPaymentStatus() != null ? order.getPaymentStatus() : "PENDING"),
                escapeJson(order.getOrderStatus() != null ? order.getOrderStatus() : "CHO_XAC_NHAN"),
                escapeJson(order.getOrderStatusText()),
                escapeJson(order.getNote() != null ? order.getNote() : ""),
                escapeJson(order.getCreatedAt() != null ? order.getCreatedAt() : "")
            ));

            json.append("},\"items\":[");
            List<OrderItem> items = order.getItems() != null ? order.getItems() : new ArrayList<>();
            boolean first = true;
            for (OrderItem item : items) {
                if (!first) json.append(",");
                first = false;

                json.append(String.format(
                    "{\"id\":%d,\"bookId\":%d,\"bookCode\":%s,\"bookTitle\":%s,\"bookImage\":%s,\"price\":%.0f,\"quantity\":%d,\"subtotal\":%.0f}",
                    item.getId(),
                    item.getBookId(),
                    escapeJson(item.getBookCode() != null ? item.getBookCode() : ""),
                    escapeJson(item.getBookTitle() != null ? item.getBookTitle() : ""),
                    escapeJson(item.getBookImage() != null ? item.getBookImage() : ""),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getSubtotal()
                ));
            }

            json.append("]}");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }
    }

    // =========================================================================
    // API QUẢN LÝ DANH MỤC SÁCH DÀNH CHO ADMIN (/api/admin/categories/*)
    // =========================================================================

    /**
     * GET /api/admin/categories?search=...
     * Tra cứu & lấy danh sách danh mục sách
     */
    static class ApiAdminCategoriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String search = "";
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                search = qp.getOrDefault("search", "").trim().toLowerCase();
            }

            List<Category> allCategories = DataStore.getAllCategoriesList();

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"categories\":[");
            boolean first = true;

            for (Category c : allCategories) {
                if (!search.isEmpty()) {
                    boolean match = (c.getCode() != null && c.getCode().toLowerCase().contains(search))
                            || (c.getName() != null && c.getName().toLowerCase().contains(search))
                            || (c.getDescription() != null && c.getDescription().toLowerCase().contains(search));
                    if (!match) {
                        continue;
                    }
                }

                if (!first) json.append(",");
                first = false;

                json.append(String.format(
                    "{\"id\":%d,\"code\":%s,\"name\":%s,\"description\":%s,\"status\":%s,\"bookCount\":%d}",
                    c.getId(),
                    escapeJson(c.getCode() != null ? c.getCode() : ""),
                    escapeJson(c.getName() != null ? c.getName() : ""),
                    escapeJson(c.getDescription() != null ? c.getDescription() : ""),
                    escapeJson(c.getStatus()),
                    c.getBookCount()
                ));
            }

            json.append("]}");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }
    }

    /**
     * GET /api/admin/categories/detail?id=...
     * Lấy chi tiết thông tin danh mục và danh sách sách thuộc danh mục đó
     */
    static class ApiAdminCategoryDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            int id = 0;
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                try { id = Integer.parseInt(qp.getOrDefault("id", "0")); } catch (Exception ignored) {}
            }

            Category category = DataStore.getCategoryById(id);
            if (category == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không tìm thấy danh mục sách.\"}");
                return;
            }

            List<Book> booksInCat = DataStore.getBooksByCategory(category.getName());

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,");
            json.append("\"category\":{");
            json.append(String.format(
                "\"id\":%d,\"code\":%s,\"name\":%s,\"description\":%s,\"status\":%s,\"bookCount\":%d",
                category.getId(),
                escapeJson(category.getCode()),
                escapeJson(category.getName()),
                escapeJson(category.getDescription() != null ? category.getDescription() : ""),
                escapeJson(category.getStatus()),
                booksInCat.size()
            ));
            json.append("},\"books\":[");

            boolean first = true;
            for (Book b : booksInCat) {
                if (!first) json.append(",");
                first = false;

                json.append(String.format(
                    "{\"id\":%d,\"code\":%s,\"title\":%s,\"author\":%s,\"price\":%.0f,\"formattedPrice\":%s,\"stock\":%d,\"image\":%s}",
                    b.getId(),
                    escapeJson(b.getCode()),
                    escapeJson(b.getTitle()),
                    escapeJson(b.getAuthor()),
                    b.getPrice(),
                    escapeJson(b.getFormattedPrice()),
                    b.getStock(),
                    escapeJson(b.getImage() != null ? b.getImage() : "")
                ));
            }

            json.append("]}");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }
    }

    /**
     * POST /api/admin/categories/add
     * Thêm danh mục sách mới
     */
    static class ApiAdminCategoryAddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            String name = extractJsonString(body, "name");
            if (name == null || name.trim().isEmpty()) {
                name = params.getOrDefault("name", "").trim();
            }

            String code = extractJsonString(body, "code");
            if (code == null || code.trim().isEmpty()) {
                code = params.getOrDefault("code", "").trim();
            }

            String description = extractJsonString(body, "description");
            if (description == null) {
                description = params.getOrDefault("description", "").trim();
            }

            String status = extractJsonString(body, "status");
            if (status == null || status.trim().isEmpty()) {
                status = params.getOrDefault("status", "ACTIVE").trim();
            }

            // Validation 1: Tên danh mục không được để trống (Requirement 6)
            if (name.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tên danh mục không được để trống.\"}");
                return;
            }

            // Validation 2: Tên danh mục không được trùng (Requirement 6)
            if (DataStore.getCategoryByName(name.trim()) != null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tên danh mục đã tồn tại.\"}");
                return;
            }

            Category cat = new Category(0, code, name.trim(), description, status);
            boolean ok = DataStore.addCategory(cat);
            if (ok) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Thêm danh mục thành công.\"}");
            } else {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Thêm danh mục thất bại. Vui lòng thử lại.\"}");
            }
        }
    }

    /**
     * POST /api/admin/categories/edit
     * Sửa danh mục sách
     */
    static class ApiAdminCategoryEditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            int id = extractJsonInt(body, "id", 0);
            if (id == 0 && params.containsKey("id")) {
                try { id = Integer.parseInt(params.get("id")); } catch (Exception ignored) {}
            }

            String name = extractJsonString(body, "name");
            if (name == null || name.trim().isEmpty()) {
                name = params.getOrDefault("name", "").trim();
            }

            String description = extractJsonString(body, "description");
            if (description == null) {
                description = params.getOrDefault("description", "").trim();
            }

            String status = extractJsonString(body, "status");
            if (status == null || status.trim().isEmpty()) {
                status = params.getOrDefault("status", "ACTIVE").trim();
            }

            if (id <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Danh mục không hợp lệ.\"}");
                return;
            }

            Category existing = DataStore.getCategoryById(id);
            if (existing == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Danh mục không tồn tại.\"}");
                return;
            }

            // Validation 1: Tên không được rỗng (Requirement 7)
            if (name.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tên danh mục không được để trống.\"}");
                return;
            }

            // Validation 2: Tên không được trùng với danh mục khác (Requirement 7)
            Category existingByName = DataStore.getCategoryByName(name.trim());
            if (existingByName != null && existingByName.getId() != id) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tên danh mục đã tồn tại.\"}");
                return;
            }

            existing.setName(name.trim());
            existing.setDescription(description);
            existing.setStatus(status);

            boolean ok = DataStore.updateCategory(existing);
            if (ok) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Cập nhật danh mục thành công.\"}");
            } else {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Cập nhật danh mục thất bại.\"}");
            }
        }
    }

    /**
     * POST /api/admin/categories/delete
     * Xóa danh mục sách (Kiểm tra xem có sách sử dụng hay không)
     */
    static class ApiAdminCategoryDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            int id = extractJsonInt(body, "id", 0);
            if (id == 0 && params.containsKey("id")) {
                try { id = Integer.parseInt(params.get("id")); } catch (Exception ignored) {}
            }

            if (id <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã danh mục không hợp lệ.\"}");
                return;
            }

            Category cat = DataStore.getCategoryById(id);
            if (cat == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Danh mục không tồn tại.\"}");
                return;
            }

            // Kiểm tra danh mục đang có sách (Requirement 10)
            List<Book> booksInCat = DataStore.getBooksByCategory(cat.getName());
            if (booksInCat != null && !booksInCat.isEmpty()) {
                String errMsg = String.format("Không thể xóa danh mục vì danh mục đang được sử dụng bởi %d cuốn sách. Vui lòng chuyển các sách sang danh mục khác trước khi xóa.", booksInCat.size());
                sendResponse(exchange, 400, "application/json; charset=UTF-8", String.format("{\"success\":false,\"message\":%s,\"bookCount\":%d}", escapeJson(errMsg), booksInCat.size()));
                return;
            }

            boolean ok = DataStore.deleteCategory(id);
            if (ok) {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Xóa danh mục thành công.\"}");
            } else {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Xóa danh mục thất bại.\"}");
            }
        }
    }

    // =========================================================================
    // API QUẢN LÝ KHUYẾN MÃI VÀ MÃ GIẢM GIÁ DÀNH CHO ADMIN (/api/admin/promotions/*)
    // =========================================================================

    /**
     * GET /api/admin/promotions?search=...&status=...&type=...
     * Tra cứu & lọc danh sách mã giảm giá
     */
    static class ApiAdminPromotionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String search = "";
            String statusFilter = "ALL";
            String typeFilter = "ALL";

            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                search = qp.getOrDefault("search", "").trim().toLowerCase();
                statusFilter = qp.getOrDefault("status", "ALL").trim().toUpperCase();
                typeFilter = qp.getOrDefault("type", "ALL").trim().toUpperCase();
            }

            List<Voucher> allVouchers = DataStore.getAllVouchersList();
            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"vouchers\":[");
            boolean first = true;

            for (Voucher v : allVouchers) {
                // Search filter (code or title)
                if (!search.isEmpty()) {
                    boolean match = (v.getCode() != null && v.getCode().toLowerCase().contains(search))
                            || (v.getTitle() != null && v.getTitle().toLowerCase().contains(search))
                            || (v.getDescription() != null && v.getDescription().toLowerCase().contains(search));
                    if (!match) continue;
                }

                // Status filter (ALL, ACTIVE, SCHEDULED, EXPIRED, DISABLED, DEPLETED)
                if (!"ALL".equalsIgnoreCase(statusFilter)) {
                    if (!v.getStatusCode().equalsIgnoreCase(statusFilter)) {
                        continue;
                    }
                }

                // Type filter (ALL, PERCENT, FIXED)
                if (!"ALL".equalsIgnoreCase(typeFilter)) {
                    if (!v.getDiscountType().equalsIgnoreCase(typeFilter)) {
                        continue;
                    }
                }

                if (!first) json.append(",");
                first = false;

                json.append(String.format(
                    "{\"id\":%d,\"code\":%s,\"title\":%s,\"description\":%s,\"discountType\":%s,\"discountValue\":%.0f,\"minOrderAmount\":%.0f,\"maxDiscountAmount\":%.0f,\"startDate\":%s,\"endDate\":%s,\"isActive\":%b,\"usageLimit\":%d,\"usedCount\":%d,\"statusCode\":%s,\"statusText\":%s}",
                    v.getId(),
                    escapeJson(v.getCode()),
                    escapeJson(v.getTitle()),
                    escapeJson(v.getDescription() != null ? v.getDescription() : ""),
                    escapeJson(v.getDiscountType()),
                    v.getDiscountValue(),
                    v.getMinOrderAmount(),
                    v.getMaxDiscountAmount(),
                    escapeJson(v.getStartDate() != null ? v.getStartDate() : ""),
                    escapeJson(v.getEndDate() != null ? v.getEndDate() : ""),
                    v.isActive(),
                    v.getUsageLimit(),
                    v.getUsedCount(),
                    escapeJson(v.getStatusCode()),
                    escapeJson(v.getStatusText())
                ));
            }

            json.append("]}");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }
    }

    /**
     * GET /api/admin/promotions/detail?id=...
     * Xem chi tiết mã giảm giá và danh sách đơn hàng đã dùng mã
     */
    static class ApiAdminPromotionDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            int id = 0;
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                try { id = Integer.parseInt(qp.getOrDefault("id", "0")); } catch (Exception ignored) {}
            }

            Voucher v = DataStore.getVoucherById(id);
            if (v == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã giảm giá không tồn tại!\"}");
                return;
            }

            List<Order> usedOrders = DataStore.getOrdersByVoucherCode(v.getCode());

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"voucher\":");
            json.append(String.format(
                "{\"id\":%d,\"code\":%s,\"title\":%s,\"description\":%s,\"discountType\":%s,\"discountValue\":%.0f,\"minOrderAmount\":%.0f,\"maxDiscountAmount\":%.0f,\"startDate\":%s,\"endDate\":%s,\"isActive\":%b,\"usageLimit\":%d,\"usedCount\":%d,\"statusCode\":%s,\"statusText\":%s}",
                v.getId(),
                escapeJson(v.getCode()),
                escapeJson(v.getTitle()),
                escapeJson(v.getDescription() != null ? v.getDescription() : ""),
                escapeJson(v.getDiscountType()),
                v.getDiscountValue(),
                v.getMinOrderAmount(),
                v.getMaxDiscountAmount(),
                escapeJson(v.getStartDate() != null ? v.getStartDate() : ""),
                escapeJson(v.getEndDate() != null ? v.getEndDate() : ""),
                v.isActive(),
                v.getUsageLimit(),
                v.getUsedCount(),
                escapeJson(v.getStatusCode()),
                escapeJson(v.getStatusText())
            ));

            json.append(",\"usedOrders\":[");
            boolean first = true;
            for (Order o : usedOrders) {
                if (!first) json.append(",");
                first = false;
                json.append(String.format(
                    "{\"id\":%d,\"orderCode\":%s,\"username\":%s,\"recipientName\":%s,\"recipientPhone\":%s,\"discountAmount\":%.0f,\"totalAmount\":%.0f,\"orderStatus\":%s,\"createdAt\":%s}",
                    o.getId(),
                    escapeJson(o.getOrderCode()),
                    escapeJson(o.getUsername()),
                    escapeJson(o.getRecipientName() != null ? o.getRecipientName() : ""),
                    escapeJson(o.getRecipientPhone() != null ? o.getRecipientPhone() : ""),
                    o.getDiscountAmount(),
                    o.getTotalAmount(),
                    escapeJson(o.getOrderStatus()),
                    escapeJson(o.getCreatedAt() != null ? o.getCreatedAt() : "")
                ));
            }
            json.append("]}");

            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }
    }

    /**
     * POST /api/admin/promotions/add
     * Thêm mới mã giảm giá
     */
    static class ApiAdminPromotionAddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            String code = extractJsonString(body, "code");
            if (code == null) code = params.getOrDefault("code", "");

            String title = extractJsonString(body, "title");
            if (title == null) title = params.getOrDefault("title", "");

            String description = extractJsonString(body, "description");
            if (description == null) description = params.getOrDefault("description", "");

            String discountType = extractJsonString(body, "discountType");
            if (discountType == null) discountType = params.getOrDefault("discountType", "PERCENT");

            double discountValue = extractJsonDouble(body, "discountValue", -1);
            if (discountValue < 0 && params.containsKey("discountValue")) {
                try { discountValue = Double.parseDouble(params.get("discountValue")); } catch (Exception ignored) {}
            }

            double minOrderAmount = extractJsonDouble(body, "minOrderAmount", 0);
            if (minOrderAmount == 0 && params.containsKey("minOrderAmount")) {
                try { minOrderAmount = Double.parseDouble(params.get("minOrderAmount")); } catch (Exception ignored) {}
            }

            double maxDiscountAmount = extractJsonDouble(body, "maxDiscountAmount", 0);
            if (maxDiscountAmount == 0 && params.containsKey("maxDiscountAmount")) {
                try { maxDiscountAmount = Double.parseDouble(params.get("maxDiscountAmount")); } catch (Exception ignored) {}
            }

            int usageLimit = extractJsonInt(body, "usageLimit", 0);
            if (usageLimit == 0 && params.containsKey("usageLimit")) {
                try { usageLimit = Integer.parseInt(params.get("usageLimit")); } catch (Exception ignored) {}
            }

            String startDate = extractJsonString(body, "startDate");
            if (startDate == null) startDate = params.getOrDefault("startDate", "");

            String endDate = extractJsonString(body, "endDate");
            if (endDate == null) endDate = params.getOrDefault("endDate", "");

            boolean isActive = true;
            if (body.contains("\"isActive\":false")) isActive = false;

            // Validation rules
            if (code == null || code.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng nhập mã giảm giá!\"}");
                return;
            }

            String cleanCode = code.trim().toUpperCase().replaceAll("\\s+", "");
            if (cleanCode.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã giảm giá không chứa khoảng trắng hợp lệ!\"}");
                return;
            }

            if (title == null || title.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng nhập tên chương trình khuyến mãi!\"}");
                return;
            }

            if (!"PERCENT".equalsIgnoreCase(discountType) && !"FIXED".equalsIgnoreCase(discountType)) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Loại giảm giá không hợp lệ (Phần trăm hoặc Tiền cố định)!\"}");
                return;
            }

            if (discountValue <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Giá trị giảm phải lớn hơn 0!\"}");
                return;
            }

            if ("PERCENT".equalsIgnoreCase(discountType) && discountValue > 100) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Phần trăm giảm giá không được vượt quá 100%!\"}");
                return;
            }

            if (minOrderAmount < 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Đơn hàng tối thiểu không được nhỏ hơn 0!\"}");
                return;
            }

            if (maxDiscountAmount < 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mức giảm tối đa không được nhỏ hơn 0!\"}");
                return;
            }

            if (usageLimit < 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Số lần sử dụng không được nhỏ hơn 0!\"}");
                return;
            }

            if (startDate == null || startDate.trim().isEmpty() || endDate == null || endDate.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng chọn thời gian bắt đầu và kết thúc!\"}");
                return;
            }

            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate.trim());
                java.time.LocalDate end = java.time.LocalDate.parse(endDate.trim());
                if (start.isAfter(end)) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Ngày bắt đầu không được lớn hơn ngày kết thúc!\"}");
                    return;
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Định dạng ngày bắt đầu hoặc ngày kết thúc không hợp lệ (YYYY-MM-DD)!\"}");
                return;
            }

            Voucher v = new Voucher(0, cleanCode, title.trim(), description.trim(), discountType.toUpperCase(),
                    discountValue, minOrderAmount, maxDiscountAmount, startDate.trim(), endDate.trim(), isActive, usageLimit, 0);

            String err = DataStore.addVoucher(v);
            if (err != null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", String.format("{\"success\":false,\"message\":%s}", escapeJson(err)));
            } else {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Thêm mới mã giảm giá thành công!\"}");
            }
        }
    }

    /**
     * POST /api/admin/promotions/edit
     * Chỉnh sửa thông tin mã giảm giá
     */
    static class ApiAdminPromotionEditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            int id = extractJsonInt(body, "id", 0);
            if (id == 0 && params.containsKey("id")) {
                try { id = Integer.parseInt(params.get("id")); } catch (Exception ignored) {}
            }

            Voucher existing = DataStore.getVoucherById(id);
            if (existing == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã giảm giá không tồn tại!\"}");
                return;
            }

            String code = extractJsonString(body, "code");
            if (code == null) code = existing.getCode();

            String title = extractJsonString(body, "title");
            if (title == null) title = existing.getTitle();

            String description = extractJsonString(body, "description");
            if (description == null) description = existing.getDescription();

            String discountType = extractJsonString(body, "discountType");
            if (discountType == null) discountType = existing.getDiscountType();

            double discountValue = extractJsonDouble(body, "discountValue", existing.getDiscountValue());
            double minOrderAmount = extractJsonDouble(body, "minOrderAmount", existing.getMinOrderAmount());
            double maxDiscountAmount = extractJsonDouble(body, "maxDiscountAmount", existing.getMaxDiscountAmount());
            int usageLimit = extractJsonInt(body, "usageLimit", existing.getUsageLimit());

            String startDate = extractJsonString(body, "startDate");
            if (startDate == null) startDate = existing.getStartDate();

            String endDate = extractJsonString(body, "endDate");
            if (endDate == null) endDate = existing.getEndDate();

            boolean isActive = existing.isActive();
            if (body.contains("\"isActive\":false")) isActive = false;
            else if (body.contains("\"isActive\":true")) isActive = true;

            // Validations
            if (code == null || code.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng nhập mã giảm giá!\"}");
                return;
            }

            String cleanCode = code.trim().toUpperCase().replaceAll("\\s+", "");

            if (title == null || title.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng nhập tên chương trình khuyến mãi!\"}");
                return;
            }

            if (discountValue <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Giá trị giảm phải lớn hơn 0!\"}");
                return;
            }

            if ("PERCENT".equalsIgnoreCase(discountType) && discountValue > 100) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Phần trăm giảm giá không được vượt quá 100%!\"}");
                return;
            }

            if (minOrderAmount < 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Đơn hàng tối thiểu không được nhỏ hơn 0!\"}");
                return;
            }

            if (startDate == null || startDate.trim().isEmpty() || endDate == null || endDate.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Vui lòng chọn thời gian bắt đầu và kết thúc!\"}");
                return;
            }

            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate.trim());
                java.time.LocalDate end = java.time.LocalDate.parse(endDate.trim());
                if (start.isAfter(end)) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Ngày bắt đầu không được lớn hơn ngày kết thúc!\"}");
                    return;
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Định dạng ngày bắt đầu hoặc ngày kết thúc không hợp lệ (YYYY-MM-DD)!\"}");
                return;
            }

            Voucher updated = new Voucher(id, cleanCode, title.trim(), description.trim(), discountType.toUpperCase(),
                    discountValue, minOrderAmount, maxDiscountAmount, startDate.trim(), endDate.trim(), isActive, usageLimit, existing.getUsedCount());

            String err = DataStore.updateVoucher(updated);
            if (err != null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", String.format("{\"success\":false,\"message\":%s}", escapeJson(err)));
            } else {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Cập nhật mã giảm giá thành công!\"}");
            }
        }
    }

    /**
     * POST /api/admin/promotions/delete
     * Xóa mã giảm giá (Chỉ cho phép khi chưa được sử dụng trong đơn hàng)
     */
    static class ApiAdminPromotionDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            int id = extractJsonInt(body, "id", 0);
            if (id == 0 && params.containsKey("id")) {
                try { id = Integer.parseInt(params.get("id")); } catch (Exception ignored) {}
            }

            if (id <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã giảm giá không hợp lệ!\"}");
                return;
            }

            String err = DataStore.deleteVoucher(id);
            if (err != null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", String.format("{\"success\":false,\"message\":%s}", escapeJson(err)));
            } else {
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Xóa mã giảm giá thành công!\"}");
            }
        }
    }

    /**
     * POST /api/admin/promotions/status
     * Bật / Tắt trạng thái mã giảm giá
     */
    static class ApiAdminPromotionStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseQueryString(body);

            int id = extractJsonInt(body, "id", 0);
            if (id == 0 && params.containsKey("id")) {
                try { id = Integer.parseInt(params.get("id")); } catch (Exception ignored) {}
            }

            boolean active = true;
            if (body.contains("\"active\":false")) active = false;
            else if (params.containsKey("active")) active = Boolean.parseBoolean(params.get("active"));

            if (id <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Mã giảm giá không hợp lệ!\"}");
                return;
            }

            DataStore.toggleVoucherStatus(id, active);
            String statusMsg = active ? "Đã bật mã giảm giá!" : "Đã tắt mã giảm giá!";
            sendResponse(exchange, 200, "application/json; charset=UTF-8", String.format("{\"success\":true,\"message\":%s}", escapeJson(statusMsg)));
        }
    }

    // =========================================================================
    // API THỐNG KÊ VÀ BÁO CÁO DÀNH CHO ADMIN (UC15 - /api/admin/reports/*)
    // =========================================================================

    /**
     * GET /api/admin/reports?type=...&fromDate=...&toDate=...&search=...
     * Lấy dữ liệu thống kê theo loại báo cáo và khoảng thời gian
     */
    static class ApiAdminReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String type = "REVENUE";
            String fromDate = "";
            String toDate = "";
            String search = "";

            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                type = qp.getOrDefault("type", "REVENUE");
                fromDate = qp.getOrDefault("fromDate", "");
                toDate = qp.getOrDefault("toDate", "");
                search = qp.getOrDefault("search", "");
            }

            try {
                DataStore.ReportResult res = DataStore.getReportData(type, fromDate, toDate, search);

                if (res.isEmpty && "Khoảng thời gian không hợp lệ.".equals(res.message)) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Khoảng thời gian không hợp lệ.\"}");
                    return;
                }

                // Build JSON response
                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"success\":true,");
                json.append("\"reportType\":").append(escapeJson(res.reportType)).append(",");
                json.append("\"fromDate\":").append(escapeJson(res.fromDate)).append(",");
                json.append("\"toDate\":").append(escapeJson(res.toDate)).append(",");
                json.append("\"isEmpty\":").append(res.isEmpty).append(",");
                json.append("\"message\":").append(escapeJson(res.message)).append(",");

                // summary
                json.append("\"summary\":{");
                boolean firstS = true;
                for (Map.Entry<String, Object> entry : res.summary.entrySet()) {
                    if (!firstS) json.append(",");
                    firstS = false;
                    json.append("\"").append(entry.getKey()).append("\":");
                    if (entry.getValue() instanceof Number) {
                        json.append(entry.getValue());
                    } else if (entry.getValue() instanceof Boolean) {
                        json.append(entry.getValue());
                    } else {
                        json.append(escapeJson(String.valueOf(entry.getValue())));
                    }
                }
                json.append("},");

                // rows
                json.append("\"rows\":[");
                for (int i = 0; i < res.rows.size(); i++) {
                    if (i > 0) json.append(",");
                    Map<String, Object> row = res.rows.get(i);
                    json.append("{");
                    boolean firstR = true;
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        if (!firstR) json.append(",");
                        firstR = false;
                        json.append("\"").append(entry.getKey()).append("\":");
                        if (entry.getValue() instanceof Number) {
                            json.append(entry.getValue());
                        } else if (entry.getValue() instanceof Boolean) {
                            json.append(entry.getValue());
                        } else {
                            json.append(escapeJson(String.valueOf(entry.getValue())));
                        }
                    }
                    json.append("}");
                }
                json.append("],");

                // chartData
                json.append("\"chartData\":{");
                json.append("\"labels\":[");
                @SuppressWarnings("unchecked")
                List<String> labels = (List<String>) res.chartData.get("labels");
                if (labels != null) {
                    for (int i = 0; i < labels.size(); i++) {
                        if (i > 0) json.append(",");
                        json.append(escapeJson(labels.get(i)));
                    }
                }
                json.append("],\"values\":[");
                List<?> values = (List<?>) res.chartData.get("values");
                if (values != null) {
                    for (int i = 0; i < values.size(); i++) {
                        if (i > 0) json.append(",");
                        json.append(values.get(i));
                    }
                }
                json.append("]}");

                json.append("}");

                sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không thể tải dữ liệu thống kê. Vui lòng thử lại.\"}");
            }
        }
    }

    /**
     * GET /api/admin/reports/export?type=...&fromDate=...&toDate=...
     * Xuất dữ liệu thống kê thành file CSV/Excel UTF-8
     */
    static class ApiAdminReportsExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String type = "REVENUE";
            String fromDate = "";
            String toDate = "";
            String search = "";

            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                type = qp.getOrDefault("type", "REVENUE");
                fromDate = qp.getOrDefault("fromDate", "");
                toDate = qp.getOrDefault("toDate", "");
                search = qp.getOrDefault("search", "");
            }

            try {
                DataStore.ReportResult res = DataStore.getReportData(type, fromDate, toDate, search);

                if (res.isEmpty && "Khoảng thời gian không hợp lệ.".equals(res.message)) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Khoảng thời gian không hợp lệ.\"}");
                    return;
                }

                if (res.rows.isEmpty()) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không thể tạo file báo cáo.\"}");
                    return;
                }

                StringBuilder csv = new StringBuilder();
                csv.append("\uFEFF"); // UTF-8 BOM

                String filename = "Bao_Cao_" + type + "_" + res.fromDate + "_Den_" + res.toDate + ".csv";

                if ("REVENUE".equalsIgnoreCase(type)) {
                    csv.append("NGÀY,SỐ ĐƠN HÀNG,TIỀN HÀNG,GIẢM GIÁ VOUCHER,PHÍ SHIP,DOANH THU THỰC TẾ\n");
                    for (Map<String, Object> r : res.rows) {
                        csv.append(String.format("%s,%s,%.0f,%.0f,%.0f,%.0f\n",
                                r.get("date"), r.get("orderCount"),
                                (Double)r.get("subtotal"), (Double)r.get("discountAmount"),
                                (Double)r.get("shippingFee"), (Double)r.get("totalAmount")));
                    }
                } else if ("ORDERS".equalsIgnoreCase(type)) {
                    csv.append("MÃ ĐƠN HÀNG,TÀI KHOẢN,NGƯỜI NHẬN,SỐ ĐIỆN THOẠI,THANH TOÁN,TRẠNG THÁI,TIỀN HÀNG,GIẢM GIÁ,TỔNG TIỀN,NGÀY ĐẶT\n");
                    for (Map<String, Object> r : res.rows) {
                        csv.append(String.format("%s,%s,\"%s\",%s,\"%s\",\"%s\",%.0f,%.0f,%.0f,%s\n",
                                r.get("orderCode"), r.get("username"), r.get("recipientName"),
                                r.get("phone"), r.get("paymentMethod"), r.get("orderStatus"),
                                (Double)r.get("subtotal"), (Double)r.get("discountAmount"),
                                (Double)r.get("totalAmount"), r.get("createdAt")));
                    }
                } else if ("PRODUCTS".equalsIgnoreCase(type)) {
                    csv.append("MÃ SÁCH,TÊN SÁCH,DANH MỤC,ĐƠN GIÁ,SỐ LƯỢNG ĐÃ BÁN,DOANH THU,TỒN KHO HIỆN TẠI\n");
                    for (Map<String, Object> r : res.rows) {
                        csv.append(String.format("%s,\"%s\",\"%s\",%.0f,%s,%.0f,%s\n",
                                r.get("bookCode"), r.get("bookTitle"), r.get("category"),
                                (Double)r.get("price"), r.get("totalSold"),
                                (Double)r.get("totalRevenue"), r.get("currentStock")));
                    }
                } else if ("INVENTORY".equalsIgnoreCase(type)) {
                    csv.append("MÃ SÁCH,TÊN SÁCH,TÁC GIẢ,DANH MỤC,ĐƠN GIÁ,TỒN KHO,GIÁ TRỊ TỒN,VỊ TRÍ KHO,TRẠNG THÁI\n");
                    for (Map<String, Object> r : res.rows) {
                        csv.append(String.format("%s,\"%s\",\"%s\",\"%s\",%.0f,%s,%.0f,\"%s\",%s\n",
                                r.get("code"), r.get("title"), r.get("author"), r.get("category"),
                                (Double)r.get("price"), r.get("stock"), (Double)r.get("stockValue"),
                                r.get("warehouseLocation"), r.get("stockStatus")));
                    }
                } else if ("CUSTOMERS".equalsIgnoreCase(type)) {
                    csv.append("TÀI KHOẢN,HỌ VÀ TÊN,EMAIL,SỐ ĐIỆN THOẠI,TRẠNG THÁI,SỐ ĐƠN ĐẶT,TỔNG TIỀN ĐÃ CHI\n");
                    for (Map<String, Object> r : res.rows) {
                        csv.append(String.format("%s,\"%s\",%s,%s,%s,%s,%.0f\n",
                                r.get("username"), r.get("fullName"), r.get("email"),
                                r.get("phone"), r.get("status"), r.get("orderCount"),
                                (Double)r.get("totalSpent")));
                    }
                }

                byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Không thể tạo file báo cáo.\"}");
            }
        }
    }

    // =========================================================
    // UC13 - QUẢN LÝ NỘI DUNG CHATBOT AI (HANDLERS)
    // =========================================================

    /**
     * GET /api/admin/chatbot/content
     */
    static class ApiAdminChatbotContentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String category = "";
            String search = "";

            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                category = qp.getOrDefault("category", "");
                search = qp.getOrDefault("search", "");
            }

            List<ChatbotContent> items = DataStore.getAllChatbotContents(category, search);

            // Calculate category summary counts
            int total = 0;
            int countBookInfo = 0;
            int countBuyPolicy = 0;
            int countPayPolicy = 0;
            int countShipPolicy = 0;
            int countFaq = 0;

            List<ChatbotContent> allItems = DataStore.getAllChatbotContents("ALL", "");
            total = allItems.size();
            for (ChatbotContent c : allItems) {
                String cat = c.getCategoryType() != null ? c.getCategoryType().toUpperCase() : "";
                switch (cat) {
                    case "THONG_TIN_SACH": countBookInfo++; break;
                    case "CHINH_SACH_MUA_HANG": countBuyPolicy++; break;
                    case "CHINH_SACH_THANH_TOAN": countPayPolicy++; break;
                    case "CHINH_SACH_GIAO_HANG": countShipPolicy++; break;
                    case "CAU_HOI_THUONG_GAP": countFaq++; break;
                }
            }

            StringBuilder json = new StringBuilder("{");
            json.append("\"success\":true,");
            json.append("\"total\":").append(items.size()).append(",");
            json.append("\"summary\":{");
            json.append("\"total\":").append(total).append(",");
            json.append("\"bookInfo\":").append(countBookInfo).append(",");
            json.append("\"buyPolicy\":").append(countBuyPolicy).append(",");
            json.append("\"payPolicy\":").append(countPayPolicy).append(",");
            json.append("\"shipPolicy\":").append(countShipPolicy).append(",");
            json.append("\"faq\":").append(countFaq);
            json.append("},\"items\":[");

            for (int i = 0; i < items.size(); i++) {
                ChatbotContent item = items.get(i);
                if (i > 0) json.append(",");
                json.append(String.format(
                    "{\"id\":%d,\"categoryType\":%s,\"categoryTypeName\":%s,\"title\":%s,\"content\":%s,\"keywords\":%s,\"status\":%s,\"createdAt\":%s,\"updatedAt\":%s}",
                    item.getId(),
                    escapeJson(item.getCategoryType()),
                    escapeJson(item.getCategoryTypeName()),
                    escapeJson(item.getTitle()),
                    escapeJson(item.getContent()),
                    escapeJson(item.getKeywords() != null ? item.getKeywords() : ""),
                    escapeJson(item.getStatus()),
                    escapeJson(item.getCreatedAt() != null ? item.getCreatedAt() : ""),
                    escapeJson(item.getUpdatedAt() != null ? item.getUpdatedAt() : "")
                ));
            }
            json.append("]}");

            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }
    }

    /**
     * GET /api/admin/chatbot/content/detail?id=X
     */
    static class ApiAdminChatbotDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            int id = 0;
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                try { id = Integer.parseInt(qp.getOrDefault("id", "0")); } catch (Exception ignored) {}
            }

            ChatbotContent item = DataStore.getChatbotContentById(id);
            if (item == null) {
                sendResponse(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Nội dung Chatbot không tồn tại!\"}");
                return;
            }

            String json = String.format(
                "{\"success\":true,\"item\":{\"id\":%d,\"categoryType\":%s,\"categoryTypeName\":%s,\"title\":%s,\"content\":%s,\"keywords\":%s,\"status\":%s,\"createdAt\":%s,\"updatedAt\":%s}}",
                item.getId(),
                escapeJson(item.getCategoryType()),
                escapeJson(item.getCategoryTypeName()),
                escapeJson(item.getTitle()),
                escapeJson(item.getContent()),
                escapeJson(item.getKeywords() != null ? item.getKeywords() : ""),
                escapeJson(item.getStatus()),
                escapeJson(item.getCreatedAt() != null ? item.getCreatedAt() : ""),
                escapeJson(item.getUpdatedAt() != null ? item.getUpdatedAt() : "")
            );

            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    /**
     * POST /api/admin/chatbot/content/add
     */
    static class ApiAdminChatbotAddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            Map<String, String> params = parseFormData(exchange);
            String categoryType = params.getOrDefault("categoryType", "").trim();
            String title = params.getOrDefault("title", "").trim();
            String content = params.getOrDefault("content", "").trim();
            String keywords = params.getOrDefault("keywords", "").trim();
            String status = params.getOrDefault("status", "ACTIVE").trim();

            if (title.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tiêu đề không được để trống.\"}");
                return;
            }

            if (content.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Nội dung không được để trống.\"}");
                return;
            }

            if (categoryType.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Dữ liệu nhập không hợp lệ.\"}");
                return;
            }

            ChatbotContent item = new ChatbotContent(0, categoryType, title, content, keywords, status, null, null);
            String err = DataStore.addChatbotContent(item);
            if (err != null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", String.format("{\"success\":false,\"message\":%s}", escapeJson(err)));
                return;
            }

            sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Cập nhật nội dung Chatbot thành công.\"}");
        }
    }

    /**
     * POST /api/admin/chatbot/content/edit
     */
    static class ApiAdminChatbotEditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            Map<String, String> params = parseFormData(exchange);
            int id = 0;
            try { id = Integer.parseInt(params.getOrDefault("id", "0")); } catch (Exception ignored) {}

            String categoryType = params.getOrDefault("categoryType", "").trim();
            String title = params.getOrDefault("title", "").trim();
            String content = params.getOrDefault("content", "").trim();
            String keywords = params.getOrDefault("keywords", "").trim();
            String status = params.getOrDefault("status", "ACTIVE").trim();

            if (id <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Dữ liệu nhập không hợp lệ.\"}");
                return;
            }

            if (title.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Tiêu đề không được để trống.\"}");
                return;
            }

            if (content.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Nội dung không được để trống.\"}");
                return;
            }

            if (categoryType.isEmpty()) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Dữ liệu nhập không hợp lệ.\"}");
                return;
            }

            ChatbotContent item = new ChatbotContent(id, categoryType, title, content, keywords, status, null, null);
            String err = DataStore.updateChatbotContent(item);
            if (err != null) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", String.format("{\"success\":false,\"message\":%s}", escapeJson(err)));
                return;
            }

            sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Cập nhật nội dung Chatbot thành công.\"}");
        }
    }

    /**
     * POST /api/admin/chatbot/content/delete
     */
    static class ApiAdminChatbotDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            Map<String, String> params = parseFormData(exchange);
            int id = 0;
            try { id = Integer.parseInt(params.getOrDefault("id", "0")); } catch (Exception ignored) {}

            if (id <= 0) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Dữ liệu nhập không hợp lệ.\"}");
                return;
            }

            boolean ok = DataStore.deleteChatbotContent(id);
            if (!ok) {
                sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Nội dung Chatbot không tồn tại hoặc đã bị xóa!\"}");
                return;
            }

            sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Cập nhật nội dung Chatbot thành công.\"}");
        }
    }

    /**
     * POST /api/admin/chatbot/content/sync
     */
    static class ApiAdminChatbotSyncHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                sendResponse(exchange, 403, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này!\"}");
                return;
            }

            sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\":true,\"message\":\"Cập nhật nội dung Chatbot thành công.\"}");
        }
    }



    // =========================================================================
    // HELPER METHODS CHO PARSE JSON VÀ ORDER REQUEST
    // =========================================================================

    static class OrderItemReq {
        int bookId;
        int quantity;
        OrderItemReq(int bookId, int quantity) {
            this.bookId = bookId;
            this.quantity = quantity;
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String decodeJsonUnicode(String str) {
        if (str == null || !str.contains("\\u")) return str;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == '\\' && i + 5 < str.length() && str.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(str.substring(i + 2, i + 6), 16);
                    sb.append((char) code);
                    i += 6;
                    continue;
                } catch (Exception ignored) {}
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return decodeJsonUnicode(m.group(1));
        // Fallback for form-data style
        if (json.contains(key + "=")) {
            Map<String, String> qp = parseQueryString(json);
            return decodeJsonUnicode(qp.get(key));
        }
        return null;
    }

    private static int extractJsonInt(String json, String key, int def) {
        if (json == null) return def;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        if (json.contains(key + "=")) {
            Map<String, String> qp = parseQueryString(json);
            if (qp.containsKey(key)) {
                try { return Integer.parseInt(qp.get(key)); } catch (Exception ignored) {}
            }
        }
        return def;
    }

    private static double extractJsonDouble(String json, String key, double def) {
        if (json == null) return def;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
        }
        if (json.contains(key + "=")) {
            Map<String, String> qp = parseQueryString(json);
            if (qp.containsKey(key)) {
                try { return Double.parseDouble(qp.get(key)); } catch (Exception ignored) {}
            }
        }
        return def;
    }

    private static boolean extractJsonBoolean(String json, String key, boolean def) {
        if (json == null) return def;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(json);
        if (m.find()) return Boolean.parseBoolean(m.group(1));
        return def;
    }

    private static List<OrderItemReq> extractOrderItems(String json) {
        List<OrderItemReq> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;

        Pattern p1 = Pattern.compile("\\{[^{}]*?\"(?:bookId|id)\"\\s*:\\s*(\\d+)[^{}]*?\"quantity\"\\s*:\\s*(\\d+)[^{}]*?\\}");
        Matcher m1 = p1.matcher(json);
        while (m1.find()) {
            try {
                int bookId = Integer.parseInt(m1.group(1));
                int qty = Integer.parseInt(m1.group(2));
                if (qty > 0) list.add(new OrderItemReq(bookId, qty));
            } catch (Exception ignored) {}
        }

        if (list.isEmpty()) {
            Pattern p2 = Pattern.compile("\\{[^{}]*?\"quantity\"\\s*:\\s*(\\d+)[^{}]*?\"(?:bookId|id)\"\\s*:\\s*(\\d+)[^{}]*?\\}");
            Matcher m2 = p2.matcher(json);
            while (m2.find()) {
                try {
                    int qty = Integer.parseInt(m2.group(1));
                    int bookId = Integer.parseInt(m2.group(2));
                    if (qty > 0) list.add(new OrderItemReq(bookId, qty));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    /**
     * Xử lý hiển thị trang Giới thiệu (/about)
     */
    static class AboutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            String html = renderContentPage("about.html", user);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * Xử lý hiển thị trang Liên hệ (/contact)
     */
    static class ContactHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            String html = renderContentPage("contact.html", user);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * Xử lý hiển thị trang Khuyến mãi (/promotions)
     */
    static class PromotionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User user = getAuthenticatedUser(exchange);
            String html = renderContentPage("promotions.html", user);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * Xử lý hiển thị trang Danh Mục Sách riêng biệt (/category) theo layout ảnh mẫu 2
     */
    static class CategoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/category/css/") || path.startsWith("/category/js/") || path.startsWith("/category/images/")) {
                new StaticFileHandler().handle(exchange);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String category = "Tất cả";
            String sort = "newest";
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("name") && !qp.get("name").trim().isEmpty()) {
                    category = qp.get("name").trim();
                } else if (qp.containsKey("category") && !qp.get("category").trim().isEmpty()) {
                    category = qp.get("category").trim();
                }
                if (qp.containsKey("sort") && !qp.get("sort").trim().isEmpty()) {
                    sort = qp.get("sort").trim();
                }
            }

            User user = getAuthenticatedUser(exchange);
            String html = renderCategoryPage(user, category, sort);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * Xử lý hiển thị trang Chi tiết sách (/book)
     */
    static class BookDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/book/css/") || path.startsWith("/book/js/") || path.startsWith("/book/images/")) {
                new StaticFileHandler().handle(exchange);
                return;
            }

            int bookId = 25; // Mặc định hiển thị sách Tháo Dây Oan Trái
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("id")) {
                    try {
                        bookId = Integer.parseInt(qp.get("id").trim());
                    } catch (NumberFormatException ignored) {}
                }
            }

            User user = getAuthenticatedUser(exchange);
            String html = renderBookDetailPage(user, bookId);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    /**
     * API trả về danh sách sách dưới dạng JSON (hỗ trợ tìm kiếm & bộ lọc đa tiêu chí)
     */
    static class ApiBooksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String keyword = "";
            String category = "Tất cả";
            String author = "Tất cả";
            String publisher = "Tất cả";
            String stockStatus = "all";
            Double minPrice = null;
            Double maxPrice = null;

            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("q")) keyword = qp.get("q");
                if (qp.containsKey("category")) category = qp.get("category");
                if (qp.containsKey("author")) author = qp.get("author");
                if (qp.containsKey("publisher")) publisher = qp.get("publisher");
                if (qp.containsKey("stockStatus")) stockStatus = qp.get("stockStatus");
                if (qp.containsKey("minPrice")) {
                    try { minPrice = Double.parseDouble(qp.get("minPrice")); } catch (NumberFormatException ignored) {}
                }
                if (qp.containsKey("maxPrice")) {
                    try { maxPrice = Double.parseDouble(qp.get("maxPrice")); } catch (NumberFormatException ignored) {}
                }
            }

            List<Book> books = DataStore.searchBooks(keyword, category, author, publisher, minPrice, maxPrice, stockStatus);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                        "{\"id\":%d,\"code\":%s,\"title\":%s,\"author\":%s,\"publisher\":%s,\"price\":%.0f,\"originalPrice\":%.0f,\"formattedPrice\":%s,\"formattedOriginalPrice\":%s,\"discountPercent\":%d,\"category\":%s,\"stock\":%d,\"stockStatus\":%s,\"isOutOfStock\":%b,\"rating\":%.1f,\"reviewCount\":%d,\"image\":%s,\"description\":%s,\"promotion\":%s,\"isBestSeller\":%b}",
                        b.getId(),
                        escapeJson(b.getCode()),
                        escapeJson(b.getTitle()),
                        escapeJson(b.getAuthor()),
                        escapeJson(b.getPublisher()),
                        b.getPrice(),
                        b.getOriginalPrice(),
                        escapeJson(b.getFormattedPrice()),
                        escapeJson(b.getFormattedOriginalPrice()),
                        b.getDiscountPercent(),
                        escapeJson(b.getCategory()),
                        b.getStock(),
                        escapeJson(b.getStockStatusText()),
                        b.isOutOfStock(),
                        b.getRating(),
                        b.getReviewCount(),
                        escapeJson(b.getImage()),
                        escapeJson(b.getDescription()),
                        escapeJson(b.getPromotion()),
                        b.isBestSeller()
                ));
            }
            sb.append("]");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", sb.toString());
        }
    }

    /**
     * API gợi ý tìm kiếm tức thì theo Use Case Luồng cơ bản (1):
     * Gợi ý từ SACH, DANH_MUC, TAC_GIA và thông tin khuyến mãi KHUYEN_MAI
     */
    static class ApiSuggestionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String keyword = "";
            if (query != null) {
                Map<String, String> qp = parseQueryString(query);
                if (qp.containsKey("q")) keyword = qp.get("q");
            }

            DataStore.SearchSuggestionResult res = DataStore.getSearchSuggestions(keyword);

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"totalMatches\":").append(res.getTotalMatches()).append(",");
            // 1. Books
            sb.append("\"books\":[");
            for (int i = 0; i < res.getBooks().size(); i++) {
                Book b = res.getBooks().get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                        "{\"id\":%d,\"code\":%s,\"title\":%s,\"author\":%s,\"publisher\":%s,\"price\":%.0f,\"formattedPrice\":%s,\"originalPrice\":%.0f,\"formattedOriginalPrice\":%s,\"category\":%s,\"stock\":%d,\"stockStatus\":%s,\"isOutOfStock\":%b,\"image\":%s,\"promotion\":%s}",
                        b.getId(),
                        escapeJson(b.getCode()),
                        escapeJson(b.getTitle()),
                        escapeJson(b.getAuthor()),
                        escapeJson(b.getPublisher()),
                        b.getPrice(),
                        escapeJson(b.getFormattedPrice()),
                        b.getOriginalPrice(),
                        escapeJson(b.getFormattedOriginalPrice()),
                        escapeJson(b.getCategory()),
                        b.getStock(),
                        escapeJson(b.getStockStatusText()),
                        b.isOutOfStock(),
                        escapeJson(b.getImage()),
                        escapeJson(b.getPromotion())
                ));
            }
            sb.append("],");

            // 2. Categories
            sb.append("\"categories\":[");
            for (int i = 0; i < res.getCategories().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(escapeJson(res.getCategories().get(i)));
            }
            sb.append("],");

            // 3. Authors
            sb.append("\"authors\":[");
            for (int i = 0; i < res.getAuthors().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(escapeJson(res.getAuthors().get(i)));
            }
            sb.append("],");

            // 4. Promotions
            sb.append("\"promotions\":[");
            for (int i = 0; i < res.getPromotions().size(); i++) {
                DataStore.PromotionItem p = res.getPromotions().get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                        "{\"code\":%s,\"title\":%s,\"category\":%s,\"description\":%s}",
                        escapeJson(p.getCode()),
                        escapeJson(p.getTitle()),
                        escapeJson(p.getApplicableCategory()),
                        escapeJson(p.getDescription())
                ));
            }
            sb.append("]}");

            sendResponse(exchange, 200, "application/json; charset=UTF-8", sb.toString());
        }
    }

    /**
     * API Chatbot AI tư vấn sách thông minh theo Use Case Luồng rẽ nhánh (2a.2)
     */
    static class ApiChatbotHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String message = "";
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                message = params.getOrDefault("message", "").trim();
            } else {
                String query = exchange.getRequestURI().getQuery();
                if (query != null) {
                    Map<String, String> qp = parseQueryString(query);
                    if (qp.containsKey("message")) message = qp.get("message").trim();
                    if (qp.containsKey("msg")) message = qp.get("msg").trim();
                }
            }

            String lowerMsg = message.toLowerCase();
            String reply;
            List<Book> suggestedBooks = new ArrayList<>();
            List<Book> all = DataStore.getAllBooks();

            String kbAnswer = DataStore.findChatbotAnswer(message);

            if (lowerMsg.isEmpty() || lowerMsg.contains("chào") || lowerMsg.contains("hello") || lowerMsg.contains("hi")) {
                reply = "Xin chào bạn! Tôi là Trợ lý AI Bookora 📚. Tôi có thể giúp bạn tìm kiếm sách theo sở thích, giải đáp chính sách giao hàng, thanh toán, đổi trả hoặc giới thiệu các tác phẩm nổi bật. Bạn cần hỗ trợ gì hôm nay?";
                for (Book b : all) {
                    if (b.isBestSeller() && suggestedBooks.size() < 3) suggestedBooks.add(b);
                }
            } else if (kbAnswer != null && !kbAnswer.trim().isEmpty()) {
                reply = kbAnswer;
                for (Book b : all) {
                    if (b.isBestSeller() && suggestedBooks.size() < 3) suggestedBooks.add(b);
                }
            } else if (lowerMsg.contains("lập trình") || lowerMsg.contains("công nghệ") || lowerMsg.contains("code") || lowerMsg.contains("software")) {
                reply = "Dành cho dân công nghệ & lập trình viên, Bookora có các cẩm nang kinh điển của Uncle Bob và các bậc thầy thế giới! Đặc biệt tháng này đang có ưu đãi 25% cho danh mục Công nghệ:";
                for (Book b : all) {
                    if ("Công nghệ".equalsIgnoreCase(b.getCategory()) && suggestedBooks.size() < 3) suggestedBooks.add(b);
                }
            } else if (lowerMsg.contains("kinh tế") || lowerMsg.contains("tài chính") || lowerMsg.contains("làm giàu") || lowerMsg.contains("tiền")) {
                reply = "Nếu bạn muốn nâng cao tư duy tài chính độc lập và phương pháp quản trị doanh nghiệp, đây là các tác phẩm được hàng triệu độc giả đánh giá cao nhất:";
                for (Book b : all) {
                    if ("Kinh tế".equalsIgnoreCase(b.getCategory()) && suggestedBooks.size() < 3) suggestedBooks.add(b);
                }
            } else if (lowerMsg.contains("văn học") || lowerMsg.contains("tiểu thuyết") || lowerMsg.contains("truyện")) {
                reply = "Về mảng Văn học, Bookora tuyển chọn những kiệt tác văn chương lay động lòng người, với ưu đãi giảm 20% mùa thu này:";
                for (Book b : all) {
                    if ("Văn học".equalsIgnoreCase(b.getCategory()) && suggestedBooks.size() < 3) suggestedBooks.add(b);
                }
            } else if (lowerMsg.contains("kỹ năng") || lowerMsg.contains("thói quen") || lowerMsg.contains("phát triển bản thân")) {
                reply = "Để phát triển bản thân và rèn luyện thói quen tích cực mỗi ngày, tôi đặc biệt gợi ý cho bạn những cuốn sách gối đầu giường sau:";
                for (Book b : all) {
                    if ("Kỹ năng sống".equalsIgnoreCase(b.getCategory()) && suggestedBooks.size() < 3) suggestedBooks.add(b);
                }
            } else if (lowerMsg.contains("tâm lý") || lowerMsg.contains("tư duy")) {
                reply = "Khám phá chiều sâu nội tâm và cách vận hành của tư duy con người qua những cuốn sách tâm lý học xuất sắc:";
                for (Book b : all) {
                    if ("Tâm lý học".equalsIgnoreCase(b.getCategory()) && suggestedBooks.size() < 3) suggestedBooks.add(b);
                }
            } else if (lowerMsg.contains("hết hàng") || lowerMsg.contains("tồn kho") || lowerMsg.contains("kho")) {
                reply = "Hệ thống Bookora kiểm tra kho hàng theo thời gian thực (Real-time Inventory). Nếu cuốn sách hiển thị nhãn 'Hết hàng', bạn có thể bấm 'Xem chi tiết' để theo dõi hoặc nhận thông báo ngay khi sách được tái bản về kho!";
                for (Book b : all) {
                    if (b.isOutOfStock() && suggestedBooks.size() < 3) suggestedBooks.add(b);
                }
            } else if (lowerMsg.contains("khuyến mãi") || lowerMsg.contains("giảm giá") || lowerMsg.contains("voucher") || lowerMsg.contains("freeship")) {
                reply = "Hiện tại Bookora đang áp dụng các chương trình ưu đãi nổi bật: Giảm 20% sách Văn học (KM_VANHOC), Ưu đãi 25% sách Công nghệ (KM_TECH2026), và Miễn phí vận chuyển cho đơn hàng từ 250.000 đ!";
                for (Book b : all) {
                    if (b.getDiscountPercent() > 0 && suggestedBooks.size() < 3) suggestedBooks.add(b);
                }
            } else {
                // Tìm kiếm theo từ khóa người dùng nhập vào
                List<Book> matches = DataStore.searchBooks(message, "Tất cả");
                if (!matches.isEmpty()) {
                    reply = "Tôi đã tìm thấy " + matches.size() + " cuốn sách phù hợp với yêu cầu '" + message + "' của bạn:";
                    for (int i = 0; i < Math.min(3, matches.size()); i++) {
                        suggestedBooks.add(matches.get(i));
                    }
                } else {
                    reply = "Rất tiếc tôi chưa tìm thấy đầu sách nào khớp hoàn toàn với '" + message + "'. Tuy nhiên, bạn có thể tham khảo một số tác phẩm kinh điển đang được bạn đọc săn đón nhiều nhất tại Bookora:";
                    for (Book b : all) {
                        if (b.isBestSeller() && suggestedBooks.size() < 3) suggestedBooks.add(b);
                    }
                }
            }

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"reply\":").append(escapeJson(reply)).append(",");
            sb.append("\"books\":[");
            for (int i = 0; i < suggestedBooks.size(); i++) {
                Book b = suggestedBooks.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                        "{\"id\":%d,\"code\":%s,\"title\":%s,\"author\":%s,\"publisher\":%s,\"price\":%.0f,\"formattedPrice\":%s,\"category\":%s,\"stock\":%d,\"stockStatus\":%s,\"isOutOfStock\":%b,\"image\":%s}",
                        b.getId(),
                        escapeJson(b.getCode()),
                        escapeJson(b.getTitle()),
                        escapeJson(b.getAuthor()),
                        escapeJson(b.getPublisher()),
                        b.getPrice(),
                        escapeJson(b.getFormattedPrice()),
                        escapeJson(b.getCategory()),
                        b.getStock(),
                        escapeJson(b.getStockStatusText()),
                        b.isOutOfStock(),
                        escapeJson(b.getImage())
                ));
            }
            sb.append("],\"recommendations\":[");
            for (int i = 0; i < suggestedBooks.size(); i++) {
                Book b = suggestedBooks.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                        "{\"id\":%d,\"code\":%s,\"title\":%s,\"author\":%s,\"publisher\":%s,\"price\":%.0f,\"formattedPrice\":%s,\"category\":%s,\"stock\":%d,\"stockStatus\":%s,\"isOutOfStock\":%b,\"image\":%s}",
                        b.getId(),
                        escapeJson(b.getCode()),
                        escapeJson(b.getTitle()),
                        escapeJson(b.getAuthor()),
                        escapeJson(b.getPublisher()),
                        b.getPrice(),
                        escapeJson(b.getFormattedPrice()),
                        escapeJson(b.getCategory()),
                        b.getStock(),
                        escapeJson(b.getStockStatusText()),
                        b.isOutOfStock(),
                        escapeJson(b.getImage())
                ));
            }
            sb.append("]}");

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
            if (path.startsWith("home/")) path = path.substring(5);
            if (path.startsWith("category/")) path = path.substring(9);
            if (path.startsWith("book/")) path = path.substring(5);
            if (path.startsWith("cart/")) path = path.substring(5);
            if (path.startsWith("login/")) path = path.substring(6);
            if (path.startsWith("register/")) path = path.substring(9);
            if (path.startsWith("profile/")) path = path.substring(8);
            Path filePath = WEBAPP_DIR.resolve(path);

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String mime = getMimeType(filePath.toString());
                byte[] bytes = Files.readAllBytes(filePath);
                exchange.getResponseHeaders().set("Content-Type", mime);
                exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
                exchange.getResponseHeaders().set("Pragma", "no-cache");
                exchange.getResponseHeaders().set("Expires", "0");
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

    private static String renderProfilePage(User user, String activeTab, String success, String error) {
        Path templatePath = WEBAPP_DIR.resolve("profile.html");
        String content = "";
        try {
            content = Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            content = "<h1>Profile Template Missing</h1>";
        }

        String errorHtml = (error != null && !error.isEmpty()) 
                ? "<div class=\"alert alert-danger\"><i class=\"fas fa-exclamation-circle\"></i> " + escapeHtml(error) + "</div>" 
                : "";
        String successHtml = (success != null && !success.isEmpty()) 
                ? "<div class=\"alert alert-success\"><i class=\"fas fa-check-circle\"></i> " + escapeHtml(success) + "</div>" 
                : "";

        content = content.replace("<!-- ${ALERT_MESSAGE} -->", errorHtml + successHtml);
        content = content.replace("${user.username}", escapeAttr(user.getUsername()));
        content = content.replace("${user.fullName}", escapeAttr(user.getFullName()));
        content = content.replace("${user.email}", escapeAttr(user.getEmail() != null ? user.getEmail() : ""));
        content = content.replace("${user.phone}", escapeAttr(user.getPhone() != null ? user.getPhone() : ""));
        content = content.replace("${user.role}", escapeAttr(user.getRole()));
        content = content.replace("${user.avatar}", escapeAttr(user.getAvatar() != null ? user.getAvatar() : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"));
        content = content.replace("${activeTab}", (activeTab != null && !activeTab.isEmpty()) ? activeTab : "profile");

        return content;
    }

    private static String renderCartPage(User user) {
        Path templatePath = WEBAPP_DIR.resolve("cart.html");
        String content = "";
        try {
            content = Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            content = "<h1>Cart Template Missing</h1>";
        }

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
                    "        <a href=\"profile\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-user-circle\"></i> Hồ sơ tài khoản\n" +
                    "        </a>\n" +
                    "        <a href=\"cart\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-bag-shopping\"></i> Giỏ hàng của tôi\n" +
                    "        </a>\n" +
                    "        <a href=\"profile#orders\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-box-open\"></i> Đơn hàng của tôi\n" +
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
                    escapeAttr(user.getEmail() != null ? user.getEmail() : "")
            );

            content = content.replace("<!-- ${TOPBAR_AUTH} -->", topbarAuth);
            content = content.replace("<!-- ${HEADER_AUTH} -->", headerAuth);
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
            content = content.replace("${user.fullName}", "Khách");
            content = content.replace("${user.username}", "guest");
            content = content.replace("${user.role}", "GUEST");
            content = content.replace("${user.avatar}", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
            content = content.replace("${user.email}", "");
        }

        content = content.replace("${searchKeyword}", "");
        return content;
    }

    private static String renderContentPage(String templateFileName, User user) {
        Path templatePath = WEBAPP_DIR.resolve(templateFileName);
        String content = "";
        try {
            content = Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            content = "<h1>Template Missing: " + templateFileName + "</h1>";
        }

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
                    "        <a href=\"profile\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-user-circle\"></i> Hồ sơ tài khoản\n" +
                    "        </a>\n" +
                    "        <a href=\"cart\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-bag-shopping\"></i> Giỏ hàng của tôi\n" +
                    "        </a>\n" +
                    "        <a href=\"profile#orders\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-box-open\"></i> Đơn hàng của tôi\n" +
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
                    escapeAttr(user.getEmail() != null ? user.getEmail() : "")
            );

            content = content.replace("<!-- ${TOPBAR_AUTH} -->", topbarAuth);
            content = content.replace("<!-- ${HEADER_AUTH} -->", headerAuth);
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
            content = content.replace("${user.fullName}", "Khách");
            content = content.replace("${user.username}", "guest");
            content = content.replace("${user.role}", "GUEST");
            content = content.replace("${user.avatar}", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
            content = content.replace("${user.email}", "");
        }

        content = content.replace("${searchKeyword}", "");
        return content;
    }

    private static String renderBookDetailPage(User user, int bookId) {
        Path templatePath = WEBAPP_DIR.resolve("book.html");
        String content = "";
        try {
            content = Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            content = "<h1>Book Detail Template Missing</h1>";
        }

        Book book = DataStore.getBookById(bookId);
        if (book == null) {
            book = DataStore.getBookById(25);
        }
        if (book == null) {
            List<Book> all = DataStore.getAllBooks();
            if (!all.isEmpty()) book = all.get(0);
        }

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
                    "        <a href=\"profile\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-user-circle\"></i> Hồ sơ tài khoản\n" +
                    "        </a>\n" +
                    "        <a href=\"cart\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-bag-shopping\"></i> Giỏ hàng của tôi\n" +
                    "        </a>\n" +
                    "        <a href=\"profile#orders\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-box-open\"></i> Đơn hàng của tôi\n" +
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
                    escapeAttr(user.getEmail() != null ? user.getEmail() : "")
            );

            content = content.replace("<!-- ${TOPBAR_AUTH} -->", topbarAuth);
            content = content.replace("<!-- ${HEADER_AUTH} -->", headerAuth);
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
            content = content.replace("${user.fullName}", "Khách");
            content = content.replace("${user.username}", "guest");
            content = content.replace("${user.role}", "GUEST");
            content = content.replace("${user.avatar}", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
            content = content.replace("${user.email}", "");
        }

        if (book != null) {
            content = content.replace("${book.id}", String.valueOf(book.getId()));
            content = content.replace("${book.title}", escapeHtml(book.getTitle()));
            content = content.replace("${book.code}", escapeHtml(book.getCode()));
            content = content.replace("${book.author}", escapeHtml(book.getAuthor()));
            content = content.replace("${book.publisher}", escapeHtml(book.getPublisher()));
            String translator = "Đang cập nhật";
            if ("8935278601425".equals(book.getCode()) || book.getId() == 25) {
                translator = "Nguyễn Văn Tuấn";
            } else if (book.getAuthor() != null && (book.getAuthor().contains("Uncle Bob") || book.getAuthor().contains("Dale Carnegie") || book.getAuthor().contains("Paulo Coelho"))) {
                translator = "Nhiều dịch giả";
            }
            content = content.replace("${book.translator}", escapeHtml(translator));
            content = content.replace("${book.publishYear}", String.valueOf(book.getPublishYear()));
            content = content.replace("${book.pageCount}", String.valueOf(book.getPageCount()));
            content = content.replace("${book.weight}", String.valueOf(book.getWeight()));
            content = content.replace("${book.dimensions}", escapeHtml(book.getDimensions()));
            content = content.replace("${book.coverFormat}", escapeHtml(book.getCoverFormat()));
            content = content.replace("${book.price}", String.format(Locale.US, "%.0f", book.getPrice()));
            content = content.replace("${book.originalPrice}", String.format(Locale.US, "%.0f", book.getOriginalPrice()));
            content = content.replace("${book.formattedPrice}", escapeHtml(book.getFormattedPrice()));
            content = content.replace("${book.formattedOriginalPrice}", escapeHtml(book.getFormattedOriginalPrice()));
            content = content.replace("${book.category}", escapeHtml(book.getCategory()));
            content = content.replace("${book.image}", escapeAttr(book.getImage()));
            content = content.replace("${book.description}", book.getDescription() != null ? escapeHtml(book.getDescription()) : "");
            content = content.replace("${book.stock}", String.valueOf(book.getStock()));
            content = content.replace("${book.stockStatusClass}", book.isOutOfStock() ? "out-of-stock" : "in-stock");
            content = content.replace("${book.stockStatusText}", escapeHtml(book.getStockStatusText()));

            if (book.getOriginalPrice() > book.getPrice()) {
                String origPriceHtml = String.format(
                        "<span class=\"detail-price-original\">%s</span>\n<span class=\"detail-discount-badge\">-%d%%</span>",
                        escapeHtml(book.getFormattedOriginalPrice()),
                        book.getDiscountPercent()
                );
                content = content.replace("<!-- ${DETAIL_ORIGINAL_PRICE_HTML} -->", origPriceHtml);
            } else {
                content = content.replace("<!-- ${DETAIL_ORIGINAL_PRICE_HTML} -->", "");
            }

            // SẢN PHẨM NỔI BẬT (Ảnh 2)
            List<Book> featuredBooks = DataStore.getFeaturedBooks(4);
            StringBuilder fbHtml = new StringBuilder();
            for (Book fb : featuredBooks) {
                String badgeHtml = "";
                if (fb.isOutOfStock()) {
                    badgeHtml = "<span class=\"featured-badge badge-black\">Hết hàng</span>";
                } else if (fb.getDiscountPercent() > 0) {
                    badgeHtml = "<span class=\"featured-badge badge-red\">-" + fb.getDiscountPercent() + "%</span>";
                }
                String origPriceHtml = fb.getOriginalPrice() > fb.getPrice()
                        ? "<span class=\"featured-price-orig\">" + escapeHtml(fb.getFormattedOriginalPrice()) + "</span>"
                        : "";

                fbHtml.append(String.format(
                        "<div class=\"featured-book-item\" onclick=\"window.location.href='book?id=%d'\">\n" +
                        "    <div class=\"featured-thumb-wrap\">\n" +
                        "        <img src=\"%s\" alt=\"%s\" class=\"featured-thumb\" onerror=\"this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';\">\n" +
                        "        %s\n" +
                        "    </div>\n" +
                        "    <div class=\"featured-meta\">\n" +
                        "        <h4 class=\"featured-book-title\" title=\"%s\">%s</h4>\n" +
                        "        <div class=\"featured-price-row\">\n" +
                        "            <span class=\"featured-price-red\">%s</span>\n" +
                        "            %s\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "</div>\n",
                        fb.getId(),
                        escapeAttr(fb.getImage()),
                        escapeAttr(fb.getTitle()),
                        badgeHtml,
                        escapeAttr(fb.getTitle()),
                        escapeHtml(fb.getTitle()),
                        escapeHtml(fb.getFormattedPrice()),
                        origPriceHtml
                ));
            }
            content = content.replace("<!-- ${FEATURED_PRODUCTS_HTML} -->", fbHtml.toString());

            // SẢN PHẨM LIÊN QUAN (Ảnh 3 - Đồng bộ chuẩn form thẻ sách với Trang chủ và Danh mục)
            List<Book> relatedBooks = DataStore.getRelatedBooks(book.getId(), book.getCategory(), 4);
            StringBuilder rbHtml = new StringBuilder();
            for (Book rb : relatedBooks) {
                String bestsellerBadge = rb.isBestSeller() ? "<span class=\"badge-tag badge-bestseller\">Bán chạy</span>" : "";
                String discountBadge = (rb.getDiscountPercent() > 0) 
                        ? "<span class=\"badge-tag badge-discount\">-" + rb.getDiscountPercent() + "%</span>" 
                        : "";
                String originalPriceHtml = (rb.getOriginalPrice() > 0 && rb.getOriginalPrice() > rb.getPrice()) 
                        ? "<span class=\"price-original\">" + rb.getFormattedOriginalPrice() + "</span>" 
                        : "";

                boolean outOfStock = rb.isOutOfStock();
                String stockBadge = outOfStock
                        ? "<span class=\"badge-tag badge-stock badge-outofstock\"><i class=\"fas fa-ban\"></i> Hết hàng</span>"
                        : "<span class=\"badge-tag badge-stock badge-instock\"><i class=\"fas fa-check\"></i> Còn " + rb.getStock() + "</span>";

                String cartButtonHtml = outOfStock
                        ? String.format("<button type=\"button\" class=\"btn-add-cart disabled\" onclick=\"notifyOutOfStock('%s')\" title=\"Sách đã hết hàng trong kho\"><i class=\"fas fa-bell\"></i></button>", escapeAttr(rb.getTitle()))
                        : String.format("<button type=\"button\" class=\"btn-add-cart\" onclick=\"addToCart(%d)\" title=\"Thêm vào giỏ\"><i class=\"fas fa-cart-plus\"></i></button>", rb.getId());

                String cardClasses = outOfStock ? "book-card is-out-of-stock" : "book-card";

                rbHtml.append(String.format(
                        "<div class=\"%s\" data-id=\"%d\" data-code=\"%s\" data-title=\"%s\" data-author=\"%s\" data-publisher=\"%s\" data-price=\"%.0f\" data-original-price=\"%.0f\" data-formatted-price=\"%s\" data-category=\"%s\" data-stock=\"%d\" data-is-out-of-stock=\"%b\" data-promotion=\"%s\" data-image=\"%s\" data-desc=\"%s\" data-rating=\"%.1f\">\n" +
                        "    <div class=\"book-card-inner\">\n" +
                        "        <div class=\"book-cover-wrap\">\n" +
                        "            <img src=\"%s\" alt=\"%s\" class=\"book-cover\" loading=\"lazy\" onclick=\"goToBookDetail(%d)\" style=\"cursor: pointer;\" onerror=\"this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';\">\n" +
                        "            <div class=\"badge-container\">%s%s%s</div>\n" +
                        "            <div class=\"book-actions-overlay\">\n" +
                        "                <button type=\"button\" class=\"btn-quickview\" onclick=\"goToBookDetail(%d)\"><i class=\"fas fa-eye\"></i> Xem chi tiết</button>\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "        <div class=\"book-info\">\n" +
                        "            <div class=\"book-meta-top\">\n" +
                        "                <span class=\"book-category\">%s</span>\n" +
                        "                <span class=\"book-code\" title=\"Mã sách: %s\"><i class=\"fas fa-barcode\"></i> %s</span>\n" +
                        "            </div>\n" +
                        "            <h3 class=\"book-title\" title=\"%s\" onclick=\"goToBookDetail(%d)\" style=\"cursor: pointer;\">%s</h3>\n" +
                        "            <p class=\"book-author\" title=\"Tác giả\"><i class=\"fas fa-feather-alt\"></i> %s</p>\n" +
                        "            <p class=\"book-publisher\" title=\"Nhà xuất bản\"><i class=\"fas fa-building-columns\"></i> %s</p>\n" +
                        "            <div class=\"book-rating-row\">\n" +
                        "                <div class=\"stars\"><i class=\"fas fa-star\"></i> <span>%.1f</span></div>\n" +
                        "                <span class=\"review-count\">(%d đánh giá)</span>\n" +
                        "                <span class=\"stock-pill %s\">%s</span>\n" +
                        "            </div>\n" +
                        "            <div class=\"book-price-row\">\n" +
                        "                <div class=\"price-box\">\n" +
                        "                    <span class=\"price-current\">%s</span>\n" +
                        "                    %s\n" +
                        "                </div>\n" +
                        "                %s\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "</div>\n",
                        cardClasses,
                        rb.getId(),
                        escapeAttr(rb.getCode()),
                        escapeAttr(rb.getTitle()),
                        escapeAttr(rb.getAuthor()),
                        escapeAttr(rb.getPublisher()),
                        rb.getPrice(),
                        rb.getOriginalPrice(),
                        escapeAttr(rb.getFormattedPrice()),
                        escapeAttr(rb.getCategory()),
                        rb.getStock(),
                        rb.isOutOfStock(),
                        escapeAttr(rb.getPromotion()),
                        escapeAttr(rb.getImage()),
                        escapeAttr(rb.getDescription()),
                        rb.getRating(),
                        escapeAttr(rb.getImage()),
                        escapeAttr(rb.getTitle()),
                        rb.getId(),
                        stockBadge,
                        bestsellerBadge,
                        discountBadge,
                        rb.getId(),
                        escapeHtml(rb.getCategory()),
                        escapeAttr(rb.getCode()),
                        escapeHtml(rb.getCode()),
                        escapeAttr(rb.getTitle()),
                        rb.getId(),
                        escapeHtml(rb.getTitle()),
                        escapeHtml(rb.getAuthor()),
                        escapeHtml(rb.getPublisher()),
                        rb.getRating(),
                        rb.getReviewCount(),
                        rb.isOutOfStock() ? "stock-out" : "stock-in",
                        escapeHtml(rb.getStockStatusText()),
                        escapeHtml(rb.getFormattedPrice()),
                        originalPriceHtml,
                        cartButtonHtml
                ));
            }
            content = content.replace("<!-- ${RELATED_PRODUCTS_HTML} -->", rbHtml.toString());
        }

        content = content.replace("${searchKeyword}", "");
        return content;
    }

    private static String renderCategoryPage(User user, String categoryName, String sortOrder) {
        Path templatePath = WEBAPP_DIR.resolve("category.html");
        String content = "";
        try {
            content = Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            content = "<h1>Category Template Missing</h1>";
        }

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
                    "        <a href=\"profile\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-user-circle\"></i> Hồ sơ tài khoản\n" +
                    "        </a>\n" +
                    "        <a href=\"cart\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-bag-shopping\"></i> Giỏ hàng của tôi\n" +
                    "        </a>\n" +
                    "        <a href=\"profile#orders\" class=\"dropdown-item\">\n" +
                    "            <i class=\"fas fa-box-open\"></i> Đơn hàng của tôi\n" +
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
                    escapeAttr(user.getEmail() != null ? user.getEmail() : "")
            );

            content = content.replace("<!-- ${TOPBAR_AUTH} -->", topbarAuth);
            content = content.replace("<!-- ${HEADER_AUTH} -->", headerAuth);
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
            content = content.replace("${user.fullName}", "Khách");
            content = content.replace("${user.username}", "guest");
            content = content.replace("${user.role}", "GUEST");
            content = content.replace("${user.avatar}", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
            content = content.replace("${user.email}", "");
        }

        List<Book> books;
        if ("Tất cả".equalsIgnoreCase(categoryName) || "Tất cả sách".equalsIgnoreCase(categoryName)) {
            books = new ArrayList<>(DataStore.getAllBooks());
        } else {
            books = DataStore.searchBooks("", categoryName, null, null, null, null, "all");
        }

        // Sorting
        if ("bestseller".equalsIgnoreCase(sortOrder)) {
            books.sort((a, b) -> Boolean.compare(b.isBestSeller(), a.isBestSeller()));
        } else if ("price_asc".equalsIgnoreCase(sortOrder)) {
            books.sort(Comparator.comparingDouble(Book::getPrice));
        } else if ("price_desc".equalsIgnoreCase(sortOrder)) {
            books.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
        } else if ("rating".equalsIgnoreCase(sortOrder)) {
            books.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        } else {
            books.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        }

        StringBuilder gridHtml = new StringBuilder();
        if (books.isEmpty()) {
            gridHtml.append(
                    "<div class=\"no-books-found\" style=\"grid-column: 1 / -1;\">\n" +
                    "    <div class=\"empty-icon-wrap\"><i class=\"fas fa-book-open\"></i></div>\n" +
                    "    <h3 class=\"empty-title\">Chưa có sách nào trong danh mục này</h3>\n" +
                    "    <p style=\"color: #64748b; font-size: 14px; margin-top: 6px;\">Hệ thống đang tiếp tục cập nhật các đầu sách mới nhất cho chuyên mục này.</p>\n" +
                    "    <div class=\"empty-action\" style=\"margin-top: 16px;\">\n" +
                    "        <a href=\"category?name=Tất cả\" class=\"btn-empty-reset\" style=\"text-decoration:none; display:inline-flex; align-items:center; gap:6px;\">\n" +
                    "            <i class=\"fas fa-border-all\"></i> Xem tất cả sách\n" +
                    "        </a>\n" +
                    "    </div>\n" +
                    "</div>"
            );
        } else {
            for (Book b : books) {
                String bestsellerBadge = b.isBestSeller() ? "<span class=\"badge-tag badge-bestseller\">Bán chạy</span>" : "";
                String discountBadge = (b.getDiscountPercent() > 0) 
                        ? "<span class=\"badge-tag badge-discount\">-" + b.getDiscountPercent() + "%</span>" 
                        : "";
                String originalPriceHtml = (b.getOriginalPrice() > 0 && b.getOriginalPrice() > b.getPrice()) 
                        ? "<span class=\"price-original\">" + b.getFormattedOriginalPrice() + "</span>" 
                        : "";

                boolean outOfStock = b.isOutOfStock();
                String stockBadge = outOfStock
                        ? "<span class=\"badge-tag badge-stock badge-outofstock\"><i class=\"fas fa-ban\"></i> Hết hàng</span>"
                        : "<span class=\"badge-tag badge-stock badge-instock\"><i class=\"fas fa-check\"></i> Còn " + b.getStock() + "</span>";

                String cartButtonHtml = outOfStock
                        ? String.format("<button type=\"button\" class=\"btn-add-cart disabled\" onclick=\"notifyOutOfStock('%s')\" title=\"Sách đã hết hàng trong kho\"><i class=\"fas fa-bell\"></i></button>", escapeAttr(b.getTitle()))
                        : String.format("<button type=\"button\" class=\"btn-add-cart\" onclick=\"addToCart(%d)\" title=\"Thêm vào giỏ\"><i class=\"fas fa-cart-plus\"></i></button>", b.getId());

                String cardClasses = outOfStock ? "book-card is-out-of-stock" : "book-card";

                gridHtml.append(String.format(
                        "<div class=\"%s\" data-id=\"%d\" data-code=\"%s\" data-title=\"%s\" data-author=\"%s\" data-publisher=\"%s\" data-price=\"%.0f\" data-original-price=\"%.0f\" data-formatted-price=\"%s\" data-category=\"%s\" data-stock=\"%d\" data-is-out-of-stock=\"%b\" data-promotion=\"%s\" data-image=\"%s\" data-desc=\"%s\" data-rating=\"%.1f\">\n" +
                        "    <div class=\"book-card-inner\">\n" +
                        "        <div class=\"book-cover-wrap\">\n" +
                        "            <img src=\"%s\" alt=\"%s\" class=\"book-cover\" loading=\"lazy\" onclick=\"goToBookDetail(%d)\" style=\"cursor: pointer;\" onerror=\"this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';\">\n" +
                        "            <div class=\"badge-container\">%s%s%s</div>\n" +
                        "            <div class=\"book-actions-overlay\">\n" +
                        "                <button type=\"button\" class=\"btn-quickview\" onclick=\"goToBookDetail(%d)\"><i class=\"fas fa-eye\"></i> Xem chi tiết</button>\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "        <div class=\"book-info\">\n" +
                        "            <div class=\"book-meta-top\">\n" +
                        "                <span class=\"book-category\">%s</span>\n" +
                        "                <span class=\"book-code\" title=\"Mã sách: %s\"><i class=\"fas fa-barcode\"></i> %s</span>\n" +
                        "            </div>\n" +
                        "            <h3 class=\"book-title\" title=\"%s\" onclick=\"goToBookDetail(%d)\" style=\"cursor: pointer;\">%s</h3>\n" +
                        "            <p class=\"book-author\" title=\"Tác giả\"><i class=\"fas fa-feather-alt\"></i> %s</p>\n" +
                        "            <p class=\"book-publisher\" title=\"Nhà xuất bản\"><i class=\"fas fa-building-columns\"></i> %s</p>\n" +
                        "            <div class=\"book-rating-row\">\n" +
                        "                <div class=\"stars\"><i class=\"fas fa-star\"></i> <span>%.1f</span></div>\n" +
                        "                <span class=\"review-count\">(%d đánh giá)</span>\n" +
                        "                <span class=\"stock-pill %s\">%s</span>\n" +
                        "            </div>\n" +
                        "            <div class=\"book-price-row\">\n" +
                        "                <div class=\"price-box\">\n" +
                        "                    <span class=\"price-current\">%s</span>\n" +
                        "                    %s\n" +
                        "                </div>\n" +
                        "                %s\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "</div>\n",
                        cardClasses,
                        b.getId(),
                        escapeAttr(b.getCode()),
                        escapeAttr(b.getTitle()),
                        escapeAttr(b.getAuthor()),
                        escapeAttr(b.getPublisher()),
                        b.getPrice(),
                        b.getOriginalPrice(),
                        escapeAttr(b.getFormattedPrice()),
                        escapeAttr(b.getCategory()),
                        b.getStock(),
                        outOfStock,
                        escapeAttr(b.getPromotion()),
                        escapeAttr(b.getImage()),
                        escapeAttr(b.getDescription()),
                        b.getRating(),
                        b.getImage(),
                        escapeAttr(b.getTitle()),
                        b.getId(),
                        stockBadge,
                        bestsellerBadge,
                        discountBadge,
                        b.getId(),
                        escapeHtml(b.getCategory()),
                        escapeAttr(b.getCode()),
                        escapeHtml(b.getCode()),
                        escapeAttr(b.getTitle()),
                        b.getId(),
                        escapeHtml(b.getTitle()),
                        escapeHtml(b.getAuthor()),
                        escapeHtml(b.getPublisher()),
                        b.getRating(),
                        b.getReviewCount(),
                        outOfStock ? "stock-pill-empty" : "stock-pill-ok",
                        outOfStock ? "Hết hàng" : "Kho: " + b.getStock(),
                        b.getFormattedPrice(),
                        originalPriceHtml,
                        cartButtonHtml
                ));
            }
        }

        content = content.replace("${CATEGORY_TITLE}", escapeHtml(categoryName));
        content = content.replace("${bookCount}", String.valueOf(books.size()));
        content = content.replace("<!-- ${CATEGORY_BOOK_GRID} -->", gridHtml.toString());
        content = content.replace("${searchKeyword}", "");

        // Set selected in dropdown
        String selectedSort = (sortOrder != null) ? sortOrder : "newest";
        content = content.replace("value=\"" + selectedSort + "\"", "value=\"" + selectedSort + "\" selected");

        return content;
    }

    private static String renderHomePage(User user, List<Book> books, List<String> categories, String selectedCat, String keyword) {
        return renderHomePage(user, books, categories, DataStore.getAuthors(), DataStore.getPublishers(), DataStore.getPromotions(),
                selectedCat, "Tất cả", "Tất cả", null, null, "all", keyword);
    }

    private static String renderHomePage(User user, List<Book> books, List<String> categories, 
                                         List<String> authors, List<String> publishers, 
                                         List<DataStore.PromotionItem> promotions,
                                         String selectedCat, String selectedAuthor, String selectedPublisher, 
                                         Double selectedMinPrice, Double selectedMaxPrice, String selectedStockStatus, 
                                         String keyword) {
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
                    "        <a href=\"profile\" class=\"dropdown-item\">\n" +
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
                    escapeAttr(user.getEmail() != null ? user.getEmail() : "")
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

        // Render Categories Pills
        StringBuilder catHtml = new StringBuilder();
        for (String cat : categories) {
            boolean active = cat.equalsIgnoreCase(selectedCat);
            catHtml.append(String.format(
                    "<button type=\"button\" class=\"category-pill %s\" data-category=\"%s\">%s</button>",
                    active ? "active" : "",
                    escapeAttr(cat),
                    escapeHtml(cat)
            ));
        }
        content = content.replace("<!-- ${CATEGORY_PILLS} -->", catHtml.toString());

        // Render Authors Options
        StringBuilder authorOptionsHtml = new StringBuilder();
        for (String a : authors) {
            boolean sel = a.equalsIgnoreCase(selectedAuthor);
            authorOptionsHtml.append(String.format("<option value=\"%s\" %s>%s</option>",
                    escapeAttr(a), sel ? "selected" : "", escapeHtml(a)));
        }
        content = content.replace("<!-- ${AUTHOR_OPTIONS} -->", authorOptionsHtml.toString());

        // Render Publishers Options
        StringBuilder pubOptionsHtml = new StringBuilder();
        for (String p : publishers) {
            boolean sel = p.equalsIgnoreCase(selectedPublisher);
            pubOptionsHtml.append(String.format("<option value=\"%s\" %s>%s</option>",
                    escapeAttr(p), sel ? "selected" : "", escapeHtml(p)));
        }
        content = content.replace("<!-- ${PUBLISHER_OPTIONS} -->", pubOptionsHtml.toString());

        // Render Active Promotions Banner
        StringBuilder promoBannerHtml = new StringBuilder();
        if (promotions != null && !promotions.isEmpty()) {
            promoBannerHtml.append("<div class=\"promo-ticker-wrap\">");
            promoBannerHtml.append("<div class=\"promo-ticker-title\"><i class=\"fas fa-tags\"></i> ƯU ĐÃI HOT</div>");
            promoBannerHtml.append("<div class=\"promo-ticker-items\">");
            for (DataStore.PromotionItem pr : promotions) {
                promoBannerHtml.append(String.format(
                        "<div class=\"promo-ticker-item\" onclick=\"applyPromoSearch('%s')\" title=\"%s\">" +
                        "<span class=\"promo-code-badge\">%s</span> %s" +
                        "</div>",
                        escapeAttr(pr.getCode()),
                        escapeAttr(pr.getDescription()),
                        escapeHtml(pr.getCode()),
                        escapeHtml(pr.getTitle())
                ));
            }
            promoBannerHtml.append("</div></div>");
        }
        content = content.replace("<!-- ${PROMOTIONS_BANNER} -->", promoBannerHtml.toString());

        // Render Book Cards
        StringBuilder booksHtml = new StringBuilder();
        if (books.isEmpty()) {
            booksHtml.append(
                    "<div class=\"no-books-found\">\n" +
                    "    <div class=\"empty-icon-wrap\"><i class=\"fas fa-book-open\"></i></div>\n" +
                    "    <h3 class=\"empty-title\">Không tìm thấy sản phẩm phù hợp</h3>\n" +
                    "    <div class=\"empty-ai-suggestion\">\n" +
                    "        <div class=\"empty-ai-msg\" onclick=\"openChatbot('Gợi ý sách cho tôi')\">\n" +
                    "            <i class=\"fas fa-robot ai-robot-icon\"></i>\n" +
                    "            <span>Gợi ý: Bạn có thể tìm kiếm bằng Chatbot AI!</span>\n" +
                    "        </div>\n" +
                    "        <div class=\"empty-ai-action\">\n" +
                    "            <button type=\"button\" class=\"btn-empty-ai\" onclick=\"openChatbot('Gợi ý cho tôi các cuốn sách đang được yêu thích nhất')\">\n" +
                    "                <i class=\"fas fa-comments\"></i> Hỏi Chatbot AI ngay\n" +
                    "            </button>\n" +
                    "            <button type=\"button\" class=\"btn-empty-reset\" onclick=\"resetAllFilters()\">\n" +
                    "                <i class=\"fas fa-rotate-left\"></i> Đặt lại bộ lọc\n" +
                    "            </button>\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</div>"
            );
        } else {
            for (Book b : books) {
                String bestsellerBadge = b.isBestSeller() ? "<span class=\"badge-tag badge-bestseller\">Bán chạy</span>" : "";
                String discountBadge = (b.getDiscountPercent() > 0) 
                        ? "<span class=\"badge-tag badge-discount\">-" + b.getDiscountPercent() + "%</span>" 
                        : "";
                String originalPriceHtml = (b.getOriginalPrice() > 0) 
                        ? "<span class=\"price-original\">" + b.getFormattedOriginalPrice() + "</span>" 
                        : "";

                boolean outOfStock = b.isOutOfStock();
                String stockBadge = outOfStock
                        ? "<span class=\"badge-tag badge-stock badge-outofstock\"><i class=\"fas fa-ban\"></i> Hết hàng</span>"
                        : "<span class=\"badge-tag badge-stock badge-instock\"><i class=\"fas fa-check\"></i> Còn " + b.getStock() + "</span>";

                String cartButtonHtml = outOfStock
                        ? String.format("<button type=\"button\" class=\"btn-add-cart disabled\" onclick=\"notifyOutOfStock('%s')\" title=\"Sách đã hết hàng trong kho\"><i class=\"fas fa-bell\"></i></button>", escapeAttr(b.getTitle()))
                        : String.format("<button type=\"button\" class=\"btn-add-cart\" onclick=\"addToCart(%d)\" title=\"Thêm vào giỏ\"><i class=\"fas fa-cart-plus\"></i></button>", b.getId());

                String cardClasses = outOfStock ? "book-card is-out-of-stock" : "book-card";

                booksHtml.append(String.format(
                        "<div class=\"%s\" data-id=\"%d\" data-code=\"%s\" data-title=\"%s\" data-author=\"%s\" data-publisher=\"%s\" data-price=\"%.0f\" data-original-price=\"%.0f\" data-formatted-price=\"%s\" data-category=\"%s\" data-stock=\"%d\" data-is-out-of-stock=\"%b\" data-promotion=\"%s\" data-image=\"%s\" data-desc=\"%s\" data-rating=\"%.1f\">\n" +
                        "    <div class=\"book-card-inner\">\n" +
                        "        <div class=\"book-cover-wrap\">\n" +
                        "            <img src=\"%s\" alt=\"%s\" class=\"book-cover\" loading=\"lazy\" onclick=\"goToBookDetail(%d)\" style=\"cursor: pointer;\" onerror=\"this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';\">\n" +
                        "            <div class=\"badge-container\">%s%s%s</div>\n" +
                        "            <div class=\"book-actions-overlay\">\n" +
                        "                <button type=\"button\" class=\"btn-quickview\" onclick=\"goToBookDetail(%d)\"><i class=\"fas fa-eye\"></i> Xem chi tiết</button>\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "        <div class=\"book-info\">\n" +
                        "            <div class=\"book-meta-top\">\n" +
                        "                <span class=\"book-category\">%s</span>\n" +
                        "                <span class=\"book-code\" title=\"Mã sách: %s\"><i class=\"fas fa-barcode\"></i> %s</span>\n" +
                        "            </div>\n" +
                        "            <h3 class=\"book-title\" title=\"%s\" onclick=\"goToBookDetail(%d)\" style=\"cursor: pointer;\">%s</h3>\n" +
                        "            <p class=\"book-author\" title=\"Tác giả\"><i class=\"fas fa-feather-alt\"></i> %s</p>\n" +
                        "            <p class=\"book-publisher\" title=\"Nhà xuất bản\"><i class=\"fas fa-building-columns\"></i> %s</p>\n" +
                        "            <div class=\"book-rating-row\">\n" +
                        "                <div class=\"stars\"><i class=\"fas fa-star\"></i> <span>%.1f</span></div>\n" +
                        "                <span class=\"review-count\">(%d đánh giá)</span>\n" +
                        "                <span class=\"stock-pill %s\">%s</span>\n" +
                        "            </div>\n" +
                        "            <div class=\"book-price-row\">\n" +
                        "                <div class=\"price-box\">\n" +
                        "                    <span class=\"price-current\">%s</span>\n" +
                        "                    %s\n" +
                        "                </div>\n" +
                        "                %s\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "</div>\n",
                        cardClasses,
                        b.getId(),
                        escapeAttr(b.getCode()),
                        escapeAttr(b.getTitle()),
                        escapeAttr(b.getAuthor()),
                        escapeAttr(b.getPublisher()),
                        b.getPrice(),
                        b.getOriginalPrice(),
                        escapeAttr(b.getFormattedPrice()),
                        escapeAttr(b.getCategory()),
                        b.getStock(),
                        outOfStock,
                        escapeAttr(b.getPromotion()),
                        escapeAttr(b.getImage()),
                        escapeAttr(b.getDescription()),
                        b.getRating(),
                        b.getImage(),
                        escapeAttr(b.getTitle()),
                        b.getId(),
                        stockBadge,
                        bestsellerBadge,
                        discountBadge,
                        b.getId(),
                        escapeHtml(b.getCategory()),
                        escapeAttr(b.getCode()),
                        escapeHtml(b.getCode()),
                        escapeAttr(b.getTitle()),
                        b.getId(),
                        escapeHtml(b.getTitle()),
                        escapeHtml(b.getAuthor()),
                        escapeHtml(b.getPublisher()),
                        b.getRating(),
                        b.getReviewCount(),
                        outOfStock ? "stock-pill-empty" : "stock-pill-ok",
                        outOfStock ? "Hết hàng" : "Kho: " + b.getStock(),
                        b.getFormattedPrice(),
                        originalPriceHtml,
                        cartButtonHtml
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
            User u = DataStore.findUser(username);
            if (u != null && "LOCKED".equalsIgnoreCase(u.getStatus())) {
                activeSessions.remove(token);
                return null;
            }
            return u;
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
