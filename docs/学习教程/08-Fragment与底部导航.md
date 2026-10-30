# 08 · Fragment 与底部导航

> **本篇你会学到**：Fragment 是什么、为什么要有它、一个 Activity 怎么装下四个页面、`containerFilled` 这个变量是干嘛的。

---

## 8.1 先看问题：底部四个 Tab 怎么实现？

首页底部有"书店 / 分类 / 购物车 / 我的"四个入口，点击切换内容。

**方案 A：四个 Activity**

```
点"分类" → startActivity(CategoryActivity) → 整个屏幕重建
点"购物车" → startActivity(CartActivity) → 又重建
```

问题：
- 每次切换整个页面重建，**有闪烁**
- 四个页面各有一个底部导航栏，**要写四遍**
- 返回栈会变成 `[首页][分类][购物车]`，按返回是"退回上一个 Tab"而不是"退出 App"，很奇怪

**方案 B：一个 Activity + 四个 Fragment**（项目的做法）

```
MainActivity
├── 顶部：FragmentContainerView（容器，占满中间）
└── 底部：BottomNavigationView（导航栏，固定不动）

容器里轮流放：
   HomeFragment / CategoryFragment / CartFragment / ProfileFragment
```

**底部导航栏只写一次，切换时只换中间那块内容。**

---

## 8.2 Fragment 是什么

**Fragment = 片段，可以理解成"半个页面"或"可复用的页面块"。**

| | Activity | Fragment |
|---|---|---|
| 能否独立存在 | 能（必须在 Manifest 登记） | **不能**，必须寄生在 Activity 里 |
| 有自己的布局 | 有 | 有 |
| 有自己的生命周期 | 有 | 有（但依附于宿主 Activity） |
| 能否直接用 `setContentView` | 能 | **不能**，用 `onCreateView` 返回 View |
| 能否用 `findViewById` | 能 | **不能**，要用 `view.findViewById` |
| 能否 `startActivity` | 能 | 能（但要用 `requireContext()`） |

> **命名解读**：`Fragment` 直译"碎片/片段"。Android 3.0 引入它是为了适配平板——左边列表、右边详情，两个 Fragment 拼在一个 Activity 里。

---

## 8.3 Fragment 的生命周期

比 Activity 多几个方法，但核心就三个：

```
onCreate()          创建 Fragment 对象（还没界面）
    ↓
onCreateView()      ★ 创建界面，返回 View（在这里 inflate 布局）
    ↓
onViewCreated()     ★ 界面创建完成，可以 findViewById 了
    ↓
onStart() / onResume()   可见、可交互
    ↓
onPause() / onStop()     不可见
    ↓
onDestroyView()     ★ 销毁界面（View 被回收，但 Fragment 对象还在）
    ↓
onDestroy()        销毁 Fragment 对象
```

**和 Activity 最大的区别：`onCreateView` + `onViewCreated` 两步。**

- `onCreateView` —— **只负责造 View**，返回给系统
- `onViewCreated` —— View 造好了，**在这里绑事件、加载数据**

项目里的标准写法：

```java
@Nullable
@Override
public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                         @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_home, container, false);
}

@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    // 这里才能 findViewById
    RecyclerView recyclerView = view.findViewById(R.id.rv_stores);
    ...
}
```

> **注意 `onViewCreated` 的参数 `view`**：它就是 `onCreateView` 返回的那个 View。Fragment 里所有 `findViewById` 都要通过它，不能像 Activity 那样直接调。

### `onDestroyView` 里为什么要清理

```java
@Override
public void onDestroyView() {
    handler.removeCallbacks(loader);   // 取消定时任务
    super.onDestroyView();
}
```

因为 **Fragment 的 View 会被销毁，但 Fragment 对象还活着**（比如切到别的 Tab，View 没了，对象还在）。

如果定时任务还在跑，它去操作一个已经销毁的 View，就会崩。**所以必须在 `onDestroyView` 里取消。**

---

## 8.4 完整精读：`MainActivity`

