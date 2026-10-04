package com.bookstore.data;

import com.bookstore.model.Address;
import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.bookstore.model.ChatbotContent;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.PaymentMethod;
import com.bookstore.model.Review;
import com.bookstore.model.User;
import com.bookstore.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kho dữ liệu thao tác trực tiếp với cơ sở dữ liệu MySQL (web_bookora),
 * có cơ chế dự phòng bộ nhớ nếu máy chủ cơ sở dữ liệu tạm thời chưa khởi động.
 */
public class DataStore {
    private static final ConcurrentHashMap<String, User> memoryUsers = new ConcurrentHashMap<>();
    private static final List<Book> memoryBooks = Collections.synchronizedList(new ArrayList<>());
    private static final List<Address> memoryAddresses = Collections.synchronizedList(new ArrayList<>());
    private static final List<Voucher> memoryVouchers = Collections.synchronizedList(new ArrayList<>());
    private static final List<PaymentMethod> memoryPaymentMethods = Collections.synchronizedList(new ArrayList<>());
    private static final List<Order> memoryOrders = Collections.synchronizedList(new ArrayList<>());
    private static final List<Category> memoryCategories = Collections.synchronizedList(new ArrayList<>());
    private static final List<Review> memoryReviews = Collections.synchronizedList(new ArrayList<>());
    private static final ConcurrentHashMap<String, List<OrderItem>> memoryCarts = new ConcurrentHashMap<>();
    private static int nextAddressId = 100;
    private static int nextOrderId = 100;

    static {
        initMemoryDefaults();
        ensureBookTableSchema();
        ensureUserTableSchema();
        ensureCategoryTableSchema();
        ensureReviewTableSchema();
    }

