package com.bookstore.data;

import com.bookstore.model.Book;
import com.bookstore.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kho dữ liệu thao tác trực tiếp với cơ sở dữ liệu MySQL (web_bookora),
 * có cơ chế dự phòng bộ nhớ nếu máy chủ cơ sở dữ liệu tạm thời chưa khởi động.
 */
public class DataStore {
    private static final ConcurrentHashMap<String, User> memoryUsers = new ConcurrentHashMap<>();
    private static final List<Book> memoryBooks = Collections.synchronizedList(new ArrayList<>());

    static {
        initMemoryDefaults();
    }

    private static void initMemoryDefaults() {
        memoryUsers.put("admin", new User("admin", "123456", "Quản Trị Viên - Hoàng Oanh", "admin@bookora.vn", "0988123456", "ADMIN", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"));
        memoryUsers.put("oanh", new User("oanh", "123456", "Hoàng Oanh", "oanh.nguyen@gmail.com", "0912345678", "CUSTOMER", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"));
        memoryUsers.put("khachhang", new User("khachhang", "123456", "Khách Hàng Thân Thiết", "khachhang@gmail.com", "0909888999", "CUSTOMER", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"));
    }

    /**
     * Tìm kiếm người dùng theo username từ MySQL
     */
    public static User findUser(String username) {
        if (username == null) return null;
        String sql = "SELECT username, password, full_name, email, phone, role, avatar FROM users WHERE LOWER(username) = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("role"),
                            rs.getString("avatar")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL findUser: Không thể truy vấn database (" + e.getMessage() + "). Dùng bộ nhớ tạm.");
        }

        return memoryUsers.get(username.trim().toLowerCase());
    }

    /**
     * Kiểm tra thông tin đăng nhập từ MySQL
     */
    public static boolean validateUser(String username, String password) {
        if (username == null || password == null) return false;
        String sql = "SELECT username FROM users WHERE LOWER(username) = ? AND password = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL validateUser: Không thể truy vấn database (" + e.getMessage() + "). Dùng bộ nhớ tạm.");
            User u = memoryUsers.get(username.trim().toLowerCase());
            return u != null && u.getPassword().equals(password);
        }

        return false;
    }

    /**
     * Đăng ký người dùng mới vào MySQL
     */
    public static boolean registerUser(User newUser) {
        if (newUser == null || newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
            return false;
        }
        String cleanUsername = newUser.getUsername().trim();

        // Kiểm tra xem username đã tồn tại chưa
        if (findUser(cleanUsername) != null) {
            return false; // Tên đăng nhập đã tồn tại
        }

        String sql = "INSERT INTO users (username, password, full_name, email, phone, role, avatar) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanUsername);
            ps.setString(2, newUser.getPassword());
            ps.setString(3, newUser.getFullName());
            ps.setString(4, (newUser.getEmail() != null && !newUser.getEmail().trim().isEmpty()) ? newUser.getEmail().trim() : null);
            ps.setString(5, (newUser.getPhone() != null && !newUser.getPhone().trim().isEmpty()) ? newUser.getPhone().trim() : null);
            ps.setString(6, newUser.getRole() != null ? newUser.getRole() : "CUSTOMER");
            ps.setString(7, newUser.getAvatar() != null ? newUser.getAvatar() : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL registerUser lỗi: " + e.getMessage());
            memoryUsers.put(cleanUsername.toLowerCase(), newUser);
            return true;
        }
    }

    // ====================== QUẢN LÝ MÃ XÁC THỰC OTP ======================

    public static class OtpSession {
        private final String code;
        private final User user;
        private final long expiryTime;
        private final String channel;
        private final String target;

        public OtpSession(String code, User user, long expiryTime, String channel, String target) {
            this.code = code;
            this.user = user;
            this.expiryTime = expiryTime;
            this.channel = channel;
            this.target = target;
        }

        public String getCode() { return code; }
        public User getUser() { return user; }
        public long getExpiryTime() { return expiryTime; }
        public String getChannel() { return channel; }
        public String getTarget() { return target; }
        public boolean isExpired() { return System.currentTimeMillis() > expiryTime; }
    }

    private static final ConcurrentHashMap<String, OtpSession> otpSessions = new ConcurrentHashMap<>();

    /**
     * Khởi tạo mã OTP ngẫu nhiên 6 chữ số với thời hạn 2 phút
     */
    public static OtpSession createOtpSession(User user, String channel) {
        String code = String.format("%06d", new java.util.Random().nextInt(900000) + 100000);
        long expiry = System.currentTimeMillis() + (2 * 60 * 1000); // 2 phút hiệu lực

        String finalChannel = channel != null ? channel.toLowerCase().trim() : "email";
        String target = "phone".equalsIgnoreCase(finalChannel) ? user.getPhone() : user.getEmail();

        // Tự động chuyển đổi nếu kênh yêu cầu không có dữ liệu nhưng kênh còn lại có dữ liệu
        if (target == null || target.trim().isEmpty()) {
            if ("phone".equalsIgnoreCase(finalChannel) && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                finalChannel = "email";
                target = user.getEmail().trim();
            } else if ("email".equalsIgnoreCase(finalChannel) && user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                finalChannel = "phone";
                target = user.getPhone().trim();
            }
        }

        OtpSession session = new OtpSession(code, user, expiry, finalChannel, target != null ? target : "");
        otpSessions.put(user.getUsername().toLowerCase(), session);
        System.out.println("===============================================================");
        System.out.println("🔔 [BOOKORA OTP] Mã xác thực đăng ký cho " + user.getUsername() + ":");
        System.out.println("   - Kênh nhận: " + ("phone".equalsIgnoreCase(finalChannel) ? "Số điện thoại (" + target + ")" : "Email (" + target + ")"));
        System.out.println("   - MÃ OTP:    >>> " + code + " <<< (Có hiệu lực trong 2 phút)");
        System.out.println("===============================================================");

        // Nếu kênh là phone -> tiến hành gửi SMS thực tế qua SMS Gateway đến SIM khách hàng
        if ("phone".equalsIgnoreCase(finalChannel) && target != null && !target.trim().isEmpty()) {
            boolean realSent = com.bookstore.service.OtpSenderService.sendRealSms(target, code);
            if (realSent) {
                System.out.println("🚀 [SMS THỰC TẾ] Đã phát sóng SMS thành công đến số điện thoại: " + target);
            }
        }

        // Nếu kênh là email -> tiến hành gửi Email thực tế qua Gmail SMTP đến hòm thư khách hàng
        if ("email".equalsIgnoreCase(finalChannel) && target != null && !target.trim().isEmpty()) {
            boolean emailSent = com.bookstore.service.EmailService.sendOtpEmail(target, code, user.getFullName());
            if (emailSent) {
                System.out.println("🚀 [EMAIL THỰC TẾ] Đã gửi thư thành công đến địa chỉ hòm thư: " + target);
            }
        }

        return session;
    }

    public static OtpSession getOtpSession(String username) {
        if (username == null) return null;
        return otpSessions.get(username.trim().toLowerCase());
    }

    /**
     * Xác thực mã OTP và lưu người dùng vào MySQL nếu chính xác
     */
    public static boolean verifyAndRegister(String username, String code) {
        if (username == null || code == null) return false;
        OtpSession session = otpSessions.get(username.trim().toLowerCase());
        if (session == null || session.isExpired()) {
            if (session != null) otpSessions.remove(username.trim().toLowerCase());
            return false;
        }
        if (session.getCode().equals(code.trim())) {
            boolean registered = registerUser(session.getUser());
            if (registered) {
                otpSessions.remove(username.trim().toLowerCase());
                return true;
            }
        }
        return false;
    }

    /**
     * Lấy danh sách toàn bộ sách từ MySQL
     */
    public static List<Book> getAllBooks() {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT id, title, author, price, original_price, category, rating, review_count, image, description, is_bestseller FROM books ORDER BY id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(extractBookFromResultSet(rs));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL getAllBooks lỗi: " + e.getMessage());
        }

        return new ArrayList<>(memoryBooks);
    }

    /**
     * Tìm sách theo ID từ MySQL
     */
    public static Book getBookById(int id) {
        String sql = "SELECT id, title, author, price, original_price, category, rating, review_count, image, description, is_bestseller FROM books WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractBookFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL getBookById lỗi: " + e.getMessage());
        }

        for (Book b : memoryBooks) {
            if (b.getId() == id) return b;
        }
        return null;
    }

