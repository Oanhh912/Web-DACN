<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bookstore.model.User" %>
<%
    User currentUser = (User) session.getAttribute("user");
    boolean isLoggedIn = (currentUser != null);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giỏ Hàng Của Bạn - Bookora | Tiệm Sách Tri Thức</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="css/style.css?v=20260925_02">
</head>
<body>

    <!-- 1. HEADER & NAVIGATION -->
    <header class="main-header">
        <div class="top-bar">
            <div class="topbar-contact-info">
                <span><i class="fas fa-phone"></i> 028.73008182</span>
                <span><i class="fas fa-envelope"></i> hotro@bookora.com</span>
                <span><i class="fas fa-location-dot"></i> Số 25, ngõ 68 phố Cầu Giấy, TP. Hà Nội</span>
            </div>
            <div class="topbar-auth-links">
                <% if (isLoggedIn) { %>
                    <span class="topbar-welcome"><i class="fas fa-circle-user"></i> Xin chào, <strong><%= currentUser.getFullName() %></strong></span>
                    <span class="topbar-divider">|</span>
                    <a href="logout" class="topbar-auth-btn"><i class="fas fa-arrow-right-from-bracket"></i> ĐĂNG XUẤT</a>
                <% } else { %>
                    <span style="color: #cbd5e1;"><i class="fas fa-truck-fast"></i> Miễn phí vận chuyển từ 250.000 đ</span>
                <% } %>
            </div>
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
                           placeholder="Tìm kiếm theo tựa sách, tác giả, NXB..." autocomplete="off">
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

            <!-- Khối hành động: Giỏ hàng & User Profile / Đăng nhập Đăng ký -->
            <div class="header-actions">
                <!-- Nút mở giỏ hàng kèm Popover chuẩn ảnh mẫu 1 -->
                <div class="cart-dropdown-wrapper" id="cartDropdownWrapper">
                    <button type="button" class="btn-cart-toggle" id="btnCartToggle" onclick="toggleCartPopover(event)" title="Xem giỏ hàng">
                        <i class="fas fa-bag-shopping"></i>
                        <span class="cart-badge" id="cartBadge" style="display: none;">0</span>
                    </button>

                    <!-- Popover giỏ hàng chuẩn ảnh mẫu 1 -->
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

                <% if (isLoggedIn) { %>
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
    </header>

    <!-- 2. GIAO DIỆN GIỎ HÀNG CHÍNH (CHUẨN ẢNH MẪU 2) -->
    <main class="cart-page-wrapper">
        <div class="cart-page-section" id="cartPageContainer">
            <!-- Header block căn giữa -->
            <div class="cart-header-block">
                <h2 class="cart-header-title">Giỏ hàng của bạn</h2>
                <p class="cart-header-subtitle">Có <span id="cartCountSubtitle">0</span> sản phẩm trong giỏ hàng</p>
                <div class="cart-header-underline"></div>
            </div>

            <!-- Bảng giỏ hàng hiển thị danh sách sản phẩm -->
            <div class="cart-table-wrapper" id="cartTableWrapper">
                <table class="cart-table">
                    <thead>
                        <tr>
                            <th class="col-product">Tên sản phẩm</th>
                            <th class="col-price">Giá</th>
                            <th class="col-qty">Số lượng</th>
                            <th class="col-total">Thành tiền</th>
                            <th class="col-action"></th>
                        </tr>
                    </thead>
                    <tbody id="cartTableBody"></tbody>
                </table>
            </div>

            <!-- Trạng thái giỏ hàng trống nếu chưa có sản phẩm -->
            <div class="cart-empty-view" id="cartEmptyView" style="display: none;">
                <i class="fas fa-bag-shopping"></i>
                <h3>Giỏ hàng của bạn đang trống</h3>
                <p>Hiện chưa có cuốn sách nào trong giỏ hàng. Hãy khám phá ngay các tựa sách mới nhất!</p>
                <a href="home" class="btn-empty-continue"><i class="fas fa-arrow-left"></i> Tiếp tục mua hàng</a>
            </div>

            <!-- Bố cục 2 cột bên dưới: Cột trái (Ghi chú), Cột phải (Thông tin đơn hàng) -->
            <div class="cart-bottom-grid" id="cartBottomGrid">
                <!-- Cột trái: Ghi chú đơn hàng -->
                <div class="cart-col-left">
                    <h3 class="cart-col-sec-title">Ghi chú đơn hàng</h3>
                    <textarea id="cartOrderNote" class="cart-note-textarea" placeholder="Ghi chú"></textarea>
                </div>

                <!-- Cột phải: Thông tin đơn hàng & Thanh toán -->
                <div class="cart-col-right">
                    <div class="cart-summary-box">
                        <h3 class="cart-col-sec-title">Thông tin đơn hàng</h3>
                        <div class="cart-summary-total-row">
                            <span class="cart-summary-total-label">Tổng tiền:</span>
                            <span class="cart-summary-total-val" id="cartPageGrandTotal">0đ</span>
                        </div>
                        <div class="cart-summary-notices">
                            <p>Phí vận chuyển sẽ được tính ở trang thanh toán.</p>
                            <p>Bạn cũng có thể nhập mã giảm giá ở trang thanh toán.</p>
                        </div>
                        <button type="button" class="btn-cart-page-checkout" onclick="proceedToCheckout()">THANH TOÁN</button>
                        <div>
                            <a href="home" class="link-continue-shopping">
                                <i class="fas fa-reply"></i> Tiếp tục mua hàng
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- 3. FOOTER -->
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
                    <li><a href="home">Giới thiệu nhà sách</a></li>
                    <li><a href="home">Hệ thống cửa hàng</a></li>
                    <li><a href="home">Tuyển dụng</a></li>
                    <li><a href="home">Liên hệ hợp tác</a></li>
                </ul>
            </div>
            <div class="footer-col">
                <h4>Hỗ Trợ Khách Hàng</h4>
                <ul>
                    <li><a href="home">Chính sách đổi trả</a></li>
                    <li><a href="home">Phương thức vận chuyển</a></li>
                    <li><a href="home">Hướng dẫn thanh toán</a></li>
                    <li><a href="home">Bảo mật thông tin</a></li>
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

    <!-- Toast Notification Container -->
    <div class="toast-container" id="toastContainer"></div>

    <script src="js/app.js?v=20260926_10"></script>
</body>
</html>
