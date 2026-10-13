USE `web_bookora`;

-- 1. BẢNG TRI THỨC NỘI DUNG CHATBOT AI (CHATBOT_NOI_DUNG)
CREATE TABLE IF NOT EXISTS `chatbot_noi_dung` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `category_type` VARCHAR(50) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `keywords` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm dữ liệu mẫu vào chatbot_noi_dung nếu chưa có
INSERT IGNORE INTO `chatbot_noi_dung` (`id`, `category_type`, `title`, `content`, `keywords`, `status`) VALUES
(1, 'CHINH_SACH_MUA_HANG', 'Quy trình và điều kiện đặt hàng sách', 'Quý khách có thể chọn sách, thêm vào giỏ hàng, chọn địa chỉ nhận hàng và phương thức thanh toán để hoàn tất đơn hàng. Bookora tiếp nhận đơn hàng 24/7 trên phạm vi toàn quốc.', 'mua hàng, quy trình đặt hàng, làm sao đặt sách, hướng dẫn mua sách', 'ACTIVE'),
(2, 'CHINH_SACH_THANH_TOAN', 'Phương thức thanh toán hỗ trợ', 'Bookora hỗ trợ 2 phương thức thanh toán: 1) Thanh toán tiền mặt khi nhận hàng (COD). 2) Thanh toán trực tuyến chuyển khoản ngân hàng nhanh VietQR/MoMo 24/7 hoàn toàn không mất phí.', 'thanh toán, cod, chuyển khoản, vietqr, momo, tiền mặt', 'ACTIVE'),
(3, 'CHINH_SACH_GIAO_HANG', 'Phạm vi, thời gian và phí vận chuyển', 'Phạm vi giao hàng: Toàn quốc. Phí giao hàng tiêu chuẩn là 30.000đ. Đơn hàng từ 250.000đ được MIỄN PHÍ VẬN CHUYỂN toàn quốc. Thời gian giao hàng từ 1 - 3 ngày làm việc.', 'giao hàng, phí ship, bao lâu nhận được, miễn phí vận chuyển, freeship', 'ACTIVE'),
(4, 'CAU_HOI_THUONG_GAP', 'Chính sách đổi trả và hoàn tiền', 'Khách hàng được quyền yêu cầu đổi trả sách trong vòng 7 ngày kể từ khi nhận hàng nếu sách có lỗi kỹ thuật (rách bìa, thiếu trang, rách hỏng do vận chuyển). Liên hệ hotline 0988123456 để được hỗ trợ.', 'đổi trả, hoàn tiền, rách sách, sách lỗi, chính sách trả hàng', 'ACTIVE'),
(5, 'THONG_TIN_SACH', 'Giới thiệu danh mục sách bán chạy & khuyến mãi', 'Bookora sở hữu bộ sưu tập sách phong phú gồm các thể loại: Văn học, Kỹ năng sống, Công nghệ & AI, Kinh tế, Tâm lý học. Nhiều chương trình ưu đãi giảm đến 25% kèm quà tặng bookmark độc quyền.', 'sách bán chạy, danh mục sách, ưu đãi sách, giảm giá sách, thể loại sách', 'ACTIVE');

-- 2. BẢNG HỘI THOẠI CHATBOT (CHATBOT_HOI_THOAI)
CREATE TABLE IF NOT EXISTS `chatbot_hoi_thoai` (
    `HoiThoaiID` INT AUTO_INCREMENT PRIMARY KEY,
    `KhachHangID` INT NOT NULL,
    `ThoiGianBatDau` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `TrangThai` VARCHAR(20) DEFAULT 'DANG_HOAT_DONG',
    FOREIGN KEY (`KhachHangID`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BẢNG TIN NHẮN CHATBOT (CHATBOT_TIN_NHAN)
CREATE TABLE IF NOT EXISTS `chatbot_tin_nhan` (
    `TinNhanID` INT AUTO_INCREMENT PRIMARY KEY,
    `NoiDung` TEXT NOT NULL,
    `NguoiGui` VARCHAR(20) NOT NULL,
    `ThoiGian` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `HoiThoaiID` INT NOT NULL,
    `KhachHangID` INT NOT NULL,
    FOREIGN KEY (`HoiThoaiID`) REFERENCES `chatbot_hoi_thoai`(`HoiThoaiID`) ON DELETE CASCADE,
    FOREIGN KEY (`KhachHangID`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm dữ liệu mẫu kiểm thử
INSERT IGNORE INTO `chatbot_hoi_thoai` (`HoiThoaiID`, `KhachHangID`, `ThoiGianBatDau`, `TrangThai`) VALUES
(1, 2, '2026-10-10 09:15:00', 'HOAN_THANH'),
(2, 3, '2026-10-11 14:30:00', 'DANG_HOAT_DONG');

INSERT IGNORE INTO `chatbot_tin_nhan` (`TinNhanID`, `NoiDung`, `NguoiGui`, `ThoiGian`, `HoiThoaiID`, `KhachHangID`) VALUES
(1, 'Xin chào, shop có sách Nhà Giả Kim không?', 'KHACH_HANG', '2026-10-10 09:15:02', 1, 2),
(2, 'Chào bạn! Bookora hiện có sẵn sách Nhà Giả Kim với giá 79.000đ và đang được ưu đãi giảm 20% tặng kèm bookmark bạn nhé!', 'CHATBOT', '2026-10-10 09:15:05', 1, 2),
(3, 'Shop giao hàng bao lâu thì tới Hà Nội ạ?', 'KHACH_HANG', '2026-10-10 09:15:40', 1, 2),
(4, 'Đơn hàng giao tới Hà Nội thường mất từ 1 - 3 ngày làm việc. Đơn từ 250.000đ được miễn phí vận chuyển toàn quốc bạn nhé!', 'CHATBOT', '2026-10-10 09:15:43', 1, 2),
(5, 'Tư vấn giúp tôi sách học lập trình cho người mới bắt đầu', 'KHACH_HANG', '2026-10-11 14:30:10', 2, 3),
(6, 'Dạ chào bạn! Để bắt đầu học lập trình, bạn có thể tham khảo cuốn "Clean Code" hoặc "The Pragmatic Programmer" trên kệ sách Công nghệ của Bookora nhé!', 'CHATBOT', '2026-10-11 14:30:15', 2, 3);
