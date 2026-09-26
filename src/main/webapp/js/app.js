/**
 * Bookstore Interactive JavaScript Application - Bookora
 * Hoàn thiện đầy đủ Use Case "Xem và tìm kiếm sách" theo chuẩn tài liệu BA
 */

// Giỏ hàng lưu trong LocalStorage
let cart = JSON.parse(localStorage.getItem('bookstore_cart') || '[]');

function formatVNCurrency(amount) {
    if (typeof amount !== 'number') amount = Number(amount) || 0;
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
}

function formatMoney(amount) {
    return formatVNCurrency(amount);
}

document.addEventListener('DOMContentLoaded', () => {
    if (typeof initPasswordToggle === 'function') initPasswordToggle();
    if (typeof initQuickLoginPills === 'function') initQuickLoginPills();
    if (typeof initUserDropdown === 'function') initUserDropdown();
    if (typeof initCategoryPills === 'function') initCategoryPills();
    if (typeof initLiveSearchAndSuggestions === 'function') initLiveSearchAndSuggestions();
    if (typeof initMultiFilters === 'function') initMultiFilters();
    if (typeof initModalListeners === 'function') initModalListeners();
    if (typeof initChatbotWidget === 'function') initChatbotWidget();
    if (typeof initCartPopoverListeners === 'function') initCartPopoverListeners();
    if (typeof initCartPage === 'function') initCartPage();
    if (typeof updateCartUI === 'function') updateCartUI();
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
/* ==========================================================================
   3. LIVE SEARCH & GỢI Ý TÌM KIẾM (HIỂN THỊ DANH SÁCH SÁCH THEO ĐÚNG ẢNH MẪU)
   ========================================================================== */
function initLiveSearchAndSuggestions() {
    const searchInput = document.getElementById('searchInput');
    const suggestionsDropdown = document.getElementById('searchSuggestionsDropdown');
    const btnSearch = document.getElementById('btnSearchSubmit');
    const searchForm = document.getElementById('searchForm');
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
        }, 180);
    });

    // Bấm nút Tìm kiếm
    if (btnSearch) {
        btnSearch.addEventListener('click', (e) => {
            e.preventDefault();
            submitFullSearch(searchInput.value.trim());
        });
    }

    // Submit form tìm kiếm khi nhấn Enter
    if (searchForm) {
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            submitFullSearch(searchInput.value.trim());
        });
    }

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
 * Định dạng tiền tệ VNĐ chuẩn dấu phẩy phân cách ngàn (Ví dụ: 100,000đ)
 */
function formatVNCurrency(price) {
    if (price === undefined || price === null || isNaN(price)) return '0đ';
    return Number(price).toLocaleString('en-US') + 'đ';
}

/**
 * Xử lý khi nhấn Xem thêm sản phẩm hoặc bấm nút Tìm kiếm
 */
