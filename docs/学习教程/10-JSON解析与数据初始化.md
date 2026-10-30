# 10 · JSON 解析与数据初始化

> **本篇你会学到**：JSON 是什么、怎么从 `assets` 读文件、怎么用 `org.json` 解析、字符串怎么变成资源 ID、解析失败怎么兜底。

---

## 10.1 问题：20 本书的数据从哪来？

App 第一次安装打开，首页就有 5 家书店、20 本书。这些数据不可能凭空出现。

**三种做法：**

| 做法 | 优点 | 缺点 |
|---|---|---|
| ① 在 Java 里硬编码 `insertBook(...)` 调 20 次 | 简单直接 | 代码巨长，改数据要改代码重新编译 |
| ② 从服务器下载 | 数据可动态更新 | **需要联网**，本项目是离线应用 |
| ③ 放在本地 JSON 文件里，启动时解析入库 | 数据与代码分离，改 JSON 不用改代码 | 需要写解析逻辑 |

**项目用的是 ③。**

数据文件在 `app/src/main/assets/seed/`：

```
seed/
├── stores.json       5 家书店
├── categories.json   5 个分类
├── books.json        20 本书
└── coupons.json      4 张优惠券
```

---

## 10.2 JSON 是什么

**JSON = JavaScript Object Notation**，一种**纯文本的数据格式**。长得像这样：

```json
{
  "id": 1,
  "name": "西西弗书店·万象城店",
  "rating": 4.8,
  "deliveryFeeFen": 500,
  "isOpen": true
}
```

**六条规则（背下来）：**

| 语法 | 含义 | 例子 |
|---|---|---|
| `{ }` | 对象（一堆键值对） | `{"name": "张三"}` |
| `[ ]` | 数组（一串值） | `[1, 2, 3]` |
| `"key": value` | 键值对，键**必须加双引号** | `"age": 18` |
| `,` | 分隔 | `{"a":1, "b":2}` |
| 值类型 | 字符串/数字/布尔/null/对象/数组 | `"abc"`、`18`、`true`、`null` |
| 不能有注释 | JSON 不支持 `//` 注释 | — |

### 项目里的 `stores.json` 长这样

```json
[
  {
    "id": 1,
    "name": "西西弗书店·万象城店",
    "slogan": "参与构成本地精神生活",
    "description": "1993 年创立于贵州遵义，以「图书零售 + 咖啡 + 文创」的复合业态著称……",
    "cover": "store_1",
    "rating": 4.8,
    "address": "贵阳市观山湖区万象城 L3-08",
    "businessHours": "10:00 - 22:00",
    "notice": "本周六 14:00 举办《落在我身上的雨》作者分享会，欢迎到店。",
    "deliveryFeeFen": 500,
    "minOrderFen": 3000
  },
  { ... 另外 4 家 ... }
]
```

**最外层是 `[ ]` 数组**，里面每个 `{ }` 是一家书店。

> **命名注意**：JSON 里的键用的是 **camelCase**（`deliveryFeeFen`），不是数据库的下划线风格。因为这份 JSON 是给 Java 代码读的，用 Java 的风格更自然。**数据库列名和 JSON 键名不要求一致**，转换由代码负责。

---

## 10.3 第一步：从 assets 读出文本

`assets` 里的文件不能直接 `File` 读取，要用 `AssetManager`：

```java
private static String readAsset(Context context, String name) throws Exception {
    try (InputStream in = context.getAssets().open("seed/" + name);
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

        byte[] buffer = new byte[4096];      // ① 一次搬 4KB
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);      // ② 搬多少写多少
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);   // ③ 字节转字符串
    }
}
```

**逐句解释：**

**① `byte[] buffer = new byte[4096]`**

文件不能一次性全读进来（不知道多大），要"一勺一勺舀"。4096 字节（4KB）是常见的缓冲区大小——太小要舀很多次，太大浪费内存。

**② `while ((read = in.read(buffer)) > 0)`**

