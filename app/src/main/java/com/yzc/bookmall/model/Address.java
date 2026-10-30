package com.yzc.bookmall.model;

/** 收货地址。 */
public class Address {
    private final long id;
    private final long userId;
    private final String name;
    private final String phone;
    private final String region;
    private final String detail;
    private final boolean isDefault;

    public Address(long id, long userId, String name, String phone, String region, String detail, boolean isDefault) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.region = region;
        this.detail = detail;
        this.isDefault = isDefault;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getRegion() { return region; }
    public String getDetail() { return detail; }
    public boolean isDefault() { return isDefault; }
    public String getFullAddress() { return region + " " + detail; }
    public ReceiverInfo toReceiverInfo() { return new ReceiverInfo(name, phone, getFullAddress()); }
}
