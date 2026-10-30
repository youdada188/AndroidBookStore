# 07 · RecyclerView 列表

> **本篇你会学到**：列表为什么能显示 100 条却只创建几个 View、Adapter 的三件套怎么配合、点击事件怎么传出去。

---

## 7.1 先想一个问题：100 本书怎么显示？

假设你要显示 100 本书，每本书是一个"封面 + 书名 + 作者 + 价格"的卡片。

**笨办法**：创建 100 个卡片，全部塞进屏幕。

问题：
- 内存爆炸（100 个 ImageView 各自加载一张图）
- 打开慢（要测量、布局、绘制 100 次）
- 大部分根本看不见，纯浪费

**RecyclerView 的办法**：**只创建屏幕上能看到的那些**（大概 8 个），滑出去的那个被**回收**，重新装上新的数据再滑进来。

这就是 **Recycle**（回收）这个词的由来。

```
屏幕高度能放 8 个：
┌──────────┐
│  卡片 1   │  ← 复用池里拿一个空壳，绑上第 1 本书的数据
│  卡片 2   │
│  卡片 3   │
│   ...    │
│  卡片 8   │
└──────────┘

用户往上滑，卡片 1 滑出屏幕：
  卡片 1 → 回收进池子 → 重新绑定第 9 本书 → 从底部滑入
```

**100 本书，始终只有 8~10 个 View 对象在转。** 这就是 RecyclerView 的核心。

---

## 7.2 三件套：Adapter、ViewHolder、LayoutManager

RecyclerView 需要三个角色配合：

| 角色 | 类比 | 职责 |
|---|---|---|
| **LayoutManager** | 场地管理员 | 决定怎么排：竖着排、横着排、几列 |
| **Adapter** | 装配工 | 决定"第 N 个位置显示什么数据" |
| **ViewHolder** | 零件盒 | 缓存一行里的控件引用，避免反复 findViewById |

三者的关系：

```
RecyclerView
    ├── LayoutManager（怎么排）
    └── Adapter（显示什么）
            └── ViewHolder（这一行的控件都在哪）
```

### 在代码里怎么组装

```java
// ① 找到 RecyclerView
RecyclerView recyclerView = view.findViewById(R.id.rv_stores);

// ② 设置排布方式：竖着排
recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

// ③ 创建 Adapter，并设置点击回调
adapter = new StoreAdapter(store -> {
    Intent intent = new Intent(requireContext(), StoreDetailActivity.class);
    intent.putExtra(StoreDetailActivity.EXTRA_STORE_ID, store.getId());
    startActivity(intent);
});

// ④ 把 Adapter 交给 RecyclerView
recyclerView.setAdapter(adapter);
```

这四步是所有列表的固定套路，项目里 8 个列表全是这么写的。

> **命名解读**：`rv_stores` = **R**ecycler**V**iew + stores（书店们）。`rv_` 前缀表示这是个 RecyclerView。

---

## 7.3 Adapter 的三个必答问题

继承 `RecyclerView.Adapter` 后，必须回答三个问题：

| 方法 | 问题 | 返回什么 |
|---|---|---|
| `getItemCount()` | 一共有多少条？ | 数据列表的长度 |
| `onCreateViewHolder()` | 第 N 行长什么样？（**造空壳**） | 一个新的 ViewHolder |
| `onBindViewHolder()` | 第 N 行要显示什么内容？（**填数据**） | 无（直接改控件） |

**关键区别**：

- `onCreateViewHolder` 只在**需要新壳子**时调用（一个屏幕大概 8 次）
- `onBindViewHolder` 在**每次滑入**时都调用（滑 100 次就调 100 次）

所以：**`onCreateViewHolder` 里做重活（inflate 布局），`onBindViewHolder` 里只填数据。**

---

## 7.4 完整精读：`BookAdapter`

这是项目里最典型的适配器，其他 7 个结构完全一样。

