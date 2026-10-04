$baseUrl = "http://localhost:8080"

# Create WebClient / HttpWebRequest to test login and review endpoints directly
$req = [System.Net.HttpWebRequest]::Create("$baseUrl/login")
$req.Method = "POST"
$req.AllowAutoRedirect = $false
$req.ContentType = "application/x-www-form-urlencoded"
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes("username=oanh&password=123456")
$req.ContentLength = $bodyBytes.Length
$reqStream = $req.GetRequestStream()
$reqStream.Write($bodyBytes, 0, $bodyBytes.Length)
$reqStream.Close()

$resp = $req.GetResponse()
$setCookie = $resp.Headers["Set-Cookie"]
$resp.Close()

Write-Host "Set-Cookie Header: $setCookie"
$sessionToken = ""
if ($setCookie -match "BOOKSTORE_SESSION=([^;]+)") {
    $sessionToken = $matches[1]
}
Write-Host "Extracted Session Token for 'oanh': $sessionToken"

if (-not $sessionToken) {
    Write-Host "ERROR: Login failed"
    exit 1
}

# 1. Eligibility Check Book 1 (Bought & Delivered)
$wc = New-Object System.Net.WebClient
$wc.Headers.Add("Cookie", "BOOKSTORE_SESSION=$sessionToken")
$wc.Encoding = [System.Text.Encoding]::UTF8

$json1 = $wc.DownloadString("$baseUrl/api/reviews/eligible?bookId=1")
Write-Host "`n1. Book 1 (Bought & Delivered) Eligibility Response: $json1"

# 2. Eligibility Check Book 3 (Order Not Delivered)
$json2 = $wc.DownloadString("$baseUrl/api/reviews/eligible?bookId=3")
Write-Host "`n2. Book 3 (Order Not Delivered) Eligibility Response: $json2"

# 3. Eligibility Check Book 2 (Not Purchased)
$json3 = $wc.DownloadString("$baseUrl/api/reviews/eligible?bookId=2")
Write-Host "`n3. Book 2 (Not Purchased) Eligibility Response: $json3"

# 4. Submit Unbought Book 2 -> Expect Rejection
try {
    $wcPost = New-Object System.Net.WebClient
    $wcPost.Headers.Add("Cookie", "BOOKSTORE_SESSION=$sessionToken")
    $wcPost.Headers.Add("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    $res4 = $wcPost.UploadString("$baseUrl/api/reviews", "POST", "bookId=2&rating=5&content=Sach+hay+qua")
    Write-Host "`n4. Unbought Book 2 Post Response: $res4"
} catch [System.Net.WebException] {
    $errStream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($errStream)
    Write-Host "`n4. Unbought Book 2 Rejected Response: "$reader.ReadToEnd()
}

# 5. Submit Undelivered Book 3 -> Expect Rejection
try {
    $wcPost = New-Object System.Net.WebClient
    $wcPost.Headers.Add("Cookie", "BOOKSTORE_SESSION=$sessionToken")
    $wcPost.Headers.Add("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    $res5 = $wcPost.UploadString("$baseUrl/api/reviews", "POST", "bookId=3&rating=5&content=Sach+hay+qua")
    Write-Host "`n5. Undelivered Book 3 Post Response: $res5"
} catch [System.Net.WebException] {
    $errStream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($errStream)
    Write-Host "`n5. Undelivered Book 3 Rejected Response: "$reader.ReadToEnd()
}

# 6. Submit Profane Content for Book 1 -> Expect Automatic Block
$wcPost = New-Object System.Net.WebClient
$wcPost.Headers.Add("Cookie", "BOOKSTORE_SESSION=$sessionToken")
$wcPost.Headers.Add("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
$wcPost.Encoding = [System.Text.Encoding]::UTF8
$res6 = $wcPost.UploadString("$baseUrl/api/reviews", "POST", "bookId=1&rating=1&content=Sách+đm+đồ+ngu+lừa+đảo+rác+rưởi")
Write-Host "`n6. Profane Review Response: $res6"

# 7. Submit Valid Content for Book 1 -> Expect Success
$wcPost = New-Object System.Net.WebClient
$wcPost.Headers.Add("Cookie", "BOOKSTORE_SESSION=$sessionToken")
$wcPost.Headers.Add("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
$wcPost.Encoding = [System.Text.Encoding]::UTF8
$res7 = $wcPost.UploadString("$baseUrl/api/reviews", "POST", "bookId=1&rating=5&content=Cuốn+sách+Nhà+Giả+Kim+mang+lại+nhiều+bài+học+sâu+sắc+về+cuộc+sống.+Giao+hàng+rất+nhanh!")
Write-Host "`n7. Valid Review Response: $res7"

# 8. Public Reviews for Book 1
$jsonReviews = $wc.DownloadString("$baseUrl/api/reviews?bookId=1")
Write-Host "`n8. Public Reviews List for Book 1: $jsonReviews"
