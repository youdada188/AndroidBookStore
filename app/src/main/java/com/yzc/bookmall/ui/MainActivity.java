package com.yzc.bookmall.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.yzc.bookmall.R;
import com.yzc.bookmall.ui.fragment.CartFragment;
import com.yzc.bookmall.ui.fragment.CategoryFragment;
import com.yzc.bookmall.ui.fragment.HomeFragment;
import com.yzc.bookmall.ui.fragment.ProfileFragment;

/** 主框架：底部导航 + Fragment 容器，承载书店、分类、购物车与个人中心四个页面。 */
public class MainActivity extends BaseActivity {

    private boolean containerFilled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyInsets(findViewById(R.id.root));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            switchTo(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            int initial = resolveInitialTab();
            // setSelectedItemId 会同步触发上面的监听器；若未触发则兜底直接提交
            bottomNav.setSelectedItemId(initial);
            if (!containerFilled) switchTo(initial);
        }
    }

    private int resolveInitialTab() {
        String tab = getIntent().getStringExtra("redirect_tab");
        if ("cart".equals(tab)) return R.id.nav_cart;
        if ("profile".equals(tab)) return R.id.nav_profile;
        if ("category".equals(tab)) return R.id.nav_category;
        return R.id.nav_home;
    }

    private void switchTo(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_home) fragment = new HomeFragment();
        else if (itemId == R.id.nav_category) fragment = new CategoryFragment();
        else if (itemId == R.id.nav_cart) fragment = new CartFragment();
        else fragment = new ProfileFragment();
        containerFilled = true;
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