这是标准写法：
- `in.read(buffer)` 读一批，返回读到的字节数
- 读到文件末尾返回 `-1`，循环结束
- `out.write(buffer, 0, read)` 只写实际读到的部分（最后一批可能不满 4096）

**③ `new String(bytes, StandardCharsets.UTF_8)`**

字节数组要指定编码才能变成字符串。**必须写 `UTF_8`**，否则中文会变乱码。

> **不指定编码会怎样？** 用系统默认编码（Windows 中文环境是 GBK），而文件是 UTF-8，结果就是"锟斤拷"那种乱码。

**`try (...)` 两个资源**

`InputStream` 和 `ByteArrayOutputStream` 都实现了 `AutoCloseable`，可以写在同一个 `try` 里，用 `;` 分隔，退出时**倒序自动关闭**。

---

## 10.4 第二步：解析 JSON

Android 内置了 `org.json` 库（不需要额外依赖）。

### 两个核心类

| 类 | 对应 JSON | 怎么拿 |
|---|---|---|
| `JSONArray` | `[ ... ]` | `new JSONArray(字符串)` |
| `JSONObject` | `{ ... }` | `array.getJSONObject(i)` |

### 标准解析套路

```java
JSONArray array = new JSONArray(readAsset(context, "stores.json"));   // ①

for (int i = 0; i < array.length(); i++) {                            // ②
    JSONObject o = array.getJSONObject(i);                            // ③

    int id = o.getInt("id");                                          // ④
    String name = o.getString("name");
    double rating = o.getDouble("rating");
    int fee = o.getInt("deliveryFeeFen");
    int sales = o.optInt("sales", 0);                                 // ⑤
}
```

**① `new JSONArray(字符串)`**
把文本解析成数组对象。**如果 JSON 格式有错（少个逗号、多个引号），这里会抛 `JSONException`。**

**② `array.length()`**
数组有几个元素。

**③ `array.getJSONObject(i)`
取第 i 个对象。

**④ `o.getInt("id")`**
按键取值，按类型取。取不到会抛异常。

**⑤ `o.optInt("sales", 0)`**
`opt` 开头的方法是"**可选**"的：键不存在时返回默认值，**不抛异常**。

| 方法 | 键不存在时 |
|---|---|
| `getInt("x")` | 抛 `JSONException` |
| `optInt("x", 0)` | 返回 `0` |

**什么时候用 `opt`？** 当某个字段是"可选的"时。项目里 `sales` 用了 `optInt("sales", 0)`，因为就算 JSON 里没写销量，也不该让整个导入失败。

---

## 10.5 完整精读：`JsonSeeder.seedStores`

```java
private static void seedStores(SQLiteDatabase db, Context context, boolean updateFirst) throws Exception {
    JSONArray array = new JSONArray(readAsset(context, "stores.json"));   // ①
    long now = System.currentTimeMillis();

    for (int i = 0; i < array.length(); i++) {
        JSONObject o = array.getJSONObject(i);

        ContentValues v = new ContentValues();          // ②
        v.put("id", o.getInt("id"));
        v.put("name", o.getString("name"));
        v.put("slogan", o.getString("slogan"));
        v.put("description", o.getString("description"));
        v.put("cover_res_id", drawableId(context, o.getString("cover")));   // ③
        v.put("rating", o.getDouble("rating"));
        v.put("address", o.getString("address"));
        v.put("business_hours", o.getString("businessHours"));              // ④
        v.put("notice", o.getString("notice"));
        v.put("delivery_fee_fen", o.getInt("deliveryFeeFen"));              // ④
        v.put("min_order_fen", o.getInt("minOrderFen"));                    // ④
        v.put("created_at", now);

        write(db, BookDbHelper.TABLE_STORES, v, updateFirst);               // ⑤
    }
}
```

**① 先读文件再解析**
`readAsset` 返回字符串，`new JSONArray(...)` 解析。两步分开写，出错时能分清是"读文件失败"还是"格式错误"。

