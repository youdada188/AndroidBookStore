package com.yzc.bookmall.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 数据库定义与升级。
 * 版本 3 引入书店、评价、地址与优惠券；升级采用增量迁移（加表 / 加列 + 目录刷新），
 * 不再删表重建，避免用户收藏、购物车与订单丢失。
 */
public class BookDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "BookDbHelper";

    public static final String DATABASE_NAME = "book_mall.db";
    public static final int DATABASE_VERSION = 3;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_STORES = "stores";
    public static final String TABLE_BOOKS = "books";
    public static final String TABLE_FAVORITES = "favorites";
    public static final String TABLE_CART = "cart_items";
    public static final String TABLE_ORDERS = "orders";
    public static final String TABLE_ORDER_ITEMS = "order_items";
    public static final String TABLE_REVIEWS = "reviews";
    public static final String TABLE_ADDRESSES = "addresses";
    public static final String TABLE_COUPONS = "coupons";
    public static final String TABLE_USER_COUPONS = "user_coupons";

    private final Context context;

    public BookDbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        // stores 必须先于 books 建立，books.store_id 的外键才有指向
        createStores(db);
        createCoreTables(db);
        createV3Tables(db);
        seedDemoUser(db);
        JsonSeeder.seedAll(db, context);
    }

    private void createStores(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS stores (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, slogan TEXT NOT NULL, description TEXT NOT NULL, cover_res_id INTEGER NOT NULL, rating REAL NOT NULL DEFAULT 5.0, address TEXT NOT NULL, business_hours TEXT NOT NULL, notice TEXT NOT NULL, delivery_fee_fen INTEGER NOT NULL DEFAULT 0, min_order_fen INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");
    }

    private void createCoreTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)");
        db.execSQL("CREATE TABLE books (id INTEGER PRIMARY KEY AUTOINCREMENT, store_id INTEGER NOT NULL DEFAULT 1, category_id INTEGER NOT NULL, title TEXT NOT NULL, author TEXT NOT NULL, publisher TEXT NOT NULL, price_fen INTEGER NOT NULL, stock INTEGER NOT NULL DEFAULT 0, cover_res_id INTEGER NOT NULL, description TEXT NOT NULL, is_recommended INTEGER NOT NULL DEFAULT 0, sales INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(category_id) REFERENCES categories(id), FOREIGN KEY(store_id) REFERENCES stores(id))");
        db.execSQL("CREATE TABLE favorites (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, book_id INTEGER NOT NULL, created_at INTEGER NOT NULL, UNIQUE(user_id, book_id), FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE cart_items (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, book_id INTEGER NOT NULL, quantity INTEGER NOT NULL, created_at INTEGER NOT NULL, UNIQUE(user_id, book_id), FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, order_no TEXT NOT NULL UNIQUE, total_amount_fen INTEGER NOT NULL, delivery_fee_fen INTEGER NOT NULL DEFAULT 0, discount_fen INTEGER NOT NULL DEFAULT 0, coupon_title TEXT, status TEXT NOT NULL, receiver_name TEXT NOT NULL, receiver_phone TEXT NOT NULL, receiver_address TEXT NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL, book_id INTEGER NOT NULL, title_snapshot TEXT NOT NULL, price_snapshot_fen INTEGER NOT NULL, quantity INTEGER NOT NULL, cover_res_id INTEGER NOT NULL, FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE)");
    }

    private void createV3Tables(SQLiteDatabase db) {
        createStores(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS reviews (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, book_id INTEGER NOT NULL, order_id INTEGER NOT NULL, rating INTEGER NOT NULL, content TEXT NOT NULL, created_at INTEGER NOT NULL, UNIQUE(user_id, book_id), FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS addresses (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL, region TEXT NOT NULL, detail TEXT NOT NULL, is_default INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS coupons (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, threshold_fen INTEGER NOT NULL, discount_fen INTEGER NOT NULL, valid_until INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS user_coupons (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, coupon_id INTEGER NOT NULL, used INTEGER NOT NULL DEFAULT 0, UNIQUE(user_id, coupon_id), FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY(coupon_id) REFERENCES coupons(id) ON DELETE CASCADE)");
    }

    private void seedDemoUser(SQLiteDatabase db) {
        ContentValues demo = new ContentValues();
        demo.put("username", "demo");
        demo.put("password", "123456");
        demo.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_USERS, null, demo, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            createV3Tables(db);
            addColumnIfMissing(db, TABLE_BOOKS, "store_id", "INTEGER NOT NULL DEFAULT 1");
            addColumnIfMissing(db, TABLE_BOOKS, "sales", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, TABLE_ORDERS, "delivery_fee_fen", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, TABLE_ORDERS, "discount_fen", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, TABLE_ORDERS, "coupon_title", "TEXT");
            JsonSeeder.refreshCatalog(db, context);
        }
    }

    private void addColumnIfMissing(SQLiteDatabase db, String table, String column, String definition) {
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (Exception e) {
            Log.w(TAG, "列 " + column + " 已存在，跳过", e);
        }
    }
}
