package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Address;
import com.yzc.bookmall.model.Book;
import com.yzc.bookmall.model.CartItem;
import com.yzc.bookmall.model.Coupon;
import com.yzc.bookmall.model.OrderItem;
import com.yzc.bookmall.model.Store;
import com.yzc.bookmall.ui.adapter.OrderItemAdapter;
import com.yzc.bookmall.util.FormatUtils;
import com.yzc.bookmall.util.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 确认订单：收货地址、优惠券、金额明细与事务提交。 */
public class CheckoutActivity extends BaseActivity {
    public static final String EXTRA_BOOK_IDS = "selected_book_ids";
    private static final int REQUEST_ADDRESS = 1001;

    private BookRepository repository;
    private SessionManager session;
    private final List<CartItem> items = new ArrayList<>();

    private Address address;
    private Coupon coupon;
    private int goodsFen;
    private int deliveryFen;
    private int discountFen;

    private TextView addressNameView;
    private TextView addressDetailView;
    private TextView discountView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        applyInsets(findViewById(android.R.id.content));

        repository = new BookRepository(this);
        session = new SessionManager(this);

        ((TextView) findViewById(R.id.tv_toolbar_title)).setText(R.string.checkout_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class).putExtra("redirect_tab", "cart"));
            finish();
            return;
        }

        items.addAll(loadSelectedItems());
        if (items.isEmpty()) {
            Ui.toast(this, "没有可结算的图书");
            finish();
            return;
        }

        goodsFen = 0;
        for (CartItem item : items) goodsFen += item.getSubtotalFen();
        deliveryFen = computeDeliveryFee();
        address = repository.getDefaultAddress(session.getUserId());
        coupon = repository.findBestCoupon(session.getUserId(), goodsFen);

        addressNameView = findViewById(R.id.tv_address_name);
        addressDetailView = findViewById(R.id.tv_address_detail);
        discountView = findViewById(R.id.tv_discount);

        RecyclerView itemList = findViewById(R.id.rv_checkout_items);
        itemList.setLayoutManager(new LinearLayoutManager(this));
        OrderItemAdapter adapter = new OrderItemAdapter();
        itemList.setAdapter(adapter);
        adapter.submit(toOrderItems());

        findViewById(R.id.layout_address).setOnClickListener(v -> openAddressPicker());
        findViewById(R.id.layout_coupon).setOnClickListener(v -> chooseCoupon());
        findViewById(R.id.btn_submit).setOnClickListener(v -> submit());

        bindAddress();
        bindAmounts();
    }

    private List<CartItem> loadSelectedItems() {
        long[] ids = getIntent().getLongArrayExtra(EXTRA_BOOK_IDS);
        List<CartItem> all = repository.getCartItems(session.getUserId());
        if (ids == null || ids.length == 0) return all;
        List<CartItem> selected = new ArrayList<>();
        for (CartItem item : all) {
            for (long id : ids) {
                if (item.getBookId() == id) {
                    selected.add(item);
                    break;
                }
            }
        }
        return selected;
    }

    private List<OrderItem> toOrderItems() {
        List<OrderItem> result = new ArrayList<>();
        for (CartItem item : items) {
            result.add(new OrderItem(0, 0, item.getBookId(), item.getTitle(),
                    item.getPriceFen(), item.getQuantity(), item.getCoverResId()));
        }
        return result;
    }

    /** 购物车里可能来自多家书店，配送费按不同书店累加。 */
    private int computeDeliveryFee() {
        Set<Long> storeIds = new HashSet<>();
        int total = 0;
        for (CartItem item : items) {
            Book book = repository.getBookById(item.getBookId());
            if (book == null) continue;
            if (!storeIds.add(book.getStoreId())) continue;
            Store store = repository.getStoreById(book.getStoreId());
            if (store != null) total += store.getDeliveryFeeFen();
        }
        return total;
    }

    private void bindAddress() {
        if (address == null) {
            addressNameView.setText(R.string.checkout_no_address);
            addressDetailView.setText("点击添加收货地址");
        } else {
            addressNameView.setText(address.getName() + "　" + address.getPhone());
            addressDetailView.setText(address.getFullAddress());
        }
    }

    private void bindAmounts() {
        discountFen = (coupon != null && coupon.isUsable(goodsFen)) ? coupon.getDiscountFen() : 0;
        int payFen = Math.max(0, goodsFen + deliveryFen - discountFen);

        ((TextView) findViewById(R.id.tv_goods_total)).setText(FormatUtils.formatPrice(goodsFen));
        ((TextView) findViewById(R.id.tv_delivery_fee)).setText(
                deliveryFen == 0 ? "免配送费" : FormatUtils.formatPrice(deliveryFen));
        if (discountFen > 0) {
            discountView.setText("−" + FormatUtils.formatPrice(discountFen) + "（" + coupon.getTitle() + "）");
            discountView.setTextColor(getColor(R.color.brand_orange));
        } else {
            discountView.setText(R.string.checkout_no_coupon);
            discountView.setTextColor(getColor(R.color.text_hint));
        }
        ((TextView) findViewById(R.id.tv_pay_total)).setText(FormatUtils.formatPrice(payFen));
        ((TextView) findViewById(R.id.tv_bottom_total)).setText("实付 " + FormatUtils.formatPrice(payFen));
    }

    private void openAddressPicker() {
        Intent intent = new Intent(this, AddressListActivity.class);
        intent.putExtra(AddressListActivity.EXTRA_PICK, true);
        startActivityForResult(intent, REQUEST_ADDRESS);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADDRESS && resultCode == RESULT_OK && data != null) {
            long id = data.getLongExtra(AddressListActivity.EXTRA_PICKED_ID, -1);
            for (Address item : repository.getAddresses(session.getUserId())) {
                if (item.getId() == id) {
                    address = item;
                    bindAddress();
                    break;
                }
            }
        }
    }

    private void chooseCoupon() {
        final List<Coupon> usable = new ArrayList<>();
        for (Coupon item : repository.getCoupons(session.getUserId())) {
            if (item.isUsable(goodsFen)) usable.add(item);
        }
        final List<String> labels = new ArrayList<>();
        labels.add("不使用优惠券");
        for (Coupon item : usable) {
            labels.add(item.getTitle() + "　（满 " + FormatUtils.yuan(item.getThresholdFen())
                    + " 减 " + FormatUtils.yuan(item.getDiscountFen()) + "）");
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.checkout_select_coupon)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    coupon = which == 0 ? null : usable.get(which - 1);
                    bindAmounts();
                })
                .show();
    }

    private void submit() {
        if (address == null) {
            Ui.toast(this, "请先添加收货地址");
            openAddressPicker();
            return;
        }
        int payFen = Math.max(0, goodsFen + deliveryFen - discountFen);
        new AlertDialog.Builder(this)
                .setTitle(R.string.checkout_confirm_title)
                .setMessage(getString(R.string.checkout_confirm_message, FormatUtils.yuan(payFen)))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> doSubmit())
                .show();
    }

    private void doSubmit() {
        try {
            long orderId = repository.createOrder(session.getUserId(), items, address.toReceiverInfo(),
                    deliveryFen, discountFen, coupon == null ? null : coupon.getTitle(),
                    coupon == null ? 0 : coupon.getId());
            Ui.toast(this, R.string.checkout_success);
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderId);
            startActivity(intent);
            finish();
        } catch (IllegalStateException e) {
            new AlertDialog.Builder(this)
                    .setTitle("提交失败")
                    .setMessage(e.getMessage())
                    .setPositiveButton(R.string.confirm, (dialog, which) -> finish())
                    .show();
        } catch (Exception e) {
            Ui.toast(this, "下单失败：" + e.getMessage());
        }
    }
}
