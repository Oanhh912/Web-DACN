package com.bookstore.model;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Model đại diện cho một cuốn sách trong cửa hàng.
 */
public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String code;
    private String title;
    private String author;
    private String publisher;
    private double price;
    private double originalPrice;
    private String category;
    private int stock;
    private double rating;
    private int reviewCount;
    private String image;
    private String description;
    private String promotion;
    private boolean isBestSeller;

    public Book() {
        this.publisher = "NXB Trẻ";
        this.stock = 10;
        this.promotion = "Tặng bookmark độc quyền";
    }

    public Book(int id, String title, String author, double price, double originalPrice, 
                String category, double rating, int reviewCount, String image, 
                String description, boolean isBestSeller) {
        this(id, String.format("MS%03d", id), title, author, "NXB Trẻ", price, originalPrice, category, 10, rating, reviewCount, image, description, "Tặng bookmark độc quyền", isBestSeller);
    }

    public Book(int id, String code, String title, String author, double price, double originalPrice, 
                String category, double rating, int reviewCount, String image, 
                String description, boolean isBestSeller) {
        this(id, code, title, author, "NXB Trẻ", price, originalPrice, category, 10, rating, reviewCount, image, description, "Tặng bookmark độc quyền", isBestSeller);
    }

    public Book(int id, String code, String title, String author, String publisher, double price, double originalPrice, 
                String category, int stock, double rating, int reviewCount, String image, 
                String description, String promotion, boolean isBestSeller) {
        this.id = id;
        this.code = (code != null && !code.trim().isEmpty()) ? code.trim() : String.format("MS%03d", id);
        this.title = title;
        this.author = author;
        this.publisher = (publisher != null && !publisher.trim().isEmpty()) ? publisher.trim() : "NXB Trẻ";
        this.price = price;
        this.originalPrice = originalPrice;
        this.category = category;
        this.stock = Math.max(0, stock);
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.image = image;
        this.description = description;
        this.promotion = (promotion != null && !promotion.trim().isEmpty()) ? promotion.trim() : "Tặng bookmark độc quyền";
        this.isBestSeller = isBestSeller;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isBestSeller() {
        return isBestSeller;
    }

    public void setBestSeller(boolean bestSeller) {
        isBestSeller = bestSeller;
    }

    /**
     * Định dạng giá tiền theo định dạng Tiếng Việt (VNĐ)
     */
    public String getFormattedPrice() {
        NumberFormat nf = NumberFormat.getInstance(Locale.of("vi", "VN"));
        return nf.format((long) price) + " đ";
    }

    public String getFormattedOriginalPrice() {
        if (originalPrice <= 0) return "";
        NumberFormat nf = NumberFormat.getInstance(Locale.of("vi", "VN"));
        return nf.format((long) originalPrice) + " đ";
    }

    public String getPublisher() {
        return publisher != null ? publisher : "NXB Trẻ";
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = Math.max(0, stock);
    }

    public String getPromotion() {
        return promotion != null ? promotion : "";
    }

    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }

    public boolean isOutOfStock() {
        return stock <= 0;
    }

    public String getStockStatusText() {
        if (isOutOfStock()) {
            return "Hết hàng";
        }
        return "Còn hàng (" + stock + ")";
    }

    public int getDiscountPercent() {
        if (originalPrice > price && originalPrice > 0) {
            return (int) Math.round(((originalPrice - price) / originalPrice) * 100);
        }
        return 0;
    }
}
