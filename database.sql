-- =========================================================
-- HỆ THỐNG CƠ SỞ DỮ LIỆU CHO WEBSITE BÁN SÁCH BOOKORA
-- Tương thích: MySQL 8.0+ / MariaDB 10.4+ (XAMPP)
-- Database Name: web_bookora
-- =========================================================

CREATE DATABASE IF NOT EXISTS `web_bookora` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `web_bookora`;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. BẢNG NGƯỜI DÙNG (USERS)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100),
    `phone` VARCHAR(20),
    `role` VARCHAR(20) DEFAULT 'CUSTOMER',
    `avatar` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm dữ liệu tài khoản mẫu
INSERT INTO `users` (`username`, `password`, `full_name`, `email`, `phone`, `role`, `avatar`, `status`) VALUES
('admin', '123456', 'Quản Trị Viên - Hoàng Oanh', 'admin@bookora.vn', '0988123456', 'ADMIN', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', 'ACTIVE'),
('oanh', '123456', 'Hoàng Oanh', 'oanh.nguyen@gmail.com', '0912345678', 'CUSTOMER', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', 'ACTIVE'),
('khachhang', '123456', 'Khách Hàng Thân Thiết', 'khachhang@gmail.com', '0909888999', 'CUSTOMER', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', 'ACTIVE');

-- 2. BẢNG SÁCH (BOOKS / SACH)
DROP TABLE IF EXISTS `books`;
CREATE TABLE `books` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `title` VARCHAR(255) NOT NULL,
    `author` VARCHAR(150) NOT NULL,
    `publisher` VARCHAR(150) NOT NULL DEFAULT 'NXB Trẻ',
    `price` DOUBLE NOT NULL,
    `original_price` DOUBLE DEFAULT 0,
    `category` VARCHAR(100) NOT NULL,
    `stock` INT NOT NULL DEFAULT 10,
    `rating` DOUBLE DEFAULT 5.0,
    `review_count` INT DEFAULT 0,
    `image` VARCHAR(500),
    `description` TEXT,
    `promotion` VARCHAR(255) DEFAULT 'Tặng bookmark độc quyền',
    `is_bestseller` BOOLEAN DEFAULT FALSE,
    `publish_year` INT DEFAULT 2024,
    `page_count` INT DEFAULT 250,
    `weight` INT DEFAULT 350,
    `dimensions` VARCHAR(100) DEFAULT '21 x 13.5 x 1.2 cm',
    `cover_format` VARCHAR(50) DEFAULT 'Bìa Mềm',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BẢNG KHO (INVENTORY / KHO - Phục vụ luồng rẽ nhánh 4a)
DROP TABLE IF EXISTS `kho`;
CREATE TABLE `kho` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `book_id` INT NOT NULL,
    `book_code` VARCHAR(50) NOT NULL,
    `quantity` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(50) NOT NULL DEFAULT 'CON_HANG',
    `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`book_id`) REFERENCES `books`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3.5 BẢNG DANH MỤC SÁCH (CATEGORIES / DANH_MUC)
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `description` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `categories` (`id`, `code`, `name`, `description`, `status`) VALUES
(1, 'DM001', 'Văn học', 'Tiểu thuyết, truyện ngắn và các tác phẩm văn học trong nước và quốc tế kinh điển', 'ACTIVE'),
(2, 'DM002', 'Kỹ năng sống', 'Sách rèn luyện bản thân, phát triển tư duy, giao tiếp và kỹ năng làm chủ cuộc sống', 'ACTIVE'),
(3, 'DM003', 'Công nghệ', 'Cẩm nang lập trình, kiến trúc phần mềm, khoa học dữ liệu & công nghệ mới', 'ACTIVE'),
(4, 'DM004', 'Tâm lý học', 'Khám phá hành vi, tư duy và nhận thức con người qua góc nhìn khoa học tâm lý', 'ACTIVE'),
(5, 'DM005', 'Kinh tế', 'Quản trị kinh doanh, tư duy tài chính, làm giàu và khởi nghiệp tinh gọn', 'ACTIVE'),
(6, 'DM006', 'Tâm linh & Đời sống', 'Sách nuôi dưỡng tâm hồn, thức tỉnh tâm thức và nghệ thuật chuyển hóa cảm xúc', 'ACTIVE'),
(7, 'DM007', 'Khoa học', 'Khám phá tri thức vũ trụ, lịch sử loài người và khoa học tự nhiên', 'ACTIVE'),
(8, 'DM008', 'Trinh thám', 'Tiểu thuyết trinh thám, vụ án bí ẩn và hình sự kịch tính', 'ACTIVE'),
(9, 'DM009', 'Sách thiếu nhi', 'Truyện tranh, sách màu và tri thức bổ ích dành cho trẻ em và tuổi mới lớn', 'ACTIVE');

-- 4. BẢNG KHUYẾN MÃI (KHUYEN_MAI, SACH_KHUYEN_MAI - Phục vụ luồng cơ bản 1)
DROP TABLE IF EXISTS `khuyen_mai`;
CREATE TABLE `khuyen_mai` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `title` VARCHAR(255) NOT NULL,
    `discount_rate` DOUBLE DEFAULT 0.0,
    `applicable_category` VARCHAR(100),
    `description` VARCHAR(500),
    `start_date` DATE,
    `end_date` DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm dữ liệu khuyến mãi
INSERT INTO `khuyen_mai` (`code`, `title`, `discount_rate`, `applicable_category`, `description`) VALUES
('KM_VANHOC', 'Ưu Đãi Sách Văn Học Mùa Thu', 20.0, 'Văn học', 'Giảm 20% cho toàn bộ tiểu thuyết và văn học kinh điển'),
('KM_TECH2026', 'Tháng Công Nghệ Tri Thức', 25.0, 'Công nghệ', 'Ưu đãi đặc biệt cẩm nang lập trình & kỹ thuật phần mềm'),
('KM_KINHTE', 'Đột Phá Tư Duy Tài Chính', 15.0, 'Kinh tế', 'Tặng kèm sổ tay tài chính cho các đầu sách làm giàu & quản trị'),
('KM_FREESHIP', 'Miễn Phí Vận Chuyển Toàn Quốc', 0.0, 'Tất cả', 'Freeship cho đơn hàng từ 250.000 đ');

-- Thêm danh mục sách phong phú kèm mã sách (MS001 - MS024), nhà xuất bản và tồn kho (stock)
-- Chú ý: MS008, MS020, MS023 có stock = 0 nhằm kiểm thử luồng rẽ nhánh 4a (Sách hết hàng)
INSERT INTO `books` (`id`, `code`, `title`, `author`, `publisher`, `price`, `original_price`, `category`, `stock`, `rating`, `review_count`, `image`, `description`, `promotion`, `is_bestseller`) VALUES
(1, 'MS001', 'Nhà Giả Kim (The Alchemist)', 'Paulo Coelho', 'NXB Hội Nhà Văn', 79000, 99000, 'Văn học', 28, 4.9, 1420, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500', 'Cuốn sách kinh điển kể về hành trình theo đuổi ước mơ và lắng nghe tiếng gọi của trái tim của chàng chăn cừu Santiago.', 'Giảm 20% - Tặng kèm Bookmark Santiago', 1),
(2, 'MS002', 'Đắc Nhân Tâm (How to Win Friends)', 'Dale Carnegie', 'NXB Tổng Hợp', 88000, 110000, 'Kỹ năng sống', 35, 4.8, 2350, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500', 'Nghệ thuật thu phục lòng người đỉnh cao, cuốn sách gối đầu giường của hàng triệu độc giả trên toàn thế giới.', 'Tặng Ebook kỹ năng giao tiếp', 1),
(3, 'MS003', 'Clean Code: A Handbook of Agile Software Craftsmanship', 'Robert C. Martin (Uncle Bob)', 'NXB Thông Tin & Truyền Thông', 285000, 350000, 'Công nghệ', 15, 4.9, 890, 'https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500', 'Cẩm nang kinh điển của mọi lập trình viên phần mềm để viết mã nguồn sạch sẽ, dễ bảo trì và mở rộng.', 'Giảm ngay 65.000đ khi mua hôm nay', 1),
(4, 'MS004', 'Tư Duy Nhanh Và Chậm (Thinking, Fast and Slow)', 'Daniel Kahneman', 'NXB Thế Giới', 145000, 185000, 'Tâm lý học', 12, 4.7, 640, 'https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=500', 'Giải thích hai hệ thống thúc đẩy cách chúng ta tư duy và đưa ra các quyết định trong cuộc sống.', 'Tặng bookmark độc quyền', 0),
(5, 'MS005', 'Cha Giàu Cha Nghèo (Rich Dad Poor Dad)', 'Robert T. Kiyosaki', 'NXB Trẻ', 95000, 125000, 'Kinh tế', 42, 4.8, 1890, 'https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=500', 'Bài học về tư duy tài chính độc lập và cách để đồng tiền làm việc cho chính bạn thay vì làm việc vì tiền.', 'Ưu đãi combo sách kinh tế', 1),
(6, 'MS006', 'Muôn Kiếp Nhân Sinh (Phần 1 & 2)', 'Nguyên Phong', 'NXB Tổng Hợp', 168000, 210000, 'Tâm linh & Đời sống', 18, 4.9, 1560, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500', 'Tác phẩm ghi lại những trải nghiệm tiền kiếp kỳ lạ và thông điệp sâu sắc về quy luật nhân quả của vũ trụ.', 'Bản in đặc biệt bìa cứng', 1),
(7, 'MS007', 'Cây Cam Ngọt Của Tôi', 'José Mauro de Vasconcelos', 'NXB Hội Nhà Văn', 82000, 108000, 'Văn học', 22, 4.9, 3200, 'https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=500', 'Câu chuyện cảm động rơi nước mắt về tuổi thơ hồn nhiên nhưng đầy vết thương của cậu bé Zezé.', 'Tặng thiệp minh họa màu', 1),
(8, 'MS008', 'Thiết Kế Giải Thuật & Cấu Trúc Dữ Liệu', 'Thomas H. Cormen', 'NXB Khoa Học & Kỹ Thuật', 320000, 390000, 'Công nghệ', 0, 4.8, 410, 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500', 'Giáo trình toàn diện về giải thuật cơ bản và nâng cao dành cho sinh viên và kỹ sư công nghệ thông tin.', 'Tạm hết hàng - Đang tái bản', 0),
(9, 'MS009', 'Thói Quen Nguyên Tử (Atomic Habits)', 'James Clear', 'NXB Thế Giới', 129000, 169000, 'Kỹ năng sống', 30, 4.9, 2900, 'https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500', 'Cách mạng hóa cuộc sống của bạn từ những thay đổi cực kỳ nhỏ bé nhưng mang lại hiệu quả phi thường mỗi ngày.', 'Tặng biểu mẫu Habit Tracker 30 ngày', 1),
(10, 'MS010', 'Hoàng Tử Bé (The Little Prince)', 'Antoine de Saint-Exupéry', 'NXB Kim Đồng', 65000, 85000, 'Văn học', 25, 4.9, 1820, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500', 'Kiệt tác văn học Pháp mang thông điệp triết lý sâu sắc về tình bạn, tình yêu và cái nhìn trong trẻo của trẻ thơ.', 'Trọn bộ sticker nhân vật màu', 1),
(11, 'MS011', 'Tâm Lý Học Về Tiền (The Psychology of Money)', 'Morgan Housel', 'NXB Trẻ', 136000, 170000, 'Kinh tế', 19, 4.9, 2150, 'https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=500', '19 câu chuyện ngắn khám phá những cách kỳ lạ mà mọi người nghĩ về tiền bạc và cách quản lý tài chính thông minh.', 'Giảm 20% cho khách hàng thân thiết', 1),
(12, 'MS012', 'Sapiens: Lược Sử Loài Người', 'Yuval Noah Harari', 'NXB Tri Thức', 195000, 250000, 'Khoa học', 14, 4.9, 3400, 'https://images.unsplash.com/photo-1447069387593-a5de0862481e?w=500', 'Hành trình kỳ vĩ kể về lịch sử tiến hóa của loài người từ thời kỳ đồ đá cho đến kỷ nguyên hiện đại.', 'Freeship đơn hàng trên 200k', 1),
(13, 'MS013', 'Khởi Nghiệp Tinh Gọn (The Lean Startup)', 'Eric Ries', 'NXB Lao Động', 125000, 160000, 'Kinh tế', 11, 4.8, 980, 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500', 'Phương pháp xây dựng doanh nghiệp đổi mới sáng tạo thành công vượt bậc trong thời đại biến động.', 'Tặng tài liệu mẫu Pitch Deck', 0),
(14, 'MS014', 'The Pragmatic Programmer: 20th Anniversary Edition', 'David Thomas, Andrew Hunt', 'NXB Thông Tin & Truyền Thông', 310000, 380000, 'Công nghệ', 8, 4.9, 760, 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500', 'Cẩm nang từ các bậc thầy lập trình giúp bạn nâng tầm kỹ năng từ một thợ code thành kỹ sư phần mềm thực thụ.', 'Tặng kèm cheat sheet phím tắt', 1),
(15, 'MS015', 'Dám Bị Ghét', 'Kishimi Ichiro, Koga Fumitake', 'NXB Nhã Nam', 98000, 125000, 'Tâm lý học', 20, 4.7, 1950, 'https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=500', 'Đối thoại triết học dựa trên tâm lý học Alfred Adler giúp bạn tìm thấy tự do và dũng khí sống thật với chính mình.', 'Tặng kèm sổ tay mini', 1),
(16, 'MS016', 'Bố Già (The Godfather)', 'Mario Puzo', 'NXB Văn Học', 118000, 150000, 'Văn học', 16, 4.9, 4100, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500', 'Tiểu thuyết kinh điển về thế giới mafia Ý-Mỹ, đỉnh cao của nghệ thuật xây dựng nhân vật và quyền lực.', 'Bản dịch mới đầy đủ nhất', 1),
(17, 'MS017', 'Mắt Biếc', 'Nguyễn Nhật Ánh', 'NXB Trẻ', 72000, 90000, 'Văn học', 32, 4.8, 5200, 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500', 'Mối tình si ngốc nghếch, trong sáng nhưng da diết của Ngạn dành cho cô bạn thời thơ ấu có đôi mắt biếc.', 'Tặng postcard nghệ thuật', 1),
(18, 'MS018', 'Từ Tốt Đến Vĩ Đại (Good to Great)', 'Jim Collins', 'NXB Trẻ', 149000, 195000, 'Kinh tế', 9, 4.8, 1200, 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=500', 'Nghiên cứu công phu về lý do tại sao một số công ty có thể tạo ra bước nhảy vọt phi thường còn số khác thì không.', 'Giảm giá 24%', 0),
(19, 'MS019', 'Clean Architecture: A Craftsman\'s Guide', 'Robert C. Martin (Uncle Bob)', 'NXB Thông Tin & Truyền Thông', 295000, 360000, 'Công nghệ', 8, 4.9, 680, 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500', 'Quy tắc vàng về kiến trúc phần mềm giúp hệ thống bền vững, độc lập framework và dễ dàng kiểm thử.', 'Giảm ngay 65.000đ khi đặt trước', 1),
(20, 'MS020', 'Sức Mạnh Của Hiện Tại (The Power of Now)', 'Eckhart Tolle', 'NXB Tổng Hợp', 105000, 135000, 'Tâm linh & Đời sống', 0, 4.8, 1430, 'https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=500', 'Hướng dẫn thức tỉnh tâm thức, giải phóng bản thân khỏi nỗi đau quá khứ và sự lo lắng về tương lai.', 'Tạm hết hàng - Vui lòng chờ đợt mới', 0),
(21, 'MS021', '7 Thói Quen Của Bạn Trẻ Thành Đạt', 'Sean Covey', 'NXB Trẻ', 95000, 120000, 'Kỹ năng sống', 24, 4.8, 2670, 'https://images.unsplash.com/photo-1507842229450-760773d528b8?w=500', 'Chiếc la bàn chỉ đường giúp thanh thiếu niên rèn luyện nhân cách, xác định mục tiêu và gặt hái thành công.', 'Tặng bookmark la bàn', 1),
(22, 'MS022', 'Đọc Vị Bất Kỳ Ai (You Can Read Anyone)', 'David J. Lieberman', 'NXB Thế Giới', 78000, 99000, 'Tâm lý học', 17, 4.7, 1880, 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500', 'Nắm bắt tâm lý, giải mã ngôn ngữ cơ thể và suy nghĩ của đối phương chỉ trong vài phút giao tiếp.', 'Ưu đãi độc giả trẻ', 0),
(23, 'MS023', 'Vũ Trụ Trong Vỏ Hạt Dẻ (The Universe in a Nutshell)', 'Stephen Hawking', 'NXB Trẻ', 155000, 195000, 'Khoa học', 0, 4.8, 1120, 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500', 'Khám phá những biên giới kỳ diệu của vật lý lý thuyết: thuyết tương đối, lỗ đen và lý thuyết siêu dây.', 'Tạm hết hàng trong kho', 0),
(24, 'MS024', 'Rừng Na Uy (Norwegian Wood)', 'Haruki Murakami', 'NXB Hội Nhà Văn', 110000, 140000, 'Văn học', 21, 4.8, 3800, 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=500', 'Tác phẩm lừng danh Nhật Bản khắc họa nỗi cô đơn và sự chới với của tuổi trẻ giữa mất mát và trưởng thành.', 'Tặng kèm bọc sách chuyên dụng', 1),
(25, '8935278601425', 'Tháo Dây Oan Trái - Nghệ Thuật Chuyển Hóa Cảm Xúc', 'Thích Nhật Từ', 'Saigon Books', 89000, 89000, 'Tâm Lý - Kỹ Năng Sống', 25, 5.0, 486, '/images/thao-day-oan-trai.jpg', 'Khi nhắc đến nghiệp, chúng ta sẽ nghĩ đến ba yếu tố tạo thành là tâm, khẩu và thân. Nghiệp từ tâm là thứ quyết định quả báo sau này, vì khi một hành động xấu tạo ra do vô tình chứ không hữu ý thì rất dễ được thông cảm và tha thứ. Tâm nghiệp xuất phát từ cảm xúc của con người. Ví như, khi có cảm xúc tích cực, yêu thương ai đó, chúng ta sẽ dễ dàng tạo ra những hành động tốt đẹp dành cho họ. Ngược lại, khi có cảm xúc tiêu cực, thù ghét, chúng ta lại dễ sinh sân hận mà tạo ra những hành vi gây thương tổn cho họ. Tam đắm, si mê và sân hận là những xúc cảm tiêu cực mà có lẽ bất kỳ ai trong chúng ta cũng đều có – đơn giản vì chúng ta vẫn còn là người phàm. Đây cũng là tiền đề dẫn đến những oan trái, khổ đau trong cuộc đời nếu tự thân chúng ta không biết hóa giải, đoạn trừ. Điều này cũng có nghĩa: Muốn đạt được đến cảm giác thanh thản, bình an, muốn chạm chân đến một bình nguyên tươi mát thì việc chuyển hóa những cảm xúc tiêu cực thành tích cực là yếu tố cần thiết nhất đối với mỗi con người. Tháo dây oan trái tập hợp những bài giảng của Thượng tọa Thích Nhật Từ, được điều chỉnh thành một tập sách chuyên sâu về đề tài chuyển hóa cảm xúc – nhất là chuyển hóa sân hận – như một món quà gửi tặng cho độc giả. Hy vọng cuốn sách này sẽ có giá trị với độc giả trên con đường tu sửa để giữ được hạnh phúc dài lâu cho chính mình và những người xung quanh!', 'Sản phẩm 100% chính hãng - Freeship từ 250k', 1),
(26, '8935278601426', 'Những Cô Gái Mất Tích', 'Megan Miranda', 'NXB Hội Nhà Văn', 158000, 158000, 'Văn học', 0, 4.8, 142, '/images/featured-1.jpg', 'Cuốn tiểu thuyết trinh thám tâm lý đầy kịch tính của Megan Miranda về sự mất tích bí ẩn của những cô gái trẻ trong thị trấn nhỏ.', 'Tạm hết hàng', 1),
(27, '8935278601427', 'Mexico Kỳ Án', 'Robert Kerner', 'NXB Công An Nhân Dân', 119000, 119000, 'Trinh thám', 18, 4.7, 98, '/images/featured-2.jpg', 'Vụ án bí ẩn ly kỳ kéo dài hàng thập kỷ tại đất nước Mexico với những tình tiết cân não và bất ngờ đến phút cuối.', 'Tặng bookmark độc quyền', 1),
(28, '8935278601428', 'Mệt Mỏi Không Phải Do Làm Việc Nhiều Mà Do Phương Pháp', 'Nhiều tác giả', 'Saigon Books', 88200, 98000, 'Tâm Lý - Kỹ Năng Sống', 30, 4.9, 215, '/images/featured-3.jpg', 'Phương pháp quản lý năng lượng và thời gian thông minh để bạn làm việc nhẹ nhàng, hiệu quả và không bị quá tải.', 'Giảm 10% hôm nay', 1),
(29, '8935278601429', 'Để Trở Thành Người Thú Vị', 'Jessica Hagy', 'NXB Trẻ', 103500, 115000, 'Tâm Lý - Kỹ Năng Sống', 22, 4.8, 310, '/images/featured-4.jpg', '10 bước khám phá bản thân, bước ra khỏi vùng an toàn để sống một cuộc đời đầy cảm hứng và màu sắc thú vị.', 'Giảm 10% hôm nay', 1),
(30, '8935278601430', 'Gia Đình - Tranh Đấu Hay Buông Xuôi?', 'Thích Nhật Từ', 'Saigon Books', 89000, 89000, 'Tâm Lý - Kỹ Năng Sống', 20, 4.9, 178, '/images/related-1.jpg', 'Nghệ thuật xây dựng và gìn giữ hạnh phúc gia đình dựa trên sự thấu hiểu, yêu thương và tha thứ của Thượng tọa Thích Nhật Từ.', 'Tặng kèm bookmark Saigon Books', 0),
(31, '8935278601431', 'Hạt Giống Tâm Hồn - Tập 16: Tìm Lại Bình Yên', 'Nhiều tác giả', 'First News - Trí Việt', 76000, 76000, 'Tâm Lý - Kỹ Năng Sống', 45, 4.9, 520, '/images/related-2.jpg', 'Những câu chuyện nuôi dưỡng tâm hồn, giúp người đọc tìm lại sự bình yên trong tâm trí giữa bộn bề lo toan của cuộc sống.', 'Tặng kèm postcard thông điệp', 1),
(32, '8935278601432', 'Hạt Giống Tâm Hồn - Tập 14: Góc Nhìn Diệu Kỳ Của Cuộc Sống', 'Nhiều tác giả', 'First News - Trí Việt', 76000, 76000, 'Tâm Lý - Kỹ Năng Sống', 35, 4.8, 412, '/images/related-3.jpg', 'Tập hợp những câu chuyện giàu tính nhân văn về góc nhìn lạc quan, sự biết ơn và niềm tin vào những điều kỳ diệu quanh ta.', 'Tặng kèm postcard thông điệp', 0),
(33, '8935278601433', 'Hạt Giống Tâm Hồn - Tập 11: Những Trải Nghiệm Cuộc Sống', 'Nhiều tác giả', 'First News - Trí Việt', 76000, 76000, 'Tâm Lý - Kỹ Năng Sống', 28, 4.8, 389, '/images/related-4.jpg', 'Chia sẻ chân thực về những va vấp, trải nghiệm và bài học quý giá trên hành trình trưởng thành của mỗi con người.', 'Tặng kèm postcard thông điệp', 0);

-- Đồng bộ dữ liệu vào bảng KHO (INVENTORY)
INSERT INTO `kho` (`book_id`, `book_code`, `quantity`, `status`)
SELECT `id`, `code`, `stock`, IF(`stock` > 0, 'CON_HANG', 'HET_HANG') FROM `books`;

-- 5. BẢNG ĐỊA CHỈ NHẬN HÀNG (DIA_CHI - Bước 3 trong luồng đặt hàng)
DROP TABLE IF EXISTS `dia_chi`;
CREATE TABLE `dia_chi` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    `recipient_name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `address_detail` VARCHAR(255) NOT NULL,
    `province` VARCHAR(100) NOT NULL,
    `district` VARCHAR(100) NOT NULL,
    `ward` VARCHAR(100) NOT NULL,
    `is_default` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`username`) REFERENCES `users`(`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `dia_chi` (`username`, `recipient_name`, `phone`, `address_detail`, `province`, `district`, `ward`, `is_default`) VALUES
('oanh', 'Hoàng Oanh', '0912345678', 'Tầng 5, Tòa nhà FPT Cầu Giấy, Số 10 Phạm Văn Bạch', 'Hà Nội', 'Quận Cầu Giấy', 'Phường Dịch Vọng Hậu', 1),
('oanh', 'Hoàng Oanh (Nhà riêng)', '0912345678', 'Số 15, Ngõ 1194 Đường Láng', 'Hà Nội', 'Quận Đống Đa', 'Phường Láng Thượng', 0),
('khachhang', 'Khách Hàng Thân Thiết', '0909888999', 'Số 68 Nguyễn Huệ, Phường Bến Nghé', 'TP. Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé', 1);

-- 6. BẢNG MÃ GIẢM GIÁ (MA_GIAM_GIA - Bước 4 trong luồng đặt hàng)
DROP TABLE IF EXISTS `ma_giam_gia`;
CREATE TABLE `ma_giam_gia` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `title` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500),
    `discount_type` VARCHAR(20) NOT NULL DEFAULT 'PERCENT', -- 'PERCENT' hoặc 'FIXED'
    `discount_value` DOUBLE NOT NULL,                       -- 30000 hoặc 20 (%)
    `min_order_amount` DOUBLE DEFAULT 0,                    -- Đơn hàng tối thiểu (VD: 200000)
    `max_discount_amount` DOUBLE DEFAULT 0,                 -- Giảm tối đa nếu là phần trăm
    `start_date` DATE,
    `end_date` DATE,
    `is_active` BOOLEAN DEFAULT TRUE,
    `usage_limit` INT DEFAULT 100,
    `used_count` INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `ma_giam_gia` (`code`, `title`, `description`, `discount_type`, `discount_value`, `min_order_amount`, `max_discount_amount`, `start_date`, `end_date`, `is_active`, `usage_limit`, `used_count`) VALUES
('BOOKORA2026', 'Giảm 30.000đ cho đơn từ 200.000đ', 'Áp dụng cho toàn bộ đầu sách trên hệ thống Bookora', 'FIXED', 30000, 200000, 0, '2026-01-01', '2026-12-31', 1, 500, 12),
('FREESHIP', 'Miễn phí vận chuyển toàn quốc', 'Miễn phí ship (trừ tối đa 30.000đ phí giao hàng) cho đơn từ 250.000đ', 'FIXED', 30000, 250000, 0, '2026-01-01', '2026-12-31', 1, 1000, 45),
('TECH30', 'Giảm 30% Sách Công Nghệ & AI', 'Giảm 30% tối đa 100.000đ cho đơn hàng từ 150.000đ', 'PERCENT', 30, 150000, 100000, '2026-01-01', '2026-12-31', 1, 200, 8),
('TRI_THUC', 'Giảm 15% Sách Kỹ Năng & Tâm Lý', 'Giảm 15% tối đa 50.000đ cho đơn hàng từ 100.000đ', 'PERCENT', 15, 100000, 50000, '2026-01-01', '2026-12-31', 1, 300, 15),
('HETHAN', 'Mã Giảm Giá Đã Hết Hạn (Để kiểm thử luồng 4)', 'Mã mẫu phục vụ kiểm thử trường hợp mã hết hạn sử dụng', 'FIXED', 50000, 100000, 0, '2023-01-01', '2023-12-31', 1, 100, 5),
('CHUADUNG', 'Mã Tạm Khóa (Để kiểm thử luồng 4)', 'Mã mẫu phục vụ kiểm thử trường hợp mã chưa mở / bị tạm khóa', 'FIXED', 20000, 50000, 0, '2026-01-01', '2026-12-31', 0, 100, 0);

-- 7. BẢNG PHƯƠNG THỨC THANH TOÁN (PHUONG_THUC_TT)
DROP TABLE IF EXISTS `phuong_thuc_tt`;
CREATE TABLE `phuong_thuc_tt` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255),
    `is_active` BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `phuong_thuc_tt` (`code`, `name`, `description`, `is_active`) VALUES
('COD', 'Thanh toán tiền mặt khi nhận hàng (COD)', 'Nhận sách, kiểm tra hàng rồi thanh toán cho shipper', 1),
('ONLINE', 'Thanh toán trực tuyến (Chuyển khoản VietQR / MoMo / Thẻ ATM)', 'Quét mã VietQR chuyển khoản nhanh 24/7 không mất phí', 1);

-- 8. BẢNG GIỎ HÀNG (GIO_HANG)
DROP TABLE IF EXISTS `gio_hang`;
CREATE TABLE `gio_hang` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`username`) REFERENCES `users`(`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. BẢNG CHI TIẾT GIỎ HÀNG (CT_GIO_HANG)
DROP TABLE IF EXISTS `ct_gio_hang`;
CREATE TABLE `ct_gio_hang` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `gio_hang_id` INT NOT NULL,
    `book_id` INT NOT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`gio_hang_id`) REFERENCES `gio_hang`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`book_id`) REFERENCES `books`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. BẢNG ĐƠN HÀNG (DON_HANG - Bước 6 trong luồng đặt hàng)
DROP TABLE IF EXISTS `don_hang`;
CREATE TABLE `don_hang` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `order_code` VARCHAR(50) NOT NULL UNIQUE,
    `username` VARCHAR(50) NOT NULL,
    `address_id` INT,
    `recipient_name` VARCHAR(100) NOT NULL,
    `recipient_phone` VARCHAR(20) NOT NULL,
    `delivery_address` VARCHAR(500) NOT NULL,
    `voucher_code` VARCHAR(50),
    `subtotal` DOUBLE NOT NULL,
    `discount_amount` DOUBLE DEFAULT 0,
    `shipping_fee` DOUBLE DEFAULT 0,
    `total_amount` DOUBLE NOT NULL,
    `payment_method_code` VARCHAR(50) NOT NULL DEFAULT 'COD',
    `payment_method_name` VARCHAR(100) DEFAULT 'Thanh toán khi nhận hàng (COD)',
    `payment_status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- 'PENDING', 'PAID', 'FAILED'
    `order_status` VARCHAR(50) NOT NULL DEFAULT 'CHO_XAC_NHAN', -- 'CHO_XAC_NHAN', 'DANG_XU_LY', 'DANG_GIAO', 'HOAN_THANH', 'DA_HUY'
    `note` TEXT,
    `transaction_id` VARCHAR(100),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`username`) REFERENCES `users`(`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. BẢNG CHI TIẾT ĐƠN HÀNG (CT_DON_HANG)
DROP TABLE IF EXISTS `ct_don_hang`;
CREATE TABLE `ct_don_hang` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `order_id` INT NOT NULL,
    `book_id` INT NOT NULL,
    `book_code` VARCHAR(50) NOT NULL,
    `book_title` VARCHAR(255) NOT NULL,
    `book_image` VARCHAR(500),
    `price` DOUBLE NOT NULL,
    `quantity` INT NOT NULL,
    `subtotal` DOUBLE NOT NULL,
    FOREIGN KEY (`order_id`) REFERENCES `don_hang`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`book_id`) REFERENCES `books`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm đơn hàng mẫu cho tài khoản oanh & khachhang
