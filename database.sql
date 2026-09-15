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

-- Thêm danh mục sách phong phú
INSERT INTO `books` (`id`, `title`, `author`, `price`, `original_price`, `category`, `rating`, `review_count`, `image`, `description`, `is_bestseller`) VALUES
(1, 'Nhà Giả Kim (The Alchemist)', 'Paulo Coelho', 79000, 99000, 'Văn học', 4.9, 1420, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500', 'Cuốn sách kinh điển kể về hành trình theo đuổi ước mơ và lắng nghe tiếng gọi của trái tim của chàng chăn cừu Santiago.', 1),
(2, 'Đắc Nhân Tâm (How to Win Friends)', 'Dale Carnegie', 88000, 110000, 'Kỹ năng sống', 4.8, 2350, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500', 'Nghệ thuật thu phục lòng người đỉnh cao, cuốn sách gối đầu giường của hàng triệu độc giả trên toàn thế giới.', 1),
(3, 'Clean Code: A Handbook of Agile Software Craftsmanship', 'Robert C. Martin (Uncle Bob)', 285000, 350000, 'Công nghệ', 4.9, 890, 'https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500', 'Cẩm nang kinh điển của mọi lập trình viên phần mềm để viết mã nguồn sạch sẽ, dễ bảo trì và mở rộng.', 1),
(4, 'Tư Duy Nhanh Và Chậm (Thinking, Fast and Slow)', 'Daniel Kahneman', 145000, 185000, 'Tâm lý học', 4.7, 640, 'https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=500', 'Giải thích hai hệ thống thúc đẩy cách chúng ta tư duy và đưa ra các quyết định trong cuộc sống.', 0),
(5, 'Cha Giàu Cha Nghèo (Rich Dad Poor Dad)', 'Robert T. Kiyosaki', 95000, 125000, 'Kinh tế', 4.8, 1890, 'https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=500', 'Bài học về tư duy tài chính độc lập và cách để đồng tiền làm việc cho chính bạn thay vì làm việc vì tiền.', 1),
(6, 'Muôn Kiếp Nhân Sinh (Phần 1 & 2)', 'Nguyên Phong', 168000, 210000, 'Tâm linh & Đời sống', 4.9, 1560, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500', 'Tác phẩm ghi lại những trải nghiệm tiền kiếp kỳ lạ và thông điệp sâu sắc về quy luật nhân quả của vũ trụ.', 1),
(7, 'Cây Cam Ngọt Của Tôi', 'José Mauro de Vasconcelos', 82000, 108000, 'Văn học', 4.9, 3200, 'https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=500', 'Câu chuyện cảm động rơi nước mắt về tuổi thơ hồn nhiên nhưng đầy vết thương của cậu bé Zezé.', 1),
(8, 'Thiết Kế Giải Thuật & Cấu Trúc Dữ Liệu', 'Thomas H. Cormen', 320000, 390000, 'Công nghệ', 4.8, 410, 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500', 'Giáo trình toàn diện về giải thuật cơ bản và nâng cao dành cho sinh viên và kỹ sư công nghệ thông tin.', 0),
(9, 'Thói Quen Nguyên Tử (Atomic Habits)', 'James Clear', 129000, 169000, 'Kỹ năng sống', 4.9, 2900, 'https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500', 'Cách mạng hóa cuộc sống của bạn từ những thay đổi cực kỳ nhỏ bé nhưng mang lại hiệu quả phi thường mỗi ngày.', 1);
