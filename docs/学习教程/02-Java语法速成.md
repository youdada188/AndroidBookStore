# 02 · Java 语法速成（只讲这个项目用到的）

> **本篇你会学到**：看懂项目里所有 Java 代码所需的语法。
> **不需要**：把 Java 学完。只学这 12 个知识点就够读这个项目了。

---

## 2.1 类 = 图纸，对象 = 按图纸造出来的东西

拿项目里的 `Book`（图书）举例：

```java
public class Book {          // 类：一张"书"的图纸
    private final String title;   // 字段（属性）
    private final int priceFen;

    public Book(String title, int priceFen) {  // 构造方法
        this.title = title;
        this.priceFen = priceFen;
    }

    public String getTitle() { return title; }  // 方法
}
```

```java
Book b = new Book("活水", 7900);   // 对象：按图纸造出来的一本具体的书
```

**三个词的区别**：

| 词 | 含义 | 例子 |
|---|---|---|
| 类（class） | 图纸 | `Book` |
| 对象（object）/ 实例（instance） | 造出来的实物 | `new Book(...)` 的结果 |
| 字段（field）/ 属性（property） | 实物上的信息 | `title`、`priceFen` |

---

## 2.2 `private` / `public` / `final` / `static`：四个修饰符

这四个词决定了"谁能碰这个东西"和"这东西能不能变"。

### `private` 私有 —— 只有自己类里能用

```java
public class Book {
    private final int priceFen;   // 外面不能直接改价格
}
```

如果外面能随便 `book.priceFen = -1`，价格就乱了。**私有化 + 提供只读方法**，这叫**封装**。

### `public` 公开 —— 谁都能用

```java
public int getPriceFen() { return priceFen; }   // 外面通过这个方法读价格
```

### `final` 最终 —— 赋值一次后不能再改

```java
private final String title;    // 书名一旦定下，不能改
```

> **为什么模型类（`Book`、`Store`、`Order`）的字段全是 `final`？**
>
> 因为一本书的数据从数据库读出来后，就不该再被修改。要"改价格"，应该是**新建一个对象**或者**去数据库改**。这样可以避免"改着改着数据就不一致了"的 bug。
>
> 这叫**不可变对象（Immutable Object）**，是良好的设计习惯。

### `static` 静态 —— 属于类，不属于某个对象

```java
public class SplashActivity {
    private static final int TOTAL_SECONDS = 3;   // 所有 SplashActivity 共用一份
}
```

对比一下：

```java
private int remaining = 3;         // 每个对象各有一份（每打开一次页面都是新的 3）
private static final int TOTAL_SECONDS = 3;  // 全局就一份，永远不会变
```

**记忆口诀**：
- `static` + `final` + 全大写 = **常量**，全项目共用一个值
- 只 `final` = **不可变字段**，每个对象有自己的值，但定下不能改

### 命名规范（重要！）

| 类型 | 规范 | 项目里的例子 |
|---|---|---|
| 类名 | **大驼峰**（每个单词首字母大写） | `BookRepository`、`StateView` |
| 方法名 / 变量名 | **小驼峰**（首单词小写，后面首字母大写） | `getPriceFen`、`selectedBookIds` |
| 常量 | **全大写下划线** | `TOTAL_SECONDS`、`DATABASE_NAME` |
| 包名 | **全小写** | `com.yzc.bookmall.data` |

**为什么这么规定？** 因为 Java 社区几百万人都是这么写的，你写 `GetPrice()` 别人一眼就知道你是新手。IDE 也能靠命名规则给你更好的代码提示。

---

## 2.3 构造方法：对象出生时做的事

```java
public Book(long id, long storeId, long categoryId, String categoryName, String title,
            String author, String publisher, int priceFen, int stock, int coverResId,
            String description, boolean recommended, int sales) {
    this.id = id;
    this.storeId = storeId;
    // ...
}
```

三个要点：

1. **方法名和类名完全一样**，没有返回值声明
2. `this.id = id` 里，`this.id` 指"这个对象自己的 id 字段"，右边的 `id` 是传进来的参数
3. 参数有 13 个，看起来吓人，但每个都对应数据库 `books` 表的一列

