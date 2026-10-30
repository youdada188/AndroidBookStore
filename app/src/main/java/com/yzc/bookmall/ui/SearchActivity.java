package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Book;
import com.yzc.bookmall.ui.adapter.BookAdapter;
import com.yzc.bookmall.ui.widget.StateView;

import java.util.List;

/** 搜索页：按书名、作者、分类或书店名模糊查询。 */
public class SearchActivity extends BaseActivity {
    private BookRepository repository;
    private BookAdapter adapter;
    private StateView stateView;
    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        applyInsets(findViewById(android.R.id.content));

        repository = new BookRepository(this);
        input = findViewById(R.id.et_search);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.tv_search).setOnClickListener(v -> search());

        RecyclerView recyclerView = findViewById(R.id.rv_search);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookAdapter(this::openBook);
        recyclerView.setAdapter(adapter);

        stateView = findViewById(R.id.state_view);
        stateView.bind(recyclerView);

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });

        String initial = getIntent().getStringExtra("keyword");
        if (initial != null && !initial.trim().isEmpty()) {
            input.setText(initial);
            input.setSelection(initial.length());
            search();
        } else {
            stateView.showEmpty("输入书名、作者或书店名开始搜索");
        }
    }

    private void search() {
        String keyword = input.getText().toString().trim();
        if (keyword.isEmpty()) {
            adapter.submit(null);
            stateView.showEmpty("输入书名、作者或书店名开始搜索");
            return;
        }
        stateView.showLoading();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                List<Book> books = repository.searchBooks(keyword);
                adapter.submit(books);
                if (books.isEmpty()) stateView.showEmpty(getString(R.string.search_empty));
                else stateView.showContent();
            } catch (Exception e) {
                stateView.showError("搜索失败：" + e.getMessage(), this::search);
            }
        }, 200);
    }

    private void openBook(Book book) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
        startActivity(intent);
    }
}
