# 06 · Intent 与页面跳转

> **本篇你会学到**：页面之间怎么跳、怎么带数据、怎么拿返回值，以及为什么**只传 ID 不传对象**。

---

## 6.1 Intent 是什么

**Intent = 意图。** 直译有点抽象，可以理解成**一个信封**：

```
┌─────────────────────────┐
│  收件人：BookDetailActivity │   ← 要去哪个页面
├─────────────────────────┤
│  附件：book_id = 7        │   ← 要带什么数据
└─────────────────────────┘
```

你把信封交给系统（`startActivity(intent)`），系统负责把信送到。

**Intent 能表达两件事**：
1. **我要去哪**（目标页面）
2. **我要带什么**（数据）

---

## 6.2 显式 Intent vs 隐式 Intent

### 显式（Explicit）—— 项目里 100% 用这种

明确写出目标类：

```java
Intent intent = new Intent(this, BookDetailActivity.class);
startActivity(intent);
```

- 第一个参数 `this` = 当前 Context（从哪出发）
- 第二个参数 `BookDetailActivity.class` = 目标页面

**读作**："从我这，去 BookDetailActivity。"

### 隐式（Implicit）—— 项目里没用

不指定具体类，只说"我要干一件事"：

```java
Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setData(Uri.parse("https://www.example.com"));
startActivity(intent);   // 系统会问：用哪个浏览器打开？
```

**为什么项目不用隐式？** 因为隐式是"跨 App"用的（打开网页、拨电话、分享）。App 内部跳转一律用显式，**明确、安全、不会跳错**。

---

## 6.3 带数据：`putExtra` 与 `getXxxExtra`

### 发送方

```java
// HomeFragment 里点击书店卡片
Intent intent = new Intent(requireContext(), StoreDetailActivity.class);
intent.putExtra(StoreDetailActivity.EXTRA_STORE_ID, store.getId());
startActivity(intent);
```

- `putExtra(键, 值)` = 往信封里塞一个键值对
- 键是 `String`，值可以是 `long`、`int`、`String`、`boolean`、`long[]` 等基本类型

### 接收方

```java
// StoreDetailActivity 的 onCreate
long storeId = getIntent().getLongExtra(EXTRA_STORE_ID, -1);
```

- `getIntent()` = 拿到"送自己来的那个信封"
- `getLongExtra(键, 默认值)` = 按类型取值，取不到就用默认值

**注意第二个参数 `-1`**：如果调用方忘了传，这里会得到 `-1`，后面就能用 `if (storeId < 0)` 判断出错了。

### 类型必须匹配

| 发送 | 接收 |
|---|---|
| `putExtra(key, 7L)` | `getLongExtra(key, -1)` |
| `putExtra(key, "abc")` | `getStringExtra(key)` |
| `putExtra(key, true)` | `getBooleanExtra(key, false)` |
| `putExtra(key, longArray)` | `getLongArrayExtra(key)` |

**类型不匹配会返回默认值，不会崩**，但你会莫名其妙拿不到数据。这是新手常见坑。

---

## 6.4 `EXTRA_` 常量：为什么不用字符串直接写

看项目里的写法：

```java
public class BookDetailActivity extends BaseActivity {
    public static final String EXTRA_BOOK_ID = "book_id";   // ← 定义一次
```

```java
// 发送方（CategoryFragment）
intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());

// 接收方（BookDetailActivity 自己）
long bookId = getIntent().getLongExtra(EXTRA_BOOK_ID, -1);
```

**如果直接写字符串会怎样？**

```java
// 发送方
intent.putExtra("book_id", book.getId());

// 接收方（手滑打错一个字母）
long bookId = getIntent().getLongExtra("boook_id", -1);   // 拿到 -1，页面打不开
```

**编译器不会报错**，你只能靠运行时调试发现。而用常量的话：

```java
BookDetailActivity.EXTRA_BOOCK_ID    // ← 编译直接报红，因为没这个字段
```

**打错字立刻发现。** 这就是常量的价值。

### 命名解读

`EXTRA_BOOK_ID` 拆开看：

| 部分 | 含义 |
|---|---|
| `EXTRA` | Intent 附加数据的通用前缀（Android 源码里也是这么命名的，如 `Intent.EXTRA_TEXT`） |
| `BOOK` | 是什么东西的 ID |
| `ID` | 主键 |

`public static final` 三件套 = **公开 + 全局唯一 + 不可改** = 常量。

### 项目里的所有 EXTRA

