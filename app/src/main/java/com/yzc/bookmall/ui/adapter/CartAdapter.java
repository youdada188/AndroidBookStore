package com.yzc.bookmall.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.model.CartItem;
import com.yzc.bookmall.util.FormatUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 购物车列表，持有勾选状态与合计计算。 */
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartHolder> {

    public interface Listener {
        void onQuantityChange(CartItem item, int delta);
        void onDelete(CartItem item);
        void onSelectionChanged();
        void onBookClick(long bookId);
    }

    private final List<CartItem> items = new ArrayList<>();
    private final Set<Long> selectedBookIds = new LinkedHashSet<>();
    private final Listener listener;

    public CartAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<CartItem> cartItems) {
        items.clear();
        if (cartItems != null) items.addAll(cartItems);
        // 清掉已不存在的勾选项
        Iterator<Long> iterator = selectedBookIds.iterator();
        while (iterator.hasNext()) {
            long id = iterator.next();
            boolean exists = false;
            for (CartItem item : items) {
                if (item.getBookId() == id) { exists = true; break; }
            }
            if (!exists) iterator.remove();
        }
        notifyDataSetChanged();
    }

    public List<CartItem> getSelectedItems() {
        List<CartItem> result = new ArrayList<>();
        for (CartItem item : items) {
            if (selectedBookIds.contains(item.getBookId())) result.add(item);
        }
        return result;
    }

    public int getSelectedTotalFen() {
        int total = 0;
        for (CartItem item : getSelectedItems()) total += item.getSubtotalFen();
        return total;
    }

    public boolean isAllSelected() {
        return !items.isEmpty() && selectedBookIds.size() == items.size();
    }

    public void setAllSelected(boolean checked) {
        selectedBookIds.clear();
        if (checked) {
            for (CartItem item : items) selectedBookIds.add(item.getBookId());
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged();
    }

    @NonNull
    @Override
    public CartHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartHolder holder, int position) {
        holder.bind(items.get(position), selectedBookIds.contains(items.get(position).getBookId()), listener, this);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBox;
        private final ImageView cover;
        private final TextView title;
        private final TextView price;
        private final TextView quantity;
        private final TextView minus;
        private final TextView plus;
        private final TextView delete;

        CartHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cb_select);
            cover = itemView.findViewById(R.id.iv_cart_cover);
            title = itemView.findViewById(R.id.tv_cart_title);
            price = itemView.findViewById(R.id.tv_cart_price);
            quantity = itemView.findViewById(R.id.tv_cart_quantity);
            minus = itemView.findViewById(R.id.btn_minus);
            plus = itemView.findViewById(R.id.btn_plus);
            delete = itemView.findViewById(R.id.tv_cart_delete);
        }

        void bind(CartItem item, boolean checked, Listener listener, CartAdapter adapter) {
            cover.setImageResource(item.getCoverResId());
            title.setText(item.getTitle());
            price.setText(FormatUtils.formatPrice(item.getPriceFen()));
            quantity.setText(String.valueOf(item.getQuantity()));

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(checked);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) adapter.selectedBookIds.add(item.getBookId());
                else adapter.selectedBookIds.remove(item.getBookId());
                if (listener != null) listener.onSelectionChanged();
            });

            minus.setOnClickListener(v -> {
                if (listener != null) listener.onQuantityChange(item, -1);
            });
            plus.setOnClickListener(v -> {
                if (listener != null) listener.onQuantityChange(item, 1);
            });
            delete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(item);
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBookClick(item.getBookId());
            });
        }
    }
}
