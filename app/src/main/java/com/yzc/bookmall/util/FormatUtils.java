package com.yzc.bookmall.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 金额、时间与评分的统一格式化。金额在数据库中一律以「分」为单位。 */
public final class FormatUtils {
    private FormatUtils() { }

    /** 1299 → "¥12.99" */
    public static String formatPrice(int priceFen) {
        return "¥" + yuan(priceFen);
    }

    /** 1299 → "12.99" */
    public static String yuan(int priceFen) {
        return String.format(Locale.CHINA, "%.2f", priceFen / 100.0);
    }

    /** 时间戳 → "2026-10-30 19:08" */
    public static String formatDate(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    /** 时间戳 → "10-30 19:08" */
    public static String formatDateShort(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    /** 4 → "★★★★☆" */
    public static String stars(int rating) {
        int value = Math.max(0, Math.min(5, rating));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 5; i++) builder.append(i < value ? '★' : '☆');
        return builder.toString();
    }

    /** 平均分 → "4.6" */
    public static String rating(double value) {
        return String.format(Locale.CHINA, "%.1f", value);
    }
}