> **为什么参数这么多？** 因为 `Book` 对象就是把数据库一行记录"搬"到内存里。数据库有几列，它就有几个字段。后面第 09 篇你会看到 `mapBook()` 方法，就是按顺序把游标里的 13 个值塞进去。

---

## 2.4 继承 `extends`：站在别人的肩膀上

```java
public class MainActivity extends BaseActivity {
```

读作"MainActivity **继承** BaseActivity"，意思是：**MainActivity 自动拥有 BaseActivity 的全部能力，还可以在此基础上加自己的东西**。

本项目里最典型的一层继承链：

```
Activity（Android 系统提供）
   ↑
AppCompatActivity（AppCompat 库提供，让老系统也能用新特性）
   ↑
BaseActivity（我们自己写的，统一处理状态栏遮挡）
   ↑
MainActivity / LoginActivity / StoreDetailActivity ...
```

**`BaseActivity` 干了什么？**

```java
public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);  // 让内容铺满全屏
        applySystemBarAppearance(true);
    }
}
```

因为所有页面都继承了它，**这行代码只写一次，13 个页面全都生效**。

> `abstract` 是"抽象类"，意思是"这个类不能直接 new，只能被继承"。`BaseActivity` 本身没有界面，只是个"半成品模板"。

> `super.onCreate(...)` 里的 `super` 指"父类"。**必须调用**，否则父类没初始化，页面会崩。

---

## 2.5 接口 `interface`：一份"你必须会做这些事"的合同

项目里到处都是接口。看 `CartAdapter` 里这个：

```java
public interface Listener {
    void onQuantityChange(CartItem item, int delta);
    void onDelete(CartItem item);
    void onSelectionChanged();
    void onBookClick(long bookId);
}
```

翻译成人话：**"谁想当我的监听者，就必须实现这四个方法。"**

谁来实现？`CartFragment`：

```java
public class CartFragment extends Fragment implements CartAdapter.Listener {
    @Override
    public void onQuantityChange(CartItem item, int delta) {
        repository.updateCartQuantity(session.getUserId(), item.getBookId(), item.getQuantity() + delta);
        loadNow();
    }
    // ... 另外三个方法
}
```

**为什么要绕这一圈？**

因为 `CartAdapter`（管列表）**不应该知道**购物车页面长什么样、数据库怎么改。它只负责"用户点了 + 号，我通知一下"。具体怎么处理，由页面决定。

这叫**解耦**。以后你想把购物车换成另一种界面，`CartAdapter` 一行都不用改。

> **命名解读**：`Listener` = "监听器"。这是 Android/Java 里的通用叫法，凡是被回调的接口都爱叫 `XxxListener`（`OnClickListener`、`TextWatcher`…）。

---

## 2.6 Lambda 表达式：把"一段代码"当参数传

老写法（匿名内部类）：

```java
button.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        finish();
    }
});
```

新写法（Lambda，Java 8 起支持）：

```java
button.setOnClickListener(v -> finish());
```

**规则**：接口里**只有一个方法**时，可以简写成 `参数 -> 方法体`。

项目里到处都是：

```java
adapter = new StoreAdapter(store -> {
    Intent intent = new Intent(requireContext(), StoreDetailActivity.class);
    intent.putExtra(StoreDetailActivity.EXTRA_STORE_ID, store.getId());
    startActivity(intent);
});
```

这行意思是：**"当用户点了某家书店，就把这家店的对象交给我，我打开它的详情页。"**

> **怎么读？** `store -> { ... }` 读作"传入一个 store，然后做这些事"。

---

## 2.7 泛型 `<T>`：给容器贴上"只能装什么"的标签

```java
List<Book> books = repository.getBooksByStore(storeId);
```

`List<Book>` 读作"Book 的列表"。尖括号里就是**元素类型**。

好处：

```java
List<Book> books = ...;
for (Book book : books) {
    book.getTitle();     // ✅ 编译器知道元素是 Book，能直接调 getTitle()
}

List list = ...;         // 不用泛型
for (Object o : list) {
    ((Book) o).getTitle();  // ❌ 要手动强转，还容易转错
}
```

