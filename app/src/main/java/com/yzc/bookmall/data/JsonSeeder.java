package com.yzc.bookmall.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.yzc.bookmall.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 从 assets/seed 下的 JSON 文件读取店铺、分类、图书与优惠券数据并写入 SQLite。
 * 首次建库时调用 {@link #seedAll}；数据库升级时调用 {@link #refreshCatalog}，
 * 后者按主键 update-or-insert，不会删除已有行，避免级联删除用户的收藏与购物车。
 */
public final class JsonSeeder {
    private static final String TAG = "JsonSeeder";
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private JsonSeeder() { }

    public static void seedAll(SQLiteDatabase db, Context context) {
        try {
            seedStores(db, context, false);
            seedCategories(db, context, false);
            seedBooks(db, context, false);
            seedCoupons(db, context, false);
        } catch (Exception e) {
            Log.e(TAG, "种子数据解析失败，改用最小数据集", e);
            fallback(db, context);
        }
    }

    public static void refreshCatalog(SQLiteDatabase db, Context context) {
        try {
            seedStores(db, context, true);
            seedCategories(db, context, true);
            seedBooks(db, context, true);
            seedCoupons(db, context, true);
        } catch (Exception e) {
            Log.e(TAG, "目录刷新失败", e);
        }
    }

    private static void seedStores(SQLiteDatabase db, Context context, boolean updateFirst) throws Exception {
        JSONArray array = new JSONArray(readAsset(context, "stores.json"));
        long now = System.currentTimeMillis();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            ContentValues v = new ContentValues();
            v.put("id", o.getInt("id"));
            v.put("name", o.getString("name"));
            v.put("slogan", o.getString("slogan"));
            v.put("description", o.getString("description"));
            v.put("cover_res_id", drawableId(context, o.getString("cover")));
            v.put("rating", o.getDouble("rating"));
            v.put("address", o.getString("address"));
            v.put("business_hours", o.getString("businessHours"));
            v.put("notice", o.getString("notice"));
            v.put("delivery_fee_fen", o.getInt("deliveryFeeFen"));
            v.put("min_order_fen", o.getInt("minOrderFen"));
            v.put("created_at", now);
            write(db, BookDbHelper.TABLE_STORES, v, updateFirst);
        }
    }

    private static void seedCategories(SQLiteDatabase db, Context context, boolean updateFirst) throws Exception {
        JSONArray array = new JSONArray(readAsset(context, "categories.json"));
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            ContentValues v = new ContentValues();
            v.put("id", o.getInt("id"));
            v.put("name", o.getString("name"));
            write(db, BookDbHelper.TABLE_CATEGORIES, v, updateFirst);
        }
    }

    private static void seedBooks(SQLiteDatabase db, Context context, boolean updateFirst) throws Exception {
        JSONArray array = new JSONArray(readAsset(context, "books.json"));
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            ContentValues v = new ContentValues();
            v.put("id", o.getInt("id"));
            v.put("store_id", o.getInt("storeId"));
            v.put("category_id", o.getInt("categoryId"));
            v.put("title", o.getString("title"));
            v.put("author", o.getString("author"));
            v.put("publisher", o.getString("publisher"));
            v.put("price_fen", o.getInt("priceFen"));
            v.put("stock", o.getInt("stock"));
            v.put("cover_res_id", drawableId(context, o.getString("cover")));
            v.put("description", o.getString("description"));
            v.put("is_recommended", o.getBoolean("recommended") ? 1 : 0);
            v.put("sales", o.optInt("sales", 0));
            write(db, BookDbHelper.TABLE_BOOKS, v, updateFirst);
        }
    }

    private static void seedCoupons(SQLiteDatabase db, Context context, boolean updateFirst) throws Exception {
        JSONArray array = new JSONArray(readAsset(context, "coupons.json"));
        long now = System.currentTimeMillis();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            ContentValues v = new ContentValues();
            v.put("id", o.getInt("id"));
            v.put("title", o.getString("title"));
            v.put("threshold_fen", o.getInt("thresholdFen"));
            v.put("discount_fen", o.getInt("discountFen"));
            v.put("valid_until", now + o.optInt("validDays", 30) * DAY_MILLIS);
            write(db, BookDbHelper.TABLE_COUPONS, v, updateFirst);
        }
    }

    /** 升级场景先尝试 UPDATE，命中则保留原行；未命中再 INSERT。 */
    private static void write(SQLiteDatabase db, String table, ContentValues values, boolean updateFirst) {
        if (updateFirst) {
            long id = values.getAsLong("id");
            int rows = db.update(table, values, "id=?", new String[]{String.valueOf(id)});
            if (rows > 0) return;
        }
        db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static String readAsset(Context context, String name) throws Exception {
        try (InputStream in = context.getAssets().open("seed/" + name);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static int drawableId(Context context, String name) {
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        return id != 0 ? id : R.drawable.book_cover_1;
    }

    /** JSON 不可用时的兜底数据，保证首页不是空白。 */
    private static void fallback(SQLiteDatabase db, Context context) {
        long now = System.currentTimeMillis();
        ContentValues store = new ContentValues();
        store.put("id", 1);
        store.put("name", "书墨自营店");
        store.put("slogan", "离线也能读到好书");
        store.put("description", "默认书店。");
        store.put("cover_res_id", R.drawable.store_1);
        store.put("rating", 4.5);
        store.put("address", "本地");
        store.put("business_hours", "09:00 - 21:00");
        store.put("notice", "欢迎光临");
        store.put("delivery_fee_fen", 0);
        store.put("min_order_fen", 0);
        store.put("created_at", now);
        db.insertWithOnConflict(BookDbHelper.TABLE_STORES, null, store, SQLiteDatabase.CONFLICT_IGNORE);

        ContentValues category = new ContentValues();
        category.put("id", 1);
        category.put("name", "文学");
        db.insertWithOnConflict(BookDbHelper.TABLE_CATEGORIES, null, category, SQLiteDatabase.CONFLICT_IGNORE);

        ContentValues book = new ContentValues();
        book.put("id", 1);
        book.put("store_id", 1);
        book.put("category_id", 1);
        book.put("title", "落在我身上的雨");
        book.put("author", "陆庆屹");
        book.put("publisher", "新星出版社");
        book.put("price_fen", 5900);
        book.put("stock", 10);
        book.put("cover_res_id", R.drawable.book_001);
        book.put("description", "默认图书数据。");
        book.put("is_recommended", 1);
        book.put("sales", 0);
        db.insertWithOnConflict(BookDbHelper.TABLE_BOOKS, null, book, SQLiteDatabase.CONFLICT_IGNORE);
    }
}
