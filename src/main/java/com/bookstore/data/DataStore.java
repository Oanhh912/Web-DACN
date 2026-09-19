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

        // Danh sách 24 cuốn sách dự phòng trong bộ nhớ (dùng khi máy chưa cài đặt hoặc chưa khởi động MySQL)
        memoryBooks.add(new Book(1, "MS001", "Nhà Giả Kim (The Alchemist)", "Paulo Coelho", 79000, 99000, "Văn học", 4.9, 1420, "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500", "Cuốn sách kinh điển kể về hành trình theo đuổi ước mơ và lắng nghe tiếng gọi của trái tim của chàng chăn cừu Santiago.", true));
        memoryBooks.add(new Book(2, "MS002", "Đắc Nhân Tâm (How to Win Friends)", "Dale Carnegie", 88000, 110000, "Kỹ năng sống", 4.8, 2350, "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "Nghệ thuật thu phục lòng người đỉnh cao, cuốn sách gối đầu giường của hàng triệu độc giả trên toàn thế giới.", true));
        memoryBooks.add(new Book(3, "MS003", "Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin (Uncle Bob)", 285000, 350000, "Công nghệ", 4.9, 890, "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", "Cẩm nang kinh điển của mọi lập trình viên phần mềm để viết mã nguồn sạch sẽ, dễ bảo trì và mở rộng.", true));
        memoryBooks.add(new Book(4, "MS004", "Tư Duy Nhanh Và Chậm (Thinking, Fast and Slow)", "Daniel Kahneman", 145000, 185000, "Tâm lý học", 4.7, 640, "https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=500", "Giải thích hai hệ thống thúc đẩy cách chúng ta tư duy và đưa ra các quyết định trong cuộc sống.", false));
        memoryBooks.add(new Book(5, "MS005", "Cha Giàu Cha Nghèo (Rich Dad Poor Dad)", "Robert T. Kiyosaki", 95000, 125000, "Kinh tế", 4.8, 1890, "https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=500", "Bài học về tư duy tài chính độc lập và cách để đồng tiền làm việc cho chính bạn thay vì làm việc vì tiền.", true));
        memoryBooks.add(new Book(6, "MS006", "Muôn Kiếp Nhân Sinh (Phần 1 & 2)", "Nguyên Phong", 168000, 210000, "Tâm linh & Đời sống", 4.9, 1560, "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500", "Tác phẩm ghi lại những trải nghiệm tiền kiếp kỳ lạ và thông điệp sâu sắc về quy luật nhân quả của vũ trụ.", true));
        memoryBooks.add(new Book(7, "MS007", "Cây Cam Ngọt Của Tôi", "José Mauro de Vasconcelos", 82000, 108000, "Văn học", 4.9, 3200, "https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=500", "Câu chuyện cảm động rơi nước mắt về tuổi thơ hồn nhiên nhưng đầy vết thương của cậu bé Zezé.", true));
        memoryBooks.add(new Book(8, "MS008", "Thiết Kế Giải Thuật & Cấu Trúc Dữ Liệu", "Thomas H. Cormen", 320000, 390000, "Công nghệ", 4.8, 410, "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500", "Giáo trình toàn diện về giải thuật cơ bản và nâng cao dành cho sinh viên và kỹ sư công nghệ thông tin.", false));
        memoryBooks.add(new Book(9, "MS009", "Thói Quen Nguyên Tử (Atomic Habits)", "James Clear", 129000, 169000, "Kỹ năng sống", 4.9, 2900, "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500", "Cách mạng hóa cuộc sống của bạn từ những thay đổi cực kỳ nhỏ bé nhưng mang lại hiệu quả phi thường mỗi ngày.", true));
        memoryBooks.add(new Book(10, "MS010", "Hoàng Tử Bé (The Little Prince)", "Antoine de Saint-Exupéry", 65000, 85000, "Văn học", 4.9, 1820, "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "Kiệt tác văn học Pháp mang thông điệp triết lý sâu sắc về tình bạn, tình yêu và cái nhìn trong trẻo của trẻ thơ.", true));
        memoryBooks.add(new Book(11, "MS011", "Tâm Lý Học Về Tiền (The Psychology of Money)", "Morgan Housel", 136000, 170000, "Kinh tế", 4.9, 2150, "https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=500", "19 câu chuyện ngắn khám phá những cách kỳ lạ mà mọi người nghĩ về tiền bạc và cách quản lý tài chính thông minh.", true));
        memoryBooks.add(new Book(12, "MS012", "Sapiens: Lược Sử Loài Người", "Yuval Noah Harari", 195000, 250000, "Khoa học", 4.9, 3400, "https://images.unsplash.com/photo-1447069387593-a5de0862481e?w=500", "Hành trình kỳ vĩ kể về lịch sử tiến hóa của loài người từ thời kỳ đồ đá cho đến kỷ nguyên hiện đại.", true));
        memoryBooks.add(new Book(13, "MS013", "Khởi Nghiệp Tinh Gọn (The Lean Startup)", "Eric Ries", 125000, 160000, "Kinh tế", 4.8, 980, "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500", "Phương pháp xây dựng doanh nghiệp đổi mới sáng tạo thành công vượt bậc trong thời đại biến động.", false));
        memoryBooks.add(new Book(14, "MS014", "The Pragmatic Programmer: 20th Anniversary Edition", "David Thomas, Andrew Hunt", 310000, 380000, "Công nghệ", 4.9, 760, "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500", "Cẩm nang từ các bậc thầy lập trình giúp bạn nâng tầm kỹ năng từ một thợ code thành kỹ sư phần mềm thực thụ.", true));
        memoryBooks.add(new Book(15, "MS015", "Dám Bị Ghét", "Kishimi Ichiro, Koga Fumitake", 98000, 125000, "Tâm lý học", 4.7, 1950, "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=500", "Đối thoại triết học dựa trên tâm lý học Alfred Adler giúp bạn tìm thấy tự do và dũng khí sống thật với chính mình.", true));
        memoryBooks.add(new Book(16, "MS016", "Bố Già (The Godfather)", "Mario Puzo", 118000, 150000, "Văn học", 4.9, 4100, "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "Tiểu thuyết kinh điển về thế giới mafia Ý-Mỹ, đỉnh cao của nghệ thuật xây dựng nhân vật và quyền lực.", true));
        memoryBooks.add(new Book(17, "MS017", "Mắt Biếc", "Nguyễn Nhật Ánh", 72000, 90000, "Văn học", 4.8, 5200, "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500", "Mối tình si ngốc nghếch, trong sáng nhưng da diết của Ngạn dành cho cô bạn thời thơ ấu có đôi mắt biếc.", true));
        memoryBooks.add(new Book(18, "MS018", "Từ Tốt Đến Vĩ Đại (Good to Great)", "Jim Collins", 149000, 195000, "Kinh tế", 4.8, 1200, "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=500", "Nghiên cứu công phu về lý do tại sao một số công ty có thể tạo ra bước nhảy vọt phi thường còn số khác thì không.", false));
        memoryBooks.add(new Book(19, "MS019", "Clean Architecture: A Craftsman's Guide", "Robert C. Martin (Uncle Bob)", 295000, 360000, "Công nghệ", 4.9, 680, "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500", "Quy tắc vàng về kiến trúc phần mềm giúp hệ thống bền vững, độc lập framework và dễ dàng kiểm thử.", true));
        memoryBooks.add(new Book(20, "MS020", "Sức Mạnh Của Hiện Tại (The Power of Now)", "Eckhart Tolle", 105000, 135000, "Tâm linh & Đời sống", 4.8, 1430, "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=500", "Hướng dẫn thức tỉnh tâm thức, giải phóng bản thân khỏi nỗi đau quá khứ và sự lo lắng về tương lai.", false));
        memoryBooks.add(new Book(21, "MS021", "7 Thói Quen Của Bạn Trẻ Thành Đạt", "Sean Covey", 95000, 120000, "Kỹ năng sống", 4.8, 2670, "https://images.unsplash.com/photo-1507842229450-760773d528b8?w=500", "Chiếc la bàn chỉ đường giúp thanh thiếu niên rèn luyện nhân cách, xác định mục tiêu và gặt hái thành công.", true));
        memoryBooks.add(new Book(22, "MS022", "Đọc Vị Bất Kỳ Ai (You Can Read Anyone)", "David J. Lieberman", 78000, 99000, "Tâm lý học", 4.7, 1880, "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500", "Nắm bắt tâm lý, giải mã ngôn ngữ cơ thể và suy nghĩ của đối phương chỉ trong vài phút giao tiếp.", false));
        memoryBooks.add(new Book(23, "MS023", "Vũ Trụ Trong Vỏ Hạt Dẻ (The Universe in a Nutshell)", "Stephen Hawking", 155000, 195000, "Khoa học", 4.8, 1120, "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500", "Khám phá những biên giới kỳ diệu của vật lý lý thuyết: thuyết tương đối, lỗ đen và lý thuyết siêu dây.", false));
        memoryBooks.add(new Book(24, "MS024", "Rừng Na Uy (Norwegian Wood)", "Haruki Murakami", 110000, 140000, "Văn học", 4.8, 3800, "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500", "Tác phẩm lừng danh Nhật Bản khắc họa nỗi cô đơn và sự chới với của tuổi trẻ giữa mất mát và trưởng thành.", true));
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
        }

        User u = memoryUsers.get(username.trim().toLowerCase());
        return u != null && u.getPassword().equals(password);
    }

    /**
     * Đăng ký người dùng mới vào MySQL (kèm đồng bộ bộ nhớ dự phòng)
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
            if (rows > 0) {
                memoryUsers.put(cleanUsername.toLowerCase(), newUser);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL registerUser lỗi (" + e.getMessage() + "). Lưu vào bộ nhớ tạm.");
            memoryUsers.put(cleanUsername.toLowerCase(), newUser);
            return true;
        }

        return false;
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
        String sql = "SELECT id, code, title, author, price, original_price, category, rating, review_count, image, description, is_bestseller FROM books ORDER BY id ASC";

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
        String sql = "SELECT id, code, title, author, price, original_price, category, rating, review_count, image, description, is_bestseller FROM books WHERE id = ?";

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
     * Tìm kiếm và lọc sách từ MySQL (theo tiêu đề, tác giả hoặc mã sách)
     */
    public static List<Book> searchBooks(String keyword, String category) {
        List<Book> list = new ArrayList<>();
        String kw = keyword == null ? "" : keyword.trim();
        String cat = category == null ? "" : category.trim();

        StringBuilder sql = new StringBuilder("SELECT id, code, title, author, price, original_price, category, rating, review_count, image, description, is_bestseller FROM books WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!kw.isEmpty()) {
            sql.append("AND (LOWER(title) LIKE ? OR LOWER(author) LIKE ? OR LOWER(code) LIKE ?) ");
            params.add("%" + kw.toLowerCase() + "%");
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
            System.err.println("⚠️ MySQL searchBooks lỗi (" + e.getMessage() + "). Dùng bộ nhớ tạm.");
            List<Book> fallback = new ArrayList<>();
            for (Book b : memoryBooks) {
                boolean matchCat = cat.isEmpty() || "Tất cả".equalsIgnoreCase(cat) || b.getCategory().equalsIgnoreCase(cat);
                boolean matchKw = kw.isEmpty() 
                        || b.getTitle().toLowerCase().contains(kw.toLowerCase()) 
                        || b.getAuthor().toLowerCase().contains(kw.toLowerCase())
                        || b.getCode().toLowerCase().contains(kw.toLowerCase());
                if (matchCat && matchKw) {
                    fallback.add(b);
                }
            }
            return fallback;
        }
    }

    /**
     * Lấy danh sách các thể loại sách từ MySQL (kèm dự phòng bộ nhớ)
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
            System.err.println("⚠️ MySQL getCategories lỗi (" + e.getMessage() + "). Dùng bộ nhớ tạm.");
            for (Book b : memoryBooks) {
                if (!categories.contains(b.getCategory())) {
                    categories.add(b.getCategory());
                }
            }
            return categories;
        }

        return categories;
    }

    private static Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        String code = "";
        try {
            code = rs.getString("code");
        } catch (SQLException ignored) {}
        if (code == null || code.trim().isEmpty()) {
            code = String.format("MS%03d", rs.getInt("id"));
        }

        return new Book(
                rs.getInt("id"),
                code,
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
