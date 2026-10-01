package com.bookstore.model;

import java.io.Serializable;

public class ChatbotContent implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String categoryType; // 'THONG_TIN_SACH', 'CHINH_SACH_MUA_HANG', 'CHINH_SACH_THANH_TOAN', 'CHINH_SACH_GIAO_HANG', 'CAU_HOI_THUONG_GAP'
    private String title;
    private String content;
    private String keywords;
    private String status; // 'ACTIVE', 'INACTIVE'
    private String createdAt;
    private String updatedAt;

    public ChatbotContent() {
        this.status = "ACTIVE";
    }

    public ChatbotContent(int id, String categoryType, String title, String content, String keywords, String status, String createdAt, String updatedAt) {
        this.id = id;
        this.categoryType = categoryType;
        this.title = title;
        this.content = content;
        this.keywords = keywords;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCategoryTypeName() {
        if (categoryType == null) return "Chưa phân loại";
        switch (categoryType.toUpperCase()) {
            case "THONG_TIN_SACH":
                return "Thông tin sách";
            case "CHINH_SACH_MUA_HANG":
                return "Chính sách mua hàng";
            case "CHINH_SACH_THANH_TOAN":
                return "Chính sách thanh toán";
            case "CHINH_SACH_GIAO_HANG":
                return "Chính sách giao hàng";
            case "CAU_HOI_THUONG_GAP":
                return "Câu hỏi thường gặp";
            default:
                return categoryType;
        }
    }
}
