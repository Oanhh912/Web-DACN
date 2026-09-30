package com.bookstore.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn hàng của khách hàng (Bảng DON_HANG)
 */
public class Order {
    private int id;
    private String orderCode;
    private String username;
    private int addressId;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private String voucherCode;
    private double subtotal;
    private double discountAmount;
    private double shippingFee;
    private double totalAmount;
    private String paymentMethodCode; // COD, ONLINE
    private String paymentMethodName;
    private String paymentStatus;     // PENDING, PAID, FAILED
    private String orderStatus;       // CHO_XAC_NHAN, DANG_XU_LY, DANG_GIAO, HOAN_THANH, DA_HUY
    private String note;
    private String transactionId;
    private String createdAt;
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Order(int id, String orderCode, String username, int addressId, String recipientName,
                 String recipientPhone, String deliveryAddress, String voucherCode, double subtotal,
                 double discountAmount, double shippingFee, double totalAmount, String paymentMethodCode,
                 String paymentMethodName, String paymentStatus, String orderStatus, String note,
                 String transactionId, String createdAt) {
        this.id = id;
        this.orderCode = orderCode;
        this.username = username;
        this.addressId = addressId;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.deliveryAddress = deliveryAddress;
        this.voucherCode = voucherCode;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.paymentMethodCode = paymentMethodCode;
        this.paymentMethodName = paymentMethodName;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.note = note;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getAddressId() { return addressId; }
    public void setAddressId(int addressId) { this.addressId = addressId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getShippingFee() { return shippingFee; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethodCode() { return paymentMethodCode; }
    public void setPaymentMethodCode(String paymentMethodCode) { this.paymentMethodCode = paymentMethodCode; }

    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getFormattedSubtotal() {
        return new DecimalFormat("###,###,### đ").format(subtotal);
    }

    public String getFormattedDiscount() {
        return new DecimalFormat("###,###,### đ").format(discountAmount);
    }

    public String getFormattedShipping() {
        if (shippingFee <= 0) return "Miễn phí (Freeship)";
        return new DecimalFormat("###,###,### đ").format(shippingFee);
    }

    public String getFormattedTotal() {
        return new DecimalFormat("###,###,### đ").format(totalAmount);
    }

    public String getPaymentStatusBadge() {
        if ("PAID".equalsIgnoreCase(paymentStatus)) {
            return "<span class=\"badge-pay badge-paid\"><i class=\"fas fa-circle-check\"></i> Đã thanh toán</span>";
        } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
            return "<span class=\"badge-pay badge-failed\"><i class=\"fas fa-circle-xmark\"></i> Thất bại</span>";
        } else {
            return "<span class=\"badge-pay badge-pending\"><i class=\"fas fa-clock\"></i> Chưa thanh toán (COD)</span>";
        }
    }

    public String getOrderStatusBadge() {
        switch (orderStatus) {
            case "CHO_XAC_NHAN":
                return "<span class=\"badge-order status-pending\"><i class=\"fas fa-hourglass-half\"></i> Chờ xác nhận</span>";
            case "DANG_XU_LY":
                return "<span class=\"badge-order status-processing\"><i class=\"fas fa-boxes-packing\"></i> Đang xử lý</span>";
            case "DANG_GIAO":
                return "<span class=\"badge-order status-shipping\"><i class=\"fas fa-truck-fast\"></i> Đang giao hàng</span>";
            case "HOAN_THANH":
                return "<span class=\"badge-order status-completed\"><i class=\"fas fa-check-double\"></i> Hoàn thành</span>";
            case "DA_HUY":
                return "<span class=\"badge-order status-cancelled\"><i class=\"fas fa-ban\"></i> Đã hủy</span>";
            default:
                return "<span class=\"badge-order status-pending\">" + orderStatus + "</span>";
        }
    }

    public String getFormattedTotalAmount() {
        return getFormattedTotal();
    }

    public String getOrderStatusText() {
        if ("CHO_XAC_NHAN".equalsIgnoreCase(orderStatus)) return "Chờ xác nhận";
        if ("DANG_XU_LY".equalsIgnoreCase(orderStatus)) return "Đang xử lý";
        if ("DANG_GIAO".equalsIgnoreCase(orderStatus)) return "Đang giao hàng";
        if ("HOAN_THANH".equalsIgnoreCase(orderStatus)) return "Đã hoàn thành";
        if ("DA_HUY".equalsIgnoreCase(orderStatus)) return "Đã hủy";
        return orderStatus != null ? orderStatus : "";
    }
}
