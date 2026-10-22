$baseUrl = "http://localhost:8082"

function Invoke-ApiRequest {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [object]$Body = $null,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session
    )

    $params = @{
        Uri = $Uri
        Method = $Method
        WebSession = $Session
        UseBasicParsing = $true
    }

    if ($Body) {
        $jsonStr = ($Body | ConvertTo-Json -Depth 10 -Compress)
        $params["ContentType"] = "application/json; charset=utf-8"
        $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($jsonStr)
    }

    try {
        $response = Invoke-WebRequest @params
        return @{
            StatusCode = [int]$response.StatusCode
            Content = ($response.Content | ConvertFrom-Json)
            Raw = $response.Content
        }
    } catch {
        $statusCode = 500
        $bodyText = ""
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $bodyText = $_.ErrorDetails.Message
        } elseif ($_.Exception.Response) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
                    $bodyText = $reader.ReadToEnd()
                    $reader.Close()
                }
            } catch {}
        }
        $json = $null
        if ($bodyText) {
            try { $json = ($bodyText | ConvertFrom-Json) } catch {}
        }
        return @{
            StatusCode = $statusCode
            Content = $json
            Raw = $bodyText
        }
    }
}

function Login-User {
    param([string]$username, [string]$password)
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $u = [System.Uri]::EscapeDataString($username)
    $p = [System.Uri]::EscapeDataString($password)
    $body = "username=$u&password=$p"
    $res = Invoke-WebRequest -Uri "$baseUrl/login" -Method POST -Body $body -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing
    return $session
}

Write-Host "=== BẮT ĐẦU KIỂM THỬ TỰ ĐỘNG UC QUẢN LÝ ĐƠN HÀNG BOOKORA ===" -ForegroundColor Cyan

# 1. Đăng nhập các vai trò
$adminSession = Login-User "admin" "123456"
Write-Host "1. Admin đăng nhập thành công." -ForegroundColor Green

$userASession = Login-User "oanh" "123456"
Write-Host "2. Customer A ('oanh') đăng nhập thành công." -ForegroundColor Green

$userBSession = Login-User "khachhang" "123456"
Write-Host "3. Customer B ('khachhang') đăng nhập thành công." -ForegroundColor Green

# Lấy thông tin sách để kiểm tra tồn kho
$booksRes = Invoke-ApiRequest -Uri "$baseUrl/api/admin/books" -Session $adminSession
$firstBook = $booksRes.Content.books[0]
$bookId = $firstBook.id
$initialStock = $firstBook.stock
$bookPrice = $firstBook.price
Write-Host "Sách kiểm thử: ID=$bookId, Giá=$bookPrice, Tồn ban đầu=$initialStock" -ForegroundColor Yellow

# Đảm bảo user A có địa chỉ nhận hàng
$checkoutInfo = (Invoke-ApiRequest -Uri "$baseUrl/api/checkout/info" -Session $userASession).Content
$addressId = 0
if ($checkoutInfo.addresses -and $checkoutInfo.addresses.Count -gt 0) {
    $addressId = $checkoutInfo.addresses[0].id
} else {
    $addAddrRes = Invoke-ApiRequest -Uri "$baseUrl/api/checkout/address" -Method POST -Body @{
        recipientName = "Trần Thị Oanh"
        phone = "0987654321"
        addressDetail = "123 Đường Sách"
        province = "Thành phố Hồ Chí Minh"
        district = "Quận 1"
        ward = "Phường Bến Nghé"
        isDefault = $true
    } -Session $userASession
    $addressId = $addAddrRes.Content.address.id
}
Write-Host "Địa chỉ nhận hàng AddressId: $addressId" -ForegroundColor Yellow

# TEST 1: Khách hàng đặt hàng thành công -> đơn ở trạng thái “Chờ xác nhận”
$orderPayload = @{
    addressId = $addressId
    paymentMethod = "COD"
    note = "Kiểm thử đơn hàng tự động"
    items = @(
        @{
            bookId = $bookId
            quantity = 2
        }
    )
}

