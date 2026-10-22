$files = @(
    "frontend/public/admin/books.html",
    "frontend/public/admin/categories.html",
    "frontend/public/admin/promotions.html",
    "frontend/public/admin/reports.html",
    "frontend/public/admin/chatbot.html"
)

$orderItemHtml = @"
                    <li class="sidebar-nav-item">
                        <a href="/admin/orders" class="sidebar-nav-link">
                            <i class="fas fa-receipt"></i>
                            <span>Quản lý đơn hàng</span>
                        </a>
                    </li>
"@

foreach ($f in $files) {
    if (Test-Path $f) {
        $content = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
        if (-not $content.Contains('/admin/orders')) {
            # Find the position right before <li class="sidebar-nav-item">\s*<a href="/admin/customers"
            $pattern = '(?m)^(\s*<li class="sidebar-nav-item">\s*<a href="/admin/customers")'
            $replacement = "$orderItemHtml`r`n`$1"
            $newContent = [regex]::Replace($content, $pattern, $replacement)
            [System.IO.File]::WriteAllText($f, $newContent, [System.Text.Encoding]::UTF8)
            Write-Host "Updated: $f"
        }
    }
}
