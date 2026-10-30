package com.yzc.bookmall.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.model.Store;
import com.yzc.bookmall.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/** 首页书店列表。 */
public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreHolder> {

    public interface OnStoreClickListener {
        void onStoreClick(Store store);
    }

    private final List<Store> items = new ArrayList<>();
    private final OnStoreClickListener listener;

    public StoreAdapter(OnStoreClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Store> stores) {
        items.clear();
        if (stores != null) items.addAll(stores);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StoreHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store, parent, false);
        return new StoreHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class StoreHolder extends RecyclerView.ViewHolder {
        private final ImageView cover;
        private final TextView name;
        private final TextView rating;
        private final TextView slogan;
        private final TextView minOrder;
        private final TextView delivery;
        private final TextView count;

        StoreHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.iv_store_cover);
            name = itemView.findViewById(R.id.tv_store_name);
            rating = itemView.findViewById(R.id.tv_store_rating);
            slogan = itemView.findViewById(R.id.tv_store_slogan);
            minOrder = itemView.findViewById(R.id.tv_store_min_order);
            delivery = itemView.findViewById(R.id.tv_store_delivery);
            count = itemView.findViewById(R.id.tv_store_count);
        }

        void bind(Store store, OnStoreClickListener listener) {
            cover.setImageResource(store.getCoverResId());
            name.setText(store.getName());
            rating.setText(itemView.getContext().getString(R.string.store_rating, FormatUtils.rating(store.getRating())));
            slogan.setText(store.getSlogan());
            minOrder.setText(itemView.getContext().getString(R.string.store_min_order, FormatUtils.yuan(store.getMinOrderFen())));
            delivery.setText(store.getDeliveryFeeFen() == 0
                    ? "免配送费"
                    : itemView.getContext().getString(R.string.store_delivery_fee, FormatUtils.yuan(store.getDeliveryFeeFen())));
            count.setText(itemView.getContext().getString(R.string.store_book_count, store.getBookCount()));
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onStoreClick(store);
            });
        }
    }
}
