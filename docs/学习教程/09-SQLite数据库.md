# 09 · SQLite 数据库

> **本篇你会学到**：数据存在哪、12 张表怎么设计、`SQLiteOpenHelper` 的三个回调、什么是事务、金额为什么用"分"。

---

## 9.1 数据库是什么（30 秒版）

**数据库 = 一堆 Excel 表格。**

```
表名：books（图书表）
┌────┬──────────┬──────────────────┬───────────┬──────┐
│ id │ store_id │ title            │ price_fen │ stock│   ← 列（字段）
├────┼──────────┼──────────────────┼───────────┼──────┤
│ 1  │ 1        │ 落在我身上的雨     │ 5900      │ 12   │   ← 行（记录）
│ 2  │ 3        │ 无论如何，潜水     │ 4980      │ 25   │
│ 3  │ 2        │ 爱的辛劳          │ 4900      │ 37   │
└────┴──────────┴──────────────────┴───────────┴──────┘
```

**五个必须懂的术语**：

| 术语 | 含义 | 项目里的例子 |
|---|---|---|
| **表（table）** | 一类数据的集合 | `books`、`orders` |
| **列 / 字段（column）** | 一类属性 | `title`、`price_fen` |
| **行 / 记录（row）** | 一条具体数据 | 一本书 |
| **主键（primary key）** | 唯一标识一行的字段 | `id` |
| **外键（foreign key）** | 指向另一张表的主键 | `books.store_id` → `stores.id` |

**SQLite 是什么？** 一个**嵌入式**数据库——不需要装服务器，数据就是一个文件，存在手机里。App 卸载时一起删掉。

> 项目里数据库文件叫 `book_mall.db`，在 App 的私有目录下，其他 App 访问不到。

---

## 9.2 三张核心表的关系

先看项目里最重要的三张表怎么连起来：

```
stores（书店）
┌────┬────────────────────┐
│ id │ name               │
├────┼────────────────────┤
│ 1  │ 西西弗书店          │
│ 2  │ 单向空间            │
└────┴────────────────────┘
         ↑
         │ store_id（外键）
         │
books（图书）
┌────┬──────────┬────────────────┬───────────┐
│ id │ store_id │ title          │ price_fen │
├────┼──────────┼────────────────┼───────────┤
│ 1  │ 1        │ 落在我身上的雨  │ 5900      │
│ 2  │ 3        │ 无论如何，潜水  │ 4980      │
└────┴──────────┴────────────────┴───────────┘
         ↑
         │ book_id（外键）
         │
order_items（订单明细）
┌────┬──────────┬────────────────┬──────────────┐
│ id │ order_id │ book_id        │ price_snapshot│
├────┼──────────┼────────────────┼──────────────┤
│ 1  │ 1        │ 1              │ 5900         │
└────┴──────────┴────────────────┴──────────────┘
```

**外键的作用**：`books.store_id = 1` 表示这本书属于 id 为 1 的书店。通过这个数字，两张表就关联起来了。

> **为什么不用"书店名"关联？** 因为名字可能重复、可能修改。用数字 ID 永远唯一、永远不变。

---

## 9.3 `SQLiteOpenHelper`：数据库的管理员

项目里所有建表逻辑都在 `BookDbHelper` 里，它继承 `SQLiteOpenHelper`。

**这个类只有三个方法需要你实现：**

| 方法 | 什么时候调用 | 干什么 |
|---|---|---|
| `onCreate(db)` | **数据库第一次被创建时**（装完 App 第一次打开） | 建表 + 灌初始数据 |
| `onUpgrade(db, old, new)` | **版本号变大时** | 升级表结构 |
| `onConfigure(db)` | 每次打开数据库前 | 配置（比如开启外键） |

### 构造方法

```java
public BookDbHelper(Context context) {
    super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    this.context = context.getApplicationContext();
}
```

`super(...)` 的四个参数：

| 参数 | 值 | 含义 |
|---|---|---|
| `context` | `getApplicationContext()` | 用 Application 的 Context，生命周期长，不会泄漏 |
| `name` | `"book_mall.db"` | 数据库文件名 |
| `factory` | `null` | 游标工厂，一般传 null 用默认的 |
| `version` | `3` | **数据库版本号**，升级时靠它判断 |

