package com.yzc.bookmall.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Book;
import com.yzc.bookmall.model.Category;
import com.yzc.bookmall.ui.BookDetailActivity;
import com.yzc.bookmall.ui.adapter.BookAdapter;
import com.yzc.bookmall.ui.adapter.CategoryAdapter;
import com.yzc.bookmall.ui.widget.StateView;

import java.util.List;

/** 分类浏览：上方分类 chip，下方该分类图书。 */
public class CategoryFragment extends Fragment {
    private static final long FAKE_LOADING_DELAY = 250L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable loader = this::loadBooksNow;

    private BookRepository repository;
    private BookAdapter bookAdapter;
    private CategoryAdapter categoryAdapter;
    private StateView stateView;
    private long selectedCategoryId = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new BookRepository(requireContext());

        RecyclerView categoryList = view.findViewById(R.id.rv_categories);
        categoryList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(categoryId -> {
            selectedCategoryId = categoryId;
            loadBooks();
        });
        categoryList.setAdapter(categoryAdapter);

        RecyclerView bookList = view.findViewById(R.id.rv_books);
        bookList.setLayoutManager(new LinearLayoutManager(requireContext()));
        bookAdapter = new BookAdapter(this::openBook);
        bookList.setAdapter(bookAdapter);

        stateView = view.findViewById(R.id.state_view);
        stateView.bind(bookList);

        loadCategories();
    }

    private void loadCategories() {
        try {
            List<Category> categories = repository.getCategories();
            categoryAdapter.submit(categories);
            loadBooks();
        } catch (Exception e) {
            stateView.showError("分类加载失败：" + e.getMessage(), this::loadCategories);
        }
    }

    private void loadBooks() {
        stateView.showLoading();
        handler.removeCallbacks(loader);
        handler.postDelayed(loader, FAKE_LOADING_DELAY);
    }

    private void loadBooksNow() {
        if (!isAdded()) return;
        try {
            List<Book> books = selectedCategoryId == 0
                    ? repository.getAllBooks()
                    : repository.getBooksByCategory(selectedCategoryId);
            bookAdapter.submit(books);
            if (books.isEmpty()) stateView.showEmpty("这个分类下还没有图书");
            else stateView.showContent();
        } catch (Exception e) {
            stateView.showError("图书加载失败：" + e.getMessage(), this::loadBooks);
        }
    }

    private void openBook(Book book) {
        Intent intent = new Intent(requireContext(), BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(loader);
        super.onDestroyView();
    }
}
