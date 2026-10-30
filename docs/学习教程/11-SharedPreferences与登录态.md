# 11 · SharedPreferences 与登录态

> **本篇你会学到**：什么是键值存储、和数据库有什么区别、登录状态怎么"记住"、`apply()` 和 `commit()` 的差别。

---

## 11.1 先分清两种存储

项目里有两套存储，用途完全不同：

| | SharedPreferences | SQLite |
|---|---|---|
| 存什么 | **少量配置**（几个键值对） | **大量结构化数据**（成千上万行） |
| 数据形状 | 键值对，像 `Map<String, Object>` | 表格，有行有列 |
| 典型用途 | 登录状态、是否首次启动、用户偏好 | 用户表、图书表、订单表 |
| 项目里的类 | `SessionManager` | `BookDbHelper` / `BookRepository` |
| 数据量级 | 几十条以内 | 无上限 |

**一句话**：**配置用 SharedPreferences，业务数据用 SQLite。**

如果拿 SharedPreferences 存 1000 本书，你会写 1000 个键，还要自己处理"查第 3 本"这种事——这就是拿锤子拧螺丝。

---

## 11.2 SharedPreferences 长什么样

它本质上是一个 **XML 文件**，存在 App 私有目录：

```xml
<!-- /data/data/com.yzc.bookmall/shared_prefs/book_mall_session.xml -->
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <long name="user_id" value="3" />
    <string name="username">demo</string>
</map>
```

**就是一堆键值对。** 你不需要手动读写这个文件，用 API 就行。

### 支持的数据类型

| 方法 | 类型 |
|---|---|
| `putString` / `getString` | 字符串 |
| `putInt` / `getInt` | 整数 |
| `putLong` / `getLong` | 长整数 |
| `putFloat` / `getFloat` | 浮点数 |
| `putBoolean` / `getBoolean` | 布尔 |
| `putStringSet` / `getStringSet` | 字符串集合 |

**不支持存对象。** 要存对象得先转成 JSON 字符串。

---

## 11.3 完整精读：`SessionManager`

整个类只有 18 行，但包含了 SharedPreferences 的全部常用操作。

```java
public class SessionManager {
    private static final String PREF_NAME = "book_mall_session";   // ①
    private static final String KEY_USER_ID = "user_id";           // ②
    private static final String KEY_USERNAME = "username";
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
    public long getUserId() { return preferences.getLong(KEY_USER_ID, -1L); }   // ⑦
    public String getUsername() { return preferences.getString(KEY_USERNAME, ""); }

    public void clearSession() { preferences.edit().clear().apply(); }   // ⑧
}
```

### ① `PREF_NAME = "book_mall_session"`

**文件名**。它会变成 `shared_prefs/book_mall_session.xml`。

> **命名解读**：`book_mall_session` = 项目名 + session（会话）。一个 App 可以有多个 SharedPreferences 文件，按用途分开（比如 `user_settings`、`app_config`），不要全塞一个。

### ② `KEY_USER_ID` / `KEY_USERNAME`

**键名**。`KEY_` 前缀是社区惯例，一眼看出这是 map 的 key 而不是普通常量。

值 `"user_id"` 用的是**下划线风格**，和数据库列名一致——因为它们在 XML 里也是"字段名"。

### ③ `private final SharedPreferences preferences`

- `private` —— 只有这个类能用，外部必须通过 `getUserId()` 等方法访问
- `final` —— 构造完就不能换对象
- 复数 `preferences` —— 因为里面存了多条键值对

### ④ 构造方法：拿实例

```java
preferences = context.getApplicationContext()
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
```

- `getApplicationContext()` —— 用 Application 的 Context（生命周期长，避免泄漏）
- `getSharedPreferences(文件名, 模式)` —— 打开（不存在则创建）这个配置文件
- `MODE_PRIVATE` —— 私有模式，只有本 App 能读写

> **另外两种模式已废弃**：`MODE_WORLD_READABLE` 和 `MODE_WORLD_WRITEABLE` 因为安全问题从 Android 7.0 起会抛异常。**永远用 `MODE_PRIVATE`。**

### ⑤ 保存：`edit()` 链式调用

```java
preferences.edit()
        .putLong(KEY_USER_ID, userId)
        .putString(KEY_USERNAME, username)
        .apply();
```

拆开看：

| 步骤 | 作用 |
|---|---|
| `edit()` | 拿到一个 `Editor`（编辑器），开启一次修改 |
| `.putLong(...)` | 写入一个 long |
| `.putString(...)` | 写入一个 String |
| `.apply()` | **提交** |