> **`DATABASE_VERSION = 3` 是什么意思？** 这是**我们自己维护的版本号**，不是 Android 版本。每次改表结构就 +1，`onUpgrade` 会收到新旧两个版本号。

### `onConfigure`：开启外键

```java
@Override
public void onConfigure(SQLiteDatabase db) {
    super.onConfigure(db);
    db.setForeignKeyConstraintsEnabled(true);
}
```

**为什么需要这行？** SQLite 默认**不检查外键**（为了兼容老版本）。不开的话，你可以往 `books` 表插一条 `store_id = 999` 的记录，哪怕根本没有 999 号书店。

开了之后，插入非法外键会直接报错——**数据完整性有保障**。

### `onCreate`：建表 + 灌数据

```java
@Override
public void onCreate(SQLiteDatabase db) {
    createStores(db);        // ① 先建 stores
    createCoreTables(db);    // ② 再建其他表
    createV3Tables(db);      // ③ 建 v3 新增的表
    seedDemoUser(db);        // ④ 插入演示账号
    JsonSeeder.seedAll(db, context);   // ⑤ 从 JSON 导入 20 本书
}
```

**① 为什么 stores 必须先建？**

因为 `books` 表里有 `FOREIGN KEY(store_id) REFERENCES stores(id)`——它引用了 `stores`。如果 `stores` 还不存在就建 `books`，SQLite 虽然不报错，但插数据时会出问题。

**这个顺序问题是真实踩过的坑**，代码里还留了注释提醒。

---

## 9.4 建表 SQL 逐字段精读

拿 `books` 表举例：

```sql
CREATE TABLE books (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id       INTEGER NOT NULL DEFAULT 1,
    category_id    INTEGER NOT NULL,
    title          TEXT    NOT NULL,
    author         TEXT    NOT NULL,
    publisher      TEXT    NOT NULL,
    price_fen      INTEGER NOT NULL,
    stock          INTEGER NOT NULL DEFAULT 0,
    cover_res_id   INTEGER NOT NULL,
    description    TEXT    NOT NULL,
    is_recommended INTEGER NOT NULL DEFAULT 0,
    sales          INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(category_id) REFERENCES categories(id),
    FOREIGN KEY(store_id)    REFERENCES stores(id)
);
```

### 字段类型只有四种

| SQLite 类型 | 含义 | Java 对应 |
|---|---|---|
| `INTEGER` | 整数 | `int` / `long` |
| `TEXT` | 文本 | `String` |
| `REAL` | 浮点数 | `double` / `float` |
| `BLOB` | 二进制 | `byte[]` |

> SQLite 是**动态类型**的，其实你写 `VARCHAR(50)` 也能跑（会被当成 TEXT）。但按标准写法来，可读性更好。

### 约束（constraint）

| 约束 | 含义 | 例子 |
|---|---|---|
| `PRIMARY KEY` | 主键，唯一标识一行 | `id` |
| `AUTOINCREMENT` | 自增，插入时不用管，自动 +1 | 插入第 21 本书自动是 21 |
| `NOT NULL` | 不能为空 | `title` 不能没有书名 |
| `UNIQUE` | 值不能重复 | `users.username` |
| `DEFAULT 值` | 不给值时的默认值 | `stock DEFAULT 0` |

### 关键字段解读

**`price_fen INTEGER`** —— 价格，单位"分"。

> **为什么不用 `REAL` 存元？** 因为浮点数有精度问题：
> ```java
> 0.1 + 0.2 == 0.30000000000000004   // true！
> ```
> 用分（整数）存，59.00 元存成 5900，加减乘除都是精确的。**这是金融类应用的标准做法。**

**`cover_res_id INTEGER`** —— 封面资源 ID。

存的是 `R.drawable.book_001` 这个整数（第 03 篇讲过，`R` 类里每个资源都是一个 int）。用的时候：

```java
cover.setImageResource(book.getCoverResId());
```

