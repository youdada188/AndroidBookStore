package com.yzc.bookmall.model;

/** 订单。totalAmountFen 为实付金额（商品合计 + 配送费 − 优惠）。 */
public class Order {
    private final long id;
    private final long userId;
    private final String orderNo;
    private final int totalAmountFen;
    private final int deliveryFeeFen;
    private final int discountFen;
    private final String couponTitle;
    private final String status;
    private final String receiverName;
    private final String receiverPhone;
    private final String receiverAddress;
    private final long createdAt;
    private final int itemCount;

    public Order(long id, long userId, String orderNo, int totalAmountFen, int deliveryFeeFen, int discountFen,
                 String couponTitle, String status, String receiverName, String receiverPhone,
                 String receiverAddress, long createdAt, int itemCount) {
        this.id = id;
        this.userId = userId;
        this.orderNo = orderNo;
        this.totalAmountFen = totalAmountFen;
        this.deliveryFeeFen = deliveryFeeFen;
        this.discountFen = discountFen;
        this.couponTitle = couponTitle;
        this.status = status;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.createdAt = createdAt;
        this.itemCount = itemCount;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getOrderNo() { return orderNo; }
    public int getTotalAmountFen() { return totalAmountFen; }
    public int getDeliveryFeeFen() { return deliveryFeeFen; }
    public int getDiscountFen() { return discountFen; }
    public String getCouponTitle() { return couponTitle; }
    public String getStatus() { return status; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public long getCreatedAt() { return createdAt; }
    public int getItemCount() { return itemCount; }

    /** 商品合计 = 实付 − 配送费 + 优惠。 */
    public int getGoodsAmountFen() { return totalAmountFen - deliveryFeeFen + discountFen; }
    public boolean isPending() { return "待发货".equals(status); }
}
