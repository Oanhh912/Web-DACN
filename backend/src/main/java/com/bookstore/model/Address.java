package com.bookstore.model;

/**
 * Đại diện cho địa chỉ nhận hàng của khách hàng (Bảng DIA_CHI)
 */
public class Address {
    private int id;
    private int userId;
    private String username;
    private String recipientName;
    private String phone;
    private String addressDetail;
    private String province;
    private String district;
    private String ward;
    private boolean isDefault;
    private String createdAt;

    public Address() {}

    public Address(int id, int userId, String username, String recipientName, String phone,
                   String addressDetail, String province, String district, String ward, boolean isDefault) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.recipientName = recipientName;
        this.phone = phone;
        this.addressDetail = addressDetail;
        this.province = province;
        this.district = district;
        this.ward = ward;
        this.isDefault = isDefault;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddressDetail() { return addressDetail; }
    public void setAddressDetail(String addressDetail) { this.addressDetail = addressDetail; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressDetail != null && !addressDetail.isEmpty()) sb.append(addressDetail);
        if (ward != null && !ward.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward);
        }
        if (district != null && !district.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(district);
        }
        if (province != null && !province.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(province);
        }
        return sb.toString();
    }
}
