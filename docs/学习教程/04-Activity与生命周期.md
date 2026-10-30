# 04 · Activity 与生命周期

> **本篇你会学到**：Activity 是什么、它从生到死经历什么、`onCreate` 里为什么这么写、`Context` 到底是个啥。

---

## 4.1 Activity 是什么

**Activity = 屏幕上的一屏内容。**

你手机上的每个 App，看到的每个"页面"，基本都对应一个 Activity：

| 你看到的画面 | 对应的 Activity |
|---|---|
| 深蓝色 Logo + 倒计时 | `SplashActivity` |
| 输入账号密码 | `LoginActivity` |
| 底部有四个 Tab 的首页 | `MainActivity` |
| 一家书店的详情 | `StoreDetailActivity` |
| 一本书的详情 | `BookDetailActivity` |

本项目一共 13 个 Activity。

> **命名解读**：`SplashActivity` 里的 `Splash` 是"溅起水花"的意思，行业里管启动页叫 **Splash Screen**（闪屏）。`Login` = 登录，`Detail` = 详情。

---

## 4.2 一个最小的 Activity 长什么样

看项目里最短的页面 `ReviewListActivity`（我的评价）：

```java
public class ReviewListActivity extends BaseActivity {   // ①

    private BookRepository repository;                    // ②
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {  // ③
        super.onCreate(savedInstanceState);               // ④
        setContentView(R.layout.activity_review_list);    // ⑤
        applyInsets(findViewById(android.R.id.content));  // ⑥

        ((TextView) findViewById(R.id.tv_toolbar_title)).setText(R.string.review_my_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());  // ⑦
    }
}
```

**逐行拆解：**

**① `extends BaseActivity`**
继承我们自己的基类，白拿"处理状态栏遮挡"的能力。`BaseActivity` 又继承 `AppCompatActivity`，`AppCompatActivity` 又继承 `Activity`。

**② 字段（成员变量）**
`private BookRepository repository;` —— 这个页面要用到的"工具"。声明成字段而不是局部变量，是因为**多个方法都要用它**。

**③ `@Override protected void onCreate(Bundle savedInstanceState)`**
- `@Override` = 我重写了父类的方法
- `protected` = 只有自己和子类能调（Android 框架约定）
- `onCreate` = 生命周期第一个方法，**页面创建时调用**
- `Bundle savedInstanceState` = 保存的临时状态（比如屏幕旋转前的数据）

**④ `super.onCreate(savedInstanceState)`**
调用父类的 `onCreate`。**必须写**，而且必须是第一行。父类要在这里做一些系统级的初始化。

**⑤ `setContentView(R.layout.activity_review_list)`**
"把这张布局贴到屏幕上"。这一行之前，页面是空白的；这一行之后，布局里的控件才存在。

**⑥ `applyInsets(...)`**
`BaseActivity` 提供的工具方法，把状态栏的高度补成内边距。因为 `targetSdk 36` 强制边到边，不补的话标题会被状态栏压住。

> `android.R.id.content` 是系统内置的 ID，指"Activity 内容区的那个容器"。所有 `setContentView` 的布局都装在里面。

**⑦ `findViewById(...).setOnClickListener(v -> finish())`**
- `findViewById(R.id.iv_back)` = 按 ID 找到那个返回箭头控件
- `setOnClickListener` = 设置点击监听
- `v -> finish()` = 点击时调用 `finish()`，**关闭当前页面**

> **命名解读**：`iv_back` = **I**mage**V**iew + back（返回）。控件 ID 也有前缀规范：
> | 前缀 | 控件类型 |
> |---|---|
> | `tv_` | TextView |
> | `iv_` | ImageView |
> | `et_` | EditText |
> | `btn_` | Button |
> | `rv_` | RecyclerView |
> | `cb_` | CheckBox |
> | `layout_` | LinearLayout / FrameLayout 等容器 |
>
> 这套前缀让你在 `findViewById` 时一眼知道该转成什么类型。

---

## 4.3 生命周期：一个页面的"一生"

