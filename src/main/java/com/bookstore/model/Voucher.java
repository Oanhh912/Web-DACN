package com.bookstore.model;

import java.time.LocalDate;

/**
 * Đại diện cho mã giảm giá / khuyến mại áp dụng khi đặt hàng (Bảng MA_GIAM_GIA)
 */
public class Voucher {
    private int id;
    private String code;
    private String title;
    private String description;
    private String discountType; // "PERCENT" hoặc "FIXED"
    private double discountValue;
    private double minOrderAmount;
    private double maxDiscountAmount;
    private String startDate; // yyyy-MM-dd
    private String endDate;   // yyyy-MM-dd
    private boolean isActive;
    private int usageLimit;
    private int usedCount;

    public Voucher() {}

    public Voucher(int id, String code, String title, String description, String discountType,
                   double discountValue, double minOrderAmount, double maxDiscountAmount,
                   String startDate, String endDate, boolean isActive, int usageLimit, int usedCount) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public double getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }

    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }

    /**
     * Kiểm tra điều kiện áp dụng mã giảm giá theo quy tắc nghiệp vụ
     * @param subtotal Tổng tiền hàng hiện tại
     * @return Thông báo lỗi nếu không hợp lệ, hoặc null nếu hợp lệ
     */
    public String validate(double subtotal) {
        if (!isActive) {
            return "Mã giảm giá '" + code + "' hiện đang bị tạm khóa hoặc chưa được kích hoạt.";
        }

        LocalDate today = LocalDate.now();
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                LocalDate start = LocalDate.parse(startDate.trim());
                if (today.isBefore(start)) {
                    return "Mã giảm giá '" + code + "' chưa đến thời gian áp dụng (bắt đầu từ " + startDate + ").";
                }
            } catch (Exception ignored) {}
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                LocalDate end = LocalDate.parse(endDate.trim());
                if (today.isAfter(end)) {
                    return "Mã giảm giá '" + code + "' đã hết hạn sử dụng (hết hạn ngày " + endDate + ").";
                }
            } catch (Exception ignored) {}
        }

        if (subtotal < minOrderAmount) {
            java.text.DecimalFormat df = new java.text.DecimalFormat("###,###,### đ");
            return "Đơn hàng cần đạt tối thiểu " + df.format(minOrderAmount) + " để áp dụng mã '" + code + "'.";
        }

        if (usageLimit > 0 && usedCount >= usageLimit) {
            return "Mã giảm giá '" + code + "' đã hết số lượt sử dụng.";
        }

        return null; // Hợp lệ
    }

    /**
     * Tính toán số tiền thực tế được giảm
     * @param subtotal Tổng tiền hàng
     * @return Số tiền được chiết khấu
     */
    public double calculateDiscount(double subtotal) {
        if (validate(subtotal) != null) return 0;

        double discount = 0;
        if ("PERCENT".equalsIgnoreCase(discountType)) {
            discount = subtotal * (discountValue / 100.0);
            if (maxDiscountAmount > 0 && discount > maxDiscountAmount) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = discountValue;
        }

        if (discount > subtotal) {
            discount = subtotal;
        }
        return Math.round(discount);
    }
}
