<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String errorMessage = (String) request.getAttribute("errorMessage");
    String urlMsg = request.getParameter("message");
    String urlErr = request.getParameter("error");
    String username = (String) request.getAttribute("username");
    if (username == null) username = "";

    String alertHtml = "";
    if (errorMessage != null && !errorMessage.isEmpty()) {
        alertHtml = "<div class=\"alert alert-danger\"><i class=\"fas fa-exclamation-circle\"></i> " + errorMessage + "</div>";
    } else if ("logged_out".equals(urlMsg)) {
        alertHtml = "<div class=\"alert alert-success\"><i class=\"fas fa-check-circle\"></i> Bạn đã đăng xuất an toàn khỏi hệ thống!</div>";
    } else if ("require_login".equals(urlErr)) {
        alertHtml = "<div class=\"alert alert-danger\"><i class=\"fas fa-shield-alt\"></i> Vui lòng đăng nhập để truy cập trang chủ!</div>";
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng Nhập - BookHaven | Tiệm Sách Tri Thức</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="login-page-wrapper">
    <div class="login-bg-decor login-bg-decor-1"></div>
    <div class="login-bg-decor login-bg-decor-2"></div>

    <main class="login-container" id="loginContainer">
        <!-- Cột trái: Banner giới thiệu -->
        <section class="login-banner">
            <div class="brand-header">
                <div class="brand-icon">
                    <i class="fas fa-book-bookmark"></i>
                </div>
                <div>
                    <div class="brand-name">BookHaven</div>
                    <div class="brand-tagline">Không Gian Tri Thức & Nghệ Thuật Sống</div>
                </div>
            </div>

            <div class="banner-content">
                <h2 class="banner-title">Mỗi cuốn sách là một cuộc phiêu lưu mới.</h2>
                <p class="banner-desc">
                    Khám phá hàng ngàn tựa sách hay nhất từ văn học, kinh tế, công nghệ cho đến kỹ năng sống. Đồng hành cùng bạn trên chặng đường phát triển bản thân.
                </p>

                <div class="banner-stats">
                    <div class="stat-item">
                        <h4>15,000+</h4>
                        <p>Đầu sách chọn lọc</p>
                    </div>
                    <div class="stat-item">
                        <h4>50,000+</h4>
                        <p>Độc giả tin cậy</p>
                    </div>
                    <div class="stat-item">
                        <h4>4.9 ★</h4>
                        <p>Đánh giá dịch vụ</p>
                    </div>
                </div>
            </div>

            <div class="banner-footer">
                <i class="fas fa-shield-halved"></i>
                <span>Bảo mật dữ liệu tuyệt đối theo tiêu chuẩn Java Security</span>
            </div>
        </section>

        <!-- Cột phải: Form đăng nhập -->
        <section class="login-form-box">
            <div class="form-header">
                <h1 class="form-title">Đăng nhập tài khoản</h1>
                <p class="form-subtitle">Chào mừng bạn trở lại! Nhập thông tin để tiếp tục.</p>
            </div>

            <%= alertHtml %>

            <form action="${pageContext.request.contextPath}/login" method="POST" id="loginForm" novalidate>
                <div class="form-group">
                    <label for="username" class="form-label">Tên đăng nhập hoặc Email</label>
                    <div class="input-with-icon">
                        <i class="fas fa-user input-icon"></i>
                        <input type="text" id="username" name="username" class="form-input" 
                               value="<%= username %>" placeholder="Nhập tên đăng nhập hoặc email..." required autofocus>
                    </div>
                </div>

                <div class="form-group">
                    <label for="password" class="form-label">Mật khẩu</label>
                    <div class="input-with-icon">
                        <i class="fas fa-lock input-icon"></i>
                        <input type="password" id="password" name="password" class="form-input" 
                               placeholder="Nhập mật khẩu..." required>
                        <button type="button" class="password-toggle" id="togglePassword" title="Hiện/ẩn mật khẩu">
                            <i class="fas fa-eye"></i>
                        </button>
                    </div>
                </div>

                <div class="form-options">
                    <label class="remember-me">
                        <input type="checkbox" name="remember" value="on">
                        <span>Ghi nhớ đăng nhập</span>
                    </label>
                    <a href="javascript:void(0)" onclick="alert('Hãy sử dụng một trong các tài khoản thử nghiệm bên dưới!')" class="forgot-link">Quên mật khẩu?</a>
                </div>

                <button type="submit" class="btn-primary" id="btnSubmit">
                    <span>Đăng Nhập Ngay</span>
                    <i class="fas fa-arrow-right"></i>
                </button>
            </form>

            <!-- Tài khoản mẫu -->
            <div class="quick-login-section">
                <div class="quick-login-title"><i class="fas fa-bolt"></i> Chọn nhanh tài khoản thử nghiệm</div>
                <div class="quick-pills">
                    <button type="button" class="quick-pill" data-user="admin" data-pass="123456" title="Tài khoản Quản Trị">
                        <i class="fas fa-user-shield"></i>
                        <span>Admin (admin)</span>
                    </button>
                    <button type="button" class="quick-pill" data-user="oanh" data-pass="123456" title="Tài khoản Hoàng Oanh">
                        <i class="fas fa-user"></i>
                        <span>Hoàng Oanh (oanh)</span>
                    </button>
                    <button type="button" class="quick-pill" data-user="khachhang" data-pass="123456" title="Tài khoản Khách hàng">
                        <i class="fas fa-bag-shopping"></i>
                        <span>Khách Hàng (khachhang)</span>
                    </button>
                </div>
            </div>

            <p style="text-align: center; margin-top: 24px; font-size: 13px; color: var(--text-secondary);">
                Chưa có tài khoản? 
                <a href="javascript:void(0)" onclick="alert('Vui lòng sử dụng các tài khoản có sẵn ở trên!')" style="color: var(--primary-light); font-weight: 700;">Đăng ký thành viên mới</a>
            </p>
        </section>
    </main>

    <script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