```
        ┌─────────────┐
        │  onCreate   │  页面被创建（setContentView 在这）
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │  onStart    │  页面可见了（但还没法交互）
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │  onResume   │  页面到了前台，可以交互了 ← 最活跃的状态
        └──────┬──────┘
               ↓  （用户按 Home / 切到别的页面）
        ┌─────────────┐
        │  onPause    │  失去焦点
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │  onStop     │  完全看不见了
        └──────┬──────┘
               ↓  （用户返回）
        ┌─────────────┐
        │ onRestart → onStart → onResume │
        └─────────────┘
               ↓  （页面被销毁）
        ┌─────────────┐
        │  onDestroy  │  释放资源
        └─────────────┘
```

### 用生活比喻

想象你在餐厅吃饭：

| 生命周期 | 现实场景 |
|---|---|
| `onCreate` | 服务员带你入座，摆好餐具（**布局加载**） |
| `onStart` | 你坐下了，能看到桌上的菜（**可见**） |
| `onResume` | 你拿起筷子开始吃（**可交互**） |
| `onPause` | 你接了个电话，筷子放下（**失去焦点**） |
| `onStop` | 你走到餐厅外面打电话（**看不见了**） |
| `onDestroy` | 你结账走人，桌子收拾干净（**销毁**） |

### 本项目实际用了哪几个？

只有三个，而且用法很典型：

| 方法 | 谁用了 | 干什么 |
|---|---|---|
| `onCreate` | 全部 13 个 Activity | 加载布局、绑定事件、首次加载数据 |
| `onResume` | `FavoriteActivity`、`OrderListActivity`、4 个 Fragment | **重新刷新数据** |
| `onDestroy` | `SplashActivity`、`StoreDetailActivity`、`BookDetailActivity` | 取消定时器，防止内存泄漏 |

---

## 4.4 为什么刷新数据要写在 `onResume`

看 `FavoriteActivity`：

```java
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
```

**为什么不写在 `onCreate`？**

设想这个场景：

1. 你在"我的收藏"页面，看到 3 本书
2. 点进其中一本，点了"取消收藏"
3. 按返回键回到收藏列表

如果刷新写在 `onCreate`，页面不会重建（它还在返回栈里），**你还会看到那本已经取消收藏的书**。

而 `onResume` 在**每次页面回到前台时都会调用**，所以数据会重新查一遍，列表就正确了。

> **记住这条规律：凡是"从别的页面返回后可能变化的数据"，都放 `onResume` 刷新。**

---

## 4.5 `onDestroy` 里为什么要 `removeCallbacks`

看 `SplashActivity`：

```java
private final Handler handler = new Handler(Looper.getMainLooper());

@Override
protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);   // 取消所有待执行的任务
    super.onDestroy();
}
```

**不写会怎样？**

假设倒计时还没跑完，用户按了返回键退出。此时 `SplashActivity` 已经销毁，但 `handler` 里还挂着一个"1 秒后执行"的任务。1 秒后任务触发，去操作一个**已经不存在的页面**——轻则报错，重则崩溃，还会让页面对象无法被回收（**内存泄漏**）。

> **`Handler` 是什么？** 它是 Android 的"定时任务投递器"。`postDelayed(任务, 毫秒)` = "过 N 毫秒后，在主线程执行这个任务"。
>
> **`Looper.getMainLooper()` 是什么？** 主线程（UI 线程）的消息循环。UI 只能在主线程改，所以 Handler 要绑定它。

---

## 4.6 `Context`：Android 里最玄乎的词

代码里到处是 `Context`：

```java
new BookRepository(context);                        // 建数据库
context.getSharedPreferences(...)                   // 读写配置
context.getAssets().open("seed/books.json")         // 读 assets
context.getString(R.string.app_name)                // 取字符串
context.getColor(R.color.brand_blue)                // 取颜色
```

**Context 直译是"上下文"，实际含义是"当前所处的环境"。**

它像一个**万能钥匙**，拿着它就能访问 App 的各种资源：

| 你想干的事 | 用 Context 的方法 |
|---|---|
| 读字符串/颜色/图片 | `getString()` / `getColor()` / `getDrawable()` |
| 启动另一个页面 | `startActivity()` |
| 弹提示 | `Toast.makeText(context, ...)` |
| 打开数据库 | `openOrCreateDatabase()` |
| 读写配置 | `getSharedPreferences()` |

### Activity 本身就是 Context

