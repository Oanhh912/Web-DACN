/**
 * Bookstore Interactive JavaScript Application
 */

// Giỏ hàng lưu trong LocalStorage
let cart = JSON.parse(localStorage.getItem('bookstore_cart') || '[]');

document.addEventListener('DOMContentLoaded', () => {
    initPasswordToggle();
    initQuickLoginPills();
    initUserDropdown();
    initCategoryPills();
    initSearchFilter();
    updateCartUI();
});

/* ==========================================================================
   1. LOGIN PAGE SCRIPTS
   ========================================================================== */
function initPasswordToggle() {
    const toggleBtn = document.getElementById('togglePassword');
    const passwordInput = document.getElementById('password');
    if (!toggleBtn || !passwordInput) return;

    toggleBtn.addEventListener('click', () => {
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        toggleBtn.innerHTML = type === 'password' 
            ? '<i class="fas fa-eye"></i>' 
            : '<i class="fas fa-eye-slash"></i>';
    });
}

function initQuickLoginPills() {
    const pills = document.querySelectorAll('.quick-pill');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');

    pills.forEach(pill => {
        pill.addEventListener('click', () => {
            const user = pill.getAttribute('data-user');
            const pass = pill.getAttribute('data-pass');
            if (usernameInput) usernameInput.value = user;
            if (passwordInput) passwordInput.value = pass;

            // Hiệu ứng nháy nhẹ ô input
            if (usernameInput) {
                usernameInput.focus();
                usernameInput.style.borderColor = '#4338ca';
                setTimeout(() => usernameInput.style.borderColor = '', 500);
            }
        });
    });
}

/* ==========================================================================
   2. USER DROPDOWN & LOGOUT
   ========================================================================== */
function initUserDropdown() {
    const trigger = document.getElementById('userMenuTrigger');
    const dropdown = document.getElementById('userMenuDropdown');
    if (!trigger || !dropdown) return;

    trigger.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('show');
    });

    document.addEventListener('click', (e) => {
        if (!dropdown.contains(e.target) && !trigger.contains(e.target)) {
            dropdown.classList.remove('show');
        }
    });
}

/* ==========================================================================
   3. CATEGORY & SEARCH FILTERING
   ========================================================================== */
function initCategoryPills() {
    const pills = document.querySelectorAll('.category-pill');
    pills.forEach(pill => {
        pill.addEventListener('click', () => {
            pills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');

            const category = pill.getAttribute('data-category');
            filterBooksClientSide(category);
        });
    });
}

function initSearchFilter() {
    const searchInput = document.getElementById('searchInput');
    if (!searchInput) return;

    let debounceTimer;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            const keyword = e.target.value.trim().toLowerCase();
            const activeCategory = document.querySelector('.category-pill.active')?.getAttribute('data-category') || 'Tất cả';
            filterBooksClientSide(activeCategory, keyword);
        }, 200);
    });
}

function filterBooksClientSide(category, keyword = '') {
    const cards = document.querySelectorAll('.book-card');
    const searchInput = document.getElementById('searchInput');
    if (keyword === '' && searchInput) {
        keyword = searchInput.value.trim().toLowerCase();
    }

    let visibleCount = 0;
    cards.forEach(card => {
        const title = (card.getAttribute('data-title') || '').toLowerCase();
        const author = (card.getAttribute('data-author') || '').toLowerCase();
        const code = (card.getAttribute('data-code') || '').toLowerCase();
        const cardCat = card.getAttribute('data-category') || '';

        const matchCat = (category === 'Tất cả' || cardCat === category);
        const matchKw = (!keyword || title.includes(keyword) || author.includes(keyword) || code.includes(keyword));

        if (matchCat && matchKw) {
            card.style.display = 'flex';
            visibleCount++;
        } else {
            card.style.display = 'none';
        }
    });

    const countElem = document.getElementById('bookCountDisplay');
    if (countElem) {
        countElem.textContent = visibleCount;
    }
}

/* ==========================================================================
   4. QUICK VIEW MODAL
   ========================================================================== */
let currentModalBook = null;

function openQuickView(bookId) {
    const card = document.querySelector(`.book-card[data-id="${bookId}"]`);
    if (!card) return;

    currentModalBook = {
        id: bookId,
        code: card.getAttribute('data-code') || '',
        title: card.getAttribute('data-title'),
        author: card.getAttribute('data-author'),
        price: parseFloat(card.getAttribute('data-price')),
        formattedPrice: card.getAttribute('data-formatted-price'),
        category: card.getAttribute('data-category'),
        image: card.getAttribute('data-image'),
        desc: card.getAttribute('data-desc'),
        rating: card.getAttribute('data-rating')
    };

    document.getElementById('modalCover').src = currentModalBook.image;
    document.getElementById('modalTitle').textContent = currentModalBook.title;
    document.getElementById('modalAuthor').textContent = currentModalBook.author;
    document.getElementById('modalCategory').textContent = currentModalBook.category;
    const modalCodeEl = document.getElementById('modalCode');
    if (modalCodeEl) {
        modalCodeEl.innerHTML = `<i class="fas fa-barcode"></i> Mã: ${currentModalBook.code}`;
    }
    document.getElementById('modalPrice').textContent = currentModalBook.formattedPrice;
    document.getElementById('modalDesc').textContent = currentModalBook.desc;
    document.getElementById('modalRating').textContent = currentModalBook.rating;
    document.getElementById('modalQty').value = 1;

    document.getElementById('quickViewModal').classList.add('active');
}