```java
public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookHolder> {
```

**这行有三个信息：**
- `extends RecyclerView.Adapter<...>` —— 继承 RecyclerView 的适配器
- `<BookAdapter.BookHolder>` —— 泛型，声明"我的 ViewHolder 是 BookHolder 这个类"
- `BookAdapter.BookHolder` —— 内部类，写在 BookAdapter 里面

---

### 第一段：回调接口

```java
public interface OnBookClickListener {
    void onBookClick(Book book);
}
```

**为什么要有这个接口？**

Adapter 只管"把数据显示出来"，它**不该知道**点击之后要跳到哪个页面。所以它只负责"喊一声"：谁点了哪本书。具体怎么处理，由外部决定。

> **命名解读**：`OnBookClickListener` = **On**（当…时）+ **Book**（书）+ **Click**（点击）+ **Listener**（监听器）。读作"当书被点击时的监听器"。

---

### 第二段：数据与构造

```java
private final List<Book> items = new ArrayList<>();
private final OnBookClickListener listener;

public BookAdapter(OnBookClickListener listener) {
    this.listener = listener;
}
```

- `items` —— **为什么叫 items 而不是 books？** 因为 Adapter 是通用的，它装的是"列表项"。如果哪天改造成显示订单，变量名不用改。这是**抽象命名**的思路。
- `listener` —— 点击回调，构造时传进来，存成字段供 `onBindViewHolder` 使用

---

### 第三段：`submit` 更新数据

```java
public void submit(List<Book> books) {
    items.clear();                    // ① 清空旧数据
    if (books != null) items.addAll(books);   // ② 加入新数据
    notifyDataSetChanged();           // ③ 通知界面刷新
}
```

**① 为什么先 clear？**

因为 `items` 是 `final` 的，不能重新赋值（`items = books` 会编译报错），只能改内容。

**② 为什么判 null？**

调用方可能传 `null`（比如清空列表时）。不判的话 `addAll(null)` 会抛空指针。

**③ `notifyDataSetChanged()` 是什么？**

告诉 RecyclerView："数据变了，重新绑一遍吧。"

> **注意**：这是"全量刷新"，效率不高（会让所有可见项重新绑定）。更好的做法是 `notifyItemInserted()`、`DiffUtil` 等增量刷新。项目里数据量小（最多 20 条），全量刷新够用，代码也更简单。

**为什么叫 `submit` 而不是 `setData`？**

`submit` 有"提交"的意思，暗示"我把新数据交给你，你来处理"。这是很多官方示例（如 `ListAdapter.submitList()`）的命名习惯。用 `setData` 也不算错，但 `submit` 更符合社区习惯。

---

### 第四段：造空壳

```java
@NonNull
@Override
public BookHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_book, parent, false);
    return new BookHolder(view);
}
```

**逐句拆：**

```java
LayoutInflater.from(parent.getContext())
```
- `LayoutInflater` = 布局填充器，负责把 XML 变成 View 对象
- `from(context)` = 从哪个上下文创建
- `parent.getContext()` = 从父容器拿上下文（不用另外传，很方便）

```java
.inflate(R.layout.item_book, parent, false)
```
- `R.layout.item_book` = 要填充的布局
- `parent` = 父容器（用来参考布局参数）
- `false` = **不要立刻加到 parent 上**（RecyclerView 自己会管）

> **这个 `false` 非常关键**。写 `true` 的话，这一行会被立刻塞进 RecyclerView，然后 RecyclerView 又塞一次，直接崩：`IllegalStateException: ViewHolder views must not be attached`。

```java
return new BookHolder(view);
```
把造好的 View 包进 ViewHolder 里返回。

**`viewType` 是干嘛的？** 当一个列表里有多种样式的行时（比如聊天界面：我的消息靠右、对方消息靠左），用 `viewType` 区分。项目里每个列表只有一种样式，所以没用它。

---

### 第五段：填数据

