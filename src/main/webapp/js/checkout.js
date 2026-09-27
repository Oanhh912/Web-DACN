/**
 * Bookora Checkout & Payment Logic - JS
 * Xử lý toàn bộ luồng nghiệp vụ Đặt hàng & Thanh toán
 */

let checkoutState = {
    user: null,
    addresses: [],
    selectedAddressId: null,
    paymentMethod: 'COD',
    appliedVoucher: null,
    discountAmount: 0,
    shippingFee: 30000,
    subtotal: 0,
    grandTotal: 0,
    cartItems: [],
    isSubmitting: false,
    onlineTimerInterval: null
};

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Lấy dữ liệu giỏ hàng từ LocalStorage
    loadCartForCheckout();

    // 2. Tải thông tin tài khoản, danh sách địa chỉ, voucher từ Backend
    await loadCheckoutInfo();

    // 3. Khởi tạo ghi chú đơn hàng từ localStorage nếu có
    const savedNote = localStorage.getItem('bookstore_order_note');
    if (savedNote && document.getElementById('orderNoteInput')) {
        document.getElementById('orderNoteInput').value = savedNote;
    }
});

/**
 * Tải sản phẩm từ giỏ hàng để phục vụ đặt hàng
 */
function loadCartForCheckout() {
    try {
        const raw = localStorage.getItem('bookstore_cart');
        checkoutState.cartItems = raw ? JSON.parse(raw) : [];
    } catch (e) {
        checkoutState.cartItems = [];
    }

    if (!checkoutState.cartItems || checkoutState.cartItems.length === 0) {
        renderEmptyCartNotice();
        return;
    }

    renderSummaryItems();
    calculateTotals();
}

/**
 * Tải thông tin người dùng, địa chỉ và phương thức thanh toán từ Backend
 */
async function loadCheckoutInfo() {
    try {
        const res = await fetch('/api/checkout/info');
        if (res.status === 401) {
            window.location.href = '/login?redirect=/checkout&require_login=true';
            return;
        }

        const data = await res.json();
        if (data && data.success) {
            checkoutState.user = data.user;
            checkoutState.addresses = data.addresses || [];
            renderAddressList();

            // Nếu tài khoản chưa có địa chỉ và số điện thoại nhận hàng, hiện thông báo và mở form nhập
            if (!checkoutState.addresses || checkoutState.addresses.length === 0) {
                showCheckoutAlert('<strong><i class="fas fa-circle-info"></i> Bạn chưa có thông tin nhận hàng!</strong> Vui lòng cung cấp số điện thoại và địa chỉ nhận hàng để hoàn tất đơn hàng.', 'warning');
                setTimeout(() => {
                    openAddAddressModal();
                }, 300);
            }
        } else {
            showCheckoutAlert(data.message || 'Không thể tải thông tin đặt hàng.', 'danger');
        }
    } catch (e) {
        console.error('Lỗi tải thông tin checkout:', e);
        showCheckoutAlert('Lỗi kết nối tới máy chủ khi tải thông tin tài khoản!', 'danger');
    }
}

/**
 * Hiển thị danh sách địa chỉ (Bảng DIA_CHI)
 * Chỉ hiển thị địa chỉ thuộc tài khoản đang đăng nhập
 */
