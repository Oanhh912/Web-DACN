/**
 * Bookstore Interactive JavaScript Application - Bookora
 * Hoàn thiện đầy đủ Use Case "Xem và tìm kiếm sách" theo chuẩn tài liệu BA
 */

// Giỏ hàng lưu trong LocalStorage
let cart = JSON.parse(localStorage.getItem('bookstore_cart') || '[]');

document.addEventListener('DOMContentLoaded', () => {
    initPasswordToggle();
    initQuickLoginPills();
    initUserDropdown();
    initCategoryPills();
    initLiveSearchAndSuggestions();
    initMultiFilters();
    initModalListeners();
    initChatbotWidget();
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
   3. LIVE SEARCH & GỢI Ý TÌM KIẾM (LUỒNG CƠ BẢN 1: SACH, DANH_MUC, KHUYEN_MAI)
   ========================================================================== */
function initLiveSearchAndSuggestions() {
    const searchInput = document.getElementById('searchInput');
    const suggestionsDropdown = document.getElementById('searchSuggestionsDropdown');
    if (!searchInput) return;

    let debounceTimer;

    searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        const query = e.target.value.trim();

        if (query.length < 1) {
            if (suggestionsDropdown) suggestionsDropdown.style.display = 'none';
            onFilterChange();
            return;
        }

        debounceTimer = setTimeout(() => {
            fetchSearchSuggestions(query);
            onFilterChange();
        }, 220);
    });

    // Ẩn dropdown khi click ra ngoài
    document.addEventListener('click', (e) => {
        if (suggestionsDropdown && !searchInput.contains(e.target) && !suggestionsDropdown.contains(e.target)) {
            suggestionsDropdown.style.display = 'none';
        }
    });

    // Hiện lại dropdown nếu input có chữ khi focus
    searchInput.addEventListener('focus', () => {
        if (searchInput.value.trim().length >= 1 && suggestionsDropdown && suggestionsDropdown.innerHTML.trim() !== '') {
            suggestionsDropdown.style.display = 'block';
        }
    });

    // Đóng dropdown khi nhấn Escape
    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && suggestionsDropdown) {
            suggestionsDropdown.style.display = 'none';
        }
    });
}

/**
 * Gọi API /api/suggestions để lấy gợi ý từ SACH, DANH_MUC và KHUYEN_MAI
 */
async function fetchSearchSuggestions(query) {
    const dropdown = document.getElementById('searchSuggestionsDropdown');
    if (!dropdown) return;

    try {
        const response = await fetch(`/api/suggestions?q=${encodeURIComponent(query)}`);
        if (response.ok) {
            const data = await response.json();
            renderSuggestions(data, query);
            return;
        }
    } catch (err) {
        // Fallback: Gợi ý trực tiếp từ các thẻ sách có sẵn trên DOM
        renderFallbackSuggestions(query);
    }
}