INSERT INTO `don_hang` (`id`, `order_code`, `username`, `address_id`, `recipient_name`, `recipient_phone`, `delivery_address`, `voucher_code`, `subtotal`, `discount_amount`, `shipping_fee`, `total_amount`, `payment_method_code`, `payment_method_name`, `payment_status`, `order_status`, `note`, `transaction_id`, `created_at`) VALUES
(101, 'ORD-20260925-1001', 'oanh', 1, 'Hoàng Oanh', '0912345678', 'Tầng 5, Tòa nhà FPT Cầu Giấy, Số 10 Phạm Văn Bạch, Hà Nội', 'BOOKORA2026', 243000, 30000, 0, 213000, 'COD', 'Thanh toán khi nhận hàng (COD)', 'PAID', 'HOAN_THANH', 'Giao trong giờ hành chính', NULL, '2026-09-25 14:30:00'),
(102, 'ORD-20260927-1002', 'oanh', 1, 'Hoàng Oanh', '0912345678', 'Tầng 5, Tòa nhà FPT Cầu Giấy, Số 10 Phạm Văn Bạch, Hà Nội', 'FREESHIP', 285000, 30000, 0, 255000, 'ONLINE', 'Chuyển khoản VietQR', 'PAID', 'DANG_GIAO', 'Gọi trước khi giao hàng', 'VNQR179051003', '2026-09-27 09:15:00'),
(103, 'ORD-20260928-1003', 'oanh', 1, 'Hoàng Oanh', '0912345678', 'Tầng 5, Tòa nhà FPT Cầu Giấy, Số 10 Phạm Văn Bạch, Hà Nội', NULL, 89000, 0, 30000, 119000, 'COD', 'Thanh toán khi nhận hàng (COD)', 'PENDING', 'CHO_XAC_NHAN', 'Đóng gói cẩn thận giúp tôi', NULL, '2026-09-28 10:00:00'),
(104, 'ORD-20260928-1004', 'khachhang', 3, 'Khách Hàng Thân Thiết', '0909888999', 'Số 68 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh', NULL, 88000, 0, 30000, 118000, 'COD', 'Thanh toán khi nhận hàng (COD)', 'PAID', 'HOAN_THANH', NULL, NULL, '2026-09-28 16:45:00');

