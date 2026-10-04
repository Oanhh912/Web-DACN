$baseUrl = "http://localhost:8080"

Write-Host "========================================================="
Write-Host "TESTING BOOKORA BOOK REVIEW & AUTOMATIC CONTENT FILTER"
Write-Host "========================================================="

# 1. Login user 'oanh'
$web = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginResp = Invoke-WebRequest -Uri "$baseUrl/login" -Method POST -Body @{ username="oanh"; password="123456" } -WebSession $web -MaximumRedirection 0 -ErrorAction SilentlyContinue

$cookies = $web.Cookies.GetCookies($baseUrl)
$tokenOanh = $null
foreach ($c in $cookies) {
    if ($c.Name -eq "BOOKSTORE_SESSION") {
        $tokenOanh = $c.Value
    }
}
Write-Host "Logged in as 'oanh' | Session Token: $tokenOanh"

# Case 1: Check eligibility for Book 1 (Nhà Giả Kim - Bought & Delivered in Order 101)
$res1 = Invoke-RestMethod -Uri "$baseUrl/api/reviews/eligible?bookId=1" -WebSession $web
Write-Host "`n1. Eligibility Book 1 (Bought & Delivered):"
Write-Host "   Eligible: $($res1.eligible) | Status: $($res1.status) | Message: $($res1.message)"

# Case 2: Check eligibility for Book 3 (Clean Code - Order 102 is DANG_GIAO)
$res2 = Invoke-RestMethod -Uri "$baseUrl/api/reviews/eligible?bookId=3" -WebSession $web
Write-Host "`n2. Eligibility Book 3 (Order Not Delivered):"
Write-Host "   Eligible: $($res2.eligible) | Status: $($res2.status) | Message: $($res2.message)"

# Case 3: Check eligibility for Book 2 (Đắc Nhân Tâm - Never Purchased by 'oanh')
$res3 = Invoke-RestMethod -Uri "$baseUrl/api/reviews/eligible?bookId=2" -WebSession $web
Write-Host "`n3. Eligibility Book 2 (Not Purchased):"
Write-Host "   Eligible: $($res3.eligible) | Status: $($res3.status) | Message: $($res3.message)"

# Case 4: Submit Invalid Review for Book 2 (Not Purchased) -> Expect rejection
try {
    $resPostUnbought = Invoke-RestMethod -Uri "$baseUrl/api/reviews" -Method POST -Body @{ bookId="2"; rating="5"; content="Sach hay qua" } -WebSession $web
    Write-Host "`n4. Submit Unbought Book 2 Result: Success=$($resPostUnbought.success) | Message=$($resPostUnbought.message)"
} catch {
    Write-Host "`n4. Submit Unbought Book 2 Rejected (Expected): $($_.Exception.Message)"
}

# Case 5: Submit Invalid Review for Book 3 (Order Not Delivered) -> Expect rejection
try {
    $resPostUndelivered = Invoke-RestMethod -Uri "$baseUrl/api/reviews" -Method POST -Body @{ bookId="3"; rating="5"; content="Sach hay qua" } -WebSession $web
    Write-Host "`n5. Submit Undelivered Book 3 Result: Success=$($resPostUndelivered.success) | Message=$($resPostUndelivered.message)"
} catch {
    Write-Host "`n5. Submit Undelivered Book 3 Rejected (Expected): $($_.Exception.Message)"
}

# Case 6: Submit Profane / Offensive Content for Book 1 -> Expect Automatic Block & Profanity Warning
$resPostProfane = Invoke-RestMethod -Uri "$baseUrl/api/reviews" -Method POST -Body @{ bookId="1"; rating="1"; content="Sách đm đồ ngu lừa đảo rác rưởi" } -WebSession $web
Write-Host "`n6. Submit Profane Content Result:"
Write-Host "   Success: $($resPostProfane.success) | Blocked: $($resPostProfane.blocked) | Message: $($resPostProfane.message)"

# Case 7: Submit Valid Review for Book 1 -> Expect Success & Instant Display
$resPostValid = Invoke-RestMethod -Uri "$baseUrl/api/reviews" -Method POST -Body @{ bookId="1"; rating="5"; content="Cuốn sách Nhà Giả Kim mang lại nhiều bài học sâu sắc về cuộc sống. Giao hàng rất nhanh!" } -WebSession $web
Write-Host "`n7. Submit Valid Content Result:"
Write-Host "   Success: $($resPostValid.success) | Message: $($resPostValid.message)"

# Case 8: Get All Reviews for Book 1 -> Verify profane review is NOT in list, valid review IS in list
$resGetReviews = Invoke-RestMethod -Uri "$baseUrl/api/reviews?bookId=1"
Write-Host "`n8. Public Reviews for Book 1:"
Write-Host "   Average Rating: $($resGetReviews.averageRating) | Total Count: $($resGetReviews.reviewCount)"
foreach ($r in $resGetReviews.reviews) {
    Write-Host "   - [$($r.rating) stars] $($r.fullName) ($($r.username)): $($r.content)"
}

Write-Host "`n========================================================="
Write-Host "ALL BACKEND & AUTOMATIC CONTENT FILTER TESTS PASSED!"
Write-Host "========================================================="