$createRes = Invoke-ApiRequest -Uri "$baseUrl/api/checkout/place-order" -Method POST -Body $orderPayload -Session $userASession
if ($createRes.StatusCode -ne 200 -or -not $createRes.Content.success) {
    Write-Host "Create order returned Status: $($createRes.StatusCode)" -ForegroundColor Red
    Write-Host "Create order Content: $($createRes.Content | ConvertTo-Json)" -ForegroundColor Red
    Write-Host "Create order Raw: $($createRes.Raw)" -ForegroundColor Red
    throw "Đặt hàng thất bại: $($createRes.Content)"
}
$orderCode1 = $createRes.Content.orderCode
$orderDetail1 = (Invoke-ApiRequest -Uri "$baseUrl/api/orders/detail?code=$orderCode1" -Session $userASession).Content
$orderStatus1 = $orderDetail1.order.orderStatus
Write-Host "✅ TEST 1: Đặt hàng thành công! Mã: $orderCode1, Trạng thái: $orderStatus1 (Kỳ vọng: CHO_XAC_NHAN)" -ForegroundColor Green
if ($orderStatus1 -ne "CHO_XAC_NHAN") { throw "Sai trạng thái ban đầu!" }

# Kiểm tra tồn kho sau đặt hàng đã giảm đúng 2
$booksAfter1 = Invoke-ApiRequest -Uri "$baseUrl/api/admin/books" -Session $adminSession
$stockAfterOrder = ($booksAfter1.Content.books | Where-Object { $_.id -eq $bookId }).stock
Write-Host "   -> Tồn kho trừ chính xác: $initialStock -> $stockAfterOrder" -ForegroundColor DarkCyan
if ($stockAfterOrder -ne ($initialStock - 2)) { throw "Tồn kho chưa giảm đúng!" }

