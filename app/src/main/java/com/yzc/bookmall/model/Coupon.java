package com.yzc.bookmall.model;

/** 优惠券（满减券）。 */
public class Coupon {
    private final long id;
    private final String title;
    private final int thresholdFen;
    private final int discountFen;
    private final long validUntil;
    private final boolean used;

    public Coupon(long id, String title, int thresholdFen, int discountFen, long validUntil, boolean used) {
        this.id = id;
        this.title = title;
        this.thresholdFen = thresholdFen;
        this.discountFen = discountFen;
        this.validUntil = validUntil;
        this.used = used;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public int getThresholdFen() { return thresholdFen; }
    public int getDiscountFen() { return discountFen; }
    public long getValidUntil() { return validUntil; }
    public boolean isUsed() { return used; }

    /** 是否满足使用门槛。 */
    public boolean isUsable(int goodsAmountFen) { return !used && goodsAmountFen >= thresholdFen; }
}
