# 图书商城本地离线版 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 Android 空工程中实现一个不依赖网络的图书商城，完成登录、浏览、搜索、收藏、购物车、下单和订单查看主流程。

**Architecture:** 使用 Java 11 + XML 布局 + SQLiteOpenHelper。`BookRepository` 负责数据库读写，`SessionManager` 负责登录会话，`MainActivity` 使用底部导航承载首页、分类、购物车和个人中心，其余流程通过独立 Activity 完成。金额以人民币“分”为整数保存，订单提交使用 SQLite 事务保证库存、订单和购物车的一致性。

**Tech Stack:** Android SDK 36、minSdk 24、Java 11、AppCompat 1.8.0、Material Components 1.14.0、SQLite、SharedPreferences、JUnit 4、AndroidX test。

**Spec:** `docs/superpowers/specs/2026-10-27-book-mall-design.md`

## Global Constraints

- 所有核心流程必须不依赖网络。
- 使用 Java 11、XML 布局和现有 AppCompat/Material 依赖，不迁移到 Kotlin 或 Compose。
- 图书封面使用项目内本地 drawable 资源。
- 首次启动预置至少 10 本图书、5 个分类和演示数据提示。
- 订单第一版只有“待发货”和“已完成”；提交订单后直接进入“待发货”。
- 登录状态通过 `SharedPreferences` 跨应用重启保留；主动退出或清空当前账号数据后回到登录页。
- 结算页收集姓名、手机号和地址，并将其保存到订单表，不扩展用户表。
- 提交订单前重新检查全部库存；任一商品不足时整单失败并返回购物车。
- 当前 workspace 不是 Git 仓库，因此以每个任务的构建或测试检查点替代 commit 步骤。

## Review Focus

- 首次启动数据库初始化失败或重复初始化时，应用仍应给出可理解的错误，不应出现空白首页或重复种子数据；由 Task 1 的数据库初始化测试覆盖。
- 用户名重复、空输入、密码确认不一致和错误密码应阻止进入商城并给出明确提示；由 Task 2 的账号验证测试覆盖。
- 无登录状态访问收藏、购物车、下单和订单入口时，应跳转登录页并在登录后回到原入口；由 Task 2/4/5 的导航测试覆盖。
- 搜索无结果、购物车为空、收藏为空和订单为空时，应展示空状态而不是崩溃或显示旧数据；由 Task 3/4/5 的界面测试覆盖。
- 库存不足、结算数量超过库存、金额计算和清空当前账号数据时，应保持数据库状态一致且不影响其他账号和图书目录；由 Task 1/4/5/6 的数据库测试覆盖。

## 文件结构与职责

### 数据与工具

- Create: `app/src/main/java/com/yzc/bookmall/model/Book.java` — 图书实体，价格字段使用 `int priceFen`。
- Create: `app/src/main/java/com/yzc/bookmall/model/Category.java` — 分类实体。
- Create: `app/src/main/java/com/yzc/bookmall/model/CartItem.java` — 当前用户购物车行，包含图书快照和数量。
- Create: `app/src/main/java/com/yzc/bookmall/model/Order.java` — 订单摘要实体。
- Create: `app/src/main/java/com/yzc/bookmall/model/OrderItem.java` — 订单明细实体，保存标题、价格和封面快照。
- Create: `app/src/main/java/com/yzc/bookmall/model/ReceiverInfo.java` — 结算页姓名、手机号和地址。
- Create: `app/src/main/java/com/yzc/bookmall/data/BookDbHelper.java` — SQLite 建表、升级、种子数据和数据库事务。
- Create: `app/src/main/java/com/yzc/bookmall/data/BookRepository.java` — 为页面提供图书、账号、收藏、购物车和订单查询/写入接口。
- Create: `app/src/main/java/com/yzc/bookmall/util/SessionManager.java` — 保存、读取和清除当前用户会话。
- Create: `app/src/main/java/com/yzc/bookmall/util/FormatUtils.java` — 人民币金额、时间和订单号格式化。

### 页面与适配器

