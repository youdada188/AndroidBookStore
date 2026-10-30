package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Book;
import com.yzc.bookmall.model.Review;
import com.yzc.bookmall.ui.adapter.ReviewAdapter;
import com.yzc.bookmall.ui.widget.StateView;
import com.yzc.bookmall.util.FormatUtils;
import com.yzc.bookmall.util.SessionManager;

import java.util.List;

/** 图书详情（对应教材原型的「菜品详情页」）：信息展示、数量选择、收藏、加入购物车与评价。 */
public class BookDetailActivity extends BaseActivity {
    public static final String EXTRA_BOOK_ID = "book_id";
    private static final long FAKE_LOADING_DELAY = 220L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private BookRepository repository;
    private SessionManager session;
    private ReviewAdapter reviewAdapter;
    private StateView stateView;
    private TextView quantityView;
    private TextView favoriteView;
    private long bookId;
    private Book book;
    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);
        applyInsets(findViewById(android.R.id.content));

        bookId = getIntent().getLongExtra(EXTRA_BOOK_ID, -1);
        repository = new BookRepository(this);
        session = new SessionManager(this);

        ((TextView) findViewById(R.id.tv_toolbar_title)).setText(R.string.book_detail_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        RecyclerView reviewList = findViewById(R.id.rv_reviews);
        reviewList.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter();
        reviewList.setAdapter(reviewAdapter);

        stateView = findViewById(R.id.state_view);
        stateView.bind(findViewById(R.id.content));

        quantityView = findViewById(R.id.tv_quantity);
        findViewById(R.id.btn_qty_minus).setOnClickListener(v -> changeQuantity(-1));
        findViewById(R.id.btn_qty_plus).setOnClickListener(v -> changeQuantity(1));

        favoriteView = findViewById(R.id.tv_favorite);
        favoriteView.setOnClickListener(v -> toggleFavorite());
        findViewById(R.id.btn_add_cart).setOnClickListener(v -> addToCart());
        findViewById(R.id.tv_write_review).setOnClickListener(v -> startReview());

        load();
    }

    private void load() {
        stateView.showLoading();
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                book = repository.getBookById(bookId);
                if (book == null) {
                    stateView.showError("这本书已下架");
                    return;
                }
                bindBook(book);
                bindReviews();
                stateView.showContent();
            } catch (Exception e) {
                stateView.showError("图书详情加载失败：" + e.getMessage(), this::load);
            }
        }, FAKE_LOADING_DELAY);
    }

    private void bindBook(Book book) {
        ((ImageView) findViewById(R.id.iv_book_cover)).setImageResource(book.getCoverResId());
        ((TextView) findViewById(R.id.tv_book_title)).setText(book.getTitle());
        ((TextView) findViewById(R.id.tv_book_meta)).setText(
                book.getAuthor() + " 著　|　" + book.getPublisher() + "　|　" + book.getCategoryName());
        ((TextView) findViewById(R.id.tv_book_price)).setText(FormatUtils.formatPrice(book.getPriceFen()));
        ((TextView) findViewById(R.id.tv_book_stock)).setText(
                getString(R.string.stock_format, book.getStock()) + " · " + getString(R.string.sales_format, book.getSales()));
        ((TextView) findViewById(R.id.tv_book_description)).setText(book.getDescription());

        quantity = 1;
        quantityView.setText("1");
        View addButton = findViewById(R.id.btn_add_cart);
        addButton.setEnabled(!book.isSoldOut());
        if (book.isSoldOut()) {
            ((TextView) addButton).setText(R.string.sold_out);
        }
        updateFavorite(repository.isFavorite(session.getUserId(), bookId));
    }

    private void bindReviews() {
        List<Review> reviews = repository.getReviewsByBook(bookId, 3);
        reviewAdapter.submit(reviews);
        int count = repository.getReviewCount(bookId);
        findViewById(R.id.tv_review_empty).setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
        TextView summary = findViewById(R.id.tv_review_summary);
        if (count == 0) {
            summary.setText("");
        } else {
            summary.setText(FormatUtils.rating(repository.getAverageRating(bookId)) + " 分 · "
                    + getString(R.string.book_review_count, count));
        }
    }

    private void changeQuantity(int delta) {
        if (book == null) return;
        int max = Math.max(1, book.getStock());
        quantity = Math.max(1, Math.min(max, quantity + delta));
        quantityView.setText(String.valueOf(quantity));
    }

    private void updateFavorite(boolean favorite) {
        favoriteView.setText(favorite ? R.string.favorited : R.string.favorite);
        favoriteView.setTextColor(getColor(favorite ? R.color.brand_orange : R.color.text_secondary));
    }

    private void toggleFavorite() {
        if (!session.isLoggedIn()) {
            requireLogin();
            return;
        }
        boolean next = !repository.isFavorite(session.getUserId(), bookId);
        repository.setFavorite(session.getUserId(), bookId, next);
        updateFavorite(next);
        Ui.toast(this, next ? "已加入收藏" : "已取消收藏");
    }

    private void addToCart() {
        if (book == null) return;
        if (!session.isLoggedIn()) {
            requireLogin();
            return;
        }
        try {
            repository.addToCart(session.getUserId(), bookId, quantity);
            Ui.toast(this, R.string.added_to_cart);
        } catch (Exception e) {
            Ui.toast(this, e.getMessage());
        }
    }

    private void startReview() {
        if (!session.isLoggedIn()) {
            requireLogin();
            return;
        }
        long userId = session.getUserId();
        if (repository.hasReviewed(userId, bookId)) {
            Ui.toast(this, R.string.review_already);
            return;
        }
        if (!repository.canReview(userId, bookId)) {
            Ui.toast(this, R.string.review_only_buyer);
            return;
        }

        int padding = Ui.dp(this, 22);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, Ui.dp(this, 8), padding, 0);

        final RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);
        ratingBar.setRating(5);
        container.addView(ratingBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final EditText input = new EditText(this);
        input.setHint(R.string.review_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.review_write)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.review_submit, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int rating = (int) ratingBar.getRating();
                    String content = input.getText().toString();
                    if (rating <= 0) {
                        Ui.toast(this, R.string.review_rating_required);
                        return;
                    }
                    if (content.trim().isEmpty()) {
                        Ui.toast(this, R.string.review_content_required);
                        return;
                    }
                    try {
                        repository.addReview(session.getUserId(), bookId, rating, content);
                        Ui.toast(this, R.string.review_saved);
                        dialog.dismiss();
                        bindReviews();
                    } catch (Exception e) {
                        Ui.toast(this, e.getMessage());
                    }
                }));
        dialog.show();
    }

    private void requireLogin() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
