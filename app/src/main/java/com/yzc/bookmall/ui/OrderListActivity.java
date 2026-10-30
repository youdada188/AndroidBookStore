package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Order;
import com.yzc.bookmall.ui.adapter.OrderAdapter;
import com.yzc.bookmall.ui.widget.StateView;
import com.yzc.bookmall.util.SessionManager;

import java.util.List;

/** 订单列表，按时间倒序。 */
public class OrderListActivity extends BaseActivity {
    private BookRepository repository;
    private SessionManager session;
    private OrderAdapter adapter;
    private StateView stateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
        applyInsets(findViewById(android.R.id.content));

        repository = new BookRepository(this);
        session = new SessionManager(this);

        ((TextView) findViewById(R.id.tv_toolbar_title)).setText(R.string.order_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.rv_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        stateView = findViewById(R.id.state_view);
        stateView.bind(recyclerView);

        if (!session.isLoggedIn()) {
            stateView.showEmpty("登录后可以查看订单");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!session.isLoggedIn()) return;
        try {
            List<Order> orders = repository.getOrders(session.getUserId());
            adapter.submit(orders);
            if (orders.isEmpty()) stateView.showEmpty(getString(R.string.order_empty));
            else stateView.showContent();
        } catch (Exception e) {
            stateView.showError("订单加载失败：" + e.getMessage(), this::reload);
        }
    }

    private void reload() {
        onResume();
    }
}