| 常量 | 值 | 谁发 | 谁收 |
|---|---|---|---|
| `StoreDetailActivity.EXTRA_STORE_ID` | `"store_id"` | `HomeFragment` | `StoreDetailActivity` |
| `BookDetailActivity.EXTRA_BOOK_ID` | `"book_id"` | 分类页、书店详情、搜索、收藏 | `BookDetailActivity` |
| `OrderDetailActivity.EXTRA_ORDER_ID` | `"order_id"` | 订单列表、结算成功 | `OrderDetailActivity` |
| `CheckoutActivity.EXTRA_BOOK_IDS` | `"selected_book_ids"` | `CartFragment` | `CheckoutActivity` |
| `AddressListActivity.EXTRA_PICK` | `"pick_mode"` | `CheckoutActivity` | `AddressListActivity` |
| `AddressListActivity.EXTRA_PICKED_ID` | `"address_id"` | `AddressListActivity` | `CheckoutActivity` |

---

## 6.5 最重要的设计原则：只传 ID，不传对象

假设你要把一本书的信息传给详情页。有两种做法：

### ❌ 做法一：把整个 Book 对象传过去

```java
intent.putExtra("book", book);   // 编译都过不了
```

`Intent` 只支持基本类型。要用对象，必须让 `Book` 实现 `Serializable` 或 `Parcelable` 接口，写一堆序列化代码。

**更严重的问题**：传过去的是**快照**。如果这本书在数据库里被改了价格，详情页显示的还是旧价格。

### ✅ 做法二：只传 ID，让对方自己查（项目的做法）

```java
// 发送方：只传一个 long
intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());

// 接收方：拿 ID 去数据库查最新数据
book = repository.getBookById(bookId);
```

**三个好处**：

1. **数据永远最新**——每次都是现查的
2. **Intent 体积小**——一个 long 才 8 字节，传对象可能几 KB
3. **解耦**——发送方不需要知道接收方要显示什么

> **这条原则在 Android 开发里叫"传 ID 不传实体"**，是行业共识。你在项目里看到的每一个 `EXTRA_XXX_ID` 都是这个思路。

---

## 6.6 完整链路追踪：从首页到图书详情

跟一遍数据是怎么流的：

```
【第 1 步】HomeFragment 显示书店列表
        ↓ 用户点击"西西弗书店"
【第 2 步】StoreAdapter 的点击回调
        store -> {
            Intent intent = new Intent(requireContext(), StoreDetailActivity.class);
            intent.putExtra(StoreDetailActivity.EXTRA_STORE_ID, store.getId());   // 传 1L
            startActivity(intent);
        }
        ↓
【第 3 步】StoreDetailActivity.onCreate
        storeId = getIntent().getLongExtra(EXTRA_STORE_ID, -1);   // 拿到 1L
        Store store = repository.getStoreById(storeId);            // 查数据库
        bindStore(store);                                          // 填到界面上
        ↓ 用户点击店里的一本书
【第 4 步】BookAdapter 的点击回调
        openBook(book) → intent.putExtra(EXTRA_BOOK_ID, book.getId());   // 传 7L
        ↓
【第 5 步】BookDetailActivity.onCreate
        bookId = getIntent().getLongExtra(EXTRA_BOOK_ID, -1);      // 拿到 7L
        book = repository.getBookById(bookId);                     // 再查一次
        bindBook(book);
```

**注意每一步都重新查了数据库**。虽然 `StoreDetailActivity` 里已经有 `Book` 对象了，但它传给详情页的仍然只是一个 ID。

---

## 6.7 带返回值跳转：结算页选地址

普通跳转是"单程"的。但有些场景需要**对方办完事回来告诉我结果**，比如：

> 结算页 → "去选地址" → 地址列表 → 选中一个 → 回到结算页，地址已经填好了

这就需要 `startActivityForResult`。

### 发送方：`CheckoutActivity`

```java
private static final int REQUEST_ADDRESS = 1001;   // ① 请求码

private void openAddressPicker() {
    Intent intent = new Intent(this, AddressListActivity.class);
    intent.putExtra(AddressListActivity.EXTRA_PICK, true);   // ② 告诉对方"我是来选择模式的"
    startActivityForResult(intent, REQUEST_ADDRESS);         // ③ 带着请求码出发
}
```

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_ADDRESS && resultCode == RESULT_OK && data != null) {  // ④
        long id = data.getLongExtra(AddressListActivity.EXTRA_PICKED_ID, -1);
        for (Address item : repository.getAddresses(session.getUserId())) {
            if (item.getId() == id) {
                address = item;
                bindAddress();      // ⑤ 更新界面
                break;
            }
        }
    }
}
```

**④ 的三个判断缺一不可**：

| 判断 | 作用 |
|---|---|
| `requestCode == REQUEST_ADDRESS` | 确认是"选地址"这件事回来了（可能同时有好几个请求） |
| `resultCode == RESULT_OK` | 确认对方是"成功返回"而不是"用户按了返回键取消" |
| `data != null` | 确认确实带了数据回来 |

### 接收方：`AddressListActivity`

```java
private boolean pickMode;   // ① 是不是"选择模式"