```java
@Override
public void onBindViewHolder(@NonNull BookHolder holder, int position) {
    holder.bind(items.get(position), listener);
}
```

只有一行，把活交给 `BookHolder.bind()`。**为什么不在 Adapter 里直接写？**

因为 `ViewHolder` 是 `static` 内部类，把"怎么填"封装在它自己里面，代码更内聚。Adapter 只负责"第 position 个是哪个数据"。

---

### 第六段：ViewHolder

```java
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
```

**为什么 ViewHolder 要缓存控件引用？**

因为 `findViewById` 是**遍历整棵 View 树**找 ID，很慢。

- 不缓存：每次滑入都要 `findViewById` 5 次 × 100 次滑动 = 500 次遍历
- 缓存：构造时找一次，之后一直用

**这就是 ViewHolder 存在的唯一理由**——名字里的 **Holder** 就是"持有者"，它**持有**这一行的控件引用。

**为什么是 `static`？**

非静态内部类会**隐式持有外部类（BookAdapter）的引用**。ViewHolder 数量多、生命周期长，如果持有 Adapter 引用，容易造成内存泄漏。加 `static` 就断开了这个引用。

> 这是 Android 面试高频题："为什么 ViewHolder 要声明成 static？"

**`bind()` 方法里的 `itemView` 是什么？**

`RecyclerView.ViewHolder` 父类里的字段，就是构造时传进去的那个 `view`（整行的根布局）。所以 `itemView.setOnClickListener(...)` 就是"整行可点击"。

**点击回调为什么传 `book` 而不是 `position`？**

因为 `position` 会变（滑动时复用），而 `book` 是这个位置**当前对应的数据**。传对象更安全，调用方也不用再去查列表。

---

## 7.5 横向列表：只改一个参数

分类页的 chip 是横向滚动的，和竖向列表代码几乎一样，只差 LayoutManager：

```java
RecyclerView categoryList = view.findViewById(R.id.rv_categories);
categoryList.setLayoutManager(
        new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
categoryAdapter = new CategoryAdapter(categoryId -> {
    selectedCategoryId = categoryId;
    loadBooks();
});
categoryList.setAdapter(categoryAdapter);
```

三个参数：
1. `requireContext()` —— 上下文
2. `RecyclerView.HORIZONTAL` —— 横向排（默认是 `VERTICAL`）
3. `false` —— 不反转顺序（`true` 会从右往左排）

**就这一个参数的区别。** 其他全一样。

---

## 7.6 列表 + 状态页：项目里的标准写法

项目里每个列表都配一个 `StateView`，用 `FrameLayout` 叠在一起：

```xml
<FrameLayout>
    <RecyclerView android:id="@+id/rv_stores" ... />
    <com.yzc.bookmall.ui.widget.StateView android:id="@+id/state_view" ... />
</FrameLayout>
```

代码里：

```java
stateView = view.findViewById(R.id.state_view);
stateView.bind(recyclerView);      // ① 绑定：切换状态时自动控制列表的可见性
```

```java
private void loadNow() {
    if (!isAdded()) return;
    try {
        List<Store> stores = repository.getStores();
        adapter.submit(stores);                              // ② 先填数据
        if (stores.isEmpty()) stateView.showEmpty("暂时还没有书店");   // ③ 空
        else stateView.showContent();                        // ④ 有数据
    } catch (Exception e) {
        stateView.showError("书店加载失败：" + e.getMessage(), this::load);  // ⑤ 错误
    }
}
```

**为什么要这么设计？**

如果不做状态管理，用户会看到：
- 加载中：一片白屏（以为卡死了）
- 没数据：一片白屏（以为出 bug 了）
- 出错：一片白屏（完全不知道发生了什么）

加上 `StateView` 后，三种情况都有明确提示。**这是及格线和良好线的区别。**

第 13 篇会详细讲 `StateView` 的实现。

---

## 7.7 项目里的 8 个列表