function renderAddressList() {
    const container = document.getElementById('addressListContainer');
    if (!container) return;

    if (!checkoutState.addresses || checkoutState.addresses.length === 0) {
        container.innerHTML = `
            <div class="address-empty-box">
                <i class="fas fa-location-crosshairs"></i>
                <p>Bạn chưa lưu địa chỉ nhận hàng và số điện thoại nào cho tài khoản này.</p>
                <button type="button" class="btn-add-first-address" onclick="openAddAddressModal()">
                    <i class="fas fa-plus"></i> Thêm địa chỉ nhận hàng ngay
                </button>
            </div>
        `;
        checkoutState.selectedAddressId = null;
        return;
    }

    // Tìm địa chỉ mặc định hoặc địa chỉ đầu tiên
    let defaultAddr = checkoutState.addresses.find(a => a.isDefault);
    if (!defaultAddr && checkoutState.addresses.length > 0) {
        defaultAddr = checkoutState.addresses[0];
    }
    checkoutState.selectedAddressId = defaultAddr ? defaultAddr.id : null;

    container.innerHTML = checkoutState.addresses.map(addr => {
        const isChecked = addr.id === checkoutState.selectedAddressId ? 'checked' : '';
        const activeClass = addr.id === checkoutState.selectedAddressId ? 'active' : '';
        const defaultBadge = addr.isDefault ? '<span class="badge-default-addr">Mặc định</span>' : '';

        return `
            <label class="address-card-item ${activeClass}" id="addrCard_${addr.id}">
                <input type="radio" name="selectedAddress" value="${addr.id}" ${isChecked} onchange="selectAddress(${addr.id})">
                <div class="addr-card-content">
                    <div class="addr-header-row">
                        <strong class="addr-recipient-name">${escapeHtml(addr.recipientName)}</strong>
                        <span class="addr-phone"><i class="fas fa-phone"></i> ${escapeHtml(addr.phone)}</span>
                        ${defaultBadge}
                    </div>
                    <div class="addr-full-text">
                        <i class="fas fa-map-pin"></i> ${escapeHtml(addr.fullAddress)}
                    </div>
                </div>
                <div class="addr-check-indicator"><i class="fas fa-circle-check"></i></div>
            </label>
        `;
    }).join('');
}

/**
 * Chọn địa chỉ nhận hàng
 */
function selectAddress(addressId) {
    checkoutState.selectedAddressId = addressId;
    document.querySelectorAll('.address-card-item').forEach(el => el.classList.remove('active'));
    const target = document.getElementById(`addrCard_${addressId}`);
    if (target) target.classList.add('active');
}

/**
 * Hiển thị tóm tắt sản phẩm trong giỏ hàng
 */
function renderSummaryItems() {
    const container = document.getElementById('summaryItemsList');
    const countEl = document.getElementById('summaryItemCount');
    if (!container) return;

    const totalQty = checkoutState.cartItems.reduce((sum, it) => sum + (it.quantity || 1), 0);
    if (countEl) countEl.textContent = totalQty;

    container.innerHTML = checkoutState.cartItems.map(item => {
        const itemPrice = item.price || 0;
        const itemSubtotal = itemPrice * (item.quantity || 1);
        return `
            <div class="summary-item-row">
                <img src="${item.image || 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=100'}" class="summary-item-thumb" alt="${escapeHtml(item.title)}">
                <div class="summary-item-info">
                    <h5 class="summary-item-title" title="${escapeHtml(item.title)}">${escapeHtml(item.title)}</h5>
                    <div class="summary-item-meta">
                        <span class="summary-item-price">${formatVNCurrency(itemPrice)}</span>
                        <span class="summary-item-qty">x ${item.quantity}</span>
                    </div>
                </div>
                <div class="summary-item-total">${formatVNCurrency(itemSubtotal)}</div>
            </div>
        `;
    }).join('');
}

/**
 * Tính toán tổng tiền theo quy tắc nghiệp vụ
 * Subtotal, Phí vận chuyển (Freeship từ 250k), Giảm giá voucher, Tổng thanh toán
 */
function calculateTotals() {
    const subtotal = checkoutState.cartItems.reduce((sum, it) => sum + ((it.price || 0) * (it.quantity || 1)), 0);
    checkoutState.subtotal = subtotal;

    // Phí vận chuyển: Miễn phí nếu >= 250.000đ, hoặc 30.000đ
    if (subtotal >= 250000 || (checkoutState.appliedVoucher && checkoutState.appliedVoucher.code === 'FREESHIP')) {
        checkoutState.shippingFee = 0;
    } else {
        checkoutState.shippingFee = 30000;
    }

    // Giảm giá voucher
    let discount = 0;
    if (checkoutState.appliedVoucher) {
        discount = checkoutState.discountAmount || 0;
    }

    checkoutState.grandTotal = Math.max(0, subtotal - discount + checkoutState.shippingFee);

    // Cập nhật DOM
    const subtotalEl = document.getElementById('priceSubtotal');
    const discountRow = document.getElementById('priceDiscountRow');
    const discountEl = document.getElementById('priceDiscount');
    const shippingEl = document.getElementById('priceShipping');
    const grandTotalEl = document.getElementById('priceGrandTotal');

    if (subtotalEl) subtotalEl.textContent = formatVNCurrency(subtotal);
    if (discountRow) {
        if (discount > 0) {
            discountRow.style.display = 'flex';
            discountEl.textContent = '-' + formatVNCurrency(discount);
        } else {
            discountRow.style.display = 'none';
        }
    }
    if (shippingEl) {
        shippingEl.textContent = checkoutState.shippingFee === 0 ? 'Miễn phí (Freeship)' : formatVNCurrency(checkoutState.shippingFee);
    }
    if (grandTotalEl) grandTotalEl.textContent = formatVNCurrency(checkoutState.grandTotal);
}

