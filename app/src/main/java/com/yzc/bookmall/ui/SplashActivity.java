package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.util.SessionManager;

/** 启动页：Logo 展示 + 3 秒倒计时，可跳过；按登录状态决定进入首页还是登录页。 */
public class SplashActivity extends BaseActivity {
    private static final int TOTAL_SECONDS = 3;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView countdownView;
    private int remaining = TOTAL_SECONDS;
    private boolean navigated;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            remaining--;
            if (remaining <= 0) {
                goNext();
                return;
            }
            countdownView.setText(getString(R.string.splash_countdown, remaining));
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        applySystemBarAppearance(false);
        applyInsets(findViewById(R.id.root));

        countdownView = findViewById(R.id.tv_countdown);
        countdownView.setText(getString(R.string.splash_countdown, remaining));
        findViewById(R.id.tv_skip).setOnClickListener(v -> goNext());
        handler.postDelayed(ticker, 1000);
    }

    private void goNext() {
        if (navigated) return;
        navigated = true;
        handler.removeCallbacksAndMessages(null);
        SessionManager session = new SessionManager(this);
        Intent intent = new Intent(this, session.isLoggedIn() ? MainActivity.class : LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