```java
public class MainActivity extends BaseActivity {

    private boolean containerFilled;      // ①

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyInsets(findViewById(R.id.root));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        bottomNav.setOnItemSelectedListener(item -> {     // ②
            switchTo(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {                 // ③
            int initial = resolveInitialTab();
            bottomNav.setSelectedItemId(initial);         // ④
            if (!containerFilled) switchTo(initial);      // ⑤
        }
    }

    private int resolveInitialTab() {                     // ⑥
        String tab = getIntent().getStringExtra("redirect_tab");
        if ("cart".equals(tab)) return R.id.nav_cart;
        if ("profile".equals(tab)) return R.id.nav_profile;
        if ("category".equals(tab)) return R.id.nav_category;
        return R.id.nav_home;
    }

    private void switchTo(int itemId) {                   // ⑦
        Fragment fragment;
        if (itemId == R.id.nav_home) fragment = new HomeFragment();
        else if (itemId == R.id.nav_category) fragment = new CategoryFragment();
        else if (itemId == R.id.nav_cart) fragment = new CartFragment();
        else fragment = new ProfileFragment();

        containerFilled = true;

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
```

### 逐条讲解

**① `containerFilled`**
"容器是否已经填过内容了"。

**为什么需要它？** 因为 `setSelectedItemId()` 会触发 `OnItemSelectedListener`，从而调用 `switchTo()`。但万一因为某种原因监听器没触发（比如传的 ID 和当前选中的一样），容器就会是空的——**白屏**。

所以第 ⑤ 行做了兜底：如果 `switchTo` 没被调用过（`containerFilled` 还是 false），就手动调一次。

> **命名解读**：`container` = 容器（指 `fragment_container`），`filled` = 已填充。布尔变量用**过去分词**表示"已经…了"。

**② `setOnItemSelectedListener`**
底部导航的点击监听。`item` 是被点击的菜单项，`item.getItemId()` 返回它的 ID（`R.id.nav_home` 等）。

`return true` 表示"我处理了这个事件"，菜单会更新选中态。**返回 false 的话，图标不会变色。**

**③ `if (savedInstanceState == null)`**
`onCreate` 的参数 `savedInstanceState`：
- **第一次创建**时为 `null`
- **配置变更后重建**（如屏幕旋转）时不为 `null`，里面存着之前的状态

**为什么要判断？** 因为屏幕旋转时，FragmentManager 会**自动恢复**之前的 Fragment。如果你再 `replace` 一次，就会创建重复的 Fragment。

**④ `setSelectedItemId(initial)`**
设置默认选中哪个 Tab。这会**同时**做两件事：更新图标高亮 + 触发监听器（进而调用 `switchTo`）。

**⑤ `if (!containerFilled) switchTo(initial)`**
兜底，见第 ① 点。

**⑥ `resolveInitialTab()`**
**`resolve` = 解析/决定**。这个方法"决定初始应该显示哪个 Tab"，优先级：Intent 传来的 `redirect_tab` > 默认首页。

**⑦ `switchTo()`**
核心方法，见下一节。

---

## 8.5 Fragment 事务：`replace` 是怎么换页的

```java
getSupportFragmentManager()
        .beginTransaction()
        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        .replace(R.id.fragment_container, fragment)
        .commit();
```

拆开看：

| 步骤 | 含义 |
|---|---|
| `getSupportFragmentManager()` | 拿到 Fragment 管理器 |
| `beginTransaction()` | 开启一个事务（"一批操作"） |
| `setCustomAnimations(in, out)` | 设置进场/退场动画（这里是淡入淡出） |
| `replace(容器ID, fragment)` | 把容器里的内容**替换**成这个 Fragment |
| `commit()` | 提交事务（真正执行） |

> **为什么要 `beginTransaction` + `commit`？** 因为 Fragment 操作可能有多步（比如"移除 A + 添加 B + 加入返回栈"）。用事务包起来，要么全成功，要么全失败，不会出现"换到一半"的中间态。

### `replace` vs `add` vs `show/hide`

| 方法 | 效果 | 内存 |
|---|---|---|
| `replace` | 移除旧的，加新的（**旧的被销毁**） | 省内存，但**状态丢失** |
| `add` + `hide/show` | 都保留，只切换可见性 | 费内存，但**状态保留** |

