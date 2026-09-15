<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.bookstore.model.User, com.bookstore.model.Book, java.util.List" %>
<%
    User currentUser = (User) request.getAttribute("currentUser");
    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/login?error=require_login");
        return;
    }
    List<Book> books = (List<Book>) request.getAttribute("books");
    List<String> categories = (List<String>) request.getAttribute("categories");
    String currentCategory = (String) request.getAttribute("currentCategory");
    if (currentCategory == null) currentCategory = "Tất cả";
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
            <div><i class="fas fa-truck-fast"></i> Miễn phí vận chuyển toàn quốc cho đơn hàng từ 250.000 đ</div>
            <div>
                <span>Hotline: 1900 6868</span>
                <span style="margin: 0 8px;">|</span>
                <a href="#catalog"><i class="fas fa-tags"></i> Khuyến Mãi Hôm Nay</a>
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

            <!-- Thanh tìm kiếm sách -->
            <form action="${pageContext.request.contextPath}/home" method="GET" class="search-form" id="searchForm">
                <div class="search-input-wrap">
                    <input type="text" id="searchInput" name="q" class="search-input" 
                           placeholder="Tìm kiếm theo tựa sách, tác giả hoặc thể loại..." 
                           value="<%= searchKeyword %>" autocomplete="off">
                    <button type="submit" class="btn-search" title="Tìm kiếm">
                        <i class="fas fa-search"></i>
                    </button>
                </div>
            </form>

            <!-- Khối hành động: Giỏ hàng & User Profile -->
            <div class="header-actions">
                <!-- Nút mở giỏ hàng -->
                <button type="button" class="btn-cart-toggle" onclick="toggleCartDrawer()" title="Xem giỏ hàng">
                    <i class="fas fa-bag-shopping"></i>
                    <span class="cart-badge" id="cartBadge" style="display: none;">0</span>
                </button>

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
                            <div class="dropdown-email"><%= currentUser.getEmail() %></div>
                        </div>
                        <a href="javascript:void(0)" class="dropdown-item" onclick="alert('Trang hồ sơ của <%= currentUser.getFullName() %>')">
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
                    Tuyển tập hơn 100 tác phẩm kinh điển và hiện đại được độc giả yêu thích nhất. Ưu đãi đến 30% cùng quà tặng sổ tay độc quyền cho thành viên <strong><%= currentUser.getFullName() %></strong>.
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

    <!-- 3. BOOK CATALOG & CATEGORIES -->
    <main class="catalog-section" id="catalog">
        <div class="section-header">
            <div class="section-title-wrap">
                <h2>Tủ Sách Nổi Bật Dành Cho Bạn</h2>
                <p>Tìm thấy <strong id="bookCountDisplay" style="color: var(--primary);"><%= (books != null) ? books.size() : 0 %></strong> cuốn sách đặc sắc phù hợp cho bạn</p>
            </div>
        </div>

        <!-- Bộ lọc phân loại danh mục -->
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

        <!-- Danh sách hiển thị các thẻ sách -->
        <div class="book-grid" id="bookGrid">
            <% if (books == null || books.isEmpty()) { %>
                <div class="no-books-found">
                    <i class="fas fa-book-open"></i>
                    <p>Không tìm thấy cuốn sách nào phù hợp.</p>
                </div>
            <% } else {
                for (Book b : books) {
            %>
                <div class="book-card" data-id="<%= b.getId() %>" 
                     data-title="<%= b.getTitle().replace("\"", "&quot;") %>" 
                     data-author="<%= b.getAuthor().replace("\"", "&quot;") %>" 
                     data-price="<%= b.getPrice() %>" 
                     data-formatted-price="<%= b.getFormattedPrice() %>" 
                     data-category="<%= b.getCategory() %>" 
                     data-image="<%= b.getImage() %>" 
                     data-desc="<%= b.getDescription().replace("\"", "&quot;") %>" 
                     data-rating="<%= b.getRating() %>">
                    <div class="book-card-inner">
                        <div class="book-cover-wrap">
                            <img src="<%= b.getImage() %>" alt="<%= b.getTitle() %>" class="book-cover" loading="lazy" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500';">
                            <div class="badge-container">
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
                            <span class="book-category"><%= b.getCategory() %></span>
                            <h3 class="book-title" title="<%= b.getTitle() %>"><%= b.getTitle() %></h3>
                            <p class="book-author"><i class="fas fa-feather-alt"></i> <%= b.getAuthor() %></p>
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
                                <button type="button" class="btn-add-cart" onclick="addToCart(<%= b.getId() %>)" title="Thêm vào giỏ">
                                    <i class="fas fa-cart-plus"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            <%  }
               } %>
        </div>
    </main>

    <!-- 4. MODAL QUICK VIEW -->
    <div class="modal-overlay" id="quickViewModal">
        <div class="modal-dialog">
            <button type="button" class="modal-close" onclick="closeQuickView()" title="Đóng">
                <i class="fas fa-times"></i>
            </button>
            <div class="modal-content-grid">
                <div class="modal-book-cover">
                    <img src="" alt="Bìa sách" id="modalCover">
                </div>
                <div class="modal-details">
                    <span class="modal-category" id="modalCategory">Thể loại</span>
                    <h2 class="modal-title" id="modalTitle">Tựa Sách</h2>
                    <p class="modal-author"><i class="fas fa-feather-alt"></i> <span id="modalAuthor">Tác giả</span></p>
                    <div class="book-rating" style="margin-bottom: 12px;">
                        <div class="stars"><i class="fas fa-star"></i> <span id="modalRating">5.0</span></div>
                        <span class="review-count">Đánh giá cao bởi cộng đồng</span>
                    </div>
                    <div class="modal-price-row">
                        <span class="modal-price" id="modalPrice">0 đ</span>
                    </div>
                    <p class="modal-desc" id="modalDesc">Mô tả tóm tắt nội dung cuốn sách...</p>
                    <div class="modal-actions">
                        <div class="qty-control">
                            <button type="button" class="btn-qty" onclick="changeModalQty(-1)">-</button>
                            <input type="text" id="modalQty" class="qty-input" value="1" readonly>
                            <button type="button" class="btn-qty" onclick="changeModalQty(1)">+</button>
                        </div>
                        <button type="button" class="btn-modal-cart" onclick="addModalBookToCart()">
                            <i class="fas fa-cart-plus"></i> Thêm Vào Giỏ Hàng
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
