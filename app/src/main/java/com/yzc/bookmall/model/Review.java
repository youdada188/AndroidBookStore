package com.yzc.bookmall.model;

/** 图书评价。 */
public class Review {
    private final long id;
    private final long userId;
    private final String username;
    private final long bookId;
    private final String bookTitle;
    private final int rating;
    private final String content;
    private final long createdAt;

    public Review(long id, long userId, String username, long bookId, String bookTitle,
                  int rating, String content, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getUsername() { return username; }
    public long getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public int getRating() { return rating; }
    public String getContent() { return content; }
    public long getCreatedAt() { return createdAt; }
}