**为什么必须 `edit()`？** 因为 SharedPreferences 对象本身是只读的，所有写操作都要通过 `Editor`。

**链式调用**能一直 `.` 下去，是因为每个 `put` 方法都返回 `Editor` 自己。

### ⑥ `isLoggedIn()`：一个巧妙的判断

```java
public boolean isLoggedIn() { return getUserId() > 0; }
```

**为什么不用单独存一个 `is_logged_in` 布尔值？**

因为**会不一致**。比如：

```java
preferences.edit().putBoolean("is_logged_in", true).apply();
// 忘了存 user_id
// 结果：isLoggedIn() 返回 true，但 getUserId() 返回 -1，后面全崩
```

**只存一份数据，用推导的方式判断**，就不会出现这种矛盾。这是"**单一数据源**"原则。

### ⑦ 默认值：`-1L` 和 `""`

```java
preferences.getLong(KEY_USER_ID, -1L)
```

第二个参数是**默认值**——键不存在时返回它。

**为什么用 `-1` 而不是 `0`？**

因为 SQLite 的自增 ID 从 `1` 开始，`0` 不是合法 ID。用 `-1` 表示"无效 ID"，语义清晰：

```java
if (userId < 0) { /* 未登录 */ }
```

如果默认值是 `0`，你写 `if (userId == 0)` 也能工作，但读代码的人会疑惑"0 是什么特殊值吗"。

> **`-1L` 的 `L` 是 long 字面量后缀。** 因为 `getLong` 要求 long 类型，写 `-1` 会被当成 int（虽然能自动转，但显式写 `L` 更规范）。

### ⑧ 清空：`clear()`

```java
preferences.edit().clear().apply();
```

`clear()` 会删掉这个文件里的**所有**键值对。注意是"这个文件"，不会影响别的 SharedPreferences 文件。

---

## 11.4 `apply()` 和 `commit()` 的区别

这是面试常问的：

| | `apply()` | `commit()` |
|---|---|---|
| 是否阻塞 | **不阻塞**（异步写磁盘） | **阻塞**（同步写，等写完才返回） |
| 返回值 | 无 | `boolean`（是否成功） |
| 适用场景 | **绝大多数情况** | 需要立刻知道结果时 |

**项目全部用 `apply()`。** 因为登录状态写入后不需要马上读磁盘验证，异步写就行，不会卡界面。

> **`apply()` 有个细节**：它保证**同一进程内**后续的 `getXxx()` 一定能读到新值（内存里已经更新了），只是磁盘写入是异步的。所以不会出现"刚存完就读不到"的问题。

---

## 11.5 登录流程：从输入密码到记住状态

跟一遍完整链路：

```
【第 1 步】LoginActivity 用户点"登录"
        ↓
【第 2 步】校验输入
        if (username.isEmpty()) { usernameInput.setError(...); return; }
        if (password.isEmpty()) { passwordInput.setError(...); return; }
        ↓
【第 3 步】查数据库验证账号密码
        long userId = repository.authenticate(username, password);
        if (userId < 0) { passwordInput.setError("用户名或密码错误"); return; }
        ↓
【第 4 步】★ 保存会话到 SharedPreferences
        sessionManager.saveSession(userId, repository.getUsername(userId));
        ↓
【第 5 步】跳转首页并清空返回栈
        openMain();
```

**第 4 步的代码：**

```java
private void login() {
    String username = usernameInput.getText().toString().trim();
    String password = passwordInput.getText().toString();

    if (username.isEmpty()) { usernameInput.setError(getString(R.string.error_username_required)); return; }
    if (password.isEmpty()) { passwordInput.setError(getString(R.string.error_password_required)); return; }

    long userId = repository.authenticate(username, password);
    if (userId < 0) { passwordInput.setError(getString(R.string.error_login_failed)); return; }

    sessionManager.saveSession(userId, repository.getUsername(userId));   // ★
    openMain();
}
```

**注意 `authenticate` 返回的是 `long`（用户 ID），不是 `boolean`。**

为什么？因为后面要用这个 ID 去查"我的订单""我的收藏"。返回 ID 一步到位，不用再查一次。

```java
public long authenticate(String username, String password) {
    try (Cursor c = dbHelper.getReadableDatabase().query(
            TABLE_USERS, new String[]{"id"},
            "username=? AND password=?",
            new String[]{username.trim(), password}, null, null, null)) {
        return c.moveToFirst() ? c.getLong(0) : -1;
    }
}
```

- 查到 → 返回 `id`
- 查不到 → 返回 `-1`

