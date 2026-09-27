package com.bookstore.model;

/**
 * Chi tiết đơn hàng (Bảng CT_DON_HANG)
 */
public class OrderItem {
    private int id;
    private int orderId;
    private int bookId;
    private String bookCode;
    private String bookTitle;
    private String bookImage;
    private double price;
    private int quantity;
    private double subtotal;

    public OrderItem() {}

    public OrderItem(int id, int orderId, int bookId, String bookCode, String bookTitle,
                     String bookImage, double price, int quantity, double subtotal) {
        this.id = id;
        this.orderId = orderId;
        this.bookId = bookId;
        this.bookCode = bookCode;
        this.bookTitle = bookTitle;
        this.bookImage = bookImage;
        this.price = price;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public OrderItem(int bookId, String bookCode, String bookTitle, String bookImage, double price, int quantity) {
        this.bookId = bookId;
        this.bookCode = bookCode;
        this.bookTitle = bookTitle;
        this.bookImage = bookImage;
        this.price = price;
        this.quantity = quantity;
        this.subtotal = price * quantity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getBookCode() { return bookCode; }
    public void setBookCode(String bookCode) { this.bookCode = bookCode; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookImage() { return bookImage; }
    public void setBookImage(String bookImage) { this.bookImage = bookImage; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public String getFormattedPrice() {
        return new java.text.DecimalFormat("###,###,### đ").format(price);
    }

    public String getFormattedSubtotal() {
        return new java.text.DecimalFormat("###,###,### đ").format(subtotal);
    }
}
