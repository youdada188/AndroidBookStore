package com.yzc.bookmall.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Coupon;
import com.yzc.bookmall.ui.AddressListActivity;
import com.yzc.bookmall.ui.FavoriteActivity;
import com.yzc.bookmall.ui.LoginActivity;
import com.yzc.bookmall.ui.OrderListActivity;
import com.yzc.bookmall.ui.ReviewListActivity;
import com.yzc.bookmall.ui.Ui;
import com.yzc.bookmall.util.FormatUtils;
import com.yzc.bookmall.util.SessionManager;

import java.util.List;

/** 个人中心：用户信息、我的订单/收藏/地址/评价/优惠券、退出登录与清空数据。 */
public class ProfileFragment extends Fragment {
    private SessionManager session;
    private BookRepository repository;
    private TextView avatar;
    private TextView name;
    private TextView subtitle;
    private LinearLayout menuContainer;
    private View logoutButton;
    private View clearButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        repository = new BookRepository(requireContext());

        avatar = view.findViewById(R.id.tv_avatar);
        name = view.findViewById(R.id.tv_profile_name);
        subtitle = view.findViewById(R.id.tv_profile_sub);
        menuContainer = view.findViewById(R.id.menu_container);
        logoutButton = view.findViewById(R.id.btn_logout);
        clearButton = view.findViewById(R.id.tv_clear_data);

        addMenu(R.drawable.ic_receipt, getString(R.string.profile_orders),
                () -> startActivity(new Intent(requireContext(), OrderListActivity.class)));
        addMenu(R.drawable.ic_heart, getString(R.string.profile_favorites),
                () -> startActivity(new Intent(requireContext(), FavoriteActivity.class)));
        addMenu(R.drawable.ic_location, getString(R.string.profile_addresses),
                () -> startActivity(new Intent(requireContext(), AddressListActivity.class)));
        addMenu(R.drawable.ic_rate, getString(R.string.profile_reviews),
                () -> startActivity(new Intent(requireContext(), ReviewListActivity.class)));
        addMenu(R.drawable.ic_ticket, getString(R.string.profile_coupons), this::showCoupons);

        logoutButton.setOnClickListener(v -> confirmLogout());
        clearButton.setOnClickListener(v -> confirmClearData());
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void addMenu(int iconRes, String label, Runnable action) {
        View item = getLayoutInflater().inflate(R.layout.item_menu, menuContainer, false);
        ((ImageView) item.findViewById(R.id.iv_menu_icon)).setImageResource(iconRes);
        ((TextView) item.findViewById(R.id.tv_menu_label)).setText(label);
        item.setOnClickListener(v -> {
            if (!session.isLoggedIn()) {
                openLogin();
                return;
            }
            action.run();
        });
        menuContainer.addView(item);
    }

    private void refresh() {
        boolean loggedIn = session.isLoggedIn();
        if (loggedIn) {
            avatar.setText(Ui.initial(session.getUsername()));
            name.setText(getString(R.string.profile_greeting, session.getUsername()));
            subtitle.setText("已登录 · 你的阅读记录保存在本地");
        } else {
            avatar.setText("书");
            name.setText("未登录");
            subtitle.setText(getString(R.string.profile_guest));
        }
        logoutButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        clearButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
    }

    private void openLogin() {
        startActivity(new Intent(requireContext(), LoginActivity.class).putExtra("redirect_tab", "profile"));
    }

    private void showCoupons() {
        List<Coupon> coupons = repository.getCoupons(session.getUserId());
        if (coupons.isEmpty()) {
            Ui.toast(requireContext(), R.string.coupon_empty);
            return;
        }
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int horizontal = Ui.dp(requireContext(), 22);
        container.setPadding(horizontal, Ui.dp(requireContext(), 8), horizontal, 0);
        for (Coupon coupon : coupons) {
            TextView row = new TextView(requireContext());
            row.setText(coupon.getTitle() + "　" + (coupon.isUsed() ? "已使用" : "未使用")
                    + "\n有效期至 " + FormatUtils.formatDate(coupon.getValidUntil()));
            row.setTextSize(13);
            row.setLineSpacing(Ui.dp(requireContext(), 2), 1f);
            row.setTextColor(requireContext().getColor(
                    coupon.isUsed() ? R.color.text_hint : R.color.text_primary));
            row.setPadding(0, Ui.dp(requireContext(), 8), 0, Ui.dp(requireContext(), 8));
            container.addView(row);
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.coupon_title)
                .setView(container)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_logout)
                .setMessage(R.string.profile_logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    session.clearSession();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .show();
    }

    private void confirmClearData() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_clear)
                .setMessage(R.string.profile_clear_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    repository.clearCurrentUserData(session.getUserId());
                    session.clearSession();
                    Ui.toast(requireContext(), R.string.profile_clear_done);
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .show();
    }
}
