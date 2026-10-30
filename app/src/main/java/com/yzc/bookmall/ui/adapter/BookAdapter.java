package com.yzc.bookmall.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.model.Book;
import com.yzc.bookmall.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/** 图书列表（首页推荐、分类、书店详情、搜索、收藏共用）。 */
public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookHolder> {

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    private final List<Book> items = new ArrayList<>();
    private final OnBookClickListener listener;

    public BookAdapter(OnBookClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Book> books) {
        items.clear();
        if (books != null) items.addAll(books);
        notifyDataSetChanged();
    }

    public List<Book> getItems() {
        return items;
    }

    @NonNull
    @Override
    public BookHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        return new BookHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BookHolder extends RecyclerView.ViewHolder {
        private final ImageView cover;
        private final TextView title;
        private final TextView author;
        private final TextView price;
        private final TextView sales;

        BookHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.iv_book_cover);
            title = itemView.findViewById(R.id.tv_book_title);
            author = itemView.findViewById(R.id.tv_book_author);
            price = itemView.findViewById(R.id.tv_book_price);
            sales = itemView.findViewById(R.id.tv_book_sales);
        }

        void bind(Book book, OnBookClickListener listener) {
            cover.setImageResource(book.getCoverResId());
            title.setText(book.getTitle());
            author.setText(book.getAuthor() + " · " + book.getCategoryName());
            price.setText(FormatUtils.formatPrice(book.getPriceFen()));
            sales.setText(itemView.getContext().getString(R.string.sales_format, book.getSales()));
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBookClick(book);
            });
        }
    }
}
