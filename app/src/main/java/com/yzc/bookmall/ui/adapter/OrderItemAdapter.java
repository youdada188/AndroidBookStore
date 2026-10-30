package com.yzc.bookmall.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.model.OrderItem;
import com.yzc.bookmall.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/** 订单明细（结算页与订单详情共用）。 */
public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ItemHolder> {

    private final List<OrderItem> items = new ArrayList<>();

    public void submit(List<OrderItem> orderItems) {
        items.clear();
        if (orderItems != null) items.addAll(orderItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_item, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        private final ImageView cover;
        private final TextView title;
        private final TextView price;
        private final TextView subtotal;

        ItemHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.iv_item_cover);
            title = itemView.findViewById(R.id.tv_item_title);
            price = itemView.findViewById(R.id.tv_item_price);
            subtotal = itemView.findViewById(R.id.tv_item_subtotal);
        }

        void bind(OrderItem item) {
            cover.setImageResource(item.getCoverResId());
            title.setText(item.getTitleSnapshot());
            price.setText(FormatUtils.formatPrice(item.getPriceSnapshotFen()) + " × " + item.getQuantity());
            subtotal.setText(FormatUtils.formatPrice(item.getSubtotalFen()));
        }
    }
}