因为 `Activity extends ContextThemeWrapper extends Context`，所以：

```java
// 在 Activity 里，可以直接写：
Toast.makeText(this, "提示", Toast.LENGTH_SHORT).show();
//               ↑ 这个 this 就是 Context

// 也可以写：
getString(R.string.app_name);
// 等价于 this.getString(R.string.app_name);
```

### `getApplicationContext()` 的区别

```java
context.getApplicationContext()
```

- **Activity 的 Context**：跟着页面走，页面销毁它就没了
- **Application 的 Context**：跟着整个 App 走，App 活着它就在

**什么时候用哪个？**

| 场景 | 用哪个 | 原因 |
|---|---|---|
| 弹 Toast、启动 Activity、加载布局 | Activity 的 `this` | 需要页面的主题和返回栈 |
| 建数据库、读写 SharedPreferences | `getApplicationContext()` | 生命周期长，避免泄漏 |

项目里的写法：

```java
// BookDbHelper 构造方法
public BookDbHelper(Context context) {
    super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    this.context = context.getApplicationContext();   // 存长生命周期的那个
}
```

```java
// SessionManager 构造方法
public SessionManager(Context context) {
    preferences = context.getApplicationContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
}
```

> **`Context.MODE_PRIVATE`** = 私有模式，只有本 App 能读写这个配置文件。

---

## 4.7 返回栈：为什么按返回键能回到上一页

Android 用**栈（Stack）**管理页面：

```
打开 App：      [Splash]
跳到登录：      [Splash] [Login]
登录后跳首页：  [Main]                    ← 用了 CLEAR_TASK 清空之前的
点书店：        [Main] [StoreDetail]
点图书：        [Main] [StoreDetail] [BookDetail]
按返回：        [Main] [StoreDetail]      ← 弹出栈顶
再按返回：      [Main]
再按返回：      退出 App
```

**栈的特点：后进先出。**

### `finish()` vs `Intent` Flag

```java
// 关闭当前页面，自动回到上一页
finish();
```

```java
// LoginActivity 登录成功后：
Intent intent = new Intent(this, MainActivity.class);
intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
              | Intent.FLAG_ACTIVITY_NEW_TASK
              | Intent.FLAG_ACTIVITY_CLEAR_TASK);
startActivity(intent);
finish();
```

这三个 Flag 组合的意思是：**清空整个返回栈，只留 MainActivity**。

**为什么？** 因为登录成功后，用户按返回键**不应该回到登录页**（已经登录了还回登录页很奇怪）。同理，退出登录时也用了这组 Flag。

| Flag | 作用 |
|---|---|
| `CLEAR_TOP` | 如果目标页面已在栈里，把它上面的都清掉 |
| `NEW_TASK` | 新建一个任务栈 |
| `CLEAR_TASK` | 清空整个任务栈 |

---

## 4.8 完整精读：`SplashActivity`

这是项目里最短但最完整的 Activity，把它吃透，其他页面都是它的变体。

```java
public class SplashActivity extends BaseActivity {
    private static final int TOTAL_SECONDS = 3;                    // ①

    private final Handler handler = new Handler(Looper.getMainLooper());  // ②
    private TextView countdownView;                                 // ③
    private int remaining = TOTAL_SECONDS;                          // ④
    private boolean navigated;                                      // ⑤

    private final Runnable ticker = new Runnable() {                // ⑥
        @Override
        public void run() {
            remaining--;                                            // ⑦
            if (remaining <= 0) {
                goNext();
                return;
            }
            countdownView.setText(getString(R.string.splash_countdown, remaining));
            handler.postDelayed(this, 1000);                        // ⑧
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);                   // ⑨
        applySystemBarAppearance(false);                            // ⑩
        applyInsets(findViewById(R.id.root));

        countdownView = findViewById(R.id.tv_countdown);
        countdownView.setText(getString(R.string.splash_countdown, remaining));
        findViewById(R.id.tv_skip).setOnClickListener(v -> goNext()); // ⑪
        handler.postDelayed(ticker, 1000);                          // ⑫
    }

    private void goNext() {
        if (navigated) return;                                      // ⑬
        navigated = true;
        handler.removeCallbacksAndMessages(null);                   // ⑭
        SessionManager session = new SessionManager(this);
        Intent intent = new Intent(this, session.isLoggedIn()
                ? MainActivity.class : LoginActivity.class);        // ⑮
        startActivity(intent);
        finish();                                                   // ⑯
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);                   // ⑰
        super.onDestroy();
    }
}
```