**② `ContentValues` 是什么？**

它是 Android 提供的"**一行数据的容器**"，本质是个 `Map<String, Object>`：

```java
ContentValues v = new ContentValues();
v.put("name", "西西弗");      // 键 = 数据库列名
v.put("rating", 4.8);
```

然后交给 `db.insert(table, null, v)` 或 `db.update(...)`。

> **为什么不用拼 SQL？** 因为 `ContentValues` 会自动处理类型转换和转义，避免 SQL 注入和引号问题。**这是 Android 推荐写法。**

**③ `drawableId(context, "store_1")`**

JSON 里存的是字符串 `"store_1"`，但数据库要存的是 `R.drawable.store_1` 这个整数。怎么转？

```java
private static int drawableId(Context context, String name) {
    int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    return id != 0 ? id : R.drawable.book_cover_1;    // 找不到就兜底
}
```

`getIdentifier(名字, 类型, 包名)` —— **用字符串动态查资源 ID**。

- 名字：`"store_1"`
- 类型：`"drawable"`
- 包名：`"com.yzc.bookmall"`

返回 `R.drawable.store_1` 对应的整数；**找不到返回 0**。

> **为什么不用 `getIdentifier`？** 它靠反射查表，比较慢。但这里只在**首次启动导入数据时**执行 25 次，完全可以接受。**性能和场景要匹配**——如果在列表滚动时调它，就不可取了。

**④ 键名转换：`businessHours` → `business_hours`**

看这三行：

```java
v.put("business_hours", o.getString("businessHours"));   // JSON 驼峰 → 数据库下划线
v.put("delivery_fee_fen", o.getInt("deliveryFeeFen"));
v.put("min_order_fen", o.getInt("minOrderFen"));
```

**左边是数据库列名（下划线），右边是 JSON 键名（驼峰）。** 这个转换是手写的，没有魔法。

> 新手常犯的错：两边写成一样的，结果 `v.put("businessHours", ...)` 找不到对应列，插入失败或该列为空。**数据库列名必须和建表语句完全一致。**

**⑤ `write(db, table, v, updateFirst)`**

这个方法在第 09 篇讲过，是"更新优先，插入兜底"：

```java
private static void write(SQLiteDatabase db, String table, ContentValues values, boolean updateFirst) {
    if (updateFirst) {
        long id = values.getAsLong("id");
        int rows = db.update(table, values, "id=?", new String[]{String.valueOf(id)});
        if (rows > 0) return;
    }
    db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
}
```

`updateFirst` 这个布尔参数决定了策略：

| 场景 | `updateFirst` | 行为 |
|---|---|---|
| 首次建库 `seedAll` | `false` | 直接插入 |
| 版本升级 `refreshCatalog` | `true` | 先更新，更新不到才插入 |

---

## 10.6 异常兜底：解析失败怎么办

```java
public static void seedAll(SQLiteDatabase db, Context context) {
    try {
        seedStores(db, context, false);
        seedCategories(db, context, false);
        seedBooks(db, context, false);
        seedCoupons(db, context, false);
    } catch (Exception e) {
        Log.e(TAG, "种子数据解析失败，改用最小数据集", e);
        fallback(db, context);      // ← 兜底
    }
}
```

**如果 JSON 写错了（少个逗号），会发生什么？**

没有 `try-catch` 的话：`JSONException` 一路抛到 `onCreate`，数据库创建失败，**App 一打开就崩，而且是永远崩**（因为每次启动都会重新尝试建库）。

有了兜底：

```java
private static void fallback(SQLiteDatabase db, Context context) {
    // 插 1 家书店、1 个分类、1 本书
    ContentValues store = new ContentValues();
    store.put("id", 1);
    store.put("name", "书墨自营店");
    ...
}
```

App 能正常打开，首页显示 1 家店、1 本书。**虽然数据不全，但至少不是白屏或崩溃。**

