package com.bookstore.util;

import com.bookstore.config.AppConfig;
import com.bookstore.data.DataStore;
import com.bookstore.model.User;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Các hàm tiện ích dùng chung cho HTTP Server của Bookora.
 *
 * Được tách ra từ BookstoreApp để giảm kích thước file và tăng khả năng tái sử dụng.
 * Mọi handler (LoginHandler, HomeHandler...) đều có thể import và dùng các method này.
 */
public class HttpUtils {

    // ─── Session & Auth ───────────────────────────────────────────────────────

    /**
     * Lấy đối tượng User đang đăng nhập từ cookie phiên.
     * Trả về null nếu chưa đăng nhập hoặc phiên không hợp lệ.
     *
     * @param exchange   HTTP exchange hiện tại
     * @param activeSessions Bản đồ token → username
     */
    public static User getAuthenticatedUser(HttpExchange exchange,
                                            Map<String, String> activeSessions) {
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

    /**
     * Lấy token phiên từ cookie của request.
     */
    public static String getSessionToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String cookieHeader : cookies) {
                String[] pairs = cookieHeader.split(";");
                for (String pair : pairs) {
                    String[] kv = pair.trim().split("=", 2);
                    if (kv.length == 2
                            && AppConfig.SESSION_COOKIE_NAME.equals(kv[0].trim())) {
                        return kv[1].trim();
                    }
                }
            }
        }
        return null;
    }

    // ─── Parsing ──────────────────────────────────────────────────────────────

    /**
     * Đọc và phân tích dữ liệu form từ request body (application/x-www-form-urlencoded).
     */
    public static Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return parseQueryString(body);
    }

    /**
     * Phân tích query string thành Map key→value.
     * Hỗ trợ URL encoding (ví dụ: %20 → khoảng trắng).
     */
    public static Map<String, String> parseQueryString(String qs) {
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

    // ─── Response helpers ─────────────────────────────────────────────────────

    /**
     * Gửi HTTP redirect (302) đến địa chỉ được chỉ định.
     */
    public static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    /**
     * Gửi HTTP response với nội dung text/HTML/JSON.
     *
     * @param exchange    HTTP exchange
     * @param status      HTTP status code (200, 400, 404, 500...)
     * @param contentType Content-Type header (vd: "application/json; charset=UTF-8")
     * @param content     Nội dung phản hồi
     */
    public static void sendResponse(HttpExchange exchange,
                                    int status,
                                    String contentType,
                                    String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Gửi JSON response lỗi chuẩn.
     */
    public static void sendJsonError(HttpExchange exchange,
                                     int status,
                                     String message) throws IOException {
        String json = "{\"success\":false,\"message\":" + escapeJson(message) + "}";
        sendResponse(exchange, status, "application/json; charset=UTF-8", json);
    }

    /**
     * Gửi JSON response thành công.
     */
    public static void sendJsonOk(HttpExchange exchange, String jsonBody) throws IOException {
        sendResponse(exchange, 200, "application/json; charset=UTF-8", jsonBody);
    }

    // ─── MIME type ────────────────────────────────────────────────────────────

    /**
     * Xác định MIME type dựa vào phần mở rộng của tên file.
     */
    public static String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".css"))              return "text/css; charset=UTF-8";
        if (lower.endsWith(".js"))               return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".png"))              return "image/png";
        if (lower.endsWith(".jpg")
                || lower.endsWith(".jpeg"))      return "image/jpeg";
        if (lower.endsWith(".gif"))              return "image/gif";
        if (lower.endsWith(".webp"))             return "image/webp";
        if (lower.endsWith(".svg"))              return "image/svg+xml";
        if (lower.endsWith(".ico"))              return "image/x-icon";
        if (lower.endsWith(".json"))             return "application/json; charset=UTF-8";
        if (lower.endsWith(".woff"))             return "font/woff";
        if (lower.endsWith(".woff2"))            return "font/woff2";
        if (lower.endsWith(".ttf"))              return "font/ttf";
        if (lower.endsWith(".pdf"))              return "application/pdf";
        return "text/html; charset=UTF-8";
    }

    // ─── Escaping ─────────────────────────────────────────────────────────────

    /**
     * Escape ký tự HTML đặc biệt để tránh XSS.
     */
    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    /**
     * Escape ký tự để dùng an toàn trong HTML attribute.
     */
    public static String escapeAttr(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    /**
     * Escape chuỗi để dùng an toàn trong JSON string value.
     * Trả về chuỗi kèm dấu nháy kép bao quanh (ví dụ: "hello \"world\"").
     */
    public static String escapeJson(String input) {
        if (input == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
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

    // ─── Password ─────────────────────────────────────────────────────────────

    /**
     * Kiểm tra mật khẩu có đủ mạnh không:
     * - Ít nhất 6 ký tự
     * - Có chữ hoa
     * - Có chữ thường
     * - Có chữ số
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 6) return false;
        boolean hasUpper  = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower  = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit  = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }
}
