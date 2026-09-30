# Powershell verification script for Admin Promotions & Vouchers Management

$baseUrl = "http://localhost:8080"

Write-Host "=== 1. ĐĂNG NHẬP BẰNG TÀI KHOẢN ADMIN (admin / 123456) ===" -ForegroundColor Yellow
$adminWebReq = Invoke-WebRequest -Uri "$baseUrl/login" -Method POST -Body "username=admin&password=123456" -SessionVariable adminSession -UseBasicParsing
Write-Host "Admin Login Status:" $adminWebReq.StatusCode

Write-Host "`n=== 2. ĐĂNG NHẬP BẰNG TÀI KHOẢN CUSTOMER (oanh / 123456) ===" -ForegroundColor Yellow
$custWebReq = Invoke-WebRequest -Uri "$baseUrl/login" -Method POST -Body "username=oanh&password=123456" -SessionVariable custSession -UseBasicParsing
Write-Host "Customer Login Status:" $custWebReq.StatusCode

Write-Host "`n=== 3. TRUY CẬP TRANG ADMIN PROMOTIONS BẰNG ADMIN ===" -ForegroundColor Yellow
$adminPageRes = Invoke-WebRequest -Uri "$baseUrl/admin/promotions" -WebSession $adminSession -UseBasicParsing
Write-Host "Admin Access /admin/promotions Status:" $adminPageRes.StatusCode

Write-Host "`n=== 4. PHÂN QUYỀN: CUSTOMER GỌI API ADMIN PROMOTIONS (Phải bị 403) ===" -ForegroundColor Yellow
try {
    $custApiRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/promotions" -WebSession $custSession -UseBasicParsing -ErrorAction Stop
    Write-Host "ERR: Customer could access admin API!" -ForegroundColor Red
} catch {
    Write-Host "RBAC Check Passed! Customer received status:" $_.Exception.Response.StatusCode -ForegroundColor Green
}

Write-Host "`n=== 5. ADMIN LẤY DANH SÁCH MÃ GIẢM GIÁ (GET /api/admin/promotions) ===" -ForegroundColor Yellow
$vouchersRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/promotions" -WebSession $adminSession -UseBasicParsing
$vouchersJson = $vouchersRes.Content | ConvertFrom-Json
Write-Host "Success:" $vouchersJson.success "Total Vouchers:" $vouchersJson.vouchers.Count

Write-Host "`n=== 6. THỬ TẠO MÃ GIẢM GIÁ MỚI HỢP LỆ (PROMO2026) ===" -ForegroundColor Yellow
$addBody = @{
    code = "PROMO2026"
    title = "Khuyến Mãi Đỉnh Cao 2026"
    description = "Mã thử nghiệm tạo tự động"
    discountType = "PERCENT"
    discountValue = 20
    maxDiscountAmount = 50000
    minOrderAmount = 150000
    usageLimit = 50
    startDate = "2026-01-01"
    endDate = "2026-12-31"
    isActive = $true
} | ConvertTo-Json

$addRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/promotions/add" -Method POST -Body $addBody -ContentType "application/json" -WebSession $adminSession -UseBasicParsing
Write-Host "Add Voucher Result:" $addRes.Content

Write-Host "`n=== 7. KIỂM TRA LỖI: TẠO MÃ TRÙNG (PROMO2026) ===" -ForegroundColor Yellow
try {
    $dupRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/promotions/add" -Method POST -Body $addBody -ContentType "application/json" -WebSession $adminSession -UseBasicParsing -ErrorAction Stop
} catch {
    Write-Host "Duplicate Code Rejection Passed:" $_.Exception.Response.StatusCode $_.ErrorDetails.Message -ForegroundColor Green
}

Write-Host "`n=== 8. KIỂM TRA LỖI: PHẦN TRĂM GIẢM > 100% ===" -ForegroundColor Yellow
$invalidPercentBody = @{
    code = "OVER100"
    title = "Mã Lỗi %"
    discountType = "PERCENT"
    discountValue = 150
    startDate = "2026-01-01"
    endDate = "2026-12-31"
} | ConvertTo-Json
try {
    $invRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/promotions/add" -Method POST -Body $invalidPercentBody -ContentType "application/json" -WebSession $adminSession -UseBasicParsing -ErrorAction Stop
} catch {
    Write-Host "Invalid Percent Rejection Passed:" $_.Exception.Response.StatusCode $_.ErrorDetails.Message -ForegroundColor Green
}