**用 `-1` 当"失败"的标志**，和前面 `getUserId` 的默认值呼应上了。

---

## 11.6 登录态在哪被使用

存进去之后，有四个地方会读：

### ① 启动页：决定去首页还是登录页

```java
// SplashActivity
SessionManager session = new SessionManager(this);
Intent intent = new Intent(this, session.isLoggedIn()
        ? MainActivity.class : LoginActivity.class);
```

### ② 登录页：已登录就直接进首页

```java
// LoginActivity.onCreate
if (sessionManager.isLoggedIn()) {
    openMain();
    return;    // 注意这个 return，不再执行 setContentView
}
```

> **这个 `return` 很重要**。不加的话，会先跳首页，然后又执行下面的 `setContentView` 显示登录界面，出现"闪一下"。

### ③ 各处需要用户 ID 的地方

```java
// 购物车
List<CartItem> items = repository.getCartItems(session.getUserId());

// 收藏
repository.setFavorite(session.getUserId(), bookId, next);

// 订单
List<Order> orders = repository.getOrders(session.getUserId());
```

**所有"我的"数据都靠这个 ID 隔离。** 换个账号登录，看到的就是另一个人的购物车。

### ④ 登录保护：没登录不让进

```java
// CartFragment
if (!session.isLoggedIn()) {
    startActivity(new Intent(requireContext(), LoginActivity.class)
            .putExtra("redirect_tab", "cart"));
    return;
}
```

---

## 11.7 退出登录做了什么

```java
// ProfileFragment
private void confirmLogout() {
    new AlertDialog.Builder(requireContext())
            .setTitle(R.string.profile_logout)
            .setMessage(R.string.profile_logout_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                session.clearSession();                     // ① 清掉登录态
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                              | Intent.FLAG_ACTIVITY_NEW_TASK);   // ② 清空返回栈
                startActivity(intent);
                requireActivity().finish();                 // ③ 关掉 MainActivity
            })
            .show();
}
```

**三个动作缺一不可：**

| 动作 | 不做会怎样 |
|---|---|
| ① `clearSession()` | 下次启动还是登录状态 |
| ② `CLEAR_TASK` | 按返回键能回到已登录的界面 |
| ③ `finish()` | MainActivity 还在栈里，内存泄漏 |

**注意：退出登录不会删除数据。** 购物车、订单、收藏都还在数据库里，下次登录还能看到。这是正确的设计——"退出"只是退出身份，不是删数据。

---

## 11.8 一个重要的安全提醒

**SharedPreferences 不能存密码。**

项目里 `users` 表的密码是**明文**存的：

```sql
CREATE TABLE users (..., password TEXT NOT NULL, ...)
```

这是课程项目的简化做法，**真实项目绝对不能这样**：

| 问题 | 正确做法 |
|---|---|
| 明文存密码 | 存**哈希值**（bcrypt / Argon2），连数据库管理员也看不到原密码 |
| SharedPreferences 存 token | 用 `EncryptedSharedPreferences` 或 Android Keystore 加密 |
| 明文传输 | HTTPS + 证书校验 |

**答辩时如果被问到，可以这样回答**：

> "课程项目为了演示方便用了明文，真实项目应该用 bcrypt 加盐哈希存储，登录时比对哈希值而不是明文。这一点我在设计文档里也标注了。"

**主动指出自己项目的局限，比被问出来好。**

---

## 11.9 本篇小结

| 概念 | 要点 |
|---|---|
| SharedPreferences | 键值对存储，本质是一个 XML 文件 |
| 与 SQLite 的分工 | 配置 vs 业务数据 |
| `edit()` | 开启一次修改，必须调 |
| `apply()` | 异步提交，不阻塞，项目全用它 |
| 默认值 | `-1L` 表示无效 ID，`""` 表示空字符串 |
| 单一数据源 | 只存 `user_id`，`isLoggedIn()` 由它推导 |
| 退出登录 | 清会话 + 清返回栈 + finish |
| 安全 | 不能存密码明文 |

### 自测

1. 什么数据该放 SharedPreferences，什么该放 SQLite？
2. `apply()` 和 `commit()` 有什么区别？项目为什么用 `apply()`？
3. `getUserId()` 的默认值为什么是 `-1` 而不是 `0`？
4. 为什么 `isLoggedIn()` 不单独存一个布尔值？
5. 退出登录时如果忘了加 `CLEAR_TASK` 会怎样？

---

下一篇：[12 分层架构与命名规范](12-分层架构与命名规范.md) —— 为什么要有 Repository，每个变量名背后的思考。
