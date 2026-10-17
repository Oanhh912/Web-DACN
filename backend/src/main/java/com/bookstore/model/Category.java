package com.bookstore.model;

import java.io.Serializable;

/**
 * Model đại diện cho Danh mục sách (Bảng DANH_MUC / categories)
 */
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String code;
    private String name;
    private String description;
    private String status = "ACTIVE"; // "ACTIVE" hoặc "INACTIVE"
    private int bookCount; // Số lượng sách thuộc danh mục

    public Category() {
    }

    public Category(int id, String code, String name, String description, String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ACTIVE";
    }

    public Category(int id, String code, String name, String description, String status, int bookCount) {
        this(id, code, name, description, status);
        this.bookCount = bookCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return (status != null && !status.trim().isEmpty()) ? status.toUpperCase() : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ACTIVE";
    }

    public int getBookCount() {
        return bookCount;
    }

    public void setBookCount(int bookCount) {
        this.bookCount = bookCount;
    }
}