function renderSuggestions(data, query) {
    const dropdown = document.getElementById('searchSuggestionsDropdown');
    if (!dropdown) return;

    let html = '';

    // 1. Gợi ý Danh mục (DANH_MUC)
    if (data.categories && data.categories.length > 0) {
        html += `
            <div class="suggest-section">
                <div class="suggest-header"><i class="fas fa-layer-group"></i> Danh mục liên quan (DANH_MUC)</div>
                <div class="suggest-cat-chips">
                    ${data.categories.map(c => `
                        <span class="suggest-cat-pill" onclick="selectSuggestionCategory('${c}')">
                            <i class="fas fa-tag"></i> ${c}
                        </span>
                    `).join('')}
                </div>
            </div>
        `;
    }

    // 2. Gợi ý Khuyến mãi (KHUYEN_MAI, SACH_KHUYEN_MAI)
    if (data.promotions && data.promotions.length > 0) {
        html += `
            <div class="suggest-section">
                <div class="suggest-header"><i class="fas fa-bolt"></i> Khuyến mãi nổi bật (KHUYEN_MAI)</div>
                ${data.promotions.map(p => `
                    <div class="suggest-promo-item">
                        <i class="fas fa-gift" style="color: #f59e0b;"></i>
                        <div>
                            <span class="suggest-promo-title">${p.title}</span> - 
                            <span class="suggest-promo-desc">${p.description}</span>
                            <span class="suggest-promo-badge">${p.discountText}</span>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }

    // 3. Gợi ý Sách phù hợp (SACH, TAC_GIA, NXB)
    if (data.books && data.books.length > 0) {
        html += `
            <div class="suggest-section">
                <div class="suggest-header"><i class="fas fa-book"></i> Sách phù hợp nhất (${data.books.length})</div>
                ${data.books.map(b => `
                    <div class="suggest-book-item" onclick="openQuickView(${b.id}); hideSuggestions();">
                        <img src="${b.image}" alt="${b.title}" class="suggest-book-img" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=100';">
                        <div class="suggest-book-info">
                            <div class="suggest-book-title">${b.title}</div>
                            <div class="suggest-book-author"><i class="fas fa-feather-alt"></i> ${b.author} | <i class="fas fa-building-columns"></i> ${b.publisher || 'NXB Tri Thức'}</div>
                            <div class="suggest-book-price">
                                <span>${b.formattedPrice}</span>
                                ${b.isOutOfStock 
                                    ? `<span class="suggest-stock-tag out"><i class="fas fa-circle-exclamation"></i> Hết hàng</span>` 
                                    : `<span class="suggest-stock-tag in"><i class="fas fa-check"></i> Còn hàng</span>`}
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }

    if (!html) {
        html = `
            <div class="suggest-empty">
                <i class="fas fa-search"></i>
                <p>Không có gợi ý tức thì cho "<strong>${escapeHtml(query)}</strong>"</p>
                <small>Nhấn Enter để tìm kiếm toàn bộ hoặc lọc theo tiêu chí bên dưới.</small>
            </div>
        `;
    }

    dropdown.innerHTML = html;
    dropdown.style.display = 'block';
}

function renderFallbackSuggestions(query) {
    const dropdown = document.getElementById('searchSuggestionsDropdown');
    const cards = Array.from(document.querySelectorAll('.book-card'));
    const qLower = query.toLowerCase();

    const matched = cards.filter(card => {
        const title = (card.getAttribute('data-title') || '').toLowerCase();
        const author = (card.getAttribute('data-author') || '').toLowerCase();
        return title.includes(qLower) || author.includes(qLower);
    }).slice(0, 4);

    if (matched.length === 0) {
        dropdown.style.display = 'none';
        return;
    }

    const books = matched.map(card => ({
        id: parseInt(card.getAttribute('data-id')),
        title: card.getAttribute('data-title'),
        author: card.getAttribute('data-author'),
        publisher: card.getAttribute('data-publisher') || 'NXB Tri Thức',
        formattedPrice: card.getAttribute('data-formatted-price'),
        image: card.getAttribute('data-image'),
        isOutOfStock: card.getAttribute('data-out-of-stock') === 'true'
    }));

    renderSuggestions({ books, categories: [], promotions: [] }, query);
}

function hideSuggestions() {
    const dropdown = document.getElementById('searchSuggestionsDropdown');
    if (dropdown) dropdown.style.display = 'none';
}

function selectSuggestionCategory(category) {
    hideSuggestions();
    const pills = document.querySelectorAll('.category-pill');
    pills.forEach(p => {
        if (p.getAttribute('data-category').toLowerCase() === category.toLowerCase()) {
            p.click();
        }
    });
}

/* ==========================================================================
   4. BỘ LỌC ĐA TIÊU CHÍ (LUỒNG RẼ NHÁNH 3a) & HIỂN THỊ DANH SÁCH (LUỒNG CƠ BẢN 3)
   ========================================================================== */
function initCategoryPills() {
    const pills = document.querySelectorAll('.category-pill');
    pills.forEach(pill => {
        pill.addEventListener('click', () => {
            pills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            onFilterChange();
        });
    });
}

function initMultiFilters() {
    const selects = ['filterAuthor', 'filterPublisher', 'filterPrice', 'filterStock'];
    selects.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('change', onFilterChange);
        }
    });
}

/**
 * Xử lý lọc client-side kết hợp toàn diện 5 tiêu chí:
 * Danh mục, Tác giả, Nhà xuất bản, Khoảng giá, Tồn kho (Bảng KHO) + Từ khóa tìm kiếm
 */