> **`Log.e(TAG, ...)` 是什么？** `Log` 是 Android 的日志工具，`e` = error（错误级别）。`TAG` 是日志标签，方便在 Logcat 里过滤。第 14 篇会讲怎么看日志。

**这就是"异常处理"的意义**：不是让错误消失，而是**让错误的影响可控**。

---

## 10.7 为什么首次启动才有数据

`JsonSeeder.seedAll` 只在 `BookDbHelper.onCreate` 里调用：

```java
@Override
public void onCreate(SQLiteDatabase db) {
    createStores(db);
    createCoreTables(db);
    createV3Tables(db);
    seedDemoUser(db);
    JsonSeeder.seedAll(db, context);   // ← 只在这里
}
```

而 `onCreate` **只在数据库文件不存在时调用一次**。

```
第一次启动：
  数据库文件不存在 → 创建 → onCreate → 建表 + 导入 20 本书

第二次启动：
  数据库文件已存在 → 不调用 onCreate → 数据还在（上次导入的）

卸载重装：
  数据库文件被删 → 又走一遍 onCreate
```

**这解释了几个常见疑问**：

| 疑问 | 答案 |
|---|---|
| 为什么我改了 `books.json`，重新运行没变化？ | 数据库已存在，`onCreate` 不再执行 |
| 怎么让新数据生效？ | 卸载重装，或把 `DATABASE_VERSION` +1 走 `onUpgrade` |
| 为什么重启 App 购物车还在？ | 数据在 SQLite 里，不是内存里 |

> **调试小技巧**：Android Studio 的 Device Explorer 里可以找到 `/data/data/com.yzc.bookmall/databases/book_mall.db`，删掉它再运行就等于"首次安装"。

---

## 10.8 四个 JSON 文件的分工

| 文件 | 条数 | 对应表 | 关键字段 |
|---|---|---|---|
| `stores.json` | 5 | `stores` | 名称、评分、起送价、配送费、公告 |
| `categories.json` | 5 | `categories` | 只有 id 和 name |
| `books.json` | 20 | `books` | 书名、作者、价格（分）、库存、封面、所属书店 |
| `coupons.json` | 4 | `coupons` | 门槛（分）、优惠（分）、有效天数 |

**为什么拆成四个文件而不是一个大文件？**

- 职责清晰：改书店只动 `stores.json`
- 解析简单：不用处理复杂的嵌套结构
- 出错好定位：报错能立刻知道是哪个文件

> 如果是一个大文件，结构会变成 `{"stores": [...], "books": [...]}`，解析时要多一层 `getJSONArray("stores")`。拆开更简单。

---

## 10.9 本篇小结

| 概念 | 要点 |
|---|---|
| JSON | `{}` 对象、`[]` 数组、键必须双引号、不能注释 |
| `assets` | 原样打包，用 `getAssets().open()` 读 |
| 读文件三步 | 开流 → 循环读 → 指定 UTF-8 转字符串 |
| `JSONArray` / `JSONObject` | 对应 `[]` 和 `{}` |
| `getXxx` vs `optXxx` | 前者缺键抛异常，后者返回默认值 |
| `ContentValues` | 一行数据的容器，键 = 数据库列名 |
| `getIdentifier` | 字符串 → 资源 ID，慢但适合一次性场景 |
| 兜底 | 解析失败用最小数据集，不让 App 崩 |
| `onCreate` 只跑一次 | 所以数据不会被重复导入 |

### 自测

1. 为什么读文件要指定 `StandardCharsets.UTF_8`？不指定会怎样？
2. `o.getInt("x")` 和 `o.optInt("x", 0)` 有什么区别？什么时候用哪个？
3. JSON 里的 `"cover": "store_1"` 是怎么变成 `R.drawable.store_1` 的？
4. 为什么改了 `books.json` 重新运行看不到变化？
5. 如果没有 `fallback()`，JSON 格式写错会导致什么后果？

---

下一篇：[11 SharedPreferences 与登录态](11-SharedPreferences与登录态.md) —— 关掉 App 再打开，为什么还是登录状态。
