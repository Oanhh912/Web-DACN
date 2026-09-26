<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.util.ArrayList" %>
<%@ page import="com.bookstore.model.Book" %>
<%@ page import="com.bookstore.model.User" %>
<%@ page import="com.bookstore.data.DataStore" %>
<%
    int bookId = 25; // Mặc định là sách Tháo Dây Oan Trái
    String idParam = request.getParameter("id");
    if (idParam != null && !idParam.trim().isEmpty()) {
        try {
            bookId = Integer.parseInt(idParam.trim());
        } catch (NumberFormatException ignored) {}
    }

    Book book = DataStore.getBookById(bookId);
    if (book == null) {
        book = DataStore.getBookById(25);
    }
    if (book == null) {
        List<Book> all = DataStore.getAllBooks();
        if (!all.isEmpty()) book = all.get(0);
    }

    User currentUser = (User) session.getAttribute("currentUser");
    List<Book> featuredBooks = DataStore.getFeaturedBooks(4);
    List<Book> relatedBooks = DataStore.getRelatedBooks(book != null ? book.getId() : 25, book != null ? book.getCategory() : "", 4);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= book != null ? book.getTitle() : "Chi Tiết Sách" %> - Bookora | Tiệm Sách Tri Thức</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css?v=20260925_03">
</head>
<body class="book-detail-page">

    <!-- 1. HEADER & NAVIGATION -->
    <header class="main-header">
        <div class="top-bar">
            <div class="topbar-contact-info">
                <span><i class="fas fa-phone"></i> 028.73008182</span>
                <span><i class="fas fa-envelope"></i> hotro@bookora.com</span>
                <span><i class="fas fa-location-dot"></i> Số 25, ngõ 68 phố Cầu Giấy, TP. Hà Nội</span>
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
                           placeholder="Tìm kiếm theo tựa sách, tác giả, NXB..." 
                           autocomplete="off">
                    <button type="submit" class="btn-search" id="btnSearchSubmit" title="Tìm kiếm">
                        Tìm kiếm
                    </button>
                </div>
                <div class="search-suggestions-dropdown" id="searchSuggestionsDropdown"></div>
            </form>

            <!-- Hotline tư vấn -->
            <div class="header-hotline">
                <div class="hotline-icon">
                    <i class="fas fa-phone-volume"></i>
                </div>
                <div class="hotline-text">
                    <small>Tư vấn bán hàng</small>
                    <strong>028.73008182</strong>
                </div>
            </div>

            <!-- Khối hành động: Giỏ hàng & User Profile -->
            <div class="header-actions">
                <!-- Nút mở giỏ hàng kèm Popover -->
                <div class="cart-dropdown-wrapper" id="cartDropdownWrapper">
                    <button type="button" class="btn-cart-toggle" id="btnCartToggle" onclick="toggleCartPopover(event)" title="Xem giỏ hàng">
                        <i class="fas fa-bag-shopping"></i>
                        <span class="cart-badge" id="cartBadge" style="display: none;">0</span>
                    </button>

                    <!-- Popover giỏ hàng -->
                    <div class="cart-popover" id="cartPopover">
                        <div class="cart-popover-caret"></div>
                        <div class="cart-popover-items" id="cartPopoverItems">
                            <!-- Render bằng JS -->
                        </div>
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
                            <img src="<%= currentUser.getAvatar() != null ? currentUser.getAvatar() : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" %>" alt="Avatar" class="user-avatar-img">
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
                            <a href="profile" class="dropdown-item"><i class="fas fa-user-circle"></i> Hồ sơ tài khoản</a>
                            <a href="cart" class="dropdown-item"><i class="fas fa-bag-shopping"></i> Giỏ hàng của tôi</a>
                            <a href="logout" class="dropdown-item logout"><i class="fas fa-arrow-right-from-bracket"></i> Đăng xuất</a>
                        </div>
                    </div>
                <% } else { %>
                    <div class="guest-auth-buttons">
                        <a href="login" class="btn-guest btn-guest-login"><i class="fas fa-arrow-right-to-bracket"></i> <span>Đăng Nhập</span></a>
                        <a href="register" class="btn-guest btn-guest-register"><i class="fas fa-user-plus"></i> <span>Đăng Ký</span></a>
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
                        <a href="javascript:void(0)" class="nav-menu-link nav-dropdown-toggle" onclick="toggleNavCategoryDropdown(event)">
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
                        <a href="javascript:void(0)" class="nav-menu-link" onclick="handleNavBestSellers()">
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

    <% if (book != null) { %>
    <!-- 2. BREADCRUMBS NAVIGATION -->
    <nav class="breadcrumb-bar" aria-label="Breadcrumb">
        <div class="container breadcrumb-inner">
            <a href="home" class="bc-link">Trang chủ</a>
            <span class="bc-sep">/</span>
            <a href="home?category=<%= book.getCategory() %>" class="bc-link"><%= book.getCategory() %></a>
            <span class="bc-sep">/</span>
            <span class="bc-current" title="<%= book.getTitle() %>"><%= book.getTitle() %></span>
        </div>
    </nav>

    <!-- 3. PHẦN 1: THÔNG TIN CHI TIẾT SÁCH (ẢNH 1) -->
    <main class="book-detail-main">
        <div class="container">
            <div class="detail-product-layout">
                
                <!-- Cột 1: Bìa sách & Thương hiệu NXB -->
                <div class="detail-cover-column">
                    <div class="publisher-brand-badge">
                        <span class="brand-tag"><i class="fas fa-certificate" style="color: #0284c7;"></i> <%= book.getPublisher() %></span>
                    </div>
                    <div class="detail-cover-card">
                        <img src="<%= book.getImage() %>" alt="<%= book.getTitle() %>" class="detail-cover-img" id="detailBookImage" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';">
                    </div>
                </div>

                <!-- Cột 2: Thông tin sách, Giá bán, Chọn số lượng & Nút Mua -->
                <div class="detail-info-column" data-id="<%= book.getId() %>" data-title="<%= book.getTitle() %>" data-price="<%= book.getPrice() %>" data-original-price="<%= book.getOriginalPrice() %>" data-image="<%= book.getImage() %>" data-stock="<%= book.getStock() %>">
                    <h1 class="product-detail-title"><%= book.getTitle() %></h1>
                    
                    <div class="product-detail-meta">
                        <span class="product-isbn">ISBN: <%= book.getCode() %></span>
                    </div>

                    <div class="product-detail-publisher">
                        <span class="publisher-text"><%= book.getPublisher() %></span>
                    </div>

                    <div class="product-detail-price-box">
                        <span class="detail-price-red"><%= book.getFormattedPrice() %></span>
                        <% if (book.getOriginalPrice() > book.getPrice()) { %>
                            <span class="detail-price-original"><%= book.getFormattedOriginalPrice() %></span>
                            <span class="detail-discount-badge">-<%= book.getDiscountPercent() %>%</span>
                        <% } %>
                    </div>

                    <!-- Khối chọn số lượng -->
                    <div class="product-quantity-selector">
                        <div class="quantity-stepper-box">
                            <button type="button" class="btn-stepper btn-minus" onclick="adjustDetailQty(-1)" aria-label="Giảm số lượng">-</button>
                            <input type="number" id="detailQuantityInput" class="stepper-input" value="1" min="1" max="<%= book.getStock() %>" readonly>
                            <button type="button" class="btn-stepper btn-plus" onclick="adjustDetailQty(1)" aria-label="Tăng số lượng">+</button>
                        </div>
                        <span class="stock-status-label <%= book.isOutOfStock() ? "out-of-stock" : "in-stock" %>"><%= book.getStockStatusText() %></span>
                    </div>

                    <!-- Nút Thêm Vào Giỏ & Mua Ngay -->
                    <div class="product-detail-actions">
                        <button type="button" class="btn-action-add-cart" id="btnDetailAddToCart" onclick="onDetailAddToCart()">
                            THÊM VÀO GIỎ
                        </button>
                        <button type="button" class="btn-action-buy-now" id="btnDetailBuyNow" onclick="onDetailBuyNow()">
                            MUA NGAY
                        </button>
                    </div>
                </div>

                <!-- Cột 3: Cam kết dịch vụ "Chỉ có ở Bookora" -->
                <div class="detail-sidebar-column">
                    <div class="bookora-service-card">
                        <div class="service-card-header">
                            <h3><i class="fas fa-book-bookmark" style="color: var(--primary); margin-right: 6px;"></i> Chỉ có ở Bookora</h3>
                        </div>
                        <div class="service-card-body">
                            <div class="service-item">
                                <div class="service-icon icon-teal">
                                    <i class="fas fa-certificate"></i>
                                </div>
                                <div class="service-desc">
                                    Sản phẩm 100% chính hãng
                                </div>
                            </div>

                            <div class="service-item">
                                <div class="service-icon icon-cyan">
                                    <i class="fas fa-headset"></i>
                                </div>
                                <div class="service-desc">
                                    Tư vấn mua sách trong giờ hành chính
                                </div>
                            </div>

                            <div class="service-item">
                                <div class="service-icon icon-orange">
                                    <i class="fas fa-truck-fast"></i>
                                </div>
                                <div class="service-desc">
                                    Miễn phí vận chuyển cho Đơn hàng từ 250.000đ
                                </div>
                            </div>

                            <div class="service-item">
                                <div class="service-icon icon-green">
                                    <i class="fas fa-phone-volume"></i>
                                </div>
                                <div class="service-desc">
                                    Hotline: 1900 6401 - 028.73008182
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>

        <!-- 4. PHẦN 2: GIỚI THIỆU SÁCH & SẢN PHẨM NỔI BẬT (ẢNH 2) -->
        <div class="container detail-content-container">
            <div class="detail-content-layout">
                
                <!-- Cột trái: GIỚI THIỆU SÁCH -->
                <section class="book-description-section">
                    <div class="section-title-line">
                        <h2 class="section-title-text">GIỚI THIỆU SÁCH</h2>
                        <div class="title-underline"></div>
                    </div>
                    <div class="book-description-text" id="bookDescriptionParagraph">
                        <%= book.getDescription() != null ? book.getDescription() : "Nội dung giới thiệu cuốn sách đang được cập nhật..." %>
                    </div>
                </section>

                <!-- Cột phải: SẢN PHẨM NỔI BẬT -->
                <aside class="featured-products-sidebar">
                    <div class="featured-title-wrap">
                        <h2 class="featured-title-text">SẢN PHẨM NỔI BẬT</h2>
                        <div class="featured-orange-bar"></div>
                    </div>
                    <div class="featured-products-list">
                        <% for (Book fb : featuredBooks) { %>
                            <div class="featured-book-item" onclick="window.location.href='book?id=<%= fb.getId() %>'">
                                <div class="featured-thumb-wrap">
                                    <img src="<%= fb.getImage() %>" alt="<%= fb.getTitle() %>" class="featured-thumb" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';">
                                    <% if (fb.isOutOfStock()) { %>
                                        <span class="featured-badge badge-black">Hết hàng</span>
                                    <% } else if (fb.getDiscountPercent() > 0) { %>
                                        <span class="featured-badge badge-red">-<%= fb.getDiscountPercent() %>%</span>
                                    <% } %>
                                </div>
                                <div class="featured-meta">
                                    <h4 class="featured-book-title" title="<%= fb.getTitle() %>"><%= fb.getTitle() %></h4>
                                    <div class="featured-price-row">
                                        <span class="featured-price-red"><%= fb.getFormattedPrice() %></span>
                                        <% if (fb.getOriginalPrice() > fb.getPrice()) { %>
                                            <span class="featured-price-orig"><%= fb.getFormattedOriginalPrice() %></span>
                                        <% } %>
                                    </div>
                                </div>
                            </div>
                        <% } %>
                    </div>
                </aside>

            </div>
        </div>

        <!-- 5. PHẦN 3: SẢN PHẨM LIÊN QUAN (ẢNH 3) -->
        <div class="container detail-related-container">
            <section class="related-products-section">
                <div class="related-header">
                    <h2 class="related-heading">Sản phẩm liên quan</h2>
                </div>
                <div class="related-products-grid">
                    <% for (Book rb : relatedBooks) { %>
                        <div class="related-book-card" onclick="window.location.href='book?id=<%= rb.getId() %>'">
                            <div class="related-cover-box">
                                <img src="<%= rb.getImage() %>" alt="<%= rb.getTitle() %>" class="related-cover-img" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';">
                                <div class="related-hover-actions" onclick="event.stopPropagation();">
                                    <button type="button" class="btn-hover-action" title="Xem nhanh" onclick="window.location.href='book?id=<%= rb.getId() %>'"><i class="fas fa-magnifying-glass-plus"></i></button>
                                    <button type="button" class="btn-hover-action" title="Thêm vào giỏ" onclick="addToCart(<%= rb.getId() %>, 1)"><i class="fas fa-cart-shopping"></i></button>
                                    <button type="button" class="btn-hover-action" title="Xem chi tiết" onclick="window.location.href='book?id=<%= rb.getId() %>'"><i class="fas fa-eye"></i></button>
                                </div>
                            </div>
                            <h3 class="related-book-title" title="<%= rb.getTitle() %>"><%= rb.getTitle() %></h3>
                            <div class="related-price-box">
                                <span class="related-price-red"><%= rb.getFormattedPrice() %></span>
                            </div>
                        </div>
                    <% } %>
                </div>
            </section>
        </div>
    </main>
    <% } %>

    <!-- FOOTER -->
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
                    <p style="margin: 4px 0;"><i class="fas fa-location-dot" style="color: var(--accent); margin-right: 6px;"></i> Số 25, ngõ 68 phố Cầu Giấy, TP. Hà Nội</p>
                    <p style="margin: 4px 0;"><i class="fas fa-phone" style="color: var(--accent); margin-right: 6px;"></i> Hotline: 1900 6401 - 028.73008182</p>
                </div>
            </div>
            <div class="footer-col">
                <h4>VỀ CHÚNG TÔI</h4>
                <ul>
                    <li><a href="home#catalog">Giới thiệu nhà sách</a></li>
                    <li><a href="home#catalog">Hệ thống cửa hàng</a></li>
                    <li><a href="home#catalog">Tuyển dụng</a></li>
                    <li><a href="home#catalog">Liên hệ hợp tác</a></li>
                </ul>
            </div>
            <div class="footer-col">
                <h4>HỖ TRỢ KHÁCH HÀNG</h4>
                <ul>
                    <li><a href="javascript:void(0)">Chính sách đổi trả</a></li>
                    <li><a href="javascript:void(0)">Phương thức vận chuyển</a></li>
                    <li><a href="javascript:void(0)">Hướng dẫn thanh toán</a></li>
                    <li><a href="javascript:void(0)">Bảo mật thông tin</a></li>
                </ul>
            </div>
            <div class="footer-col">
                <h4>KẾT NỐI VỚI CHÚNG TÔI</h4>
                <div class="footer-social-links" style="display: flex; gap: 10px; margin-bottom: 15px;">
                    <a href="javascript:void(0)" class="social-btn facebook" style="width: 36px; height: 36px; border-radius: 50%; background: #1e293b; color: #38bdf8; display: flex; align-items: center; justify-content: center;"><i class="fab fa-facebook-f"></i></a>
                    <a href="javascript:void(0)" class="social-btn youtube" style="width: 36px; height: 36px; border-radius: 50%; background: #1e293b; color: #f43f5e; display: flex; align-items: center; justify-content: center;"><i class="fab fa-youtube"></i></a>
                    <a href="javascript:void(0)" class="social-btn tiktok" style="width: 36px; height: 36px; border-radius: 50%; background: #1e293b; color: #e2e8f0; display: flex; align-items: center; justify-content: center;"><i class="fab fa-tiktok"></i></a>
                    <a href="javascript:void(0)" class="social-btn instagram" style="width: 36px; height: 36px; border-radius: 50%; background: #1e293b; color: #ec4899; display: flex; align-items: center; justify-content: center;"><i class="fab fa-instagram"></i></a>
                </div>
                <div style="font-size: 12px; color: #64748b;">
                    <span style="display: inline-block; padding: 4px 8px; background: #1e293b; border-radius: 4px; border: 1px solid #334155;"><i class="fas fa-shield-check" style="color: #10b981;"></i> Đã thông báo Bộ Công Thương</span>
                </div>
            </div>
        </div>
        <div class="footer-bottom">
            <p>© 2026 Bookora - Đồ án chuyên ngành Web Bán Sách Java. Bản quyền thuộc về Tiệm Sách Tri Thức.</p>
        </div>
    </footer>

    <!-- SLIDE-IN CART DRAWER -->
    <div class="cart-drawer-backdrop" id="cartDrawerBackdrop" onclick="toggleCartDrawer()"></div>
    <div class="cart-drawer" id="cartDrawer">
        <div class="cart-drawer-header">
            <h3><i class="fas fa-bag-shopping"></i> Giỏ Hàng Của Bạn (<span id="cartDrawerCount">0</span>)</h3>
            <button type="button" class="btn-close-drawer" onclick="toggleCartDrawer()">&times;</button>
        </div>
        <div class="cart-drawer-body" id="cartDrawerBody">
            <!-- Render danh sách qua app.js?v=20260926_10 -->
        </div>
        <div class="cart-drawer-footer">
            <div class="cart-drawer-total">
                <span>Tổng thành tiền:</span>
                <strong id="cartDrawerTotal" class="text-orange">0 đ</strong>
            </div>
            <div class="cart-drawer-actions">
                <a href="cart" class="btn-drawer-cart">Xem chi tiết giỏ hàng</a>
                <button type="button" class="btn-drawer-checkout" onclick="proceedToCheckout()">Thanh toán ngay</button>
            </div>
        </div>
    </div>

    <!-- TOAST NOTIFICATION -->
    <div class="toast-container" id="toastContainer"></div>

    <!-- CHATBOT AI WIDGET -->
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
        <div class="chatbot-prompts-bar">
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Gợi ý sách văn học kinh điển')">📚 Sách Văn học</span>
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Tìm sách lập trình công nghệ')">💻 Sách Công nghệ</span>
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Sách tư duy tài chính kinh tế')">📈 Sách Kinh tế</span>
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Các chương trình khuyến mãi hiện có')">🎁 Khuyến mãi</span>
            <span class="chat-prompt-chip" onclick="sendChatbotPrompt('Kiểm tra sách nào đang hết hàng?')">📦 Tồn kho</span>
        </div>
        <div class="chatbot-input-wrap">
            <input type="text" id="chatbotInput" class="chatbot-input" placeholder="Hỏi AI về sách, tác giả, giá tiền..." onkeydown="if(event.key==='Enter') sendChatbotMessage()">
            <button type="button" class="btn-chatbot-send" onclick="sendChatbotMessage()" title="Gửi tin nhắn">
                <i class="fas fa-paper-plane"></i>
            </button>
        </div>
    </div>

    <!-- ==================== CÁC MODAL THÔNG TIN MÔ PHỎNG ==================== -->
    <div class="nav-info-modal-backdrop" id="navInfoModalBackdrop" onclick="closeAllInfoModals()"></div>

    <!-- 1. Modal Giới Thiệu -->
    <div class="nav-info-modal" id="aboutModal">
        <div class="nav-info-modal-header">
            <h3><i class="fas fa-circle-info"></i> Giới Thiệu Về Tiệm Sách Tri Thức Bookora</h3>
            <button type="button" class="btn-close-info-modal" onclick="closeAboutModal()">&times;</button>
        </div>
        <div class="nav-info-modal-body">
            <div class="about-hero-box">
                <h4>📚 Nơi Hội Tụ Tinh Hoa Tri Thức Nhân Loại</h4>
                <p style="margin: 0; font-size: 13px; color: #475569;">Được thành lập với sứ mệnh kết nối độc giả Việt Nam với nguồn tri thức nhân loại quý giá nhất.</p>
            </div>

            <div class="about-stats-grid">
                <div class="about-stat-item">
                    <strong>50.000+</strong>
                    <span>Đầu sách bản quyền</span>
                </div>
                <div class="about-stat-item">
                    <strong>200+</strong>
                    <span>NXB & Đối tác lớn</span>
                </div>
                <div class="about-stat-item">
                    <strong>1.000.000+</strong>
                    <span>Độc giả toàn quốc</span>
                </div>
            </div>

            <h5 style="margin: 15px 0 10px; color: #1e293b; font-size: 14.5px;">⭐ 4 Cam Kết Vàng Từ Bookora:</h5>
            <div class="about-commit-list">
                <div class="about-commit-item">
                    <i class="fas fa-shield-check"></i>
                    <div><strong>100% Sách Thật & Bản Quyền:</strong> Nói không tuyệt đối với sách lậu, sách photocopy kém chất lượng.</div>
                </div>
                <div class="about-commit-item">
                    <i class="fas fa-truck-fast"></i>
                    <div><strong>Giao Hàng Siêu Tốc & An Toàn:</strong> Đóng gói chống sốc 3 lớp, miễn phí vận chuyển cho đơn từ 250.000đ.</div>
                </div>
                <div class="about-commit-item">
                    <i class="fas fa-rotate-left"></i>
                    <div><strong>Đổi Trả Linh Hoạt 30 Ngày:</strong> Hỗ trợ 1 đổi 1 nếu sách bị lỗi in ấn hoặc hư hỏng trong quá trình vận chuyển.</div>
                </div>
                <div class="about-commit-item">
                    <i class="fas fa-headset"></i>
                    <div><strong>Tư Vấn Tận Tâm 24/7:</strong> Trợ lý AI Bookora và đội ngũ tư vấn luôn sẵn sàng gợi ý cuốn sách phù hợp nhất cho bạn.</div>
                </div>
            </div>
        </div>
    </div>

    <!-- 2. Modal Khuyến Mãi -->
    <div class="nav-info-modal" id="promotionModal">
        <div class="nav-info-modal-header">
            <h3><i class="fas fa-gift"></i> Mã Giảm Giá & Ưu Đãi Độc Quyền Bookora</h3>
            <button type="button" class="btn-close-info-modal" onclick="closePromotionModal()">&times;</button>
        </div>
        <div class="nav-info-modal-body">
            <p style="margin-top: 0; font-size: 13px; color: #64748b;">Nhấp <strong>Sao chép</strong> mã ưu đãi để áp dụng khi thanh toán đơn hàng:</p>
            <div class="promo-list-wrap">
                <div class="promo-ticket-card">
                    <div class="promo-ticket-left">
                        <i class="fas fa-tag"></i>
                        <span>GIẢM 30K</span>
                    </div>
                    <div class="promo-ticket-right">
                        <div class="promo-ticket-info">
                            <h5>Giảm 30.000đ cho đơn từ 200.000đ</h5>
                            <p>Áp dụng cho toàn bộ đầu sách trên hệ thống</p>
                            <span class="promo-code-badge">BOOKORA2026</span>
                        </div>
                        <button type="button" class="btn-copy-promo" onclick="copyPromoCode('BOOKORA2026')">
                            <i class="fas fa-copy"></i> Sao chép
                        </button>
                    </div>
                </div>

                <div class="promo-ticket-card">
                    <div class="promo-ticket-left" style="background: linear-gradient(135deg, #10b981, #047857);">
                        <i class="fas fa-truck-fast"></i>
                        <span>FREESHIP</span>
                    </div>
                    <div class="promo-ticket-right">
                        <div class="promo-ticket-info">
                            <h5>Miễn phí vận chuyển toàn quốc</h5>
                            <p>Áp dụng cho đơn hàng giá trị từ 250.000đ</p>
                            <span class="promo-code-badge">FREESHIP</span>
                        </div>
                        <button type="button" class="btn-copy-promo" onclick="copyPromoCode('FREESHIP')">
                            <i class="fas fa-copy"></i> Sao chép
                        </button>
                    </div>
                </div>

                <div class="promo-ticket-card">
                    <div class="promo-ticket-left" style="background: linear-gradient(135deg, #3b82f6, #1d4ed8);">
                        <i class="fas fa-laptop-code"></i>
                        <span>GIẢM 30%</span>
                    </div>
                    <div class="promo-ticket-right">
                        <div class="promo-ticket-info">
                            <h5>Giảm 30% Sách Công Nghệ & AI</h5>
                            <p>Dành riêng cho sinh viên & lập trình viên</p>
                            <span class="promo-code-badge">TECH30</span>
                        </div>
                        <button type="button" class="btn-copy-promo" onclick="copyPromoCode('TECH30')">
                            <i class="fas fa-copy"></i> Sao chép
                        </button>
                    </div>
                </div>

                <div class="promo-ticket-card">
                    <div class="promo-ticket-left" style="background: linear-gradient(135deg, #8b5cf6, #6d28d9);">
                        <i class="fas fa-brain"></i>
                        <span>GIẢM 15%</span>
                    </div>
                    <div class="promo-ticket-right">
                        <div class="promo-ticket-info">
                            <h5>Giảm 15% Sách Kỹ Năng & Tâm Lý</h5>
                            <p>Bộ sách nuôi dưỡng tâm hồn và phát triển bản thân</p>
                            <span class="promo-code-badge">TRI_THUC</span>
                        </div>
                        <button type="button" class="btn-copy-promo" onclick="copyPromoCode('TRI_THUC')">
                            <i class="fas fa-copy"></i> Sao chép
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 3. Modal Liên Hệ -->
    <div class="nav-info-modal" id="contactModal">
        <div class="nav-info-modal-header">
            <h3><i class="fas fa-headset"></i> Liên Hệ & Chăm Sóc Khách Hàng Bookora</h3>
            <button type="button" class="btn-close-info-modal" onclick="closeContactModal()">&times;</button>
        </div>
        <div class="nav-info-modal-body">
            <div class="contact-layout-grid">
                <div class="contact-info-col">
                    <h5>📍 Thông Tin Nhà Sách</h5>
                    <div class="contact-list-items">
                        <div class="contact-item">
                            <i class="fas fa-location-dot"></i>
                            <div><strong>Địa chỉ:</strong> Số 25, ngõ 68 phố Cầu Giấy, P. Quan Hoa, Q. Cầu Giấy, Hà Nội</div>
                        </div>
                        <div class="contact-item">
                            <i class="fas fa-phone-volume"></i>
                            <div><strong>Hotline tư vấn:</strong> 028.73008182 - 1900 6401</div>
                        </div>
                        <div class="contact-item">
                            <i class="fas fa-envelope"></i>
                            <div><strong>Email:</strong> hotro@bookora.com / lienhe@bookora.com</div>
                        </div>
                        <div class="contact-item">
                            <i class="fas fa-clock"></i>
                            <div><strong>Giờ mở cửa:</strong> 8:00 - 22:00 (Tất cả các ngày trong tuần)</div>
                        </div>
                    </div>
                </div>

                <div class="contact-form-col">
                    <h5>💬 Gửi Yêu Cầu / Góp Ý</h5>
                    <form onsubmit="return handleSendContact(event)">
                        <input type="text" id="contactName" class="contact-input-field" placeholder="Họ và tên của bạn *" required>
                        <input type="email" id="contactEmail" class="contact-input-field" placeholder="Địa chỉ email *">
                        <textarea id="contactMessage" class="contact-input-field" style="height: 70px; resize: none;" placeholder="Nội dung cần tư vấn hoặc góp ý..."></textarea>
                        <button type="submit" class="btn-send-contact">
                            <i class="fas fa-paper-plane"></i> Gửi Tin Nhắn
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- SCRIPTS -->
    <script src="/js/app.js?v=20260926_10"></script>
    <script>
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

        function onDetailAddToCart() {
            const infoCol = document.querySelector('.detail-info-column');
            if (!infoCol) return;
            const bookId = parseInt(infoCol.getAttribute('data-id'));
            const qty = parseInt(document.getElementById('detailQuantityInput').value) || 1;
            if (bookId) {
                addToCart(bookId, qty);
            }
        }

        function onDetailBuyNow() {
            const infoCol = document.querySelector('.detail-info-column');
            if (!infoCol) return;
            const bookId = parseInt(infoCol.getAttribute('data-id'));
            const qty = parseInt(document.getElementById('detailQuantityInput').value) || 1;
            if (bookId) {
                addToCart(bookId, qty);
                window.location.href = 'cart';
            }
        }
    </script>
</body>
</html>
