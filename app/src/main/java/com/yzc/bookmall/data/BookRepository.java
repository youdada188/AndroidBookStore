package com.yzc.bookmall.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.yzc.bookmall.model.Address;
import com.yzc.bookmall.model.Book;
import com.yzc.bookmall.model.CartItem;
import com.yzc.bookmall.model.Category;
import com.yzc.bookmall.model.Coupon;
import com.yzc.bookmall.model.Order;
import com.yzc.bookmall.model.OrderItem;
import com.yzc.bookmall.model.ReceiverInfo;
import com.yzc.bookmall.model.Review;
import com.yzc.bookmall.model.Store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** 数据库读写统一入口。所有金额以「分」为单位。 */
public class BookRepository {
    private static final String BOOK_COLUMNS =
            "b.id,b.store_id,b.category_id,c.name,b.title,b.author,b.publisher,b.price_fen,b.stock," +
            "b.cover_res_id,b.description,b.is_recommended,b.sales";
    private static final String BOOK_FROM =
            " FROM books b JOIN categories c ON c.id=b.category_id LEFT JOIN stores s ON s.id=b.store_id";
    private static final String ORDER_COLUMNS =
            "o.id,o.user_id,o.order_no,o.total_amount_fen,o.delivery_fee_fen,o.discount_fen,o.coupon_title," +
            "o.status,o.receiver_name,o.receiver_phone,o.receiver_address,o.created_at," +
            "(SELECT COUNT(*) FROM order_items oi WHERE oi.order_id=o.id)";
    private static final String ORDER_FROM = " FROM orders o";

    private final BookDbHelper dbHelper;

    public BookRepository(Context context) {
        dbHelper = new BookDbHelper(context);
    }

    // ---------------------------------------------------------------- 书店

