package com.yzc.bookmall.model;

public class CartItem {
    private final long id; private final long userId; private final long bookId;
    private final String title; private final String author; private final int priceFen;
    private final int coverResId; private final int stock; private final int quantity;
    public CartItem(long id, long userId, long bookId, String title, String author,
                    int priceFen, int coverResId, int quantity) {
        this(id, userId, bookId, title, author, priceFen, coverResId, Integer.MAX_VALUE, quantity);
    }
    public CartItem(long id, long userId, long bookId, String title, String author,
                    int priceFen, int coverResId, int stock, int quantity) {
        this.id = id; this.userId = userId; this.bookId = bookId; this.title = title;
        this.author = author; this.priceFen = priceFen; this.coverResId = coverResId;
        this.stock = stock; this.quantity = quantity;
    }
    public long getId() { return id; }
    public long getUserId() { return userId; }
    public long getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getPriceFen() { return priceFen; }
    public int getCoverResId() { return coverResId; }
    public int getStock() { return stock; }
    public int getQuantity() { return quantity; }
    public int getSubtotalFen() { return priceFen * quantity; }
}