### 逐条讲解

**① `TOTAL_SECONDS`**
常量，倒计时总秒数。全大写是常量规范。**为什么叫 `TOTAL_` 而不是 `SECONDS`？** 因为它强调"总共"，和下面的 `remaining`（剩余）形成对照。

**② `handler`**
定时器。`new Handler(Looper.getMainLooper())` 保证任务在主线程执行（UI 只能在主线程改）。

**③ `countdownView`**
"倒计时那个文本控件"。**`View` 后缀说明它是个界面控件**，`countdown` 说明它显示倒计时。

**④ `remaining`**
剩余秒数。**用 `-ing` 结尾表示"正在进行的量"**，和 `TOTAL` 对比：总 3 秒，剩 2 秒。

**⑤ `navigated`**
"是否已经跳转过了"。**布尔变量用过去分词 `navigated`**，读作"已跳转"。

**为什么要这个标志？** 因为用户可能先点了"跳过"，3 秒后倒计时也到点了——`goNext()` 会被调用两次，页面会跳两次。加了 `navigated` 判断，第二次直接 return。

**⑥ `ticker`**
"滴答器"，指每秒执行一次的任务。**`-er` 结尾常表示"做某事的東西"**（`adapter` 适配器、`handler` 处理器、`ticker` 滴答器）。

**⑦ `remaining--`**
自减，等价于 `remaining = remaining - 1`。

**⑧ `handler.postDelayed(this, 1000)`**
"1 秒后再执行我自己一次"。**这就是循环倒计时的写法**：任务执行完，再把自己排进队列。

**⑨ `setContentView(R.layout.activity_splash)`**
把 `res/layout/activity_splash.xml` 贴到屏幕上。

**⑩ `applySystemBarAppearance(false)`**
`false` = 状态栏图标用**白色**。因为启动页背景是深蓝色，白图标才看得清。其他页面背景是浅色，用默认的深色图标。

**⑪ `setOnClickListener(v -> goNext())`**
点"跳过"直接跳转。

**⑫ `handler.postDelayed(ticker, 1000)`**
1 秒后开始第一次倒计时。

**⑬ `if (navigated) return;`**
防重复跳转（见第 ⑤ 点）。

**⑭ `removeCallbacksAndMessages(null)`**
取消所有挂起的任务。`null` 表示"不管什么任务，全取消"。

**⑮ 三元表达式选目标页面**
已登录 → 首页；未登录 → 登录页。

**⑯ `finish()`**
关闭启动页。**必须写**，否则用户按返回键会回到启动页，然后又跳一次。

**⑰ `onDestroy` 里再取消一次**
双保险。万一 `goNext` 没走到（比如系统直接销毁），这里也能清理干净。

---

## 4.9 本篇小结

| 概念 | 要点 |
|---|---|
| Activity | 屏幕上的一屏内容，必须在 Manifest 登记 |
| `onCreate` | 创建时调用，`setContentView` 在这里 |
| `onResume` | 每次回到前台都调用，**刷新数据放这里** |
| `onDestroy` | 销毁时调用，**清理定时器放这里** |
| `finish()` | 关闭当前页面，回到上一页 |
| `Context` | 访问 App 资源的万能钥匙，Activity 本身就是 Context |
| 返回栈 | 后进先出，可用 Flag 清空 |
| 控件 ID 前缀 | `tv_` / `iv_` / `et_` / `btn_` / `rv_` |

### 自测

1. `onCreate` 和 `onResume` 的区别是什么？什么数据该放 `onResume` 刷新？
2. 为什么 `SplashActivity` 要有 `navigated` 这个变量？
3. 不写 `onDestroy` 里的 `removeCallbacks` 会有什么后果？
4. `this` 在 Activity 里为什么能当 `Context` 用？

---

下一篇：[05 XML 布局与常用控件](05-XML布局与常用控件.md) —— 界面到底是怎么"摆"出来的。
