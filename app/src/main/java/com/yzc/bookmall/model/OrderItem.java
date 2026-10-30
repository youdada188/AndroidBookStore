package com.yzc.bookmall.model;

public class OrderItem {
    private final long id; private final long orderId; private final long bookId;
    private final String titleSnapshot; private final int priceSnapshotFen;
    private final int quantity; private final int coverResId;
    public OrderItem(long id, long orderId, long bookId, String titleSnapshot,
                     int priceSnapshotFen, int quantity, int coverResId) {
        this.id=id; this.orderId=orderId; this.bookId=bookId; this.titleSnapshot=titleSnapshot;
        this.priceSnapshotFen=priceSnapshotFen; this.quantity=quantity; this.coverResId=coverResId;
    }
    public long getId() { return id; }
    public long getOrderId() { return orderId; }
    public long getBookId() { return bookId; }
    public String getTitleSnapshot() { return titleSnapshot; }
    public int getPriceSnapshotFen() { return priceSnapshotFen; }
    public int getQuantity() { return quantity; }
    public int getCoverResId() { return coverResId; }
    public int getSubtotalFen() { return priceSnapshotFen * quantity; }
}