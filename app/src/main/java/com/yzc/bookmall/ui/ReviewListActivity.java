package com.yzc.bookmall.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Review;
import com.yzc.bookmall.ui.adapter.ReviewAdapter;
import com.yzc.bookmall.ui.widget.StateView;
import com.yzc.bookmall.util.SessionManager;

import java.util.List;

/** 我的评价。 */
public class ReviewListActivity extends BaseActivity {
    private BookRepository repository;
    private SessionManager session;
    private ReviewAdapter adapter;
    private StateView stateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_list);
        applyInsets(findViewById(android.R.id.content));

        repository = new BookRepository(this);
        session = new SessionManager(this);

        ((TextView) findViewById(R.id.tv_toolbar_title)).setText(R.string.review_my_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.rv_reviews);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter();
        adapter.setShowBookTitle(true);
        recyclerView.setAdapter(adapter);

        stateView = findViewById(R.id.state_view);
        stateView.bind(recyclerView);

        if (!session.isLoggedIn()) {
            stateView.showEmpty("登录后可以查看评价");
            return;
        }
        try {
            List<Review> reviews = repository.getReviewsByUser(session.getUserId());
            adapter.submit(reviews);
            if (reviews.isEmpty()) stateView.showEmpty(getString(R.string.review_empty));
            else stateView.showContent();
        } catch (Exception e) {
            stateView.showError("评价加载失败：" + e.getMessage());
        }
    }
}
