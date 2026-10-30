package com.yzc.bookmall.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.yzc.bookmall.R;

/**
 * 列表页统一的四态容器：加载中 / 内容 / 空 / 错误。
 * 与 RecyclerView 放在同一个 FrameLayout 中，通过 {@link #bind(View)} 关联内容视图，
 * 切换状态时自动控制两者的可见性，避免出现白屏。
 */
public class StateView extends LinearLayout {
    public static final int LOADING = 0;
    public static final int EMPTY = 1;
    public static final int ERROR = 2;
    public static final int CONTENT = 3;

    private final ProgressBar progressBar;
    private final TextView messageView;
    private final MaterialButton retryButton;
    private View contentView;
    private Runnable retryAction;
    private int state = CONTENT;

    public StateView(Context context) {
        this(context, null);
    }

    public StateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(context.getColor(R.color.surface));
        int padding = dp(28);
        setPadding(padding, padding, padding, padding);

        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        addView(progressBar, new LayoutParams(dp(36), dp(36)));

        messageView = new TextView(context);
        messageView.setTextSize(14);
        messageView.setTextColor(context.getColor(R.color.text_secondary));
        messageView.setGravity(Gravity.CENTER);
        LayoutParams messageParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = dp(14);
        addView(messageView, messageParams);

        retryButton = new MaterialButton(context);
        retryButton.setText(R.string.retry);
        retryButton.setTextSize(14);
        retryButton.setVisibility(GONE);
        LayoutParams retryParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        retryParams.topMargin = dp(16);
        addView(retryButton, retryParams);
        retryButton.setOnClickListener(v -> {
            if (retryAction != null) retryAction.run();
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** 关联列表或内容视图，切换状态时一并控制其可见性。 */
    public void bind(View content) {
        this.contentView = content;
        showContent();
    }

    public int getState() { return state; }

    public void showLoading() {
        state = LOADING;
        setVisibility(VISIBLE);
        progressBar.setVisibility(VISIBLE);
        messageView.setVisibility(VISIBLE);
        messageView.setText(R.string.loading);
        messageView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        retryButton.setVisibility(GONE);
        if (contentView != null) contentView.setVisibility(GONE);
    }

    public void showContent() {
        state = CONTENT;
        setVisibility(GONE);
        if (contentView != null) contentView.setVisibility(VISIBLE);
    }

    public void showEmpty(String message) {
        state = EMPTY;
        setVisibility(VISIBLE);
        progressBar.setVisibility(GONE);
        messageView.setVisibility(VISIBLE);
        messageView.setText(message == null || message.isEmpty() ? getContext().getString(R.string.empty_default) : message);
        messageView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        retryButton.setVisibility(GONE);
        if (contentView != null) contentView.setVisibility(GONE);
    }

    public void showError(String message, Runnable retry) {
        state = ERROR;
        setVisibility(VISIBLE);
        progressBar.setVisibility(GONE);
        messageView.setVisibility(VISIBLE);
        messageView.setText(message == null || message.isEmpty() ? getContext().getString(R.string.error_load) : message);
        messageView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        retryAction = retry;
        retryButton.setVisibility(retry == null ? GONE : VISIBLE);
        if (contentView != null) contentView.setVisibility(GONE);
    }

    public void showError(String message) {
        showError(message, null);
    }
}