> **为什么不在数据库里存图片？** 图片几 MB，存进数据库会让数据库臃肿、查询变慢。**存路径或资源 ID，图片本身放文件系统或 res 目录**，这是标准做法。

**`is_recommended INTEGER`** —— 是否推荐。

SQLite 没有 boolean 类型，用 `0` / `1` 表示 false / true。

```java
// 写入
values.put("is_recommended", recommended ? 1 : 0);

// 读出
boolean recommended = c.getInt(11) == 1;
```

**`sales INTEGER`** —— 销量。用于列表排序（卖得好的排前面）。

---

## 9.5 命名规范：SQL 用下划线，Java 用驼峰

这是个**非常重要的细节**，很多人没注意：

| 位置 | 命名风格 | 例子 |
|---|---|---|
| **数据库列名** | `snake_case`（下划线） | `price_fen`、`store_id`、`created_at` |
| **Java 变量名** | `camelCase`（驼峰） | `priceFen`、`storeId`、`createdAt` |

**为什么要分开？**

- SQL 社区几百年的传统就是下划线（因为 SQL 关键字不区分大小写，`storeId` 和 `storeid` 容易混）
- Java 社区的传统是驼峰

**谁负责转换？** 看 `BookRepository.mapBook()`：

```java
private Book mapBook(Cursor c) {
    return new Book(
        c.getLong(0),    // id
        c.getLong(1),    // store_id   → storeId
        c.getLong(2),    // category_id → categoryId
        c.getString(3),  // c.name     → categoryName
        c.getString(4),  // title      → title
        ...
    );
}
```

**`mapBook` = "把数据库行映射成 Java 对象"**。`map` 在编程里就是"映射/转换"的意思。

> 所以你在项目里会看到：数据库里叫 `price_fen`，Java 里叫 `priceFen`，`Book` 类里叫 `getPriceFen()`。**三层名字不同，但是同一个东西**。看代码时不要被绕晕。

---

## 9.6 查询：`Cursor` 是什么

```java
public List<Store> getStores() {
    String sql = "SELECT s.id, s.name, ... FROM stores s ORDER BY s.id ASC";
    List<Store> result = new ArrayList<>();

    try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, null)) {
        while (c.moveToNext()) {
            result.add(mapStore(c));
        }
    }
    return result;
}
```

**`Cursor`（游标）= 一个指向查询结果的"指针"。**

```
查询结果：
┌────┬──────────┐
│ 1  │ 西西弗    │  ← moveToNext() 先指向这里
│ 2  │ 单向空间  │
│ 3  │ 先锋书店  │
└────┴──────────┘
```

**标准遍历套路**：

```java
while (c.moveToNext()) {    // 移动到下一行，有数据返回 true，到底返回 false
    c.getLong(0);           // 取第 0 列（按索引，从 0 开始）
    c.getString(1);         // 取第 1 列
}
```

**`try (Cursor c = ...)` 是什么？**

第 02 篇讲过的 `try-with-resources`。**Cursor 必须关闭**，否则会内存泄漏。用这个语法，出了大括号自动关。

### `query()` vs `rawQuery()`

| 方法 | 写法 | 什么时候用 |
|---|---|---|
| `query(table, columns, selection, ...)` | 参数化，不用写 `SELECT` | 简单查询 |
| `rawQuery(sql, args)` | 手写完整 SQL | 有 `JOIN`、子查询的复杂场景 |

项目里两种都用：

```java
// 简单查询用 query
dbHelper.getReadableDatabase().query(
        TABLE_CATEGORIES,                    // 表名
        new String[]{"id", "name"},          // 要哪些列
        null, null, null, null,              // where / group / having
        "id ASC");                           // 排序

// 复杂查询用 rawQuery（因为有 JOIN）
String sql = "SELECT b.id, ... FROM books b JOIN categories c ON c.id=b.category_id";
dbHelper.getReadableDatabase().rawQuery(sql, null);
```

### 为什么用 `?` 占位而不是拼字符串

