package com.yzc.bookmall.model;

/** 图书实体。价格以「分」为单位保存，避免浮点误差。 */
public class Book {
    private final long id;
    private final long storeId;
    private final long categoryId;
    private final String categoryName;
    private final String title;
    private final String author;
    private final String publisher;
    private final int priceFen;
    private final int stock;
    private final int coverResId;
    private final String description;
    private final boolean recommended;
    private final int sales;

    public Book(long id, long storeId, long categoryId, String categoryName, String title, String author,
                String publisher, int priceFen, int stock, int coverResId,
                String description, boolean recommended, int sales) {
        this.id = id;
        this.storeId = storeId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.priceFen = priceFen;
        this.stock = stock;
        this.coverResId = coverResId;
        this.description = description;
        this.recommended = recommended;
        this.sales = sales;
    }

    public long getId() { return id; }
    public long getStoreId() { return storeId; }
    public long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public int getPriceFen() { return priceFen; }
    public int getStock() { return stock; }
    public int getCoverResId() { return coverResId; }
    public String getDescription() { return description; }
    public boolean isRecommended() { return recommended; }
    public int getSales() { return sales; }
    public boolean isSoldOut() { return stock <= 0; }
}