INSERT INTO `ct_don_hang` (`order_id`, `book_id`, `book_code`, `book_title`, `book_image`, `price`, `quantity`, `subtotal`) VALUES
(101, 1, 'MS001', 'Nhà Giả Kim (The Alchemist)', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500', 79000, 1, 79000),
(101, 7, 'MS007', 'Cây Cam Ngọt Của Tôi', 'https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=500', 82000, 2, 164000),
(102, 3, 'MS003', 'Clean Code: A Handbook of Agile Software Craftsmanship', 'https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500', 285000, 1, 285000),
(103, 25, '8935278601425', 'Tháo Dây Oan Trái - Nghệ Thuật Chuyển Hóa Cảm Xúc', '/images/thao-day-oan-trai.jpg', 89000, 1, 89000),
(104, 2, 'MS002', 'Đắc Nhân Tâm (How to Win Friends)', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500', 88000, 1, 88000);

-- 12. BẢNG NỘI DUNG CHATBOT AI (CHATBOT_NOI_DUNG - Phục vụ UC13)
DROP TABLE IF EXISTS `chatbot_noi_dung`;
CREATE TABLE `chatbot_noi_dung` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `category_type` VARCHAR(50) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `keywords` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `chatbot_noi_dung` (`id`, `category_type`, `title`, `content`, `keywords`, `status`) VALUES
(1, 'CHINH_SACH_MUA_HANG', 'Quy trình và điều kiện đặt hàng sách', 'Quý khách có thể chọn sách, thêm vào giỏ hàng, chọn địa chỉ nhận hàng và phương thức thanh toán để hoàn tất đơn hàng. Bookora tiếp nhận đơn hàng 24/7 trên phạm vi toàn quốc.', 'mua hàng, quy trình đặt hàng, làm sao đặt sách, hướng dẫn mua sách', 'ACTIVE'),
(2, 'CHINH_SACH_THANH_TOAN', 'Phương thức thanh toán hỗ trợ', 'Bookora hỗ trợ 2 phương thức thanh toán: 1) Thanh toán tiền mặt khi nhận hàng (COD). 2) Thanh toán trực tuyến chuyển khoản ngân hàng nhanh VietQR/MoMo 24/7 hoàn toàn không mất phí.', 'thanh toán, cod, chuyển khoản, vietqr, momo, tiền mặt', 'ACTIVE'),
(3, 'CHINH_SACH_GIAO_HANG', 'Phạm vi, thời gian và phí vận chuyển', 'Phạm vi giao hàng: Toàn quốc. Phí giao hàng tiêu chuẩn là 30.000đ. Đơn hàng từ 250.000đ được MIỄN PHÍ VẬN CHUYỂN toàn quốc. Thời gian giao hàng từ 1 - 3 ngày làm việc.', 'giao hàng, phí ship, bao lâu nhận được, miễn phí vận chuyển, freeship', 'ACTIVE'),
(4, 'CAU_HOI_THUONG_GAP', 'Chính sách đổi trả và hoàn tiền', 'Khách hàng được quyền yêu cầu đổi trả sách trong vòng 7 ngày kể từ khi nhận hàng nếu sách có lỗi kỹ thuật (rách bìa, thiếu trang, rách hỏng do vận chuyển). Liên hệ hotline 0988123456 để được hỗ trợ.', 'đổi trả, hoàn tiền, rách sách, sách lỗi, chính sách trả hàng', 'ACTIVE'),
(5, 'THONG_TIN_SACH', 'Giới thiệu danh mục sách bán chạy & khuyến mãi', 'Bookora sở hữu bộ sưu tập sách phong phú gồm các thể loại: Văn học, Kỹ năng sống, Công nghệ & AI, Kinh tế, Tâm lý học. Nhiều chương trình ưu đãi giảm đến 25% kèm quà tặng bookmark độc quyền.', 'sách bán chạy, danh mục sách, ưu đãi sách, giảm giá sách, thể loại sách', 'ACTIVE');

SET FOREIGN_KEY_CHECKS = 1;