**项目用的是 `replace`**，原因：

- 简单，不容易出 bug
- 每个 Tab 的数据都在 `onResume` 里重新查数据库，不怕状态丢失
- 4 个 Fragment 都很轻，重建成本低

**代价**：切到"分类"再切回"书店"，列表会滚回顶部。对这个小项目可以接受。

> 如果你想优化，可以改成 `add` + `hide/show`，用 `FragmentTransaction` 手动管理。这是第 14 篇的扩展练习之一。

---

## 8.6 完整精读：`HomeFragment`

```java
public class HomeFragment extends Fragment {
    private static final long FAKE_LOADING_DELAY = 300L;      // ①

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable loader = this::loadNow;            // ②

    private BookRepository repository;
    private StoreAdapter adapter;
    private StateView stateView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);   // ③
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new BookRepository(requireContext());     // ④

        RecyclerView recyclerView = view.findViewById(R.id.rv_stores);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new StoreAdapter(store -> {                  // ⑤
            Intent intent = new Intent(requireContext(), StoreDetailActivity.class);
            intent.putExtra(StoreDetailActivity.EXTRA_STORE_ID, store.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        stateView = view.findViewById(R.id.state_view);
        stateView.bind(recyclerView);                          // ⑥

        view.findViewById(R.id.layout_search).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SearchActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        load();                                                // ⑦
    }

    private void load() {
        stateView.showLoading();
        handler.removeCallbacks(loader);                       // ⑧
        handler.postDelayed(loader, FAKE_LOADING_DELAY);       // ⑨
    }

    private void loadNow() {
        if (!isAdded()) return;                                // ⑩
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
        handler.removeCallbacks(loader);                       // ⑪
        super.onDestroyView();
    }
}
```

### 逐条讲解

**① `FAKE_LOADING_DELAY = 300L`**
"假装加载的延迟，300 毫秒"。**`L` 后缀表示 `long` 类型**（`postDelayed` 要求 long）。

**为什么要假装延迟？** 因为数据库查询太快了（几毫秒），加载状态一闪而过，用户根本看不见。加 300ms 是为了**演示"加载中"这个状态**，答辩时能展示。

真实项目里这里应该开子线程查数据库，而不是故意延迟。**代码里加了注释说明这是模拟。**

**② `private final Runnable loader = this::loadNow;`**

这行有点高级，拆开看：

- `Runnable` = 一个"可执行的任务"接口，只有一个 `run()` 方法
- `this::loadNow` = **方法引用**，等价于 `() -> loadNow()`
- 合起来：创建一个任务对象，执行它就是调用 `loadNow()`

**为什么要存成字段，不直接 `postDelayed(this::loadNow, 300)`？**

因为 `removeCallbacks()` 需要**同一个对象**才能取消。如果每次都 new 一个，取消不掉。

> **命名解读**：`loader` = 加载器（执行加载动作的东西）。

**③ `inflater.inflate(R.layout.fragment_home, container, false)`**
把 `fragment_home.xml` 变成一个 View 返回。第三个参数 `false` 同样表示"先别加进去"。

**④ `requireContext()`**
Fragment 没有 `this` 可以当 Context 用，所以用 `requireContext()`：
- `getContext()` 可能返回 `null`（Fragment 已脱离 Activity）
- `requireContext()` 拿不到会**直接抛异常**，让你早点发现错误

**⑤ `new StoreAdapter(store -> {...})`**
把点击回调传进去。注意这里用的是 `requireContext()` 而不是 `this`。

**⑥ `stateView.bind(recyclerView)`**
告诉 StateView："列表是这个，切状态时帮我控制它的可见性。"

**⑦ `onResume` 里调 `load()`**
每次回到这个 Tab 都刷新。原因和 Activity 一样：从详情页返回后数据可能变了。

**⑧ `handler.removeCallbacks(loader)`**
取消上一次还没执行的加载。**为什么？** 如果用户快速来回切 Tab，可能同时挂了好几个任务，都执行会重复刷新。

**⑨ `handler.postDelayed(loader, 300)`**
300ms 后执行。

