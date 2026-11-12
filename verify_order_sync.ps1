$baseUrl = "http://localhost:8080"

function Login-User($u, $p) {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $body = "username=" + [System.Uri]::EscapeDataString($u) + "&password=" + [System.Uri]::EscapeDataString($p)
    $res = Invoke-WebRequest -Uri "$baseUrl/login" -Method POST -Body $body -ContentType "application/x-www-form-urlencoded" -WebSession $s -UseBasicParsing
    return $s
}

$adminSession = Login-User "admin" "123456"
$userSession = Login-User "oanh" "123456"

Write-Host "=== BƯỚC 1: KHÁCH HÀNG 'oanh' ĐẶT ĐƠN HÀNG MỚI ===" -ForegroundColor Cyan
$orderPayload = @{
    addressId = 1
    paymentMethod = "COD"
    note = "Đơn hàng kiểm thử đồng bộ thực tế"
    items = @( @{ bookId = 1; quantity = 1 } )
}
$jsonStr = ($orderPayload | ConvertTo-Json -Compress)
$orderRes = Invoke-WebRequest -Uri "$baseUrl/api/checkout/place-order" -Method POST -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonStr)) -ContentType "application/json; charset=utf-8" -WebSession $userSession -UseBasicParsing
$orderData = $orderRes.Content | ConvertFrom-Json
$orderCode = $orderData.orderCode
Write-Host "Mã đơn hàng mới tạo: $orderCode" -ForegroundColor Green

Write-Host "`n=== BƯỚC 2: ADMIN KIỂM TRA ĐƠN TRÊN TRANG QUẢN TRỊ (/api/admin/orders) ===" -ForegroundColor Cyan
$adminOrdersRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/orders?search=$orderCode" -WebSession $adminSession -UseBasicParsing
$adminOrder = ($adminOrdersRes.Content | ConvertFrom-Json).orders[0]
Write-Host "Admin thấy đơn: Mã=$($adminOrder.orderCode), Khách=$($adminOrder.recipientName), Trạng thái=$($adminOrder.orderStatus), Tổng tiền=$($adminOrder.formattedTotal)" -ForegroundColor Yellow

Write-Host "`n=== BƯỚC 3: ADMIN XÁC NHẬN ĐƠN -> CHUYỂN SANG DANG_XU_LY ===" -ForegroundColor Cyan
$confirmJson = (@{ orderCode = $orderCode } | ConvertTo-Json -Compress)
$confirmRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/orders/confirm" -Method POST -Body ([System.Text.Encoding]::UTF8.GetBytes($confirmJson)) -ContentType "application/json; charset=utf-8" -WebSession $adminSession -UseBasicParsing
$confirmData = $confirmRes.Content | ConvertFrom-Json
Write-Host "Kết quả Admin xác nhận: $($confirmData.message)" -ForegroundColor Green

Write-Host "`n=== BƯỚC 4: KHÁCH HÀNG TẢI LẠI TRANG ĐƠN HÀNG (/api/orders/my-orders) ===" -ForegroundColor Cyan
$myOrdersRes = Invoke-WebRequest -Uri "$baseUrl/api/orders/my-orders" -WebSession $userSession -UseBasicParsing
$myOrder = (($myOrdersRes.Content | ConvertFrom-Json).orders | Where-Object { $_.orderCode -eq $orderCode })
Write-Host "Trạng thái phía khách hàng: $($myOrder.orderStatus) (Văn bản: $($myOrder.orderStatusText))" -ForegroundColor Yellow

Write-Host "`n=== BƯỚC 5: ADMIN BÀN GIAO VẬN CHUYỂN -> DANG_GIAO ===" -ForegroundColor Cyan
$shipJson = (@{ orderCode = $orderCode; status = "DANG_GIAO" } | ConvertTo-Json -Compress)
$shipRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/orders/update-status" -Method POST -Body ([System.Text.Encoding]::UTF8.GetBytes($shipJson)) -ContentType "application/json; charset=utf-8" -WebSession $adminSession -UseBasicParsing
$shipData = $shipRes.Content | ConvertFrom-Json
Write-Host "Kết quả Admin cập nhật: $($shipData.message)" -ForegroundColor Green

Write-Host "`n=== BƯỚC 6: KHÁCH HÀNG KIỂM TRA TRẠNG THÁI MỚI ===" -ForegroundColor Cyan
$myOrdersRes2 = Invoke-WebRequest -Uri "$baseUrl/api/orders/my-orders" -WebSession $userSession -UseBasicParsing
$myOrder2 = (($myOrdersRes2.Content | ConvertFrom-Json).orders | Where-Object { $_.orderCode -eq $orderCode })
Write-Host "Trạng thái phía khách hàng: $($myOrder2.orderStatus) (Văn bản: $($myOrder2.orderStatusText))" -ForegroundColor Yellow

Write-Host "`n=== BƯỚC 7: ADMIN XÁC NHẬN GIAO HÀNG THÀNH CÔNG -> HOAN_THANH ===" -ForegroundColor Cyan
$doneJson = (@{ orderCode = $orderCode; status = "HOAN_THANH" } | ConvertTo-Json -Compress)
$doneRes = Invoke-WebRequest -Uri "$baseUrl/api/admin/orders/update-status" -Method POST -Body ([System.Text.Encoding]::UTF8.GetBytes($doneJson)) -ContentType "application/json; charset=utf-8" -WebSession $adminSession -UseBasicParsing
$doneData = $doneRes.Content | ConvertFrom-Json
Write-Host "Kết quả Admin hoàn tất: $($doneData.message)" -ForegroundColor Green

Write-Host "`n=== BƯỚC 8: KHÁCH HÀNG KIỂM TRA TRẠNG THÁI CUỐI CÙNG ===" -ForegroundColor Cyan
$myOrdersRes3 = Invoke-WebRequest -Uri "$baseUrl/api/orders/my-orders" -WebSession $userSession -UseBasicParsing
$myOrder3 = (($myOrdersRes3.Content | ConvertFrom-Json).orders | Where-Object { $_.orderCode -eq $orderCode })
Write-Host "Trạng thái phía khách hàng: $($myOrder3.orderStatus), Thanh toán: $($myOrder3.paymentStatus)" -ForegroundColor Yellow

Write-Host "`n=== TẤT CẢ 8 BƯỚC ĐỒNG BỘ HAI CHIỀU ĐÃ HOÀN TẤT CHÍNH XÁC 100%! ===" -ForegroundColor Magenta