/**
 * Đổi phương thức thanh toán
 */
function onPaymentMethodChange(method) {
    checkoutState.paymentMethod = method;
    document.querySelectorAll('.payment-method-card').forEach(c => c.classList.remove('active'));
    if (method === 'COD') {
        const c = document.getElementById('pmCardCod');
        if (c) c.classList.add('active');
    } else {
        const c = document.getElementById('pmCardOnline');
        if (c) c.classList.add('active');
    }
}

/**
 * Gợi ý điền nhanh mã voucher từ chip
 */
function quickFillVoucher(code) {
    const input = document.getElementById('voucherInput');
    if (input) {
        input.value = code;
        applyVoucherCode();
    }
}

/**
 * BƯỚC 4: Kiểm tra và áp dụng mã giảm giá (Gửi lên Backend để kiểm tra và tự tính tiền)
 */
async function applyVoucherCode() {
    const input = document.getElementById('voucherInput');
    const errBox = document.getElementById('voucherErrorMsg');
    const btn = document.getElementById('btnApplyVoucher');
    if (!input) return;

    const code = input.value.trim().toUpperCase();
    if (!code) {
        showVoucherError('Vui lòng nhập mã giảm giá!');
        return;
    }

    if (checkoutState.cartItems.length === 0) {
        showVoucherError('Đơn hàng chưa có sản phẩm nào!');
        return;
    }

    if (btn) btn.disabled = true;
    hideVoucherError();

    try {
        const payload = {
            code: code,
            items: checkoutState.cartItems.map(it => ({ bookId: it.id, quantity: it.quantity }))
        };

        const res = await fetch('/api/checkout/validate-voucher', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (data && data.success) {
            checkoutState.appliedVoucher = {
                code: data.code,
                title: data.title
            };
            checkoutState.discountAmount = data.discountAmount || 0;
            
            // Hiển thị banner voucher đã áp dụng
            const banner = document.getElementById('appliedVoucherBanner');
            const avCode = document.getElementById('avCode');
            const avTitle = document.getElementById('avTitle');
            if (banner) {
                banner.style.display = 'flex';
                if (avCode) avCode.textContent = data.code;
                if (avTitle) avTitle.textContent = 'Đã giảm ' + formatVNCurrency(data.discountAmount);
            }
            input.value = '';
            calculateTotals();
            showToast('Đã áp dụng mã giảm giá ' + data.code + ' thành công!');
        } else {
            showVoucherError(data.message || 'Mã giảm giá không hợp lệ hoặc không đủ điều kiện.');
            checkoutState.appliedVoucher = null;
            checkoutState.discountAmount = 0;
            calculateTotals();
        }
    } catch (e) {
        console.error('Lỗi kiểm tra voucher:', e);
        showVoucherError('Không thể kết nối máy chủ để kiểm tra mã giảm giá!');
    } finally {
        if (btn) btn.disabled = false;
    }
}

/**
 * Hủy bỏ mã voucher đã áp dụng
 */
function removeVoucherCode() {
    checkoutState.appliedVoucher = null;
    checkoutState.discountAmount = 0;
    const banner = document.getElementById('appliedVoucherBanner');
    if (banner) banner.style.display = 'none';
    hideVoucherError();
    calculateTotals();
    showToast('Đã hủy áp dụng mã giảm giá.');
}

function showVoucherError(msg) {
    const errBox = document.getElementById('voucherErrorMsg');
    if (errBox) {
        errBox.textContent = msg;
        errBox.style.display = 'block';
    }
}

function hideVoucherError() {
    const errBox = document.getElementById('voucherErrorMsg');
    if (errBox) {
        errBox.style.display = 'none';
    }
}

/**
 * BƯỚC 5 & 6: XỬ LÝ ĐẶT HÀNG CHÍNH (HANDLE PLACE ORDER)
 * Bắt buộc kiểm tra: Đăng nhập, Tồn kho, Địa chỉ thuộc tài khoản, Chống click nhiều lần
 */
async function handlePlaceOrder() {
    if (checkoutState.isSubmitting) return;

    hideCheckoutAlert();

    // 1. Kiểm tra giỏ hàng
    if (!checkoutState.cartItems || checkoutState.cartItems.length === 0) {
        showCheckoutAlert('Giỏ hàng trống! Vui lòng chọn ít nhất một cuốn sách.', 'warning');
        return;
    }

    // 2. Kiểm tra địa chỉ nhận hàng
    if (!checkoutState.selectedAddressId) {
        showCheckoutAlert('Vui lòng chọn hoặc thêm địa chỉ nhận hàng!', 'warning');
        openAddAddressModal();
        return;
    }

    const noteEl = document.getElementById('orderNoteInput');
    const note = noteEl ? noteEl.value.trim() : '';

    // Khóa nút để chống double-click (Chống tạo đơn trùng)
    const btnPlace = document.getElementById('btnPlaceOrder');
    setButtonLoading(btnPlace, true);
    checkoutState.isSubmitting = true;

    // Sinh request ID duy nhất
    const clientRequestId = 'req_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8);

    const orderPayload = {
        clientRequestId: clientRequestId,
        addressId: checkoutState.selectedAddressId,
        paymentMethod: checkoutState.paymentMethod,
        voucherCode: checkoutState.appliedVoucher ? checkoutState.appliedVoucher.code : '',
        note: note,
        items: checkoutState.cartItems.map(it => ({ bookId: it.id, quantity: it.quantity }))
    };

    try {
        const res = await fetch('/api/checkout/place-order', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orderPayload)
        });

        const data = await res.json();

        // 1. Kiểm tra chưa đăng nhập
        if (res.status === 401 || (data && data.needLogin)) {
            showToast('Vui lòng đăng nhập để hoàn tất đặt hàng!');
            setTimeout(() => {
                window.location.href = '/login?redirect=/checkout&require_login=true';
            }, 1000);
            return;
        }

        // 2. Kiểm tra không đủ tồn kho (KHO)
        if (data && data.stockError) {
            showCheckoutAlert(`⚠️ ${data.message}`, 'danger');
            showStockWarningModal(data.message);
            setButtonLoading(btnPlace, false);
            checkoutState.isSubmitting = false;
            return;
        }

        // 3. Nếu là phương thức THANH TOÁN TRỰC TUYẾN (ONLINE) -> Mở modal cổng thanh toán mô phỏng
        if (data && data.needsOnlineModal) {
            setButtonLoading(btnPlace, false);
            checkoutState.isSubmitting = false;
            openOnlinePaymentModal(data);
            return;
        }

        // 4. Nếu là phương thức COD thành công
        if (data && data.success) {
            // Xóa giỏ hàng trên trình duyệt
            localStorage.setItem('bookstore_cart', '[]');
            localStorage.removeItem('bookstore_order_note');
            if (typeof updateCartUI === 'function') updateCartUI();

            showToast('🎉 Đặt hàng thành công! Đang chuyển hướng...');
            setTimeout(() => {
                window.location.href = data.redirectUrl || `/order-success?code=${encodeURIComponent(data.orderCode)}`;
            }, 800);
            return;
        }

        // Trường hợp lỗi khác từ server (Địa chỉ không hợp lệ, voucher hết hạn...)
        showCheckoutAlert(`❌ ${data.message || 'Đặt hàng thất bại. Vui lòng thử lại!'}`, 'danger');

    } catch (e) {
        console.error('Lỗi khi gửi yêu cầu đặt hàng:', e);
        showCheckoutAlert('Lỗi mạng hoặc máy chủ không phản hồi. Vui lòng kiểm tra lại kết nối!', 'danger');
    } finally {
        if (!checkoutState.paymentMethod || checkoutState.paymentMethod === 'COD') {
            setButtonLoading(btnPlace, false);
            checkoutState.isSubmitting = false;
        }
    }
}

