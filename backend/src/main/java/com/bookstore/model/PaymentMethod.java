package com.bookstore.model;

/**
 * Phương thức thanh toán (Bảng PHUONG_THUC_TT)
 */
public class PaymentMethod {
    private int id;
    private String code; // COD, ONLINE
    private String name;
    private String description;
    private boolean isActive;

    public PaymentMethod() {}

    public PaymentMethod(int id, String code, String name, String description, boolean isActive) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