**项目里的泛型用法**：

| 写法 | 含义 |
|---|---|
| `List<Book>` | 图书列表 |
| `List<Store>` | 书店列表 |
| `List<CartItem>` | 购物车条目列表 |
| `RecyclerView.Adapter<StoreHolder>` | 一个"装 StoreHolder 的适配器" |

---

## 2.8 集合三兄弟：List、ArrayList、Set

```java
private final List<Book> items = new ArrayList<>();
```

这行是本项目出现频率最高的一行，拆开看：

- **`List`** —— 接口，"有序、可重复的列表"
- **`ArrayList`** —— 具体实现，"底层用数组实现的 List"
- **`<>`** —— 泛型，这里装 `Book`

**为什么左边写 `List` 右边写 `ArrayList`？**

这叫"**面向接口编程**"。左边是"我需要一个列表"，右边是"给我一个数组实现的"。哪天想换成 `LinkedList`，只改右边一处。

**`Set` 呢？** 用在购物车勾选：

```java
private final Set<Long> selectedBookIds = new LinkedHashSet<>();
```

- `Set` 保证**元素不重复**（一本书不可能被勾选两次）
- `LinkedHashSet` 保证**保持插入顺序**（勾选顺序不乱）

> **命名解读**：`selectedBookIds` = selected（已选中的）+ Book + Ids（多个 ID）。**末尾加 s 表示"多个"**，这是英文复数的习惯用法。

---

## 2.9 `try-with-resources`：用完自动关

数据库查询要打开"游标"（Cursor），用完必须关，否则内存泄漏。老写法要写 `finally`：

```java
Cursor c = db.query(...);
try {
    while (c.moveToNext()) { ... }
} finally {
    c.close();     // 必须记得关
}
```

新写法（Java 7+）：

```java
try (Cursor c = dbHelper.getReadableDatabase().query(...)) {
    while (c.moveToNext()) result.add(mapStore(c));
}   // 出了大括号，c 自动 close()
```

**只要类实现了 `AutoCloseable` 接口，就能这么写。** `Cursor`、`InputStream` 都实现了。

项目里所有数据库查询、所有读文件的代码都用了这个写法。

---

## 2.10 三元表达式 `? :`

```java
delivery.setText(store.getDeliveryFeeFen() == 0
        ? "免配送费"
        : itemView.getContext().getString(R.string.store_delivery_fee, FormatUtils.yuan(store.getDeliveryFeeFen())));
```

读作：**"条件 ? 成立时的值 : 不成立时的值"**。

等价于：

```java
if (store.getDeliveryFeeFen() == 0) {
    delivery.setText("免配送费");
} else {
    delivery.setText(...);
}
```

简单判断用三元更紧凑，复杂逻辑还是老老实实写 `if`。

---

## 2.11 字符串格式化：`String.format` 与 `Locale`

```java
public static String yuan(int priceFen) {
    return String.format(Locale.CHINA, "%.2f", priceFen / 100.0);
}
```

- `%.2f` = 保留 2 位小数的浮点数
- `priceFen / 100.0` = 分转元（7900 分 → 79.00 元）
- `Locale.CHINA` = 用中国地区的数字格式

**为什么一定要传 `Locale`？**

因为某些国家用逗号当小数点（`79,00`）。不指定 Locale 的话，用户手机语言一变，价格就显示错了。

> **命名解读**：`Locale` = 语言环境/地区设置。`%.2f` 里的 `f` 是 **float**（浮点数）。

项目里还有一种格式化，用资源字符串：

```xml
<string name="store_rating">%1$s 分</string>
```

```java
getString(R.string.store_rating, FormatUtils.rating(store.getRating()))
```

- `%1$s` = 第 1 个参数，按字符串输出
- `%1$d` = 第 1 个参数，按整数输出

**为什么要放资源文件里？** 因为要做多语言时，只要加一份 `values-en/strings.xml` 就行，代码不用改。

---

## 2.12 常见小语法速查

