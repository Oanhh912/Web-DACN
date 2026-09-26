<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.util.ArrayList" %>
<%@ page import="com.bookstore.model.Book" %>
<%@ page import="com.bookstore.model.User" %>
<%@ page import="com.bookstore.data.DataStore" %>
<%
    String categoryTitle = (String) request.getAttribute("categoryTitle");
    if (categoryTitle == null || categoryTitle.trim().isEmpty()) {
        categoryTitle = request.getParameter("name");
        if (categoryTitle == null || categoryTitle.trim().isEmpty()) {
            categoryTitle = "Tất cả";
        }
    }
    
    List<Book> books = (List<Book>) request.getAttribute("books");
    if (books == null) {
        if ("Tất cả".equalsIgnoreCase(categoryTitle) || "Tất cả sách".equalsIgnoreCase(categoryTitle)) {
            books = DataStore.getAllBooks();
        } else {
            books = DataStore.searchBooks("", categoryTitle, null, null, null, null, "all");
        }
    }

    User currentUser = (User) session.getAttribute("currentUser");
    String currentSort = (String) request.getAttribute("currentSort");
    if (currentSort == null) currentSort = "newest";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= categoryTitle %> - Bookora | Tiệm Sách Tri Thức</title>
    <meta name="description" content="Khám phá các đầu sách thuộc danh mục <%= categoryTitle %> tại Bookora - Tiệm Sách Tri Thức.">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Playfair+Display:ital,wght@0,600;0,700;1,400&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css?v=20260926_02">
