package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Book;
import com.yzc.bookmall.ui.adapter.BookAdapter;
import com.yzc.bookmall.ui.widget.StateView;
import com.yzc.bookmall.util.SessionManager;

import java.util.List;

/** 我的收藏。 */
public class FavoriteActivity extends BaseActivity {
    private BookRepository repository;
    private SessionManager session;
    private BookAdapter adapter;
    private StateView stateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        applyInsets(findViewById(android.R.id.content));

        repository = new BookRepository(this);
        session = new SessionManager(this);

        ((TextView) findViewById(R.id.tv_toolbar_title)).setText(R.string.favorite_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.rv_favorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookAdapter(this::openBook);
        recyclerView.setAdapter(adapter);

        stateView = findViewById(R.id.state_view);
        stateView.bind(recyclerView);

        if (!session.isLoggedIn()) {
            stateView.showEmpty("登录后可以查看收藏");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!session.isLoggedIn()) return;
        try {
            List<Book> books = repository.getFavoriteBooks(session.getUserId());
            adapter.submit(books);
            if (books.isEmpty()) stateView.showEmpty(getString(R.string.favorite_empty));
            else stateView.showContent();
        } catch (Exception e) {
            stateView.showError("收藏加载失败：" + e.getMessage(), this::reload);
        }
    }

    private void reload() {
        onResume();
    }

    private void openBook(Book book) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
        startActivity(intent);
    }
}