    public static void ensureCategoryTableSchema() {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS categories ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "code VARCHAR(50) NOT NULL UNIQUE, "
                + "name VARCHAR(100) NOT NULL UNIQUE, "
                + "description VARCHAR(500), "
                + "status VARCHAR(20) DEFAULT 'ACTIVE', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sqlCreate);

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String[] defaultCats = {
                        "INSERT INTO categories (code, name, description, status) VALUES ('DM001', 'Văn học', 'Tiểu thuyết, truyện ngắn và các tác phẩm văn học trong nước và quốc tế kinh điển', 'ACTIVE')",
                        "INSERT INTO categories (code, name, description, status) VALUES ('DM002', 'Kỹ năng sống', 'Sách rèn luyện bản thân, phát triển tư duy, giao tiếp và kỹ năng làm chủ cuộc sống', 'ACTIVE')",
                        "INSERT INTO categories (code, name, description, status) VALUES ('DM003', 'Công nghệ', 'Cẩm nang lập trình, kiến trúc phần mềm, khoa học dữ liệu & công nghệ mới', 'ACTIVE')",
                        "INSERT INTO categories (code, name, description, status) VALUES ('DM004', 'Tâm lý học', 'Khám phá hành vi, tư duy và nhận thức con người qua góc nhìn khoa học tâm lý', 'ACTIVE')",
                        "INSERT INTO categories (code, name, description, status) VALUES ('DM005', 'Kinh tế', 'Quản trị kinh doanh, tư duy tài chính, làm giàu và khởi nghiệp tinh gọn', 'ACTIVE')",
                        "INSERT INTO categories (code, name, description, status) VALUES ('DM006', 'Tâm linh & Đời sống', 'Sách nuôi dưỡng tâm hồn, thức tỉnh tâm thức và nghệ thuật chuyển hóa cảm xúc', 'ACTIVE')",
                        "INSERT INTO categories (code, name, description, status) VALUES ('DM007', 'Khoa học', 'Khám phá tri thức vũ trụ, lịch sử loài người và khoa học tự nhiên', 'ACTIVE')",
                        "INSERT INTO categories (code, name, description, status) VALUES ('DM008', 'Trinh thám', 'Tiểu thuyết trinh thám, vụ án bí ẩn và hình sự kịch tính', 'ACTIVE')",
                        "INSERT INTO categories (code, name, description, status) VALUES ('DM009', 'Sách thiếu nhi', 'Truyện tranh, sách màu và tri thức bổ ích dành cho trẻ em và tuổi mới lớn', 'ACTIVE')"
                    };
                    for (String seedSql : defaultCats) {
                        try { stmt.executeUpdate(seedSql); } catch (SQLException ignored) {}
                    }
                }
            }
        } catch (SQLException ignored) {}
    }

    public static void ensureUserTableSchema() {
        String sql = "ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE'";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException ignored) {}
    }

    public static void ensureBookTableSchema() {
        String[] alterSqls = {
            "ALTER TABLE books ADD COLUMN publish_year INT DEFAULT 2024",
            "ALTER TABLE books ADD COLUMN page_count INT DEFAULT 250",
            "ALTER TABLE books ADD COLUMN weight INT DEFAULT 350",
            "ALTER TABLE books ADD COLUMN dimensions VARCHAR(100) DEFAULT '21 x 13.5 x 1.2 cm'",
            "ALTER TABLE books ADD COLUMN cover_format VARCHAR(50) DEFAULT 'Bìa Mềm'"
        };
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : alterSqls) {
                try {
                    stmt.executeUpdate(sql);
                } catch (SQLException ignored) {}
            }
        } catch (SQLException ignored) {}
    }

    private static void initMemoryDefaults() {
        memoryUsers.put("admin", new User("admin", "123456", "Quản Trị Viên - Hoàng Oanh", "admin@bookora.vn", "0988123456", "ADMIN", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"));
        memoryUsers.put("oanh", new User("oanh", "123456", "Hoàng Oanh", "oanh.nguyen@gmail.com", "0912345678", "CUSTOMER", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"));
        memoryUsers.put("khachhang", new User("khachhang", "123456", "Khách Hàng Thân Thiết", "khachhang@gmail.com", "0909888999", "CUSTOMER", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"));

        // Danh sách các cuốn sách trong bộ nhớ (kèm nhà xuất bản, tồn kho, khuyến mãi)
        memoryBooks.add(new Book(1, "MS001", "Nhà Giả Kim (The Alchemist)", "Paulo Coelho", "NXB Hội Nhà Văn", 79000, 99000, "Văn học", 28, 4.9, 1420, "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500", "Cuốn sách kinh điển kể về hành trình theo đuổi ước mơ và lắng nghe tiếng gọi của trái tim của chàng chăn cừu Santiago.", "Giảm 20% - Tặng kèm Bookmark Santiago", true));
        memoryBooks.add(new Book(2, "MS002", "Đắc Nhân Tâm (How to Win Friends)", "Dale Carnegie", "NXB Tổng Hợp", 88000, 110000, "Kỹ năng sống", 35, 4.8, 2350, "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "Nghệ thuật thu phục lòng người đỉnh cao, cuốn sách gối đầu giường của hàng triệu độc giả trên toàn thế giới.", "Tặng Ebook kỹ năng giao tiếp", true));
        memoryBooks.add(new Book(3, "MS003", "Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin (Uncle Bob)", "NXB Thông Tin & Truyền Thông", 285000, 350000, "Công nghệ thông tin", 15, 4.9, 890, "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", "Cẩm nang kinh điển của mọi lập trình viên phần mềm để viết mã nguồn sạch sẽ, dễ bảo trì và mở rộng.", "Giảm ngay 65.000đ khi mua hôm nay", true));
        memoryBooks.add(new Book(4, "MS004", "Tư Duy Nhanh Và Chậm (Thinking, Fast and Slow)", "Daniel Kahneman", "NXB Thế Giới", 145000, 185000, "Tâm lý học", 12, 4.7, 640, "https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=500", "Giải thích hai hệ thống thúc đẩy cách chúng ta tư duy và đưa ra các quyết định trong cuộc sống.", "Tặng bookmark độc quyền", false));
        memoryBooks.add(new Book(5, "MS005", "Cha Giàu Cha Nghèo (Rich Dad Poor Dad)", "Robert T. Kiyosaki", "NXB Trẻ", 95000, 125000, "Kinh tế", 42, 4.8, 1890, "https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=500", "Bài học về tư duy tài chính độc lập và cách để đồng tiền làm việc cho chính bạn thay vì làm việc vì tiền.", "Ưu đãi combo sách kinh tế", true));
        memoryBooks.add(new Book(6, "MS006", "Muôn Kiếp Nhân Sinh (Phần 1 & 2)", "Nguyên Phong", "NXB Tổng Hợp", 168000, 210000, "Tâm linh & Đời sống", 18, 4.9, 1560, "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500", "Tác phẩm ghi lại những trải nghiệm tiền kiếp kỳ lạ và thông điệp sâu sắc về quy luật nhân quả của vũ trụ.", "Bản in đặc biệt bìa cứng", true));
        memoryBooks.add(new Book(7, "MS007", "Cây Cam Ngọt Của Tôi", "José Mauro de Vasconcelos", "NXB Hội Nhà Văn", 82000, 108000, "Văn học", 22, 4.9, 3200, "https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=500", "Câu chuyện cảm động rơi nước mắt về tuổi thơ hồn nhiên nhưng đầy vết thương của cậu bé Zezé.", "Tặng thiệp minh họa màu", true));
        memoryBooks.add(new Book(8, "MS008", "Thiết Kế Giải Thuật & Cấu Trúc Dữ Liệu", "Thomas H. Cormen", "NXB Khoa Học & Kỹ Thuật", 320000, 390000, "Công nghệ thông tin", 0, 4.8, 410, "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500", "Giáo trình toàn diện về giải thuật cơ bản và nâng cao dành cho sinh viên và kỹ sư công nghệ thông tin.", "Tạm hết hàng - Đang tái bản", false));
        memoryBooks.add(new Book(9, "MS009", "Thói Quen Nguyên Tử (Atomic Habits)", "James Clear", "NXB Thế Giới", 129000, 169000, "Kỹ năng sống", 30, 4.9, 2900, "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500", "Cách mạng hóa cuộc sống của bạn từ những thay đổi cực kỳ nhỏ bé nhưng mang lại hiệu quả phi thường mỗi ngày.", "Tặng biểu mẫu Habit Tracker 30 ngày", true));
        memoryBooks.add(new Book(10, "MS010", "Hoàng Tử Bé (The Little Prince)", "Antoine de Saint-Exupéry", "NXB Kim Đồng", 65000, 85000, "Văn học", 25, 4.9, 1820, "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "Kiệt tác văn học Pháp mang thông điệp triết lý sâu sắc về tình bạn, tình yêu và cái nhìn trong trẻo của trẻ thơ.", "Trọn bộ sticker nhân vật màu", true));
        memoryBooks.add(new Book(11, "MS011", "Tâm Lý Học Về Tiền (The Psychology of Money)", "Morgan Housel", "NXB Trẻ", 136000, 170000, "Kinh tế", 19, 4.9, 2150, "https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=500", "19 câu chuyện ngắn khám phá những cách kỳ lạ mà mọi người nghĩ về tiền bạc và cách quản lý tài chính thông minh.", "Giảm 20% cho khách hàng thân thiết", true));
        memoryBooks.add(new Book(12, "MS012", "Sapiens: Lược Sử Loài Người", "Yuval Noah Harari", "NXB Tri Thức", 195000, 250000, "Khoa học", 14, 4.9, 3400, "https://images.unsplash.com/photo-1447069387593-a5de0862481e?w=500", "Hành trình kỳ vĩ kể về lịch sử tiến hóa của loài người từ thời kỳ đồ đá cho đến kỷ nguyên hiện đại.", "Freeship đơn hàng trên 200k", true));
        memoryBooks.add(new Book(13, "MS013", "Khởi Nghiệp Tinh Gọn (The Lean Startup)", "Eric Ries", "NXB Lao Động", 125000, 160000, "Kinh tế", 11, 4.8, 980, "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500", "Phương pháp xây dựng doanh nghiệp đổi mới sáng tạo thành công vượt bậc trong thời đại biến động.", "Tặng tài liệu mẫu Pitch Deck", false));
        memoryBooks.add(new Book(14, "MS014", "The Pragmatic Programmer: 20th Anniversary Edition", "David Thomas, Andrew Hunt", "NXB Thông Tin & Truyền Thông", 310000, 380000, "Công nghệ thông tin", 8, 4.9, 760, "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500", "Cẩm nang từ các bậc thầy lập trình giúp bạn nâng tầm kỹ năng từ một thợ code thành kỹ sư phần mềm thực thụ.", "Tặng kèm cheat sheet phím tắt", true));
        memoryBooks.add(new Book(15, "MS015", "Dám Bị Ghét", "Kishimi Ichiro, Koga Fumitake", "NXB Nhã Nam", 98000, 125000, "Tâm lý học", 20, 4.7, 1950, "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=500", "Đối thoại triết học dựa trên tâm lý học Alfred Adler giúp bạn tìm thấy tự do và dũng khí sống thật với chính mình.", "Tặng kèm sổ tay mini", true));
        memoryBooks.add(new Book(16, "MS016", "Bố Già (The Godfather)", "Mario Puzo", "NXB Văn Học", 118000, 150000, "Văn học", 16, 4.9, 4100, "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "Tiểu thuyết kinh điển về thế giới mafia Ý-Mỹ, đỉnh cao của nghệ thuật xây dựng nhân vật và quyền lực.", "Bản dịch mới đầy đủ nhất", true));
        memoryBooks.add(new Book(17, "MS017", "Mắt Biếc", "Nguyễn Nhật Ánh", "NXB Trẻ", 72000, 90000, "Văn học", 32, 4.8, 5200, "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500", "Mối tình si ngốc nghếch, trong sáng nhưng da diết của Ngạn dành cho cô bạn thời thơ ấu có đôi mắt biếc.", "Tặng postcard nghệ thuật", true));
        memoryBooks.add(new Book(18, "MS018", "Từ Tốt Đến Vĩ Đại (Good to Great)", "Jim Collins", "NXB Trẻ", 149000, 195000, "Kinh tế", 9, 4.8, 1200, "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=500", "Nghiên cứu công phu về lý do tại sao một số công ty có thể tạo ra bước nhảy vọt phi thường còn số khác thì không.", "Giảm giá 24%", false));
        memoryBooks.add(new Book(19, "MS019", "Clean Architecture: A Craftsman's Guide", "Robert C. Martin (Uncle Bob)", "NXB Thông Tin & Truyền Thông", 295000, 360000, "Công nghệ thông tin", 8, 4.9, 680, "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500", "Quy tắc vàng về kiến trúc phần mềm giúp hệ thống bền vững, độc lập framework và dễ dàng kiểm thử.", "Giảm ngay 65.000đ khi đặt trước", true));
        memoryBooks.add(new Book(20, "MS020", "Sức Mạnh Của Hiện Tại (The Power of Now)", "Eckhart Tolle", "NXB Tổng Hợp", 105000, 135000, "Tâm linh & Đời sống", 0, 4.8, 1430, "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=500", "Hướng dẫn thức tỉnh tâm thức, giải phóng bản thân khỏi nỗi đau quá khứ và sự lo lắng về tương lai.", "Tạm hết hàng - Vui lòng chờ đợt mới", false));
        memoryBooks.add(new Book(21, "MS021", "7 Thói Quen Của Bạn Trẻ Thành Đạt", "Sean Covey", "NXB Trẻ", 95000, 120000, "Kỹ năng sống", 24, 4.8, 2670, "https://images.unsplash.com/photo-1507842229450-760773d528b8?w=500", "Chiếc la bàn chỉ đường giúp thanh thiếu niên rèn luyện nhân cách, xác định mục tiêu và gặt hái thành công.", "Tặng bookmark la bàn", true));
        memoryBooks.add(new Book(22, "MS022", "Đọc Vị Bất Kỳ Ai (You Can Read Anyone)", "David J. Lieberman", "NXB Thế Giới", 78000, 99000, "Tâm lý học", 17, 4.7, 1880, "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500", "Nắm bắt tâm lý, giải mã ngôn ngữ cơ thể và suy nghĩ của đối phương chỉ trong vài phút giao tiếp.", "Ưu đãi độc giả trẻ", false));
        memoryBooks.add(new Book(23, "MS023", "Vũ Trụ Trong Vỏ Hạt Dẻ (The Universe in a Nutshell)", "Stephen Hawking", "NXB Trẻ", 155000, 195000, "Khoa học", 0, 4.8, 1120, "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500", "Khám phá những biên giới kỳ diệu của vật lý lý thuyết: thuyết tương đối, lỗ đen và lý thuyết siêu dây.", "Tạm hết hàng trong kho", false));
        memoryBooks.add(new Book(24, "MS024", "Rừng Na Uy (Norwegian Wood)", "Haruki Murakami", "NXB Hội Nhà Văn", 110000, 140000, "Văn học", 21, 4.8, 3800, "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500", "Tác phẩm lừng danh Nhật Bản khắc họa nỗi cô đơn và sự chới với của tuổi trẻ giữa mất mát và trưởng thành.", "Tặng kèm bọc sách chuyên dụng", true));

        // Sách chi tiết theo hình ảnh thực tế
        memoryBooks.add(new Book(25, "8935278601425", "Tháo Dây Oan Trái - Nghệ Thuật Chuyển Hóa Cảm Xúc", "Thích Nhật Từ", "Saigon Books", 89000, 89000, "Tâm lý học", 25, 5.0, 486, "/images/thao-day-oan-trai.jpg", "Khi nhắc đến nghiệp, chúng ta sẽ nghĩ đến ba yếu tố tạo thành là tâm, khẩu và thân. Nghiệp từ tâm là thứ quyết định quả báo sau này, vì khi một hành động xấu tạo ra do vô tình chứ không hữu ý thì rất dễ được thông cảm và tha thứ. Tâm nghiệp xuất phát từ cảm xúc của con người. Ví như, khi có cảm xúc tích cực, yêu thương ai đó, chúng ta sẽ dễ dàng tạo ra những hành động tốt đẹp dành cho họ. Ngược lại, khi có cảm xúc tiêu cực, thù ghét, chúng ta lại dễ sinh sân hận mà tạo ra những hành vi gây thương tổn cho họ. Tam đắm, si mê và sân hận là những xúc cảm tiêu cực mà có lẽ bất kỳ ai trong chúng ta cũng đều có – đơn giản vì chúng ta vẫn còn là người phàm. Đây cũng là tiền đề dẫn đến những oan trái, khổ đau trong cuộc đời nếu tự thân chúng ta không biết hóa giải, đoạn trừ. Điều này cũng có nghĩa: Muốn đạt được đến cảm giác thanh thản, bình an, muốn chạm chân đến một bình nguyên tươi mát thì việc chuyển hóa những cảm xúc tiêu cực thành tích cực là yếu tố cần thiết nhất đối với mỗi con người. Tháo dây oan trái tập hợp những bài giảng của Thượng tọa Thích Nhật Từ, được điều chỉnh thành một tập sách chuyên sâu về đề tài chuyển hóa cảm xúc – nhất là chuyển hóa sân hận – như một món quà gửi tặng cho độc giả. Hy vọng cuốn sách này sẽ có giá trị với độc giả trên con đường tu sửa để giữ được hạnh phúc dài lâu cho chính mình và những người xung quanh!", "Sản phẩm 100% chính hãng - Freeship từ 250k", true));
        memoryBooks.add(new Book(26, "8935278601426", "Những Cô Gái Mất Tích", "Megan Miranda", "NXB Hội Nhà Văn", 158000, 158000, "Văn học", 0, 4.8, 142, "/images/featured-1.jpg", "Cuốn tiểu thuyết trinh thám tâm lý đầy kịch tính của Megan Miranda về sự mất tích bí ẩn của những cô gái trẻ trong thị trấn nhỏ.", "Tạm hết hàng", true));
        memoryBooks.add(new Book(27, "8935278601427", "Mexico Kỳ Án", "Robert Kerner", "NXB Công An Nhân Dân", 119000, 119000, "Trinh thám", 18, 4.7, 98, "/images/featured-2.jpg", "Vụ án bí ẩn ly kỳ kéo dài hàng thập kỷ tại đất nước Mexico với những tình tiết cân não và bất ngờ đến phút cuối.", "Tặng bookmark độc quyền", true));
        memoryBooks.add(new Book(28, "8935278601428", "Mệt Mỏi Không Phải Do Làm Việc Nhiều Mà Do Phương Pháp", "Nhiều tác giả", "Saigon Books", 88200, 98000, "Kỹ năng sống", 30, 4.9, 215, "/images/featured-3.jpg", "Phương pháp quản lý năng lượng và thời gian thông minh để bạn làm việc nhẹ nhàng, hiệu quả và không bị quá tải.", "Giảm 10% hôm nay", true));
        memoryBooks.add(new Book(29, "8935278601429", "Để Trở Thành Người Thú Vị", "Jessica Hagy", "NXB Trẻ", 103500, 115000, "Kỹ năng sống", 22, 4.8, 310, "/images/featured-4.jpg", "10 bước khám phá bản thân, bước ra khỏi vùng an toàn để sống một cuộc đời đầy cảm hứng và màu sắc thú vị.", "Giảm 10% hôm nay", true));
        memoryBooks.add(new Book(30, "8935278601430", "Gia Đình - Tranh Đấu Hay Buông Xuôi?", "Thích Nhật Từ", "Saigon Books", 89000, 89000, "Tâm linh & Đời sống", 20, 4.9, 178, "/images/related-1.jpg", "Nghệ thuật xây dựng và gìn giữ hạnh phúc gia đình dựa trên sự thấu hiểu, yêu thương và tha thứ của Thượng tọa Thích Nhật Từ.", "Tặng kèm bookmark Saigon Books", false));
        memoryBooks.add(new Book(31, "8935278601431", "Hạt Giống Tâm Hồn - Tập 16: Tìm Lại Bình Yên", "Nhiều tác giả", "First News - Trí Việt", 76000, 76000, "Kỹ năng sống", 45, 4.9, 520, "/images/related-2.jpg", "Những câu chuyện nuôi dưỡng tâm hồn, giúp người đọc tìm lại sự bình yên trong tâm trí giữa bộn bề lo toan của cuộc sống.", "Tặng kèm postcard thông điệp", true));
        memoryBooks.add(new Book(32, "8935278601432", "Hạt Giống Tâm Hồn - Tập 14: Góc Nhìn Diệu Kỳ Của Cuộc Sống", "Nhiều tác giả", "First News - Trí Việt", 76000, 76000, "Kỹ năng sống", 35, 4.8, 412, "/images/related-3.jpg", "Tập hợp những câu chuyện giàu tính nhân văn về góc nhìn lạc quan, sự biết ơn và niềm tin vào những điều kỳ diệu quanh ta.", "Tặng kèm postcard thông điệp", false));
        memoryBooks.add(new Book(33, "8935278601433", "Hạt Giống Tâm Hồn - Tập 11: Những Trải Nghiệm Cuộc Sống", "Nhiều tác giả", "First News - Trí Việt", 76000, 76000, "Kỹ năng sống", 28, 4.8, 389, "/images/related-4.jpg", "Chia sẻ chân thực về những va vấp, trải nghiệm và bài học quý giá trên hành trình trưởng thành của mỗi con người.", "Tặng kèm postcard thông điệp", false));

        // Sách truyện dài Nguyễn Nhật Ánh (Văn học)
        memoryBooks.add(new Book(34, "8935278601434", "Còn Chút Gì Để Nhớ (Tái Bản 2019)", "Nguyễn Nhật Ánh", "NXB Trẻ", 55000, 55000, "Văn học", 0, 4.9, 1280, "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500", "Tác phẩm truyện dài nổi tiếng của nhà văn Nguyễn Nhật Ánh về ký ức tuổi học trò đầy hoài niệm.", "Tạm hết hàng", false));
        memoryBooks.add(new Book(35, "8935278601435", "Chú Bé Rắc Rối (Tái Bản 2019)", "Nguyễn Nhật Ánh", "NXB Trẻ", 48000, 48000, "Văn học", 0, 4.8, 960, "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "Câu chuyện hài hước và tinh nghịch của tuổi học trò qua ngòi bút hóm hỉnh của Nguyễn Nhật Ánh.", "Tạm hết hàng", false));
        memoryBooks.add(new Book(36, "8935278601436", "Bong Bóng Lên Trời (Tái Bản 2019)", "Nguyễn Nhật Ánh", "NXB Trẻ", 49000, 49000, "Văn học", 0, 4.9, 1140, "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500", "Câu chuyện xúc động về tình bạn và nghị lực vượt khó của tuổi hoa niên.", "Tạm hết hàng", false));
        memoryBooks.add(new Book(37, "8935278601437", "Thiên Thần Nhỏ Của Tôi", "Nguyễn Nhật Ánh", "NXB Trẻ", 45000, 45000, "Văn học", 18, 4.8, 850, "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", "Khúc ca trong trẻo và lắng đọng về tuổi thơ và tình cảm gia đình.", "Sách bán chạy", true));
        memoryBooks.add(new Book(38, "8935278601438", "Phòng Trọ Ba Người", "Nguyễn Nhật Ánh", "NXB Trẻ", 48000, 48000, "Văn học", 24, 4.8, 730, "https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=500", "Cuộc sống sinh viên muôn màu, hài hước và giàu tình người nơi xóm trọ.", "Sách mới", true));

        // Khởi tạo địa chỉ (DIA_CHI): Khách hàng tự nhập khi đặt hàng hoặc thêm vào sổ địa chỉ

        // Khởi tạo mã giảm giá mẫu (MA_GIAM_GIA)
        memoryVouchers.add(new Voucher(1, "BOOKORA2026", "Giảm 30.000đ cho đơn từ 200.000đ", "Áp dụng cho toàn bộ đầu sách trên hệ thống Bookora", "FIXED", 30000, 200000, 0, "2026-01-01", "2026-12-31", true, 500, 12));
        memoryVouchers.add(new Voucher(2, "FREESHIP", "Miễn phí vận chuyển toàn quốc", "Freeship (trừ tối đa 30.000đ) cho đơn từ 250.000đ", "FIXED", 30000, 250000, 0, "2026-01-01", "2026-12-31", true, 1000, 45));
        memoryVouchers.add(new Voucher(3, "TECH30", "Giảm 30% Sách Công Nghệ & AI", "Giảm 30% tối đa 100.000đ cho đơn hàng từ 150.000đ", "PERCENT", 30, 150000, 100000, "2026-01-01", "2026-12-31", true, 200, 8));
        memoryVouchers.add(new Voucher(4, "TRI_THUC", "Giảm 15% Sách Kỹ Năng & Tâm Lý", "Giảm 15% tối đa 50.000đ cho đơn hàng từ 100.000đ", "PERCENT", 15, 100000, 50000, "2026-01-01", "2026-12-31", true, 300, 15));
        memoryVouchers.add(new Voucher(5, "HETHAN", "Mã Giảm Giá Đã Hết Hạn (Để kiểm thử luồng 4)", "Mã mẫu kiểm thử lỗi hết hạn", "FIXED", 50000, 100000, 0, "2023-01-01", "2023-12-31", true, 100, 5));
        memoryVouchers.add(new Voucher(6, "CHUADUNG", "Mã Tạm Khóa (Để kiểm thử luồng 4)", "Mã mẫu kiểm thử lỗi chưa mở", "FIXED", 20000, 50000, 0, "2026-01-01", "2026-12-31", false, 100, 0));

        // Khởi tạo phương thức thanh toán (PHUONG_THUC_TT)
        memoryPaymentMethods.add(new PaymentMethod(1, "COD", "Thanh toán tiền mặt khi nhận hàng (COD)", "Nhận sách, kiểm tra hàng rồi thanh toán cho shipper", true));
        memoryPaymentMethods.add(new PaymentMethod(2, "ONLINE", "Thanh toán trực tuyến (Chuyển khoản VietQR / MoMo / Thẻ ATM)", "Quét mã VietQR chuyển khoản nhanh 24/7 không mất phí", true));

        // Khởi tạo các đơn hàng mẫu cho tài khoản thử nghiệm (oanh, khachhang, admin)
        Order o1 = new Order(
            101, "ORD-20260925-1001", "oanh", 1, "Hoàng Oanh", "0912345678",
            "123 Đường Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
            "BOOKORA2026", 243000, 30000, 0, 213000,
            "COD", "Thanh toán khi nhận hàng (COD)", "PAID", "HOAN_THANH",
            "Giao trong giờ hành chính", null, "2026-09-25 14:30:00"
        );
        List<OrderItem> o1Items = new ArrayList<>();
        o1Items.add(new OrderItem(1, 101, 1, "MS001", "Nhà Giả Kim (The Alchemist)", "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500", 79000, 1, 79000));
        o1Items.add(new OrderItem(2, 101, 7, "MS007", "Cây Cam Ngọt Của Tôi", "https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=500", 82000, 2, 164000));
        o1.setItems(o1Items);
        memoryOrders.add(o1);

        Order o2 = new Order(
            102, "ORD-20260927-1002", "oanh", 1, "Hoàng Oanh", "0912345678",
            "123 Đường Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
            "FREESHIP", 285000, 30000, 0, 255000,
            "ONLINE", "Chuyển khoản VietQR", "PAID", "DANG_GIAO",
            "Gọi trước khi giao hàng", "VNQR179051003", "2026-09-27 09:15:00"
        );
        List<OrderItem> o2Items = new ArrayList<>();
        o2Items.add(new OrderItem(3, 102, 3, "MS003", "Clean Code: A Handbook of Agile Software Craftsmanship", "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", 285000, 1, 285000));
        o2.setItems(o2Items);
        memoryOrders.add(o2);

        Order o3 = new Order(
            103, "ORD-20260928-1003", "oanh", 1, "Hoàng Oanh", "0912345678",
            "123 Đường Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
            null, 89000, 0, 30000, 119000,
            "COD", "Thanh toán khi nhận hàng (COD)", "PENDING", "CHO_XAC_NHAN",
            "Đóng gói cẩn thận giúp tôi", null, "2026-09-28 10:00:00"
        );
        List<OrderItem> o3Items = new ArrayList<>();
        o3Items.add(new OrderItem(4, 103, 25, "8935278601425", "Tháo Dây Oan Trái - Nghệ Thuật Chuyển Hóa Cảm Xúc", "/images/thao-day-oan-trai.jpg", 89000, 1, 89000));
        o3.setItems(o3Items);
        memoryOrders.add(o3);

        Order o4 = new Order(
            104, "ORD-20260928-1004", "khachhang", 2, "Khách Hàng Thân Thiết", "0909888999",
            "456 Đường Lê Lợi, Quận 1, TP. Hồ Chí Minh",
            null, 88000, 0, 30000, 118000,
            "COD", "Thanh toán khi nhận hàng (COD)", "PAID", "HOAN_THANH",
            "Đơn mẫu khách hàng", null, "2026-09-28 11:00:00"
        );
        List<OrderItem> o4Items = new ArrayList<>();
        o4Items.add(new OrderItem(5, 104, 2, "MS002", "Đắc Nhân Tâm (How to Win Friends)", "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", 88000, 1, 88000));
        o4.setItems(o4Items);
        memoryOrders.add(o4);

        memoryCategories.clear();
        memoryCategories.add(new Category(1, "DM001", "Văn học", "Tiểu thuyết, truyện ngắn và các tác phẩm văn học trong nước và quốc tế kinh điển", "ACTIVE"));
        memoryCategories.add(new Category(2, "DM002", "Kỹ năng sống", "Sách rèn luyện bản thân, phát triển tư duy, giao tiếp và kỹ năng làm chủ cuộc sống", "ACTIVE"));
        memoryCategories.add(new Category(3, "DM003", "Công nghệ", "Cẩm nang lập trình, kiến trúc phần mềm, khoa học dữ liệu & công nghệ mới", "ACTIVE"));
        memoryCategories.add(new Category(4, "DM004", "Tâm lý học", "Khám phá hành vi, tư duy và nhận thức con người qua góc nhìn khoa học tâm lý", "ACTIVE"));
        memoryCategories.add(new Category(5, "DM005", "Kinh tế", "Quản trị kinh doanh, tư duy tài chính, làm giàu và khởi nghiệp tinh gọn", "ACTIVE"));
        memoryCategories.add(new Category(6, "DM006", "Tâm linh & Đời sống", "Sách nuôi dưỡng tâm hồn, thức tỉnh tâm thức và nghệ thuật chuyển hóa cảm xúc", "ACTIVE"));
        memoryCategories.add(new Category(7, "DM007", "Khoa học", "Khám phá tri thức vũ trụ, lịch sử loài người và khoa học tự nhiên", "ACTIVE"));
        memoryCategories.add(new Category(8, "DM008", "Trinh thám", "Tiểu thuyết trinh thám, vụ án bí ẩn và hình sự kịch tính", "ACTIVE"));
        memoryCategories.add(new Category(9, "DM009", "Sách thiếu nhi", "Truyện tranh, sách màu và tri thức bổ ích dành cho trẻ em và tuổi mới lớn", "ACTIVE"));
    }

    /**
     * Tìm kiếm người dùng theo username từ MySQL
     */
    public static User findUser(String username) {
        if (username == null) return null;
        String sql = "SELECT username, password, full_name, email, phone, role, avatar, status FROM users WHERE LOWER(username) = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String st = "ACTIVE";
                    try {
                        st = rs.getString("status");
                    } catch (SQLException ignored) {}
                    if (st == null || st.trim().isEmpty()) st = "ACTIVE";

                    return new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("role"),
                            rs.getString("avatar"),
                            st
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

        String sql = "INSERT INTO users (username, password, full_name, email, phone, role, avatar, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanUsername);
            ps.setString(2, newUser.getPassword());
            ps.setString(3, newUser.getFullName());
            ps.setString(4, (newUser.getEmail() != null && !newUser.getEmail().trim().isEmpty()) ? newUser.getEmail().trim() : null);
            ps.setString(5, (newUser.getPhone() != null && !newUser.getPhone().trim().isEmpty()) ? newUser.getPhone().trim() : null);
            ps.setString(6, newUser.getRole() != null ? newUser.getRole() : "CUSTOMER");
            ps.setString(7, newUser.getAvatar() != null ? newUser.getAvatar() : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
            ps.setString(8, newUser.getStatus() != null ? newUser.getStatus() : "ACTIVE");
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

    /**
     * Lấy danh sách tất cả tài khoản người dùng
     */
    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT username, password, full_name, email, phone, role, avatar, status FROM users ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String st = "ACTIVE";
                try {
                    st = rs.getString("status");
                } catch (SQLException ignored) {}
                if (st == null || st.trim().isEmpty()) st = "ACTIVE";

                list.add(new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getString("avatar"),
                        st
                ));
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL getAllUsers fallback: " + e.getMessage());
            list.addAll(memoryUsers.values());
        }
        if (list.isEmpty()) {
            list.addAll(memoryUsers.values());
        }
        return list;
    }

    /**
     * Cập nhật trạng thái tài khoản người dùng (ACTIVE / LOCKED)
     */
    public static boolean updateUserStatus(String username, String status) {
        if (username == null || username.trim().isEmpty()) return false;
        String cleanUser = username.trim().toLowerCase();
        String newStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ACTIVE";

        String sql = "UPDATE users SET status = ? WHERE LOWER(username) = ?";
        boolean updatedInDb = false;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, cleanUser);
            updatedInDb = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL updateUserStatus lỗi: " + e.getMessage());
        }

        User u = memoryUsers.get(cleanUser);
        if (u != null) {
            u.setStatus(newStatus);
        } else {
            User dbUser = findUser(cleanUser);
            if (dbUser != null) {
                dbUser.setStatus(newStatus);
                memoryUsers.put(cleanUser, dbUser);
            }
        }
        return updatedInDb || memoryUsers.containsKey(cleanUser);
    }

    /**
     * Lấy danh sách tất cả đơn hàng trên hệ thống
     */
    public static List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT id, order_code, username, recipient_name, recipient_phone, delivery_address, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, payment_method_code, payment_method_name, payment_status, order_status, note, transaction_id, created_at FROM don_hang ORDER BY id DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Order order = new Order(
                        rs.getInt("id"),
                        rs.getString("order_code"),
                        rs.getString("username"),
                        0,
                        rs.getString("recipient_name"),
                        rs.getString("recipient_phone"),
                        rs.getString("delivery_address"),
                        rs.getString("voucher_code"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("discount_amount"),
                        rs.getDouble("shipping_fee"),
                        rs.getDouble("total_amount"),
                        rs.getString("payment_method_code"),
                        rs.getString("payment_method_name"),
                        rs.getString("payment_status"),
                        rs.getString("order_status"),
                        rs.getString("note"),
                        rs.getString("transaction_id"),
                        rs.getString("created_at")
                );
                list.add(order);
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL getAllOrders fallback: " + e.getMessage());
            list.addAll(memoryOrders);
        }
        if (list.isEmpty()) {
            list.addAll(memoryOrders);
        }
        return list;
    }

    /**
     * Cập nhật thông tin cá nhân của người dùng vào MySQL và bộ nhớ dự phòng
     */
    public static boolean updateUserProfile(String username, String fullName, String email, String phone, String avatar) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String cleanUsername = username.trim().toLowerCase();
        User existing = findUser(cleanUsername);
        if (existing == null) {
            return false;
        }

        if (fullName != null && !fullName.trim().isEmpty()) existing.setFullName(fullName.trim());
        existing.setEmail((email != null && !email.trim().isEmpty()) ? email.trim() : null);
        existing.setPhone((phone != null && !phone.trim().isEmpty()) ? phone.trim() : null);
        if (avatar != null && !avatar.trim().isEmpty()) existing.setAvatar(avatar.trim());

        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, avatar = ? WHERE LOWER(username) = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, existing.getFullName());
            ps.setString(2, existing.getEmail());
            ps.setString(3, existing.getPhone());
            ps.setString(4, existing.getAvatar());
            ps.setString(5, cleanUsername);
            int rows = ps.executeUpdate();
            memoryUsers.put(cleanUsername, existing);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL updateUserProfile lỗi (" + e.getMessage() + "). Lưu vào bộ nhớ tạm.");
            memoryUsers.put(cleanUsername, existing);
            return true;
        }
    }

    /**
     * Thay đổi mật khẩu người dùng
     * @return null nếu thành công, hoặc chuỗi thông báo lỗi nếu thất bại
     */
    public static String changePassword(String username, String oldPassword, String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            return "Tên đăng nhập không hợp lệ!";
        }
        if (oldPassword == null || oldPassword.isEmpty()) {
            return "Vui lòng nhập mật khẩu hiện tại!";
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return "Vui lòng nhập mật khẩu mới!";
        }

        String cleanUsername = username.trim().toLowerCase();
        User existing = findUser(cleanUsername);
        if (existing == null) {
            return "Tài khoản không tồn tại trong hệ thống!";
        }

        if (!existing.getPassword().equals(oldPassword)) {
            return "Mật khẩu hiện tại không chính xác!";
        }

        if (oldPassword.equals(newPassword)) {
            return "Mật khẩu mới không được trùng với mật khẩu hiện tại!";
        }

        String sql = "UPDATE users SET password = ? WHERE LOWER(username) = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, cleanUsername);
            ps.executeUpdate();
            existing.setPassword(newPassword);
            memoryUsers.put(cleanUsername, existing);
            return null; // Thành công
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL changePassword lỗi (" + e.getMessage() + "). Lưu vào bộ nhớ tạm.");
            existing.setPassword(newPassword);
            memoryUsers.put(cleanUsername, existing);
            return null; // Thành công trong bộ nhớ tạm
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
     * Lấy danh sách toàn bộ sách từ MySQL (kèm dự phòng bộ nhớ)
     */
    public static List<Book> getAllBooks() {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT id, code, title, author, publisher, price, original_price, category, stock, rating, review_count, image, description, promotion, is_bestseller, publish_year, page_count, weight, dimensions, cover_format FROM books ORDER BY id ASC";

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
        String sql = "SELECT id, code, title, author, publisher, price, original_price, category, stock, rating, review_count, image, description, promotion, is_bestseller, publish_year, page_count, weight, dimensions, cover_format FROM books WHERE id = ?";

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
     * Lấy danh sách sản phẩm liên quan (gợi ý theo danh mục / sách mà khách hàng đang quan tâm)
     */
    public static List<Book> getRelatedBooks(int currentBookId, String category, int limit) {
        List<Book> result = new ArrayList<>();
        List<Book> all = getAllBooks();

        // Nếu là sách Tháo Dây Oan Trái (ID 25), ưu tiên chọn đúng 4 cuốn sách liên quan như trong thiết kế mẫu
        if (currentBookId == 25) {
            int[] specialIds = {30, 31, 32, 33};
            for (int sid : specialIds) {
                for (Book b : all) {
                    if (b.getId() == sid && !result.contains(b)) {
                        result.add(b);
                        break;
                    }
                }
            }
            if (result.size() >= limit) return result;
        }

        // 1. Lọc theo cùng danh mục (khác sách đang xem)
        if (category != null && !category.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(category)) {
            for (Book b : all) {
                if (b.getId() != currentBookId && category.equalsIgnoreCase(b.getCategory()) && !result.contains(b)) {
                    result.add(b);
                    if (result.size() >= limit) return result;
                }
            }
        }

        // 2. Nếu chưa đủ limit, gợi ý thêm các cuốn sách nổi bật khác
        for (Book b : all) {
            if (b.getId() != currentBookId && !result.contains(b)) {
                result.add(b);
                if (result.size() >= limit) break;
            }
        }
        return result;
    }

    /**
     * Lấy danh sách sản phẩm nổi bật (SẢN PHẨM NỔI BẬT cho thanh bên)
     */
    public static List<Book> getFeaturedBooks(int limit) {
        List<Book> result = new ArrayList<>();
        List<Book> all = getAllBooks();

        // Ưu tiên 4 cuốn sách nổi bật theo đúng thiết kế mẫu: ID 26, 27, 28, 29
        int[] priorityIds = {26, 27, 28, 29};
        for (int pid : priorityIds) {
            for (Book b : all) {
                if (b.getId() == pid && !result.contains(b)) {
                    result.add(b);
                    break;
                }
            }
        }

        // Nếu chưa đủ limit, lấy thêm các sách bán chạy
        if (result.size() < limit) {
            for (Book b : all) {
                if (b.isBestSeller() && !result.contains(b)) {
                    result.add(b);
                    if (result.size() >= limit) break;
                }
            }
        }
        return result;
    }

    /**
     * Tìm kiếm và lọc sách cơ bản (tương thích ngược)
     */
    public static List<Book> searchBooks(String keyword, String category) {
        return searchBooks(keyword, category, null, null, null, null, null);
    }

    /**
     * Tìm kiếm và lọc sách đa tiêu chí theo đúng Use Case của BA:
     * - Từ khóa tìm kiếm: Tiêu đề sách, Tác giả, Nhà xuất bản, Danh mục, Mã sách
     * - Bộ lọc: Danh mục, Tác giả, Nhà xuất bản, Khoảng giá (minPrice - maxPrice), Trạng thái tồn kho (KHO)
     */
    public static List<Book> searchBooks(String keyword, String category, String author, String publisher, 
                                        Double minPrice, Double maxPrice, String stockStatus) {
        List<Book> list = new ArrayList<>();
        String kw = keyword == null ? "" : keyword.trim();
        String cat = category == null ? "" : category.trim();
        String auth = author == null ? "" : author.trim();
        String pub = publisher == null ? "" : publisher.trim();
        String stock = stockStatus == null ? "" : stockStatus.trim();

        StringBuilder sql = new StringBuilder("SELECT id, code, title, author, publisher, price, original_price, category, stock, rating, review_count, image, description, promotion, is_bestseller, publish_year, page_count, weight, dimensions, cover_format FROM books WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!kw.isEmpty()) {
            String kwNorm = removeDiacritics(kw);
            boolean isGenericBook = "sach".equals(kwNorm) || "book".equals(kwNorm);
            if (!isGenericBook) {
                sql.append("AND (LOWER(title) LIKE ? OR LOWER(author) LIKE ? OR LOWER(publisher) LIKE ? OR LOWER(code) LIKE ? OR LOWER(category) LIKE ? OR LOWER(description) LIKE ?) ");
                String kwPattern = "%" + kw.toLowerCase() + "%";
                params.add(kwPattern);
                params.add(kwPattern);
                params.add(kwPattern);
                params.add(kwPattern);
                params.add(kwPattern);
                params.add(kwPattern);
            }
        }

        if (!cat.isEmpty() && !"Tất cả".equalsIgnoreCase(cat)) {
            sql.append("AND category = ? ");
            params.add(cat);
        }

        if (!auth.isEmpty() && !"Tất cả".equalsIgnoreCase(auth)) {
            sql.append("AND author = ? ");
            params.add(auth);
        }

        if (!pub.isEmpty() && !"Tất cả".equalsIgnoreCase(pub)) {
            sql.append("AND publisher = ? ");
            params.add(pub);
        }

        if (minPrice != null && minPrice > 0) {
            sql.append("AND price >= ? ");
            params.add(minPrice);
        }

        if (maxPrice != null && maxPrice > 0) {
            sql.append("AND price <= ? ");
            params.add(maxPrice);
        }

        if ("in_stock".equalsIgnoreCase(stock)) {
            sql.append("AND stock > 0 ");
        } else if ("out_of_stock".equalsIgnoreCase(stock)) {
            sql.append("AND stock <= 0 ");
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
                boolean matchKw = kw.isEmpty()
                        || b.getTitle().toLowerCase().contains(kw.toLowerCase())
                        || b.getAuthor().toLowerCase().contains(kw.toLowerCase())
                        || b.getPublisher().toLowerCase().contains(kw.toLowerCase())
                        || b.getCode().toLowerCase().contains(kw.toLowerCase())
                        || b.getCategory().toLowerCase().contains(kw.toLowerCase());

                boolean matchCat = cat.isEmpty() || "Tất cả".equalsIgnoreCase(cat) || b.getCategory().equalsIgnoreCase(cat);
                boolean matchAuth = auth.isEmpty() || "Tất cả".equalsIgnoreCase(auth) || b.getAuthor().equalsIgnoreCase(auth);
                boolean matchPub = pub.isEmpty() || "Tất cả".equalsIgnoreCase(pub) || b.getPublisher().equalsIgnoreCase(pub);

                boolean matchMinPrice = (minPrice == null || minPrice <= 0 || b.getPrice() >= minPrice);
                boolean matchMaxPrice = (maxPrice == null || maxPrice <= 0 || b.getPrice() <= maxPrice);

                boolean matchStock = true;
                if ("in_stock".equalsIgnoreCase(stock)) {
                    matchStock = (b.getStock() > 0);
                } else if ("out_of_stock".equalsIgnoreCase(stock)) {
                    matchStock = (b.getStock() <= 0);
                }

                if (matchKw && matchCat && matchAuth && matchPub && matchMinPrice && matchMaxPrice && matchStock) {
                    fallback.add(b);
                }
            }
            return fallback;
        }
    }

    /**
     * Lấy danh sách đối tượng Category từ MySQL / Memory kèm tính toán số lượng sách
     */
    public static List<Category> getAllCategoriesList() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, code, name, description, status FROM categories ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Category(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL getAllCategoriesList error: " + e.getMessage());
            list.addAll(memoryCategories);
        }

        if (list.isEmpty()) {
            list.addAll(memoryCategories);
        }

        // Tính toán số lượng sách của từng danh mục
        List<Book> allBooks = getAllBooks();
        for (Category c : list) {
            int count = 0;
            if (c.getName() != null) {
                String cName = c.getName().trim().toLowerCase();
                for (Book b : allBooks) {
                    if (b.getCategory() != null && b.getCategory().trim().toLowerCase().equals(cName)) {
                        count++;
                    }
                }
            }
            c.setBookCount(count);
        }

        return list;
    }

    public static Category getCategoryById(int id) {
        List<Category> all = getAllCategoriesList();
        for (Category c : all) {
            if (c.getId() == id) return c;
        }
        return null;
    }

    public static Category getCategoryByCode(String code) {
        if (code == null) return null;
        List<Category> all = getAllCategoriesList();
        for (Category c : all) {
            if (code.trim().equalsIgnoreCase(c.getCode())) return c;
        }
        return null;
    }

    public static Category getCategoryByName(String name) {
        if (name == null) return null;
        List<Category> all = getAllCategoriesList();
        for (Category c : all) {
            if (name.trim().equalsIgnoreCase(c.getName())) return c;
        }
        return null;
    }

    public static boolean addCategory(Category newCat) {
        if (newCat == null || newCat.getName() == null || newCat.getName().trim().isEmpty()) {
            return false;
        }
        String cleanName = newCat.getName().trim();

        // Kiểm tra trùng tên (Requirement 6)
        if (getCategoryByName(cleanName) != null) {
            return false;
        }

        String cleanCode = (newCat.getCode() != null && !newCat.getCode().trim().isEmpty())
                ? newCat.getCode().trim().toUpperCase()
                : "DM" + String.format("%03d", getAllCategoriesList().size() + 1);

        String sql = "INSERT INTO categories (code, name, description, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanCode);
            ps.setString(2, cleanName);
            ps.setString(3, newCat.getDescription() != null ? newCat.getDescription().trim() : "");
            ps.setString(4, newCat.getStatus() != null ? newCat.getStatus() : "ACTIVE");
            int rows = ps.executeUpdate();
            if (rows > 0) {
                newCat.setCode(cleanCode);
                newCat.setName(cleanName);
                memoryCategories.add(newCat);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL addCategory error: " + e.getMessage());
            newCat.setCode(cleanCode);
            newCat.setName(cleanName);
            memoryCategories.add(newCat);
            return true;
        }

        return false;
    }

    public static boolean updateCategory(Category cat) {
        if (cat == null || cat.getName() == null || cat.getName().trim().isEmpty()) {
            return false;
        }
        String cleanName = cat.getName().trim();

        // Kiểm tra trùng tên với các danh mục khác (Requirement 7)
        Category existingByName = getCategoryByName(cleanName);
        if (existingByName != null && existingByName.getId() != cat.getId()) {
            return false;
        }

        Category oldCat = getCategoryById(cat.getId());
        String oldName = oldCat != null ? oldCat.getName() : null;

        String sql = "UPDATE categories SET name = ?, description = ?, status = ? WHERE id = ?";
        boolean updatedInDb = false;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanName);
            ps.setString(2, cat.getDescription() != null ? cat.getDescription().trim() : "");
            ps.setString(3, cat.getStatus() != null ? cat.getStatus() : "ACTIVE");
            ps.setInt(4, cat.getId());
            updatedInDb = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL updateCategory error: " + e.getMessage());
        }

        for (Category c : memoryCategories) {
            if (c.getId() == cat.getId()) {
                c.setName(cleanName);
                c.setDescription(cat.getDescription());
                c.setStatus(cat.getStatus());
                break;
            }
        }

        // Đổi tên thể loại trong sách nếu tên danh mục thay đổi (Requirement 13)
        if (oldName != null && !oldName.equalsIgnoreCase(cleanName)) {
            String sqlBookUpdate = "UPDATE books SET category = ? WHERE LOWER(category) = ?";
            try (Connection conn = DBContext.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlBookUpdate)) {
                ps.setString(1, cleanName);
                ps.setString(2, oldName.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException ignored) {}

            for (Book b : memoryBooks) {
                if (b.getCategory() != null && b.getCategory().equalsIgnoreCase(oldName)) {
                    b.setCategory(cleanName);
                }
            }
        }

        return updatedInDb || true;
    }

    public static List<Book> getBooksByCategory(String categoryName) {
        List<Book> list = new ArrayList<>();
        if (categoryName == null || categoryName.trim().isEmpty()) return list;
        String cleanCat = categoryName.trim().toLowerCase();
        for (Book b : getAllBooks()) {
            if (b.getCategory() != null && b.getCategory().trim().toLowerCase().equals(cleanCat)) {
                list.add(b);
            }
        }
        return list;
    }

    public static boolean deleteCategory(int id) {
        Category cat = getCategoryById(id);
        if (cat == null) return false;

        // Kiểm tra nếu danh mục đang được sử dụng bởi sách (Requirement 9 & 10)
        List<Book> booksInCat = getBooksByCategory(cat.getName());
        if (booksInCat != null && !booksInCat.isEmpty()) {
            return false; // KHÔNG CHO PHÉP XÓA DANH MỤC ĐANG CÓ SÁCH
        }

        String sql = "DELETE FROM categories WHERE id = ?";
        boolean deletedInDb = false;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            deletedInDb = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL deleteCategory error: " + e.getMessage());
        }

        memoryCategories.removeIf(c -> c.getId() == id);
        return deletedInDb || true;
    }

    /**
     * Lấy danh sách các tên thể loại sách cho Customer site (Đồng bộ trực tiếp từ bảng categories)
     */
    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("Tất cả");

        List<Category> catList = getAllCategoriesList();
        for (Category c : catList) {
            if (c.getName() != null && "ACTIVE".equalsIgnoreCase(c.getStatus()) && !categories.contains(c.getName())) {
                categories.add(c.getName());
            }
        }
        return categories;
    }

    /**
     * Lấy danh sách các tác giả từ MySQL (kèm dự phòng bộ nhớ)
     */
    public static List<String> getAuthors() {
        List<String> authors = new ArrayList<>();
        authors.add("Tất cả");
        String sql = "SELECT DISTINCT author FROM books ORDER BY author ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String a = rs.getString("author");
                if (a != null && !authors.contains(a)) {
                    authors.add(a);
                }
            }
            if (authors.size() > 1) {
                return authors;
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL getAuthors lỗi (" + e.getMessage() + "). Dùng bộ nhớ tạm.");
        }

        for (Book b : memoryBooks) {
            if (!authors.contains(b.getAuthor())) {
                authors.add(b.getAuthor());
            }
        }
        return authors;
    }

    /**
     * Lấy danh sách các nhà xuất bản từ MySQL (kèm dự phòng bộ nhớ)
     */
    public static List<String> getPublishers() {
        List<String> publishers = new ArrayList<>();
        publishers.add("Tất cả");
        String sql = "SELECT DISTINCT publisher FROM books ORDER BY publisher ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String p = rs.getString("publisher");
                if (p != null && !publishers.contains(p)) {
                    publishers.add(p);
                }
            }
            if (publishers.size() > 1) {
                return publishers;
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL getPublishers lỗi (" + e.getMessage() + "). Dùng bộ nhớ tạm.");
        }

        for (Book b : memoryBooks) {
            if (!publishers.contains(b.getPublisher())) {
                publishers.add(b.getPublisher());
            }
        }
        return publishers;
    }

    /**
     * Thêm sách mới vào MySQL và bộ nhớ dự phòng
     */
    public static boolean addBook(Book book) {
        if (book == null || book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            return false;
        }

        String sql = "INSERT INTO books (code, title, author, publisher, price, original_price, category, stock, rating, review_count, image, description, promotion, is_bestseller, publish_year, page_count, weight, dimensions, cover_format) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getCode());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getPublisher());
            ps.setDouble(5, book.getPrice());
            ps.setDouble(6, book.getOriginalPrice());
            ps.setString(7, book.getCategory());
            ps.setInt(8, book.getStock());
            ps.setDouble(9, book.getRating() > 0 ? book.getRating() : 5.0);
            ps.setInt(10, book.getReviewCount());
            ps.setString(11, book.getImage());
            ps.setString(12, book.getDescription());
            ps.setString(13, book.getPromotion());
            ps.setBoolean(14, book.isBestSeller());
            ps.setInt(15, book.getPublishYear());
            ps.setInt(16, book.getPageCount());
            ps.setInt(17, book.getWeight());
            ps.setString(18, book.getDimensions());
            ps.setString(19, book.getCoverFormat());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        book.setId(rs.getInt(1));
                    }
                }
            }
            memoryBooks.add(book);
            return true;
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL addBook error: " + e.getMessage() + ". Saving to memory.");
            if (book.getId() <= 0) {
                int maxId = memoryBooks.stream().mapToInt(Book::getId).max().orElse(33);
                book.setId(maxId + 1);
            }
            memoryBooks.add(book);
            return true;
        }
    }

    /**
     * Cập nhật thông tin sách vào MySQL và bộ nhớ dự phòng
     */
    public static boolean updateBook(Book book) {
        if (book == null || book.getId() <= 0 || book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            return false;
        }

        String sql = "UPDATE books SET code = ?, title = ?, author = ?, publisher = ?, price = ?, original_price = ?, category = ?, stock = ?, image = ?, description = ?, promotion = ?, is_bestseller = ?, publish_year = ?, page_count = ?, weight = ?, dimensions = ?, cover_format = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getCode());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getPublisher());
            ps.setDouble(5, book.getPrice());
            ps.setDouble(6, book.getOriginalPrice());
            ps.setString(7, book.getCategory());
            ps.setInt(8, book.getStock());
            ps.setString(9, book.getImage());
            ps.setString(10, book.getDescription());
            ps.setString(11, book.getPromotion());
            ps.setBoolean(12, book.isBestSeller());
            ps.setInt(13, book.getPublishYear());
            ps.setInt(14, book.getPageCount());
            ps.setInt(15, book.getWeight());
            ps.setString(16, book.getDimensions());
            ps.setString(17, book.getCoverFormat());
            ps.setInt(18, book.getId());

            ps.executeUpdate();

            for (int i = 0; i < memoryBooks.size(); i++) {
                if (memoryBooks.get(i).getId() == book.getId()) {
                    memoryBooks.set(i, book);
                    break;
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL updateBook error: " + e.getMessage() + ". Updating memory.");
            for (int i = 0; i < memoryBooks.size(); i++) {
                if (memoryBooks.get(i).getId() == book.getId()) {
                    memoryBooks.set(i, book);
                    break;
                }
            }
            return true;
        }
    }

    /**
     * Cập nhật số lượng tồn kho của sách
     */
    public static boolean updateStock(int bookId, int newStock) {
        if (newStock < 0) return false;

        Book book = getBookById(bookId);
        if (book != null) {
            book.setStock(newStock);
        }

        String sql = "UPDATE books SET stock = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, bookId);
            ps.executeUpdate();

            for (Book b : memoryBooks) {
                if (b.getId() == bookId) {
                    b.setStock(newStock);
                    break;
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL updateStock error: " + e.getMessage());
            for (Book b : memoryBooks) {
                if (b.getId() == bookId) {
                    b.setStock(newStock);
                    break;
                }
            }
            return true;
        }
    }

    /**
     * Xóa sách khỏi MySQL và bộ nhớ dự phòng.
     * Trả về NULL nếu thành công, hoặc thông báo lỗi nếu sách đang ở trong đơn hàng chưa hoàn tất.
     */
    public static String deleteBook(int bookId) {
        Book book = getBookById(bookId);
        if (book == null) {
            return "Sách không tồn tại trong hệ thống!";
        }

        // 1. Kiểm tra nếu sách đang nằm trong đơn hàng chưa hoàn tất
        String checkSql = "SELECT COUNT(*) FROM ct_don_hang ct JOIN don_hang dh ON ct.order_id = dh.id WHERE ct.book_id = ? AND UPPER(dh.order_status) NOT IN ('HOAN_THANH', 'DA_HUY')";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return "Không thể xóa sách vì sách đang được sử dụng trong đơn hàng chưa hoàn tất.";
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL deleteBook check order error: " + e.getMessage());
        }

        // 2. Xóa sách khỏi database
        String deleteSql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, bookId);
            ps.executeUpdate();
            memoryBooks.removeIf(b -> b.getId() == bookId);
            return null; // Thành công
        } catch (SQLException e) {
            System.err.println("⚠️ MySQL deleteBook error: " + e.getMessage() + ". Xóa khỏi bộ nhớ.");
            memoryBooks.removeIf(b -> b.getId() == bookId);
            return null;
        }
    }

    /**
     * Lớp đại diện cho chương trình khuyến mãi (KHUYEN_MAI)
     */
    public static class PromotionItem {
        private String code;
        private String title;
        private String applicableCategory;
        private String description;

        public PromotionItem(String code, String title, String applicableCategory, String description) {
            this.code = code;
            this.title = title;
            this.applicableCategory = applicableCategory;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getTitle() { return title; }
        public String getApplicableCategory() { return applicableCategory; }
        public String getDescription() { return description; }
    }

    /**
     * Lấy danh sách các chương trình khuyến mãi đang áp dụng
     */
    public static List<PromotionItem> getPromotions() {
        List<PromotionItem> promos = new ArrayList<>();
        String sql = "SELECT code, title, applicable_category, description FROM khuyen_mai";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                promos.add(new PromotionItem(
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("applicable_category"),
                        rs.getString("description")
                ));
            }
            if (!promos.isEmpty()) {
                return promos;
            }
        } catch (SQLException e) {
            // Không ngắt, dùng danh sách tĩnh mặc định bên dưới
        }

        promos.add(new PromotionItem("KM_VANHOC", "Ưu Đãi Sách Văn Học Mùa Thu", "Văn học", "Giảm 20% cho toàn bộ tiểu thuyết kinh điển"));
        promos.add(new PromotionItem("KM_TECH2026", "Tháng Công Nghệ Tri Thức", "Công nghệ", "Ưu đãi 25% cẩm nang lập trình & kỹ thuật phần mềm"));
        promos.add(new PromotionItem("KM_KINHTE", "Đột Phá Tư Duy Tài Chính", "Kinh tế", "Tặng kèm sổ tay tài chính độc quyền"));
        promos.add(new PromotionItem("KM_FREESHIP", "Miễn Phí Vận Chuyển Toàn Quốc", "Tất cả", "Freeship cho đơn hàng từ 250.000 đ"));
        return promos;
    }

    private static Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        String code = "";
        try {
            code = rs.getString("code");
        } catch (SQLException ignored) {}
        if (code == null || code.trim().isEmpty()) {
            code = String.format("MS%03d", rs.getInt("id"));
        }

        String publisher = "NXB Trẻ";
        try {
            String p = rs.getString("publisher");
            if (p != null && !p.trim().isEmpty()) publisher = p;
        } catch (SQLException ignored) {}

        int stock = 10;
        try {
            stock = rs.getInt("stock");
        } catch (SQLException ignored) {}

        String promotion = "Tặng bookmark độc quyền";
        try {
            String pr = rs.getString("promotion");
            if (pr != null && !pr.trim().isEmpty()) promotion = pr;
        } catch (SQLException ignored) {}

        int publishYear = 2024;
        try { publishYear = rs.getInt("publish_year"); } catch (SQLException ignored) {}
        if (publishYear <= 0) publishYear = 2024;

        int pageCount = 250;
        try { pageCount = rs.getInt("page_count"); } catch (SQLException ignored) {}
        if (pageCount <= 0) pageCount = 250;

        int weight = 350;
        try { weight = rs.getInt("weight"); } catch (SQLException ignored) {}
        if (weight <= 0) weight = 350;

        String dimensions = "21 x 13.5 x 1.2 cm";
        try {
            String d = rs.getString("dimensions");
            if (d != null && !d.trim().isEmpty()) dimensions = d;
        } catch (SQLException ignored) {}

        String coverFormat = "Bìa Mềm";
        try {
            String cf = rs.getString("cover_format");
            if (cf != null && !cf.trim().isEmpty()) coverFormat = cf;
        } catch (SQLException ignored) {}

        return new Book(
                rs.getInt("id"),
                code,
                rs.getString("title"),
                rs.getString("author"),
                publisher,
                rs.getDouble("price"),
                rs.getDouble("original_price"),
                rs.getString("category"),
                stock,
                rs.getDouble("rating"),
                rs.getInt("review_count"),
                rs.getString("image"),
                rs.getString("description"),
                promotion,
                rs.getBoolean("is_bestseller"),
                publishYear,
                pageCount,
                weight,
                dimensions,
                coverFormat
        );
    }

    /**
     * Cấu trúc kết quả gợi ý tìm kiếm:
     * Gợi ý từ SACH, DANH_MUC, TAC_GIA và KHUYEN_MAI kèm tổng số kết quả khớp
     */
    public static class SearchSuggestionResult {
        private final List<Book> books;
        private final List<String> categories;
        private final List<String> authors;
        private final List<PromotionItem> promotions;
        private final int totalMatches;

        public SearchSuggestionResult(List<Book> books, List<String> categories, List<String> authors, List<PromotionItem> promotions, int totalMatches) {
            this.books = books;
            this.categories = categories;
            this.authors = authors;
            this.promotions = promotions;
            this.totalMatches = totalMatches;
        }

        public SearchSuggestionResult(List<Book> books, List<String> categories, List<String> authors, List<PromotionItem> promotions) {
            this(books, categories, authors, promotions, books.size());
        }

        public List<Book> getBooks() { return books; }
        public List<String> getCategories() { return categories; }
        public List<String> getAuthors() { return authors; }
        public List<PromotionItem> getPromotions() { return promotions; }
        public int getTotalMatches() { return totalMatches; }
    }

    /**
     * Truy xuất dữ liệu gợi ý trực tiếp khi người dùng nhập từ khóa
     */
    public static SearchSuggestionResult getSearchSuggestions(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Book> matchedBooks = new ArrayList<>();
        List<String> matchedCats = new ArrayList<>();
        List<String> matchedAuthors = new ArrayList<>();
        List<PromotionItem> matchedPromos = new ArrayList<>();

        List<Book> all = getAllBooks();
        List<PromotionItem> allPromos = getPromotions();
        int totalMatches = 0;

        if (q.isEmpty()) {
            // Khi chưa nhập từ khóa: gợi ý 5 sách nổi bật, tất cả khuyến mãi, 4 danh mục phổ biến
            for (Book b : all) {
                if (b.isBestSeller() && matchedBooks.size() < 5) {
                    matchedBooks.add(b);
                }
            }
            totalMatches = all.size();
            matchedPromos.addAll(allPromos);
            List<String> cats = getCategories();
            for (String c : cats) {
                if (!"Tất cả".equalsIgnoreCase(c) && matchedCats.size() < 4) {
                    matchedCats.add(c);
                }
            }
        } else {
            String qNorm = removeDiacritics(q);
            boolean isGenericBook = "sach".equals(qNorm) || "book".equals(qNorm);

            // Khi đã nhập: tìm kiếm sách phù hợp (hiển thị 5 cuốn đầu tiên, tính tổng số sách khớp)
            for (Book b : all) {
                String titleNorm = removeDiacritics(b.getTitle());
                String authorNorm = removeDiacritics(b.getAuthor());
                String catNorm = removeDiacritics(b.getCategory());
                String descNorm = removeDiacritics(b.getDescription());

                boolean bookMatches = isGenericBook
                        || b.getTitle().toLowerCase().contains(q) 
                        || b.getAuthor().toLowerCase().contains(q) 
                        || b.getCode().toLowerCase().contains(q)
                        || b.getPublisher().toLowerCase().contains(q)
                        || b.getCategory().toLowerCase().contains(q)
                        || (b.getDescription() != null && b.getDescription().toLowerCase().contains(q))
                        || (b.getPromotion() != null && b.getPromotion().toLowerCase().contains(q))
                        || titleNorm.contains(qNorm)
                        || authorNorm.contains(qNorm)
                        || catNorm.contains(qNorm)
                        || descNorm.contains(qNorm);

                if (bookMatches) {
                    totalMatches++;
                    if (matchedBooks.size() < 5) {
                        matchedBooks.add(b);
                    }
                }

                // Tìm tác giả phù hợp
                if (b.getAuthor().toLowerCase().contains(q) && !matchedAuthors.contains(b.getAuthor())) {
                    if (matchedAuthors.size() < 3) {
                        matchedAuthors.add(b.getAuthor());
                    }
                }
                // Tìm danh mục phù hợp
                if (b.getCategory().toLowerCase().contains(q) && !matchedCats.contains(b.getCategory())) {
                    if (matchedCats.size() < 3) {
                        matchedCats.add(b.getCategory());
                    }
                }
            }

            // Tìm khuyến mãi phù hợp
            for (PromotionItem p : allPromos) {
                if (p.getTitle().toLowerCase().contains(q) 
                        || p.getDescription().toLowerCase().contains(q) 
                        || (p.getApplicableCategory() != null && p.getApplicableCategory().toLowerCase().contains(q))) {
                    matchedPromos.add(p);
                }
            }
            if (matchedPromos.isEmpty() && !allPromos.isEmpty()) {
                matchedPromos.add(allPromos.get(0));
            }
        }

        return new SearchSuggestionResult(matchedBooks, matchedCats, matchedAuthors, matchedPromos, totalMatches);
    }

    /**
     * Chuẩn hóa chuỗi Tiếng Việt không dấu phục vụ so khớp tìm kiếm thông minh
     */
    public static String removeDiacritics(String str) {
        if (str == null) return "";
        String nfd = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(nfd)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase();
    }

    // =========================================================================
    // PHẦN CHỨC NĂNG ĐẶT HÀNG & THANH TOÁN (CHECKOUT & ORDER MANAGEMENT)
    // =========================================================================

    /**
     * Lấy danh sách địa chỉ nhận hàng của người dùng (Bảng DIA_CHI)
     * Chỉ cho phép lấy địa chỉ thuộc tài khoản đang đăng nhập.
     */
    public static List<Address> getAddressesByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return new ArrayList<>();
        List<Address> list = new ArrayList<>();
        String sql = "SELECT id, username, recipient_name, phone, address_detail, province, district, ward, is_default, created_at FROM dia_chi WHERE LOWER(username) = ? ORDER BY is_default DESC, id DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Address addr = new Address(
                            rs.getInt("id"),
                            0,
                            rs.getString("username"),
                            rs.getString("recipient_name"),
                            rs.getString("phone"),
                            rs.getString("address_detail"),
                            rs.getString("province"),
                            rs.getString("district"),
                            rs.getString("ward"),
                            rs.getBoolean("is_default")
                    );
                    addr.setCreatedAt(rs.getString("created_at"));
                    list.add(addr);
                }
            }
            if (!list.isEmpty()) return list;
        } catch (SQLException e) {
            // Dùng dự phòng bộ nhớ
        }

        for (Address a : memoryAddresses) {
            if (username.trim().equalsIgnoreCase(a.getUsername())) {
                list.add(a);
            }
        }
        return list;
    }

    /**
     * Lấy địa chỉ theo ID và kiểm tra quyền sở hữu (Bắt buộc phải thuộc tài khoản đang đăng nhập)
     */
    public static Address getAddressById(int id, String username) {
        if (username == null) return null;
        String sql = "SELECT id, username, recipient_name, phone, address_detail, province, district, ward, is_default, created_at FROM dia_chi WHERE id = ? AND LOWER(username) = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Address addr = new Address(
                            rs.getInt("id"),
                            0,
                            rs.getString("username"),
                            rs.getString("recipient_name"),
                            rs.getString("phone"),
                            rs.getString("address_detail"),
                            rs.getString("province"),
                            rs.getString("district"),
                            rs.getString("ward"),
                            rs.getBoolean("is_default")
                    );
                    addr.setCreatedAt(rs.getString("created_at"));
                    return addr;
                }
            }
        } catch (SQLException e) {
            // Dùng dự phòng bộ nhớ
        }

        for (Address a : memoryAddresses) {
            if (a.getId() == id && username.trim().equalsIgnoreCase(a.getUsername())) {
                return a;
            }
        }
        return null;
    }

    /**
     * Thêm địa chỉ nhận hàng mới cho người dùng
     */
    public static synchronized Address addAddress(String username, String recipientName, String phone,
                                                  String addressDetail, String province, String district,
                                                  String ward, boolean isDefault) {
        if (username == null || recipientName == null || phone == null || addressDetail == null) return null;
        
        // Nếu đặt làm mặc định -> hủy mặc định các địa chỉ cũ
        if (isDefault) {
            try (Connection conn = DBContext.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE dia_chi SET is_default = 0 WHERE LOWER(username) = ?")) {
                ps.setString(1, username.trim().toLowerCase());
                ps.executeUpdate();
            } catch (SQLException ignored) {}

            for (Address a : memoryAddresses) {
                if (username.trim().equalsIgnoreCase(a.getUsername())) {
                    a.setDefault(false);
                }
            }
        }

        int newId = ++nextAddressId;
        String sql = "INSERT INTO dia_chi (username, recipient_name, phone, address_detail, province, district, ward, is_default) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, recipientName.trim());
            ps.setString(3, phone.trim());
            ps.setString(4, addressDetail.trim());
            ps.setString(5, province != null ? province.trim() : "");
            ps.setString(6, district != null ? district.trim() : "");
            ps.setString(7, ward != null ? ward.trim() : "");
            ps.setBoolean(8, isDefault);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    newId = rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {}

        Address addr = new Address(newId, 0, username.trim().toLowerCase(), recipientName.trim(), phone.trim(),
                addressDetail.trim(), province != null ? province.trim() : "",
                district != null ? district.trim() : "",
                ward != null ? ward.trim() : "", isDefault);
        addr.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        memoryAddresses.add(addr);
        return addr;
    }

    /**
     * Tra cứu thông tin mã giảm giá theo Code (Bảng MA_GIAM_GIA)
     */
    public static Voucher findVoucher(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        String cleanCode = code.trim().toUpperCase();

        String sql = "SELECT id, code, title, description, discount_type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, is_active, usage_limit, used_count FROM ma_giam_gia WHERE UPPER(code) = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Voucher(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("discount_type"),
                            rs.getDouble("discount_value"),
                            rs.getDouble("min_order_amount"),
                            rs.getDouble("max_discount_amount"),
                            rs.getString("start_date"),
                            rs.getString("end_date"),
                            rs.getBoolean("is_active"),
                            rs.getInt("usage_limit"),
                            rs.getInt("used_count")
                    );
                }
            }
        } catch (SQLException ignored) {}

        for (Voucher v : memoryVouchers) {
            if (cleanCode.equalsIgnoreCase(v.getCode())) {
                return v;
            }
        }
        return null;
    }

    /**
     * Lấy danh sách các mã giảm giá đang hoạt động
     */
    public static List<Voucher> getActiveVouchers() {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT id, code, title, description, discount_type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, is_active, usage_limit, used_count FROM ma_giam_gia WHERE is_active = 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Voucher(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("discount_type"),
                        rs.getDouble("discount_value"),
                        rs.getDouble("min_order_amount"),
                        rs.getDouble("max_discount_amount"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getBoolean("is_active"),
                        rs.getInt("usage_limit"),
                        rs.getInt("used_count")
                ));
            }
            if (!list.isEmpty()) return list;
        } catch (SQLException ignored) {}

        for (Voucher v : memoryVouchers) {
            if (v.isActive()) list.add(v);
        }
        return list;
    }

    /**
     * Lấy tất cả mã giảm giá cho trang Quản lý Khuyến Mãi Admin
     */
    public static List<Voucher> getAllVouchersList() {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT id, code, title, description, discount_type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, is_active, usage_limit, used_count FROM ma_giam_gia ORDER BY id DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Voucher(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("discount_type"),
                        rs.getDouble("discount_value"),
                        rs.getDouble("min_order_amount"),
                        rs.getDouble("max_discount_amount"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getBoolean("is_active"),
                        rs.getInt("usage_limit"),
                        rs.getInt("used_count")
                ));
            }
            if (!list.isEmpty()) return list;
        } catch (SQLException ignored) {}

        return new ArrayList<>(memoryVouchers);
    }

    /**
     * Lấy thông tin mã giảm giá theo ID
     */
    public static Voucher getVoucherById(int id) {
        String sql = "SELECT id, code, title, description, discount_type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, is_active, usage_limit, used_count FROM ma_giam_gia WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Voucher(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("discount_type"),
                            rs.getDouble("discount_value"),
                            rs.getDouble("min_order_amount"),
                            rs.getDouble("max_discount_amount"),
                            rs.getString("start_date"),
                            rs.getString("end_date"),
                            rs.getBoolean("is_active"),
                            rs.getInt("usage_limit"),
                            rs.getInt("used_count")
                    );
                }
            }
        } catch (SQLException ignored) {}

        for (Voucher v : memoryVouchers) {
            if (v.getId() == id) return v;
        }
        return null;
    }

    /**
     * Thêm mới mã giảm giá
     */
    public static String addVoucher(Voucher v) {
        if (v == null || v.getCode() == null || v.getCode().trim().isEmpty()) {
            return "Mã giảm giá không được để trống!";
        }
        String cleanCode = v.getCode().trim().toUpperCase();
        v.setCode(cleanCode);

        Voucher existing = findVoucher(cleanCode);
        if (existing != null) {
            return "Mã giảm giá '" + cleanCode + "' đã tồn tại trên hệ thống!";
        }

        String sql = "INSERT INTO ma_giam_gia (code, title, description, discount_type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, is_active, usage_limit, used_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getCode());
            ps.setString(2, v.getTitle());
            ps.setString(3, v.getDescription());
            ps.setString(4, v.getDiscountType());
            ps.setDouble(5, v.getDiscountValue());
            ps.setDouble(6, v.getMinOrderAmount());
            ps.setDouble(7, v.getMaxDiscountAmount());
            ps.setString(8, v.getStartDate());
            ps.setString(9, v.getEndDate());
            ps.setBoolean(10, v.isActive());
            ps.setInt(11, v.getUsageLimit());
            ps.setInt(12, 0);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    v.setId(rs.getInt(1));
                }
            }
        } catch (SQLException ignored) {
            if (v.getId() <= 0) v.setId((int)(System.currentTimeMillis() % 100000));
        }

        memoryVouchers.add(v);
        return null; // Thành công
    }

    /**
     * Chỉnh sửa thông tin mã giảm giá
     */
    public static String updateVoucher(Voucher v) {
        if (v == null || v.getCode() == null || v.getCode().trim().isEmpty()) {
            return "Mã giảm giá không được để trống!";
        }
        String cleanCode = v.getCode().trim().toUpperCase();
        v.setCode(cleanCode);

        Voucher existing = findVoucher(cleanCode);
        if (existing != null && existing.getId() != v.getId()) {
            return "Mã giảm giá '" + cleanCode + "' trùng với một mã khác đã tồn tại!";
        }

        String sql = "UPDATE ma_giam_gia SET code = ?, title = ?, description = ?, discount_type = ?, discount_value = ?, min_order_amount = ?, max_discount_amount = ?, start_date = ?, end_date = ?, is_active = ?, usage_limit = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getCode());
            ps.setString(2, v.getTitle());
            ps.setString(3, v.getDescription());
            ps.setString(4, v.getDiscountType());
            ps.setDouble(5, v.getDiscountValue());
            ps.setDouble(6, v.getMinOrderAmount());
            ps.setDouble(7, v.getMaxDiscountAmount());
            ps.setString(8, v.getStartDate());
            ps.setString(9, v.getEndDate());
            ps.setBoolean(10, v.isActive());
            ps.setInt(11, v.getUsageLimit());
            ps.setInt(12, v.getId());
            ps.executeUpdate();
        } catch (SQLException ignored) {}

        for (int i = 0; i < memoryVouchers.size(); i++) {
            if (memoryVouchers.get(i).getId() == v.getId()) {
                Voucher old = memoryVouchers.get(i);
                v.setUsedCount(old.getUsedCount()); // Giữ nguyên lịch sử số lần đã sử dụng
                memoryVouchers.set(i, v);
                break;
            }
        }
        return null; // Thành công
    }

    /**
     * Xóa mã giảm giá (Kiểm tra lịch sử sử dụng trong đơn hàng)
     */
    public static String deleteVoucher(int id) {
        Voucher v = getVoucherById(id);
        if (v == null) {
            return "Mã giảm giá không tồn tại!";
        }

        // 1. Kiểm tra số lần đã sử dụng từ Voucher object
        if (v.getUsedCount() > 0) {
            return "Không thể xóa mã giảm giá vì mã đã được sử dụng trong đơn hàng.";
        }

        // 2. Kiểm tra trực tiếp bảng don_hang
        String countSql = "SELECT COUNT(*) FROM don_hang WHERE UPPER(voucher_code) = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setString(1, v.getCode().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return "Không thể xóa mã giảm giá vì mã đã được sử dụng trong đơn hàng.";
                }
            }
        } catch (SQLException ignored) {}

        // 3. Nếu chưa từng sử dụng -> Cho phép xóa
        String deleteSql = "DELETE FROM ma_giam_gia WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {}

        memoryVouchers.removeIf(item -> item.getId() == id);
        return null; // Thành công
    }

    /**
     * Bật / Tắt trạng thái mã giảm giá
     */
    public static boolean toggleVoucherStatus(int id, boolean active) {
        String sql = "UPDATE ma_giam_gia SET is_active = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {}

        for (Voucher v : memoryVouchers) {
            if (v.getId() == id) {
                v.setActive(active);
                break;
            }
        }
        return true;
    }

    /**
     * Lấy danh sách các đơn hàng đã sử dụng mã giảm giá này (Dành cho Xem chi tiết Admin)
     */
    public static List<Order> getOrdersByVoucherCode(String code) {
        List<Order> list = new ArrayList<>();
        if (code == null || code.trim().isEmpty()) return list;

        String sql = "SELECT id, order_code, username, address_id, recipient_name, recipient_phone, delivery_address, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, payment_method_code, payment_method_name, payment_status, order_status, note, transaction_id, created_at FROM don_hang WHERE UPPER(voucher_code) = ? ORDER BY id DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Order(
                            rs.getInt("id"),
                            rs.getString("order_code"),
                            rs.getString("username"),
                            rs.getInt("address_id"),
                            rs.getString("recipient_name"),
                            rs.getString("recipient_phone"),
                            rs.getString("delivery_address"),
                            rs.getString("voucher_code"),
                            rs.getDouble("subtotal"),
                            rs.getDouble("discount_amount"),
                            rs.getDouble("shipping_fee"),
                            rs.getDouble("total_amount"),
                            rs.getString("payment_method_code"),
                            rs.getString("payment_method_name"),
                            rs.getString("payment_status"),
                            rs.getString("order_status"),
                            rs.getString("note"),
                            rs.getString("transaction_id"),
                            rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException ignored) {}

        if (list.isEmpty()) {
            for (Order o : memoryOrders) {
                if (o.getVoucherCode() != null && code.trim().equalsIgnoreCase(o.getVoucherCode().trim())) {
                    list.add(o);
                }
            }
        }
        return list;
    }

    /**
     * Lấy danh sách phương thức thanh toán hỗ trợ (Bảng PHUONG_THUC_TT)
     */
    public static List<PaymentMethod> getPaymentMethods() {
        List<PaymentMethod> list = new ArrayList<>();
        String sql = "SELECT id, code, name, description, is_active FROM phuong_thuc_tt WHERE is_active = 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PaymentMethod(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("is_active")
                ));
            }
            if (!list.isEmpty()) return list;
        } catch (SQLException ignored) {}

        return new ArrayList<>(memoryPaymentMethods);
    }

    /**
     * Lấy số lượng tồn kho thực tế của một cuốn sách (Bảng KHO & BOOKS)
     */
    public static int getBookStock(int bookId) {
        String sql = "SELECT stock FROM books WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }
        } catch (SQLException ignored) {}

        for (Book b : memoryBooks) {
            if (b.getId() == bookId) return b.getStock();
        }
        return 0;
    }

    /**
     * Kiểm tra số lượng tồn kho từng sản phẩm trong giỏ hàng (Bước 2 trong luồng đặt hàng)
     * @param items Danh sách sản phẩm muốn đặt
     * @param outErrorMsg Chuỗi chứa thông báo lỗi chi tiết nếu có sản phẩm không đủ hàng
     * @return true nếu TẤT CẢ sản phẩm đều đủ hàng, false nếu có bất kỳ sản phẩm nào thiếu
     */
    public static synchronized boolean checkStockAvailable(List<OrderItem> items, StringBuilder outErrorMsg) {
        if (items == null || items.isEmpty()) {
            if (outErrorMsg != null) outErrorMsg.append("Không có sản phẩm nào trong đơn hàng!");
            return false;
        }

        for (OrderItem item : items) {
            int currentStock = getBookStock(item.getBookId());
            if (currentStock < item.getQuantity()) {
                if (outErrorMsg != null) {
                    if (currentStock <= 0) {
                        outErrorMsg.append("Sản phẩm '").append(item.getBookTitle())
                                .append("' hiện đã hết hàng trong kho. Vui lòng bỏ sản phẩm này để tiếp tục.");
                    } else {
                        outErrorMsg.append("Sản phẩm '").append(item.getBookTitle())
                                .append("' chỉ còn ").append(currentStock)
                                .append(" cuốn trong kho, không đủ số lượng bạn đặt (")
                                .append(item.getQuantity()).append(" cuốn). Vui lòng điều chỉnh lại số lượng.");
                    }
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Cập nhật trừ tồn kho trong KHO và BOOKS khi đặt hàng thành công (Atomic)
     * Đảm bảo tính nhất quán: Nếu 1 cuốn thất bại -> không trừ cuốn nào.
     */
    public static synchronized boolean deductStock(List<OrderItem> items) {
        // Bước 1: Kiểm tra lại toàn bộ tồn kho trước khi trừ
        StringBuilder error = new StringBuilder();
        if (!checkStockAvailable(items, error)) {
            return false;
        }

        // Bước 2: Trừ trong MySQL nếu có kết nối
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlBooks = "UPDATE books SET stock = stock - ? WHERE id = ? AND stock >= ?";
                String sqlKho = "UPDATE kho SET quantity = quantity - ?, status = IF(quantity <= 0, 'HET_HANG', 'CON_HANG') WHERE book_id = ?";

                try (PreparedStatement psBooks = conn.prepareStatement(sqlBooks);
                     PreparedStatement psKho = conn.prepareStatement(sqlKho)) {
                    for (OrderItem item : items) {
                        psBooks.setInt(1, item.getQuantity());
                        psBooks.setInt(2, item.getBookId());
                        psBooks.setInt(3, item.getQuantity());
                        int updated = psBooks.executeUpdate();
                        if (updated <= 0) {
                            conn.rollback();
                            return false;
                        }

                        psKho.setInt(1, item.getQuantity());
                        psKho.setInt(2, item.getBookId());
                        psKho.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ignored) {}

        // Bước 3: Cập nhật trong bộ nhớ
        for (OrderItem item : items) {
            for (Book b : memoryBooks) {
                if (b.getId() == item.getBookId()) {
                    b.setStock(Math.max(0, b.getStock() - item.getQuantity()));
                    break;
                }
            }
        }
        return true;
    }

    /**
     * Khôi phục lại tồn kho nếu đơn hàng bị hủy hoặc thanh toán trực tuyến bị lỗi
     */
    public static synchronized void restoreStock(List<OrderItem> items) {
        if (items == null) return;
        try (Connection conn = DBContext.getConnection()) {
            String sqlBooks = "UPDATE books SET stock = stock + ? WHERE id = ?";
            String sqlKho = "UPDATE kho SET quantity = quantity + ?, status = 'CON_HANG' WHERE book_id = ?";
            try (PreparedStatement psBooks = conn.prepareStatement(sqlBooks);
                 PreparedStatement psKho = conn.prepareStatement(sqlKho)) {
                for (OrderItem item : items) {
                    psBooks.setInt(1, item.getQuantity());
                    psBooks.setInt(2, item.getBookId());
                    psBooks.executeUpdate();

                    psKho.setInt(1, item.getQuantity());
                    psKho.setInt(2, item.getBookId());
                    psKho.executeUpdate();
                }
            }
        } catch (SQLException ignored) {}

        for (OrderItem item : items) {
            for (Book b : memoryBooks) {
                if (b.getId() == item.getBookId()) {
                    b.setStock(b.getStock() + item.getQuantity());
                    break;
                }
            }
        }
    }

    /**
     * Tạo đơn hàng mới (DON_HANG & CT_DON_HANG)
     */
    public static synchronized Order createOrder(String username, Address address, List<OrderItem> items,
                                                 String voucherCode, double subtotal, double discountAmount,
                                                 double shippingFee, double totalAmount, String paymentMethodCode,
                                                 String paymentStatus, String orderStatus, String note,
                                                 String transactionId) {
        int orderId = ++nextOrderId;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int rand = (int) (Math.random() * 900) + 100;
        String orderCode = "BK" + timestamp + rand;

        String paymentMethodName = "ONLINE".equalsIgnoreCase(paymentMethodCode)
                ? "Thanh toán trực tuyến (VietQR / MoMo)"
                : "Thanh toán khi nhận hàng (COD)";

        String createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Order order = new Order(
                orderId,
                orderCode,
                username,
                address != null ? address.getId() : 0,
                address != null ? address.getRecipientName() : "",
                address != null ? address.getPhone() : "",
                address != null ? address.getFullAddress() : "",
                voucherCode,
                subtotal,
                discountAmount,
                shippingFee,
                totalAmount,
                paymentMethodCode,
                paymentMethodName,
                paymentStatus,
                orderStatus,
                note,
                transactionId,
                createdAt
        );

        // Sao chép các item vào đơn
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItem it : items) {
            OrderItem orderItem = new OrderItem(
                    it.getId() > 0 ? it.getId() : (int)(Math.random() * 100000),
                    orderId,
                    it.getBookId(),
                    it.getBookCode(),
                    it.getBookTitle(),
                    it.getBookImage(),
                    it.getPrice(),
                    it.getQuantity(),
                    it.getSubtotal()
            );
            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        // Lưu vào MySQL nếu có kết nối
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlOrder = "INSERT INTO don_hang (order_code, username, address_id, recipient_name, recipient_phone, delivery_address, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, payment_method_code, payment_method_name, payment_status, order_status, note, transaction_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS)) {
                    psOrder.setString(1, orderCode);
                    psOrder.setString(2, username);
                    psOrder.setInt(3, address != null ? address.getId() : 0);
                    psOrder.setString(4, address != null ? address.getRecipientName() : "");
                    psOrder.setString(5, address != null ? address.getPhone() : "");
                    psOrder.setString(6, address != null ? address.getFullAddress() : "");
                    psOrder.setString(7, voucherCode);
                    psOrder.setDouble(8, subtotal);
                    psOrder.setDouble(9, discountAmount);
                    psOrder.setDouble(10, shippingFee);
                    psOrder.setDouble(11, totalAmount);
                    psOrder.setString(12, paymentMethodCode);
                    psOrder.setString(13, paymentMethodName);
                    psOrder.setString(14, paymentStatus);
                    psOrder.setString(15, orderStatus);
                    psOrder.setString(16, note);
                    psOrder.setString(17, transactionId);
                    psOrder.setString(18, createdAt);
                    psOrder.executeUpdate();

                    try (ResultSet rs = psOrder.getGeneratedKeys()) {
                        if (rs.next()) {
                            orderId = rs.getInt(1);
                            order.setId(orderId);
                        }
                    }
                }

                String sqlItem = "INSERT INTO ct_don_hang (order_id, book_id, book_code, book_title, book_image, price, quantity, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psItem = conn.prepareStatement(sqlItem)) {
                    for (OrderItem it : orderItems) {
                        it.setOrderId(orderId);
                        psItem.setInt(1, orderId);
                        psItem.setInt(2, it.getBookId());
                        psItem.setString(3, it.getBookCode());
                        psItem.setString(4, it.getBookTitle());
                        psItem.setString(5, it.getBookImage());
                        psItem.setDouble(6, it.getPrice());
                        psItem.setInt(7, it.getQuantity());
                        psItem.setDouble(8, it.getSubtotal());
                        psItem.executeUpdate();
                    }
                }

                // Nếu có áp dụng voucher -> tăng used_count
                if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                    try (PreparedStatement psVoucher = conn.prepareStatement("UPDATE ma_giam_gia SET used_count = used_count + 1 WHERE UPPER(code) = ?")) {
                        psVoucher.setString(1, voucherCode.trim().toUpperCase());
                        psVoucher.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ignored) {}

        // Tăng used_count trong memory
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Voucher v = findVoucher(voucherCode);
            if (v != null) v.setUsedCount(v.getUsedCount() + 1);
        }

        memoryOrders.add(0, order);
        return order;
    }

    public static Order getOrderByCode(String orderCode) {
        return getOrderByCode(orderCode, "admin");
    }

    /**
     * Tra cứu chi tiết đơn hàng theo mã đơn (order_code)
     * Đảm bảo tính bảo mật: chỉ cho phép xem đơn thuộc sở hữu của tài khoản đang đăng nhập (trừ admin).
     */
    public static Order getOrderByCode(String orderCode, String username) {
        if (orderCode == null || orderCode.trim().isEmpty()) return null;
        String cleanCode = orderCode.trim();

        // 1. Tìm trong memory
        for (Order o : memoryOrders) {
            if (cleanCode.equalsIgnoreCase(o.getOrderCode())) {
                if (username == null || "admin".equalsIgnoreCase(username) || o.getUsername().equalsIgnoreCase(username)) {
                    return o;
                }
                return null; // Không thuộc tài khoản
            }
        }

        // 2. Tìm trong MySQL
        String sql = "SELECT id, order_code, username, address_id, recipient_name, recipient_phone, delivery_address, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, payment_method_code, payment_method_name, payment_status, order_status, note, transaction_id, created_at FROM don_hang WHERE order_code = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String owner = rs.getString("username");
                    if (username != null && !"admin".equalsIgnoreCase(username) && !owner.equalsIgnoreCase(username)) {
                        return null; // Bảo mật: Không thuộc tài khoản này!
                    }

                    int oId = rs.getInt("id");
                    Order o = new Order(
                            oId,
                            rs.getString("order_code"),
                            owner,
                            rs.getInt("address_id"),
                            rs.getString("recipient_name"),
                            rs.getString("recipient_phone"),
                            rs.getString("delivery_address"),
                            rs.getString("voucher_code"),
                            rs.getDouble("subtotal"),
                            rs.getDouble("discount_amount"),
                            rs.getDouble("shipping_fee"),
                            rs.getDouble("total_amount"),
                            rs.getString("payment_method_code"),
                            rs.getString("payment_method_name"),
                            rs.getString("payment_status"),
                            rs.getString("order_status"),
                            rs.getString("note"),
                            rs.getString("transaction_id"),
                            rs.getString("created_at")
                    );

                    // Lấy chi tiết items (tự động ghép thông tin với bảng books để luôn có Tên sách & Ảnh chính xác)
                    List<OrderItem> items = new ArrayList<>();
                    String itemSql = "SELECT ct.id, ct.order_id, ct.book_id, " +
                            "COALESCE(NULLIF(b.code, ''), NULLIF(ct.book_code, ''), 'MS000') AS final_code, " +
                            "COALESCE(NULLIF(b.title, ''), NULLIF(ct.book_title, ''), 'Sách Bookora') AS final_title, " +
                            "COALESCE(NULLIF(b.image, ''), NULLIF(ct.book_image, ''), '/images/default-book.jpg') AS final_image, " +
                            "ct.price, ct.quantity, ct.subtotal " +
                            "FROM ct_don_hang ct " +
                            "LEFT JOIN books b ON ct.book_id = b.id " +
                            "WHERE ct.order_id = ?";
                    try (PreparedStatement psItems = conn.prepareStatement(itemSql)) {
                        psItems.setInt(1, oId);
                        try (ResultSet rsItems = psItems.executeQuery()) {
                            while (rsItems.next()) {
                                items.add(new OrderItem(
                                        rsItems.getInt("id"),
                                        rsItems.getInt("order_id"),
                                        rsItems.getInt("book_id"),
                                        rsItems.getString("final_code"),
                                        rsItems.getString("final_title"),
                                        rsItems.getString("final_image"),
                                        rsItems.getDouble("price"),
                                        rsItems.getInt("quantity"),
                                        rsItems.getDouble("subtotal")
                                ));
                            }
                        }
                    }
                    o.setItems(items);
                    return o;
                }
            }
        } catch (SQLException ignored) {}

        return null;
    }

    /**
     * Lấy danh sách toàn bộ đơn hàng của một người dùng
     */
    public static List<Order> getOrdersByUsername(String username) {
        List<Order> list = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) return list;

        Set<String> seenCodes = new HashSet<>();
        String sql = "SELECT id, order_code, username, address_id, recipient_name, recipient_phone, delivery_address, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, payment_method_code, payment_method_name, payment_status, order_status, note, transaction_id, created_at FROM don_hang WHERE LOWER(username) = ? ORDER BY id DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = new Order(
                            rs.getInt("id"),
                            rs.getString("order_code"),
                            rs.getString("username"),
                            rs.getInt("address_id"),
                            rs.getString("recipient_name"),
                            rs.getString("recipient_phone"),
                            rs.getString("delivery_address"),
                            rs.getString("voucher_code"),
                            rs.getDouble("subtotal"),
                            rs.getDouble("discount_amount"),
                            rs.getDouble("shipping_fee"),
                            rs.getDouble("total_amount"),
                            rs.getString("payment_method_code"),
                            rs.getString("payment_method_name"),
                            rs.getString("payment_status"),
                            rs.getString("order_status"),
                            rs.getString("note"),
                            rs.getString("transaction_id"),
                            rs.getString("created_at")
                    );
                    list.add(o);
                    if (o.getOrderCode() != null) {
                        seenCodes.add(o.getOrderCode().toLowerCase());
                    }
                }
            }

            String itemSql = "SELECT ct.id, ct.order_id, ct.book_id, " +
                    "COALESCE(NULLIF(b.code, ''), NULLIF(ct.book_code, ''), 'MS000') AS final_code, " +
                    "COALESCE(NULLIF(b.title, ''), NULLIF(ct.book_title, ''), 'Sách Bookora') AS final_title, " +
                    "COALESCE(NULLIF(b.image, ''), NULLIF(ct.book_image, ''), '/images/default-book.jpg') AS final_image, " +
                    "ct.price, ct.quantity, ct.subtotal " +
                    "FROM ct_don_hang ct " +
                    "LEFT JOIN books b ON ct.book_id = b.id " +
                    "WHERE ct.order_id = ?";
            for (Order o : list) {
                List<OrderItem> items = new ArrayList<>();
                try (PreparedStatement psItems = conn.prepareStatement(itemSql)) {
                    psItems.setInt(1, o.getId());
                    try (ResultSet rsItems = psItems.executeQuery()) {
                        while (rsItems.next()) {
                            items.add(new OrderItem(
                                    rsItems.getInt("id"),
                                    rsItems.getInt("order_id"),
                                    rsItems.getInt("book_id"),
                                    rsItems.getString("final_code"),
                                    rsItems.getString("final_title"),
                                    rsItems.getString("final_image"),
                                    rsItems.getDouble("price"),
                                    rsItems.getInt("quantity"),
                                    rsItems.getDouble("subtotal")
                            ));
                        }
                    }
                }
                o.setItems(items);
            }
        } catch (SQLException ignored) {}

        for (Order o : memoryOrders) {
            if (o.getUsername() != null && username.trim().equalsIgnoreCase(o.getUsername().trim())) {
                String code = o.getOrderCode();
                if (code != null && !seenCodes.contains(code.toLowerCase())) {
                    // Đảm bảo thông tin sách trong bộ nhớ cũng đầy đủ tên và ảnh
                    if (o.getItems() != null) {
                        for (OrderItem it : o.getItems()) {
                            if (it.getBookTitle() == null || it.getBookTitle().trim().isEmpty() ||
                                it.getBookImage() == null || it.getBookImage().trim().isEmpty()) {
                                Book b = getBookById(it.getBookId());
                                if (b != null) {
                                    if (it.getBookTitle() == null || it.getBookTitle().trim().isEmpty()) it.setBookTitle(b.getTitle());
                                    if (it.getBookImage() == null || it.getBookImage().trim().isEmpty()) it.setBookImage(b.getImage());
                                    if (it.getBookCode() == null || it.getBookCode().trim().isEmpty()) it.setBookCode(b.getCode());
                                }
                            }
                        }
                    }
                    list.add(0, o);
                    seenCodes.add(code.toLowerCase());
                }
            }
        }

        return list;
    }

    /**
     * Cập nhật trạng thái thanh toán của đơn hàng (sau khi thanh toán trực tuyến thành công)
     */
    public static synchronized boolean updatePaymentStatus(String orderCode, String paymentStatus, String transactionId) {
        if (orderCode == null) return false;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE don_hang SET payment_status = ?, transaction_id = ?, order_status = IF(? = 'PAID', 'DANG_XU_LY', order_status) WHERE order_code = ?")) {
            ps.setString(1, paymentStatus);
            ps.setString(2, transactionId);
            ps.setString(3, paymentStatus);
            ps.setString(4, orderCode);
            ps.executeUpdate();
        } catch (SQLException ignored) {}

        for (Order o : memoryOrders) {
            if (orderCode.equalsIgnoreCase(o.getOrderCode())) {
                o.setPaymentStatus(paymentStatus);
                if (transactionId != null) o.setTransactionId(transactionId);
                if ("PAID".equalsIgnoreCase(paymentStatus)) {
                    o.setOrderStatus("DANG_XU_LY");
                }
                return true;
            }
        }
        return true;
    }

    /**
     * Đồng bộ giỏ hàng của người dùng vào Database (Bảng GIO_HANG & CT_GIO_HANG)
     */
    public static synchronized void syncCartToDb(String username, List<OrderItem> items) {
        if (username == null) return;
        memoryCarts.put(username.toLowerCase(), new ArrayList<>(items));

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int cartId = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM gio_hang WHERE LOWER(username) = ?")) {
                    ps.setString(1, username.toLowerCase());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            cartId = rs.getInt("id");
                        }
                    }
                }

                if (cartId == 0) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO gio_hang (username) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, username.toLowerCase());
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) cartId = rs.getInt(1);
                        }
                    }
                }

                if (cartId > 0) {
                    try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM ct_gio_hang WHERE gio_hang_id = ?")) {
                        psDel.setInt(1, cartId);
                        psDel.executeUpdate();
                    }

                    if (items != null && !items.isEmpty()) {
                        try (PreparedStatement psIns = conn.prepareStatement("INSERT INTO ct_gio_hang (gio_hang_id, book_id, quantity) VALUES (?, ?, ?)")) {
                            for (OrderItem item : items) {
                                psIns.setInt(1, cartId);
                                psIns.setInt(2, item.getBookId());
                                psIns.setInt(3, item.getQuantity());
                                psIns.executeUpdate();
                            }
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ignored) {}
    }

    // =========================================================================
    // UC15: THỐNG KÊ VÀ BÁO CÁO (REPORTS & STATISTICS DATA PROVIDER)
    // =========================================================================

    public static class ReportResult {
        public String reportType;
        public String fromDate;
        public String toDate;
        public boolean isEmpty;
        public String message;
        public Map<String, Object> summary = new HashMap<>();
        public List<Map<String, Object>> rows = new ArrayList<>();
        public Map<String, Object> chartData = new HashMap<>();

        public ReportResult() {}

        public ReportResult(String reportType, String fromDate, String toDate) {
            this.reportType = reportType;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.isEmpty = false;
            this.message = "Thành công";
        }
    }

    /**
     * Tổng hợp dữ liệu báo cáo thống kê theo loại báo cáo và khoảng thời gian (UC15)
     */
    public static ReportResult getReportData(String type, String fromDate, String toDate, String search) {
        if (type == null || type.trim().isEmpty()) type = "REVENUE";
        type = type.trim().toUpperCase();

        if (fromDate == null || fromDate.trim().isEmpty()) {
            fromDate = LocalDate.now().minusDays(30).toString();
        } else {
            fromDate = fromDate.trim();
        }

        if (toDate == null || toDate.trim().isEmpty()) {
            toDate = LocalDate.now().toString();
        } else {
            toDate = toDate.trim();
        }

        ReportResult res = new ReportResult(type, fromDate, toDate);

        // Validation 1: Ngày bắt đầu không được lớn hơn ngày kết thúc
        if (fromDate.compareTo(toDate) > 0) {
            res.isEmpty = true;
            res.message = "Khoảng thời gian không hợp lệ.";
            return res;
        }

        switch (type) {
            case "ORDERS":
                fetchOrdersReport(res, search);
                break;
            case "PRODUCTS":
                fetchProductsReport(res, search);
                break;
            case "INVENTORY":
                fetchInventoryReport(res, search);
                break;
            case "CUSTOMERS":
                fetchCustomersReport(res, search);
                break;
            case "REVENUE":
            default:
                fetchRevenueReport(res, search);
                break;
        }

        if (res.rows.isEmpty()) {
            res.isEmpty = true;
            res.message = "Không có dữ liệu thống kê";
        }

        return res;
    }

    private static void fetchRevenueReport(ReportResult res, String search) {
        String sql = "SELECT DATE(created_at) AS rdate, COUNT(*) AS order_count, SUM(subtotal) AS total_subtotal, SUM(discount_amount) AS total_discount, SUM(shipping_fee) AS total_shipping, SUM(total_amount) AS total_net FROM don_hang WHERE DATE(created_at) >= ? AND DATE(created_at) <= ? AND UPPER(order_status) != 'DA_HUY' GROUP BY DATE(created_at) ORDER BY rdate ASC";

        double grandTotalNet = 0;
        long grandTotalOrders = 0;
        double grandTotalDiscount = 0;
        double grandTotalShipping = 0;

        List<String> chartLabels = new ArrayList<>();
        List<Double> chartValues = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, res.fromDate);
            ps.setString(2, res.toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rdate = rs.getString("rdate");
                    long cnt = rs.getLong("order_count");
                    double sub = rs.getDouble("total_subtotal");
                    double disc = rs.getDouble("total_discount");
                    double ship = rs.getDouble("total_shipping");
                    double net = rs.getDouble("total_net");

                    grandTotalNet += net;
                    grandTotalOrders += cnt;
                    grandTotalDiscount += disc;
                    grandTotalShipping += ship;

                    Map<String, Object> row = new HashMap<>();
                    row.put("date", rdate);
                    row.put("orderCount", cnt);
                    row.put("subtotal", sub);
                    row.put("discountAmount", disc);
                    row.put("shippingFee", ship);
                    row.put("totalAmount", net);
                    res.rows.add(row);

                    chartLabels.add(rdate);
                    chartValues.add(net);
                }
            }
        } catch (SQLException e) {
            // Memory fallback
            Map<String, Map<String, Object>> dateMap = new TreeMap<>();
            for (Order o : memoryOrders) {
                if (o.getCreatedAt() != null && o.getCreatedAt().length() >= 10) {
                    String d = o.getCreatedAt().substring(0, 10);
                    if (d.compareTo(res.fromDate) >= 0 && d.compareTo(res.toDate) <= 0 && !"DA_HUY".equalsIgnoreCase(o.getOrderStatus())) {
                        Map<String, Object> m = dateMap.computeIfAbsent(d, k -> {
                            Map<String, Object> item = new HashMap<>();
                            item.put("date", k);
                            item.put("orderCount", 0L);
                            item.put("subtotal", 0.0);
                            item.put("discountAmount", 0.0);
                            item.put("shippingFee", 0.0);
                            item.put("totalAmount", 0.0);
                            return item;
                        });
                        m.put("orderCount", ((Long)m.get("orderCount")) + 1);
                        m.put("subtotal", ((Double)m.get("subtotal")) + o.getSubtotal());
                        m.put("discountAmount", ((Double)m.get("discountAmount")) + o.getDiscountAmount());
                        m.put("shippingFee", ((Double)m.get("shippingFee")) + o.getShippingFee());
                        m.put("totalAmount", ((Double)m.get("totalAmount")) + o.getTotalAmount());
                    }
                }
            }
            for (Map<String, Object> m : dateMap.values()) {
                res.rows.add(m);
                double net = (Double) m.get("totalAmount");
                grandTotalNet += net;
                grandTotalOrders += (Long) m.get("orderCount");
                grandTotalDiscount += (Double) m.get("discountAmount");
                grandTotalShipping += (Double) m.get("shippingFee");
                chartLabels.add((String) m.get("date"));
                chartValues.add(net);
            }
        }

        double avgOrderValue = grandTotalOrders > 0 ? grandTotalNet / grandTotalOrders : 0;
        res.summary.put("totalRevenue", grandTotalNet);
        res.summary.put("totalOrders", grandTotalOrders);
        res.summary.put("avgOrderValue", Math.round(avgOrderValue));
        res.summary.put("totalDiscount", grandTotalDiscount);
        res.summary.put("totalShipping", grandTotalShipping);

        res.chartData.put("labels", chartLabels);
        res.chartData.put("values", chartValues);
    }

    private static void fetchOrdersReport(ReportResult res, String search) {
        String sql = "SELECT id, order_code, username, recipient_name, recipient_phone, payment_method_name, order_status, subtotal, discount_amount, shipping_fee, total_amount, created_at FROM don_hang WHERE DATE(created_at) >= ? AND DATE(created_at) <= ? ORDER BY id DESC";

        long completedCount = 0;
        long processingCount = 0;
        long cancelledCount = 0;
        double totalOrderValue = 0;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, res.fromDate);
            ps.setString(2, res.toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("order_status");
                    String code = rs.getString("order_code");
                    String user = rs.getString("username");
                    String name = rs.getString("recipient_name");

                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.trim().toLowerCase();
                        boolean match = (code != null && code.toLowerCase().contains(s))
                                || (user != null && user.toLowerCase().contains(s))
                                || (name != null && name.toLowerCase().contains(s));
                        if (!match) continue;
                    }

                    double total = rs.getDouble("total_amount");
                    totalOrderValue += total;

                    if ("HOAN_THANH".equalsIgnoreCase(status)) completedCount++;
                    else if ("DA_HUY".equalsIgnoreCase(status)) cancelledCount++;
                    else processingCount++;

                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("orderCode", code);
                    row.put("username", user);
                    row.put("recipientName", name);
                    row.put("phone", rs.getString("recipient_phone"));
                    row.put("paymentMethod", rs.getString("payment_method_name"));
                    row.put("orderStatus", status);
                    row.put("subtotal", rs.getDouble("subtotal"));
                    row.put("discountAmount", rs.getDouble("discount_amount"));
                    row.put("totalAmount", total);
                    row.put("createdAt", rs.getString("created_at"));
                    res.rows.add(row);
                }
            }
        } catch (SQLException e) {
            for (Order o : memoryOrders) {
                if (o.getCreatedAt() != null && o.getCreatedAt().length() >= 10) {
                    String d = o.getCreatedAt().substring(0, 10);
                    if (d.compareTo(res.fromDate) >= 0 && d.compareTo(res.toDate) <= 0) {
                        String status = o.getOrderStatus();
                        if ("HOAN_THANH".equalsIgnoreCase(status)) completedCount++;
                        else if ("DA_HUY".equalsIgnoreCase(status)) cancelledCount++;
                        else processingCount++;
                        totalOrderValue += o.getTotalAmount();

                        Map<String, Object> row = new HashMap<>();
                        row.put("id", o.getId());
                        row.put("orderCode", o.getOrderCode());
                        row.put("username", o.getUsername());
                        row.put("recipientName", o.getRecipientName());
                        row.put("phone", o.getRecipientPhone());
                        row.put("paymentMethod", o.getPaymentMethodName());
                        row.put("orderStatus", status);
                        row.put("subtotal", o.getSubtotal());
                        row.put("discountAmount", o.getDiscountAmount());
                        row.put("totalAmount", o.getTotalAmount());
                        row.put("createdAt", o.getCreatedAt());
                        res.rows.add(row);
                    }
                }
            }
        }

        res.summary.put("totalOrders", (long)res.rows.size());
        res.summary.put("completedCount", completedCount);
        res.summary.put("processingCount", processingCount);
        res.summary.put("cancelledCount", cancelledCount);
        res.summary.put("totalOrderValue", totalOrderValue);
    }

    private static void fetchProductsReport(ReportResult res, String search) {
        String sql = "SELECT ct.book_id, ct.book_code, ct.book_title, COALESCE(NULLIF(b.category,''), 'Chưa phân loại') AS category, ct.price, SUM(ct.quantity) AS total_sold, SUM(ct.subtotal) AS total_revenue, COALESCE(b.stock, 0) AS current_stock FROM ct_don_hang ct JOIN don_hang dh ON ct.order_id = dh.id LEFT JOIN books b ON ct.book_id = b.id WHERE DATE(dh.created_at) >= ? AND DATE(dh.created_at) <= ? AND UPPER(dh.order_status) != 'DA_HUY' GROUP BY ct.book_id, ct.book_code, ct.book_title, category, ct.price, current_stock ORDER BY total_sold DESC";

        long grandTotalUnits = 0;
        double grandTotalRev = 0;
        String topProductTitle = "—";
        long topProductCount = 0;

        List<String> chartLabels = new ArrayList<>();
        List<Long> chartValues = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, res.fromDate);
            ps.setString(2, res.toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("book_title");
                    String code = rs.getString("book_code");
                    long sold = rs.getLong("total_sold");
                    double rev = rs.getDouble("total_revenue");

                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.trim().toLowerCase();
                        boolean match = (title != null && title.toLowerCase().contains(s))
                                || (code != null && code.toLowerCase().contains(s));
                        if (!match) continue;
                    }

                    grandTotalUnits += sold;
                    grandTotalRev += rev;

                    if (sold > topProductCount) {
                        topProductCount = sold;
                        topProductTitle = title;
                    }

                    Map<String, Object> row = new HashMap<>();
                    row.put("bookId", rs.getInt("book_id"));
                    row.put("bookCode", code);
                    row.put("bookTitle", title);
                    row.put("category", rs.getString("category"));
                    row.put("price", rs.getDouble("price"));
                    row.put("totalSold", sold);
                    row.put("totalRevenue", rev);
                    row.put("currentStock", rs.getInt("current_stock"));
                    res.rows.add(row);

                    if (chartLabels.size() < 7) {
                        chartLabels.add(title.length() > 20 ? title.substring(0, 18) + "..." : title);
                        chartValues.add(sold);
                    }
                }
            }
        } catch (SQLException e) {
            Map<Integer, Map<String, Object>> prodMap = new HashMap<>();
            for (Order o : memoryOrders) {
                if (o.getCreatedAt() != null && o.getCreatedAt().length() >= 10) {
                    String d = o.getCreatedAt().substring(0, 10);
                    if (d.compareTo(res.fromDate) >= 0 && d.compareTo(res.toDate) <= 0 && !"DA_HUY".equalsIgnoreCase(o.getOrderStatus())) {
                        if (o.getItems() != null) {
                            for (OrderItem item : o.getItems()) {
                                Map<String, Object> m = prodMap.computeIfAbsent(item.getBookId(), k -> {
                                    Map<String, Object> r = new HashMap<>();
                                    r.put("bookId", k);
                                    r.put("bookCode", item.getBookCode());
                                    r.put("bookTitle", item.getBookTitle());
                                    r.put("category", "Sách Bookora");
                                    r.put("price", item.getPrice());
                                    r.put("totalSold", 0L);
                                    r.put("totalRevenue", 0.0);
                                    r.put("currentStock", 20);
                                    return r;
                                });
                                m.put("totalSold", ((Long)m.get("totalSold")) + item.getQuantity());
                                m.put("totalRevenue", ((Double)m.get("totalRevenue")) + item.getSubtotal());
                            }
                        }
                    }
                }
            }

            for (Map<String, Object> m : prodMap.values()) {
                long sold = (Long) m.get("totalSold");
                double rev = (Double) m.get("totalRevenue");
                grandTotalUnits += sold;
                grandTotalRev += rev;
                if (sold > topProductCount) {
                    topProductCount = sold;
                    topProductTitle = (String) m.get("bookTitle");
                }
                res.rows.add(m);
            }
        }

        res.summary.put("totalUnitsSold", grandTotalUnits);
        res.summary.put("totalProductRevenue", grandTotalRev);
        res.summary.put("topProductTitle", topProductTitle);
        res.summary.put("topProductCount", topProductCount);

        res.chartData.put("labels", chartLabels);
        res.chartData.put("values", chartValues);
    }

    private static void fetchInventoryReport(ReportResult res, String search) {
        String sql = "SELECT b.id, b.code, b.title, b.author, b.publisher, b.category, b.price, b.stock, COALESCE(k.warehouse_location, 'Kho chính - Tầng 1') AS warehouse_location, COALESCE(k.status, IF(b.stock > 5, 'CON_HANG', IF(b.stock > 0, 'SAP_HET', 'HET_HANG'))) AS stock_status FROM books b LEFT JOIN kho k ON b.id = k.book_id ORDER BY b.stock ASC";

        long totalBooksCount = 0;
        double totalStockValue = 0;
        long inStockCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String title = rs.getString("title");
                String code = rs.getString("code");
                int stock = rs.getInt("stock");
                double price = rs.getDouble("price");

                if (search != null && !search.trim().isEmpty()) {
                    String s = search.trim().toLowerCase();
                    boolean match = (title != null && title.toLowerCase().contains(s))
                            || (code != null && code.toLowerCase().contains(s))
                            || (rs.getString("category") != null && rs.getString("category").toLowerCase().contains(s));
                    if (!match) continue;
                }

                totalBooksCount++;
                totalStockValue += (stock * price);

                if (stock > 5) inStockCount++;
                else if (stock > 0) lowStockCount++;
                else outOfStockCount++;

                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("code", code);
                row.put("title", title);
                row.put("author", rs.getString("author"));
                row.put("publisher", rs.getString("publisher"));
                row.put("category", rs.getString("category"));
                row.put("price", price);
                row.put("stock", stock);
                row.put("stockValue", stock * price);
                row.put("warehouseLocation", rs.getString("warehouse_location"));
                row.put("stockStatus", rs.getString("stock_status"));
                res.rows.add(row);
            }
        } catch (SQLException e) {
            for (Book b : memoryBooks) {
                totalBooksCount++;
                totalStockValue += (b.getStock() * b.getPrice());
                if (b.getStock() > 5) inStockCount++;
                else if (b.getStock() > 0) lowStockCount++;
                else outOfStockCount++;

                Map<String, Object> row = new HashMap<>();
                row.put("id", b.getId());
                row.put("code", b.getCode());
                row.put("title", b.getTitle());
                row.put("author", b.getAuthor());
                row.put("publisher", b.getPublisher());
                row.put("category", b.getCategory());
                row.put("price", b.getPrice());
                row.put("stock", b.getStock());
                row.put("stockValue", b.getStock() * b.getPrice());
                row.put("warehouseLocation", "Kho chính - Tầng 1");
                row.put("stockStatus", b.getStock() > 5 ? "CON_HANG" : (b.getStock() > 0 ? "SAP_HET" : "HET_HANG"));
                res.rows.add(row);
            }
        }

        res.summary.put("totalBooksCount", totalBooksCount);
        res.summary.put("totalStockValue", totalStockValue);
        res.summary.put("inStockCount", inStockCount);
        res.summary.put("lowStockCount", lowStockCount);
        res.summary.put("outOfStockCount", outOfStockCount);
    }

    private static void fetchCustomersReport(ReportResult res, String search) {
        String sql = "SELECT u.username, u.full_name, u.email, u.phone, u.status, u.created_at, COUNT(dh.id) AS order_count, COALESCE(SUM(IF(UPPER(dh.order_status) != 'DA_HUY', dh.total_amount, 0)), 0) AS total_spent FROM users u LEFT JOIN don_hang dh ON LOWER(u.username) = LOWER(dh.username) AND DATE(dh.created_at) >= ? AND DATE(dh.created_at) <= ? WHERE UPPER(u.role) = 'CUSTOMER' GROUP BY u.username, u.full_name, u.email, u.phone, u.status, u.created_at ORDER BY total_spent DESC";

        long totalCustomers = 0;
        long activeCustomers = 0;
        long purchasingCustomers = 0;
        double grandTotalSpent = 0;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, res.fromDate);
            ps.setString(2, res.toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String user = rs.getString("username");
                    String name = rs.getString("full_name");
                    long orders = rs.getLong("order_count");
                    double spent = rs.getDouble("total_spent");
                    String status = rs.getString("status");

                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.trim().toLowerCase();
                        boolean match = (user != null && user.toLowerCase().contains(s))
                                || (name != null && name.toLowerCase().contains(s))
                                || (rs.getString("email") != null && rs.getString("email").toLowerCase().contains(s));
                        if (!match) continue;
                    }

                    totalCustomers++;
                    if (!"LOCKED".equalsIgnoreCase(status)) activeCustomers++;
                    if (orders > 0) purchasingCustomers++;
                    grandTotalSpent += spent;

                    Map<String, Object> row = new HashMap<>();
                    row.put("username", user);
                    row.put("fullName", name);
                    row.put("email", rs.getString("email"));
                    row.put("phone", rs.getString("phone"));
                    row.put("status", status != null ? status : "ACTIVE");
                    row.put("orderCount", orders);
                    row.put("totalSpent", spent);
                    row.put("createdAt", rs.getString("created_at"));
                    res.rows.add(row);
                }
            }
        } catch (SQLException e) {
            for (User u : memoryUsers.values()) {
                if ("CUSTOMER".equalsIgnoreCase(u.getRole())) {
                    totalCustomers++;
                    if (!"LOCKED".equalsIgnoreCase(u.getStatus())) activeCustomers++;

                    long orders = 0;
                    double spent = 0;
                    for (Order o : memoryOrders) {
                        if (u.getUsername().equalsIgnoreCase(o.getUsername()) && !"DA_HUY".equalsIgnoreCase(o.getOrderStatus())) {
                            orders++;
                            spent += o.getTotalAmount();
                        }
                    }

                    if (orders > 0) purchasingCustomers++;
                    grandTotalSpent += spent;

                    Map<String, Object> row = new HashMap<>();
                    row.put("username", u.getUsername());
                    row.put("fullName", u.getFullName());
                    row.put("email", u.getEmail());
                    row.put("phone", u.getPhone());
                    row.put("status", u.getStatus() != null ? u.getStatus() : "ACTIVE");
                    row.put("orderCount", orders);
                    row.put("totalSpent", spent);
                    row.put("createdAt", "2026-01-01");
                    res.rows.add(row);
                }
            }
        }

        res.summary.put("totalCustomers", totalCustomers);
        res.summary.put("activeCustomers", activeCustomers);
        res.summary.put("purchasingCustomers", purchasingCustomers);
        res.summary.put("grandTotalSpent", grandTotalSpent);
    }

    // =========================================================
    // UC13 - QUẢN LÝ NỘI DUNG CHATBOT AI (CHATBOT_NOI_DUNG)
    // =========================================================

    private static final List<ChatbotContent> chatbotContentList = Collections.synchronizedList(new ArrayList<>());
    private static boolean chatbotInitialized = false;

    private static synchronized void ensureChatbotLoaded() {
        if (chatbotInitialized) return;

        List<ChatbotContent> list = new ArrayList<>();
        String sql = "SELECT * FROM chatbot_noi_dung ORDER BY id ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ChatbotContent item = new ChatbotContent(
                    rs.getInt("id"),
                    rs.getString("category_type"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("keywords"),
                    rs.getString("status"),
                    rs.getString("created_at"),
                    rs.getString("updated_at")
                );
                list.add(item);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi đọc chatbot_noi_dung từ MySQL: " + e.getMessage());
        }

        if (list.isEmpty()) {
            // Seed default 5 items
            list.add(new ChatbotContent(1, "CHINH_SACH_MUA_HANG", "Quy trình và điều kiện đặt hàng sách", "Quý khách có thể chọn sách, thêm vào giỏ hàng, chọn địa chỉ nhận hàng và phương thức thanh toán để hoàn tất đơn hàng. Bookora tiếp nhận đơn hàng 24/7 trên phạm vi toàn quốc.", "mua hàng, quy trình đặt hàng, làm sao đặt sách, hướng dẫn mua sách", "ACTIVE", "2026-01-01 00:00:00", "2026-01-01 00:00:00"));
            list.add(new ChatbotContent(2, "CHINH_SACH_THANH_TOAN", "Phương thức thanh toán hỗ trợ", "Bookora hỗ trợ 2 phương thức thanh toán: 1) Thanh toán tiền mặt khi nhận hàng (COD). 2) Thanh toán trực tuyến chuyển khoản ngân hàng nhanh VietQR/MoMo 24/7 hoàn toàn không mất phí.", "thanh toán, cod, chuyển khoản, vietqr, momo, tiền mặt", "ACTIVE", "2026-01-01 00:00:00", "2026-01-01 00:00:00"));
            list.add(new ChatbotContent(3, "CHINH_SACH_GIAO_HANG", "Phạm vi, thời gian và phí vận chuyển", "Phạm vi giao hàng: Toàn quốc. Phí giao hàng tiêu chuẩn là 30.000đ. Đơn hàng từ 250.000đ được MIỄN PHÍ VẬN CHUYỂN toàn quốc. Thời gian giao hàng từ 1 - 3 ngày làm việc.", "giao hàng, phí ship, bao lâu nhận được, miễn phí vận chuyển, freeship", "ACTIVE", "2026-01-01 00:00:00", "2026-01-01 00:00:00"));
            list.add(new ChatbotContent(4, "CAU_HOI_THUONG_GAP", "Chính sách đổi trả và hoàn tiền", "Khách hàng được quyền yêu cầu đổi trả sách trong vòng 7 ngày kể từ khi nhận hàng nếu sách có lỗi kỹ thuật (rách bìa, thiếu trang, rách hỏng do vận chuyển). Liên hệ hotline 0988123456 để được hỗ trợ.", "đổi trả, hoàn tiền, rách sách, sách lỗi, chính sách trả hàng", "ACTIVE", "2026-01-01 00:00:00", "2026-01-01 00:00:00"));
            list.add(new ChatbotContent(5, "THONG_TIN_SACH", "Giới thiệu danh mục sách bán chạy & khuyến mãi", "Bookora sở hữu bộ sưu tập sách phong phú gồm các thể loại: Văn học, Kỹ năng sống, Công nghệ & AI, Kinh tế, Tâm lý học. Nhiều chương trình ưu đãi giảm đến 25% kèm quà tặng bookmark độc quyền.", "sách bán chạy, danh mục sách, ưu đãi sách, giảm giá sách, thể loại sách", "ACTIVE", "2026-01-01 00:00:00", "2026-01-01 00:00:00"));
        }

        chatbotContentList.clear();
        chatbotContentList.addAll(list);
        chatbotInitialized = true;
    }

    public static List<ChatbotContent> getAllChatbotContents(String categoryType, String search) {
        ensureChatbotLoaded();
        List<ChatbotContent> result = new ArrayList<>();

        String catFilter = categoryType != null ? categoryType.trim().toUpperCase() : "";
        String searchFilter = search != null ? search.trim().toLowerCase() : "";

        synchronized (chatbotContentList) {
            for (ChatbotContent item : chatbotContentList) {
                boolean matchCat = catFilter.isEmpty() || "ALL".equals(catFilter) || item.getCategoryType().toUpperCase().equals(catFilter);
                boolean matchSearch = searchFilter.isEmpty()
                    || item.getTitle().toLowerCase().contains(searchFilter)
                    || item.getContent().toLowerCase().contains(searchFilter)
                    || (item.getKeywords() != null && item.getKeywords().toLowerCase().contains(searchFilter));

                if (matchCat && matchSearch) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    public static ChatbotContent getChatbotContentById(int id) {
        ensureChatbotLoaded();
        synchronized (chatbotContentList) {
            for (ChatbotContent item : chatbotContentList) {
                if (item.getId() == id) return item;
            }
        }
        return null;
    }

    public static String addChatbotContent(ChatbotContent item) {
        if (item == null) return "Dữ liệu nhập không hợp lệ.";
        if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
            return "Tiêu đề không được để trống.";
        }
        if (item.getContent() == null || item.getContent().trim().isEmpty()) {
            return "Nội dung không được để trống.";
        }
        if (item.getCategoryType() == null || item.getCategoryType().trim().isEmpty()) {
            return "Loại nội dung không được để trống.";
        }

        ensureChatbotLoaded();

        String sql = "INSERT INTO chatbot_noi_dung (category_type, title, content, keywords, status) VALUES (?, ?, ?, ?, ?)";
        int newId = 0;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getCategoryType().trim());
            ps.setString(2, item.getTitle().trim());
            ps.setString(3, item.getContent().trim());
            ps.setString(4, item.getKeywords() != null ? item.getKeywords().trim() : "");
            ps.setString(5, item.getStatus() != null ? item.getStatus() : "ACTIVE");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) newId = rs.getInt(1);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi thêm chatbot_noi_dung vào DB: " + e.getMessage());
        }

        if (newId <= 0) {
            // Auto generate ID for in-memory fallback
            int maxId = 0;
            synchronized (chatbotContentList) {
                for (ChatbotContent c : chatbotContentList) {
                    if (c.getId() > maxId) maxId = c.getId();
                }
            }
            newId = maxId + 1;
        }

        item.setId(newId);
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        item.setCreatedAt(nowStr);
        item.setUpdatedAt(nowStr);

        synchronized (chatbotContentList) {
            chatbotContentList.add(item);
        }

        return null; // success
    }

    public static String updateChatbotContent(ChatbotContent item) {
        if (item == null) return "Dữ liệu nhập không hợp lệ.";
        if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
            return "Tiêu đề không được để trống.";
        }
        if (item.getContent() == null || item.getContent().trim().isEmpty()) {
            return "Nội dung không được để trống.";
        }
        if (item.getCategoryType() == null || item.getCategoryType().trim().isEmpty()) {
            return "Loại nội dung không được để trống.";
        }

        ensureChatbotLoaded();
        ChatbotContent existing = getChatbotContentById(item.getId());
        if (existing == null) {
            return "Nội dung Chatbot không tồn tại!";
        }

        String sql = "UPDATE chatbot_noi_dung SET category_type = ?, title = ?, content = ?, keywords = ?, status = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getCategoryType().trim());
            ps.setString(2, item.getTitle().trim());
            ps.setString(3, item.getContent().trim());
            ps.setString(4, item.getKeywords() != null ? item.getKeywords().trim() : "");
            ps.setString(5, item.getStatus() != null ? item.getStatus() : "ACTIVE");
            ps.setInt(6, item.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi cập nhật chatbot_noi_dung vào DB: " + e.getMessage());
        }

        existing.setCategoryType(item.getCategoryType().trim());
        existing.setTitle(item.getTitle().trim());
        existing.setContent(item.getContent().trim());
        existing.setKeywords(item.getKeywords() != null ? item.getKeywords().trim() : "");
        existing.setStatus(item.getStatus() != null ? item.getStatus() : "ACTIVE");
        existing.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return null; // success
    }

    public static boolean deleteChatbotContent(int id) {
        ensureChatbotLoaded();
        String sql = "DELETE FROM chatbot_noi_dung WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi xóa chatbot_noi_dung từ DB: " + e.getMessage());
        }

        synchronized (chatbotContentList) {
            return chatbotContentList.removeIf(c -> c.getId() == id);
        }
    }

    /**
     * Tìm câu trả lời trong Tri thức Chatbot AI từ dữ liệu CHATBOT_NOI_DUNG
     */
    public static String findChatbotAnswer(String message) {
        if (message == null || message.trim().isEmpty()) return null;
        ensureChatbotLoaded();
        String lowerMsg = message.trim().toLowerCase();

        synchronized (chatbotContentList) {
            for (ChatbotContent item : chatbotContentList) {
                if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) continue;

                // Match keywords
                if (item.getKeywords() != null && !item.getKeywords().isEmpty()) {
                    String[] kw = item.getKeywords().split("[,;]");
                    for (String k : kw) {
                        String cleanK = k.trim().toLowerCase();
                        if (!cleanK.isEmpty() && lowerMsg.contains(cleanK)) {
                            return item.getContent();
                        }
                    }
                }

                // Match title
                if (item.getTitle() != null && lowerMsg.contains(item.getTitle().toLowerCase())) {
                    return item.getContent();
                }

                // Topic specific match
                if ("CHINH_SACH_GIAO_HANG".equals(item.getCategoryType()) &&
                    (lowerMsg.contains("giao hàng") || lowerMsg.contains("phí ship") || lowerMsg.contains("bao lâu nhận được") || lowerMsg.contains("freeship"))) {
                    return item.getContent();
                }

                if ("CHINH_SACH_THANH_TOAN".equals(item.getCategoryType()) &&
                    (lowerMsg.contains("thanh toán") || lowerMsg.contains("cod") || lowerMsg.contains("vietqr") || lowerMsg.contains("momo") || lowerMsg.contains("chuyển khoản"))) {
                    return item.getContent();
                }

                if ("CHINH_SACH_MUA_HANG".equals(item.getCategoryType()) &&
                    (lowerMsg.contains("mua hàng") || lowerMsg.contains("quy trình") || lowerMsg.contains("đặt sách"))) {
                    return item.getContent();
                }

                if ("CAU_HOI_THUONG_GAP".equals(item.getCategoryType()) &&
                    (lowerMsg.contains("đổi trả") || lowerMsg.contains("hoàn tiền") || lowerMsg.contains("rách") || lowerMsg.contains("lỗi"))) {
                    return item.getContent();
                }
            }
        }
        return null;
    }

    // =========================================================================
    // HỆ THỐNG ĐÁNH GIÁ SÁCH (BOOK REVIEWS & RATINGS)
    // =========================================================================

    public static void ensureReviewTableSchema() {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS danh_gia ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "username VARCHAR(50) NOT NULL, "
                + "book_id INT NOT NULL, "
                + "order_id INT DEFAULT NULL, "
                + "rating INT NOT NULL, "
                + "content TEXT NOT NULL, "
                + "status VARCHAR(20) DEFAULT 'HIEN_THI', "
                + "block_reason VARCHAR(255) DEFAULT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE, "
                + "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sqlCreate);

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM danh_gia")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String[] seedReviews = {
                        "INSERT INTO danh_gia (username, book_id, order_id, rating, content, status, created_at) VALUES ('khachhang', 1, 101, 5, 'Sách hay tuyệt vời, giấy in nét và bọc cẩn thận. Đáng mua!', 'HIEN_THI', NOW())",
                        "INSERT INTO danh_gia (username, book_id, order_id, rating, content, status, created_at) VALUES ('oanh', 1, 101, 5, 'Nội dung rất triết lý và truyền cảm hứng. Giao hàng cực nhanh!', 'HIEN_THI', NOW())",
                        "INSERT INTO danh_gia (username, book_id, order_id, rating, content, status, created_at) VALUES ('khachhang', 2, 104, 5, 'Đắc Nhân Tâm cuốn sách kinh điển mọi người nên đọc ít nhất một lần.', 'HIEN_THI', NOW())"
                    };
                    for (String sql : seedReviews) {
                        try { stmt.executeUpdate(sql); } catch (SQLException ignored) {}
                    }
                }
            }
        } catch (SQLException ignored) {}
    }

    public enum ReviewEligibilityResult {
        ELIGIBLE,              // Đủ điều kiện đánh giá (Đã mua + Đã giao)
        NOT_PURCHASED,         // Chưa mua sách
        ORDER_NOT_DELIVERED    // Đã mua nhưng đơn chưa giao
    }

    public static boolean isOrderDelivered(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return "HOAN_THANH".equals(s) || "DA_GIAO".equals(s) || "DELIVERED".equals(s) 
            || "ĐÃ GIAO".equalsIgnoreCase(status) || "ĐÃ HOÀN THÀNH".equalsIgnoreCase(status);
    }

    public static ReviewEligibilityResult checkReviewEligibility(String username, int bookId) {
        if (username == null || username.trim().isEmpty()) {
            return ReviewEligibilityResult.NOT_PURCHASED;
        }

        String sql = "SELECT dh.order_status FROM ct_don_hang ct "
                   + "JOIN don_hang dh ON ct.order_id = dh.id "
                   + "WHERE LOWER(dh.username) = ? AND ct.book_id = ?";

        boolean hasPurchasedAny = false;
        boolean hasDeliveredOrder = false;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasPurchasedAny = true;
                    String status = rs.getString("order_status");
                    if (isOrderDelivered(status)) {
                        hasDeliveredOrder = true;
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error checking review eligibility: " + e.getMessage());
        }

        // Memory fallback search if MySQL returned false or DB disconnected
        if (!hasDeliveredOrder) {
            for (Order o : memoryOrders) {
                if (username.equalsIgnoreCase(o.getUsername())) {
                    boolean containsBook = false;
                    if (o.getItems() != null) {
                        for (OrderItem item : o.getItems()) {
                            if (item.getBookId() == bookId) {
                                containsBook = true;
                                break;
                            }
                        }
                    }
                    if (containsBook) {
                        hasPurchasedAny = true;
                        if (isOrderDelivered(o.getOrderStatus())) {
                            hasDeliveredOrder = true;
                            break;
                        }
                    }
                }
            }
        }

        if (hasDeliveredOrder) {
            return ReviewEligibilityResult.ELIGIBLE;
        } else if (hasPurchasedAny) {
            return ReviewEligibilityResult.ORDER_NOT_DELIVERED;
        } else {
            return ReviewEligibilityResult.NOT_PURCHASED;
        }
    }

    public static Review getExistingUserReview(String username, int bookId) {
        if (username == null || username.trim().isEmpty()) return null;
        String sql = "SELECT r.*, u.full_name, u.avatar FROM danh_gia r "
                   + "LEFT JOIN users u ON r.username = u.username "
                   + "WHERE LOWER(r.username) = ? AND r.book_id = ? ORDER BY r.id DESC LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Review r = new Review();
                    r.setId(rs.getInt("id"));
                    r.setUsername(rs.getString("username"));
                    r.setFullName(rs.getString("full_name") != null ? rs.getString("full_name") : rs.getString("username"));
                    r.setAvatar(rs.getString("avatar"));
                    r.setBookId(rs.getInt("book_id"));
                    r.setOrderId((Integer) rs.getObject("order_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setContent(rs.getString("content"));
                    r.setStatus(rs.getString("status"));
                    r.setBlockReason(rs.getString("block_reason"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    return r;
                }
            }
        } catch (SQLException ignored) {}

        for (Review r : memoryReviews) {
            if (username.equalsIgnoreCase(r.getUsername()) && r.getBookId() == bookId) {
                return r;
            }
        }
        return null;
    }

    public static boolean saveReview(Review review) {
        if (review == null) return false;

        String checkExistSql = "SELECT id FROM danh_gia WHERE LOWER(username) = ? AND book_id = ?";
        Integer existingId = null;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkExistSql)) {
            ps.setString(1, review.getUsername().trim().toLowerCase());
            ps.setInt(2, review.getBookId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    existingId = rs.getInt("id");
                }
            }
        } catch (SQLException ignored) {}

        if (existingId != null) {
            review.setId(existingId);
            String updateSql = "UPDATE danh_gia SET rating = ?, content = ?, status = ?, block_reason = ?, updated_at = NOW() WHERE id = ?";
            try (Connection conn = DBContext.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, review.getRating());
                ps.setString(2, review.getContent());
                ps.setString(3, review.getStatus());
                ps.setString(4, review.getBlockReason());
                ps.setInt(5, existingId);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("⚠️ MySQL updateReview error: " + e.getMessage());
            }
        } else {
            String insertSql = "INSERT INTO danh_gia (username, book_id, order_id, rating, content, status, block_reason, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            try (Connection conn = DBContext.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, review.getUsername().trim().toLowerCase());
                ps.setInt(2, review.getBookId());
                if (review.getOrderId() != null) {
                    ps.setInt(3, review.getOrderId());
                } else {
                    ps.setNull(3, java.sql.Types.INTEGER);
                }
                ps.setInt(4, review.getRating());
                ps.setString(5, review.getContent());
                ps.setString(6, review.getStatus());
                ps.setString(7, review.getBlockReason());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        review.setId(rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                System.err.println("⚠️ MySQL saveReview error: " + e.getMessage());
            }
        }

        memoryReviews.removeIf(r -> r.getUsername().equalsIgnoreCase(review.getUsername()) && r.getBookId() == review.getBookId());
        if (review.getCreatedAt() == null) {
            review.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }
        memoryReviews.add(review);

        // Chỉ tính toán lại điểm số nếu trạng thái là HIEN_THI (Hợp lệ)
        recalculateBookRating(review.getBookId());
        return true;
    }

    public static void recalculateBookRating(int bookId) {
        String sql = "SELECT COUNT(*) as cnt, AVG(rating) as avg_rating FROM danh_gia WHERE book_id = ? AND status = 'HIEN_THI'";
        int totalReviews = 0;
        double avgRating = 5.0;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalReviews = rs.getInt("cnt");
                    if (totalReviews > 0) {
                        avgRating = rs.getDouble("avg_rating");
                        avgRating = Math.round(avgRating * 10.0) / 10.0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error recalculating rating: " + e.getMessage());
        }

        if (totalReviews == 0) {
            int memCount = 0;
            double memSum = 0;
            for (Review r : memoryReviews) {
                if (r.getBookId() == bookId && "HIEN_THI".equalsIgnoreCase(r.getStatus())) {
                    memCount++;
                    memSum += r.getRating();
                }
            }
            if (memCount > 0) {
                totalReviews = memCount;
                avgRating = Math.round((memSum / memCount) * 10.0) / 10.0;
            }
        }

        String updateBookSql = "UPDATE books SET rating = ?, review_count = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateBookSql)) {
            ps.setDouble(1, avgRating);
            ps.setInt(2, totalReviews);
            ps.setInt(3, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠️ Error updating book rating in DB: " + e.getMessage());
        }

        Book b = getBookById(bookId);
        if (b != null) {
            b.setRating(avgRating);
            b.setReviewCount(totalReviews);
        }
    }

    public static List<Review> getReviewsForBook(int bookId) {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT r.*, u.full_name, u.avatar FROM danh_gia r "
                   + "LEFT JOIN users u ON r.username = u.username "
                   + "WHERE r.book_id = ? AND r.status = 'HIEN_THI' "
                   + "ORDER BY r.created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review();
                    r.setId(rs.getInt("id"));
                    r.setUsername(rs.getString("username"));
                    r.setFullName(rs.getString("full_name") != null ? rs.getString("full_name") : rs.getString("username"));
                    r.setAvatar(rs.getString("avatar") != null ? rs.getString("avatar") : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
                    r.setBookId(rs.getInt("book_id"));
                    r.setOrderId((Integer) rs.getObject("order_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setContent(rs.getString("content"));
                    r.setStatus(rs.getString("status"));
                    r.setBlockReason(rs.getString("block_reason"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error getting reviews for book: " + e.getMessage());
        }

        if (list.isEmpty()) {
            for (Review r : memoryReviews) {
                if (r.getBookId() == bookId && "HIEN_THI".equalsIgnoreCase(r.getStatus())) {
                    if (r.getFullName() == null) {
                        User u = findUser(r.getUsername());
                        if (u != null) {
                            r.setFullName(u.getFullName());
                            r.setAvatar(u.getAvatar());
                        } else {
                            r.setFullName(r.getUsername());
                        }
                    }
                    list.add(r);
                }
            }
        }
        return list;
    }

    public static List<Review> getAllReviewsForAdmin(String statusFilter) {
        List<Review> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT r.*, u.full_name, u.avatar, b.title as book_title FROM danh_gia r ")
                   .append("LEFT JOIN users u ON r.username = u.username ")
                   .append("LEFT JOIN books b ON r.book_id = b.id WHERE 1=1 ");

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append("AND r.status = ? ");
        }
        sql.append("ORDER BY r.created_at DESC");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
                ps.setString(1, statusFilter.trim().toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review();
                    r.setId(rs.getInt("id"));
                    r.setUsername(rs.getString("username"));
                    r.setFullName(rs.getString("full_name") != null ? rs.getString("full_name") : rs.getString("username"));
                    r.setAvatar(rs.getString("avatar") != null ? rs.getString("avatar") : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
                    r.setBookId(rs.getInt("book_id"));
                    r.setBookTitle(rs.getString("book_title"));
                    r.setOrderId((Integer) rs.getObject("order_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setContent(rs.getString("content"));
                    r.setStatus(rs.getString("status"));
                    r.setBlockReason(rs.getString("block_reason"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error getting admin reviews: " + e.getMessage());
        }

        if (list.isEmpty()) {
            for (Review r : memoryReviews) {
                if (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter) || r.getStatus().equalsIgnoreCase(statusFilter)) {
                    list.add(r);
                }
            }
        }
        return list;
    }

    public static boolean updateReviewStatus(int reviewId, String newStatus) {
        String sql = "UPDATE danh_gia SET status = ? WHERE id = ?";
        int bookId = 0;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, reviewId);
            ps.executeUpdate();

            try (PreparedStatement ps2 = conn.prepareStatement("SELECT book_id FROM danh_gia WHERE id = ?")) {
                ps2.setInt(1, reviewId);
                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) {
                        bookId = rs.getInt("book_id");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error updating review status: " + e.getMessage());
        }

        for (Review r : memoryReviews) {
            if (r.getId() == reviewId) {
                r.setStatus(newStatus);
                bookId = r.getBookId();
                break;
            }
        }

        if (bookId > 0) {
            recalculateBookRating(bookId);
        }
        return true;
    }
}