/**
 * Mở modal thanh toán trực tuyến mô phỏng (VietQR / MoMo / Thẻ ATM)
 */
function openOnlinePaymentModal(data) {
    const modal = document.getElementById('onlinePaymentModal');
    const backdrop = document.getElementById('onlineModalBackdrop');
    if (!modal) return;

    const amount = data.totalAmount || checkoutState.grandTotal;
    const memo = 'BK' + Date.now().toString().slice(-6);

    document.getElementById('onlineModalAmount').textContent = formatVNCurrency(amount);
    document.getElementById('onlineTransferMemo').textContent = memo;

    // Sinh mã VietQR động
    const qrUrl = `https://api.vietqr.io/image/970422-0912345678-compact2.png?amount=${Math.round(amount)}&addInfo=${encodeURIComponent(memo)}&accountName=${encodeURIComponent('BOOKORA STORE')}`;
    const qrImg = document.getElementById('onlineQrImage');
    if (qrImg) qrImg.src = qrUrl;

    modal.style.display = 'block';
    if (backdrop) backdrop.style.display = 'block';

    // Đếm ngược 15 phút
    startOnlineCountdown(15 * 60);
}

function closeOnlinePaymentModal() {
    const modal = document.getElementById('onlinePaymentModal');
    const backdrop = document.getElementById('onlineModalBackdrop');
    if (modal) modal.style.display = 'none';
    if (backdrop) backdrop.style.display = 'none';
    if (checkoutState.onlineTimerInterval) clearInterval(checkoutState.onlineTimerInterval);
}

