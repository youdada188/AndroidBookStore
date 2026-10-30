package com.yzc.bookmall.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Order;
import com.yzc.bookmall.model.OrderItem;
import com.yzc.bookmall.ui.adapter.OrderItemAdapter;
import com.yzc.bookmall.ui.widget.StateView;
import com.yzc.bookmall.util.FormatUtils;
import com.yzc.bookmall.util.SessionManager;

import java.util.List;

/** 订单详情：金额明细、商品快照、确认收货。 */
public class OrderDetailActivity extends BaseActivity {
    public static final String EXTRA_ORDER_ID = "order_id";

    private BookRepository repository;
    private SessionManager session;
    private OrderItemAdapter itemAdapter;
    private StateView stateView;
    private long orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);
        applyInsets(findViewById(android.R.id.content));

        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);
        repository = new BookRepository(this);
        session = new SessionManager(this);

        ((TextView) findViewById(R.id.tv_toolbar_title)).setText(R.string.order_detail_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        RecyclerView itemList = findViewById(R.id.rv_detail_items);
        itemList.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new OrderItemAdapter();
        itemList.setAdapter(itemAdapter);

        stateView = findViewById(R.id.state_view);
        stateView.bind(findViewById(R.id.content));

        load();
    }

    private void load() {
        try {
            Order order = repository.getOrder(session.getUserId(), orderId);
            if (order == null) {
                stateView.showError("订单不存在或无权查看");
                return;
            }
            bind(order);
            stateView.showContent();
        } catch (Exception e) {
            stateView.showError("订单加载失败：" + e.getMessage(), this::load);
        }
    }

    private void bind(Order order) {
        ((TextView) findViewById(R.id.tv_detail_status)).setText(order.getStatus());
        ((TextView) findViewById(R.id.tv_detail_status)).setTextColor(getColor(
                order.isPending() ? R.color.brand_orange : R.color.success));
        ((TextView) findViewById(R.id.tv_detail_order_no)).setText(
                getString(R.string.order_no, order.getOrderNo()));
        ((TextView) findViewById(R.id.tv_detail_time)).setText("下单时间 " + FormatUtils.formatDate(order.getCreatedAt()));
        ((TextView) findViewById(R.id.tv_detail_receiver)).setText(
                getString(R.string.order_receiver) + "：" + order.getReceiverName() + "　" + order.getReceiverPhone());
        ((TextView) findViewById(R.id.tv_detail_address)).setText(order.getReceiverAddress());

        List<OrderItem> items = repository.getOrderItems(orderId);
        itemAdapter.submit(items);

        ((TextView) findViewById(R.id.tv_detail_goods)).setText(FormatUtils.formatPrice(order.getGoodsAmountFen()));
        ((TextView) findViewById(R.id.tv_detail_delivery)).setText(
                order.getDeliveryFeeFen() == 0 ? "免配送费" : FormatUtils.formatPrice(order.getDeliveryFeeFen()));
        TextView discountView = findViewById(R.id.tv_detail_discount);
        if (order.getDiscountFen() > 0) {
            String title = order.getCouponTitle() == null ? "" : "（" + order.getCouponTitle() + "）";
            discountView.setText("−" + FormatUtils.formatPrice(order.getDiscountFen()) + title);
        } else {
            discountView.setText("未使用");
            discountView.setTextColor(getColor(R.color.text_hint));
        }
        ((TextView) findViewById(R.id.tv_detail_total)).setText(FormatUtils.formatPrice(order.getTotalAmountFen()));

        View receiptButton = findViewById(R.id.btn_confirm_receipt);
        receiptButton.setVisibility(order.isPending() ? View.VISIBLE : View.GONE);
        receiptButton.setOnClickListener(v -> {
            repository.confirmReceipt(orderId, session.getUserId());
            Ui.toast(this, R.string.order_receipt_done);
            load();
        });
    }
}