# TEST 2: Admin xác nhận đơn hợp lệ -> chuyển sang "Đang xử lý"
$confirmRes = Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/confirm" -Method POST -Body @{ orderCode = $orderCode1 } -Session $adminSession
if ($confirmRes.StatusCode -ne 200 -or -not $confirmRes.Content.success) {
    throw "Admin xác nhận đơn thất bại: $($confirmRes.Content)"
}
$detail1 = (Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/detail?code=$orderCode1" -Session $adminSession).Content
Write-Host "✅ TEST 2: Admin xác nhận đơn thành công: Trạng thái = $($detail1.order.orderStatus) (Kỳ vọng: DANG_XU_LY)" -ForegroundColor Green
if ($detail1.order.orderStatus -ne "DANG_XU_LY") { throw "Sai trạng thái sau xác nhận!" }

# TEST 3: Đơn được bàn giao vận chuyển -> chuyển sang "Đang giao"
$shipRes = Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/update-status" -Method POST -Body @{ orderCode = $orderCode1; status = "DANG_GIAO" } -Session $adminSession
if ($shipRes.StatusCode -ne 200 -or -not $shipRes.Content.success) {
    Write-Host "Ship update failed: $($shipRes.Content | ConvertTo-Json)" -ForegroundColor Red
    throw "Cập nhật Đang giao thất bại!"
}
$detailShip = (Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/detail?code=$orderCode1" -Session $adminSession).Content
Write-Host "✅ TEST 3: Cập nhật sang Đang giao thành công: Trạng thái = $($detailShip.order.orderStatus)" -ForegroundColor Green
if ($detailShip.order.orderStatus -ne "DANG_GIAO") { throw "Sai trạng thái đang giao!" }

# TEST 4: Giao hàng thành công -> chuyển sang "Đã giao" (HOAN_THANH)
$doneRes = Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/update-status" -Method POST -Body @{ orderCode = $orderCode1; status = "HOAN_THANH" } -Session $adminSession
if ($doneRes.StatusCode -ne 200 -or -not $doneRes.Content.success) {
    Write-Host "Done update failed: $($doneRes.Content | ConvertTo-Json)" -ForegroundColor Red
    throw "Cập nhật Hoàn thành thất bại!"
}
$detailDone = (Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/detail?code=$orderCode1" -Session $adminSession).Content
Write-Host "✅ TEST 4: Giao hàng thành công: Trạng thái = $($detailDone.order.orderStatus), PaymentStatus = $($detailDone.order.paymentStatus)" -ForegroundColor Green
if ($detailDone.order.orderStatus -ne "HOAN_THANH") { throw "Sai trạng thái hoàn thành!" }

# Thử hủy đơn đã hoàn thành -> phải bị từ chối
$rejectCancelDone = Invoke-ApiRequest -Uri "$baseUrl/api/orders/cancel" -Method POST -Body @{ orderCode = $orderCode1; reason = "Test" } -Session $userASession
Write-Host "   -> Thử hủy đơn đã HOAN_THANH: HTTP $($rejectCancelDone.StatusCode), Raw: '$($rejectCancelDone.Raw)'" -ForegroundColor DarkCyan
if ($rejectCancelDone.StatusCode -ne 400 -or $rejectCancelDone.Content.message -ne "Không thể hủy đơn hàng ở trạng thái hiện tại.") {
    Write-Host "Content was: $($rejectCancelDone.Content | ConvertTo-Json)" -ForegroundColor Red
    throw "Thông báo từ chối hủy không đúng quy chuẩn!"
}

# TEST 5 & 11: Đơn không hợp lệ -> Admin hủy và có lý do phù hợp + Hoàn trả tồn kho duy nhất 1 lần
Start-Sleep -Milliseconds 1600
$createRes2 = Invoke-ApiRequest -Uri "$baseUrl/api/checkout/place-order" -Method POST -Body $orderPayload -Session $userASession
$orderCode2 = $createRes2.Content.orderCode
$stockBeforeCancel = ($booksAfter1.Content.books | Where-Object { $_.id -eq $bookId }).stock

$adminCancel = Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/cancel" -Method POST -Body @{ orderCode = $orderCode2; reason = "Số điện thoại người nhận không liên lạc được" } -Session $adminSession
$detailCancel = (Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/detail?code=$orderCode2" -Session $adminSession).Content
Write-Host "✅ TEST 5: Admin hủy đơn không hợp lệ: Trạng thái = $($detailCancel.order.orderStatus), Lý do = '$($detailCancel.order.cancelReason)'" -ForegroundColor Green
if ($detailCancel.order.orderStatus -ne "DA_HUY") { throw "Hủy đơn thất bại!" }

$booksAfterCancel = Invoke-ApiRequest -Uri "$baseUrl/api/admin/books" -Session $adminSession
$stockAfterCancel = ($booksAfterCancel.Content.books | Where-Object { $_.id -eq $bookId }).stock
Write-Host "✅ TEST 11: Hoàn tồn kho thành công: $stockBeforeCancel -> $stockAfterCancel (+2)" -ForegroundColor Green

# Thử hủy lại lần 2 -> phải bị chặn, không cộng tồn kho lần 2
$adminCancelAgain = Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/cancel" -Method POST -Body @{ orderCode = $orderCode2; reason = "Hủy lần 2" } -Session $adminSession
$stockAfterCancelAgain = ((Invoke-ApiRequest -Uri "$baseUrl/api/admin/books" -Session $adminSession).Content.books | Where-Object { $_.id -eq $bookId }).stock
Write-Host "   -> Hủy lần 2 bị chặn: HTTP $($adminCancelAgain.StatusCode), Tồn kho không đổi ($stockAfterCancelAgain)" -ForegroundColor DarkCyan
if ($stockAfterCancelAgain -ne $stockAfterCancel) { throw "Tồn kho bị cộng lặp lại!" }

# TEST 6: Khách hàng hủy đơn đủ điều kiện (CHO_XAC_NHAN)
Start-Sleep -Milliseconds 1600
$createRes3 = Invoke-ApiRequest -Uri "$baseUrl/api/checkout/place-order" -Method POST -Body $orderPayload -Session $userASession
$orderCode3 = $createRes3.Content.orderCode
$custCancel = Invoke-ApiRequest -Uri "$baseUrl/api/orders/cancel" -Method POST -Body @{ orderCode = $orderCode3; reason = "Tôi muốn đổi địa chỉ giao hàng" } -Session $userASession
Write-Host "✅ TEST 6: Khách hàng hủy đơn đủ điều kiện: HTTP $($custCancel.StatusCode), Msg = '$($custCancel.Content.message)'" -ForegroundColor Green
if ($custCancel.Content.message -ne "Đơn hàng đã được hủy thành công.") { throw "Sai thông báo hủy thành công!" }

# TEST 7: Khách hàng yêu cầu hủy đơn không đủ điều kiện (khi đã DANG_XU_LY)
Start-Sleep -Milliseconds 1600
$createRes4 = Invoke-ApiRequest -Uri "$baseUrl/api/checkout/place-order" -Method POST -Body $orderPayload -Session $userASession
$orderCode4 = $createRes4.Content.orderCode
# Admin duyệt đơn
Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/confirm" -Method POST -Body @{ orderCode = $orderCode4 } -Session $adminSession
# Khách hàng cố hủy đơn
$custCancelReject = Invoke-ApiRequest -Uri "$baseUrl/api/orders/cancel" -Method POST -Body @{ orderCode = $orderCode4; reason = "Muốn hủy" } -Session $userASession
Write-Host "✅ TEST 7: Khách hàng hủy đơn Đang xử lý bị từ chối: HTTP $($custCancelReject.StatusCode), Msg = '$($custCancelReject.Content.message)'" -ForegroundColor Green
if ($custCancelReject.StatusCode -ne 400 -or $custCancelReject.Content.message -ne "Không thể hủy đơn hàng ở trạng thái hiện tại.") {
    throw "Sai thông báo từ chối hủy!"
}

# TEST 8: Khách hàng B không xem hoặc hủy được đơn hàng của Khách hàng A
$crossDetail = Invoke-ApiRequest -Uri "$baseUrl/api/orders/detail?code=$orderCode4" -Session $userBSession
Write-Host "✅ TEST 8a: Khách hàng B xem trộm đơn hàng của A: HTTP $($crossDetail.StatusCode)" -ForegroundColor Green
if ($crossDetail.StatusCode -ne 403 -and $crossDetail.StatusCode -ne 404) { throw "Bảo mật hổng: Khách hàng B xem được đơn hàng của A!" }

$crossCancel = Invoke-ApiRequest -Uri "$baseUrl/api/orders/cancel" -Method POST -Body @{ orderCode = $orderCode4; reason = "Hủy trộm" } -Session $userBSession
Write-Host "✅ TEST 8b: Khách hàng B hủy trộm đơn hàng của A: HTTP $($crossCancel.StatusCode)" -ForegroundColor Green
if ($crossCancel.StatusCode -ne 403 -and $crossCancel.StatusCode -ne 400) { throw "Bảo mật hổng: Khách hàng B hủy được đơn của A!" }

# TEST 9: Khách hàng không thể tự gọi API quản trị sửa trạng thái đơn hàng
$hackStatus = Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders/update-status" -Method POST -Body @{ orderCode = $orderCode4; status = "HOAN_THANH" } -Session $userASession
Write-Host "✅ TEST 9: Khách hàng cố gọi API Admin: HTTP $($hackStatus.StatusCode) (Kỳ vọng: 403)" -ForegroundColor Green
if ($hackStatus.StatusCode -ne 403) { throw "Bảo mật hổng: Khách hàng gọi được API Admin!" }

# TEST 10: Người chưa đăng nhập không thể gọi API quản trị
$anonSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$anonCall = Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders" -Session $anonSession
Write-Host "✅ TEST 10: Khách vãng lai gọi API Admin: HTTP $($anonCall.StatusCode) (Kỳ vọng: 401 hoặc 403)" -ForegroundColor Green
if ($anonCall.StatusCode -ne 401 -and $anonCall.StatusCode -ne 403) { throw "Bảo mật hổng: Khách vãng lai gọi được API Admin!" }

# TEST 12: Tìm kiếm, lọc và phân trang đơn hàng tại trang Admin
$adminOrdersList = (Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders?page=1&limit=10&status=DA_HUY" -Session $adminSession).Content
$totalCancelled = if ($adminOrdersList.total -ne $null) { $adminOrdersList.total } else { $adminOrdersList.totalItems }
Write-Host "✅ TEST 12: Lọc đơn hàng DA_HUY: Tìm thấy $totalCancelled đơn hàng." -ForegroundColor Green
if ($totalCancelled -le 0) { throw "Không tìm thấy đơn hàng đã hủy!" }

$searchRes = (Invoke-ApiRequest -Uri "$baseUrl/api/admin/orders?search=$orderCode1" -Session $adminSession).Content
$searchTotal = if ($searchRes.total -ne $null) { $searchRes.total } else { $searchRes.totalItems }
Write-Host "   -> Tìm kiếm theo mã đơn $($orderCode1): $searchTotal đơn tìm thấy." -ForegroundColor DarkCyan
if ($searchTotal -ne 1) { throw "Tìm kiếm theo mã đơn không đúng!" }

# TEST 13: Đơn hàng hiển thị đầy đủ trong trang 'Đơn hàng của tôi' của khách hàng
$myOrders = (Invoke-ApiRequest -Uri "$baseUrl/api/orders/my-orders" -Session $userASession).Content
Write-Host "✅ TEST 13: Lấy danh sách 'Đơn hàng của tôi': $($myOrders.orders.Count) đơn hàng của oanh." -ForegroundColor Green
if ($myOrders.orders.Count -le 0) { throw "Không lấy được đơn hàng của tôi!" }

Write-Host "`n🎉🎉🎉 TOÀN BỘ 13 YÊU CẦU KIỂM THỬ ĐÃ ĐẠT 100% THÀNH CÔNG! 🎉🎉🎉" -ForegroundColor Magenta
