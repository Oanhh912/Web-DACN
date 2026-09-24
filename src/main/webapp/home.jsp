<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.bookstore.model.User, com.bookstore.model.Book, com.bookstore.data.DataStore, java.util.List" %>
<%
    User currentUser = (User) request.getAttribute("currentUser");
    boolean isLoggedIn = (currentUser != null);
    List<Book> books = (List<Book>) request.getAttribute("books");
    List<String> categories = (List<String>) request.getAttribute("categories");
    List<String> authors = (List<String>) request.getAttribute("authors");
    List<String> publishers = (List<String>) request.getAttribute("publishers");
    List<DataStore.PromotionItem> promotions = (List<DataStore.PromotionItem>) request.getAttribute("promotions");
    String currentCategory = (String) request.getAttribute("currentCategory");
    if (currentCategory == null) currentCategory = "Tất cả";
    String currentAuthor = (String) request.getAttribute("currentAuthor");
    if (currentAuthor == null) currentAuthor = "Tất cả";
    String currentPublisher = (String) request.getAttribute("currentPublisher");
    if (currentPublisher == null) currentPublisher = "Tất cả";
    String currentStockStatus = (String) request.getAttribute("currentStockStatus");
    if (currentStockStatus == null) currentStockStatus = "all";
    Double currentMinPrice = (Double) request.getAttribute("currentMinPrice");
    Double currentMaxPrice = (Double) request.getAttribute("currentMaxPrice");
    String searchKeyword = (String) request.getAttribute("searchKeyword");
    if (searchKeyword == null) searchKeyword = "";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trang Chủ - Bookora | Tiệm Sách Tri Thức</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

    <!-- 1. HEADER & NAVIGATION -->
    <header class="main-header">
        <div class="top-bar">
            <div class="topbar-contact-info">
                <span><i class="fas fa-phone"></i> 028.73008182</span>
                <span><i class="fas fa-envelope"></i> hotro@bookora.com</span>
                <span><i class="fas fa-location-dot"></i> Số 25, ngõ 68 phố Cầu Giấy, phường Quan Hoa, quận Cầu Giấy, TP. Hà Nội</span>
            </div>
            <div class="topbar-auth-links">
            <% if (isLoggedIn) { %>
                <span class="topbar-welcome"><i class="fas fa-circle-user"></i> Xin chào, <strong><%= currentUser.getFullName() %></strong></span>
                <span class="topbar-divider">|</span>
                <a href="${pageContext.request.contextPath}/logout" class="topbar-auth-btn"><i class="fas fa-arrow-right-from-bracket"></i> ĐĂNG XUẤT</a>
            <% } else { %>
                <span style="color: #cbd5e1;"><i class="fas fa-truck-fast"></i> Miễn phí vận chuyển từ 250.000 đ</span>
            <% } %>
            </div>
        </div>

        <div class="navbar-container">
            <!-- Logo -->
            <a href="${pageContext.request.contextPath}/home" class="site-logo">
                <div class="logo-icon">
                    <i class="fas fa-book-bookmark"></i>
                </div>
                <div class="logo-text">
                    <h1>Bookora</h1>
                    <span>Tiệm Sách Tri Thức</span>
                </div>
            </a>

            <!-- Thanh tìm kiếm sách với Auto-suggest (Luồng 1) -->
            <form action="${pageContext.request.contextPath}/home" method="GET" class="search-form" id="searchForm">
                <div class="search-input-wrap">
                    <input type="text" id="searchInput" name="q" class="search-input" 
                           placeholder="Tìm kiếm theo tựa sách, tác giả, NXB, mã sách..." 
                           value="<%= searchKeyword %>" autocomplete="off">
                    <button type="submit" class="btn-search" title="Tìm kiếm">
                        <i class="fas fa-search"></i>
                    </button>
                    <!-- Dropdown gợi ý tìm kiếm tức thì theo Use Case Luồng cơ bản (1) -->
                    <div class="search-suggestions-dropdown" id="searchSuggestionsDropdown" style="display:none;"></div>
                </div>
            </form>

            <!-- Hotline tư vấn phong cách Vinabook -->
            <div class="header-hotline">
                <div class="hotline-icon">
                    <i class="fas fa-phone-volume"></i>
                </div>
                <div class="hotline-text">
                    <small>Tư vấn bán hàng</small>
                    <strong>028.73008182</strong>
                </div>
            </div>

            <!-- Khối hành động: Giỏ hàng & User Profile / Đăng nhập Đăng ký -->
            <div class="header-actions">
                <!-- Nút mở giỏ hàng -->
                <button type="button" class="btn-cart-toggle" onclick="toggleCartDrawer()" title="Xem giỏ hàng">
                    <i class="fas fa-bag-shopping"></i>
                    <span class="cart-badge" id="cartBadge" style="display: none;">0</span>
                </button>

                <% if (isLoggedIn) { %>
                <!-- Menu người dùng đã đăng nhập -->
                <div class="user-dropdown">
                    <button type="button" class="user-profile-trigger" id="userMenuTrigger">
                        <img src="<%= currentUser.getAvatar() %>" alt="Avatar" class="user-avatar-img" onerror="this.src='https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150';">
                        <div class="user-meta">
                            <div class="user-greeting">Xin chào,</div>
                            <div class="user-fullname"><%= currentUser.getFullName() %></div>
                        </div>
                        <span class="user-role-tag"><%= currentUser.getRole() %></span>
                        <i class="fas fa-chevron-down" style="font-size: 11px; color: var(--text-muted); margin-left: 4px;"></i>
                    </button>

                    <!-- Dropdown danh mục cá nhân -->
                    <div class="user-menu-dropdown" id="userMenuDropdown">
                        <div class="dropdown-header-info">
                            <div style="font-weight: 700; font-size: 13px; color: var(--primary);"><%= currentUser.getFullName() %></div>
                            <div class="dropdown-email"><%= currentUser.getEmail() != null ? currentUser.getEmail() : "" %></div>
                        </div>
                        <a href="${pageContext.request.contextPath}/profile" class="dropdown-item">
                            <i class="fas fa-user-circle"></i> Hồ sơ tài khoản
                        </a>
                        <a href="javascript:void(0)" class="dropdown-item" onclick="toggleCartDrawer()">
                            <i class="fas fa-box-archive"></i> Đơn hàng của tôi
                        </a>
                        <a href="javascript:void(0)" class="dropdown-item" onclick="alert('Danh sách yêu thích đang được đồng bộ!')">
                            <i class="fas fa-heart"></i> Sách yêu thích
                        </a>
                        <a href="${pageContext.request.contextPath}/logout" class="dropdown-item logout">
                            <i class="fas fa-arrow-right-from-bracket"></i> Đăng xuất
                        </a>
                    </div>
                </div>
                <% } else { %>
                <div class="guest-auth-buttons">
                    <a href="${pageContext.request.contextPath}/login" class="btn-guest btn-guest-login">
                        <i class="fas fa-arrow-right-to-bracket"></i>
                        <span>Đăng Nhập</span>
                    </a>
                    <a href="${pageContext.request.contextPath}/register" class="btn-guest btn-guest-register">
                        <i class="fas fa-user-plus"></i>
                        <span>Đăng Ký</span>
                    </a>
                </div>
                <% } %>
            </div>
        </div>
    </header>

    <!-- 2. HERO BANNER -->
    <section class="hero-section">
        <div class="hero-card">
            <div class="hero-content">
                <span class="hero-tag"><i class="fas fa-sparkles"></i> Sự Kiện Văn Hóa Đọc 2026</span>
                <h2 class="hero-title">Khám Phá Tri Thức,<br><span>Nâng Tầm Tương Lai</span></h2>
                <p class="hero-desc">
                    Tuyển tập hơn 100 tác phẩm kinh điển và hiện đại được độc giả yêu thích nhất. Ưu đãi đến 30% cùng quà tặng sổ tay độc quyền dành cho <%= isLoggedIn ? "thành viên <strong>" + currentUser.getFullName() + "</strong>" : "quý độc giả và thành viên mới" %>.
                </p>
                <a href="#catalog" class="hero-cta">
                    <span>Xem Ngay Bộ Sưu Tập</span>
                    <i class="fas fa-arrow-down"></i>
                </a>
            </div>
            <div class="hero-illustration">
                <i class="fas fa-book-open-reader"></i>
            </div>
        </div>
    </section>

    <!-- Thanh Thông Tin Khuyến Mãi (Tích hợp từ KHUYEN_MAI) -->
    <% if (promotions != null && !promotions.isEmpty()) { %>
    <div class="promo-ticker-wrap">
        <div class="promo-ticker-inner">
            <div class="promo-ticker-badge"><i class="fas fa-bolt"></i> ƯU ĐÃI NỔI BẬT:</div>
            <div class="promo-ticker-items">
                <% for (DataStore.PromotionItem p : promotions) { %>
                    <span class="promo-pill"><i class="fas fa-tag"></i> <strong><%= p.title %></strong> - <%= p.description %> (<%= p.discountText %>)</span>
                <% } %>
            </div>
        </div>
    </div>
    <% } %>

    <!-- 3. BOOK CATALOG & CATEGORIES -->
    <main class="catalog-section" id="catalog">
        <div class="section-header">
            <div class="section-title-wrap">
                <h2>Tủ Sách Nổi Bật Dành Cho Bạn</h2>
                <p>Tìm thấy <strong id="bookCountDisplay" style="color: var(--primary);"><%= (books != null) ? books.size() : 0 %></strong> cuốn sách đặc sắc phù hợp cho bạn</p>
            </div>
        </div>

        <!-- Bộ lọc phân loại danh mục (Pills) -->
        <div class="category-filter-bar" id="categoryFilterBar">
            <% if (categories != null) {
                for (String cat : categories) {
                    boolean active = cat.equalsIgnoreCase(currentCategory);
            %>
                <button class="category-pill <%= active ? "active" : "" %>" data-category="<%= cat %>">
                    <%= cat %>
                </button>
            <%   }
               } %>
        </div>

        <!-- Thanh công cụ Bộ lọc nâng cao đa tiêu chí theo Use Case Luồng rẽ nhánh (3a) -->
        <div class="multi-filter-toolbar">
            <div class="filter-toolbar-top">
                <div class="filter-heading">
                    <i class="fas fa-filter"></i>
                    <span>Bộ Lọc Nâng Cao (Danh mục, Tác giả, NXB, Khoảng giá, Tồn kho)</span>
                </div>
                <button type="button" class="btn-reset-filters" onclick="resetAllFilters()" title="Đặt lại về mặc định">
                    <i class="fas fa-rotate-left"></i> Đặt lại bộ lọc
                </button>
            </div>
            <div class="filter-grid">
                <div class="filter-field">
                    <label class="filter-label"><i class="fas fa-feather-alt"></i> Tác giả (TAC_GIA)</label>
                    <select id="filterAuthor" class="filter-select" onchange="onFilterChange()">
                        <option value="Tất cả">Tất cả tác giả</option>
                        <% if (authors != null) {
                            for (String a : authors) {
                                boolean sel = a.equalsIgnoreCase(currentAuthor);
                        %>
                            <option value="<%= a %>" <%= sel ? "selected" : "" %>><%= a %></option>
                        <%   }
                           } %>
                    </select>
                </div>
                <div class="filter-field">
                    <label class="filter-label"><i class="fas fa-building-columns"></i> Nhà xuất bản (NHA_XUAT_BAN)</label>
                    <select id="filterPublisher" class="filter-select" onchange="onFilterChange()">
                        <option value="Tất cả">Tất cả nhà xuất bản</option>
                        <% if (publishers != null) {
                            for (String pub : publishers) {
                                boolean sel = pub.equalsIgnoreCase(currentPublisher);
                        %>
                            <option value="<%= pub %>" <%= sel ? "selected" : "" %>><%= pub %></option>
                        <%   }
                           } %>
                    </select>
                </div>
                <div class="filter-field">
                    <label class="filter-label"><i class="fas fa-money-bill-wave"></i> Khoảng giá bán</label>
                    <select id="filterPrice" class="filter-select" onchange="onFilterChange()">
                        <option value="all">Tất cả mức giá</option>
                        <option value="0-100000" <%= (currentMaxPrice != null && currentMaxPrice <= 100000) ? "selected" : "" %>>Dưới 100.000 đ</option>
                        <option value="100000-200000" <%= (currentMinPrice != null && currentMinPrice >= 100000 && currentMaxPrice != null && currentMaxPrice <= 200000) ? "selected" : "" %>>100.000 đ - 200.000 đ</option>
                        <option value="200000-300000" <%= (currentMinPrice != null && currentMinPrice >= 200000 && currentMaxPrice != null && currentMaxPrice <= 300000) ? "selected" : "" %>>200.000 đ - 300.000 đ</option>
                        <option value="300000-9999999" <%= (currentMinPrice != null && currentMinPrice >= 300000) ? "selected" : "" %>>Trên 300.000 đ</option>
                    </select>
                </div>
                <div class="filter-field">
                    <label class="filter-label"><i class="fas fa-boxes-stacked"></i> Tồn kho (Bảng KHO)</label>
                    <select id="filterStock" class="filter-select" onchange="onFilterChange()">
                        <option value="all" <%= "all".equals(currentStockStatus) ? "selected" : "" %>>Tất cả trạng thái kho</option>
                        <option value="in_stock" <%= "in_stock".equals(currentStockStatus) ? "selected" : "" %>>Còn hàng trong kho (Stock > 0)</option>
                        <option value="out_of_stock" <%= "out_of_stock".equals(currentStockStatus) ? "selected" : "" %>>Hết hàng (Stock = 0)</option>
                    </select>
                </div>
            </div>
        </div>

        <!-- Danh sách hiển thị các thẻ sách -->
        <div class="book-grid" id="bookGrid">
            <% if (books == null || books.isEmpty()) { %>
                <div class="no-books-found">
                    <div class="empty-icon-wrap"><i class="fas fa-book-open"></i></div>
                    <h3>Không tìm thấy sản phẩm phù hợp</h3>
                    <p>Rất tiếc, không có cuốn sách nào khớp với từ khóa hoặc tiêu chí lọc của bạn.</p>
                    <div class="empty-ai-suggestion">
                        <p><i class="fas fa-robot" style="color: var(--accent);"></i> Gợi ý: Bạn có thể thử tìm kiếm với từ khóa ngắn hơn hoặc hỏi Trợ lý AI của chúng tôi để được gợi ý sách tương đương!</p>
                        <button type="button" class="btn-empty-ai" onclick="openChatbot('Gợi ý sách tương tự từ khóa: <%= searchKeyword %>')">
                            <i class="fas fa-comments"></i> Hỏi Chatbot AI tư vấn ngay
                        </button>
                    </div>
                </div>
            <% } else {
                for (Book b : books) {
            %>
                <div class="book-card <%= b.isOutOfStock() ? "is-out-of-stock" : "" %>" 
                     data-id="<%= b.getId() %>" 
                     data-code="<%= b.getCode() %>" 
                     data-title="<%= b.getTitle().replace("\"", "&quot;") %>" 
                     data-author="<%= b.getAuthor().replace("\"", "&quot;") %>" 
                     data-publisher="<%= b.getPublisher() != null ? b.getPublisher().replace("\"", "&quot;") : "" %>"
                     data-price="<%= b.getPrice() %>" 
                     data-original-price="<%= b.getOriginalPrice() %>"
                     data-formatted-price="<%= b.getFormattedPrice() %>" 
                     data-category="<%= b.getCategory() %>" 
                     data-image="<%= b.getImage() %>" 
                     data-desc="<%= b.getDescription().replace("\"", "&quot;") %>" 
                     data-rating="<%= b.getRating() %>"
                     data-review-count="<%= b.getReviewCount() %>"
                     data-stock="<%= b.getStock() %>"
                     data-out-of-stock="<%= b.isOutOfStock() %>"
                     data-promotion="<%= b.getPromotion() != null ? b.getPromotion().replace("\"", "&quot;") : "" %>">
                    <div class="book-card-inner">
                        <div class="book-cover-wrap">
                            <img src="<%= b.getImage() %>" alt="<%= b.getTitle() %>" class="book-cover" loading="lazy" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';">
                            <div class="badge-container">
                                <% if (b.isOutOfStock()) { %>
                                    <span class="badge-tag badge-out-of-stock"><i class="fas fa-circle-exclamation"></i> Hết hàng</span>
                                <% } else { %>
                                    <span class="badge-tag badge-in-stock"><i class="fas fa-check"></i> Còn hàng</span>
                                <% } %>
                                <% if (b.isBestSeller()) { %>
                                    <span class="badge-tag badge-bestseller">Bán chạy</span>
                                <% } %>
                                <% if (b.getDiscountPercent() > 0) { %>
                                    <span class="badge-tag badge-discount">-<%= b.getDiscountPercent() %>%</span>
                                <% } %>
                            </div>
                            <div class="book-actions-overlay">
                                <button type="button" class="btn-quickview" onclick="openQuickView(<%= b.getId() %>)"><i class="fas fa-eye"></i> Xem nhanh</button>
                            </div>
                        </div>
                        <div class="book-info">
                            <div class="book-meta-top">
                                <span class="book-category"><%= b.getCategory() %></span>
                                <span class="book-code" title="Mã sách: <%= b.getCode() %>"><i class="fas fa-barcode"></i> <%= b.getCode() %></span>
                            </div>
                            <h3 class="book-title" title="<%= b.getTitle() %>"><%= b.getTitle() %></h3>
                            <p class="book-author"><i class="fas fa-feather-alt"></i> <%= b.getAuthor() %></p>
                            <p class="book-publisher-mini"><i class="fas fa-building-columns"></i> <%= b.getPublisher() != null ? b.getPublisher() : "NXB Tri Thức" %></p>
                            <div class="book-rating">
                                <div class="stars"><i class="fas fa-star"></i> <span><%= b.getRating() %></span></div>
                                <span class="review-count">(<%= b.getReviewCount() %> đánh giá)</span>
                            </div>
                            <div class="book-price-row">
                                <div class="price-box">
                                    <span class="price-current"><%= b.getFormattedPrice() %></span>
                                    <% if (b.getOriginalPrice() > 0) { %>
                                        <span class="price-original"><%= b.getFormattedOriginalPrice() %></span>
                                    <% } %>
                                </div>
                                <% if (b.isOutOfStock()) { %>
                                    <button type="button" class="btn-add-cart disabled" onclick="notifyOutOfStock('<%= b.getTitle().replace("'", "\\'") %>')" title="Hết hàng - Bấm để nhận thông báo">
                                        <i class="fas fa-bell"></i>
                                    </button>
                                <% } else { %>
                                    <button type="button" class="btn-add-cart" onclick="addToCart(<%= b.getId() %>)" title="Thêm vào giỏ">
                                        <i class="fas fa-cart-plus"></i>
                                    </button>
                                <% } %>
                            </div>
                        </div>
                    </div>
                </div>
            <%  }
               } %>
        </div>
    </main>

    <!-- 4. MODAL CHI TIẾT SÁCH (QUICK VIEW ĐẦY ĐỦ 9 THÔNG TIN THEO BA) -->
    <div class="modal-overlay" id="quickViewModal">
        <div class="modal-dialog">
            <button type="button" class="modal-close" onclick="closeQuickView()" title="Đóng">
                <i class="fas fa-times"></i>
            </button>
            <div class="modal-content-grid">
                <!-- 1. Ảnh bìa sách -->
                <div class="modal-book-cover">
                    <img src="" alt="Bìa sách" id="modalCover">
                </div>
                <!-- Chi tiết sách -->
                <div class="modal-details">
                    <div class="modal-meta-top">
                        <!-- 6. Danh mục -->
                        <span class="modal-category" id="modalCategory">Thể loại</span>
                        <span class="modal-code" id="modalCode"><i class="fas fa-barcode"></i> Mã: MS001</span>
                    </div>

                    <!-- 2. Tên sách -->
                    <h2 class="modal-title" id="modalTitle">Tựa Sách</h2>

                    <!-- 3. Giá bán & Giá gốc -->
                    <div class="modal-price-row">
                        <span class="modal-price" id="modalPrice">0 đ</span>
                        <span class="modal-price-orig" id="modalOriginalPrice" style="text-decoration: line-through; color: #94a3b8; font-size: 14px; margin-left: 10px;"></span>
                        <span class="modal-discount-tag" id="modalDiscountTag" style="display:none; margin-left: 8px; font-size: 11px; background: #f59e0b; color: white; padding: 2px 6px; border-radius: 4px; font-weight: 700;"></span>
                    </div>

                    <!-- Thông tin Tác giả, Nhà xuất bản, Đánh giá, Tồn kho (Grid) -->
                    <div class="modal-meta-grid">
                        <!-- 4. Tác giả -->
                        <div class="modal-meta-item">
                            <i class="fas fa-feather-alt"></i>
                            <span>Tác giả: <strong id="modalAuthor">Tác giả</strong></span>
                        </div>
                        <!-- 5. Nhà xuất bản -->
                        <div class="modal-meta-item">
                            <i class="fas fa-building-columns"></i>
                            <span>NXB: <strong id="modalPublisher">NXB Trẻ</strong></span>
                        </div>
                        <!-- 8. Đánh giá -->
                        <div class="modal-meta-item">
                            <i class="fas fa-star" style="color: #f59e0b;"></i>
                            <span>Đánh giá: <strong id="modalRating">5.0</strong> <span id="modalReviewCount" style="color: #64748b; font-size: 12px;">(120 đánh giá)</span></span>
                        </div>
                        <!-- 9. Tồn kho -->
                        <div class="modal-meta-item">
                            <i class="fas fa-boxes-stacked"></i>
                            <span>Tồn kho: <strong id="modalStockText">Còn hàng</strong></span>
                        </div>
                    </div>

                    <!-- Trạng thái kho & Khuyến mãi -->
                    <div class="modal-stock-badge-wrap">
                        <span class="modal-stock-badge in-stock" id="modalStockBadge">
                            <i class="fas fa-check-circle"></i> Còn hàng trong kho
                        </span>
                    </div>

                    <!-- Khuyến mãi đi kèm -->
                    <div class="modal-promo-box" id="modalPromoBox">
                        <i class="fas fa-gift"></i>
                        <span id="modalPromotion">Ưu đãi: Tặng kèm Bookmark độc quyền Bookora</span>
                    </div>

                    <!-- 7. Mô tả -->
                    <p class="modal-desc" id="modalDesc">Mô tả tóm tắt nội dung cuốn sách...</p>

                    <!-- Khối hành động Mua hàng -->
                    <div class="modal-actions" id="modalActionsWrap">
                        <div class="qty-control" id="modalQtyControl">
                            <button type="button" class="btn-qty" onclick="changeModalQty(-1)">-</button>
                            <input type="text" id="modalQty" class="qty-input" value="1" readonly>
                            <button type="button" class="btn-qty" onclick="changeModalQty(1)">+</button>
                        </div>
                        <button type="button" class="btn-modal-cart" id="btnModalAddToCart" onclick="addModalBookToCart()">
                            <i class="fas fa-cart-plus"></i> Thêm Vào Giỏ Hàng
                        </button>
                        <button type="button" class="btn-modal-cart" id="btnModalNotify" style="display:none; background: #dc2626;" onclick="notifyOutOfStockFromModal()">
                            <i class="fas fa-bell"></i> Báo Khi Có Hàng Lại
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 5. SLIDE-OUT MINI CART DRAWER -->
    <div class="cart-drawer-overlay" id="cartOverlay" onclick="toggleCartDrawer()"></div>
    <div class="cart-drawer" id="cartDrawer">
        <div class="cart-drawer-header">
            <h3><i class="fas fa-bag-shopping"></i> Giỏ Hàng Của Bạn</h3>
            <button type="button" class="btn-close-drawer" onclick="toggleCartDrawer()">
                <i class="fas fa-times"></i>
            </button>
        </div>
        <div class="cart-items-list" id="cartItemsList">
            <!-- Render các sản phẩm bằng JavaScript -->
        </div>
        <div class="cart-drawer-footer">
            <div class="cart-total-row">
                <span>Tổng tiền:</span>
                <span id="cartTotalPrice">0 đ</span>
            </div>
            <button type="button" class="btn-checkout" onclick="checkoutCart()">
                <span>Tiến Hành Đặt Hàng</span>
            </button>
        </div>
    </div>

    <!-- 6. CHATBOT AI WIDGET (BOOKORA AI ASSISTANT - Luồng 2a.2) -->
    <!-- Nút Floating Action Button mở Chatbot AI -->
    <button type="button" class="chatbot-fab" id="chatbotFab" onclick="toggleChatbot()" title="Trò chuyện với Trợ lý AI Bookora">
        <i class="fas fa-robot"></i>
        <span class="chatbot-fab-badge">AI</span>
    </button>

    <!-- Cửa sổ Chatbot AI -->
    <div class="chatbot-window" id="chatbotWindow">
        <div class="chatbot-header">
            <div class="chatbot-header-info">
                <div class="chatbot-avatar">
                    <i class="fas fa-brain"></i>
                </div>
                <div class="chatbot-title">
                    <h4>Bookora AI Assistant</h4>
                    <span>Trợ lý tư vấn sách 24/7</span>
                </div>
            </div>
            <button type="button" class="chatbot-close-btn" onclick="closeChatbot()" title="Đóng">
                <i class="fas fa-times"></i>
            </button>
        </div>
        <div class="chatbot-messages" id="chatbotMessages">
            <div class="chat-bubble chat-bubble-ai">
                Xin chào quý độc giả! 👋 Tôi là <strong>Bookora AI Assistant</strong>. Tôi có thể giúp bạn tìm kiếm sách theo thể loại, gợi ý sách theo tác giả, kiểm tra tình trạng tồn kho hoặc tư vấn khuyến mãi hot. Bạn đang tìm cuốn sách nào?
            </div>
        </div>
        <!-- Gợi ý câu hỏi nhanh -->
        <div class="chatbot-prompts-bar">
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Gợi ý sách văn học kinh điển')">📚 Sách Văn học</span>
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Tìm sách lập trình công nghệ')">💻 Sách Công nghệ</span>
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Sách tư duy tài chính kinh tế')">📈 Sách Kinh tế</span>
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Các chương trình khuyến mãi hiện có')">🎁 Khuyến mãi</span>
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Kiểm tra sách nào đang hết hàng?')">📦 Tồn kho</span>
        </div>
        <!-- Khung nhập tin nhắn -->
        <div class="chatbot-input-wrap">
            <input type="text" id="chatbotInput" class="chatbot-input" placeholder="Hỏi AI về sách, tác giả, giá tiền..." onkeydown="if(event.key==='Enter') sendChatbotMessage()">
            <button type="button" class="btn-chatbot-send" onclick="sendChatbotMessage()" title="Gửi tin nhắn">
                <i class="fas fa-paper-plane"></i>
            </button>
        </div>
    </div>

    <!-- 6. FOOTER -->
    <footer class="site-footer">
        <div class="footer-container">
            <div class="footer-col">
                <div class="site-logo" style="margin-bottom: 12px;">
                    <div class="logo-icon" style="background: white; color: var(--primary);">
                        <i class="fas fa-book-bookmark"></i>
                    </div>
                    <div class="logo-text">
                        <h1 style="color: white;">Bookora</h1>
                        <span style="color: #fde68a;">Tiệm Sách Tri Thức</span>
                    </div>
                </div>
                <p class="footer-desc">
                    Nơi lan tỏa tình yêu sách và nâng tầm tri thức Việt. Cam kết 100% sách có bản quyền từ các nhà xuất bản uy tín hàng đầu.
                </p>
                <div style="font-size: 13px; color: #94a3b8; margin-top: 10px; line-height: 1.6;">
                    <p style="margin: 4px 0;"><i class="fas fa-location-dot" style="color: var(--accent); margin-right: 6px;"></i> Số 25, ngõ 68 phố Cầu Giấy, phường Quan Hoa, quận Cầu Giấy, TP. Hà Nội</p>
                </div>
            </div>
            <div class="footer-col">
                <h4>Về Chúng Tôi</h4>
                <ul>
                    <li><a href="#catalog">Giới thiệu nhà sách</a></li>
                    <li><a href="#catalog">Hệ thống cửa hàng</a></li>
                    <li><a href="#catalog">Tuyển dụng</a></li>
                    <li><a href="#catalog">Liên hệ hợp tác</a></li>
                </ul>
            </div>
            <div class="footer-col">
                <h4>Hỗ Trợ Khách Hàng</h4>
                <ul>
                    <li><a href="#catalog">Chính sách đổi trả</a></li>
                    <li><a href="#catalog">Phương thức vận chuyển</a></li>
                    <li><a href="#catalog">Hướng dẫn thanh toán</a></li>
                    <li><a href="#catalog">Bảo mật thông tin</a></li>
                </ul>
            </div>
            <div class="footer-col">
                <h4>Kết Nối Với Chúng Tôi</h4>
                <p style="font-size: 14px; margin-bottom: 12px;">Nhận tin khuyến mãi mới nhất qua email:</p>
                <div style="display: flex; gap: 8px;">
                    <input type="email" placeholder="Email của bạn..." style="padding: 10px 14px; border-radius: 8px; border: 1px solid #334155; background: #1e293b; color: white; font-size: 13px; width: 100%; outline: none;">
                    <button type="button" style="background: var(--accent); color: white; border: none; padding: 0 14px; border-radius: 8px; cursor: pointer;" onclick="alert('Đã đăng ký nhận bản tin khuyến mãi!')">Gửi</button>
                </div>
            </div>
        </div>
        <div class="footer-bottom">
            <p>© 2026 Bookora - Đồ án chuyên ngành Web Bán Sách Java. Phát triển hoàn chỉnh cho trải nghiệm học tập và nghiên cứu.</p>
        </div>
    </footer>

    <script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