```java
// ❌ 危险写法
String sql = "SELECT * FROM users WHERE username='" + name + "'";
// 如果 name 是  ' OR '1'='1
// SQL 就变成：SELECT * FROM users WHERE username='' OR '1'='1'   → 查出所有用户！

// ✅ 安全写法
db.query(TABLE_USERS, ..., "username=? AND password=?", new String[]{name, password}, ...);
```

这叫 **SQL 注入**。用 `?` 占位，参数会被当成**纯数据**而不是 SQL 语句的一部分。**项目里所有查询都是参数化的。**

---

## 9.7 事务：全成功或全失败

这是本项目的技术亮点，也是答辩必问的点。

### 先看问题

用户下单要做 5 件事：

1. 检查每本书的库存够不够
2. 插入一条订单记录
3. 插入 N 条订单明细
4. 扣减每本书的库存
5. 删除购物车里已购买的行

**如果第 4 步失败了会怎样？**

订单已经创建了，但库存没扣——**超卖**。用户买了 1 本，库存还是原数，下一个人还能买。

**事务就是解决这个的**：把这 5 步打包成"一个不可分割的整体"，要么全成功，要么全回滚到初始状态。

### 事务的三段式

```java
SQLiteDatabase db = dbHelper.getWritableDatabase();

db.beginTransaction();          // ① 开始事务
try {
    // ... 一堆操作 ...

    db.setTransactionSuccessful();   // ② 标记成功
} finally {
    db.endTransaction();             // ③ 结束事务
}
```

**关键点：**

- 只有调用了 `setTransactionSuccessful()`，`endTransaction()` 才会**真正提交**
- 如果中途抛异常没走到 ②，`endTransaction()` 会**自动回滚**所有操作
- ③ 必须写在 `finally` 里，保证一定会执行

> **这个"标记成功"的设计很反直觉**，但很重要：默认是"失败"状态，你主动说"我成功了"才提交。这样万一中间抛异常，不用写一堆 `catch` 就能自动回滚。

### 完整精读：`createOrder`

```java
public long createOrder(long userId, List<CartItem> selectedItems, ReceiverInfo receiverInfo,
                        int deliveryFeeFen, int discountFen, String couponTitle, long couponId) {

    if (selectedItems == null || selectedItems.isEmpty())
        throw new IllegalStateException("请选择要结算的图书");
    validateReceiver(receiverInfo);

    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.beginTransaction();                              // ① 开启事务
    try {
        // ---- 第一轮：只算钱，不改数据 ----
        int goodsFen = 0;
        for (CartItem item : selectedItems) {
            Book current = getBookInTransaction(db, item.getBookId());
            if (current == null)
                throw new IllegalStateException("图书已下架：" + item.getTitle());
            if (item.getQuantity() <= 0 || item.getQuantity() > current.getStock())
                throw new IllegalStateException("库存不足：" + item.getTitle()
                        + "（仅剩 " + current.getStock() + " 本）");
            goodsFen += current.getPriceFen() * item.getQuantity();   // ② 累加金额
        }

        int safeDiscount = Math.max(0, Math.min(discountFen, goodsFen));  // ③ 优惠不能超过商品总额
        int totalFen = Math.max(0, goodsFen + deliveryFeeFen - safeDiscount);

        // ---- 写订单主表 ----
        ContentValues order = new ContentValues();
        order.put("user_id", userId);
        order.put("order_no", generateOrderNo());
        order.put("total_amount_fen", totalFen);
        order.put("delivery_fee_fen", deliveryFeeFen);
        order.put("discount_fen", safeDiscount);
        order.put("coupon_title", couponTitle);
        order.put("status", "待发货");
        order.put("receiver_name", receiverInfo.getName().trim());
        order.put("receiver_phone", receiverInfo.getPhone().trim());
        order.put("receiver_address", receiverInfo.getAddress().trim());
        order.put("created_at", System.currentTimeMillis());
        long orderId = db.insertOrThrow(TABLE_ORDERS, null, order);   // ④ 拿到自增 ID

        // ---- 逐条写明细、扣库存、清购物车 ----
        for (CartItem item : selectedItems) {
            Book current = getBookInTransaction(db, item.getBookId());

            ContentValues detail = new ContentValues();
            detail.put("order_id", orderId);
            detail.put("book_id", current.getId());
            detail.put("title_snapshot", current.getTitle());          // ⑤ 书名快照
            detail.put("price_snapshot_fen", current.getPriceFen());   // ⑤ 价格快照
            detail.put("quantity", item.getQuantity());
            detail.put("cover_res_id", current.getCoverResId());
            db.insertOrThrow(TABLE_ORDER_ITEMS, null, detail);

            ContentValues stock = new ContentValues();
            stock.put("stock", current.getStock() - item.getQuantity());  // ⑥ 扣库存
            db.update(TABLE_BOOKS, stock, "id=?", new String[]{String.valueOf(current.getId())});

            db.delete(TABLE_CART, "user_id=? AND book_id=?",               // ⑦ 清购物车
                    new String[]{String.valueOf(userId), String.valueOf(item.getBookId())});
        }

        // ---- 标记优惠券已使用 ----
        if (couponId > 0) {
            ContentValues used = new ContentValues();
            used.put("used", 1);
            db.update(TABLE_USER_COUPONS, used, "user_id=? AND coupon_id=?",
                    new String[]{String.valueOf(userId), String.valueOf(couponId)});
        }

        db.setTransactionSuccessful();      // ⑧ 全部成功，标记提交
        return orderId;

    } finally {
        db.endTransaction();                // ⑨ 结束（没标记成功就回滚）
    }
}
```