function onFilterChange() {
    const cards = document.querySelectorAll('.book-card');
    const searchInput = document.getElementById('searchInput');
    const keyword = searchInput ? searchInput.value.trim().toLowerCase() : '';

    const activeCatEl = document.querySelector('.category-pill.active');
    const selectedCategory = activeCatEl ? activeCatEl.getAttribute('data-category') : 'Tất cả';

    const authorSelect = document.getElementById('filterAuthor');
    const selectedAuthor = authorSelect ? authorSelect.value : 'Tất cả';

    const publisherSelect = document.getElementById('filterPublisher');
    const selectedPublisher = publisherSelect ? publisherSelect.value : 'Tất cả';

    const priceSelect = document.getElementById('filterPrice');
    const selectedPrice = priceSelect ? priceSelect.value : 'all';

    const stockSelect = document.getElementById('filterStock');
    const selectedStock = stockSelect ? stockSelect.value : 'all';

    let minPrice = 0;
    let maxPrice = Infinity;
    if (selectedPrice !== 'all') {
        const parts = selectedPrice.split('-');
        if (parts.length === 2) {
            minPrice = parseFloat(parts[0]);
            maxPrice = parseFloat(parts[1]);
        }
    }

    let visibleCount = 0;

    cards.forEach(card => {
        const title = (card.getAttribute('data-title') || '').toLowerCase();
        const author = (card.getAttribute('data-author') || '').toLowerCase();
        const publisher = (card.getAttribute('data-publisher') || '').toLowerCase();
        const code = (card.getAttribute('data-code') || '').toLowerCase();
        const category = card.getAttribute('data-category') || '';
        const price = parseFloat(card.getAttribute('data-price')) || 0;
        const isOutOfStock = card.getAttribute('data-out-of-stock') === 'true';

        // 1. Kiểm tra Danh mục
        const matchCat = (selectedCategory === 'Tất cả' || category.toLowerCase() === selectedCategory.toLowerCase());

        // 2. Kiểm tra Từ khóa (tiêu đề, tác giả, NXB, mã sách)
        const matchKeyword = (!keyword || 
            title.includes(keyword) || 
            author.includes(keyword) || 
            publisher.includes(keyword) || 
            code.includes(keyword)
        );

        // 3. Kiểm tra Tác giả
        const matchAuthor = (selectedAuthor === 'Tất cả' || author.toLowerCase() === selectedAuthor.toLowerCase());

        // 4. Kiểm tra Nhà xuất bản
        const matchPublisher = (selectedPublisher === 'Tất cả' || publisher.toLowerCase() === selectedPublisher.toLowerCase());

        // 5. Kiểm tra Khoảng giá
        const matchPrice = (price >= minPrice && price <= maxPrice);

        // 6. Kiểm tra Tồn kho (Bảng KHO - Luồng 4a)
        let matchStock = true;
        if (selectedStock === 'in_stock') {
            matchStock = !isOutOfStock;
        } else if (selectedStock === 'out_of_stock') {
            matchStock = isOutOfStock;
        }

        if (matchCat && matchKeyword && matchAuthor && matchPublisher && matchPrice && matchStock) {
            card.style.display = 'flex';
            visibleCount++;
        } else {
            card.style.display = 'none';
        }
    });

    // Cập nhật số lượng sách hiển thị
    const countElem = document.getElementById('bookCountDisplay');
    if (countElem) {
        countElem.textContent = visibleCount;
    }

    // Luồng rẽ nhánh 2a: Xử lý hiển thị "Không tìm thấy sản phẩm phù hợp" + gợi ý AI Chatbot
    updateEmptyState(visibleCount, keyword);
}

/**
 * Hiển thị khối không tìm thấy sản phẩm phù hợp khi kết quả = 0 (Luồng 2a.1 & 2a.2)
 */