@Override
protected void onCreate(Bundle savedInstanceState) {
    ...
    pickMode = getIntent().getBooleanExtra(EXTRA_PICK, false);   // ② 读出来
    ...
}

// 用户点了某个地址
@Override
public void onSelect(Address address) {
    if (pickMode) {
        Intent data = new Intent();                              // ③ 造一个空信封
        data.putExtra(EXTRA_PICKED_ID, address.getId());         // ④ 塞入选中的 ID
        setResult(RESULT_OK, data);                              // ⑤ 设置返回值
        finish();                                                // ⑥ 关闭自己，回到上一页
    } else {
        // 非选择模式：设为默认地址
        repository.setDefaultAddress(session.getUserId(), address.getId());
        Ui.toast(this, "已设为默认地址");
        reload();
    }
}
```

**这个设计的巧妙之处**：`AddressListActivity` **一份代码两种用途**——

| 场景 | `pickMode` | 点击地址的效果 |
|---|---|---|
| 从"我的 → 收货地址"进入 | `false` | 设为默认地址 |
| 从"结算页 → 选择地址"进入 | `true` | 返回选中的 ID 并关闭 |

不用写两个页面。

> **`REQUEST_ADDRESS = 1001` 为什么是 1001？** 只是个约定的数字，只要**同一个页面里不重复**就行。用 1001 而不是 1，是为了避免和系统或库内部的请求码撞车。

---

## 6.8 `finish()` 与返回栈的关系

```java
startActivity(intent);   // 新页面入栈
finish();                // 当前页面出栈
```

**常见组合**：

```java
// 场景 1：普通跳转，保留当前页
startActivity(intent);
// 按返回键能回到这里

// 场景 2：跳转后不要当前页了
startActivity(intent);
finish();
// 按返回键回不到这里

// 场景 3：跳转并清空整个栈（登录成功、退出登录）
intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
startActivity(intent);
finish();
// 按返回键直接退出 App
```

**项目里场景 2 的例子**：`SplashActivity` 跳转后 `finish()`，否则用户按返回会回到启动页，又倒计时 3 秒，很怪。

**项目里场景 3 的例子**：登录成功、退出登录、清空数据后回登录页。

---

## 6.9 一个特殊场景：登录后回到原入口

用户没登录时点"购物车 → 去结算"，应该跳登录页；**登录完要回到购物车**，而不是回首页。

项目的做法：

```java
// ① CartFragment 跳登录时带上"我想去哪"
startActivity(new Intent(requireContext(), LoginActivity.class)
        .putExtra("redirect_tab", "cart"));
```

```java
// ② LoginActivity 登录成功后，把意图继续传下去
private void openMain() {
    Intent intent = new Intent(this, MainActivity.class);
    String tab = getIntent().getStringExtra("redirect_tab");
    if (tab != null) intent.putExtra("redirect_tab", tab);   // 转发
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    finish();
}
```

```java
// ③ MainActivity 根据 tab 决定默认显示哪个 Fragment
private int resolveInitialTab() {
    String tab = getIntent().getStringExtra("redirect_tab");
    if ("cart".equals(tab)) return R.id.nav_cart;
    if ("profile".equals(tab)) return R.id.nav_profile;
    return R.id.nav_home;
}
```

**这就是"参数接力"**：CartFragment → LoginActivity → MainActivity，一路把 `redirect_tab` 传下去。

> **为什么不直接 `startActivity(CartActivity)`？** 因为购物车已经不是独立页面了，它是 `MainActivity` 里的一个 Fragment。所以只能告诉 MainActivity"你打开时默认选中购物车 Tab"。

---

## 6.10 本篇小结

| 概念 | 要点 |
|---|---|
| Intent | 信封：去哪 + 带什么 |
| 显式 Intent | `new Intent(this, 目标.class)`，App 内跳转用它 |
| `putExtra` / `getXxxExtra` | 键值对传数据，类型必须匹配 |
| `EXTRA_` 常量 | 防止打错字，编译期就能发现 |
| 只传 ID | 传 ID 让对方查库，保证数据最新、体积小 |
| `startActivityForResult` | 需要对方返回结果时用 |
| `setResult` + `finish` | 设置返回值并关闭自己 |
| `REQUEST_` 请求码 | 区分是哪个请求回来了 |
| Flag | 控制返回栈行为 |

### 自测

1. 为什么项目里传的是 `book_id` 而不是整个 `Book` 对象？说出至少两个理由。
2. `EXTRA_BOOK_ID` 这个常量如果用字符串 `"book_id"` 直接写，会有什么风险？
3. `onActivityResult` 里三个判断分别防止什么情况？
4. `startActivity(intent)` 之后加不加 `finish()`，返回键的行为有什么区别？

---

下一篇：[07 RecyclerView 列表](07-RecyclerView列表.md) —— 一百本书为什么只用三个 View。