### 七个设计要点

**① 事务包住所有写操作**
从 `beginTransaction()` 到 `endTransaction()` 之间，任何一步抛异常，前面做的全部作废。

**② 第一轮循环只算钱，不写数据**

注意是**两轮循环**：
- 第一轮：检查库存 + 累加金额
- 第二轮：写明细 + 扣库存 + 清购物车

**为什么分两轮？** 因为要先确认"全部商品都够"，才动手写。如果边检查边写，第一本书扣了库存，第二本书发现不够——虽然事务会回滚，但逻辑上不干净，而且错误信息会变成"扣了一半"。

**③ 优惠不能超过商品总额**

```java
int safeDiscount = Math.max(0, Math.min(discountFen, goodsFen));
```

- `Math.min(discountFen, goodsFen)` —— 优惠最多等于商品总额（不能倒贴钱）
- `Math.max(0, ...)` —— 优惠不能是负数

**④ `insertOrThrow` 返回自增 ID**

`insertOrThrow()` 插入成功会返回新行的 `id`。后面写明细要用这个 `orderId` 做外键。

**⑤ 快照字段：为什么要存书名和价格？**

因为书可能会**改价**或**下架**。如果订单明细只存 `book_id`，用户半年后看订单，看到的是"现在的价格"，而不是"当时买的价格"。

存快照（snapshot）就解决了：

```java
detail.put("title_snapshot", current.getTitle());
detail.put("price_snapshot_fen", current.getPriceFen());
```

**这是电商系统的标准设计**，答辩时能讲出来会加分。

**⑥ 扣库存**

```java
stock.put("stock", current.getStock() - item.getQuantity());
```

注意用的是**重新查出来的 `current.getStock()`**，不是购物车里记的库存。因为购物车可能是几分钟前加载的，库存可能已经变了。

**⑦ 只删已购买的行**

```java
db.delete(TABLE_CART, "user_id=? AND book_id=?", ...);
```

只删这次结算的商品，购物车里没勾选的要保留。

**⑧⑨ 提交与回滚**

见前面的三段式说明。

---

## 9.8 版本升级：`onUpgrade`

假设已经装了旧版本（数据库版本 2），现在装了新版本（版本 3），`onUpgrade` 会被调用：

```java
@Override
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 3) {
        createV3Tables(db);                                    // ① 建新表
        addColumnIfMissing(db, TABLE_BOOKS, "store_id", "INTEGER NOT NULL DEFAULT 1");  // ② 加新列
        addColumnIfMissing(db, TABLE_BOOKS, "sales", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, TABLE_ORDERS, "delivery_fee_fen", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, TABLE_ORDERS, "discount_fen", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, TABLE_ORDERS, "coupon_title", "TEXT");
        JsonSeeder.refreshCatalog(db, context);                // ③ 刷新目录数据
    }
}
```