function updateEmptyState(visibleCount, keyword) {
    const bookGrid = document.getElementById('bookGrid');
    if (!bookGrid) return;

    let emptyElem = document.getElementById('clientEmptyState');

    if (visibleCount === 0) {
        if (!emptyElem) {
            emptyElem = document.createElement('div');
            emptyElem.id = 'clientEmptyState';
            emptyElem.className = 'no-books-found';
            bookGrid.appendChild(emptyElem);
        }
        emptyElem.style.display = 'block';
        emptyElem.innerHTML = `
            <div class="empty-icon-wrap"><i class="fas fa-book-open"></i></div>
            <h3>Không tìm thấy sản phẩm phù hợp</h3>
            <p>Rất tiếc, không có cuốn sách nào khớp với từ khóa <strong>"${escapeHtml(keyword)}"</strong> hoặc bộ lọc hiện tại của bạn.</p>
            <div class="empty-ai-suggestion">
                <p><i class="fas fa-robot" style="color: var(--accent);"></i> Gợi ý: Bạn có thể thử đặt lại bộ lọc hoặc hỏi Trợ lý AI Bookora để được tìm và gợi ý sách tương đương!</p>
                <div style="display:flex; justify-content:center; gap: 12px; margin-top: 14px; flex-wrap: wrap;">
                    <button type="button" class="btn-empty-ai" onclick="openChatbot('Gợi ý sách tương tự từ khóa: ${escapeHtml(keyword)}')">
                        <i class="fas fa-comments"></i> Hỏi Chatbot AI tư vấn ngay
                    </button>
                    <button type="button" class="btn-reset-filters" style="padding: 10px 18px; border-radius: 20px;" onclick="resetAllFilters()">
                        <i class="fas fa-rotate-left"></i> Đặt lại bộ lọc
                    </button>
                </div>
            </div>
        `;
    } else {
        if (emptyElem) {
            emptyElem.style.display = 'none';
        }
    }
}

/**
 * Đặt lại tất cả các bộ lọc về mặc định ban đầu
 */
function resetAllFilters() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) searchInput.value = '';

    const authorSelect = document.getElementById('filterAuthor');
    if (authorSelect) authorSelect.value = 'Tất cả';

    const pubSelect = document.getElementById('filterPublisher');
    if (pubSelect) pubSelect.value = 'Tất cả';

    const priceSelect = document.getElementById('filterPrice');
    if (priceSelect) priceSelect.value = 'all';

    const stockSelect = document.getElementById('filterStock');
    if (stockSelect) stockSelect.value = 'all';

    const pills = document.querySelectorAll('.category-pill');
    pills.forEach((p, idx) => {
        if (idx === 0) p.classList.add('active');
        else p.classList.remove('active');
    });

    hideSuggestions();
    onFilterChange();
    showToast('Đã đặt lại toàn bộ bộ lọc!');
}

/* ==========================================================================
   5. QUICK VIEW MODAL - ĐẦY ĐỦ 9 THÔNG TIN THEO BA & XỬ LÝ HẾT HÀNG (LUỒNG 4 & 4a)
   ========================================================================== */
let currentModalBook = null;