function startOnlineCountdown(seconds) {
    if (checkoutState.onlineTimerInterval) clearInterval(checkoutState.onlineTimerInterval);
    let remaining = seconds;
    const timerEl = document.getElementById('onlineCountdownTimer');

    function updateDisplay() {
        const m = Math.floor(remaining / 60);
        const s = remaining % 60;
        if (timerEl) timerEl.textContent = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
        if (remaining <= 0) {
            clearInterval(checkoutState.onlineTimerInterval);
            simulatePaymentResult('FAILED');
        }
        remaining--;
    }

    updateDisplay();
    checkoutState.onlineTimerInterval = setInterval(updateDisplay, 1000);
}

/**
 * BƯỚC 6 & MỤC 3, 4: Mô phỏng kết quả thanh toán trực tuyến
 * - SUCCESS: Trừ kho, tạo đơn, trạng thái PAID, xóa giỏ hàng
 * - FAILED: KHÔNG TRỪ KHO, thông báo "Thanh toán thất bại", cho phép thanh toán lại hoặc chọn COD
 */
async function simulatePaymentResult(action) {
    const btnSuccess = document.getElementById('btnSimSuccess');
    const btnFail = document.getElementById('btnSimFailure');
    if (btnSuccess) btnSuccess.disabled = true;
    if (btnFail) btnFail.disabled = true;

    const payload = {
        action: action, // 'SUCCESS' hoặc 'FAILED'
        addressId: checkoutState.selectedAddressId,
        voucherCode: checkoutState.appliedVoucher ? checkoutState.appliedVoucher.code : '',
        note: document.getElementById('orderNoteInput') ? document.getElementById('orderNoteInput').value.trim() : '',
        items: checkoutState.cartItems.map(it => ({ bookId: it.id, quantity: it.quantity }))
    };

    try {
        const res = await fetch('/api/payment/confirm-online', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        closeOnlinePaymentModal();

        // 1. Trường hợp THANH TOÁN THẤT BẠI
        if (action === 'FAILED' || (data && data.paymentFailed)) {
            showCheckoutAlert(
                `<strong><i class="fas fa-triangle-exclamation"></i> Thanh toán thất bại!</strong> Giao dịch thanh toán trực tuyến không thành công hoặc do bạn hủy bỏ. Kho hàng chưa bị trừ. Bạn có thể bấm nút <strong>Thử thanh toán lại</strong> hoặc chuyển sang <strong>Thanh toán khi nhận hàng (COD)</strong>.`,
                'danger',
                true // Có nút chuyển sang COD
            );
            return;
        }

        // 2. Trường hợp KHÔNG ĐỦ TỒN KHO khi xác nhận
        if (data && data.stockError) {
            showCheckoutAlert(`⚠️ ${data.message}`, 'danger');
            showStockWarningModal(data.message);
            return;
        }

        // 3. Trường hợp THANH TOÁN THÀNH CÔNG
        if (data && data.success) {
            localStorage.setItem('bookstore_cart', '[]');
            localStorage.removeItem('bookstore_order_note');
            if (typeof updateCartUI === 'function') updateCartUI();

            showToast('🎉 Thanh toán trực tuyến thành công! Đang chuyển hướng...');
            setTimeout(() => {
                window.location.href = data.redirectUrl || `/order-success?code=${encodeURIComponent(data.orderCode)}`;
            }, 800);
            return;
        }

        showCheckoutAlert(`❌ ${data.message || 'Giao dịch gặp lỗi.'}`, 'danger');

    } catch (e) {
        console.error('Lỗi xác nhận thanh toán trực tuyến:', e);
        showCheckoutAlert('Lỗi mạng khi kết nối tới cổng thanh toán!', 'danger');
    } finally {
        if (btnSuccess) btnSuccess.disabled = false;
        if (btnFail) btnFail.disabled = false;
    }
}

/**
 * Hiển thị cảnh báo lỗi trên giao diện Đặt hàng
 */
function showCheckoutAlert(msg, type = 'danger', showSwitchToCod = false) {
    const banner = document.getElementById('checkoutAlertBanner');
    if (!banner) return;

    let switchBtnHtml = '';
    if (showSwitchToCod) {
        switchBtnHtml = `
            <div style="margin-top: 10px; display: flex; gap: 10px;">
                <button type="button" class="btn-alert-switch-cod" onclick="switchToCodAndOrder()">
                    <i class="fas fa-hand-holding-dollar"></i> Chuyển sang thanh toán khi nhận hàng (COD)
                </button>
                <button type="button" class="btn-alert-retry-online" onclick="handlePlaceOrder()">
                    <i class="fas fa-rotate"></i> Thử thanh toán online lại
                </button>
            </div>
        `;
    }

    banner.className = `checkout-alert-box alert-${type}`;
    banner.innerHTML = `
        <div class="alert-content">
            <div class="alert-msg">${msg}</div>
            ${switchBtnHtml}
        </div>
        <button type="button" class="btn-close-alert" onclick="hideCheckoutAlert()">&times;</button>
    `;
    banner.style.display = 'flex';
    banner.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function hideCheckoutAlert() {
    const banner = document.getElementById('checkoutAlertBanner');
    if (banner) banner.style.display = 'none';
}

function switchToCodAndOrder() {
    onPaymentMethodChange('COD');
    hideCheckoutAlert();
    handlePlaceOrder();
}

/**
 * Modal thêm địa chỉ nhận hàng
 */
function openAddAddressModal() {
    const modal = document.getElementById('addAddressModal');
    const backdrop = document.getElementById('addAddressModalBackdrop');
    if (modal) {
        modal.style.display = 'block';
        // Tự động điền trước thông tin người dùng nếu các trường còn trống
        const nameInput = document.getElementById('newAddrName');
        const phoneInput = document.getElementById('newAddrPhone');
        if (nameInput && !nameInput.value && checkoutState.user && checkoutState.user.fullName) {
            nameInput.value = checkoutState.user.fullName;
        }
        if (phoneInput && !phoneInput.value && checkoutState.user && checkoutState.user.phone) {
            phoneInput.value = checkoutState.user.phone;
        }
    }
    if (backdrop) backdrop.style.display = 'block';
}

function closeAddAddressModal() {
    const modal = document.getElementById('addAddressModal');
    const backdrop = document.getElementById('addAddressModalBackdrop');
    if (modal) modal.style.display = 'none';
    if (backdrop) backdrop.style.display = 'none';
}

async function handleSaveNewAddress(e) {
    e.preventDefault();
    const btn = document.getElementById('btnSaveAddress');
    if (btn) btn.disabled = true;

    const payload = {
        recipientName: document.getElementById('newAddrName').value.trim(),
        phone: document.getElementById('newAddrPhone').value.trim(),
        province: document.getElementById('newAddrProvince').value.trim(),
        district: document.getElementById('newAddrDistrict').value.trim(),
        ward: document.getElementById('newAddrWard').value.trim(),
        addressDetail: document.getElementById('newAddrDetail').value.trim(),
        isDefault: document.getElementById('newAddrIsDefault').checked
    };

    if (!payload.recipientName || !payload.phone || !payload.province || !payload.addressDetail) {
        alert('Vui lòng điền đầy đủ họ tên, số điện thoại, tỉnh thành và địa chỉ chi tiết!');
        if (btn) btn.disabled = false;
        return;
    }

    const cleanPhone = payload.phone.replace(/[\s.-]/g, '');
    if (!/^[0-9]{9,11}$/.test(cleanPhone)) {
        alert('Số điện thoại không hợp lệ! Vui lòng nhập từ 9 đến 11 chữ số.');
        if (btn) btn.disabled = false;
        return;
    }

    try {
        const res = await fetch('/api/checkout/address', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (data && data.success && data.address) {
            showToast('Đã thêm địa chỉ nhận hàng thành công!');
            closeAddAddressModal();
            // Tải lại danh sách địa chỉ và chọn địa chỉ mới
            await loadCheckoutInfo();
            selectAddress(data.address.id);
        } else {
            alert(data.message || 'Không thể lưu địa chỉ.');
        }
    } catch (err) {
        console.error('Lỗi lưu địa chỉ:', err);
        alert('Lỗi kết nối khi lưu địa chỉ nhận hàng!');
    } finally {
        if (btn) btn.disabled = false;
    }
}

/**
 * Modal hiển thị thông báo không đủ tồn kho
 */
function showStockWarningModal(message) {
    if (typeof showOutOfStockAlertModal === 'function') {
        showOutOfStockAlertModal(message);
        return;
    }
    alert(`⚠️ THÔNG BÁO TỒN KHO:\n${message}\n\nVui lòng vào lại giỏ hàng để điều chỉnh số lượng!`);
}

/**
 * Hiển thị thông báo khi giỏ hàng trống
 */
function renderEmptyCartNotice() {
    const container = document.querySelector('.checkout-container');
    if (container) {
        container.innerHTML = `
            <div class="checkout-empty-cart-card">
                <div class="empty-icon"><i class="fas fa-bag-shopping"></i></div>
                <h3>Giỏ hàng của bạn đang trống!</h3>
                <p>Hãy dạo một vòng tiệm sách Bookora và chọn cho mình những cuốn sách ưng ý nhất trước khi đặt hàng nhé.</p>
                <div style="margin-top: 20px;">
                    <a href="home" class="btn-cart-page-checkout" style="display: inline-block; width: auto; padding: 12px 30px; text-decoration: none;">
                        <i class="fas fa-reply"></i> Khám phá sách ngay
                    </a>
                </div>
            </div>
        `;
    }
}

function setButtonLoading(btn, isLoading) {
    if (!btn) return;
    btn.disabled = isLoading;
    const txt = btn.querySelector('.btn-text');
    const sp = btn.querySelector('.btn-spinner');
    if (txt) txt.style.display = isLoading ? 'none' : 'inline-block';
    if (sp) sp.style.display = isLoading ? 'inline-block' : 'none';
}

function formatVNCurrency(val) {
    return new Intl.NumberFormat('vi-VN').format(Math.round(val || 0)) + ' đ';
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showToast('Đã sao chép: ' + text);
    });
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