- Create: `app/src/main/java/com/yzc/bookmall/ui/MainActivity.java` — 底部导航和四个主页面容器。
- Create: `app/src/main/java/com/yzc/bookmall/ui/LoginActivity.java` — 登录。
- Create: `app/src/main/java/com/yzc/bookmall/ui/RegisterActivity.java` — 注册。
- Create: `app/src/main/java/com/yzc/bookmall/ui/BookDetailActivity.java` — 图书详情、收藏和加入购物车。
- Create: `app/src/main/java/com/yzc/bookmall/ui/SearchActivity.java` — 搜索结果。
- Create: `app/src/main/java/com/yzc/bookmall/ui/CheckoutActivity.java` — 收货信息、订单确认和提交。
- Create: `app/src/main/java/com/yzc/bookmall/ui/OrderListActivity.java` — 订单列表。
- Create: `app/src/main/java/com/yzc/bookmall/ui/OrderDetailActivity.java` — 订单详情和确认收货。
- Create: `app/src/main/java/com/yzc/bookmall/ui/FavoriteActivity.java` — 收藏列表。
- Create: `app/src/main/java/com/yzc/bookmall/ui/fragment/HomeFragment.java` — 推荐图书、分类入口和热门列表。
- Create: `app/src/main/java/com/yzc/bookmall/ui/fragment/CategoryFragment.java` — 分类和分类下图书。
- Create: `app/src/main/java/com/yzc/bookmall/ui/fragment/CartFragment.java` — 当前用户购物车。
- Create: `app/src/main/java/com/yzc/bookmall/ui/fragment/ProfileFragment.java` — 个人中心。
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/BookAdapter.java` — 图书列表/横向卡片。
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/CategoryAdapter.java` — 分类入口。
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/CartAdapter.java` — 购物车数量、选择和删除。
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/OrderAdapter.java` — 订单列表。
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/OrderItemAdapter.java` — 订单明细。

### 资源与测试

- Modify: `app/src/main/AndroidManifest.xml` — 注册 Activity 并设置启动页。
- Modify: `app/src/main/res/values/strings.xml` — 页面文案、错误提示和状态文本。
- Modify: `app/src/main/res/values/colors.xml`、`themes.xml` — 暖橙/深蓝 Material 主题。
- Create: `app/src/main/res/layout/*.xml` — Activity、Fragment、列表项和空状态布局。
- Create: `app/src/main/res/drawable/book_cover_*.xml` — 至少 5 个本地矢量封面资源，供 10 本种子图书复用或组合。
- Create: `app/src/test/java/com/yzc/bookmall/FormatUtilsTest.java` — 金额和订单号格式化单元测试。
- Create: `app/src/androidTest/java/com/yzc/bookmall/BookDatabaseTest.java` — 数据库初始化、唯一约束、库存和订单事务测试。
- Create: `app/src/androidTest/java/com/yzc/bookmall/BookMallNavigationTest.java` — Activity 启动、登录保护和关键页面导航测试。

## Implementation Tasks

### Task 1: 建立数据模型、SQLite 数据库和种子数据

**Files:**
- Create: `app/src/main/java/com/yzc/bookmall/model/Book.java`
- Create: `app/src/main/java/com/yzc/bookmall/model/Category.java`
- Create: `app/src/main/java/com/yzc/bookmall/model/CartItem.java`
- Create: `app/src/main/java/com/yzc/bookmall/model/Order.java`
- Create: `app/src/main/java/com/yzc/bookmall/model/OrderItem.java`
- Create: `app/src/main/java/com/yzc/bookmall/model/ReceiverInfo.java`
- Create: `app/src/main/java/com/yzc/bookmall/data/BookDbHelper.java`
- Create: `app/src/main/java/com/yzc/bookmall/data/BookRepository.java`
- Create: `app/src/androidTest/java/com/yzc/bookmall/BookDatabaseTest.java`
- Create: `app/src/main/res/drawable/book_cover_1.xml` through `book_cover_5.xml`

**Interfaces:**
- `BookRepository(Context context)`
- `List<Book> getRecommendedBooks()`
- `List<Book> getBooksByCategory(long categoryId)`
- `List<Book> searchBooks(String keyword)`
- `Book getBookById(long bookId)`
- `List<Category> getCategories()`
- `long registerUser(String username, String password)`; duplicate username returns `-1`.
- `long authenticate(String username, String password)`; failed authentication returns `-1`.
- `boolean isFavorite(long userId, long bookId)`
- `void setFavorite(long userId, long bookId, boolean favorite)`
- `List<Book> getFavoriteBooks(long userId)`
- `List<CartItem> getCartItems(long userId)`
- `void addToCart(long userId, long bookId, int quantity)`
- `void updateCartQuantity(long userId, long bookId, int quantity)`
- `void deleteCartItem(long userId, long bookId)`
- `void clearCurrentUserData(long userId)`
- `long createOrder(long userId, List<CartItem> selectedItems, ReceiverInfo receiverInfo)`; throws `IllegalStateException` for invalid receiver data or insufficient stock and performs all writes in one transaction.
- `List<Order> getOrders(long userId)`
- `List<OrderItem> getOrderItems(long orderId)`
- `void confirmReceipt(long orderId, long userId)`

**Steps:**

- [ ] Create model classes with immutable identifiers, getters, and constructors matching the fields in the design spec; store `priceFen` and `totalAmountFen` as `int`.
- [ ] Implement `BookDbHelper` with tables `users`, `categories`, `books`, `favorites`, `cart_items`, `orders`, and `order_items`; add foreign keys and unique constraints for username and `(user_id, book_id)` favorites/cart rows.
- [ ] Seed 5 categories, at least 10 books, and one demo account only when the tables are empty; use local drawable resource IDs for covers and realistic Chinese book metadata.
- [ ] Implement repository reads and writes with parameterized queries and explicit cursor closing; return empty lists for valid queries with no rows.
- [ ] Implement `createOrder` as a transaction: re-read each selected book stock, reject the whole order if any item is unavailable, insert the order and snapshot item rows, decrement stock, then delete only the selected cart rows.
- [ ] Add database tests for seed counts, duplicate favorite/cart prevention, inventory rejection rollback, successful stock decrement, order snapshot values, and preservation of another user’s data during `clearCurrentUserData`.
- [ ] Run `./gradlew testDebugUnitTest` and `./gradlew connectedDebugAndroidTest` when an emulator/device is available; expected result is PASS for the new database coverage.

### Task 2: 应用基础、会话管理和账号流程

**Files:**
- Create: `app/src/main/java/com/yzc/bookmall/util/SessionManager.java`
- Create: `app/src/main/java/com/yzc/bookmall/util/FormatUtils.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/LoginActivity.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/RegisterActivity.java`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/layout/activity_login.xml`
- Create: `app/src/main/res/layout/activity_register.xml`
- Create: `app/src/test/java/com/yzc/bookmall/FormatUtilsTest.java`

**Interfaces:**
- `SessionManager(Context context)`
- `void saveSession(long userId, String username)`
- `boolean isLoggedIn()`
- `long getUserId()`
- `String getUsername()`
- `void clearSession()`
- `String FormatUtils.formatPrice(int priceFen)` returns `¥12.00` style text.
- `String FormatUtils.generateOrderNo()` returns a non-empty time-based order number.

**Steps:**

- [ ] Implement `SessionManager` with a private SharedPreferences file; persist only user ID and username, and make `clearSession()` remove both values.
- [ ] Implement `FormatUtils` with integer-fen price formatting and a stable human-readable date formatter; add unit assertions for `0`, `1299`, and large values.
- [ ] Create the login and registration XML with Material text fields, loading-safe buttons, error text, and links between the two pages.
- [ ] Implement `LoginActivity`: reject blank fields, call `authenticate`, save a valid session, open `MainActivity`, and show a clear error for invalid credentials.
- [ ] Implement `RegisterActivity`: validate required fields, password confirmation, minimum password length, call `registerUser`, and route successful registration to login with the username prefilled.
- [ ] Register activities and set `LoginActivity` as the launcher until startup routing is added; keep all strings in `strings.xml`.
- [ ] Run unit tests and build the debug APK; expected result is PASS and no manifest/resource errors.

### Task 3: 主框架、首页、分类、搜索和详情

**Files:**
- Create: `app/src/main/java/com/yzc/bookmall/ui/MainActivity.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/BookDetailActivity.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/SearchActivity.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/fragment/HomeFragment.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/fragment/CategoryFragment.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/BookAdapter.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/CategoryAdapter.java`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/layout/activity_book_detail.xml`
- Create: `app/src/main/res/layout/activity_search.xml`
- Create: `app/src/main/res/layout/fragment_home.xml`
- Create: `app/src/main/res/layout/fragment_category.xml`
- Create: `app/src/main/res/layout/item_book.xml`
- Create: `app/src/main/res/layout/item_category.xml`
- Create: `app/src/main/res/layout/item_book_horizontal.xml`
- Create: `app/src/main/res/layout/view_empty_state.xml`

**Interfaces:**
- `MainActivity.openBook(long bookId)` starts `BookDetailActivity` with `EXTRA_BOOK_ID`.
- `BookAdapter.OnBookClickListener.onBookClick(Book book)`.
- `CategoryAdapter.OnCategoryClickListener.onCategoryClick(Category category)`.
- `SearchActivity` accepts `EXTRA_INITIAL_KEYWORD` and displays `searchBooks(keyword)` results.

**Steps:**

- [ ] Build `activity_main.xml` with a fragment container and Material bottom navigation entries for Home, Category, Cart, and Profile; make MainActivity select Home by default.
- [ ] Implement startup routing: if `SessionManager.isLoggedIn()` is true, open MainActivity; otherwise open LoginActivity; clear the launcher Activity from the back stack after routing.
- [ ] Implement HomeFragment with search action, category shortcuts, recommended horizontal list, and book list; reload data in `onResume()`.
- [ ] Implement CategoryFragment with category list and selected-category book results; show an empty state for a valid category with no books.
- [ ] Implement SearchActivity with keyword input, IME search action, title/author/category matching through the repository, and a no-results state.
- [ ] Implement BookDetailActivity with cover, title, author, publisher, description, stock, price, quantity controls, favorite toggle, and add-to-cart action; clamp quantity to available stock.
- [ ] Add local vector covers and book list/card layouts that fit 360dp–411dp portrait widths without clipping.
- [ ] Run `./gradlew assembleDebug`; expected result is a successful APK with the main navigation, catalog, search, and detail screens registered.

### Task 4: 收藏和购物车

**Files:**
- Create: `app/src/main/java/com/yzc/bookmall/ui/fragment/CartFragment.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/CartAdapter.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/FavoriteActivity.java`
- Create: `app/src/main/res/layout/fragment_cart.xml`
- Create: `app/src/main/res/layout/activity_favorite.xml`
- Create: `app/src/main/res/layout/item_cart.xml`

**Interfaces:**
- `CartAdapter.OnCartChangedListener.onCartChanged()` recalculates selected total and enabled checkout state.
- `CartAdapter.OnBookClickListener.onBookClick(long bookId)`.
- `CartFragment.requireLoginOrOpenLogin()` routes unauthenticated users to LoginActivity.

**Steps:**

- [ ] Add login gating to cart, favorite, and profile actions while preserving the intended destination in the Activity intent.
- [ ] Implement FavoriteActivity using repository favorite queries, with toggle/remove support and an empty state; refresh in `onResume()`.
- [ ] Implement CartFragment with selected flags, select-all, plus/minus quantity updates, deletion confirmation, total calculation in fen, and a checkout button disabled when no row is selected.
- [ ] Make cart quantity updates reject values below 1 or above the current stock and refresh the row with a user-visible message.
- [ ] Add click-through from cart/favorites to BookDetailActivity and return refresh behavior through `onResume()`.
- [ ] Add UI/database tests for empty states, login gating, duplicate add-to-cart merge behavior, select-all totals, and deleting only the requested row.
- [ ] Run unit, instrumentation, and debug build checks; expected result is PASS.

### Task 5: 结算、订单和库存事务

**Files:**
- Create: `app/src/main/java/com/yzc/bookmall/ui/CheckoutActivity.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/OrderListActivity.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/OrderDetailActivity.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/OrderAdapter.java`
- Create: `app/src/main/java/com/yzc/bookmall/ui/adapter/OrderItemAdapter.java`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/layout/activity_checkout.xml`
- Create: `app/src/main/res/layout/activity_order_list.xml`
- Create: `app/src/main/res/layout/activity_order_detail.xml`
- Create: `app/src/main/res/layout/item_order.xml`
- Create: `app/src/main/res/layout/item_order_item.xml`

**Interfaces:**
- `CheckoutActivity` receives selected cart item IDs or a serialized selected-item list and a user ID from SessionManager.
- `OrderDetailActivity` receives `EXTRA_ORDER_ID` and validates ownership before displaying.
- `BookRepository.createOrder(...)` is the single write path for checkout.

**Steps:**

- [ ] Build checkout UI for selected books, total price, editable receiver name/phone/address, validation messages, and submit button with a confirmation dialog.
- [ ] Implement checkout submission through `BookRepository.createOrder`; on insufficient stock show the exact failure reason and return to CartFragment without creating an order.
- [ ] Implement OrderListActivity sorted by newest first with status badges “待发货” and “已完成”, order total, item summary, and empty state.
- [ ] Implement OrderDetailActivity with receiver info, order items, price snapshots, total, status, and ownership check; show “确认收货” only for the “待发货” state.
- [ ] Implement confirmation receipt as an ownership-checked update from “待发货” to “已完成”, then refresh the detail screen.
- [ ] Add instrumentation coverage for successful checkout, inventory rollback, cart cleanup, order snapshots, and confirm-receipt transition.
- [ ] Run `./gradlew connectedDebugAndroidTest` and `./gradlew assembleDebug`; expected result is PASS.

### Task 6: 个人中心、重置数据和整体视觉收尾

**Files:**
- Create: `app/src/main/java/com/yzc/bookmall/ui/fragment/ProfileFragment.java`
- Modify: `app/src/main/java/com/yzc/bookmall/ui/MainActivity.java`
- Modify: `app/src/main/java/com/yzc/bookmall/util/SessionManager.java`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Create/Modify: profile and shared layout resources under `app/src/main/res/layout/`

**Interfaces:**
- `ProfileFragment` opens `FavoriteActivity` and `OrderListActivity`, calls `SessionManager.clearSession()` on logout, and calls `BookRepository.clearCurrentUserData(userId)` on confirmed reset.

**Steps:**

- [ ] Implement ProfileFragment with username, favorite/order entry points, logout, and clear-current-account-data actions.
- [ ] Add confirmation dialogs for logout and data reset; after reset, clear session and route to LoginActivity with the back stack removed.
- [ ] Apply the approved warm orange/deep blue Material palette, consistent typography, spacing, button states, toolbar titles, and empty/error states.
- [ ] Verify the cold-start route keeps a saved session and opens MainActivity, while logout/reset opens LoginActivity.
- [ ] Verify another account’s favorites, cart, and orders remain after the current account reset.
- [ ] Run the full debug build and all available unit/instrumentation tests; expected result is PASS.

### Task 7: Final verification and handoff

**Files:**
- Modify only files needed to fix verification failures.
- Add or update tests only when a failure exposes a missing behavior from the approved spec.

**Steps:**

- [ ] Run `./gradlew testDebugUnitTest` and record the result.
- [ ] Run `./gradlew assembleDebug` and record the generated APK path.
- [ ] If an emulator/device is available, run `./gradlew connectedDebugAndroidTest` and fix failures before claiming completion.
- [ ] Manually walk the acceptance flow: first launch, register, login, browse, search, detail, favorite, cart, checkout, order detail, confirm receipt, logout, restart, and reset current account data.
- [ ] Confirm no screen requires network access and no seeded data is duplicated after relaunch.
- [ ] Report changed files, test commands, results, and any environment limitation such as unavailable emulator.

## Self-Review Checklist

- **Spec coverage:** Tasks 1–2 cover local persistence, accounts, session behavior, and money rules; Tasks 3–4 cover catalog, search, detail, favorites, and cart; Task 5 covers checkout, stock transaction, and orders; Task 6 covers profile, logout, reset, and visual consistency; Task 7 covers the complete acceptance flow.
- **Interface consistency:** Repository method names and return values are defined in Task 1 and reused unchanged by later tasks; Activity extras are named in Tasks 3 and 5.
- **Failure coverage:** Empty states, login protection, duplicate data, stock conflicts, rollback, and cross-account reset behavior are explicitly assigned to tests.
- **Proportion:** The plan defines file boundaries, interfaces, and checks without prescribing method bodies beyond the transaction behavior fixed by the spec.
