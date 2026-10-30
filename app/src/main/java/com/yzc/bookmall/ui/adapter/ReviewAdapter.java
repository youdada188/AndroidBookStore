package com.yzc.bookmall.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.model.Review;
import com.yzc.bookmall.ui.Ui;
import com.yzc.bookmall.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/** 评价列表。 */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewHolder> {

    private final List<Review> items = new ArrayList<>();
    private boolean showBookTitle = false;

    public void submit(List<Review> reviews) {
        items.clear();
        if (reviews != null) items.addAll(reviews);
        notifyDataSetChanged();
    }

    /** 「我的评价」需要显示书名，「图书评价」不需要。 */
    public void setShowBookTitle(boolean show) {
        this.showBookTitle = show;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewHolder holder, int position) {
        holder.bind(items.get(position), showBookTitle);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReviewHolder extends RecyclerView.ViewHolder {
        private final TextView avatar;
        private final TextView user;
        private final TextView stars;
        private final TextView date;
        private final TextView content;

        ReviewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.tv_review_avatar);
            user = itemView.findViewById(R.id.tv_review_user);
            stars = itemView.findViewById(R.id.tv_review_stars);
            date = itemView.findViewById(R.id.tv_review_date);
            content = itemView.findViewById(R.id.tv_review_content);
        }

        void bind(Review review, boolean showBookTitle) {
            avatar.setText(Ui.initial(review.getUsername()));
            user.setText(showBookTitle ? "《" + review.getBookTitle() + "》" : review.getUsername());
            stars.setText(FormatUtils.stars(review.getRating()));
            date.setText(FormatUtils.formatDateShort(review.getCreatedAt()));
            content.setText(review.getContent());
        }
    }
}
