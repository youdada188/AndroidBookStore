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
import com.yzc.bookmall.model.Store;
import com.yzc.bookmall.ui.SearchActivity;
import com.yzc.bookmall.ui.StoreDetailActivity;
import com.yzc.bookmall.ui.adapter.StoreAdapter;
import com.yzc.bookmall.ui.widget.StateView;

import java.util.List;

/** 首页：书店列表（对应教材原型的「店铺页面」）。 */
public class HomeFragment extends Fragment {
    private static final long FAKE_LOADING_DELAY = 300L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable loader = this::loadNow;

    private BookRepository repository;
    private StoreAdapter adapter;
    private StateView stateView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new BookRepository(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.rv_stores);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new StoreAdapter(store -> {
            Intent intent = new Intent(requireContext(), StoreDetailActivity.class);
            intent.putExtra(StoreDetailActivity.EXTRA_STORE_ID, store.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        stateView = view.findViewById(R.id.state_view);
        stateView.bind(recyclerView);

        view.findViewById(R.id.layout_search).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SearchActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        stateView.showLoading();
        handler.removeCallbacks(loader);
        // 模拟一次数据加载，让加载状态可见（真实场景应放在子线程）
        handler.postDelayed(loader, FAKE_LOADING_DELAY);
    }

    private void loadNow() {
        if (!isAdded()) return;
        try {
            List<Store> stores = repository.getStores();
            adapter.submit(stores);
            if (stores.isEmpty()) stateView.showEmpty("暂时还没有书店");
            else stateView.showContent();
        } catch (Exception e) {
            stateView.showError("书店加载失败：" + e.getMessage(), this::load);
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(loader);
        super.onDestroyView();
    }
}
