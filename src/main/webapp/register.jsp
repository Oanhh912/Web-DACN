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

        <form action="${pageContext.request.contextPath}/register" method="POST" id="registerForm" onsubmit="event.preventDefault(); handleRegisterSubmit();" novalidate>
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
                <label class="remember-me" id="agreeTermsLabel" style="cursor: pointer;">
                    <input type="checkbox" name="agreeTerms" id="agreeTerms" required>
                    <span style="font-size: 13px;">Tôi đồng ý với <a href="javascript:void(0)" style="color: var(--primary-light); text-decoration: underline;">Điều khoản sử dụng</a> & <a href="javascript:void(0)" style="color: var(--primary-light); text-decoration: underline;">Chính sách bảo mật</a></span>
                </label>
            </div>

            <div style="display: flex; justify-content: center; margin-top: 10px;">
                <button type="button" class="btn-primary" id="btnRegisterSubmit" onclick="handleRegisterSubmit()" style="padding: 12px 36px; font-size: 15px; width: auto; min-width: 200px;">
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

    <!-- POPUP MODAL THÔNG BÁO / CẢNH BÁO ĐĂNG KÝ -->
    <div class="otp-modal-overlay" id="warningAlertModal">
        <div class="otp-modal-box" style="max-width: 440px;">
            <div class="otp-icon-wrap" id="warningAlertIconWrap" style="background: #fee2e2; color: #ef4444; width: 68px; height: 68px; margin: 0 auto 16px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 28px;">
                <i class="fas fa-triangle-exclamation" id="warningAlertIcon"></i>
            </div>
            <h2 class="otp-modal-title" id="warningAlertTitle" style="font-size: 22px; font-weight: 700; margin-bottom: 8px; color: #0f172a;">Cảnh báo</h2>
            <p class="otp-modal-desc" id="warningAlertMsg" style="font-size: 14.5px; line-height: 1.6; color: #475569; margin: 0 0 24px;">
                Nội dung cảnh báo...
            </p>
            <div id="warningAlertActions" style="display: flex; gap: 10px; justify-content: center;">
                <button type="button" class="btn-primary" id="btnWarningClose" onclick="closeWarningAlertModal()" style="min-width: 140px;">
                    <span id="btnWarningCloseText">Đã hiểu</span>
                </button>
                <a href="${pageContext.request.contextPath}/login" class="btn-primary" id="btnWarningLogin" style="min-width: 140px; background: #f8fafc; color: var(--primary); border: 1px solid #cbd5e1; display: none; text-decoration: none; align-items: center; justify-content: center; font-weight: 600;">
                    <span><i class="fas fa-arrow-right-to-bracket"></i> Đăng nhập ngay</span>
                </a>
            </div>
        </div>
    </div>

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
            if (!pwd || typeof pwd !== 'string') return false;
            if (pwd.length < 8) return false;
            if (/\s/.test(pwd)) return false; // Không chấp nhận khoảng trắng
            const hasUpper = /[A-Z]/.test(pwd);
            const hasLower = /[a-z]/.test(pwd);
            const hasDigit = /[0-9]/.test(pwd);
            const hasSpecial = /[^A-Za-z0-9\s]/.test(pwd);
            return hasUpper && hasLower && hasDigit && hasSpecial;
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

        // ====================== XỬ LÝ POPUP CẢNH BÁO NGHIỆP VỤ ======================
        const warningAlertModal = document.getElementById('warningAlertModal');
        const warningAlertTitle = document.getElementById('warningAlertTitle');
        const warningAlertMsg = document.getElementById('warningAlertMsg');
        const warningAlertIconWrap = document.getElementById('warningAlertIconWrap');
        const warningAlertIcon = document.getElementById('warningAlertIcon');
        const btnWarningLogin = document.getElementById('btnWarningLogin');
        const btnWarningCloseText = document.getElementById('btnWarningCloseText');

        function showWarningPopup(title, message, iconClass = 'fa-triangle-exclamation', iconColor = '#ef4444', iconBg = '#fee2e2', showLoginBtn = false, focusElId = null) {
            warningAlertTitle.textContent = title;
            warningAlertMsg.textContent = message;
            warningAlertIconWrap.style.background = iconBg;
            warningAlertIconWrap.style.color = iconColor;
            warningAlertIcon.className = 'fas ' + iconClass;
            btnWarningLogin.style.display = showLoginBtn ? 'inline-flex' : 'none';
            btnWarningCloseText.textContent = showLoginBtn ? 'Đổi email khác' : 'Đã hiểu';

            warningAlertModal.classList.add('active');
            warningAlertModal.dataset.focusEl = focusElId || '';
        }

        function closeWarningAlertModal() {
            warningAlertModal.classList.remove('active');
            const focusId = warningAlertModal.dataset.focusEl;
            if (focusId) {
                const el = document.getElementById(focusId);
                if (el) {
                    el.focus();
                    if (el.select) el.select();
                }
            }
        }

        if (warningAlertModal) {
            warningAlertModal.addEventListener('click', (e) => {
                if (e.target === warningAlertModal) {
                    closeWarningAlertModal();
                }
            });
        }

        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,24}$/;
        const invalidTlds = ['colm', 'con', 'comm', 'cmo', 'cpm', 'coom', 'cm', 'c0m', 'gmai', 'gamil', 'gmial', 'gmaill', 'yaho', 'yahooo', 'hotmial'];
        const invalidDomains = ['gmai.com', 'gamil.com', 'gmial.com', 'gmaill.com', 'gmail.con', 'gmail.colm', 'yaho.com', 'yahooo.com', 'hotmial.com', 'outlok.com'];

        function isClientEmailValid(val) {
            if (!val || !emailRegex.test(val)) return false;
            const at = val.lastIndexOf('@');
            if (at <= 0) return false;
            const domain = val.substring(at + 1).toLowerCase();
            const dot = domain.lastIndexOf('.');
            if (dot <= 0) return false;
            const tld = domain.substring(dot + 1);
            if (invalidTlds.includes(tld)) return false;
            if (invalidDomains.includes(domain)) return false;
            return true;
        }

        const emailInput = document.getElementById('email');

        // Kiểm tra Email ngay khi người dùng rời khỏi ô nhập (on blur)
        if (emailInput) {
            emailInput.addEventListener('blur', async () => {
                const val = emailInput.value.trim();
                if (!val) return;

                if (!isClientEmailValid(val)) {
                    showWarningPopup(
                        'Sai định dạng Email',
                        'Bạn đang nhập sai định dạng email! Vui lòng kiểm tra lại địa chỉ email (ví dụ: yourname@gmail.com).',
                        'fa-envelope-circle-check',
                        '#f59e0b',
                        '#fef3c7',
                        false,
                        'email'
                    );
                    return;
                }

                try {
                    const res = await fetch('${pageContext.request.contextPath}/api/check-email?email=' + encodeURIComponent(val));
                    const d = await res.json();
                    if (d.code === 'INVALID_EMAIL' || (!d.success && !d.exists)) {
                        showWarningPopup(
                            'Sai định dạng Email',
                            d.message || 'Bạn đang nhập sai định dạng email! Vui lòng kiểm tra lại địa chỉ email (ví dụ: yourname@gmail.com).',
                            'fa-envelope-circle-check',
                            '#f59e0b',
                            '#fef3c7',
                            false,
                            'email'
                        );
                    } else if (d.exists || d.code === 'EMAIL_EXISTS') {
                        showWarningPopup(
                            'Email đã tồn tại',
                            d.message || 'Email này đã tồn tại! Mỗi email chỉ được đăng ký 1 tài khoản duy nhất. Vui lòng sử dụng email khác hoặc đăng nhập bằng tài khoản hiện có.',
                            'fa-triangle-exclamation',
                            '#ef4444',
                            '#fee2e2',
                            true,
                            'email'
                        );
                    }
                } catch (ignored) {}
            });
        }

        function escapeHtmlText(str) {
            return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }

        async function handleRegisterSubmit() {
            const fullName = (document.getElementById('fullName') ? document.getElementById('fullName').value : '').trim();
            const username = (document.getElementById('username') ? document.getElementById('username').value : '').trim();
            const email = (document.getElementById('email') ? document.getElementById('email').value : '').trim();
            const password = document.getElementById('password') ? document.getElementById('password').value : '';
            const confirmPassword = document.getElementById('confirmPassword') ? document.getElementById('confirmPassword').value : '';
            const agreeTerms = document.getElementById('agreeTerms');

            // 1. Kiểm tra các trường bắt buộc
            if (!fullName || !username || !email || !password || !confirmPassword) {
                showWarningPopup(
                    'Thiếu thông tin bắt buộc',
                    'Vui lòng điền đầy đủ các thông tin bắt buộc (Họ tên, Tên đăng nhập, Email, Mật khẩu)!',
                    'fa-circle-exclamation',
                    '#ef4444',
                    '#fee2e2'
                );
                return;
            }

            // 2. Kiểm tra định dạng Email
            if (!isClientEmailValid(email)) {
                showWarningPopup(
                    'Sai định dạng Email',
                    'Bạn đang nhập sai định dạng email! Vui lòng nhập đúng định dạng email chuẩn (ví dụ: yourname@gmail.com).',
                    'fa-envelope-circle-check',
                    '#f59e0b',
                    '#fef3c7',
                    false,
                    'email'
                );
                return;
            }

            // 3. Kiểm tra điều khoản sử dụng
            if (!agreeTerms || !agreeTerms.checked) {
                showWarningPopup(
                    'Chưa đồng ý điều khoản',
                    'Bạn chưa đồng ý điều khoản và yêu cầu phải đồng ý điều khoản mới được đăng ký tài khoản!',
                    'fa-file-shield',
                    '#f59e0b',
                    '#fef3c7',
                    false,
                    'agreeTerms'
                );
                return;
            }

            // 4. Kiểm tra mật khẩu an toàn
            if (!checkPasswordStrength(password)) {
                showWarningPopup(
                    'Mật khẩu chưa đáp ứng yêu cầu',
                    'Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.',
                    'fa-shield-halved',
                    '#f59e0b',
                    '#fef3c7',
                    false,
                    'password'
                );
                return;
            }

            // 5. Kiểm tra mật khẩu xác nhận
            if (password !== confirmPassword) {
                showWarningPopup(
                    'Mật khẩu không khớp',
                    'Mật khẩu xác nhận không trùng khớp! Vui lòng kiểm tra lại.',
                    'fa-key',
                    '#ef4444',
                    '#fee2e2',
                    false,
                    'confirmPassword'
                );
                return;
            }

            btnRegisterSubmit.disabled = true;
            btnRegisterSubmit.innerHTML = '<span>Đang kiểm tra & gửi mã OTP...</span> <i class="fas fa-spinner fa-spin"></i>';

            try {
                const formData = new URLSearchParams();
                formData.append('fullName', fullName);
                formData.append('username', username);
                formData.append('email', email);
                formData.append('phone', '');
                formData.append('password', password);
                formData.append('confirmPassword', confirmPassword);
                formData.append('agreeTerms', agreeTerms.checked ? 'true' : 'false');
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
                    if (data.code === 'EMAIL_EXISTS' || (data.message && data.message.includes('Email') && data.message.includes('tồn tại'))) {
                        showWarningPopup(
                            'Email đã tồn tại',
                            data.message || 'Email này đã tồn tại! Mỗi email chỉ được đăng ký 1 tài khoản duy nhất. Vui lòng sử dụng email khác hoặc đăng nhập bằng tài khoản hiện có.',
                            'fa-triangle-exclamation',
                            '#ef4444',
                            '#fee2e2',
                            true,
                            'email'
                        );
                    } else if (data.code === 'INVALID_EMAIL' || (data.message && data.message.includes('định dạng email'))) {
                        showWarningPopup(
                            'Sai định dạng Email',
                            data.message || 'Bạn đã nhập sai định dạng email! Vui lòng nhập đúng định dạng (ví dụ: yourname@gmail.com).',
                            'fa-envelope-circle-check',
                            '#f59e0b',
                            '#fef3c7',
                            false,
                            'email'
                        );
                    } else if (data.code === 'TERMS_NOT_AGREED' || (data.message && data.message.includes('điều khoản'))) {
                        showWarningPopup(
                            'Chưa đồng ý điều khoản',
                            data.message || 'Bạn chưa đồng ý điều khoản và yêu cầu phải đồng ý điều khoản mới được đăng ký tài khoản!',
                            'fa-file-shield',
                            '#f59e0b',
                            '#fef3c7',
                            false,
                            'agreeTerms'
                        );
                    } else {
                        showWarningPopup(
                            'Không thể tiếp tục',
                            data.message || 'Có lỗi xảy ra, vui lòng thử lại!',
                            'fa-circle-exclamation',
                            '#ef4444',
                            '#fee2e2'
                        );
                    }
                }
            } catch (err) {
                btnRegisterSubmit.disabled = false;
                btnRegisterSubmit.innerHTML = '<span>Tiếp tục</span> <i class="fas fa-arrow-right"></i>';
                showWarningPopup(
                    'Lỗi kết nối',
                    'Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại kết nối mạng!',
                    'fa-wifi',
                    '#ef4444',
                    '#fee2e2'
                );
            }
        }
        window.handleRegisterSubmit = handleRegisterSubmit;

        if (regForm) {
            regForm.addEventListener('submit', (e) => {
                e.preventDefault();
                handleRegisterSubmit();
            });
        }

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