function openQuickView(bookId) {
    const card = document.querySelector(`.book-card[data-id="${bookId}"]`);
    if (!card) return;

    const isOutOfStock = card.getAttribute('data-out-of-stock') === 'true';
    const stockCount = parseInt(card.getAttribute('data-stock')) || 0;
    const publisher = card.getAttribute('data-publisher') || 'NXB Tri Thức';
    const promotion = card.getAttribute('data-promotion') || 'Tặng kèm Bookmark độc quyền Bookora';
    const originalPrice = parseFloat(card.getAttribute('data-original-price')) || 0;

    currentModalBook = {
        id: bookId,
        code: card.getAttribute('data-code') || '',
        title: card.getAttribute('data-title'),
        author: card.getAttribute('data-author'),
        publisher: publisher,
        price: parseFloat(card.getAttribute('data-price')),
        originalPrice: originalPrice,
        formattedPrice: card.getAttribute('data-formatted-price'),
        category: card.getAttribute('data-category'),
        image: card.getAttribute('data-image'),
        desc: card.getAttribute('data-desc'),
        rating: card.getAttribute('data-rating'),
        reviewCount: card.getAttribute('data-review-count') || '100',
        stock: stockCount,
        isOutOfStock: isOutOfStock,
        promotion: promotion
    };

    // 1. Ảnh bìa
    const coverEl = document.getElementById('modalCover');
    if (coverEl) coverEl.src = currentModalBook.image;

    // 2. Tên sách
    const titleEl = document.getElementById('modalTitle');
    if (titleEl) titleEl.textContent = currentModalBook.title;

    // 3. Giá bán & Giá gốc
    const priceEl = document.getElementById('modalPrice');
    if (priceEl) priceEl.textContent = currentModalBook.formattedPrice;

    const origPriceEl = document.getElementById('modalOriginalPrice');
    const discountTagEl = document.getElementById('modalDiscountTag');
    if (origPriceEl) {
        if (originalPrice > currentModalBook.price) {
            origPriceEl.textContent = formatMoney(originalPrice);
            origPriceEl.style.display = 'inline';
            if (discountTagEl) {
                const discount = Math.round((originalPrice - currentModalBook.price) / originalPrice * 100);
                discountTagEl.textContent = `-${discount}%`;
                discountTagEl.style.display = 'inline-block';
            }
        } else {
            origPriceEl.style.display = 'none';
            if (discountTagEl) discountTagEl.style.display = 'none';
        }
    }

    // 4. Tác giả
    const authorEl = document.getElementById('modalAuthor');
    if (authorEl) authorEl.textContent = currentModalBook.author;

    // 5. Nhà xuất bản (NHA_XUAT_BAN)
    const pubEl = document.getElementById('modalPublisher');
    if (pubEl) pubEl.textContent = currentModalBook.publisher;

    // 6. Danh mục (DANH_MUC)
    const catEl = document.getElementById('modalCategory');
    if (catEl) catEl.textContent = currentModalBook.category;

    const codeEl = document.getElementById('modalCode');
    if (codeEl) codeEl.innerHTML = `<i class="fas fa-barcode"></i> Mã: ${currentModalBook.code}`;

    // 7. Mô tả
    const descEl = document.getElementById('modalDesc');
    if (descEl) descEl.textContent = currentModalBook.desc;

    // 8. Đánh giá
    const ratingEl = document.getElementById('modalRating');
    if (ratingEl) ratingEl.textContent = currentModalBook.rating;
    const reviewCountEl = document.getElementById('modalReviewCount');
    if (reviewCountEl) reviewCountEl.textContent = `(${currentModalBook.reviewCount} đánh giá)`;

    // 9. Tồn kho & Trạng thái kho (Bảng KHO - Luồng 4 & 4a)
    const stockTextEl = document.getElementById('modalStockText');
    const stockBadgeEl = document.getElementById('modalStockBadge');
    const btnAddToCart = document.getElementById('btnModalAddToCart');
    const btnNotify = document.getElementById('btnModalNotify');
    const qtyControl = document.getElementById('modalQtyControl');

    if (currentModalBook.isOutOfStock) {
        // Luồng 4a: Sách hết hàng
        if (stockTextEl) stockTextEl.textContent = 'Hết hàng (0 cuốn)';
        if (stockBadgeEl) {
            stockBadgeEl.className = 'modal-stock-badge out-of-stock';
            stockBadgeEl.innerHTML = '<i class="fas fa-circle-exclamation"></i> Hết hàng trong kho (0 cuốn)';
        }
        if (btnAddToCart) btnAddToCart.style.display = 'none';
        if (qtyControl) qtyControl.style.display = 'none';
        if (btnNotify) {
            btnNotify.style.display = 'inline-flex';
            btnNotify.textContent = 'Báo Khi Có Hàng Lại';
        }
    } else {
        // Luồng 4: Còn hàng
        if (stockTextEl) stockTextEl.textContent = `Còn hàng (${currentModalBook.stock} cuốn)`;
        if (stockBadgeEl) {
            stockBadgeEl.className = 'modal-stock-badge in-stock';
            stockBadgeEl.innerHTML = `<i class="fas fa-check-circle"></i> Còn hàng trong kho (${currentModalBook.stock} cuốn)`;
        }
        if (btnAddToCart) {
            btnAddToCart.style.display = 'inline-flex';
            btnAddToCart.disabled = false;
        }
        if (qtyControl) qtyControl.style.display = 'flex';
        if (btnNotify) btnNotify.style.display = 'none';
    }

    // Khuyến mãi đi kèm (SACH_KHUYEN_MAI, KHUYEN_MAI)
    const promoEl = document.getElementById('modalPromotion');
    if (promoEl) {
        promoEl.textContent = `Ưu đãi: ${currentModalBook.promotion}`;
    }

    const qtyInput = document.getElementById('modalQty');
    if (qtyInput) qtyInput.value = 1;

    document.getElementById('quickViewModal').classList.add('active');
}

function closeQuickView() {
    const modal = document.getElementById('quickViewModal');
    if (modal) modal.classList.remove('active');
}

