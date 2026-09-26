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

    <main class="register-card" id="registerContainer">
        <!-- Logo & Tiêu đề đăng ký -->
        <div style="text-align: center; margin-bottom: 28px;">
            <a href="${pageContext.request.contextPath}/home" class="site-logo" style="justify-content: center; margin-bottom: 14px; display: inline-flex;">
                <div class="logo-icon">
                    <i class="fas fa-book-bookmark"></i>
                </div>
                <div class="logo-text" style="text-align: left;">
                    <h1>Bookora</h1>
                    <span>Tiệm Sách Tri Thức</span>
                </div>
            </a>
            <h1 class="form-title" style="font-size: 26px; margin-bottom: 6px;">Đăng ký tài khoản</h1>
            <p class="form-subtitle">Điền thông tin bên dưới để trở thành độc giả thân thiết của Bookora</p>
        </div>

        <%= alertHtml %>

        <form action="${pageContext.request.contextPath}/register" method="POST" id="registerForm" novalidate>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;">
                <div class="form-group" style="margin-bottom: 0;">
                    <label for="fullName" class="form-label">Họ và tên của bạn <span style="color: #ef4444;">*</span></label>
                    <div class="input-with-icon">
                        <i class="fas fa-address-card input-icon"></i>
                        <input type="text" id="fullName" name="fullName" class="form-input" 
                               value="<%= fullName %>" placeholder="Ví dụ: Nguyễn Văn An" required autofocus>
                    </div>
                </div>

                <div class="form-group" style="margin-bottom: 0;">
                    <label for="username" class="form-label">Tên đăng nhập <span style="color: #ef4444;">*</span></label>
                    <div class="input-with-icon">
                        <i class="fas fa-user input-icon"></i>
                        <input type="text" id="username" name="username" class="form-input" 
                               value="<%= username %>" placeholder="Tên tài khoản viết liền..." required>
                    </div>
                </div>
            </div>

            <div class="form-group" style="margin-bottom: 16px;">
                <label for="email" class="form-label">Địa chỉ Email <span style="color: #ef4444;">*</span></label>
                <div class="input-with-icon">
                    <i class="fas fa-envelope input-icon"></i>
                    <input type="email" id="email" name="email" class="form-input" 
                           value="<%= email %>" placeholder="Nhập địa chỉ email của bạn (ví dụ: yourname@gmail.com)" required>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px;">
                <div class="form-group" style="margin-bottom: 0;">
                    <label for="password" class="form-label">Mật khẩu <span style="color: #ef4444;">*</span></label>
                    <div class="input-with-icon">
                        <i class="fas fa-lock input-icon"></i>
                        <input type="password" id="password" name="password" class="form-input" 
                               placeholder="Tối thiểu 8 ký tự..." required>
                        <button type="button" class="password-toggle" id="toggleRegPassword" title="Hiện/ẩn mật khẩu">
                            <i class="fas fa-eye"></i>
                        </button>
                    </div>
                </div>

                <div class="form-group" style="margin-bottom: 0;">
                    <label for="confirmPassword" class="form-label">Xác nhận mật khẩu <span style="color: #ef4444;">*</span></label>
                    <div class="input-with-icon">
                        <i class="fas fa-shield-alt input-icon"></i>
                        <input type="password" id="confirmPassword" name="confirmPassword" class="form-input" 
                               placeholder="Nhập lại mật khẩu..." required>
                    </div>
                </div>
            </div>

            <div class="form-options" style="margin-bottom: 22px;">
                <label class="remember-me">
                    <input type="checkbox" name="agreeTerms" id="agreeTerms" checked required>
                    <span style="font-size: 13px;">Tôi đồng ý với <a href="javascript:void(0)" style="color: var(--primary-light); text-decoration: underline;">Điều khoản sử dụng</a> & <a href="javascript:void(0)" style="color: var(--primary-light); text-decoration: underline;">Chính sách bảo mật</a></span>
                </label>
            </div>

            <div style="display: flex; justify-content: center; margin-top: 10px;">
                <button type="submit" class="btn-primary" id="btnRegisterSubmit" style="padding: 12px 36px; font-size: 15px; width: auto; min-width: 200px;">
                    <span>Tiếp tục</span>
                    <i class="fas fa-arrow-right"></i>
                </button>
            </div>
        </form>

        <p style="text-align: center; margin-top: 24px; font-size: 14px; color: var(--text-secondary);">
            Đã có tài khoản? 
            <a href="${pageContext.request.contextPath}/login" style="color: var(--primary-light); font-weight: 700; text-decoration: none;">Đăng nhập ngay</a>
            <span style="margin: 0 10px; color: var(--border);">|</span>
            <a href="${pageContext.request.contextPath}/home" style="color: var(--text-muted);"><i class="fas fa-house"></i> Về trang chủ</a>
        </p>
    </main>

    <!-- POPUP MODAL XÁC THỰC MÃ OTP -->
    <div class="otp-modal-overlay" id="otpModal">
        <div class="otp-modal-box">
            <div class="otp-icon-wrap">
                <i class="fas fa-shield-halved"></i>
            </div>
            <h2 class="otp-modal-title">Xác thực tài khoản</h2>
            <p class="otp-modal-desc">
                Mã xác thực OTP gồm 6 chữ số đã được gửi đến <br>
                <strong id="otpTargetDisplay" style="color: var(--primary-light);">...</strong>
            </p>

            <div id="smsGatewayStatus" style="font-size: 13px; color: #475569; margin: 14px 0 18px; min-height: 20px; line-height: 1.5;"></div>

            <div style="margin-bottom: 12px;">
                <input type="text" id="inputOtpCode" class="otp-input-field" maxlength="6" placeholder="• • • • • •" autocomplete="off">
            </div>

            <div class="otp-timer-row">
                <div><i class="far fa-clock"></i> Còn lại: <strong id="timerText" style="color: #ef4444;">02:00</strong></div>
                <button type="button" class="btn-resend-otp" id="btnResend" onclick="handleResendOtp()" disabled>Gửi lại mã</button>
            </div>

            <div id="otpErrorMsg" class="alert alert-danger" style="display: none; padding: 10px 14px; font-size: 13px; margin-bottom: 16px;"></div>

            <div style="display: flex; gap: 10px;">
                <button type="button" class="btn-primary" style="background: #f1f5f9; color: var(--text-secondary); box-shadow: none; flex: 1;" onclick="closeOtpModal()">
                    <span>Quay lại</span>
                </button>
                <button type="button" class="btn-primary" id="btnConfirmOtp" style="flex: 2;" onclick="handleVerifyOtp()">
                    <span>Xác Nhận Đăng Ký</span>
                    <i class="fas fa-check"></i>
                </button>
            </div>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/js/app.js?v=20260926_10"></script>
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

        // Kiểm tra ràng buộc mật khẩu an toàn
        function checkPasswordStrength(pwd) {
            const hasLength = (pwd || '').length >= 8;
            const hasUpper = /[A-Z]/.test(pwd || '');
            const hasLower = /[a-z]/.test(pwd || '');
            const hasDigit = /[0-9]/.test(pwd || '');
            const hasSpecial = /[^A-Za-z0-9]/.test(pwd || '');
            return hasLength && hasUpper && hasLower && hasDigit && hasSpecial;
        }

        // ====================== XỬ LÝ GỬI & XÁC THỰC OTP QUA EMAIL ======================
        let currentOtpCode = '';
        let currentUsername = '';
        let timerInterval = null;
        let timeLeft = 120; // 2 phút

        const regForm = document.getElementById('registerForm');
        const otpModal = document.getElementById('otpModal');
        const inputOtpCode = document.getElementById('inputOtpCode');
        const otpErrorMsg = document.getElementById('otpErrorMsg');
        const timerText = document.getElementById('timerText');
        const btnResend = document.getElementById('btnResend');
        const btnRegisterSubmit = document.getElementById('btnRegisterSubmit');

        function escapeHtmlText(str) {
            return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }

        regForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const fullName = document.getElementById('fullName').value.trim();
            const username = document.getElementById('username').value.trim();
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            if (!fullName || !username || !email || !password || !confirmPassword) {
                alert('Vui lòng điền đầy đủ các thông tin bắt buộc (Họ tên, Tên đăng nhập, Email, Mật khẩu)!');
                return;
            }

            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                alert('Địa chỉ Email không hợp lệ! Vui lòng kiểm tra lại.');
                document.getElementById('email').focus();
                return;
            }

            if (!checkPasswordStrength(password)) {
                alert('Mật khẩu phải có ít nhất 8 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt!');
                document.getElementById('password').focus();
                return;
            }

            if (password !== confirmPassword) {
                alert('Mật khẩu xác nhận không trùng khớp!');
                return;
            }

            btnRegisterSubmit.disabled = true;
            btnRegisterSubmit.innerHTML = '<span>Đang gửi mã OTP...</span> <i class="fas fa-spinner fa-spin"></i>';

            try {
                const formData = new URLSearchParams();
                formData.append('fullName', fullName);
                formData.append('username', username);
                formData.append('email', email);
                formData.append('phone', '');
                formData.append('password', password);
                formData.append('confirmPassword', confirmPassword);
                formData.append('otpChannel', 'email');

                const response = await fetch('${pageContext.request.contextPath}/api/send-otp', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                    body: formData.toString()
                });

                const data = await response.json();
                btnRegisterSubmit.disabled = false;
                btnRegisterSubmit.innerHTML = '<span>Tiếp tục</span> <i class="fas fa-arrow-right"></i>';

                if (data.success) {
                    currentUsername = username;

                    document.getElementById('otpTargetDisplay').textContent = data.target;

                    const gatewayEl = document.getElementById('smsGatewayStatus');
                    if (data.isRealEmail) {
                        gatewayEl.innerHTML = '<span style="color: #16a34a; font-weight: 600;"><i class="fas fa-envelope-circle-check"></i> Đã gửi email chứa mã OTP đến <strong>' + escapeHtmlText(data.target) + '</strong>.<br><small style="color: #64748b;">(Vui lòng mở hòm thư để lấy mã 6 chữ số. Kiểm tra cả thư mục Spam nếu cần)</small></span>';
                    } else {
                        gatewayEl.innerHTML = '<span style="color: #92400e; background: #fef3c7; padding: 6px 10px; border-radius: 6px; display: inline-block;"><i class="fas fa-info-circle"></i> Chưa cấu hình Gmail gửi trong <code>email_config.properties</code>.</span>';
                    }

                    inputOtpCode.value = '';
                    otpErrorMsg.style.display = 'none';
                    otpModal.classList.add('active');
                    inputOtpCode.focus();

                    startCountdown();
                } else {
                    alert(data.message || 'Có lỗi xảy ra, vui lòng thử lại!');
                }
            } catch (err) {
                btnRegisterSubmit.disabled = false;
                btnRegisterSubmit.innerHTML = '<span>Tiếp tục</span> <i class="fas fa-arrow-right"></i>';
                alert('Không thể kết nối tới máy chủ. Vui lòng thử lại!');
            }
        });

        function closeOtpModal() {
            otpModal.classList.remove('active');
            clearInterval(timerInterval);
        }

        function startCountdown() {
            clearInterval(timerInterval);
            timeLeft = 120;
            btnResend.disabled = true;

            updateTimerDisplay();
            timerInterval = setInterval(() => {
                timeLeft--;
                updateTimerDisplay();
                if (timeLeft <= 0) {
                    clearInterval(timerInterval);
                    btnResend.disabled = false;
                    timerText.textContent = '00:00 (Hết hạn)';
                }
            }, 1000);
        }

        function updateTimerDisplay() {
            const m = Math.floor(timeLeft / 60);
            const s = timeLeft % 60;
            timerText.textContent = (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
        }

        async function handleResendOtp() {
            btnResend.disabled = true;
            btnResend.textContent = 'Đang gửi lại...';

            const fullName = document.getElementById('fullName').value.trim();
            const username = document.getElementById('username').value.trim();
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            const formData = new URLSearchParams();
            formData.append('fullName', fullName);
            formData.append('username', username);
            formData.append('email', email);
            formData.append('phone', '');
            formData.append('password', password);
            formData.append('confirmPassword', confirmPassword);
            formData.append('otpChannel', 'email');

            try {
                const response = await fetch('${pageContext.request.contextPath}/api/send-otp', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                    body: formData.toString()
                });
                const data = await response.json();
                btnResend.textContent = 'Gửi lại mã';

                if (data.success) {
                    inputOtpCode.value = '';
                    otpErrorMsg.style.display = 'none';
                    startCountdown();
                    alert('Đã gửi lại mã OTP mới! Vui lòng kiểm tra hộp thư của bạn.');
                } else {
                    alert(data.message || 'Không thể gửi lại mã OTP.');
                }
            } catch (err) {
                btnResend.disabled = false;
                btnResend.textContent = 'Gửi lại mã';
                alert('Lỗi kết nối máy chủ khi gửi lại OTP!');
            }
        }

        async function handleVerifyOtp() {
            const otp = inputOtpCode.value.trim();
            if (!otp || otp.length < 6) {
                otpErrorMsg.textContent = 'Vui lòng nhập đủ 6 chữ số mã OTP!';
                otpErrorMsg.style.display = 'block';
                return;
            }

            const btnConfirm = document.getElementById('btnConfirmOtp');
            btnConfirm.disabled = true;
            btnConfirm.innerHTML = '<span>Đang xác nhận...</span> <i class="fas fa-spinner fa-spin"></i>';

            try {
                const formData = new URLSearchParams();
                formData.append('username', currentUsername);
                formData.append('otp', otp);

                const response = await fetch('${pageContext.request.contextPath}/api/verify-otp', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                    body: formData.toString()
                });

                const data = await response.json();
                btnConfirm.disabled = false;
                btnConfirm.innerHTML = '<span>Xác Nhận Đăng Ký</span> <i class="fas fa-check"></i>';

                if (data.success) {
                    clearInterval(timerInterval);
                    window.location.href = '${pageContext.request.contextPath}/' + (data.redirect || 'login?message=register_success');
                } else {
                    otpErrorMsg.textContent = data.message || 'Mã OTP không chính xác!';
                    otpErrorMsg.style.display = 'block';
                }
            } catch (err) {
                btnConfirm.disabled = false;
                btnConfirm.innerHTML = '<span>Xác Nhận Đăng Ký</span> <i class="fas fa-check"></i>';
                otpErrorMsg.textContent = 'Không thể kết nối đến máy chủ xác thực!';
                otpErrorMsg.style.display = 'block';
            }
        }
    </script>
</body>
</html>
