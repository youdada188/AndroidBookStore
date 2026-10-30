package com.yzc.bookmall.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.model.Order;
import com.yzc.bookmall.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/** 订单列表。 */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private final List<Order> items = new ArrayList<>();
    private final OnOrderClickListener listener;

    public OrderAdapter(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Order> orders) {
        items.clear();
        if (orders != null) items.addAll(orders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OrderHolder extends RecyclerView.ViewHolder {
        private final TextView orderNo;
        private final TextView status;
        private final TextView time;
        private final TextView summary;
        private final TextView count;
        private final TextView total;

        OrderHolder(@NonNull View itemView) {
            super(itemView);
            orderNo = itemView.findViewById(R.id.tv_order_no);
            status = itemView.findViewById(R.id.tv_order_status);
            time = itemView.findViewById(R.id.tv_order_time);
            summary = itemView.findViewById(R.id.tv_order_summary);
            count = itemView.findViewById(R.id.tv_order_count);
            total = itemView.findViewById(R.id.tv_order_total);
        }

        void bind(Order order, OnOrderClickListener listener) {
            orderNo.setText(itemView.getContext().getString(R.string.order_no, order.getOrderNo()));
            status.setText(order.getStatus());
            status.setTextColor(itemView.getContext().getColor(
                    order.isPending() ? R.color.brand_orange : R.color.success));
            time.setText(FormatUtils.formatDate(order.getCreatedAt()));
            summary.setText(itemView.getContext().getString(R.string.order_item_count, order.getItemCount()));
            count.setText("下单于 " + FormatUtils.formatDateShort(order.getCreatedAt()));
            total.setText(FormatUtils.formatPrice(order.getTotalAmountFen()));
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
        }
    }
}
