package com.yzc.bookmall.ui;

import android.content.Context;
import android.widget.Toast;

/** 轻量 UI 工具：尺寸换算、提示与文本处理。 */
public final class Ui {
    private Ui() { }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void toast(Context context, String message) {
        if (context == null) return;
        Toast.makeText(context, message == null || message.isEmpty() ? "操作失败" : message, Toast.LENGTH_SHORT).show();
    }

    public static void toast(Context context, int resId) {
        if (context == null) return;
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    /** 取名字首字符做头像，空名回退为「书」。 */
    public static String initial(String name) {
        if (name == null || name.trim().isEmpty()) return "书";
        return name.trim().substring(0, 1).toUpperCase();
    }
}
