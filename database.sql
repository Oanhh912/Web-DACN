-- =========================================================
-- HỆ THỐNG CƠ SỞ DỮ LIỆU CHO WEBSITE BÁN SÁCH BOOKORA
-- Tương thích: MySQL 8.0+ / MariaDB 10.4+ (XAMPP)
-- Database Name: web_bookora
-- =========================================================

CREATE DATABASE IF NOT EXISTS `web_bookora` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `web_bookora`;

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
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm dữ liệu tài khoản mẫu
INSERT INTO `users` (`username`, `password`, `full_name`, `email`, `phone`, `role`, `avatar`) VALUES
('admin', '123456', 'Quản Trị Viên - Hoàng Oanh', 'admin@bookora.vn', '0988123456', 'ADMIN', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'),
('oanh', '123456', 'Hoàng Oanh', 'oanh.nguyen@gmail.com', '0912345678', 'CUSTOMER', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150'),
('khachhang', '123456', 'Khách Hàng Thân Thiết', 'khachhang@gmail.com', '0909888999', 'CUSTOMER', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150');

-- 2. BẢNG SÁCH (BOOKS)
DROP TABLE IF EXISTS `books`;
CREATE TABLE `books` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `title` VARCHAR(255) NOT NULL,
    `author` VARCHAR(150) NOT NULL,
    `price` DOUBLE NOT NULL,
    `original_price` DOUBLE DEFAULT 0,
    `category` VARCHAR(100) NOT NULL,
    `rating` DOUBLE DEFAULT 5.0,
    `review_count` INT DEFAULT 0,
    `image` VARCHAR(500),
    `description` TEXT,
    `is_bestseller` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm danh mục sách phong phú kèm mã sách (MS001 - MS024)
INSERT INTO `books` (`id`, `code`, `title`, `author`, `price`, `original_price`, `category`, `rating`, `review_count`, `image`, `description`, `is_bestseller`) VALUES
(1, 'MS001', 'Nhà Giả Kim (The Alchemist)', 'Paulo Coelho', 79000, 99000, 'Văn học', 4.9, 1420, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500', 'Cuốn sách kinh điển kể về hành trình theo đuổi ước mơ và lắng nghe tiếng gọi của trái tim của chàng chăn cừu Santiago.', 1),
(2, 'MS002', 'Đắc Nhân Tâm (How to Win Friends)', 'Dale Carnegie', 88000, 110000, 'Kỹ năng sống', 4.8, 2350, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500', 'Nghệ thuật thu phục lòng người đỉnh cao, cuốn sách gối đầu giường của hàng triệu độc giả trên toàn thế giới.', 1),
(3, 'MS003', 'Clean Code: A Handbook of Agile Software Craftsmanship', 'Robert C. Martin (Uncle Bob)', 285000, 350000, 'Công nghệ', 4.9, 890, 'https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500', 'Cẩm nang kinh điển của mọi lập trình viên phần mềm để viết mã nguồn sạch sẽ, dễ bảo trì và mở rộng.', 1),
(4, 'MS004', 'Tư Duy Nhanh Và Chậm (Thinking, Fast and Slow)', 'Daniel Kahneman', 145000, 185000, 'Tâm lý học', 4.7, 640, 'https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=500', 'Giải thích hai hệ thống thúc đẩy cách chúng ta tư duy và đưa ra các quyết định trong cuộc sống.', 0),
(5, 'MS005', 'Cha Giàu Cha Nghèo (Rich Dad Poor Dad)', 'Robert T. Kiyosaki', 95000, 125000, 'Kinh tế', 4.8, 1890, 'https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=500', 'Bài học về tư duy tài chính độc lập và cách để đồng tiền làm việc cho chính bạn thay vì làm việc vì tiền.', 1),
(6, 'MS006', 'Muôn Kiếp Nhân Sinh (Phần 1 & 2)', 'Nguyên Phong', 168000, 210000, 'Tâm linh & Đời sống', 4.9, 1560, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500', 'Tác phẩm ghi lại những trải nghiệm tiền kiếp kỳ lạ và thông điệp sâu sắc về quy luật nhân quả của vũ trụ.', 1),
(7, 'MS007', 'Cây Cam Ngọt Của Tôi', 'José Mauro de Vasconcelos', 82000, 108000, 'Văn học', 4.9, 3200, 'https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=500', 'Câu chuyện cảm động rơi nước mắt về tuổi thơ hồn nhiên nhưng đầy vết thương của cậu bé Zezé.', 1),
(8, 'MS008', 'Thiết Kế Giải Thuật & Cấu Trúc Dữ Liệu', 'Thomas H. Cormen', 320000, 390000, 'Công nghệ', 4.8, 410, 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500', 'Giáo trình toàn diện về giải thuật cơ bản và nâng cao dành cho sinh viên và kỹ sư công nghệ thông tin.', 0),
(9, 'MS009', 'Thói Quen Nguyên Tử (Atomic Habits)', 'James Clear', 129000, 169000, 'Kỹ năng sống', 4.9, 2900, 'https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500', 'Cách mạng hóa cuộc sống của bạn từ những thay đổi cực kỳ nhỏ bé nhưng mang lại hiệu quả phi thường mỗi ngày.', 1),
(10, 'MS010', 'Hoàng Tử Bé (The Little Prince)', 'Antoine de Saint-Exupéry', 65000, 85000, 'Văn học', 4.9, 1820, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500', 'Kiệt tác văn học Pháp mang thông điệp triết lý sâu sắc về tình bạn, tình yêu và cái nhìn trong trẻo của trẻ thơ.', 1),
(11, 'MS011', 'Tâm Lý Học Về Tiền (The Psychology of Money)', 'Morgan Housel', 136000, 170000, 'Kinh tế', 4.9, 2150, 'https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=500', '19 câu chuyện ngắn khám phá những cách kỳ lạ mà mọi người nghĩ về tiền bạc và cách quản lý tài chính thông minh.', 1),
(12, 'MS012', 'Sapiens: Lược Sử Loài Người', 'Yuval Noah Harari', 195000, 250000, 'Khoa học', 4.9, 3400, 'https://images.unsplash.com/photo-1447069387593-a5de0862481e?w=500', 'Hành trình kỳ vĩ kể về lịch sử tiến hóa của loài người từ thời kỳ đồ đá cho đến kỷ nguyên hiện đại.', 1),
(13, 'MS013', 'Khởi Nghiệp Tinh Gọn (The Lean Startup)', 'Eric Ries', 125000, 160000, 'Kinh tế', 4.8, 980, 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500', 'Phương pháp xây dựng doanh nghiệp đổi mới sáng tạo thành công vượt bậc trong thời đại biến động.', 0),
(14, 'MS014', 'The Pragmatic Programmer: 20th Anniversary Edition', 'David Thomas, Andrew Hunt', 310000, 380000, 'Công nghệ', 4.9, 760, 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500', 'Cẩm nang từ các bậc thầy lập trình giúp bạn nâng tầm kỹ năng từ một thợ code thành kỹ sư phần mềm thực thụ.', 1),
(15, 'MS015', 'Dám Bị Ghét', 'Kishimi Ichiro, Koga Fumitake', 98000, 125000, 'Tâm lý học', 4.7, 1950, 'https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=500', 'Đối thoại triết học dựa trên tâm lý học Alfred Adler giúp bạn tìm thấy tự do và dũng khí sống thật với chính mình.', 1),
(16, 'MS016', 'Bố Già (The Godfather)', 'Mario Puzo', 118000, 150000, 'Văn học', 4.9, 4100, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500', 'Tiểu thuyết kinh điển về thế giới mafia Ý-Mỹ, đỉnh cao của nghệ thuật xây dựng nhân vật và quyền lực.', 1),
(17, 'MS017', 'Mắt Biếc', 'Nguyễn Nhật Ánh', 72000, 90000, 'Văn học', 4.8, 5200, 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500', 'Mối tình si ngốc nghếch, trong sáng nhưng da diết của Ngạn dành cho cô bạn thời thơ ấu có đôi mắt biếc.', 1),
(18, 'MS018', 'Từ Tốt Đến Vĩ Đại (Good to Great)', 'Jim Collins', 149000, 195000, 'Kinh tế', 4.8, 1200, 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=500', 'Nghiên cứu công phu về lý do tại sao một số công ty có thể tạo ra bước nhảy vọt phi thường còn số khác thì không.', 0),
(19, 'MS019', 'Clean Architecture: A Craftsman\'s Guide', 'Robert C. Martin (Uncle Bob)', 295000, 360000, 'Công nghệ', 4.9, 680, 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500', 'Quy tắc vàng về kiến trúc phần mềm giúp hệ thống bền vững, độc lập framework và dễ dàng kiểm thử.', 1),
(20, 'MS020', 'Sức Mạnh Của Hiện Tại (The Power of Now)', 'Eckhart Tolle', 105000, 135000, 'Tâm linh & Đời sống', 4.8, 1430, 'https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=500', 'Hướng dẫn thức tỉnh tâm thức, giải phóng bản thân khỏi nỗi đau quá khứ và sự lo lắng về tương lai.', 0),
(21, 'MS021', '7 Thói Quen Của Bạn Trẻ Thành Đạt', 'Sean Covey', 95000, 120000, 'Kỹ năng sống', 4.8, 2670, 'https://images.unsplash.com/photo-1507842229450-760773d528b8?w=500', 'Chiếc la bàn chỉ đường giúp thanh thiếu niên rèn luyện nhân cách, xác định mục tiêu và gặt hái thành công.', 1),
(22, 'MS022', 'Đọc Vị Bất Kỳ Ai (You Can Read Anyone)', 'David J. Lieberman', 78000, 99000, 'Tâm lý học', 4.7, 1880, 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500', 'Nắm bắt tâm lý, giải mã ngôn ngữ cơ thể và suy nghĩ của đối phương chỉ trong vài phút giao tiếp.', 0),
(23, 'MS023', 'Vũ Trụ Trong Vỏ Hạt Dẻ (The Universe in a Nutshell)', 'Stephen Hawking', 155000, 195000, 'Khoa học', 4.8, 1120, 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500', 'Khám phá những biên giới kỳ diệu của vật lý lý thuyết: thuyết tương đối, lỗ đen và lý thuyết siêu dây.', 0),
(24, 'MS024', 'Rừng Na Uy (Norwegian Wood)', 'Haruki Murakami', 110000, 140000, 'Văn học', 4.8, 3800, 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=500', 'Tác phẩm lừng danh Nhật Bản khắc họa nỗi cô đơn và sự chới với của tuổi trẻ giữa mất mát và trưởng thành.', 1);