    /**
     * Tìm kiếm và lọc sách từ MySQL
     */
    public static List<Book> searchBooks(String keyword, String category) {
        List<Book> list = new ArrayList<>();
        String kw = keyword == null ? "" : keyword.trim();
        String cat = category == null ? "" : category.trim();

        StringBuilder sql = new StringBuilder("SELECT id, title, author, price, original_price, category, rating, review_count, image, description, is_bestseller FROM books WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!kw.isEmpty()) {
            sql.append("AND (LOWER(title) LIKE ? OR LOWER(author) LIKE ?) ");
            params.add("%" + kw.toLowerCase() + "%");
            params.add("%" + kw.toLowerCase() + "%");
        }

        if (!cat.isEmpty() && !"Tất cả".equalsIgnoreCase(cat)) {
            sql.append("AND category = ? ");
            params.add(cat);
        }

        sql.append("ORDER BY id ASC");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractBookFromResultSet(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL searchBooks lỗi: " + e.getMessage());
        }

        return getAllBooks();
    }

    /**
     * Lấy danh sách các thể loại sách từ MySQL
     */
    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("Tất cả");
        String sql = "SELECT DISTINCT category FROM books ORDER BY category ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String c = rs.getString("category");
                if (c != null && !categories.contains(c)) {
                    categories.add(c);
                }
            }
            if (categories.size() > 1) {
                return categories;
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL getCategories lỗi: " + e.getMessage());
        }

        return categories;
    }

    private static Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        return new Book(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getDouble("price"),
                rs.getDouble("original_price"),
                rs.getString("category"),
                rs.getDouble("rating"),
                rs.getInt("review_count"),
                rs.getString("image"),
                rs.getString("description"),
                rs.getBoolean("is_bestseller")
        );
    }
}