function initModalListeners() {
    const modal = document.getElementById('quickViewModal');
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeQuickView();
            }
        });
    }
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeQuickView();
        }
    });
}

function changeModalQty(delta) {
    const input = document.getElementById('modalQty');
    if (!input || !currentModalBook) return;
    let val = parseInt(input.value) || 1;
    const maxStock = currentModalBook.stock > 0 ? currentModalBook.stock : 1;
    val = Math.max(1, Math.min(maxStock, val + delta));
    input.value = val;
}

function addModalBookToCart() {
    if (!currentModalBook) return;
    if (currentModalBook.isOutOfStock) {
        notifyOutOfStock(currentModalBook.title);
        return;
    }
    const qty = parseInt(document.getElementById('modalQty').value) || 1;
    addToCart(currentModalBook.id, qty);
    closeQuickView();
}

function notifyOutOfStockFromModal() {
    if (currentModalBook) {
        notifyOutOfStock(currentModalBook.title);
        closeQuickView();
    }
}

function notifyOutOfStock(title) {
    showToast(`🔔 Đã ghi nhận! Chúng tôi sẽ thông báo cho bạn ngay khi cuốn "${title}" có hàng trở lại.`);
}

/* ==========================================================================
   6. CHATBOT AI WIDGET (BOOKORA AI ASSISTANT - LUỒNG RẼ NHÁNH 2a.2)
   ========================================================================== */
let chatbotToggleLock = false;

function toggleChatbot() {
    if (chatbotToggleLock) return;
    chatbotToggleLock = true;
    setTimeout(() => { chatbotToggleLock = false; }, 250);

    const windowEl = document.getElementById('chatbotWindow');
    if (!windowEl) {
        console.warn('[Bookora AI] chatbotWindow element not found in DOM');
        return;
    }

    const isCurrentlyOpen = windowEl.classList.contains('active') || windowEl.classList.contains('open') || windowEl.style.display === 'flex';
    if (isCurrentlyOpen) {
        windowEl.classList.remove('active', 'open');
        windowEl.style.display = 'none';
        console.log('[Bookora AI] Chatbot window closed');
    } else {
        windowEl.classList.add('active', 'open');
        windowEl.style.display = 'flex';
        console.log('[Bookora AI] Chatbot window opened');
        const input = document.getElementById('chatbotInput');
        if (input) setTimeout(() => input.focus(), 150);
    }
}

function closeChatbot() {
    const windowEl = document.getElementById('chatbotWindow');
    if (!windowEl) return;
    windowEl.classList.remove('active', 'open');
    windowEl.style.display = 'none';
    console.log('[Bookora AI] Chatbot window closed via close button');
}

function openChatbot(initialPrompt = '') {
    const windowEl = document.getElementById('chatbotWindow');
    if (windowEl) {
        windowEl.classList.add('active', 'open');
        windowEl.style.display = 'flex';
    }
    if (initialPrompt) {
        const input = document.getElementById('chatbotInput');
        if (input) input.value = initialPrompt;
        sendChatbotMessage();
    }
}

function initChatbotWidget() {
    // Không gắn thêm addEventListener cho fab vì nút đã có onclick="toggleChatbot()" trên HTML,
    // tránh kích hoạt kép (double invocation) làm đóng mở trong cùng một thao tác click.
    console.log('[Bookora AI] Chatbot widget initialized successfully');
}

// Gán trực tiếp vào window để hỗ trợ mọi lời gọi inline onclick
window.toggleChatbot = toggleChatbot;
window.closeChatbot = closeChatbot;
window.openChatbot = openChatbot;
window.sendChatbotPrompt = sendChatbotPrompt;
window.sendChatbotMessage = sendChatbotMessage;

function sendChatbotPrompt(promptText) {
    const input = document.getElementById('chatbotInput');
    if (input) input.value = promptText;
    sendChatbotMessage();
}

