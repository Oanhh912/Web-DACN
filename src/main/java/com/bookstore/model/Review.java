package com.bookstore.model;

import java.sql.Timestamp;

public class Review {
    private int id;
    private String username;
    private String fullName;
    private String avatar;
    private int bookId;
    private String bookTitle;
    private Integer orderId;
    private int rating; // 1 to 5
    private String content;
    private String status; // "HIEN_THI" or "DA_CHAN"
    private String blockReason;
    private Timestamp createdAt;

    public Review() {}

    public Review(int id, String username, String fullName, String avatar, int bookId, String bookTitle, Integer orderId, int rating, String content, String status, String blockReason, Timestamp createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.avatar = avatar;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.orderId = orderId;
        this.rating = rating;
        this.content = content;
        this.status = status;
        this.blockReason = blockReason;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
