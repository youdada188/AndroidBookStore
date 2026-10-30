package com.yzc.bookmall.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 所有页面的基类：统一处理 edge-to-edge 布局。
 * targetSdk 35 起系统强制边到边，必须自己把系统栏高度补成内边距，
 * 否则标题会被状态栏压住（Android 15 及以上的已知表现）。
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applySystemBarAppearance(true);
    }

    /** 状态栏 / 导航栏图标是否为深色。深色背景页面传 false。 */
    protected void applySystemBarAppearance(boolean lightIcons) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(lightIcons);
        controller.setAppearanceLightNavigationBars(lightIcons);
    }

    /** 把系统栏高度叠加到根布局原有内边距上。 */
    protected void applyInsets(View root) {
        if (root == null) return;
        final int start = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int end = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(start + bars.left, top + bars.top, end + bars.right, bottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
