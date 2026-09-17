<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String errorMessage = (String) request.getAttribute("errorMessage");
    String fullName = (String) request.getAttribute("fullName");
    String username = (String) request.getAttribute("username");
    String email = (String) request.getAttribute("email");
    String phone = (String) request.getAttribute("phone");

    if (fullName == null) fullName = "";
    if (username == null) username = "";
    if (email == null) email = "";
    if (phone == null) phone = "";

    String alertHtml = "";
    if (errorMessage != null && !errorMessage.isEmpty()) {
        alertHtml = "<div class=\"alert alert-danger\"><i class=\"fas fa-exclamation-circle\"></i> " + errorMessage + "</div>";
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng Ký Tài Khoản - Bookora | Tiệm Sách Tri Thức</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="login-page-wrapper">
    <div class="login-bg-decor login-bg-decor-1"></div>
    <div class="login-bg-decor login-bg-decor-2"></div>

    <main class="login-container" id="registerContainer" style="max-width: 1060px;">
        <!-- Cột trái: Giới thiệu quyền lợi thành viên -->
        <section class="login-banner">
            <div class="brand-header">
                <div class="brand-icon">
                    <i class="fas fa-book-bookmark"></i>
                </div>
                <div>
                    <div class="brand-name">Bookora</div>
                    <div class="brand-tagline">Không Gian Tri Thức & Nghệ Thuật Sống</div>
                </div>
            </div>

            <div class="banner-content">
                <h2 class="banner-title">Gia nhập cộng đồng yêu sách Bookora</h2>
                <p class="banner-desc">
                    Tạo tài khoản ngay hôm nay để nhận nhiều đặc quyền hấp dẫn dành riêng cho thành viên mới.
                </p>

                <div style="display: flex; flex-direction: column; gap: 16px; margin-top: 24px;">
                    <div style="display: flex; align-items: center; gap: 14px;">
                        <div style="width: 36px; height: 36px; border-radius: 50%; background: rgba(253, 230, 138, 0.2); display: flex; align-items: center; justify-content: center; color: #fde68a; font-size: 16px;">
                            <i class="fas fa-gift"></i>
                        </div>
                        <div>
                            <div style="font-weight: 700; font-size: 14px;">Tặng mã giảm giá 20%</div>
                            <div style="font-size: 12px; color: #c7d2fe;">Áp dụng cho đơn hàng đầu tiên của bạn</div>
                        </div>
                    </div>

                    <div style="display: flex; align-items: center; gap: 14px;">
                        <div style="width: 36px; height: 36px; border-radius: 50%; background: rgba(253, 230, 138, 0.2); display: flex; align-items: center; justify-content: center; color: #fde68a; font-size: 16px;">
                            <i class="fas fa-coins"></i>
                        </div>
                        <div>
                            <div style="font-weight: 700; font-size: 14px;">Tích điểm đổi quà</div>
                            <div style="font-size: 12px; color: #c7d2fe;">Tích lũy 5% giá trị mỗi cuốn sách mua</div>
                        </div>
                    </div>

                    <div style="display: flex; align-items: center; gap: 14px;">
                        <div style="width: 36px; height: 36px; border-radius: 50%; background: rgba(253, 230, 138, 0.2); display: flex; align-items: center; justify-content: center; color: #fde68a; font-size: 16px;">
                            <i class="fas fa-bolt"></i>
                        </div>
                        <div>
                            <div style="font-weight: 700; font-size: 14px;">Giao hàng ưu tiên 2 Giờ</div>
                            <div style="font-size: 12px; color: #c7d2fe;">Đóng gói cẩn thận, bảo vệ mép sách hoàn hảo</div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="banner-footer">
                <i class="fas fa-shield-halved"></i>
                <span>Thông tin cá nhân được bảo mật an toàn theo tiêu chuẩn MySQL</span>
            </div>
        </section>

        <!-- Cột phải: Form đăng ký thành viên mới -->
        <section class="login-form-box" style="padding: 40px 45px;">
            <div class="form-header" style="margin-bottom: 20px;">
                <h1 class="form-title">Đăng ký tài khoản</h1>
                <p class="form-subtitle">Chỉ mất 1 phút để trở thành độc giả thân thiết của Bookora.</p>
            </div>

            <%= alertHtml %>

            <form action="${pageContext.request.contextPath}/register" method="POST" id="registerForm" novalidate>
                <div class="form-group" style="margin-bottom: 14px;">
                    <label for="fullName" class="form-label">Họ và tên của bạn</label>
                    <div class="input-with-icon">
                        <i class="fas fa-address-card input-icon"></i>
                        <input type="text" id="fullName" name="fullName" class="form-input" 
                               value="<%= fullName %>" placeholder="Ví dụ: Nguyễn Văn An" required autofocus>
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 14px;">
                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="username" class="form-label">Tên đăng nhập</label>
                        <div class="input-with-icon">
                            <i class="fas fa-user input-icon"></i>
                            <input type="text" id="username" name="username" class="form-input" 
                                   value="<%= username %>" placeholder="Tên tài khoản..." required>
                        </div>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="phone" class="form-label">Số điện thoại</label>
                        <div class="input-with-icon">
                            <i class="fas fa-phone input-icon"></i>
                            <input type="tel" id="phone" name="phone" class="form-input" 
                                   value="<%= phone %>" placeholder="09xxxxxxxxx">
                        </div>
                    </div>
                </div>

                <div class="form-group" style="margin-bottom: 14px;">
                    <label for="email" class="form-label">Địa chỉ Email</label>
                    <div class="input-with-icon">
                        <i class="fas fa-envelope input-icon"></i>
                        <input type="email" id="email" name="email" class="form-input" 
                               value="<%= email %>" placeholder="email@domain.com" required>
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px;">
                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="password" class="form-label">Mật khẩu</label>
                        <div class="input-with-icon">
                            <i class="fas fa-lock input-icon"></i>
                            <input type="password" id="password" name="password" class="form-input" 
                                   placeholder="Tối thiểu 6 ký tự..." required>
                            <button type="button" class="password-toggle" id="toggleRegPassword" title="Hiện/ẩn mật khẩu">
                                <i class="fas fa-eye"></i>
                            </button>
                        </div>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="confirmPassword" class="form-label">Xác nhận mật khẩu</label>
                        <div class="input-with-icon">
                            <i class="fas fa-shield-alt input-icon"></i>
                            <input type="password" id="confirmPassword" name="confirmPassword" class="form-input" 
                                   placeholder="Nhập lại mật khẩu..." required>
                        </div>
                    </div>
                </div>

                <div class="form-options" style="margin-bottom: 20px;">
                    <label class="remember-me">
                        <input type="checkbox" name="agreeTerms" id="agreeTerms" checked required>
                        <span style="font-size: 12px;">Tôi đồng ý với <a href="javascript:void(0)" style="color: var(--primary-light); text-decoration: underline;">Điều khoản sử dụng</a> & <a href="javascript:void(0)" style="color: var(--primary-light); text-decoration: underline;">Chính sách bảo mật</a></span>
                    </label>
                </div>

                <button type="submit" class="btn-primary" id="btnRegisterSubmit">
                    <span>Đăng Ký Tài Khoản</span>
                    <i class="fas fa-user-plus"></i>
                </button>
            </form>

            <p style="text-align: center; margin-top: 20px; font-size: 13px; color: var(--text-secondary);">
                Đã có tài khoản? 
                <a href="${pageContext.request.contextPath}/login" style="color: var(--primary-light); font-weight: 700;">Đăng nhập ngay</a>
            </p>
        </section>
    </main>

    <script src="${pageContext.request.contextPath}/js/app.js"></script>
    <script>
        const regToggle = document.getElementById('toggleRegPassword');
        const regPass = document.getElementById('password');
        const regConfirm = document.getElementById('confirmPassword');
        if (regToggle && regPass) {
            regToggle.addEventListener('click', () => {
                const type = regPass.getAttribute('type') === 'password' ? 'text' : 'password';
                regPass.setAttribute('type', type);
                if (regConfirm) regConfirm.setAttribute('type', type);
                regToggle.innerHTML = type === 'password' ? '<i class="fas fa-eye"></i>' : '<i class="fas fa-eye-slash"></i>';
            });
        }
    </script>
</body>
</html>