| 写法 | 含义 | 项目例子 |
|---|---|---|
| `a != null && !a.isEmpty()` | 非空且非空串 | 到处都有 |
| `Math.max(0, Math.min(x, y))` | 把 x 夹在 0~y 之间 | 数量限制、金额不为负 |
| `System.currentTimeMillis()` | 当前时间戳（毫秒） | 创建时间、订单号 |
| `@Override` | 标记"我重写了父类方法" | 每个生命周期方法 |
| `@NonNull` | 告诉 IDE"这个参数不会是 null" | Adapter 的 `onBindViewHolder` |
| `instanceof` | 判断是不是某个类型 | 用得少 |
| `final` 修饰参数 | 参数在方法内不能重新赋值 | 弹窗里的 `final RatingBar` |

---

## 2.13 一个完整例子：读懂 `SessionManager`

现在用学到的知识，逐行读这个只有 18 行的类：

```java
public class SessionManager {
    private static final String PREF_NAME = "book_mall_session";   // ①
    private static final String KEY_USER_ID = "user_id";           // ②
    private static final String KEY_USERNAME = "username";         // ②
    private final SharedPreferences preferences;                   // ③

    public SessionManager(Context context) {                       // ④
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(long userId, String username) {        // ⑤
        preferences.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public boolean isLoggedIn() { return getUserId() > 0; }        // ⑥
    public long getUserId() { return preferences.getLong(KEY_USER_ID, -1L); }
    public String getUsername() { return preferences.getString(KEY_USERNAME, ""); }
    public void clearSession() { preferences.edit().clear().apply(); }
}
```

**逐条解释：**

① `PREF_NAME` —— 存储文件的名字。**PREF 是 Preference（偏好/配置）的缩写**，Android 里管这种键值存储叫 SharedPreferences。名字里带 `_NAME` 表示"这是个名字"。

② `KEY_USER_ID` / `KEY_USERNAME` —— 存进去的"键"。**KEY 是钥匙，用来找到对应的值**。用 `KEY_` 开头是社区惯例，一眼能看出这是 map 的 key。

③ `private final SharedPreferences preferences` —— 字段名用了复数 `preferences`，因为它内部存了很多条键值对。`final` 表示构造完就不能换对象了。

④ 构造方法接收 `Context`。**Context 是"上下文"，可以理解为"这个 App 的运行环境"**，获取资源、打开数据库、读写配置都要用它。`getApplicationContext()` 拿的是"整个 App 的 Context"，比 Activity 的活得久，不会泄漏。

⑤ `saveSession` —— **save（保存）+ Session（会话）**。Session 指"用户登录后的一段会话期"。`edit()` 开启编辑，`putXxx` 写入，`apply()` 异步提交。

⑥ `isLoggedIn` —— **布尔值方法用 `is` 开头**，读起来像一句话："是否已登录"。实现是"用户 ID 大于 0 就算登录了"（没登录时存的是 -1）。

**看，18 行代码里包含了：类、常量、字段、构造方法、方法、返回值、链式调用、默认值。** 你已经能读懂它了。

---

## 2.14 本篇小结

| 概念 | 一句话 |
|---|---|
| 类 / 对象 | 图纸 / 按图纸造出来的东西 |
| `private` | 只有自己能用（封装） |
| `final` | 赋值后不能改（不可变） |
| `static` | 属于类，全局一份 |
| `extends` | 继承，白拿父类能力 |
| `implements` | 实现接口，履行合同 |
| Lambda | `x -> 做什么`，把代码当参数传 |
| 泛型 `<T>` | 容器里只能装 T |
| `List` / `Set` | 有序可重复 / 去重 |
| try-with-resources | 用完自动关 |
| 命名规范 | 类大驼峰、方法小驼峰、常量全大写 |

### 自测

1. `private final int priceFen` 里的 `final` 去掉会怎样？为什么项目里要去掉？
2. `List<Book> list = new ArrayList<>()` 为什么左边写 `List` 不写 `ArrayList`？
3. `store -> { ... }` 这种写法叫什么？它要求接口满足什么条件？
4. 为什么常量叫 `TOTAL_SECONDS` 而不是 `totalSeconds`？

---

下一篇：[03 工程结构与资源系统](03-工程结构与资源系统.md) —— 搞懂每个文件夹是干嘛的，以及那个神秘的 `R` 类。