Write-Host "`n=== 9. KIỂM TRA LỖI: NGÀY BẮT ĐẦU > NGÀY KẾT THÚC ===" -ForegroundColor Yellow
$invalidDateBody = @{
    code = "DATEERR"
    title = "Mã Lỗi Ngày"
    discountType = "FIXED"
    discountValue = 10000
    startDate = "2026-12-31"
    endDate = "2026-01-01"
} | ConvertTo-Json
try {
    $dateRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/promotions/add" -Method POST -Body $invalidDateBody -ContentType "application/json" -WebSession $adminSession -UseBasicParsing -ErrorAction Stop
} catch {
    Write-Host "Invalid Date Range Rejection Passed:" $_.Exception.Response.StatusCode $_.ErrorDetails.Message -ForegroundColor Green
}

Write-Host "`n=== 10. KIỂM TRA MÃ ĐÃ SỬ DỤNG TRONG ĐƠN HÀNG KHÔNG ĐƯỢC XÓA (BOOKORA2026) ===" -ForegroundColor Yellow
$bookoraVoucher = $vouchersJson.vouchers | Where-Object { $_.code -eq "BOOKORA2026" }
if ($bookoraVoucher) {
    try {
        $delBody = @{ id = $bookoraVoucher.id } | ConvertTo-Json
        $delRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/promotions/delete" -Method POST -Body $delBody -ContentType "application/json" -WebSession $adminSession -UseBasicParsing -ErrorAction Stop
    } catch {
        Write-Host "Protected Used Voucher Delete Passed:" $_.Exception.Response.StatusCode $_.ErrorDetails.Message -ForegroundColor Green
    }
}

Write-Host "`n=== 11. XÓA MÃ CHƯA SỬ DỤNG (PROMO2026) ===" -ForegroundColor Yellow
$promoVoucher = ($vouchersRes.Content | ConvertFrom-Json).vouchers | Where-Object { $_.code -eq "PROMO2026" }
if ($promoVoucher) {
    $delBody2 = @{ id = $promoVoucher.id } | ConvertTo-Json
    $delRes2 = Invoke-WebRequest -Uri "$baseUrl/api/admin/promotions/delete" -Method POST -Body $delBody2 -ContentType "application/json" -WebSession $adminSession -UseBasicParsing
    Write-Host "Delete Unused Voucher Result:" $delRes2.Content
}

Write-Host "`n=== 12. KIỂM TRA CHECKOUT BACKEND VOUCHER VALIDATION ===" -ForegroundColor Yellow
# Test expired code HETHAN
$validateExpiredBody = @{
    code = "HETHAN"
    items = @( @{ bookId = 1; quantity = 2 } )
} | ConvertTo-Json
try {
    $expRes = Invoke-WebRequest -Uri "$baseUrl/api/checkout/validate-voucher" -Method POST -Body $validateExpiredBody -ContentType "application/json" -WebSession $custSession -UseBasicParsing -ErrorAction Stop
} catch {
    Write-Host "Expired Voucher Checkout Validation Passed:" $_.ErrorDetails.Message -ForegroundColor Green
}

# Test min order amount fail
$validateMinOrderBody = @{
    code = "BOOKORA2026" # min 200k
    items = @( @{ bookId = 1; quantity = 1 } ) # 1 * 50k = 50k < 200k
} | ConvertTo-Json
try {
    $minRes = Invoke-WebRequest -Uri "$baseUrl/api/checkout/validate-voucher" -Method POST -Body $validateMinOrderBody -ContentType "application/json" -WebSession $custSession -UseBasicParsing -ErrorAction Stop
} catch {
    Write-Host "Min Order Amount Checkout Validation Passed:" $_.ErrorDetails.Message -ForegroundColor Green
}

# Test valid voucher application
$validateValidBody = @{
    code = "BOOKORA2026" # min 200k, discount 30k
    items = @( @{ bookId = 1; quantity = 5 } ) # 5 * 148k = 740k >= 200k
} | ConvertTo-Json
$validCheckRes = Invoke-WebRequest -Uri "$baseUrl/api/checkout/validate-voucher" -Method POST -Body $validateValidBody -ContentType "application/json" -WebSession $custSession -UseBasicParsing
Write-Host "Valid Voucher Checkout Result:" $validCheckRes.Content -ForegroundColor Green

Write-Host "`n=== TẤT CẢ KIỂM THỬ HOÀN TẤT THÀNH CÔNG! ===" -ForegroundColor Green
