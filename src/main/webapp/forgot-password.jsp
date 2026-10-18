<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên Mật Khẩu - Bookora | Tiệm Sách Tri Thức</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261018_square">
    <style>
        .fp-steps-nav {
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: relative;
            margin-bottom: 24px;
            padding: 0 4px;
        }
        .fp-steps-nav::before {
            content: '';
            position: absolute;
            top: 18px;
            left: 24px;
            right: 24px;
            height: 2px;
            background: #e2e8f0;
            z-index: 1;
        }
        .fp-step-item {
            position: relative;
            z-index: 2;
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
            gap: 6px;
            flex: 1;
        }
        .fp-step-circle {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: #fff;
            border: 2px solid #cbd5e1;
            color: #64748b;
            font-size: 14px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s ease;
        }
        .fp-step-item.active .fp-step-circle {
            background: var(--primary, #059669);
            border-color: var(--primary, #059669);
            color: #fff;
            box-shadow: 0 0 0 4px rgba(5, 150, 105, 0.18);
        }
        .fp-step-item.completed .fp-step-circle {
            background: #10b981;
            border-color: #10b981;
            color: #fff;
        }
        .fp-step-label {
            font-size: 11.5px;
            font-weight: 600;
            color: #64748b;
            text-transform: uppercase;
            letter-spacing: 0.3px;
        }
        .fp-step-item.active .fp-step-label {
            color: var(--primary, #059669);
        }
        .fp-step-item.completed .fp-step-label {
            color: #10b981;
        }

        .fp-step-panel {
            display: none;
            animation: fadeIn 0.3s ease;
        }
        .fp-step-panel.active {
            display: block;
        }

        .otp-input-field {
            font-family: monospace;
            font-size: 26px;
            font-weight: 700;
            text-align: center;
            letter-spacing: 10px;
            padding: 14px 16px;
            background: #f8fafc;
            border: 2px solid #cbd5e1;
            border-radius: var(--radius-md, 8px);
            color: #1e293b;
            transition: all 0.2s ease;
            width: 100%;
            box-sizing: border-box;
        }
        .otp-input-field:focus {
            border-color: var(--primary, #059669);
            background: #fff;
            outline: none;
            box-shadow: 0 0 0 4px rgba(5, 150, 105, 0.15);
        }

        .otp-timer-box {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: 10px;
            margin-bottom: 20px;
            font-size: 13px;
            color: #64748b;
        }
        .otp-timer-countdown {
            font-weight: 700;
            color: #ef4444;
        }

        .btn-resend-link {
            background: none;
            border: none;
            color: var(--primary, #059669);
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            padding: 0;
            text-decoration: underline;
            transition: color 0.2s;
        }
        .btn-resend-link:disabled {
            color: #94a3b8;
            cursor: not-allowed;
            text-decoration: none;
        }

        .pwd-rules-list {
            margin: 14px 0;
            padding: 12px 14px;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            font-size: 12.5px;
            list-style: none;
        }
        .pwd-rules-list li {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 6px;
            color: #64748b;
            transition: color 0.2s;
        }
        .pwd-rules-list li:last-child {
            margin-bottom: 0;
        }
        .pwd-rules-list li.valid {
            color: #059669;
            font-weight: 600;
        }
        .pwd-rules-list li.valid i {
            color: #059669;
        }
        .pwd-rules-list li.invalid i {
            color: #cbd5e1;
        }

        .alert-dismiss {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 10px;
            padding: 12px 16px;
            border-radius: 8px;
            font-size: 13.5px;
            margin-bottom: 16px;
            line-height: 1.4;
        }
        .alert-dismiss.alert-danger {
            background: #fee2e2;
            color: #991b1b;
            border: 1px solid #fecaca;
        }
        .alert-dismiss.alert-success {
            background: #dcfce7;
            color: #166534;
            border: 1px solid #bbf7d0;
        }
        .alert-dismiss.alert-info {
            background: #e0f2fe;
            color: #075985;
            border: 1px solid #bae6fd;
        }
    </style>
</head>
<body class="login-page-wrapper">
    <div class="login-bg-decor login-bg-decor-1"></div>
    <div class="login-bg-decor login-bg-decor-2"></div>

    <main class="login-container" id="forgotPasswordContainer">
        <!-- Cột trái: Giới thiệu & Điểm nhấn nhà sách -->
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
                <h2 class="banner-title">Khôi phục quyền truy cập nhanh chóng và an toàn.</h2>
                <p class="banner-desc">
                    Hệ thống xác thực 2 lớp qua Gmail giúp bảo vệ toàn diện tài khoản, lịch sử mua sắm và giỏ hàng của bạn tại Bookora.
                </p>

                <div class="banner-stats">
                    <div class="stat-item">
                        <h4>5 phút</h4>
                        <p>Hiệu lực mã OTP</p>
                    </div>
                    <div class="stat-item">
                        <h4>256-bit</h4>
                        <p>Mã hóa dữ liệu</p>
                    </div>
                    <div class="stat-item">
                        <h4>100%</h4>
                        <p>Bảo mật Gmail</p>
                    </div>
                </div>
            </div>

            <div class="banner-footer">
                <i class="fas fa-shield-halved"></i>
                <span>Xác thực OTP chuẩn bảo mật Gmail SMTP & Java Security</span>
            </div>
        </section>

        <!-- Cột phải: Form quy trình 3 bước -->
        <section class="login-form-box">
            <!-- Header quy trình 3 bước -->
            <div class="fp-steps-nav">
                <div class="fp-step-item active" id="stepIndicator1">
                    <div class="fp-step-circle">1</div>
                    <span class="fp-step-label">Nhập Email</span>
                </div>
                <div class="fp-step-item" id="stepIndicator2">
                    <div class="fp-step-circle">2</div>
                    <span class="fp-step-label">Xác Minh OTP</span>
                </div>
                <div class="fp-step-item" id="stepIndicator3">
                    <div class="fp-step-circle">3</div>
                    <span class="fp-step-label">Mật Khẩu Mới</span>
                </div>
            </div>

            <!-- Khu vực thông báo lỗi / thành công chung -->
            <div id="generalAlert" style="display: none;"></div>

            <!-- BƯỚC 1: NHẬP EMAIL -->
            <div class="fp-step-panel active" id="stepPanel1">
                <div class="form-header">
                    <h1 class="form-title">Quên mật khẩu?</h1>
                    <p class="form-subtitle">Nhập địa chỉ email đã đăng ký để nhận mã OTP xác thực khôi phục tài khoản.</p>
                </div>

                <form id="formStep1" onsubmit="handleSendOtp(event)">
                    <div class="form-group">
                        <label for="fpEmail" class="form-label">Địa chỉ Email đã đăng ký</label>
                        <div class="input-with-icon">
                            <i class="fas fa-envelope input-icon"></i>
                            <input type="email" id="fpEmail" class="form-input" 
                                   placeholder="Ví dụ: yourname@gmail.com" required autofocus>
                        </div>
                    </div>

                    <button type="submit" class="btn-primary" id="btnSendOtp">
                        <span>Gửi Mã Xác Thực OTP</span>
                        <i class="fas fa-paper-plane"></i>
                    </button>
                </form>

                <p style="text-align: center; margin-top: 24px; font-size: 13.5px; color: var(--text-secondary);">
                    Bạn đã nhớ lại mật khẩu? 
                    <a href="${pageContext.request.contextPath}/login" class="form-link"><strong>Đăng nhập ngay</strong></a>
                </p>
            </div>

            <!-- BƯỚC 2: NHẬP VÀ XÁC MINH OTP -->
            <div class="fp-step-panel" id="stepPanel2">
                <div class="form-header">
                    <h1 class="form-title">Xác thực mã OTP</h1>
                    <p class="form-subtitle">
                        Mã OTP 6 chữ số đã được gửi đến: <strong id="maskedEmailDisplay" style="color: var(--primary, #059669);">email của bạn</strong>.
                    </p>
                </div>

                <div class="otp-hint-banner" style="margin-bottom: 16px;">
                    <i class="fas fa-clock"></i>
                    <span>Mã OTP có hiệu lực trong <strong>5 phút</strong>. Vui lòng kiểm tra hộp thư đến (hoặc thư mục Spam).</span>
                </div>

                <form id="formStep2" onsubmit="handleVerifyOtp(event)">
                    <div class="form-group">
                        <label for="fpOtp" class="form-label">Nhập mã xác thực (6 chữ số)</label>
                        <input type="text" id="fpOtp" class="otp-input-field" 
                               maxlength="6" placeholder="------" autocomplete="one-time-code" required>
                    </div>

                    <div class="otp-timer-box">
                        <span>Thời hạn còn lại: <span class="otp-timer-countdown" id="timerDisplay">05:00</span></span>
                        <button type="button" class="btn-resend-link" id="btnResendOtp" onclick="handleResendOtp()" disabled>
                            Gửi lại mã (<span id="resendCountdown">60</span>s)
                        </button>
                    </div>

                    <button type="submit" class="btn-primary" id="btnVerifyOtp">
                        <span>Xác Nhận Mã OTP</span>
                        <i class="fas fa-arrow-right"></i>
                    </button>
                </form>

                <div style="display: flex; justify-content: space-between; margin-top: 22px; font-size: 13px;">
                    <a href="javascript:void(0)" onclick="goToStep(1)" class="form-link">
                        <i class="fas fa-arrow-left"></i> Đổi địa chỉ email khác
                    </a>
                    <a href="${pageContext.request.contextPath}/login" class="form-link">Về trang đăng nhập</a>
                </div>
            </div>

            <!-- BƯỚC 3: THIẾT LẬP MẬT KHẨU MỚI -->
            <div class="fp-step-panel" id="stepPanel3">
                <div class="form-header">
                    <h1 class="form-title">Đặt lại mật khẩu</h1>
                    <p class="form-subtitle">Thiết lập mật khẩu mới an toàn cho tài khoản Bookora của bạn.</p>
                </div>

                <form id="formStep3" onsubmit="handleResetPassword(event)">
                    <div class="form-group">
                        <label for="newPassword" class="form-label">Mật khẩu mới</label>
                        <div class="input-with-icon">
                            <i class="fas fa-lock input-icon"></i>
                            <input type="password" id="newPassword" class="form-input" 
                                   placeholder="Nhập mật khẩu mới..." required oninput="validatePasswordRules()">
                            <button type="button" class="password-toggle" id="toggleNewPwd" title="Hiện/ẩn mật khẩu">
                                <i class="fas fa-eye"></i>
                            </button>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="confirmPassword" class="form-label">Xác nhận mật khẩu mới</label>
                        <div class="input-with-icon">
                            <i class="fas fa-lock input-icon"></i>
                            <input type="password" id="confirmPassword" class="form-input" 
                                   placeholder="Nhập lại mật khẩu mới..." required oninput="validatePasswordRules()">
                            <button type="button" class="password-toggle" id="toggleConfirmPwd" title="Hiện/ẩn mật khẩu">
                                <i class="fas fa-eye"></i>
                            </button>
                        </div>
                    </div>

                    <!-- Danh sách quy tắc mật khẩu trực quan -->
                    <ul class="pwd-rules-list" id="pwdRulesList">
                        <li id="ruleLength" class="invalid"><i class="fas fa-circle-xmark"></i> Tối thiểu 8 ký tự</li>
                        <li id="ruleUpper" class="invalid"><i class="fas fa-circle-xmark"></i> Ít nhất 1 chữ cái in hoa (A-Z)</li>
                        <li id="ruleLower" class="invalid"><i class="fas fa-circle-xmark"></i> Ít nhất 1 chữ cái in thường (a-z)</li>
                        <li id="ruleDigit" class="invalid"><i class="fas fa-circle-xmark"></i> Ít nhất 1 chữ số (0-9)</li>
                        <li id="ruleSpecial" class="invalid"><i class="fas fa-circle-xmark"></i> Ít nhất 1 ký tự đặc biệt (!@#$%...)</li>
                        <li id="ruleNoSpace" class="invalid"><i class="fas fa-circle-xmark"></i> Không chứa khoảng trắng</li>
                        <li id="ruleMatch" class="invalid"><i class="fas fa-circle-xmark"></i> Mật khẩu xác nhận trùng khớp</li>
                    </ul>

                    <button type="submit" class="btn-primary" id="btnResetPassword">
                        <span>Lưu Mật Khẩu Mới</span>
                        <i class="fas fa-check-double"></i>
                    </button>
                </form>

                <p style="text-align: center; margin-top: 24px; font-size: 13.5px; color: var(--text-secondary);">
                    <a href="${pageContext.request.contextPath}/login" class="form-link"><i class="fas fa-arrow-left"></i> Quay lại trang Đăng nhập</a>
                </p>
            </div>
        </section>
    </main>

    <script>
        const ctx = '${pageContext.request.contextPath}';
        // State
        let currentEmail = '';
        let currentResetToken = '';
        let otpTimerInterval = null;
        let otpTimeLeft = 300; // 5 phút
        let resendTimerInterval = null;
        let resendTimeLeft = 60; // 60s cooldown

        function showAlert(msg, type = 'danger') {
            const el = document.getElementById('generalAlert');
            if (!msg) {
                el.style.display = 'none';
                el.innerHTML = '';
                return;
            }
            const icon = type === 'success' ? 'fa-circle-check' : (type === 'info' ? 'fa-circle-info' : 'fa-circle-exclamation');
            el.className = 'alert-dismiss alert-' + type;
            el.innerHTML = `<div><i class="fas ${icon}"></i> <span>${msg}</span></div>
                            <button type="button" onclick="showAlert('')" style="background:none;border:none;cursor:pointer;color:inherit;font-size:14px;">✕</button>`;
            el.style.display = 'flex';
        }

        function goToStep(step) {
            showAlert('');
            for (let i = 1; i <= 3; i++) {
                const indicator = document.getElementById('stepIndicator' + i);
                const panel = document.getElementById('stepPanel' + i);
                if (i < step) {
                    indicator.className = 'fp-step-item completed';
                } else if (i === step) {
                    indicator.className = 'fp-step-item active';
                } else {
                    indicator.className = 'fp-step-item';
                }
                panel.className = 'fp-step-panel' + (i === step ? ' active' : '');
            }

            if (step === 2) {
                const otpInput = document.getElementById('fpOtp');
                if (otpInput) {
                    otpInput.value = '';
                    otpInput.focus();
                }
            } else if (step === 3) {
                const pwdInput = document.getElementById('newPassword');
                if (pwdInput) {
                    pwdInput.value = '';
                    pwdInput.focus();
                }
            }
        }

        // Toggle Password visibility
        function setupToggle(toggleId, inputId) {
            const btn = document.getElementById(toggleId);
            const input = document.getElementById(inputId);
            if (btn && input) {
                btn.addEventListener('click', () => {
                    const isPwd = input.getAttribute('type') === 'password';
                    input.setAttribute('type', isPwd ? 'text' : 'password');
                    btn.innerHTML = isPwd ? '<i class="fas fa-eye-slash"></i>' : '<i class="fas fa-eye"></i>';
                });
            }
        }
        setupToggle('toggleNewPwd', 'newPassword');
        setupToggle('toggleConfirmPwd', 'confirmPassword');

        // Countdown timers
        function startOtpCountdown() {
            clearInterval(otpTimerInterval);
            otpTimeLeft = 300;
            const display = document.getElementById('timerDisplay');
            
            function update() {
                const m = Math.floor(otpTimeLeft / 60);
                const s = otpTimeLeft % 60;
                display.textContent = (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s);
                if (otpTimeLeft <= 0) {
                    clearInterval(otpTimerInterval);
                    display.textContent = 'Hết hạn';
                    showAlert('Mã OTP đã hết hiệu lực (5 phút). Vui lòng nhấn "Gửi lại mã" để nhận mã mới.', 'danger');
                }
                otpTimeLeft--;
            }
            update();
            otpTimerInterval = setInterval(update, 1000);
        }

        function startResendCountdown(seconds = 60) {
            clearInterval(resendTimerInterval);
            resendTimeLeft = seconds;
            const btn = document.getElementById('btnResendOtp');
            btn.disabled = true;

            function update() {
                if (resendTimeLeft <= 0) {
                    clearInterval(resendTimerInterval);
                    btn.disabled = false;
                    btn.textContent = 'Gửi lại mã OTP';
                } else {
                    btn.innerHTML = `Gửi lại sau (<span id="resendCountdown">${resendTimeLeft}</span>s)`;
                    resendTimeLeft--;
                }
            }
            update();
            resendTimerInterval = setInterval(update, 1000);
        }

        // Bước 1: Gửi yêu cầu mã OTP
        async function handleSendOtp(e) {
            e.preventDefault();
            const emailInput = document.getElementById('fpEmail');
            const email = (emailInput.value || '').trim();
            if (!email) {
                showAlert('Vui lòng nhập địa chỉ email của bạn!');
                return;
            }

            const btn = document.getElementById('btnSendOtp');
            btn.disabled = true;
            btn.innerHTML = '<span>Đang gửi mã OTP qua Gmail...</span> <i class="fas fa-spinner fa-spin"></i>';
            showAlert('');

            try {
                const res = await fetch(ctx + '/api/forgot-password/send-otp', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                    body: new URLSearchParams({ email })
                });
                const data = await res.json();

                if (data.success) {
                    currentEmail = email;
                    document.getElementById('maskedEmailDisplay').textContent = email;
                    goToStep(2);
                    showAlert(data.message || 'Mã OTP đã được gửi đến email của bạn!', 'info');
                    startOtpCountdown();
                    startResendCountdown(data.cooldown || 60);
                } else {
                    showAlert(data.message || 'Không thể gửi mã OTP. Vui lòng kiểm tra lại!');
                }
            } catch (err) {
                showAlert('Không thể kết nối đến máy chủ. Vui lòng thử lại sau!');
            } finally {
                btn.disabled = false;
                btn.innerHTML = '<span>Gửi Mã Xác Thực OTP</span> <i class="fas fa-paper-plane"></i>';
            }
        }

        // Gửi lại mã OTP tại bước 2
        async function handleResendOtp() {
            if (!currentEmail) return;
            const btn = document.getElementById('btnResendOtp');
            btn.disabled = true;
            showAlert('');

            try {
                const res = await fetch(ctx + '/api/forgot-password/send-otp', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                    body: new URLSearchParams({ email: currentEmail })
                });
                const data = await res.json();
                if (data.success) {
                    showAlert(data.message || 'Mã OTP mới đã được gửi lại vào email của bạn!', 'info');
                    startOtpCountdown();
                    startResendCountdown(data.cooldown || 60);
                } else {
                    showAlert(data.message || 'Không thể gửi lại mã OTP lúc này!');
                    startResendCountdown(data.cooldown || 10);
                }
            } catch (err) {
                showAlert('Không thể kết nối đến máy chủ!');
                startResendCountdown(10);
            }
        }

        // Bước 2: Xác thực mã OTP
        async function handleVerifyOtp(e) {
            e.preventDefault();
            const otpInput = document.getElementById('fpOtp');
            const otp = (otpInput.value || '').trim();

            if (!otp || otp.length !== 6) {
                showAlert('Vui lòng nhập đúng mã OTP gồm 6 chữ số!');
                otpInput.focus();
                return;
            }

            const btn = document.getElementById('btnVerifyOtp');
            btn.disabled = true;
            btn.innerHTML = '<span>Đang xác thực OTP...</span> <i class="fas fa-spinner fa-spin"></i>';
            showAlert('');

            try {
                const res = await fetch(ctx + '/api/forgot-password/verify-otp', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                    body: new URLSearchParams({ email: currentEmail, otp })
                });
                const data = await res.json();

                if (data.success && data.resetToken) {
                    currentResetToken = data.resetToken;
                    clearInterval(otpTimerInterval);
                    clearInterval(resendTimerInterval);
                    goToStep(3);
                    showAlert(data.message || 'Xác thực OTP thành công! Hãy nhập mật khẩu mới.', 'success');
                } else {
                    showAlert(data.message || 'Mã OTP không chính xác hoặc đã hết hạn!');
                    otpInput.select();
                }
            } catch (err) {
                showAlert('Có lỗi khi kết nối máy chủ!');
            } finally {
                btn.disabled = false;
                btn.innerHTML = '<span>Xác Nhận Mã OTP</span> <i class="fas fa-arrow-right"></i>';
            }
        }

        // Kiểm tra trực quan các quy tắc mật khẩu
        function validatePasswordRules() {
            const pwd = document.getElementById('newPassword').value || '';
            const confirm = document.getElementById('confirmPassword').value || '';

            const hasLength = pwd.length >= 8;
            const hasUpper = /[A-Z]/.test(pwd);
            const hasLower = /[a-z]/.test(pwd);
            const hasDigit = /[0-9]/.test(pwd);
            const hasSpecial = /[^A-Za-z0-9\s]/.test(pwd);
            const hasNoSpace = pwd.length > 0 && !/\s/.test(pwd);
            const isMatch = pwd.length > 0 && confirm.length > 0 && pwd === confirm;

            function setRuleState(id, ok) {
                const el = document.getElementById(id);
                if (el) {
                    el.className = ok ? 'valid' : 'invalid';
                    el.querySelector('i').className = ok ? 'fas fa-circle-check' : 'fas fa-circle-xmark';
                }
            }

            setRuleState('ruleLength', hasLength);
            setRuleState('ruleUpper', hasUpper);
            setRuleState('ruleLower', hasLower);
            setRuleState('ruleDigit', hasDigit);
            setRuleState('ruleSpecial', hasSpecial);
            setRuleState('ruleNoSpace', hasNoSpace);
            setRuleState('ruleMatch', isMatch);

            return hasLength && hasUpper && hasLower && hasDigit && hasSpecial && hasNoSpace && isMatch;
        }

        // Bước 3: Đặt lại mật khẩu mới
        async function handleResetPassword(e) {
            e.preventDefault();
            const newPassword = document.getElementById('newPassword').value || '';
            const confirmPassword = document.getElementById('confirmPassword').value || '';

            if (!newPassword || !confirmPassword) {
                showAlert('Vui lòng nhập mật khẩu mới và xác nhận mật khẩu!');
                return;
            }

            if (!validatePasswordRules()) {
                showAlert('Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.');
                return;
            }

            const btn = document.getElementById('btnResetPassword');
            btn.disabled = true;
            btn.innerHTML = '<span>Đang cập nhật mật khẩu...</span> <i class="fas fa-spinner fa-spin"></i>';
            showAlert('');

            try {
                const res = await fetch(ctx + '/api/forgot-password/reset-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                    body: new URLSearchParams({
                        email: currentEmail,
                        resetToken: currentResetToken,
                        newPassword,
                        confirmPassword
                    })
                });
                const data = await res.json();

                if (data.success) {
                    showAlert('Đặt lại mật khẩu thành công! Đang chuyển về trang Đăng nhập...', 'success');
                    setTimeout(() => {
                        window.location.href = ctx + '/login?message=reset_pwd_success';
                    }, 1200);
                } else {
                    showAlert(data.message || 'Không thể đặt lại mật khẩu. Vui lòng thử lại!');
                    btn.disabled = false;
                    btn.innerHTML = '<span>Lưu Mật Khẩu Mới</span> <i class="fas fa-check-double"></i>';
                }
            } catch (err) {
                showAlert('Lỗi kết nối máy chủ!');
                btn.disabled = false;
                btn.innerHTML = '<span>Lưu Mật Khẩu Mới</span> <i class="fas fa-check-double"></i>';
            }
        }
    </script>
</body>
</html>
