package com.yzc.bookmall.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.CartItem;
import com.yzc.bookmall.ui.BookDetailActivity;
import com.yzc.bookmall.ui.CheckoutActivity;
import com.yzc.bookmall.ui.LoginActivity;
import com.yzc.bookmall.ui.Ui;
import com.yzc.bookmall.ui.adapter.CartAdapter;
import com.yzc.bookmall.ui.widget.StateView;
import com.yzc.bookmall.util.FormatUtils;
import com.yzc.bookmall.util.SessionManager;

import java.util.List;

/** 购物车：勾选、全选、增减数量、删除与合计。 */
public class CartFragment extends Fragment implements CartAdapter.Listener {
    private static final long FAKE_LOADING_DELAY = 250L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable loader = this::loadNow;

    private BookRepository repository;
    private SessionManager session;
    private CartAdapter adapter;
    private StateView stateView;
    private CheckBox selectAll;
    private TextView totalView;
    private MaterialButton checkoutButton;
    private boolean bindingSelectAll;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new BookRepository(requireContext());
        session = new SessionManager(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.rv_cart);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CartAdapter(this);
        recyclerView.setAdapter(adapter);

        stateView = view.findViewById(R.id.state_view);
        stateView.bind(recyclerView);

        selectAll = view.findViewById(R.id.cb_all);
        totalView = view.findViewById(R.id.tv_cart_total);
        checkoutButton = view.findViewById(R.id.btn_checkout);

        selectAll.setOnCheckedChangeListener((buttonView, checked) -> {
            if (bindingSelectAll) return;
            adapter.setAllSelected(checked);
        });
        checkoutButton.setOnClickListener(v -> checkout());
        updateSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (!session.isLoggedIn()) {
            adapter.submit(null);
            stateView.showEmpty("登录后可以查看购物车");
            updateSummary();
            return;
        }
        stateView.showLoading();
        handler.removeCallbacks(loader);
        handler.postDelayed(loader, FAKE_LOADING_DELAY);
    }

    private void loadNow() {
        if (!isAdded()) return;
        try {
            List<CartItem> items = repository.getCartItems(session.getUserId());
            adapter.submit(items);
            if (items.isEmpty()) stateView.showEmpty(getString(R.string.cart_empty));
            else stateView.showContent();
            updateSummary();
        } catch (Exception e) {
            stateView.showError("购物车加载失败：" + e.getMessage(), this::load);
        }
    }

    private void updateSummary() {
        int total = adapter.getSelectedTotalFen();
        totalView.setText(getString(R.string.cart_total) + " " + FormatUtils.formatPrice(total));
        checkoutButton.setEnabled(total > 0);
        bindingSelectAll = true;
        selectAll.setChecked(adapter.isAllSelected());
        bindingSelectAll = false;
    }

    private void checkout() {
        if (!session.isLoggedIn()) {
            startActivity(new Intent(requireContext(), LoginActivity.class)
                    .putExtra("redirect_tab", "cart"));
            return;
        }
        List<CartItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            Ui.toast(requireContext(), R.string.cart_select_hint);
            return;
        }
        long[] ids = new long[selected.size()];
        for (int i = 0; i < selected.size(); i++) ids[i] = selected.get(i).getBookId();
        Intent intent = new Intent(requireContext(), CheckoutActivity.class);
        intent.putExtra(CheckoutActivity.EXTRA_BOOK_IDS, ids);
        startActivity(intent);
    }

    // ------------------------------------------------ CartAdapter.Listener

    @Override
    public void onQuantityChange(CartItem item, int delta) {
        try {
            repository.updateCartQuantity(session.getUserId(), item.getBookId(), item.getQuantity() + delta);
            loadNow();
        } catch (Exception e) {
            Ui.toast(requireContext(), e.getMessage());
        }
    }

    @Override
    public void onDelete(CartItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.cart_delete_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    repository.deleteCartItem(session.getUserId(), item.getBookId());
                    loadNow();
                })
                .show();
    }

    @Override
    public void onSelectionChanged() {
        updateSummary();
    }

    @Override
    public void onBookClick(long bookId) {
        Intent intent = new Intent(requireContext(), BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, bookId);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(loader);
        super.onDestroyView();
    }
}
