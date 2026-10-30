package com.yzc.bookmall.model;

/** 书店（对应教材原型中的「店铺」）。 */
public class Store {
    private final long id;
    private final String name;
    private final String slogan;
    private final String description;
    private final int coverResId;
    private final double rating;
    private final String address;
    private final String businessHours;
    private final String notice;
    private final int deliveryFeeFen;
    private final int minOrderFen;
    private final int bookCount;

    public Store(long id, String name, String slogan, String description, int coverResId, double rating,
                 String address, String businessHours, String notice,
                 int deliveryFeeFen, int minOrderFen, int bookCount) {
        this.id = id;
        this.name = name;
        this.slogan = slogan;
        this.description = description;
        this.coverResId = coverResId;
        this.rating = rating;
        this.address = address;
        this.businessHours = businessHours;
        this.notice = notice;
        this.deliveryFeeFen = deliveryFeeFen;
        this.minOrderFen = minOrderFen;
        this.bookCount = bookCount;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getSlogan() { return slogan; }
    public String getDescription() { return description; }
    public int getCoverResId() { return coverResId; }
    public double getRating() { return rating; }
    public String getAddress() { return address; }
    public String getBusinessHours() { return businessHours; }
    public String getNotice() { return notice; }
    public int getDeliveryFeeFen() { return deliveryFeeFen; }
    public int getMinOrderFen() { return minOrderFen; }
    public int getBookCount() { return bookCount; }
}