</head>
<body>
    <!-- 1. HEADER & NAVIGATION (ĐỒNG NHẤT VỚI TRANG CHỦ) -->
    <header class="main-header">
        <div class="top-bar">
            <div class="topbar-contact-info">
                <span><i class="fas fa-phone"></i> 028.73008182</span>
                <span><i class="fas fa-envelope"></i> hotro@bookora.com</span>
                <span><i class="fas fa-location-dot"></i> Số 25, ngõ 68 phố Cầu Giấy, phường Quan Hoa, quận Cầu Giấy, TP. Hà Nội</span>
            </div>
            <% if (currentUser != null) { %>
                <div class="topbar-auth-links">
                    <span class="topbar-welcome"><i class="fas fa-circle-user"></i> Xin chào, <strong><%= currentUser.getFullName() %></strong></span>
                    <span class="topbar-divider">|</span>
                    <a href="logout" class="topbar-auth-btn"><i class="fas fa-arrow-right-from-bracket"></i> ĐĂNG XUẤT</a>
                </div>
            <% } else { %>
                <div class="topbar-auth-links">
                    <span style="color: #cbd5e1;"><i class="fas fa-truck-fast"></i> Miễn phí vận chuyển từ 250.000 đ</span>
                </div>
            <% } %>
        </div>

        <div class="navbar-container">
            <!-- Logo -->
            <a href="home" class="site-logo">
                <div class="logo-icon">
                    <i class="fas fa-book-bookmark"></i>
                </div>
                <div class="logo-text">
                    <h1>Bookora</h1>
                    <span>Tiệm Sách Tri Thức</span>
                </div>
            </a>

            <!-- Thanh tìm kiếm sách -->
            <form action="home" method="GET" class="search-form" id="searchForm">
                <div class="search-input-wrap">
                    <input type="text" id="searchInput" name="q" class="search-input" 
                           placeholder="Tìm kiếm theo tựa sách, tác giả, NXB, mã sách (MS001)..." 
                           value="" autocomplete="off">
                    <button type="button" class="btn-search" id="btnSearchSubmit" title="Tìm kiếm">
                        Tìm kiếm
                    </button>
                </div>
                <div class="search-suggestions-dropdown" id="searchSuggestionsDropdown"></div>
            </form>

            <!-- Hotline tư vấn Bookora -->
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
                <div class="cart-dropdown-wrapper" id="cartDropdownWrapper">
                    <button type="button" class="btn-cart-toggle" id="btnCartToggle" onclick="toggleCartPopover(event)" title="Xem giỏ hàng">
                        <i class="fas fa-bag-shopping"></i>
                        <span class="cart-badge" id="cartBadge" style="display: none;">0</span>
                    </button>

                    <!-- Popover giỏ hàng -->
                    <div class="cart-popover" id="cartPopover">
                        <div class="cart-popover-caret"></div>
                        <div class="cart-popover-items" id="cartPopoverItems"></div>
                        <div class="cart-popover-divider"></div>
                        <div class="cart-popover-total-row">
                            <span class="cart-popover-total-label">TỔNG TIỀN:</span>
                            <span class="cart-popover-total-val" id="cartPopoverTotal">0đ</span>
                        </div>
                        <div class="cart-popover-actions">
                            <a href="cart" class="btn-popover-view-cart">XEM GIỎ HÀNG</a>
                            <button type="button" class="btn-popover-checkout" onclick="proceedToCheckout()">THANH TOÁN</button>
                        </div>
                    </div>
                </div>

                <% if (currentUser != null) { %>
                    <div class="user-dropdown">
                        <button type="button" class="user-profile-trigger" id="userMenuTrigger">
                            <img src="<%= (currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) ? currentUser.getAvatar() : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" %>" 
                                 alt="Avatar" class="user-avatar-img">
                            <div class="user-meta">
                                <div class="user-greeting">Xin chào,</div>
                                <div class="user-fullname"><%= currentUser.getFullName() %></div>
                            </div>
                            <span class="user-role-tag"><%= currentUser.getRole() %></span>
                            <i class="fas fa-chevron-down" style="font-size: 11px; color: var(--text-muted); margin-left: 4px;"></i>
                        </button>
                        <div class="user-menu-dropdown" id="userMenuDropdown">
                            <div class="dropdown-header-info">
                                <div style="font-weight: 700; font-size: 13px; color: var(--primary);"><%= currentUser.getFullName() %></div>
                                <div class="dropdown-email"><%= currentUser.getEmail() %></div>
                            </div>
                            <a href="profile" class="dropdown-item">
                                <i class="fas fa-user-circle"></i> Hồ sơ tài khoản
                            </a>
                            <a href="cart" class="dropdown-item">
                                <i class="fas fa-bag-shopping"></i> Giỏ hàng của tôi
                            </a>
                            <a href="logout" class="dropdown-item logout">
                                <i class="fas fa-arrow-right-from-bracket"></i> Đăng xuất
                            </a>
                        </div>
                    </div>
                <% } else { %>
                    <div class="guest-auth-buttons">
                        <a href="login" class="btn-guest btn-guest-login">
                            <i class="fas fa-arrow-right-to-bracket"></i>
                            <span>Đăng Nhập</span>
                        </a>
                        <a href="register" class="btn-guest btn-guest-register">
                            <i class="fas fa-user-plus"></i>
                            <span>Đăng Ký</span>
                        </a>
                    </div>
                <% } %>
            </div>
        </div>

        <!-- 2. THANH ĐIỀU HƯỚNG CHÍNH (HEADER NAVIGATION BAR) -->
        <nav class="main-nav-bar">
            <div class="nav-bar-container">
                <ul class="nav-menu-list">
                    <li class="nav-menu-item">
                        <a href="home" class="nav-menu-link">
                            <i class="fas fa-house"></i> TRANG CHỦ
                        </a>
                    </li>
                    <li class="nav-menu-item">
                        <a href="about" class="nav-menu-link">
                            <i class="fas fa-circle-info"></i> GIỚI THIỆU
                        </a>
                    </li>
                    <li class="nav-menu-item nav-dropdown-item" id="navCategoryDropdownWrapper">
                        <a href="javascript:void(0)" class="nav-menu-link nav-dropdown-toggle active" onclick="toggleNavCategoryDropdown(event)">
                            <i class="fas fa-list-ul"></i> DANH MỤC SÁCH <i class="fas fa-chevron-down nav-caret"></i>
                        </a>
                        <!-- Dropdown tổng hợp danh mục sách thực tế của Bookora -->
                        <div class="nav-category-dropdown" id="navCategoryDropdown">
                            <div class="nav-dropdown-header">
                                <span><i class="fas fa-layer-group"></i> Danh Mục Sách</span>
                            </div>
                            <div class="nav-category-vertical-list">
                                <a href="category?name=Tất cả" class="nav-cat-list-link"><i class="fas fa-border-all"></i> Tất cả sách</a>
                                <a href="category?name=Văn học" class="nav-cat-list-link"><i class="fas fa-book-open"></i> Văn học</a>
                                <a href="category?name=Kinh tế" class="nav-cat-list-link"><i class="fas fa-chart-line"></i> Kinh tế</a>
                                <a href="category?name=Kỹ năng sống" class="nav-cat-list-link"><i class="fas fa-seedling"></i> Kỹ năng sống</a>
                                <a href="category?name=Tâm lý học" class="nav-cat-list-link"><i class="fas fa-brain"></i> Tâm lý học</a>
                                <a href="category?name=Công nghệ thông tin" class="nav-cat-list-link"><i class="fas fa-laptop-code"></i> Công nghệ thông tin</a>
                                <a href="category?name=Tâm linh & Đời sống" class="nav-cat-list-link"><i class="fas fa-spa"></i> Tâm linh & Đời sống</a>
                                <a href="category?name=Khoa học" class="nav-cat-list-link"><i class="fas fa-atom"></i> Khoa học</a>
                                <a href="category?name=Trinh thám" class="nav-cat-list-link"><i class="fas fa-mask"></i> Trinh thám & Bí ẩn</a>
                            </div>
                        </div>
                    </li>
                    <li class="nav-menu-item">
                        <a href="home?sort=bestseller#catalog" class="nav-menu-link">
                            <i class="fas fa-fire" style="color: #ef4444;"></i> SÁCH BÁN CHẠY
                        </a>
                    </li>
                    <li class="nav-menu-item">
                        <a href="promotions" class="nav-menu-link">
                            <i class="fas fa-gift" style="color: #f59e0b;"></i> KHUYẾN MÃI
                            <span class="nav-badge-hot">HOT</span>
                        </a>
                    </li>
                    <li class="nav-menu-item">
                        <a href="contact" class="nav-menu-link">
                            <i class="fas fa-headset" style="color: #06b6d4;"></i> LIÊN HỆ
                        </a>
                    </li>
                </ul>
            </div>
        </nav>
    </header>

    <!-- 2. BREADCRUMBS -->
    <nav class="breadcrumb-bar" aria-label="Breadcrumb">
        <div class="container breadcrumb-inner">
            <a href="home" class="bc-link"><i class="fas fa-house"></i> Trang chủ</a>
            <span class="bc-sep"><i class="fas fa-chevron-right"></i></span>
            <a href="category?name=Tất cả" class="bc-link">Danh mục sách</a>
            <span class="bc-sep"><i class="fas fa-chevron-right"></i></span>
            <span class="bc-current" id="categoryBreadcrumb"><%= categoryTitle %></span>
        </div>
    </nav>

    <!-- 3. MAIN CATALOG SECTION -->
    <main class="catalog-section" style="padding-top: 10px; padding-bottom: 60px; min-height: 550px;">
        <div class="container">
            <!-- Header Chuyên Mục: Tiêu đề bên trái & Sắp xếp bên phải -->
            <div class="category-clean-header">
                <h1 class="category-clean-title" id="categoryPageTitle"><%= categoryTitle %></h1>
                <div class="category-sort-wrap">
                    <label for="categorySortSelect"><i class="fas fa-arrow-down-short-wide"></i> Sắp xếp:</label>
                    <select id="categorySortSelect" class="category-sort-select" onchange="onCategorySortChange(this.value)">
                        <option value="newest" <%= "newest".equals(currentSort) ? "selected" : "" %>>Mới nhất</option>
                        <option value="bestseller" <%= "bestseller".equals(currentSort) ? "selected" : "" %>>Bán chạy nhất</option>
                        <option value="price_asc" <%= "price_asc".equals(currentSort) ? "selected" : "" %>>Giá: Thấp đến Cao</option>
                        <option value="price_desc" <%= "price_desc".equals(currentSort) ? "selected" : "" %>>Giá: Cao đến Thấp</option>
                        <option value="rating" <%= "rating".equals(currentSort) ? "selected" : "" %>>Đánh giá cao nhất</option>
                    </select>
                </div>
            </div>

            <!-- Danh sách hiển thị các thẻ sách chuẩn đồng nhất với Trang Chủ -->
            <div class="book-grid" id="bookGrid">
                <% if (books.isEmpty()) { %>
                    <div class="no-books-found">
                        <div class="empty-icon-wrap"><i class="fas fa-book-open"></i></div>
                        <h3 class="empty-title">Chưa có cuốn sách nào trong danh mục này</h3>
                        <p class="empty-desc">Hệ thống đang tiếp tục cập nhật các đầu sách mới nhất cho chuyên mục <%= categoryTitle %>.</p>
                        <div class="empty-ai-action">
                            <a href="home" class="btn-empty-reset"><i class="fas fa-arrow-left"></i> Quay lại Trang Chủ</a>
                        </div>
                    </div>
                <% } else { %>
                    <% for (Book b : books) { %>
                        <% 
                            boolean outOfStock = b.isOutOfStock();
                            String cardClasses = outOfStock ? "book-card is-out-of-stock" : "book-card";
                            String stockBadge = outOfStock
                                ? "<span class=\"badge-tag badge-stock badge-outofstock\"><i class=\"fas fa-ban\"></i> Hết hàng</span>"
                                : "<span class=\"badge-tag badge-stock badge-instock\"><i class=\"fas fa-check\"></i> Còn " + b.getStock() + "</span>";
                            String bestsellerBadge = b.isBestSeller() ? "<span class=\"badge-tag badge-bestseller\">Bán chạy</span>" : "";
                            String discountBadge = (b.getDiscountPercent() > 0) 
                                ? "<span class=\"badge-tag badge-discount\">-" + b.getDiscountPercent() + "%</span>" 
                                : "";
                        %>
                        <div class="<%= cardClasses %>" 
                             data-id="<%= b.getId() %>" 
                             data-code="<%= b.getCode() %>" 
                             data-title="<%= b.getTitle() %>" 
                             data-author="<%= b.getAuthor() %>" 
                             data-publisher="<%= b.getPublisher() %>" 
                             data-price="<%= (long) b.getPrice() %>" 
                             data-original-price="<%= (long) b.getOriginalPrice() %>" 
                             data-formatted-price="<%= b.getFormattedPrice() %>" 
                             data-category="<%= b.getCategory() %>" 
                             data-stock="<%= b.getStock() %>" 
                             data-is-out-of-stock="<%= outOfStock %>" 
                             data-promotion="<%= b.getPromotion() %>" 
                             data-image="<%= b.getImage() %>" 
                             data-desc="<%= b.getDescription() %>" 
                             data-rating="<%= b.getRating() %>">
                            <div class="book-card-inner">
                                <div class="book-cover-wrap">
                                    <img src="<%= b.getImage() %>" alt="<%= b.getTitle() %>" class="book-cover" loading="lazy" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';">
                                    <div class="badge-container">
                                        <%= stockBadge %>
                                        <%= bestsellerBadge %>
                                        <%= discountBadge %>
                                    </div>
                                    <div class="book-actions-overlay">
                                        <button type="button" class="btn-quickview" onclick="openQuickView(<%= b.getId() %>)"><i class="fas fa-eye"></i> Xem chi tiết</button>
                                    </div>
                                </div>
                                <div class="book-info">
                                    <div class="book-meta-top">
                                        <span class="book-category"><%= b.getCategory() %></span>
                                        <span class="book-code" title="Mã sách: <%= b.getCode() %>"><i class="fas fa-barcode"></i> <%= b.getCode() %></span>
                                    </div>
                                    <h3 class="book-title" title="<%= b.getTitle() %>" onclick="goToBookDetail(<%= b.getId() %>)"><%= b.getTitle() %></h3>
                                    <p class="book-author" title="Tác giả"><i class="fas fa-feather-alt"></i> <%= b.getAuthor() %></p>
                                    <p class="book-publisher" title="Nhà xuất bản"><i class="fas fa-building-columns"></i> <%= b.getPublisher() %></p>
                                    <div class="book-rating-row">
                                        <div class="stars"><i class="fas fa-star"></i> <span><%= b.getRating() %></span></div>
                                        <span class="review-count">(<%= b.getReviewCount() %> đánh giá)</span>
                                        <span class="stock-pill <%= outOfStock ? "stock-pill-empty" : "stock-pill-ok" %>"><%= outOfStock ? "Hết hàng" : "Kho: " + b.getStock() %></span>
                                    </div>
                                    <div class="book-price-row">
                                        <div class="price-box">
                                            <span class="price-current"><%= b.getFormattedPrice() %></span>
                                            <% if (b.getOriginalPrice() > b.getPrice()) { %>
                                                <span class="price-original"><%= b.getFormattedOriginalPrice() %></span>
                                            <% } %>
                                        </div>
                                        <% if (outOfStock) { %>
                                            <button type="button" class="btn-add-cart disabled" onclick="notifyOutOfStock('<%= b.getTitle() %>')" title="Sách đã hết hàng trong kho"><i class="fas fa-bell"></i></button>
                                        <% } else { %>
                                            <button type="button" class="btn-add-cart" onclick="addToCart(<%= b.getId() %>)" title="Thêm vào giỏ"><i class="fas fa-cart-plus"></i></button>
                                        <% } %>
                                    </div>
                                </div>
                            </div>
                        </div>
                    <% } %>
                <% } %>
            </div>
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

                    <!-- Thông tin Tác giả, Nhà xuất bản, Đánh giá (Grid) -->
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
                            <button type="button" class="btn-qty" onclick="changeModalQty(-1)"><i class="fas fa-minus"></i></button>
                            <input type="text" id="modalQty" class="qty-input" value="1" readonly>
                            <button type="button" class="btn-qty" onclick="changeModalQty(1)"><i class="fas fa-plus"></i></button>
                        </div>
                        <button type="button" class="btn-modal-cart btn-modal-add-cart" id="btnModalAddToCart" onclick="addModalBookToCart()">
                            <i class="fas fa-cart-plus"></i> Thêm Vào Giỏ Hàng
                        </button>
                        <button type="button" class="btn-modal-cart btn-modal-buy-now" id="btnModalBuyNow" style="background: #2c3e50;" onclick="buyNowFromModal()">
                            <i class="fas fa-bolt"></i> Mua Ngay
                        </button>
                        <button type="button" class="btn-modal-cart" id="btnModalNotify" style="display:none; background: #dc2626;" onclick="notifyOutOfStockFromModal()">
                            <i class="fas fa-bell"></i> Báo Khi Có Hàng Lại
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>



    <!-- 6. FOOTER (ĐỒNG NHẤT 100% VỚI TRANG CHỦ) -->
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
                    <li><a href="about">Giới thiệu nhà sách</a></li>
                    <li><a href="home#catalog">Hệ thống cửa hàng</a></li>
                    <li><a href="about">Tuyển dụng</a></li>
                    <li><a href="contact">Liên hệ hợp tác</a></li>
                </ul>
            </div>
            <div class="footer-col">
                <h4>Hỗ Trợ Khách Hàng</h4>
                <ul>
                    <li><a href="promotions">Chính sách đổi trả</a></li>
                    <li><a href="promotions">Phương thức vận chuyển</a></li>
                    <li><a href="cart">Hướng dẫn thanh toán</a></li>
                    <li><a href="about">Bảo mật thông tin</a></li>
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

    <!-- ==================== CÁC MODAL THÔNG TIN MÔ PHỎNG ==================== -->
    <div class="nav-info-modal-backdrop" id="navInfoModalBackdrop" onclick="closeAllInfoModals()"></div>

    <!-- Container Thông Báo Toast -->
    <div class="toast-container" id="toastContainer"></div>

    <!-- Scripts -->
    <script src="/js/app.js?v=20260926_10"></script>
</body>
</html>