### 为什么不用"删表重建"

**最省事的升级方式是**：

```java
db.execSQL("DROP TABLE books");
onCreate(db);   // 重新建
```

**但这会丢失所有用户数据**——收藏、购物车、订单全没了。用户更新个 App，购物车清空了，肯定差评。

**正确做法是"增量迁移"**：只加新东西，不动老数据。

```java
private void addColumnIfMissing(SQLiteDatabase db, String table, String column, String definition) {
    try {
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    } catch (Exception e) {
        Log.w(TAG, "列 " + column + " 已存在，跳过", e);   // 已经加过就忽略
    }
}
```

> **命名解读**：`addColumnIfMissing` = "如果缺了就加列"。`IfMissing` 后缀表示"缺失时才执行"，这是很常见的命名模式。

**③ `refreshCatalog` 的巧妙之处**

新的目录数据（书店、图书）要更新，但**不能删了重建**——因为 `favorites`、`cart_items` 表通过外键引用了 `books`，删 `books` 会级联删除用户的收藏和购物车。

所以 `JsonSeeder` 用的是 **update-or-insert**：

```java
private static void write(SQLiteDatabase db, String table, ContentValues values, boolean updateFirst) {
    if (updateFirst) {
        long id = values.getAsLong("id");
        int rows = db.update(table, values, "id=?", new String[]{String.valueOf(id)});
        if (rows > 0) return;      // 更新成功，直接返回
    }
    db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);  // 没有才插入
}
```

**先试着更新，更新不到才插入。** 这样既刷新了数据，又不会触发级联删除。

---

## 9.9 常见坑清单

| 坑 | 现象 | 解决 |
|---|---|---|
| 忘了关 Cursor | 内存泄漏、报 `CursorWindow` 错误 | 用 `try-with-resources` |
| 忘了 `setTransactionSuccessful()` | 数据没保存，但也不报错 | 事务最后一行必须调 |
| `endTransaction()` 没放 `finally` | 异常时事务卡住，后续操作全阻塞 | 必须放 `finally` |
| 外键约束没开 | 插入非法数据不报错，数据变脏 | `onConfigure` 里 `setForeignKeyConstraintsEnabled(true)` |
| 主线程查大表 | 界面卡顿 | 数据量小可接受，大了要开子线程 |
| 用 `REAL` 存金额 | 0.1+0.2 算错 | 用 `INTEGER` 存"分" |
| `onUpgrade` 删表重建 | 用户数据丢失 | 增量迁移 |

---

## 9.10 本篇小结

| 概念 | 要点 |
|---|---|
| SQLite | 嵌入式数据库，数据是一个文件 |
| 主键 / 外键 | 唯一标识 / 指向别的表 |
| `SQLiteOpenHelper` | 三个回调：`onCreate` / `onUpgrade` / `onConfigure` |
| `Cursor` | 结果集指针，`moveToNext()` 遍历，必须关闭 |
| 参数化查询 | 用 `?` 占位，防 SQL 注入 |
| 事务 | `begin` → `setSuccessful` → `end`，失败自动回滚 |
| 金额用分 | 避免浮点误差 |
| 快照字段 | 订单明细存当时的书名和价格 |
| 增量迁移 | 加表加列，不删表 |

### 自测

1. `onCreate` 和 `onUpgrade` 分别在什么时候被调用？
2. 为什么 `books` 表要在 `stores` 表之后创建？
3. 事务里 `setTransactionSuccessful()` 不调用会怎样？
4. 为什么订单明细要存 `title_snapshot` 而不是只存 `book_id`？
5. 数据库里叫 `price_fen`，Java 里叫 `priceFen`，中间是谁做的转换？
6. 为什么升级数据库不能直接 `DROP TABLE` 重建？

---

下一篇：[10 JSON 解析与数据初始化](10-JSON解析与数据初始化.md) —— 首次启动的 20 本书到底从哪来。
