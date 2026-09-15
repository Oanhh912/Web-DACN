package com.bookstore.data;

import com.bookstore.model.Book;
import com.bookstore.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kho dữ liệu mô phỏng trong bộ nhớ cho Bookstore.
 */
public class DataStore {
    private static final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private static final List<Book> books = Collections.synchronizedList(new ArrayList<>());

    static {
        initUsers();
        initBooks();
    }

    private static void initUsers() {
        // Tài khoản mặc định để thử nghiệm
        User admin = new User(
                "admin",
                "123456",
                "Quản Trị Viên - Hoàng Oanh",
                "admin@bookstore.vn",
                "0988123456",
                "ADMIN",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
        );

        User oanh = new User(
                "oanh",
                "123456",
                "Hoàng Oanh",
                "oanh.nguyen@gmail.com",
                "0912345678",
                "CUSTOMER",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"
        );

        User khachhang = new User(
                "khachhang",
                "123456",
                "Khách Hàng Thân Thiết",
                "khachhang@gmail.com",
                "0909888999",
                "CUSTOMER",
                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
        );

        users.put(admin.getUsername().toLowerCase(), admin);
        users.put(oanh.getUsername().toLowerCase(), oanh);
        users.put(khachhang.getUsername().toLowerCase(), khachhang);
    }

    private static void initBooks() {
        books.add(new Book(
                1,
                "Nhà Giả Kim (The Alchemist)",
                "Paulo Coelho",
                79000,
                99000,
                "Văn học",
                4.9,
                1420,
                "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&auto=format&fit=crop&q=80",
                "Cuốn sách kinh điển kể về hành trình theo đuổi ước mơ và lắng nghe tiếng gọi của trái tim của chàng chăn cừu Santiago.",
                true
        ));

        books.add(new Book(
                2,
                "Đắc Nhân Tâm (How to Win Friends)",
                "Dale Carnegie",
                88000,
                110000,
                "Kỹ năng sống",
                4.8,
                2350,
                "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500&auto=format&fit=crop&q=80",
                "Nghệ thuật thu phục lòng người đỉnh cao, cuốn sách gối đầu giường của hàng triệu độc giả trên toàn thế giới.",
                true
        ));

        books.add(new Book(
                3,
                "Clean Code: A Handbook of Agile Software Craftsmanship",
                "Robert C. Martin (Uncle Bob)",
                285000,
                350000,
                "Công nghệ",
                4.9,
                890,
                "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500&auto=format&fit=crop&q=80",
                "Cẩm nang kinh điển của mọi lập trình viên phần mềm để viết mã nguồn sạch sẽ, dễ bảo trì và mở rộng.",
                true
        ));

        books.add(new Book(
                4,
                "Tư Duy Nhanh Và Chậm (Thinking, Fast and Slow)",
                "Daniel Kahneman",
                145000,
                185000,
                "Tâm lý học",
                4.7,
                640,
                "https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=500&auto=format&fit=crop&q=80",
                "Giải thích hai hệ thống thúc đẩy cách chúng ta tư duy và đưa ra các quyết định trong cuộc sống.",
                false
        ));

        books.add(new Book(
                5,
                "Cha Giàu Cha Nghèo (Rich Dad Poor Dad)",
                "Robert T. Kiyosaki",
                95000,
                125000,
                "Kinh tế",
                4.8,
                1890,
                "https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=500&auto=format&fit=crop&q=80",
                "Bài học về tư duy tài chính độc lập và cách để đồng tiền làm việc cho chính bạn thay vì làm việc vì tiền.",
                true
        ));

        books.add(new Book(
                6,
                "Muôn Kiếp Nhân Sinh (Phần 1 & 2)",
                "Nguyên Phong",
                168000,
                210000,
                "Tâm linh & Đời sống",
                4.9,
                1560,
                "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500&auto=format&fit=crop&q=80",
                "Tác phẩm ghi lại những trải nghiệm tiền kiếp kỳ lạ và thông điệp sâu sắc về quy luật nhân quả của vũ trụ.",
                true
        ));

        books.add(new Book(
                7,
                "Cây Cam Ngọt Của Tôi",
                "José Mauro de Vasconcelos",
                82000,
                108000,
                "Văn học",
                4.9,
                3200,
                "https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=500&auto=format&fit=crop&q=80",
                "Câu chuyện cảm động rơi nước mắt về tuổi thơ hồn nhiên nhưng đầy vết thương của cậu bé Zezé.",
                true
        ));

        books.add(new Book(
                8,
                "Thiết Kế Giải Thuật & Cấu Trúc Dữ Liệu",
                "Thomas H. Cormen",
                320000,
                390000,
                "Công nghệ",
                4.8,
                410,
                "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500&auto=format&fit=crop&q=80",
                "Giáo trình toàn diện về giải thuật cơ bản và nâng cao dành cho sinh viên và kỹ sư công nghệ thông tin.",
                false
        ));

        books.add(new Book(
                9,
                "Thói Quen Nguyên Tử (Atomic Habits)",
                "James Clear",
                129000,
                169000,
                "Kỹ năng sống",
                4.9,
                2900,
                "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500&auto=format&fit=crop&q=80",
                "Cách mạng hóa cuộc sống của bạn từ những thay đổi cực kỳ nhỏ bé nhưng mang lại hiệu quả phi thường mỗi ngày.",
                true
        ));
    }

    public static User findUser(String username) {
        if (username == null) return null;
        return users.get(username.trim().toLowerCase());
    }

    public static boolean validateUser(String username, String password) {
        User user = findUser(username);
        if (user == null) return false;
        return user.getPassword().equals(password);
    }

    public static boolean registerUser(User newUser) {
        if (newUser == null || newUser.getUsername() == null) return false;
        String key = newUser.getUsername().trim().toLowerCase();
        if (users.containsKey(key)) {
            return false; // đã tồn tại
        }
        users.put(key, newUser);
        return true;
    }

    public static List<Book> getAllBooks() {
        return new ArrayList<>(books);
    }

    public static Book getBookById(int id) {
        for (Book b : books) {
            if (b.getId() == id) {
                return b;
            }
        }
        return null;
    }

    public static List<Book> searchBooks(String keyword, String category) {
        List<Book> result = new ArrayList<>();
        String kw = keyword == null ? "" : keyword.toLowerCase().trim();
        String cat = category == null ? "" : category.trim();

        for (Book b : books) {
            boolean matchKeyword = kw.isEmpty() ||
                    b.getTitle().toLowerCase().contains(kw) ||
                    b.getAuthor().toLowerCase().contains(kw);

            boolean matchCategory = cat.isEmpty() || cat.equalsIgnoreCase("Tất cả") ||
                    b.getCategory().equalsIgnoreCase(cat);

            if (matchKeyword && matchCategory) {
                result.add(b);
            }
        }
        return result;
    }

    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("Tất cả");
        for (Book b : books) {
            if (!categories.contains(b.getCategory())) {
                categories.add(b.getCategory());
            }
        }
        return categories;
    }
}