function closeQuickView() {
    const modal = document.getElementById('quickViewModal');
    if (modal) modal.classList.remove('active');
}

function changeModalQty(delta) {
    const input = document.getElementById('modalQty');
    if (!input) return;
    let val = parseInt(input.value) || 1;
    val = Math.max(1, Math.min(99, val + delta));
    input.value = val;
}

function addModalBookToCart() {
    if (!currentModalBook) return;
    const qty = parseInt(document.getElementById('modalQty').value) || 1;
    addToCart(currentModalBook.id, qty);
    closeQuickView();
}

/* ==========================================================================
   5. SHOPPING CART LOGIC
   ========================================================================== */
function addToCart(bookId, quantity = 1) {
    const card = document.querySelector(`.book-card[data-id="${bookId}"]`);
    if (!card) return;

    const code = card.getAttribute('data-code') || '';
    const title = card.getAttribute('data-title');
    const author = card.getAttribute('data-author');
    const price = parseFloat(card.getAttribute('data-price'));
    const formattedPrice = card.getAttribute('data-formatted-price');
    const image = card.getAttribute('data-image');

    const existingIndex = cart.findIndex(item => item.id === bookId);
    if (existingIndex > -1) {
        cart[existingIndex].quantity += quantity;
        if (!cart[existingIndex].code && code) {
            cart[existingIndex].code = code;
        }
    } else {
        cart.push({
            id: bookId,
            code: code,
            title: title,
            author: author,
            price: price,
            formattedPrice: formattedPrice,
            image: image,
            quantity: quantity
        });
    }

    saveCart();
    updateCartUI();
    showToast(`Đã thêm "${title}" vào giỏ hàng!`);

    // Hiệu ứng nảy số badge
    const badge = document.getElementById('cartBadge');
    if (badge) {
        badge.classList.add('bump');
        setTimeout(() => badge.classList.remove('bump'), 300);
    }
}

function saveCart() {
    localStorage.setItem('bookstore_cart', JSON.stringify(cart));
}

function updateCartUI() {
    const badge = document.getElementById('cartBadge');
    const totalCount = cart.reduce((sum, item) => sum + item.quantity, 0);
    if (badge) {
        badge.textContent = totalCount;
        badge.style.display = totalCount > 0 ? 'flex' : 'none';
    }

    renderCartDrawerItems();
}

function toggleCartDrawer() {
    const drawer = document.getElementById('cartDrawer');
    const overlay = document.getElementById('cartOverlay');
    if (!drawer || !overlay) return;

    const isOpen = drawer.classList.contains('open');
    if (isOpen) {
        drawer.classList.remove('open');
        overlay.classList.remove('open');
    } else {
        drawer.classList.add('open');
        overlay.classList.add('open');
        renderCartDrawerItems();
    }
}

function renderCartDrawerItems() {
    const container = document.getElementById('cartItemsList');
    const totalElem = document.getElementById('cartTotalPrice');
    if (!container || !totalElem) return;

    if (cart.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; color: #94a3b8; padding: 40px 0;">
                <i class="fas fa-shopping-bag" style="font-size: 40px; margin-bottom: 12px; color: #cbd5e1;"></i>
                <p>Giỏ hàng của bạn đang trống.</p>
            </div>
        `;
        totalElem.textContent = '0 đ';
        return;
    }

    let total = 0;
    container.innerHTML = cart.map(item => {
        const itemTotal = item.price * item.quantity;
        total += itemTotal;
        const codeHtml = item.code ? `<span class="cart-item-code"><i class="fas fa-barcode"></i> Mã: ${item.code}</span>` : '';
        return `
            <div class="cart-item">
                <img src="${item.image}" alt="${item.title}" class="cart-item-img" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=100';">
                <div class="cart-item-details">
                    <h4 class="cart-item-title">${item.title}</h4>
                    ${codeHtml}
                    <span class="cart-item-price">${formatMoney(item.price)}</span>
                    <div class="cart-item-footer">
                        <div class="cart-item-qty">
                            <button class="btn-qty-mini" onclick="updateItemQuantity(${item.id}, -1)">-</button>
                            <span>${item.quantity}</span>
                            <button class="btn-qty-mini" onclick="updateItemQuantity(${item.id}, 1)">+</button>
                        </div>
                        <button class="btn-remove-item" onclick="removeFromCart(${item.id})" title="Xóa">
                            <i class="fas fa-trash-alt"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    totalElem.textContent = formatMoney(total);
}

function updateItemQuantity(bookId, delta) {
    const item = cart.find(i => i.id === bookId);
    if (!item) return;

    item.quantity += delta;
    if (item.quantity <= 0) {
        cart = cart.filter(i => i.id !== bookId);
    }
    saveCart();
    updateCartUI();
}

function removeFromCart(bookId) {
    cart = cart.filter(i => i.id !== bookId);
    saveCart();
    updateCartUI();
}

function checkoutCart() {
    if (cart.length === 0) {
        showToast('Giỏ hàng trống! Hãy chọn một cuốn sách yêu thích.');
        return;
    }
    alert('🎉 Cảm ơn bạn đã đặt sách! Đơn hàng mô phỏng đã được ghi nhận thành công.');
    cart = [];
    saveCart();
    updateCartUI();
    toggleCartDrawer();
}

function formatMoney(amount) {
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
}

/* ==========================================================================
   6. TOAST NOTIFICATION
   ========================================================================== */
function showToast(message) {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `<i class="fas fa-check-circle"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(50px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}