async function sendChatbotMessage() {
    const input = document.getElementById('chatbotInput');
    const messagesContainer = document.getElementById('chatbotMessages');
    if (!input || !messagesContainer) return;

    const messageText = input.value.trim();
    if (!messageText) return;

    // 1. Thêm bubble tin nhắn của người dùng
    appendChatBubble('user', escapeHtml(messageText));
    input.value = '';

    // 2. Thêm bubble loading của AI
    const loadingId = 'ai-loading-' + Date.now();
    const loadingBubble = document.createElement('div');
    loadingBubble.id = loadingId;
    loadingBubble.className = 'chat-bubble chat-bubble-ai';
    loadingBubble.innerHTML = '<i class="fas fa-circle-notch fa-spin"></i> Trợ lý AI đang tra cứu sách và khuyến mãi...';
    messagesContainer.appendChild(loadingBubble);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;

    // 3. Gửi truy vấn tới backend API /api/chatbot
    try {
        const response = await fetch(`/api/chatbot?msg=${encodeURIComponent(messageText)}`);
        const loadingEl = document.getElementById(loadingId);
        if (loadingEl) loadingEl.remove();

        if (response.ok) {
            const data = await response.json();
            renderChatbotResponse(data);
        } else {
            renderChatbotFallback(messageText);
        }
    } catch (err) {
        const loadingEl = document.getElementById(loadingId);
        if (loadingEl) loadingEl.remove();
        renderChatbotFallback(messageText);
    }
}

function appendChatBubble(sender, textHtml) {
    const messagesContainer = document.getElementById('chatbotMessages');
    if (!messagesContainer) return;

    const bubble = document.createElement('div');
    bubble.className = `chat-bubble chat-bubble-${sender}`;
    bubble.innerHTML = textHtml;
    messagesContainer.appendChild(bubble);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function renderChatbotResponse(data) {
    const messagesContainer = document.getElementById('chatbotMessages');
    if (!messagesContainer) return;

    // 1. Tin nhắn trả lời dạng bubble văn bản
    const replyText = data.reply || 'Dưới đây là một số tựa sách gợi ý phù hợp dành cho bạn:';
    appendChatBubble('ai', replyText);

    // 2. Khối thẻ sách gợi ý - Chiếm đúng 2/3 chiều ngang khung Chatbot AI
    const recList = data.recommendations || data.books || [];
    if (recList.length > 0) {
        const recWrap = document.createElement('div');
        recWrap.className = 'chat-recommendations';
        recWrap.innerHTML = recList.map(book => `
            <div class="chat-rec-item" onclick="openQuickView(${book.id})" title="Nhấp để xem chi tiết ${escapeHtml(book.title)}">
                <img src="${book.image}" alt="${escapeHtml(book.title)}" class="chat-rec-img" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=100';">
                <div class="chat-rec-meta">
                    <strong class="chat-rec-title">${escapeHtml(book.title)}</strong>
                    <span class="chat-rec-author"><i class="fas fa-feather-alt"></i> ${escapeHtml(book.author)}</span>
                    <div class="chat-rec-price-row">
                        <span class="chat-rec-price">${book.formattedPrice}</span>
                        ${book.isOutOfStock 
                            ? `<span class="badge-tag badge-out-of-stock" style="font-size:9px; padding:1px 5px;"><i class="fas fa-circle-exclamation"></i> Hết</span>` 
                            : `<span class="badge-tag badge-in-stock" style="font-size:9px; padding:1px 5px;"><i class="fas fa-check"></i> Còn</span>`}
                    </div>
                </div>
            </div>
        `).join('');
        messagesContainer.appendChild(recWrap);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
}

function renderChatbotFallback(messageText) {
    // Trả lời thông minh offline dựa trên từ khóa nếu mất kết nối backend
    let reply = `Cảm ơn bạn đã hỏi về "<strong>${escapeHtml(messageText)}</strong>". Bạn có thể tìm thấy rất nhiều tác phẩm kinh điển tại Bookora, ví dụ như "Tuổi Trẻ Đáng Giá Bao Nhiêu", "Đắc Nhân Tâm", hoặc các đầu sách Lập trình Java. Bạn có muốn xem thêm chi tiết không?`;
    appendChatBubble('ai', reply);
}

/* ==========================================================================
   7. SHOPPING CART LOGIC
   ========================================================================== */
function addToCart(bookId, quantity = 1) {
    const card = document.querySelector(`.book-card[data-id="${bookId}"]`);
    if (!card) return;

    const isOutOfStock = card.getAttribute('data-out-of-stock') === 'true';
    if (isOutOfStock) {
        notifyOutOfStock(card.getAttribute('data-title'));
        return;
    }

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

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/* ==========================================================================
   8. TOAST NOTIFICATION
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
    toast.innerHTML = `<i class="fas fa-info-circle"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(50px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