**⑩ `if (!isAdded()) return;`**
**`isAdded()` = "我还挂在 Activity 上吗"**。

300ms 后回调执行时，用户可能已经切走了，Fragment 已经从 Activity 上摘下来。这时候再操作 View 会崩。这行是**保险**。

**⑪ `onDestroyView` 里再取消一次**
双保险，防止 View 销毁后任务还在跑。

---

## 8.7 底部导航栏怎么配置

### 菜单文件：`res/menu/menu_bottom_nav.xml`

```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/nav_home"
        android:icon="@drawable/ic_nav_home"
        android:title="@string/nav_home" />
    <item
        android:id="@+id/nav_category"
        android:icon="@drawable/ic_nav_category"
        android:title="@string/nav_category" />
    <item
        android:id="@+id/nav_cart"
        android:icon="@drawable/ic_nav_cart"
        android:title="@string/nav_cart" />
    <item
        android:id="@+id/nav_profile"
        android:icon="@drawable/ic_nav_profile"
        android:title="@string/nav_profile" />
</menu>
```

> **命名解读**：`nav_` 前缀表示"导航项"。图标 `ic_nav_home` = 图标 + 导航 + 首页。

### 布局里的 BottomNavigationView

```xml
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:id="@+id/bottom_nav"
    app:itemIconTint="@color/bottom_nav_tint"
    app:itemTextColor="@color/bottom_nav_tint"
    app:labelVisibilityMode="labeled"
    app:menu="@menu/menu_bottom_nav" />
```

- `app:menu` —— 指定菜单文件
- `app:itemIconTint` / `itemTextColor` —— 图标和文字的颜色，指向一个**颜色选择器**
- `app:labelVisibilityMode="labeled"` —— 始终显示文字（不选"auto"是因为 auto 在某些情况下会隐藏未选中项的文字）

### 颜色选择器：`res/color/bottom_nav_tint.xml`

```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/brand_blue" android:state_checked="true" />
    <item android:color="@color/text_hint" />
</selector>
```

**读法**：选中时用品牌蓝，其他情况用灰色。**顺序很重要**——第一个匹配上的规则生效，所以 `state_checked="true"` 必须写在前面。

---

## 8.8 Fragment 里怎么跳转和弹 Toast

| 操作 | Activity 里 | Fragment 里 |
|---|---|---|
| 启动页面 | `startActivity(intent)` | `startActivity(intent)`（一样） |
| 拿 Context | `this` | `requireContext()` 或 `requireActivity()` |
| 找控件 | `findViewById(...)` | `view.findViewById(...)` |
| 弹 Toast | `Toast.makeText(this, ...)` | `Toast.makeText(requireContext(), ...)` |
| 取字符串 | `getString(...)` | `getString(...)`（一样） |

项目里的 `Ui.toast()` 就是为简化这个：

```java
public static void toast(Context context, String message) {
    if (context == null) return;
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
}
```

用法：`Ui.toast(requireContext(), "已加入购物车");`

---

## 8.9 本篇小结

| 概念 | 要点 |
|---|---|
| Fragment | 寄生在 Activity 里的"半个页面" |
| `onCreateView` | 造 View 返回 |
| `onViewCreated` | View 造好了，在这里 findViewById |
| `onDestroyView` | View 销毁，**取消定时任务** |
| `requireContext()` | Fragment 里拿 Context 的标准方式 |
| `isAdded()` | 判断 Fragment 是否还挂着 |
| FragmentManager | `beginTransaction().replace().commit()` |
| `replace` vs `add` | 销毁重建 vs 保留状态 |
| `containerFilled` | 兜底标志，防止容器空着 |

### 自测

1. Fragment 里为什么不能直接用 `findViewById`？
2. `onDestroyView` 和 `onDestroy` 有什么区别？定时任务该在哪个里面取消？
3. `replace` 和 `add` + `hide/show` 各有什么代价？
4. `containerFilled` 这个变量是为了防什么情况？
5. `this::loadNow` 和 `() -> loadNow()` 是同一个意思吗？

---

下一篇：[09 SQLite 数据库](09-SQLite数据库.md) —— 数据到底存在哪、12 张表怎么建、什么是事务。
