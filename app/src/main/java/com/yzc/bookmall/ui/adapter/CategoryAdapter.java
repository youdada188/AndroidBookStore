package com.yzc.bookmall.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.model.Category;

import java.util.ArrayList;
import java.util.List;

/** 分类筛选 chip。首项固定为「全部」。 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ChipHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(long categoryId);
    }

    private final List<Category> items = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private long selectedId = 0;

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Category> categories) {
        items.clear();
        items.add(new Category(0, "全部"));
        if (categories != null) items.addAll(categories);
        notifyDataSetChanged();
    }

    public void setSelected(long categoryId) {
        if (selectedId == categoryId) return;
        selectedId = categoryId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChipHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false);
        return new ChipHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipHolder holder, int position) {
        Category category = items.get(position);
        holder.text.setText(category.getName());
        holder.text.setSelected(category.getId() == selectedId);
        holder.text.setOnClickListener(v -> {
            if (selectedId == category.getId()) return;
            selectedId = category.getId();
            notifyDataSetChanged();
            if (listener != null) listener.onCategoryClick(category.getId());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ChipHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        ChipHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tv_chip);
        }
    }
}