    public List<Store> getStores() {
        String sql = "SELECT s.id,s.name,s.slogan,s.description,s.cover_res_id,s.rating,s.address," +
                "s.business_hours,s.notice,s.delivery_fee_fen,s.min_order_fen," +
                "(SELECT COUNT(*) FROM books b WHERE b.store_id=s.id) AS book_count" +
                " FROM stores s ORDER BY s.id ASC";
        List<Store> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) result.add(mapStore(c));
        }
        return result;
    }

    public Store getStoreById(long storeId) {
        String sql = "SELECT s.id,s.name,s.slogan,s.description,s.cover_res_id,s.rating,s.address," +
                "s.business_hours,s.notice,s.delivery_fee_fen,s.min_order_fen," +
                "(SELECT COUNT(*) FROM books b WHERE b.store_id=s.id) AS book_count" +
                " FROM stores s WHERE s.id=?";
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(storeId)})) {
            return c.moveToFirst() ? mapStore(c) : null;
        }
    }

    private Store mapStore(Cursor c) {
        return new Store(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4),
                c.getDouble(5), c.getString(6), c.getString(7), c.getString(8),
                c.getInt(9), c.getInt(10), c.getInt(11));
    }

    // ---------------------------------------------------------------- 图书

    public List<Category> getCategories() {
        List<Category> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().query(BookDbHelper.TABLE_CATEGORIES,
                new String[]{"id", "name"}, null, null, null, null, "id ASC")) {
            while (c.moveToNext()) result.add(new Category(c.getLong(0), c.getString(1)));
        }
        return result;
    }

    /** 某书店内实际有书的分类，用于店铺详情的筛选条。 */
    public List<Category> getCategoriesByStore(long storeId) {
        String sql = "SELECT DISTINCT c.id,c.name FROM categories c JOIN books b ON b.category_id=c.id" +
                " WHERE b.store_id=? ORDER BY c.id ASC";
        List<Category> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(storeId)})) {
            while (c.moveToNext()) result.add(new Category(c.getLong(0), c.getString(1)));
        }
        return result;
    }

    public List<Book> getAllBooks() { return queryBooks(null, null, "b.id ASC"); }

    public List<Book> getRecommendedBooks() { return queryBooks("b.is_recommended = 1", null, "b.id ASC"); }

    public List<Book> getBooksByCategory(long categoryId) {
        return queryBooks("b.category_id = ?", new String[]{String.valueOf(categoryId)}, "b.id ASC");
    }

    public List<Book> getBooksByStore(long storeId) {
        return queryBooks("b.store_id = ?", new String[]{String.valueOf(storeId)}, "b.sales DESC, b.id ASC");
    }

    public List<Book> getBooksByStoreAndCategory(long storeId, long categoryId) {
        return queryBooks("b.store_id = ? AND b.category_id = ?",
                new String[]{String.valueOf(storeId), String.valueOf(categoryId)}, "b.sales DESC, b.id ASC");
    }

    public List<Book> searchBooks(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        if (value.isEmpty()) return Collections.emptyList();
        String pattern = "%" + value + "%";
        return queryBooks("b.title LIKE ? OR b.author LIKE ? OR c.name LIKE ? OR s.name LIKE ?",
                new String[]{pattern, pattern, pattern, pattern}, "b.is_recommended DESC, b.sales DESC");
    }

    public Book getBookById(long bookId) {
        List<Book> books = queryBooks("b.id = ?", new String[]{String.valueOf(bookId)}, null);
        return books.isEmpty() ? null : books.get(0);
    }

    private List<Book> queryBooks(String selection, String[] args, String orderBy) {
        StringBuilder sql = new StringBuilder("SELECT ").append(BOOK_COLUMNS).append(BOOK_FROM);
        if (selection != null && !selection.isEmpty()) sql.append(" WHERE ").append(selection);
        if (orderBy != null && !orderBy.isEmpty()) sql.append(" ORDER BY ").append(orderBy);
        List<Book> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql.toString(), args)) {
            while (c.moveToNext()) result.add(mapBook(c));
        }
        return result;
    }

    private Book mapBook(Cursor c) {
        return new Book(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3), c.getString(4),
                c.getString(5), c.getString(6), c.getInt(7), c.getInt(8), c.getInt(9),
                c.getString(10), c.getInt(11) == 1, c.getInt(12));
    }

    // ---------------------------------------------------------------- 账号

    public long registerUser(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.isEmpty()) return -1;
        ContentValues v = new ContentValues();
        v.put("username", username.trim());
        v.put("password", password);
        v.put("created_at", System.currentTimeMillis());
        try {
            return dbHelper.getWritableDatabase().insertOrThrow(BookDbHelper.TABLE_USERS, null, v);
        } catch (SQLiteConstraintException e) {
            return -1;
        }
    }

    public long authenticate(String username, String password) {
        if (username == null || password == null) return -1;
        try (Cursor c = dbHelper.getReadableDatabase().query(BookDbHelper.TABLE_USERS, new String[]{"id"},
                "username=? AND password=?", new String[]{username.trim(), password}, null, null, null)) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        }
    }

    public String getUsername(long userId) {
        try (Cursor c = dbHelper.getReadableDatabase().query(BookDbHelper.TABLE_USERS, new String[]{"username"},
                "id=?", new String[]{String.valueOf(userId)}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }

    // ---------------------------------------------------------------- 收藏

    public boolean isFavorite(long userId, long bookId) {
        try (Cursor c = dbHelper.getReadableDatabase().query(BookDbHelper.TABLE_FAVORITES, new String[]{"id"},
                "user_id=? AND book_id=?", new String[]{String.valueOf(userId), String.valueOf(bookId)},
                null, null, null)) {
            return c.moveToFirst();
        }
    }

    public void setFavorite(long userId, long bookId, boolean favorite) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (favorite) {
            ContentValues v = new ContentValues();
            v.put("user_id", userId);
            v.put("book_id", bookId);
            v.put("created_at", System.currentTimeMillis());
            db.insertWithOnConflict(BookDbHelper.TABLE_FAVORITES, null, v, SQLiteDatabase.CONFLICT_IGNORE);
        } else {
            db.delete(BookDbHelper.TABLE_FAVORITES, "user_id=? AND book_id=?",
                    new String[]{String.valueOf(userId), String.valueOf(bookId)});
        }
    }

    public List<Book> getFavoriteBooks(long userId) {
        return queryBooks("EXISTS (SELECT 1 FROM favorites f WHERE f.book_id=b.id AND f.user_id=?)",
                new String[]{String.valueOf(userId)}, "b.id ASC");
    }

    // ---------------------------------------------------------------- 购物车

    public List<CartItem> getCartItems(long userId) {
        String sql = "SELECT ci.id,ci.user_id,ci.book_id,b.title,b.author,b.price_fen,b.cover_res_id,b.stock,ci.quantity" +
                " FROM cart_items ci JOIN books b ON b.id=ci.book_id WHERE ci.user_id=? ORDER BY ci.id DESC";
        List<CartItem> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                result.add(new CartItem(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3),
                        c.getString(4), c.getInt(5), c.getInt(6), c.getInt(7), c.getInt(8)));
            }
        }
        return result;
    }

    public int getCartCount(long userId) {
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT IFNULL(SUM(quantity),0) FROM cart_items WHERE user_id=?",
                new String[]{String.valueOf(userId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** 重复加入同一本书时合并数量，并夹紧到库存上限。 */
    public void addToCart(long userId, long bookId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("购买数量必须大于 0");
        Book book = getBookById(bookId);
        if (book == null) throw new IllegalStateException("图书不存在");
        if (book.getStock() <= 0) throw new IllegalStateException("这本书已经售罄了");
        int existing = 0;
        try (Cursor c = dbHelper.getReadableDatabase().query(BookDbHelper.TABLE_CART, new String[]{"quantity"},
                "user_id=? AND book_id=?", new String[]{String.valueOf(userId), String.valueOf(bookId)},
                null, null, null)) {
            if (c.moveToFirst()) existing = c.getInt(0);
        }
        int target = existing + quantity;
        if (target > book.getStock()) throw new IllegalStateException("库存仅剩 " + book.getStock() + " 本");
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("book_id", bookId);
        v.put("quantity", target);
        v.put("created_at", System.currentTimeMillis());
        dbHelper.getWritableDatabase()
                .insertWithOnConflict(BookDbHelper.TABLE_CART, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void updateCartQuantity(long userId, long bookId, int quantity) {
        if (quantity <= 0) {
            deleteCartItem(userId, bookId);
            return;
        }
        Book book = getBookById(bookId);
        if (book == null) throw new IllegalStateException("图书不存在");
        if (quantity > book.getStock()) throw new IllegalStateException("库存仅剩 " + book.getStock() + " 本");
        ContentValues v = new ContentValues();
        v.put("quantity", quantity);
        dbHelper.getWritableDatabase().update(BookDbHelper.TABLE_CART, v,
                "user_id=? AND book_id=?", new String[]{String.valueOf(userId), String.valueOf(bookId)});
    }

    public void deleteCartItem(long userId, long bookId) {
        dbHelper.getWritableDatabase().delete(BookDbHelper.TABLE_CART,
                "user_id=? AND book_id=?", new String[]{String.valueOf(userId), String.valueOf(bookId)});
    }

    // ---------------------------------------------------------------- 订单

    private String generateOrderNo() {
        return String.format(Locale.US, "BM%d%03d", System.currentTimeMillis(), new Random().nextInt(1000));
    }

    private void validateReceiver(ReceiverInfo info) {
        if (info == null || isBlank(info.getName()) || isBlank(info.getPhone()) || isBlank(info.getAddress())) {
            throw new IllegalStateException("请完整填写收货信息");
        }
    }

    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    /**
     * 提交订单：单事务完成库存复核、订单写入、明细快照、库存扣减与购物车清理。
     * 任一商品库存不足则整单回滚。
     */
    public long createOrder(long userId, List<CartItem> selectedItems, ReceiverInfo receiverInfo,
                            int deliveryFeeFen, int discountFen, String couponTitle, long couponId) {
        if (selectedItems == null || selectedItems.isEmpty()) throw new IllegalStateException("请选择要结算的图书");
        validateReceiver(receiverInfo);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int goodsFen = 0;
            for (CartItem item : selectedItems) {
                Book current = getBookInTransaction(db, item.getBookId());
                if (current == null) throw new IllegalStateException("图书已下架：" + item.getTitle());
                if (item.getQuantity() <= 0 || item.getQuantity() > current.getStock()) {
                    throw new IllegalStateException("库存不足：" + item.getTitle() + "（仅剩 " + current.getStock() + " 本）");
                }
                goodsFen += current.getPriceFen() * item.getQuantity();
            }
            int safeDiscount = Math.max(0, Math.min(discountFen, goodsFen));
            int totalFen = Math.max(0, goodsFen + deliveryFeeFen - safeDiscount);

            ContentValues order = new ContentValues();
            order.put("user_id", userId);
            order.put("order_no", generateOrderNo());
            order.put("total_amount_fen", totalFen);
            order.put("delivery_fee_fen", deliveryFeeFen);
            order.put("discount_fen", safeDiscount);
            order.put("coupon_title", couponTitle);
            order.put("status", "待发货");
            order.put("receiver_name", receiverInfo.getName().trim());
            order.put("receiver_phone", receiverInfo.getPhone().trim());
            order.put("receiver_address", receiverInfo.getAddress().trim());
            order.put("created_at", System.currentTimeMillis());
            long orderId = db.insertOrThrow(BookDbHelper.TABLE_ORDERS, null, order);

            for (CartItem item : selectedItems) {
                Book current = getBookInTransaction(db, item.getBookId());
                ContentValues detail = new ContentValues();
                detail.put("order_id", orderId);
                detail.put("book_id", current.getId());
                detail.put("title_snapshot", current.getTitle());
                detail.put("price_snapshot_fen", current.getPriceFen());
                detail.put("quantity", item.getQuantity());
                detail.put("cover_res_id", current.getCoverResId());
                db.insertOrThrow(BookDbHelper.TABLE_ORDER_ITEMS, null, detail);

                ContentValues stock = new ContentValues();
                stock.put("stock", current.getStock() - item.getQuantity());
                db.update(BookDbHelper.TABLE_BOOKS, stock, "id=?", new String[]{String.valueOf(current.getId())});
                db.delete(BookDbHelper.TABLE_CART, "user_id=? AND book_id=?",
                        new String[]{String.valueOf(userId), String.valueOf(item.getBookId())});
            }
            if (couponId > 0) {
                ContentValues used = new ContentValues();
                used.put("used", 1);
                db.update(BookDbHelper.TABLE_USER_COUPONS, used, "user_id=? AND coupon_id=?",
                        new String[]{String.valueOf(userId), String.valueOf(couponId)});
            }
            db.setTransactionSuccessful();
            return orderId;
        } finally {
            db.endTransaction();
        }
    }

    private Book getBookInTransaction(SQLiteDatabase db, long bookId) {
        String sql = "SELECT " + BOOK_COLUMNS + BOOK_FROM + " WHERE b.id=?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(bookId)})) {
            return c.moveToFirst() ? mapBook(c) : null;
        }
    }

    public List<Order> getOrders(long userId) {
        String sql = "SELECT " + ORDER_COLUMNS + ORDER_FROM + " WHERE o.user_id=? ORDER BY o.created_at DESC, o.id DESC";
        List<Order> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) result.add(mapOrder(c));
        }
        return result;
    }

    public Order getOrder(long userId, long orderId) {
        String sql = "SELECT " + ORDER_COLUMNS + ORDER_FROM + " WHERE o.user_id=? AND o.id=?";
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql,
                new String[]{String.valueOf(userId), String.valueOf(orderId)})) {
            return c.moveToFirst() ? mapOrder(c) : null;
        }
    }

    private Order mapOrder(Cursor c) {
        return new Order(c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3), c.getInt(4), c.getInt(5),
                c.getString(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10),
                c.getLong(11), c.getInt(12));
    }

    public List<OrderItem> getOrderItems(long orderId) {
        List<OrderItem> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().query(BookDbHelper.TABLE_ORDER_ITEMS,
                new String[]{"id", "order_id", "book_id", "title_snapshot", "price_snapshot_fen", "quantity", "cover_res_id"},
                "order_id=?", new String[]{String.valueOf(orderId)}, null, null, "id ASC")) {
            while (c.moveToNext()) {
                result.add(new OrderItem(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3),
                        c.getInt(4), c.getInt(5), c.getInt(6)));
            }
        }
        return result;
    }

    public void confirmReceipt(long orderId, long userId) {
        ContentValues v = new ContentValues();
        v.put("status", "已完成");
        dbHelper.getWritableDatabase().update(BookDbHelper.TABLE_ORDERS, v,
                "id=? AND user_id=? AND status=?", new String[]{String.valueOf(orderId), String.valueOf(userId), "待发货"});
    }

    // ---------------------------------------------------------------- 地址

    public List<Address> getAddresses(long userId) {
        List<Address> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().query(BookDbHelper.TABLE_ADDRESSES,
                new String[]{"id", "user_id", "name", "phone", "region", "detail", "is_default"},
                "user_id=?", new String[]{String.valueOf(userId)}, null, null, "is_default DESC, id DESC")) {
            while (c.moveToNext()) result.add(mapAddress(c));
        }
        return result;
    }

    public Address getDefaultAddress(long userId) {
        List<Address> list = getAddresses(userId);
        return list.isEmpty() ? null : list.get(0);
    }

    private Address mapAddress(Cursor c) {
        return new Address(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3),
                c.getString(4), c.getString(5), c.getInt(6) == 1);
    }

    /** 保存地址；isDefault 为 true 时先把该用户其他地址的默认标记清掉。 */
    public long saveAddress(long userId, long addressId, String name, String phone,
                            String region, String detail, boolean isDefault) {
        if (isBlank(name) || isBlank(phone) || isBlank(region) || isBlank(detail)) {
            throw new IllegalStateException("请完整填写收货信息");
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            boolean first = getAddresses(userId).isEmpty();
            ContentValues v = new ContentValues();
            v.put("user_id", userId);
            v.put("name", name.trim());
            v.put("phone", phone.trim());
            v.put("region", region.trim());
            v.put("detail", detail.trim());
            v.put("is_default", (isDefault || first) ? 1 : 0);
            v.put("created_at", System.currentTimeMillis());
            long id = addressId;
            if (addressId > 0) {
                db.update(BookDbHelper.TABLE_ADDRESSES, v, "id=? AND user_id=?",
                        new String[]{String.valueOf(addressId), String.valueOf(userId)});
            } else {
                id = db.insertOrThrow(BookDbHelper.TABLE_ADDRESSES, null, v);
            }
            if (isDefault || first) setDefaultAddressInternal(db, userId, id);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void setDefaultAddress(long userId, long addressId) {
        setDefaultAddressInternal(dbHelper.getWritableDatabase(), userId, addressId);
    }

    private void setDefaultAddressInternal(SQLiteDatabase db, long userId, long addressId) {
        ContentValues clear = new ContentValues();
        clear.put("is_default", 0);
        db.update(BookDbHelper.TABLE_ADDRESSES, clear, "user_id=?", new String[]{String.valueOf(userId)});
        ContentValues set = new ContentValues();
        set.put("is_default", 1);
        db.update(BookDbHelper.TABLE_ADDRESSES, set, "id=? AND user_id=?",
                new String[]{String.valueOf(addressId), String.valueOf(userId)});
    }

    public void deleteAddress(long userId, long addressId) {
        dbHelper.getWritableDatabase().delete(BookDbHelper.TABLE_ADDRESSES,
                "id=? AND user_id=?", new String[]{String.valueOf(addressId), String.valueOf(userId)});
    }

    // ---------------------------------------------------------------- 评价

    public List<Review> getReviewsByBook(long bookId, int limit) {
        String sql = "SELECT r.id,r.user_id,u.username,r.book_id,b.title,r.rating,r.content,r.created_at" +
                " FROM reviews r JOIN users u ON u.id=r.user_id JOIN books b ON b.id=r.book_id" +
                " WHERE r.book_id=? ORDER BY r.created_at DESC" + (limit > 0 ? " LIMIT " + limit : "");
        List<Review> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(bookId)})) {
            while (c.moveToNext()) result.add(mapReview(c));
        }
        return result;
    }

    public List<Review> getReviewsByUser(long userId) {
        String sql = "SELECT r.id,r.user_id,u.username,r.book_id,b.title,r.rating,r.content,r.created_at" +
                " FROM reviews r JOIN users u ON u.id=r.user_id JOIN books b ON b.id=r.book_id" +
                " WHERE r.user_id=? ORDER BY r.created_at DESC";
        List<Review> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) result.add(mapReview(c));
        }
        return result;
    }

    private Review mapReview(Cursor c) {
        return new Review(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getString(4),
                c.getInt(5), c.getString(6), c.getLong(7));
    }

    public double getAverageRating(long bookId) {
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT IFNULL(AVG(rating),0) FROM reviews WHERE book_id=?", new String[]{String.valueOf(bookId)})) {
            return c.moveToFirst() ? c.getDouble(0) : 0;
        }
    }

    public int getReviewCount(long bookId) {
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM reviews WHERE book_id=?", new String[]{String.valueOf(bookId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public boolean hasReviewed(long userId, long bookId) {
        try (Cursor c = dbHelper.getReadableDatabase().query(BookDbHelper.TABLE_REVIEWS, new String[]{"id"},
                "user_id=? AND book_id=?", new String[]{String.valueOf(userId), String.valueOf(bookId)},
                null, null, null)) {
            return c.moveToFirst();
        }
    }

    /** 只有买过这本书（存在包含该书的订单）才允许评价。 */
    public boolean canReview(long userId, long bookId) {
        if (userId <= 0) return false;
        if (hasReviewed(userId, bookId)) return false;
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT 1 FROM orders o JOIN order_items oi ON oi.order_id=o.id WHERE o.user_id=? AND oi.book_id=? LIMIT 1",
                new String[]{String.valueOf(userId), String.valueOf(bookId)})) {
            return c.moveToFirst();
        }
    }

    public long addReview(long userId, long bookId, int rating, String content) {
        if (rating <= 0) throw new IllegalStateException("请先选择评分");
        if (isBlank(content)) throw new IllegalStateException("请填写评价内容");
        long orderId = findOrderIdForBook(userId, bookId);
        if (orderId <= 0) throw new IllegalStateException("购买并完成订单后才能评价");
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("book_id", bookId);
        v.put("order_id", orderId);
        v.put("rating", rating);
        v.put("content", content.trim());
        v.put("created_at", System.currentTimeMillis());
        return dbHelper.getWritableDatabase()
                .insertWithOnConflict(BookDbHelper.TABLE_REVIEWS, null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private long findOrderIdForBook(long userId, long bookId) {
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT o.id FROM orders o JOIN order_items oi ON oi.order_id=o.id" +
                        " WHERE o.user_id=? AND oi.book_id=? ORDER BY o.created_at DESC LIMIT 1",
                new String[]{String.valueOf(userId), String.valueOf(bookId)})) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        }
    }

    // ---------------------------------------------------------------- 优惠券

    /** 为当前用户发放全部券（已领过的忽略）。 */
    public void claimCoupons(long userId) {
        if (userId <= 0) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try (Cursor c = db.query(BookDbHelper.TABLE_COUPONS, new String[]{"id"}, null, null, null, null, null)) {
            while (c.moveToNext()) {
                ContentValues v = new ContentValues();
                v.put("user_id", userId);
                v.put("coupon_id", c.getLong(0));
                v.put("used", 0);
                db.insertWithOnConflict(BookDbHelper.TABLE_USER_COUPONS, null, v, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
    }

    public List<Coupon> getCoupons(long userId) {
        claimCoupons(userId);
        String sql = "SELECT c.id,c.title,c.threshold_fen,c.discount_fen,c.valid_until,uc.used" +
                " FROM user_coupons uc JOIN coupons c ON c.id=uc.coupon_id WHERE uc.user_id=?" +
                " ORDER BY c.threshold_fen ASC";
        List<Coupon> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                result.add(new Coupon(c.getLong(0), c.getString(1), c.getInt(2), c.getInt(3),
                        c.getLong(4), c.getInt(5) == 1));
            }
        }
        return result;
    }

    /** 返回满足门槛且未使用、优惠最大的券。 */
    public Coupon findBestCoupon(long userId, int goodsAmountFen) {
        Coupon best = null;
        for (Coupon coupon : getCoupons(userId)) {
            if (!coupon.isUsable(goodsAmountFen)) continue;
            if (best == null || coupon.getDiscountFen() > best.getDiscountFen()) best = coupon;
        }
        return best;
    }

    // ---------------------------------------------------------------- 数据清理

    public void clearCurrentUserData(long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            String[] args = new String[]{String.valueOf(userId)};
            db.delete(BookDbHelper.TABLE_FAVORITES, "user_id=?", args);
            db.delete(BookDbHelper.TABLE_CART, "user_id=?", args);
            db.delete(BookDbHelper.TABLE_REVIEWS, "user_id=?", args);
            db.delete(BookDbHelper.TABLE_ADDRESSES, "user_id=?", args);
            db.delete(BookDbHelper.TABLE_USER_COUPONS, "user_id=?", args);
            try (Cursor c = db.query(BookDbHelper.TABLE_ORDERS, new String[]{"id"}, "user_id=?", args, null, null, null)) {
                while (c.moveToNext()) {
                    db.delete(BookDbHelper.TABLE_ORDER_ITEMS, "order_id=?", new String[]{String.valueOf(c.getLong(0))});
                }
            }
            db.delete(BookDbHelper.TABLE_ORDERS, "user_id=?", args);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
