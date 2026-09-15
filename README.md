# 📚 DỰ ÁN WEB BÁN SÁCH JAVA (BOOKORA)
> **Đồ án chuyên ngành / Bài tập lớn Lập trình Web Java**  
> Tương thích: **Eclipse IDE**, **Apache Tomcat 10.1 (Jakarta EE 10)** & **Java Standalone Server**

---

## 🌟 1. Giới thiệu tổng quan

Dự án mô phỏng website bán sách trực tuyến hoàn chỉnh với 2 chức năng trọng tâm:
1. **Khung Đăng Nhập Tài Khoản (Login Form)**:
   - Giao diện hiện đại phong cách nhà sách (Deep Indigo, Warm Gold), responsive trên mọi thiết bị.
   - Kiểm tra hợp lệ (validation), hiển thị/ẩn mật khẩu, ghi nhớ đăng nhập (Remember me).
   - Nút chọn nhanh tài khoản thử nghiệm (Admin, Khách hàng) bằng 1 click.
   - Xử lý xác thực đăng nhập qua Java Backend, quản lý phiên đăng nhập an toàn bằng `HttpSession` / Token.
   - Bộ lọc `AuthFilter` ngăn chặn truy cập trái phép khi chưa đăng nhập.

2. **Trang Chủ Bán Sách Sau Khi Đăng Nhập (Home Page)**:
   - Thanh điều hướng Header cá nhân hóa: hiển thị Avatar, lời chào theo tên người dùng (`Xin chào, Hoàng Oanh`), badge vai trò (`ADMIN` / `CUSTOMER`).
   - Tìm kiếm sách trực tiếp (Live search theo tiêu đề hoặc tác giả).
   - Phân loại sách theo danh mục (Văn học, Kinh tế, Công nghệ, Kỹ năng sống, Tâm lý học...).
   - Lưới danh mục sách kèm giá tiền VNĐ, giá gốc, huy hiệu "Bán chạy", huy hiệu giảm giá, đánh giá sao.
   - **Xem nhanh chi tiết sách (Quick View Modal)** xem tóm tắt nội dung và tăng giảm số lượng.
   - **Giỏ hàng mini (Cart Drawer)** trượt từ cạnh phải, thêm bớt sách, tính tổng tiền tự động.
   - Chức năng **Đăng xuất (Logout)** hủy session an toàn và điều hướng về màn hình đăng nhập.

---

## 🚀 2. Hướng dẫn chạy nhanh (Không cần cài đặt thêm)

### Cách 1: Chạy trực tiếp bằng 1 cú click (Khuyên dùng khi cần xem ngay)
- Click đúp chuột vào tệp **`run.bat`** tại thư mục gốc của dự án.
- Máy chủ Java độc lập sẽ tự động biên dịch và mở trình duyệt tại:
  👉 **`http://localhost:8080/login`**

### Cách 2: Chạy từ cửa sổ dòng lệnh PowerShell / Terminal
```powershell
# 1. Biên dịch
.\compile.bat

# 2. Khởi chạy
java -cp "build\classes;lib\servlet-api.jar" com.bookstore.server.BookstoreApp
```

---

## 💻 3. Hướng dẫn chạy trên Eclipse & Apache Tomcat 10.1

Dự án đã được cấu hình sẵn theo chuẩn **Dynamic Web Project**:

1. Mở **Eclipse IDE**.
2. Chọn menu **File** -> **Open Projects from File System...** (hoặc **Import...** -> **Existing Projects into Workspace**).
3. Tại mục **Import source**, bấm **Directory...** và trỏ đến thư mục `e:\Web DACN`.
4. Bấm **Finish** để import dự án.
5. Click chuột phải vào dự án `Web_DACN` -> **Run As** -> **Run on Server**.
6. Chọn **Apache Tomcat v10.1** (đã có sẵn trên máy) và bấm **Finish**.
7. Trình duyệt Eclipse sẽ tự động mở trang web tại `http://localhost:8080/Web_DACN/login`.

---

## 🔑 4. Danh sách tài khoản thử nghiệm có sẵn

| Tên đăng nhập | Mật khẩu | Họ và tên | Quyền hạn (Role) | Email |
|:---|:---:|:---|:---:|:---|
| **`admin`** | `123456` | Quản Trị Viên - Hoàng Oanh | `ADMIN` | `admin@bookstore.vn` |
| **`oanh`** | `123456` | Hoàng Oanh | `CUSTOMER` | `oanh.nguyen@gmail.com` |
| **`khachhang`** | `123456` | Khách Hàng Thân Thiết | `CUSTOMER` | `khachhang@gmail.com` |

*(Tại màn hình đăng nhập, bạn cũng có thể bấm vào các nút tròn bên dưới để hệ thống tự điền thông tin tài khoản và mật khẩu!)*

---

## 📁 5. Cấu trúc thư mục dự án

```text
Web DACN/
├── .settings/                         # Cấu hình Facet và Component của Eclipse
├── build/classes/                     # Thư mục chứa mã byte-code sau khi biên dịch
├── lib/
│   └── servlet-api.jar                # Thư viện Jakarta Servlet 6.0
├── src/main/java/com/bookstore/
│   ├── model/
│   │   ├── User.java                  # Model người dùng (Username, FullName, Role,...)
│   │   └── Book.java                  # Model cuốn sách (Title, Price, Category, Rating,...)
│   ├── data/
│   │   └── DataStore.java             # Kho dữ liệu mô phỏng bộ nhớ, danh mục & sách mẫu
│   ├── servlet/
│   │   ├── LoginServlet.java          # Xử lý xác thực đăng nhập & tạo Session
│   │   ├── HomeServlet.java           # Xử lý nạp dữ liệu và điều hướng trang chủ
│   │   ├── LogoutServlet.java         # Xử lý hủy phiên đăng nhập
│   │   └── AuthFilter.java            # Bộ lọc bảo vệ ngăn chặn truy cập khi chưa đăng nhập
│   └── server/
│       └── BookstoreApp.java          # Máy chủ Java Standalone chạy trực tiếp
├── src/main/webapp/
│   ├── css/
│   │   └── style.css                  # Toàn bộ mã CSS hiện đại, responsive, hiệu ứng
│   ├── js/
│   │   └── app.js                     # JavaScript tương tác tìm kiếm, lọc, modal, giỏ hàng
│   ├── WEB-INF/
│   │   ├── lib/
│   │   │   └── servlet-api.jar
│   │   └── web.xml                    # Deployment Descriptor Jakarta EE 10
│   ├── login.html / login.jsp         # Giao diện khung đăng nhập
│   └── home.html / home.jsp           # Giao diện trang chủ sau khi đăng nhập
├── .classpath                         # Cấu hình Classpath Eclipse
├── .project                           # Cấu hình Project Eclipse
├── compile.bat                        # Script biên dịch nhanh
├── run.bat                            # Script chạy nhanh 1-click
└── README.md                          # Hướng dẫn chi tiết
```