function submitFullSearch(query) {
    hideSuggestions();
    const searchInput = document.getElementById('searchInput');
    if (searchInput && query !== undefined && query !== null) {
        searchInput.value = query;
    }
    onFilterChange();
    const target = document.getElementById('bookGrid') || document.querySelector('.books-section');
    if (target) {
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

/**
 * Gọi API /api/suggestions để lấy danh sách gợi ý sách
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

/**
 * Hiển thị danh sách gợi ý sách chuẩn theo giao diện ảnh mẫu:
 * - Ảnh bìa sách bên trái
 * - Tiêu đề sách
 * - Giá bán hiện tại in đậm kèm giá gốc gạch ngang (nếu có giảm giá)
 * - Dưới cùng là nút "Xem thêm ... sản phẩm"
 */
function renderSuggestions(data, query) {
    const dropdown = document.getElementById('searchSuggestionsDropdown');
    if (!dropdown) return;

    const books = (data && data.books) ? data.books : [];
    const totalMatches = (data && data.totalMatches !== undefined) ? data.totalMatches : books.length;

    if (books.length === 0) {
        dropdown.innerHTML = `
            <div class="search-suggest-empty">
                <i class="fas fa-search" style="color: #9ca3af;"></i>
                <span>Không tìm thấy sản phẩm nào phù hợp với "<strong>${escapeHtml(query)}</strong>"</span>
            </div>
        `;
        dropdown.style.display = 'block';
        return;
    }

    let html = `
        <div class="search-suggest-list">
            ${books.map(b => {
                const priceFormatted = formatVNCurrency(b.price);
                const hasDiscount = b.originalPrice && b.originalPrice > b.price;
                const origPriceFormatted = hasDiscount ? formatVNCurrency(b.originalPrice) : '';

                return `
                    <div class="search-suggest-item" onclick="goToBookDetail(${b.id}); hideSuggestions();">
                        <img src="${b.image}" alt="${escapeHtml(b.title)}" class="search-suggest-thumb" 
                             onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=100';">
                        <div class="search-suggest-info">
                            <div class="search-suggest-title" title="${escapeHtml(b.title)}">${escapeHtml(b.title)}</div>
                            <div class="search-suggest-price-row">
                                <span class="search-suggest-price">${priceFormatted}</span>
                                ${hasDiscount ? `<span class="search-suggest-original-price">${origPriceFormatted}</span>` : ''}
                            </div>
                        </div>
                    </div>
                `;
            }).join('')}
        </div>
    `;

    // Thanh chân trang: Xem thêm [N] sản phẩm (chuẩn theo ảnh mẫu của người dùng)
    const countDisplay = totalMatches > 0 ? totalMatches : books.length;
    html += `
        <div class="search-suggest-footer" onclick="submitFullSearch('${escapeHtml(query)}')">
            <span>Xem thêm ${countDisplay} sản phẩm</span>
        </div>
    `;

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
        const code = (card.getAttribute('data-code') || '').toLowerCase();
        const category = (card.getAttribute('data-category') || '').toLowerCase();
        const publisher = (card.getAttribute('data-publisher') || '').toLowerCase();
        return title.includes(qLower) || author.includes(qLower) || code.includes(qLower) || category.includes(qLower) || publisher.includes(qLower);
    });

    if (matched.length === 0) {
        renderSuggestions({ books: [], totalMatches: 0 }, query);
        return;
    }

    const books = matched.slice(0, 5).map(card => {
        const price = parseFloat(card.getAttribute('data-price')) || 0;
        const origPrice = parseFloat(card.getAttribute('data-original-price')) || 0;
        return {
            id: parseInt(card.getAttribute('data-id')),
            title: card.getAttribute('data-title'),
            author: card.getAttribute('data-author'),
            publisher: card.getAttribute('data-publisher') || 'NXB Trẻ',
            price: price,
            originalPrice: origPrice,
            image: card.getAttribute('data-image'),
            isOutOfStock: card.getAttribute('data-is-out-of-stock') === 'true'
        };
    });

    renderSuggestions({ books, totalMatches: matched.length }, query);
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
 * Kiểm tra xem danh mục sách trong DB/Card có khớp với danh mục được người dùng chọn không
 */
function isCategoryMatch(bookCategory, targetCategory) {
    if (!targetCategory || targetCategory === 'Tất cả' || targetCategory === 'Tất cả sách') return true;
    if (!bookCategory) return false;

    const bCat = bookCategory.toLowerCase().trim();
    const tCat = targetCategory.toLowerCase().trim();

    if (bCat === tCat) return true;
    if (bCat.includes(tCat) || tCat.includes(bCat)) return true;

    const bNorm = removeDiacritics(bCat);
    const tNorm = removeDiacritics(tCat);
    if (bNorm === tNorm || bNorm.includes(tNorm) || tNorm.includes(bNorm)) return true;

    // Mapping đồng nghĩa tự nhiên theo danh mục sách thực tế của Bookora
    const categoryMap = {
        'văn học': ['văn học', 'tiểu thuyết', 'tản văn', 'truyện', 'kinh điển', 'bố già', 'nguyễn nhật ánh'],
        'kinh tế': ['kinh tế', 'tài chính', 'quản trị', 'đầu tư', 'kinh doanh', 'khởi nghiệp'],
        'kỹ năng sống': ['kỹ năng', 'kỹ năng sống', 'phát triển bản thân', 'đắc nhân tâm', 'thói quen', 'hạt giống tâm hồn'],
        'tâm lý học': ['tâm lý', 'tâm lý học', 'tư duy', 'cảm xúc', 'dám bị ghét', 'đọc vị'],
        'công nghệ thông tin': ['công nghệ', 'tin học', 'lập trình', 'code', 'phần mềm', 'developer'],
        'tâm linh & đời sống': ['tâm linh', 'đời sống', 'nhân quả', 'bình an', 'hiện tại', 'gia đình'],
        'khoa học': ['khoa học', 'vũ trụ', 'sapiens', 'lịch sử', 'vật lý', 'tri thức'],
        'trinh thám': ['trinh thám', 'kỳ án', 'bí ẩn', 'vụ án', 'hình sự']
    };

    if (categoryMap[tCat]) {
        return categoryMap[tCat].some(kw => bCat.includes(kw) || bNorm.includes(removeDiacritics(kw)));
    }
    return false;
}

/**
 * Xử lý lọc client-side kết hợp toàn diện 5 tiêu chí:
 * Danh mục, Tác giả, Nhà xuất bản, Khoảng giá, Tồn kho (Bảng KHO) + Từ khóa tìm kiếm
 */
function onFilterChange(forcedCategory) {
    const cards = document.querySelectorAll('.book-card');
    const searchInput = document.getElementById('searchInput');
    const keyword = searchInput ? searchInput.value.trim().toLowerCase() : '';

    // Xác định Danh mục đang chọn
    let selectedCategory = forcedCategory;
    if (!selectedCategory) {
        const titleEl = document.getElementById('selectedCategoryTitle');
        if (titleEl && titleEl.textContent !== 'Tủ Sách Nổi Bật Dành Cho Bạn') {
            selectedCategory = titleEl.textContent;
        } else {
            const activeCatEl = document.querySelector('.category-pill.active');
            selectedCategory = activeCatEl ? activeCatEl.getAttribute('data-category') : 'Tất cả';
        }
    }

    // Cập nhật Tiêu đề hiển thị chuẩn theo Ảnh 2 của người dùng
    const titleEl = document.getElementById('selectedCategoryTitle');
    if (titleEl && selectedCategory) {
        titleEl.textContent = (selectedCategory === 'Tất cả') ? 'Tủ Sách Nổi Bật Dành Cho Bạn' : selectedCategory;
    }

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
        const desc = (card.getAttribute('data-desc') || '').toLowerCase();
        const promo = (card.getAttribute('data-promotion') || '').toLowerCase();
        const price = parseFloat(card.getAttribute('data-price')) || 0;
        const isOutOfStock = card.getAttribute('data-out-of-stock') === 'true';

        // 1. Kiểm tra Danh mục
        const matchCat = isCategoryMatch(category, selectedCategory);

        // 2. Kiểm tra Từ khóa
        const kwNorm = removeDiacritics(keyword);
        const isGenericBook = kwNorm === 'sach' || kwNorm === 'book';

        const matchKeyword = (!keyword || isGenericBook || 
            title.includes(keyword) || 
            author.includes(keyword) || 
            publisher.includes(keyword) || 
            code.includes(keyword) ||
            category.toLowerCase().includes(keyword) ||
            desc.includes(keyword) ||
            promo.includes(keyword) ||
            removeDiacritics(title).includes(kwNorm) ||
            removeDiacritics(author).includes(kwNorm) ||
            removeDiacritics(category).includes(kwNorm) ||
            removeDiacritics(desc).includes(kwNorm)
        );

        // 3. Kiểm tra Tác giả
        const matchAuthor = (selectedAuthor === 'Tất cả' || author.toLowerCase() === selectedAuthor.toLowerCase());

        // 4. Kiểm tra Nhà xuất bản
        const matchPublisher = (selectedPublisher === 'Tất cả' || publisher.toLowerCase() === selectedPublisher.toLowerCase());

        // 5. Kiểm tra Khoảng giá
        const matchPrice = (price >= minPrice && price <= maxPrice);

        // 6. Kiểm tra Tồn kho
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

    updateEmptyState(visibleCount, keyword);
}

/**
 * Xử lý sắp xếp sản phẩm (Chuẩn ảnh mẫu 2)
 */
function onSortChange(sortValue) {
    const grid = document.getElementById('bookGrid');
    if (!grid) return;
    const cards = Array.from(grid.querySelectorAll('.book-card'));

    cards.sort((a, b) => {
        const priceA = parseFloat(a.getAttribute('data-price')) || 0;
        const priceB = parseFloat(b.getAttribute('data-price')) || 0;
        const ratingA = parseFloat(a.getAttribute('data-rating')) || 0;
        const ratingB = parseFloat(b.getAttribute('data-rating')) || 0;
        const idA = parseInt(a.getAttribute('data-id')) || 0;
        const idB = parseInt(b.getAttribute('data-id')) || 0;

        if (sortValue === 'price_asc') return priceA - priceB;
        if (sortValue === 'price_desc') return priceB - priceA;
        if (sortValue === 'rating') return ratingB - ratingA;
        if (sortValue === 'bestseller') return ratingB - ratingA;
        return idB - idA; // Mới nhất
    });

    cards.forEach(card => grid.appendChild(card));
    showToast('Đã sắp xếp danh sách sản phẩm');

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
        emptyElem.style.display = 'flex';
        emptyElem.innerHTML = `
            <div class="empty-icon-wrap"><i class="fas fa-book-open"></i></div>
            <h3 class="empty-title">Không tìm thấy sản phẩm phù hợp</h3>
            <div class="empty-ai-suggestion">
                <div class="empty-ai-msg" onclick="openChatbot('Gợi ý sách cho tôi${keyword ? ': ' + escapeHtml(keyword) : ''}')">
                    <i class="fas fa-robot ai-robot-icon"></i>
                    <span>Gợi ý: Bạn có thể tìm kiếm bằng Chatbot AI!</span>
                </div>
                <div class="empty-ai-action">
                    <button type="button" class="btn-empty-ai" onclick="openChatbot('Gợi ý sách cho tôi${keyword ? ' với từ khóa: ' + escapeHtml(keyword) : ''}')">
                        <i class="fas fa-comments"></i> Hỏi Chatbot AI tư vấn ngay
                    </button>
                    <button type="button" class="btn-empty-reset" onclick="resetAllFilters()">
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

function goToBookDetail(bookId) {
    if (!bookId) return;
    window.location.href = 'book?id=' + bookId;
}

function quickViewBook(bookId) {
    openQuickView(bookId);
}

async function openQuickView(bookId) {
    if (!bookId) return;
    bookId = Number(bookId);
    let card = document.querySelector(`.book-card[data-id="${bookId}"]`);
    
    let bookData = null;
    if (card) {
        const isOutOfStock = card.classList.contains('is-out-of-stock') || 
                             card.getAttribute('data-is-out-of-stock') === 'true' || 
                             card.getAttribute('data-out-of-stock') === 'true';
        const stockCount = parseInt(card.getAttribute('data-stock')) || (isOutOfStock ? 0 : 20);
        const publisher = card.getAttribute('data-publisher') || 'NXB Tri Thức';
        const promotion = card.getAttribute('data-promotion') || 'Tặng kèm Bookmark độc quyền Bookora';
        const originalPrice = parseFloat(card.getAttribute('data-original-price')) || 0;
        const price = parseFloat(card.getAttribute('data-price')) || 0;
        const formattedPrice = card.getAttribute('data-formatted-price') || formatVNCurrency(price);

        bookData = {
            id: bookId,
            code: card.getAttribute('data-code') || ('MS' + String(bookId).padStart(3, '0')),
            title: card.getAttribute('data-title') || 'Sách Bookora',
            author: card.getAttribute('data-author') || 'Nhiều tác giả',
            publisher: publisher,
            price: price,
            originalPrice: originalPrice,
            formattedPrice: formattedPrice,
            category: card.getAttribute('data-category') || 'Tổng hợp',
            image: card.getAttribute('data-image') || (card.querySelector('img') ? card.querySelector('img').src : ''),
            desc: card.getAttribute('data-desc') || 'Tác phẩm chọn lọc đặc sắc được phát hành chính hãng tại Bookora.',
            rating: card.getAttribute('data-rating') || '4.9',
            reviewCount: card.getAttribute('data-review-count') || '120',
            stock: stockCount,
            isOutOfStock: isOutOfStock,
            promotion: promotion
        };
    } else {
        try {
            const res = await fetch(`/api/books?q=`);
            if (res.ok) {
                const list = await res.json();
                const found = list.find(b => b.id === bookId);
                if (found) {
                    bookData = {
                        id: found.id,
                        code: found.code,
                        title: found.title,
                        author: found.author,
                        publisher: found.publisher,
                        price: found.price,
                        originalPrice: found.originalPrice,
                        formattedPrice: found.formattedPrice || formatVNCurrency(found.price),
                        category: found.category,
                        image: found.image,
                        desc: found.description,
                        rating: String(found.rating || 5.0),
                        reviewCount: String(found.reviewCount || 100),
                        stock: found.stock,
                        isOutOfStock: found.isOutOfStock,
                        promotion: found.promotion || 'Tặng kèm Bookmark độc quyền Bookora'
                    };
                }
            }
        } catch (e) {
            console.error('Failed to fetch book data for quickview:', e);
        }
    }

    if (!bookData) {
        goToBookDetail(bookId);
        return;
    }

    currentModalBook = bookData;

    // 1. Cập nhật Ảnh bìa
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
        if (currentModalBook.originalPrice > currentModalBook.price) {
            origPriceEl.textContent = formatMoney(currentModalBook.originalPrice);
            origPriceEl.style.display = 'inline';
            if (discountTagEl) {
                const discount = Math.round((currentModalBook.originalPrice - currentModalBook.price) / currentModalBook.originalPrice * 100);
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

    // 5. Nhà xuất bản
    const pubEl = document.getElementById('modalPublisher');
    if (pubEl) pubEl.textContent = currentModalBook.publisher;

    // 6. Danh mục & Mã sách
    const catEl = document.getElementById('modalCategory');
    if (catEl) catEl.textContent = currentModalBook.category;

    const codeEl = document.getElementById('modalCode');
    if (codeEl) codeEl.innerHTML = `<i class="fas fa-barcode"></i> Mã: ${currentModalBook.code}`;

    // 7. Mô tả tóm tắt
    const descEl = document.getElementById('modalDesc');
    if (descEl) descEl.textContent = currentModalBook.desc;

    // 8. Đánh giá sao
    const ratingEl = document.getElementById('modalRating');
    if (ratingEl) ratingEl.textContent = currentModalBook.rating;
    const reviewCountEl = document.getElementById('modalReviewCount');
    if (reviewCountEl) reviewCountEl.textContent = `(${currentModalBook.reviewCount} đánh giá)`;

    // 9. Tồn kho & Trạng thái kho
    const stockTextEl = document.getElementById('modalStockText');
    const stockBadgeEl = document.getElementById('modalStockBadge');
    const btnAddToCart = document.getElementById('btnModalAddToCart');
    const btnBuyNow = document.getElementById('btnModalBuyNow');
    const btnNotify = document.getElementById('btnModalNotify');
    const qtyControl = document.getElementById('modalQtyControl') || document.querySelector('.modal-qty-box');

    if (currentModalBook.isOutOfStock) {
        if (stockTextEl) stockTextEl.textContent = 'Hết hàng (0 cuốn)';
        if (stockBadgeEl) {
            stockBadgeEl.className = 'modal-stock-badge out-of-stock';
            stockBadgeEl.innerHTML = '<i class="fas fa-circle-exclamation"></i> Hết hàng trong kho (0 cuốn)';
        }
        if (btnAddToCart) btnAddToCart.style.display = 'none';
        if (btnBuyNow) btnBuyNow.style.display = 'none';
        if (qtyControl) qtyControl.style.display = 'none';
        if (btnNotify) {
            btnNotify.style.display = 'inline-flex';
            btnNotify.textContent = 'Báo Khi Có Hàng Lại';
        }
    } else {
        if (stockTextEl) stockTextEl.textContent = `Còn hàng (${currentModalBook.stock} cuốn)`;
        if (stockBadgeEl) {
            stockBadgeEl.className = 'modal-stock-badge in-stock';
            stockBadgeEl.innerHTML = `<i class="fas fa-check-circle"></i> Còn hàng trong kho (${currentModalBook.stock} cuốn)`;
        }
        if (btnAddToCart) {
            btnAddToCart.style.display = 'inline-flex';
            btnAddToCart.disabled = false;
        }
        if (btnBuyNow) btnBuyNow.style.display = 'inline-flex';
        if (qtyControl) qtyControl.style.display = 'flex';
        if (btnNotify) btnNotify.style.display = 'none';
    }

    // Khuyến mãi đi kèm
    const promoEl = document.getElementById('modalPromotion');
    if (promoEl) {
        promoEl.textContent = `Ưu đãi: ${currentModalBook.promotion}`;
    }

    const qtyInput = document.getElementById('modalQty') || document.getElementById('modalQuantity');
    if (qtyInput) qtyInput.value = 1;

    const modal = document.getElementById('quickViewModal');
    if (modal) modal.classList.add('active');
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
    const input = document.getElementById('modalQty') || document.getElementById('modalQuantity');
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
    const input = document.getElementById('modalQty') || document.getElementById('modalQuantity');
    const qty = input ? (parseInt(input.value) || 1) : 1;
    addToCart(currentModalBook.id, qty);
    closeQuickView();
}

function addModalItemToCart() {
    addModalBookToCart();
}

function buyNowFromModal() {
    if (!currentModalBook) return;
    if (currentModalBook.isOutOfStock) {
        notifyOutOfStock(currentModalBook.title);
        return;
    }
    const input = document.getElementById('modalQty') || document.getElementById('modalQuantity');
    const qty = input ? (parseInt(input.value) || 1) : 1;
    addToCart(currentModalBook.id, qty);
    closeQuickView();
    window.location.href = 'cart';
}

function notifyOutOfStockFromModal() {
    if (currentModalBook) {
        notifyOutOfStock(currentModalBook.title);
        closeQuickView();
    }
}

function notifyOutOfStock(title) {
    showToast(`🔔 Đã ghi nhận! Chúng tôi sẽ thông báo cho bạn ngay khi cuốn "${title || 'này'}" có hàng trở lại.`);
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
window.toggleCartPopover = toggleCartPopover;
window.hideCartPopover = hideCartPopover;
window.showCartPopoverAuto = showCartPopoverAuto;
window.addToCart = addToCart;
window.removeFromCart = removeFromCart;
window.changeCartItemQty = changeCartItemQty;
window.proceedToCheckout = proceedToCheckout;
window.checkoutCart = checkoutCart;

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
            <div class="chat-rec-item" onclick="goToBookDetail(${book.id})" title="Nhấp để xem chi tiết ${escapeHtml(book.title)}">
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
   7. SHOPPING CART LOGIC & POPOVER (CHUẨN ẢNH MẪU 1)
   ========================================================================== */
let cartPopoverTimer = null;

function initCartPopoverListeners() {
    // Đóng popover khi nhấp chuột ra ngoài
    document.addEventListener('click', (e) => {
        const popover = document.getElementById('cartPopover');
        const toggleBtn = document.getElementById('btnCartToggle');
        if (popover && popover.classList.contains('show')) {
            if (!popover.contains(e.target) && (!toggleBtn || !toggleBtn.contains(e.target))) {
                hideCartPopover();
            }
        }
    });

    // Cập nhật vị trí caret khi thay đổi kích thước cửa sổ
    window.addEventListener('resize', () => {
        const popover = document.getElementById('cartPopover');
        if (popover && popover.classList.contains('show')) {
            adjustCartPopoverCaret();
        }
    });
}

function adjustCartPopoverCaret() {
    const popover = document.getElementById('cartPopover');
    const btn = document.getElementById('btnCartToggle');
    if (!popover || !btn) return;

    requestAnimationFrame(() => {
        const btnRect = btn.getBoundingClientRect();
        const popRect = popover.getBoundingClientRect();
        const caret = popover.querySelector('.cart-popover-caret');
        if (caret && popRect.width > 0) {
            const caretLeft = (btnRect.left + btnRect.width / 2) - popRect.left - 8;
            caret.style.left = `${Math.max(14, Math.min(popRect.width - 24, caretLeft))}px`;
            caret.style.right = 'auto';
        }
    });
}

function toggleCartPopover(event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    const popover = document.getElementById('cartPopover');
    if (!popover) {
        // Fallback mở trang giỏ hàng
        window.location.href = 'cart';
        return;
    }

    if (popover.classList.contains('show')) {
        hideCartPopover();
    } else {
        showCartPopover();
    }
}

function showCartPopover() {
    clearTimeout(cartPopoverTimer);
    const popover = document.getElementById('cartPopover');
    if (popover) {
        renderCartPopoverItems();
        popover.classList.add('show');
        adjustCartPopoverCaret();
    }
}

function showCartPopoverAuto() {
    showCartPopover();
    clearTimeout(cartPopoverTimer);
    // Tự động đóng sau 5.5 giây nếu người dùng không tương tác
    cartPopoverTimer = setTimeout(() => {
        hideCartPopover();
    }, 5500);
}

function hideCartPopover() {
    clearTimeout(cartPopoverTimer);
    const popover = document.getElementById('cartPopover');
    if (popover) {
        popover.classList.remove('show');
    }
}

async function addToCart(bookId, quantity = 1) {
    if (!bookId) return;
    bookId = Number(bookId);
    quantity = Math.max(1, parseInt(quantity) || 1);

    const card = document.querySelector(`.book-card[data-id="${bookId}"]`);
    const detailCol = document.querySelector(`.detail-info-column[data-id="${bookId}"], .detail-info-column`);
    const relatedCard = document.querySelector(`.related-book-card[onclick*="id=${bookId}"]`);

    let code = '';
    let title = '';
    let author = '';
    let publisher = 'NXB Tri Thức';
    let category = 'Tổng hợp';
    let price = 0;
    let formattedPrice = '';
    let image = 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';

    if (card) {
        const isOutOfStock = card.classList.contains('is-out-of-stock') || 
                             card.getAttribute('data-is-out-of-stock') === 'true' || 
                             card.getAttribute('data-out-of-stock') === 'true';
        if (isOutOfStock) {
            notifyOutOfStock(card.getAttribute('data-title') || 'Sách');
            return;
        }

        code = card.getAttribute('data-code') || ('MS' + String(bookId).padStart(3, '0'));
        title = card.getAttribute('data-title') || 'Sách Bookora';
        author = card.getAttribute('data-author') || 'Nhiều tác giả';
        publisher = card.getAttribute('data-publisher') || 'NXB Tri Thức';
        category = card.getAttribute('data-category') || 'Tổng hợp';
        price = parseFloat(card.getAttribute('data-price')) || 0;
        formattedPrice = card.getAttribute('data-formatted-price') || formatVNCurrency(price);
        image = card.getAttribute('data-image') || (card.querySelector('img') ? card.querySelector('img').src : image);
    } else if (currentModalBook && currentModalBook.id === bookId) {
        if (currentModalBook.isOutOfStock) {
            notifyOutOfStock(currentModalBook.title);
            return;
        }
        code = currentModalBook.code || ('MS' + String(bookId).padStart(3, '0'));
        title = currentModalBook.title;
        author = currentModalBook.author || 'Nhiều tác giả';
        publisher = currentModalBook.publisher || 'NXB Tri Thức';
        category = currentModalBook.category || 'Tổng hợp';
        price = currentModalBook.price || 0;
        formattedPrice = currentModalBook.formattedPrice || formatVNCurrency(price);
        image = currentModalBook.image || image;
    } else if (detailCol && (detailCol.getAttribute('data-id') == bookId || !card)) {
        const stockVal = parseInt(detailCol.getAttribute('data-stock')) || 0;
        if (stockVal <= 0 && detailCol.getAttribute('data-stock') !== null) {
            notifyOutOfStock(detailCol.getAttribute('data-title') || 'Sách');
            return;
        }
        title = detailCol.getAttribute('data-title') || (document.querySelector('.product-detail-title') ? document.querySelector('.product-detail-title').textContent.trim() : 'Sách');
        price = parseFloat(detailCol.getAttribute('data-price')) || 0;
        formattedPrice = formatVNCurrency(price);
        image = detailCol.getAttribute('data-image') || (document.getElementById('detailBookImage') ? document.getElementById('detailBookImage').src : image);
        const isbnEl = document.querySelector('.product-isbn');
        code = isbnEl ? isbnEl.textContent.replace('ISBN:', '').trim() : ('MS' + String(bookId).padStart(3, '0'));
        const pubEl = document.querySelector('.publisher-text, .publisher-brand-badge span');
        publisher = pubEl ? pubEl.textContent.trim() : 'NXB Tri Thức';
        const bcEl = document.querySelector('.breadcrumb-inner a:nth-child(3)');
        category = bcEl ? bcEl.textContent.trim() : 'Tổng hợp';
    } else if (relatedCard) {
        const titleEl = relatedCard.querySelector('.related-book-title');
        title = titleEl ? titleEl.textContent.trim() : 'Sách liên quan';
        const priceEl = relatedCard.querySelector('.related-price-red');
        const priceStr = priceEl ? priceEl.textContent.replace(/[^0-9]/g, '') : '0';
        price = parseFloat(priceStr) || 0;
        formattedPrice = priceEl ? priceEl.textContent.trim() : formatVNCurrency(price);
        const imgEl = relatedCard.querySelector('.related-cover-img');
        image = imgEl ? imgEl.getAttribute('src') : image;
    } else {
        try {
            const res = await fetch(`/api/books?q=`);
            if (res.ok) {
                const list = await res.json();
                const found = list.find(b => b.id === bookId);
                if (found) {
                    if (found.isOutOfStock) {
                        notifyOutOfStock(found.title);
                        return;
                    }
                    code = found.code;
                    title = found.title;
                    author = found.author;
                    publisher = found.publisher;
                    category = found.category;
                    price = found.price;
                    formattedPrice = found.formattedPrice || formatVNCurrency(price);
                    image = found.image;
                }
            }
        } catch (e) {
            console.error(e);
        }
        if (!title) {
            title = 'Sách #' + bookId;
            formattedPrice = formatVNCurrency(price);
        }
    }

    const existingIndex = cart.findIndex(item => item.id == bookId);
    if (existingIndex > -1) {
        cart[existingIndex].quantity += quantity;
        if (!cart[existingIndex].code && code) cart[existingIndex].code = code;
        if (!cart[existingIndex].publisher && publisher) cart[existingIndex].publisher = publisher;
        if (!cart[existingIndex].category && category) cart[existingIndex].category = category;
        if (!cart[existingIndex].author && author) cart[existingIndex].author = author;
        if (price > 0 && (!cart[existingIndex].price || cart[existingIndex].price === 0)) cart[existingIndex].price = price;
        if (image && (!cart[existingIndex].image || cart[existingIndex].image.includes('placeholder'))) cart[existingIndex].image = image;
    } else {
        cart.push({
            id: bookId,
            code: code,
            title: title,
            author: author,
            publisher: publisher,
            category: category,
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

    // Hiển thị Cart Popover chuẩn mẫu hình 1
    showCartPopoverAuto();
}

function saveCart() {
    localStorage.setItem('bookstore_cart', JSON.stringify(cart));
}

function updateCartUI() {
    const badge = document.getElementById('cartBadge');
    const totalCount = cart.reduce((sum, item) => sum + (parseInt(item.quantity) || 1), 0);
    if (badge) {
        badge.textContent = totalCount;
        badge.style.display = totalCount > 0 ? 'flex' : 'none';
    }

    // Render Mini Cart Popover (Hình 1)
    renderCartPopoverItems();

    // Render Full Cart Page (Hình 2) nếu đang ở trang /cart
    renderCartPageUI();

    // Đồng thời cập nhật Drawer cũ nếu có
    renderCartDrawerItems();
}

function renderCartPopoverItems() {
    const container = document.getElementById('cartPopoverItems');
    const totalVal = document.getElementById('cartPopoverTotal');
    if (!container || !totalVal) return;

    if (cart.length === 0) {
        container.innerHTML = `
            <div class="cart-popover-empty">
                <i class="fas fa-bag-shopping"></i>
                <p>Giỏ hàng của bạn đang trống</p>
            </div>
        `;
        totalVal.textContent = '0đ';
        return;
    }

    let total = 0;
    container.innerHTML = cart.map(item => {
        const itemTotal = (item.price || 0) * (item.quantity || 1);
        total += itemTotal;
        const metaParts = [];
        if (item.publisher) metaParts.push(item.publisher);
        if (item.author) metaParts.push(item.author);
        if (item.category) metaParts.push(item.category);
        const metaText = metaParts.join(' / ') || 'NXB Tri Thức / Sách chọn lọc';

        return `
            <div class="cart-popover-item">
                <img src="${item.image}" alt="${escapeHtml(item.title)}" class="cart-popover-img" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=100';">
                <div class="cart-popover-info">
                    <div class="cart-popover-top-row">
                        <h4 class="cart-popover-title" style="cursor: pointer;" onclick="goToBookDetail(${item.id})">${escapeHtml(item.title)}</h4>
                        <button type="button" class="btn-popover-remove" onclick="removeFromCart(${item.id})" title="Xóa">✕</button>
                    </div>
                    <div class="cart-popover-meta">${escapeHtml(metaText)}</div>
                    <div class="cart-popover-bottom-row">
                        <span class="cart-popover-qty-box">${item.quantity}</span>
                        <span class="cart-popover-price">${formatVNCurrency(itemTotal)}</span>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    totalVal.textContent = formatVNCurrency(total);
}

function removeFromCart(bookId) {
    cart = cart.filter(i => i.id != bookId);
    saveCart();
    updateCartUI();
    showToast('Đã xóa sản phẩm khỏi giỏ hàng.');
}

function proceedToCheckout() {
    if (cart.length === 0) {
        showToast('Giỏ hàng trống! Hãy chọn ít nhất một cuốn sách.');
        return;
    }
    const total = cart.reduce((sum, item) => sum + ((item.price || 0) * (item.quantity || 1)), 0);
    const confirmed = confirm(`Xác nhận tiến hành thanh toán đơn hàng với tổng số tiền: ${formatVNCurrency(total)}?`);
    if (confirmed) {
        alert(`🎉 Chúc mừng bạn! Đơn hàng trị giá ${formatVNCurrency(total)} đã được đặt thành công. Bookora sẽ liên hệ giao hàng sớm nhất!`);
        cart = [];
        saveCart();
        updateCartUI();
        hideCartPopover();
    }
}

function checkoutCart() {
    proceedToCheckout();
}

/* ==========================================================================
   8. FULL CART PAGE LOGIC (CHUẨN ẢNH MẪU 2)
   ========================================================================== */
function initCartPage() {
    const pageContainer = document.getElementById('cartPageContainer');
    if (!pageContainer) return;

    // Phục hồi ghi chú đơn hàng nếu đã lưu
    const noteEl = document.getElementById('cartOrderNote');
    if (noteEl) {
        const savedNote = localStorage.getItem('bookstore_order_note') || '';
        noteEl.value = savedNote;
        noteEl.addEventListener('input', (e) => {
            localStorage.setItem('bookstore_order_note', e.target.value);
        });
    }

    renderCartPageUI();
}

function renderCartPageUI() {
    const pageContainer = document.getElementById('cartPageContainer');
    if (!pageContainer) return;

    const subtitleCount = document.getElementById('cartCountSubtitle');
    const tableBody = document.getElementById('cartTableBody');
    const grandTotal = document.getElementById('cartPageGrandTotal');
    const tableWrapper = document.getElementById('cartTableWrapper');
    const bottomGrid = document.getElementById('cartBottomGrid');
    const emptyView = document.getElementById('cartEmptyView');

    const totalQty = cart.reduce((sum, item) => sum + (parseInt(item.quantity) || 1), 0);
    const totalPrice = cart.reduce((sum, item) => sum + ((item.price || 0) * (item.quantity || 1)), 0);

    if (subtitleCount) {
        subtitleCount.textContent = totalQty;
    }

    if (cart.length === 0) {
        if (tableWrapper) tableWrapper.style.display = 'none';
        if (bottomGrid) bottomGrid.style.display = 'none';
        if (emptyView) emptyView.style.display = 'block';
        return;
    }

    if (tableWrapper) tableWrapper.style.display = 'block';
    if (bottomGrid) bottomGrid.style.display = 'grid';
    if (emptyView) emptyView.style.display = 'none';

    if (tableBody) {
        tableBody.innerHTML = cart.map(item => {
            const itemTotal = (item.price || 0) * (item.quantity || 1);
            return `
                <tr>
                    <td class="col-product-cell">
                        <div class="cart-prod-flex">
                            <img src="${item.image}" alt="${escapeHtml(item.title)}" class="cart-prod-img" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=100';">
                            <div class="cart-prod-title" style="cursor: pointer;" onclick="goToBookDetail(${item.id})">${escapeHtml(item.title)}</div>
                        </div>
                    </td>
                    <td class="cart-cell-price">${formatVNCurrency(item.price)}</td>
                    <td class="cart-cell-qty">
                        <div class="cart-stepper">
                            <button type="button" class="btn-stepper" onclick="changeCartItemQty(${item.id}, -1)">-</button>
                            <input type="text" class="stepper-val" value="${item.quantity}" readonly>
                            <button type="button" class="btn-stepper" onclick="changeCartItemQty(${item.id}, 1)">+</button>
                        </div>
                    </td>
                    <td class="cart-cell-total">${formatVNCurrency(itemTotal)}</td>
                    <td class="cart-cell-action">
                        <button type="button" class="btn-cart-remove-row" onclick="removeFromCart(${item.id})" title="Xóa">✕</button>
                    </td>
                </tr>
            `;
        }).join('');
    }

    if (grandTotal) {
        grandTotal.textContent = formatVNCurrency(totalPrice);
    }
}

function changeCartItemQty(bookId, delta) {
    const item = cart.find(i => i.id == bookId);
    if (!item) return;

    const newQty = (parseInt(item.quantity) || 1) + delta;
    if (newQty <= 0) {
        const confirmRemove = confirm(`Bạn có chắc muốn xóa "${item.title}" khỏi giỏ hàng?`);
        if (confirmRemove) {
            removeFromCart(bookId);
        }
        return;
    }

    item.quantity = newQty;
    saveCart();
    updateCartUI();
}

// Điều chỉnh số lượng sách trên trang chi tiết
function adjustDetailQty(delta) {
    const input = document.getElementById('detailQuantityInput');
    if (!input) return;
    let current = parseInt(input.value) || 1;
    let max = parseInt(input.getAttribute('max')) || 999;
    let next = current + delta;
    if (next < 1) next = 1;
    if (next > max) next = max;
    input.value = next;
}

// Thêm vào giỏ từ trang chi tiết
function onDetailAddToCart() {
    const infoCol = document.querySelector('.detail-info-column');
    if (!infoCol) return;
    const bookId = parseInt(infoCol.getAttribute('data-id'));
    const qty = parseInt(document.getElementById('detailQuantityInput') ? document.getElementById('detailQuantityInput').value : 1) || 1;
    if (bookId) {
        addToCart(bookId, qty);
    }
}

// Mua ngay từ trang chi tiết
function onDetailBuyNow() {
    const infoCol = document.querySelector('.detail-info-column');
    if (!infoCol) return;
    const bookId = parseInt(infoCol.getAttribute('data-id'));
    const qty = parseInt(document.getElementById('detailQuantityInput') ? document.getElementById('detailQuantityInput').value : 1) || 1;
    if (bookId) {
        addToCart(bookId, qty);
        window.location.href = 'cart';
    }
}

function toggleCartDrawer() {
    toggleCartPopover();
}

function renderCartDrawerItems() {
    // Được duy trì cho tính tương thích
}

function updateItemQuantity(bookId, delta) {
    changeCartItemQty(bookId, delta);
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

function removeDiacritics(str) {
    if (!str) return '';
    return String(str).normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd')
        .replace(/Đ/g, 'd')
        .toLowerCase();
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

/* ==========================================================================
   9. HEADER NAVIGATION BAR & INFO MODALS HANDLERS (TRANG CHỦ, GIỚI THIỆU, DANH MỤC, BÁN CHẠY, KM, LIÊN HỆ)
   ========================================================================== */

function toggleNavCategoryDropdown(e) {
    if (e) e.stopPropagation();
    const dropdown = document.getElementById('navCategoryDropdown');
    if (!dropdown) return;
    const isShown = dropdown.classList.contains('show');
    if (isShown) {
        dropdown.classList.remove('show');
    } else {
        dropdown.classList.add('show');
    }
}

document.addEventListener('click', (e) => {
    const dropdown = document.getElementById('navCategoryDropdown');
    const wrapper = document.getElementById('navCategoryDropdownWrapper');
    if (dropdown && wrapper && !wrapper.contains(e.target)) {
        dropdown.classList.remove('show');
    }
});

function selectNavCategory(e, category) {
    if (e) {
        e.stopPropagation();
    }
    const dropdown = document.getElementById('navCategoryDropdown');
    if (dropdown) dropdown.classList.remove('show');

    // Điều hướng trực tiếp sang trang Danh mục riêng biệt (theo Ảnh mẫu 2)
    window.location.href = 'category?name=' + encodeURIComponent(category);
}

/**
 * Xử lý sắp xếp trên trang Category
 */
function onCategorySortChange(sortVal) {
    const grid = document.getElementById('categoryBookGrid');
    if (!grid) {
        const url = new URL(window.location.href);
        url.searchParams.set('sort', sortVal);
        window.location.href = url.toString();
        return;
    }

    const cards = Array.from(grid.querySelectorAll('.category-book-card'));
    if (!cards.length) return;

    cards.sort((a, b) => {
        const idA = parseInt(a.getAttribute('data-id') || '0', 10);
        const idB = parseInt(b.getAttribute('data-id') || '0', 10);
        const priceA = parseFloat(a.getAttribute('data-price') || '0');
        const priceB = parseFloat(b.getAttribute('data-price') || '0');
        const ratingA = parseFloat(a.getAttribute('data-rating') || '0');
        const ratingB = parseFloat(b.getAttribute('data-rating') || '0');
        const isBestsellerA = a.getAttribute('data-bestseller') === 'true';
        const isBestsellerB = b.getAttribute('data-bestseller') === 'true';

        if (sortVal === 'bestseller') {
            if (isBestsellerA && !isBestsellerB) return -1;
            if (!isBestsellerA && isBestsellerB) return 1;
            return idB - idA;
        } else if (sortVal === 'price_asc') {
            return priceA - priceB;
        } else if (sortVal === 'price_desc') {
            return priceB - priceA;
        } else if (sortVal === 'rating') {
            return ratingB - ratingA;
        } else {
            // newest
            return idB - idA;
        }
    });

    cards.forEach(card => grid.appendChild(card));
}

/**
 * Khởi tạo trang Category chuyên biệt (hỗ trợ cả server-side và static client-side)
 */
function initCategoryPage() {
    const pageTitleEl = document.getElementById('categoryPageTitle');
    const pageCountEl = document.getElementById('categoryPageCount');
    const bookGrid = document.getElementById('categoryBookGrid');
    const breadcrumb = document.getElementById('categoryBreadcrumb');
    if (!pageTitleEl || !bookGrid) return;

    const urlParams = new URLSearchParams(window.location.search);
    const catName = urlParams.get('name') || urlParams.get('category') || 'Tất cả';

    if (pageTitleEl.textContent.includes('${CATEGORY_TITLE}') || pageTitleEl.textContent.trim() === '') {
        pageTitleEl.textContent = catName;
    }
    if (breadcrumb && (breadcrumb.textContent.includes('${CATEGORY_TITLE}') || breadcrumb.textContent.trim() === '')) {
        breadcrumb.textContent = catName;
    }
    document.title = (pageTitleEl.textContent || catName) + ' - Bookora | Tiệm Sách Tri Thức';

    // Nếu các thẻ sách chưa được render (vd mở trực tiếp file static category.html)
    if (!bookGrid.querySelector('.category-book-card') || bookGrid.innerHTML.includes('${CATEGORY_BOOK_GRID}')) {
        fetch(`/api/books?category=${encodeURIComponent(catName)}`)
            .then(res => res.json())
            .then(books => {
                if (pageCountEl) {
                    pageCountEl.innerHTML = `(Tìm thấy <strong style="color: #e05828;">${books.length}</strong> cuốn sách)`;
                }
                if (!books.length) {
                    bookGrid.innerHTML = `
                        <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px;">
                            <i class="fas fa-book-open" style="font-size: 48px; color: #cbd5e1; margin-bottom: 16px;"></i>
                            <h3 style="color: #475569; font-size: 18px; margin-bottom: 8px;">Chưa có sách thuộc danh mục này</h3>
                            <p style="color: #94a3b8; font-size: 14px;">Vui lòng chọn danh mục khác hoặc quay lại trang chủ.</p>
                            <a href="home" style="display: inline-block; margin-top: 16px; padding: 10px 20px; background: #e05828; color: white; border-radius: 6px; text-decoration: none; font-weight: 600;">Quay lại Trang Chủ</a>
                        </div>
                    `;
                    return;
                }
                let html = '';
                books.forEach(b => {
                    const outBadge = b.isOutOfStock ? '<div class="category-book-out-badge">Hết hàng</div>' : '';
                    const origPrice = (b.originalPrice && b.originalPrice > b.price) 
                        ? `<span class="category-book-original-price">${formatVNCurrency(b.originalPrice)}</span>` 
                        : '';
                    html += `
                        <div class="category-book-card" data-id="${b.id}" data-title="${b.title}" data-price="${b.price}" data-rating="${b.rating}" data-bestseller="${b.isBestSeller}" data-stock="${b.stock}" onclick="window.location.href='book?id=${b.id}'">
                            <div class="category-book-cover-wrap">
                                <img src="${b.image}" alt="${b.title}" class="category-book-cover" loading="lazy" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';">
                                ${outBadge}
                            </div>
                            <div class="category-book-info">
                                <h3 class="category-book-title" title="${b.title}">${b.title}</h3>
                                <p class="category-book-author">${b.author}</p>
                                <div class="category-book-price-row">
                                    <span class="category-book-price">${b.formattedPrice || formatVNCurrency(b.price)}</span>
                                    ${origPrice}
                                </div>
                            </div>
                        </div>
                    `;
                });
                bookGrid.innerHTML = html;
            })
            .catch(() => {});
    }
}

/**
 * Kiểm tra URL param khi trang chủ tải
 */
function checkUrlCategoryParam() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('category')) {
        const cat = urlParams.get('category');
        if (cat) {
            setTimeout(() => {
                selectNavCategory(null, cat);
            }, 100);
        }
    }
}

function handleNavBestSellers() {
    const sortSelect = document.getElementById('sortFilter') || document.getElementById('filterSort');
    const catalogEl = document.getElementById('catalog') || document.querySelector('.catalog-section');
    
    if (sortSelect) {
        // Đang ở trang chủ
        sortSelect.value = 'rating_desc';
        onFilterChange();
        if (catalogEl) catalogEl.scrollIntoView({ behavior: 'smooth' });
        showToast('🔥 Đang hiển thị danh sách <strong>Sách Bán Chạy Nhất</strong> tại Bookora!');
    } else {
        // Đang ở trang khác
        window.location.href = 'home?sort=bestseller#catalog';
    }
}

/* Modals Giới Thiệu */
function openAboutModal() {
    const modal = document.getElementById('aboutModal');
    const backdrop = document.getElementById('navInfoModalBackdrop');
    if (modal && backdrop) {
        backdrop.classList.add('active');
        modal.classList.add('active');
    }
}

function closeAboutModal() {
    const modal = document.getElementById('aboutModal');
    const backdrop = document.getElementById('navInfoModalBackdrop');
    if (modal && backdrop) {
        modal.classList.remove('active');
        backdrop.classList.remove('active');
    }
}

/* Modals Khuyến Mãi */
function openPromotionModal() {
    const modal = document.getElementById('promotionModal');
    const backdrop = document.getElementById('navInfoModalBackdrop');
    if (modal && backdrop) {
        backdrop.classList.add('active');
        modal.classList.add('active');
    }
}

function closePromotionModal() {
    const modal = document.getElementById('promotionModal');
    const backdrop = document.getElementById('navInfoModalBackdrop');
    if (modal && backdrop) {
        modal.classList.remove('active');
        backdrop.classList.remove('active');
    }
}

function copyPromoCode(code) {
    if (!code) return;
    navigator.clipboard.writeText(code).then(() => {
        showToast(`🎉 Đã sao chép mã ưu đãi <strong>${code}</strong> vào bộ nhớ tạm!`);
    }).catch(() => {
        showToast(`Mã ưu đãi của bạn: <strong>${code}</strong>`);
    });
}

/* Modals Liên Hệ */
function openContactModal() {
    const modal = document.getElementById('contactModal');
    const backdrop = document.getElementById('navInfoModalBackdrop');
    if (modal && backdrop) {
        backdrop.classList.add('active');
        modal.classList.add('active');
    }
}

function closeContactModal() {
    const modal = document.getElementById('contactModal');
    const backdrop = document.getElementById('navInfoModalBackdrop');
    if (modal && backdrop) {
        modal.classList.remove('active');
        backdrop.classList.remove('active');
    }
}

function handleSendContact(e) {
    if (e) e.preventDefault();
    const nameInput = document.getElementById('contactName');
    const emailInput = document.getElementById('contactEmail');
    const messageInput = document.getElementById('contactMessage');

    const name = nameInput ? nameInput.value.trim() : '';
    if (!name) {
        showToast('⚠️ Vui lòng nhập họ và tên của bạn!');
        return false;
    }

    if (nameInput) nameInput.value = '';
    if (emailInput) emailInput.value = '';
    if (messageInput) messageInput.value = '';

    closeContactModal();
    showToast(`💌 Cảm ơn bạn <strong>${name}</strong>! Bookora đã nhận được tin nhắn và sẽ phản hồi qua email/SĐT sớm nhất.`);
    return false;
}

function closeAllInfoModals() {
    closeAboutModal();
    closePromotionModal();
    closeContactModal();
}

/* ==========================================================================
   CATEGORY CATALOG PAGE INTERACTION LOGIC
   ========================================================================== */
function filterCategoryStatus(btn, status) {
    if (!btn) return;
    
    // Cập nhật trạng thái active cho nút lọc
    const container = btn.closest('.status-pills-list');
    if (container) {
        container.querySelectorAll('.status-pill').forEach(b => b.classList.remove('active'));
    }
    btn.classList.add('active');

    const cards = document.querySelectorAll('#bookGrid .book-card');
    let visibleCount = 0;

    cards.forEach(card => {
        const isOutOfStock = card.classList.contains('is-out-of-stock') || card.getAttribute('data-is-out-of-stock') === 'true';
        const hasBestseller = card.querySelector('.badge-bestseller') !== null;
        const hasDiscount = card.querySelector('.badge-discount') !== null;
        
        let match = false;
        if (status === 'all') {
            match = true;
        } else if (status === 'instock') {
            match = !isOutOfStock;
        } else if (status === 'bestseller') {
            match = hasBestseller;
        } else if (status === 'discount') {
            match = hasDiscount;
        }

        if (match) {
            card.style.display = '';
            visibleCount++;
        } else {
            card.style.display = 'none';
        }
    });

    // Cập nhật số lượng sách hiển thị
    const countEl = document.getElementById('categoryPageCount');
    if (countEl) {
        countEl.textContent = visibleCount;
    }
}

function onCategorySortChange(sortVal) {
    const urlParams = new URLSearchParams(window.location.search);
    const catName = urlParams.get('name') || urlParams.get('category') || 'Tất cả';
    urlParams.set('name', catName);
    urlParams.set('sort', sortVal);
    window.location.href = `category?${urlParams.toString()}`;
}

// Global window bindings to guarantee inline event attributes execute seamlessly
window.formatVNCurrency = formatVNCurrency;
window.formatMoney = formatMoney;
window.goToBookDetail = goToBookDetail;
window.quickViewBook = quickViewBook;
window.openQuickView = openQuickView;
window.closeQuickView = closeQuickView;
window.changeModalQty = changeModalQty;
window.addModalBookToCart = addModalBookToCart;
window.addModalItemToCart = addModalItemToCart;
window.buyNowFromModal = buyNowFromModal;
window.notifyOutOfStock = notifyOutOfStock;
window.notifyOutOfStockFromModal = notifyOutOfStockFromModal;
window.toggleChatbot = toggleChatbot;
window.closeChatbot = closeChatbot;
window.openChatbot = openChatbot;
window.sendChatbotPrompt = sendChatbotPrompt;
window.sendChatbotMessage = sendChatbotMessage;
window.toggleCartPopover = toggleCartPopover;
window.showCartPopover = showCartPopover;
window.showCartPopoverAuto = showCartPopoverAuto;
window.hideCartPopover = hideCartPopover;
window.addToCart = addToCart;
window.saveCart = saveCart;
window.updateCartUI = updateCartUI;
window.renderCartPopoverItems = renderCartPopoverItems;
window.removeFromCart = removeFromCart;
window.proceedToCheckout = proceedToCheckout;
window.checkoutCart = checkoutCart;
window.initCartPage = initCartPage;
window.renderCartPageUI = renderCartPageUI;
window.changeCartItemQty = changeCartItemQty;
window.updateItemQuantity = updateItemQuantity;
window.adjustDetailQty = adjustDetailQty;
window.onDetailAddToCart = onDetailAddToCart;
window.onDetailBuyNow = onDetailBuyNow;
window.showToast = showToast;
window.toggleNavCategoryDropdown = toggleNavCategoryDropdown;
window.handleNavBestSellers = handleNavBestSellers;
window.openAboutModal = openAboutModal;
window.closeAboutModal = closeAboutModal;
window.openPromotionModal = openPromotionModal;
window.closePromotionModal = closePromotionModal;
window.openContactModal = openContactModal;
window.closeContactModal = closeContactModal;
window.handleSendContact = handleSendContact;
window.closeAllInfoModals = closeAllInfoModals;
window.filterCategoryStatus = filterCategoryStatus;
window.onCategorySortChange = onCategorySortChange;
window.onFilterChange = onFilterChange;
window.onSortChange = onSortChange;
window.resetAllFilters = resetAllFilters;
window.copyPromoCode = copyPromoCode;
window.applyPromoSearch = function(code) {
    const inp = document.getElementById('searchInput');
    if (inp) { inp.value = code; }
    onFilterChange();
};
window.hideSuggestions = hideSuggestions;