| 列表 | Adapter | 数据 | LayoutManager |
|---|---|---|---|
| 首页书店 | `StoreAdapter` | `List<Store>` | 竖向 |
| 分类 chip | `CategoryAdapter` | `List<Category>` | 横向 |
| 图书（分类/书店/搜索/收藏共用） | `BookAdapter` | `List<Book>` | 竖向 |
| 购物车 | `CartAdapter` | `List<CartItem>` | 竖向 |
| 订单列表 | `OrderAdapter` | `List<Order>` | 竖向 |
| 订单明细 | `OrderItemAdapter` | `List<OrderItem>` | 竖向 |
| 评价 | `ReviewAdapter` | `List<Review>` | 竖向 |
| 收货地址 | `AddressAdapter` | `List<Address>` | 竖向 |

**注意 `BookAdapter` 被复用了 4 次**（分类页、书店详情、搜索、收藏）。因为它们的列表项长得一样，没必要写四个。

> **这就是适配器的价值**：数据源换了，Adapter 不用改。

---

## 7.8 购物车适配器的特殊之处

`CartAdapter` 比其他复杂一点，因为它要管**勾选状态**：

```java
private final Set<Long> selectedBookIds = new LinkedHashSet<>();
```

**为什么状态放在 Adapter 里，不放在每个 CartItem 上？**

因为 `CartItem` 是数据库查出来的**数据对象**，不应该混入"界面状态"。勾选是临时的、纯界面的，不该写进数据库。

**为什么用 `Set<Long>` 存 ID 而不是 `Set<CartItem>`？**

因为 `CartItem` 每次刷新都会重新创建新对象，`equals()` 默认比的是内存地址，旧的勾选记录就"对不上"了。存 ID（`long`）就没这个问题——值相等就是相等。

**勾选状态怎么和 CheckBox 同步？**

```java
checkBox.setOnCheckedChangeListener(null);      // ① 先解绑，防止复用触发误回调
checkBox.setChecked(checked);                   // ② 设置状态
checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {   // ③ 再绑回来
    if (isChecked) adapter.selectedBookIds.add(item.getBookId());
    else adapter.selectedBookIds.remove(item.getBookId());
    if (listener != null) listener.onSelectionChanged();
});
```

**① 那行是必须的。** 因为 ViewHolder 是复用的：滑出屏幕的第 3 项被拿去显示第 10 项时，`setChecked()` 会触发监听器，导致第 10 项被错误地标记为选中。先解绑再设置，就不会触发。

> 这个坑叫 **"RecyclerView 复用导致的错误回调"**，是新手必踩的。记住套路：**改状态前先解绑监听器**。

---

## 7.9 本篇小结

| 概念 | 要点 |
|---|---|
| RecyclerView | 只创建可见的 View，滑出即回收 |
| LayoutManager | 决定怎么排（竖/横/网格） |
| Adapter | 回答三个问题：多少条、怎么造、怎么填 |
| ViewHolder | 缓存控件引用，必须 `static` |
| `onCreateViewHolder` | 造壳，只调用少数几次 |
| `onBindViewHolder` | 填数据，每次滑入都调用 |
| `notifyDataSetChanged` | 全量刷新 |
| `submit()` | 项目约定的更新数据方法 |
| 复用陷阱 | 改 CheckBox 前先解绑监听器 |

### 自测

1. 100 条数据，RecyclerView 会创建多少个 ViewHolder？为什么？
2. `onCreateViewHolder` 和 `onBindViewHolder` 的调用次数为什么差很多？
3. `ViewHolder` 为什么必须声明成 `static`？
4. 横向列表和竖向列表代码差在哪一行？
5. 为什么购物车用 `Set<Long>` 存勾选状态，而不是给 `CartItem` 加个 `selected` 字段？

---

下一篇：[08 Fragment 与底部导航](08-Fragment与底部导航.md) —— 一个 Activity 里怎么装下四个页面。
