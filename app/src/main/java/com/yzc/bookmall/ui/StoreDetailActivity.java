package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Book;
import com.yzc.bookmall.model.Store;
import com.yzc.bookmall.ui.adapter.BookAdapter;
import com.yzc.bookmall.ui.adapter.CategoryAdapter;
import com.yzc.bookmall.ui.widget.StateView;
import com.yzc.bookmall.util.FormatUtils;

import java.util.List;

/** 书店详情（对应教材原型的「店铺详情页」）：店铺信息 + 分类筛选 + 店内图书。 */
public class StoreDetailActivity extends BaseActivity {
    public static final String EXTRA_STORE_ID = "store_id";
    private static final long FAKE_LOADING_DELAY = 250L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private BookRepository repository;
    private BookAdapter bookAdapter;
    private CategoryAdapter categoryAdapter;
    private StateView stateView;
    private long storeId;
    private long categoryId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_detail);
        applyInsets(findViewById(android.R.id.content));

        storeId = getIntent().getLongExtra(EXTRA_STORE_ID, -1);
        repository = new BookRepository(this);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        RecyclerView categoryList = findViewById(R.id.rv_store_categories);
        categoryList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(id -> {
            categoryId = id;
            loadBooks();
        });
        categoryList.setAdapter(categoryAdapter);

        RecyclerView bookList = findViewById(R.id.rv_store_books);
        bookList.setLayoutManager(new LinearLayoutManager(this));
        bookAdapter = new BookAdapter(this::openBook);
        bookList.setAdapter(bookAdapter);

        stateView = findViewById(R.id.state_view);
        stateView.bind(findViewById(R.id.content));

        loadStore();
    }

    private void loadStore() {
        stateView.showLoading();
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                Store store = repository.getStoreById(storeId);
                if (store == null) {
                    stateView.showError("书店不存在或已关闭");
                    return;
                }
                bindStore(store);
                categoryAdapter.submit(repository.getCategoriesByStore(storeId));
                loadBooksNow();
                stateView.showContent();
            } catch (Exception e) {
                stateView.showError("书店信息加载失败：" + e.getMessage(), this::loadStore);
            }
        }, FAKE_LOADING_DELAY);
    }

    private void bindStore(Store store) {
        ((ImageView) findViewById(R.id.iv_store_header)).setImageResource(store.getCoverResId());
        ((TextView) findViewById(R.id.tv_store_name)).setText(store.getName());
        ((TextView) findViewById(R.id.tv_store_slogan)).setText(store.getSlogan());
        ((TextView) findViewById(R.id.tv_store_rating)).setText(
                getString(R.string.store_rating, FormatUtils.rating(store.getRating())));
        ((TextView) findViewById(R.id.tv_store_hours)).setText(
                getString(R.string.store_hours, store.getBusinessHours()));
        ((TextView) findViewById(R.id.tv_store_address)).setText(store.getAddress());
        ((TextView) findViewById(R.id.tv_store_notice)).setText(
                getString(R.string.store_notice_title) + "：" + store.getNotice());
        ((TextView) findViewById(R.id.tv_store_description)).setText(store.getDescription());
    }

    private void loadBooks() {
        try {
            loadBooksNow();
        } catch (Exception e) {
            Ui.toast(this, "图书加载失败：" + e.getMessage());
        }
    }

    private void loadBooksNow() {
        List<Book> books = categoryId == 0
                ? repository.getBooksByStore(storeId)
                : repository.getBooksByStoreAndCategory(storeId, categoryId);
        bookAdapter.submit(books);
        if (books.isEmpty()) Ui.toast(this, "该分类下暂无图书");
    }

    private void openBook(Book book) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
