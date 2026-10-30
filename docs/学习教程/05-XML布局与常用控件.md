# 05 · XML 布局与常用控件

> **本篇你会学到**：界面是怎么用 XML"摆"出来的、`weight` 怎么算、`margin` 和 `padding` 的区别、项目里两种最典型的布局怎么读。

---

## 5.1 布局的本质：一棵 View 树

打开 `res/layout/activity_splash.xml`，它就是一棵**树**：

```
FrameLayout（根，铺满全屏，深蓝背景）
├── LinearLayout（垂直排列，居中）
│   ├── ImageView（Logo）
│   ├── TextView（书墨商城）
│   └── TextView（标语）
├── TextView（跳过，右上角）
└── TextView（倒计时，底部居中）
```

**每个标签就是一个"控件"（View）**，嵌套关系决定它们的位置关系。

> **命名解读**：`View` 是 Android 里所有界面元素的**基类**。`TextView`、`ImageView`、`Button` 全都继承自 `View`。所以代码里常写 `View v = findViewById(...)` —— 因为它是所有控件的共同类型。

---

## 5.2 XML 文件的骨架

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <!-- 子控件 -->
</FrameLayout>
```

**三个必须知道的点：**

1. **`xmlns:android="..."` 是命名空间声明**，必须写在根节点上。没有它，`android:` 前缀就报错。
   > `xmlns` = **XML NameSpace**（XML 命名空间）。

2. **一个布局文件只能有一个根节点**。想放多个并列的东西，外面套一个 `FrameLayout` 或 `LinearLayout`。

3. **`<!-- 注释 -->`** 是 XML 注释，和 HTML 一样。

有些布局还会加 `xmlns:app="http://schemas.android.com/apk/res-auto"`，这是给 **Material 库的自定义属性**用的（比如 `app:cardCornerRadius`）。

---

## 5.3 尺寸三兄弟：`match_parent` / `wrap_content` / 固定值

```xml
android:layout_width="match_parent"    <!-- 和父容器一样宽 -->
android:layout_height="wrap_content"   <!-- 刚好包住内容 -->
android:layout_width="76dp"            <!-- 固定 76dp -->
```

| 值 | 含义 | 什么时候用 |
|---|---|---|
| `match_parent` | 撑满父容器（老写法叫 `fill_parent`） | 整页背景、整行列表项 |
| `wrap_content` | 内容多大就多大 | 文字、图标 |
| `48dp` | 固定尺寸 | 按钮高度、图标大小 |

> **命名解读**：`match_parent` = 匹配父级；`wrap_content` = 包裹内容。两个词都很形象。

---

## 5.4 `margin` 和 `padding`：最容易搞混的一对

```
┌─────────────────────────┐  ← margin（外边距）：自己和其他控件的距离
│   ┌───────────────────┐ │
│   │  padding（内边距） │ │  ← padding：自己边框到内容的距离
│   │   ┌───────────┐   │ │
│   │   │   内容     │   │ │
│   │   └───────────┘   │ │
│   └───────────────────┘ │
└─────────────────────────┘
```

| | 作用 | 属性 |
|---|---|---|
| **margin** | 把自己往外推，影响**自己和别人**的距离 | `layout_margin` / `layout_marginTop` |
| **padding** | 把自己往里缩，影响**自己和内容**的距离 | `padding` / `paddingStart` |

**记忆技巧**：带 `layout_` 前缀的是"我在布局里怎么摆"，不带的是"我自己内部怎么排"。

项目例子：

```xml
<LinearLayout
    android:layout_marginStart="14dp"    ← 整个卡片离屏幕左边 14dp
    android:padding="14dp">              ← 卡片内部的文字离边框 14dp
```

> **为什么用 `Start`/`End` 而不是 `Left`/`Right`？**
> 因为阿拉伯语、希伯来语是从右往左读的。用 `Start`/`End` 的话，切换到这些语言时布局会自动镜像。这是国际化规范。

---

## 5.5 五大布局容器

### ① `LinearLayout` 线性布局 —— 项目里用得最多

按顺序一个挨一个排，靠 `orientation` 决定横还是竖。

```xml
<LinearLayout
    android:orientation="vertical"     <!-- 竖着排 -->
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <TextView ... />   <!-- 第 1 个 -->
    <TextView ... />   <!-- 第 2 个 -->
</LinearLayout>
```

`orientation` 可选 `vertical`（垂直）或 `horizontal`（水平）。

### ② `FrameLayout` 帧布局 —— 层叠

子控件**从左上角开始堆**，后加的盖在前面的上面。

```xml
<FrameLayout>
    <ImageView ... />    <!-- 底层：书店头图 -->
    <View ... />         <!-- 中间层：半透明黑色遮罩 -->
    <ImageView ... />    <!-- 顶层：白色返回箭头 -->
</FrameLayout>
```

**项目里的用法**：`activity_store_detail.xml` 的书店头图就是三层叠起来的。

还有一个高频用法——**用 FrameLayout 做"内容 + 状态页"的切换**：

```xml
<FrameLayout>
    <RecyclerView android:id="@+id/rv_stores" ... />   <!-- 正常内容 -->
    <com.yzc.bookmall.ui.widget.StateView
        android:id="@+id/state_view" ... />            <!-- 加载/空/错误 -->
</FrameLayout>
```

两个控件叠在同一个位置，通过**控制谁可见**来切换。第 13 篇会细讲。

### ③ `ScrollView` 滚动容器

内容超过一屏时可以上下滑。

```xml
<ScrollView>
    <LinearLayout android:orientation="vertical">
        <!-- 很长很长的内容 -->
    </LinearLayout>
</ScrollView>
```

**规则：`ScrollView` 只能有一个直接子控件**，所以里面通常套一个 `LinearLayout`。

`NestedScrollView` 是它的升级版，支持嵌套滚动（比如里面再放一个 RecyclerView）。项目里 `activity_store_detail.xml` 用的是它。

### ④ `MaterialCardView` 卡片

带圆角、阴影、描边的容器。

```xml
<com.google.android.material.card.MaterialCardView
    app:cardCornerRadius="12dp"        <!-- 圆角半径 -->
    app:cardElevation="0dp"            <!-- 阴影高度 -->
    app:strokeColor="@color/divider"   <!-- 描边颜色 -->
    app:strokeWidth="1dp">             <!-- 描边宽度 -->

    <LinearLayout ...>
        <!-- 卡片内容 -->
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

项目里 8 个列表项（`item_book`、`item_store`、`item_order`…）全是用它做的，视觉统一。

> **为什么写这么长的全名 `com.google.android.material.card.MaterialCardView`？**
> 因为它不是 Android 自带的控件，而是 Material 库提供的。XML 里用非 `android.widget` 包下的控件，必须写完整包名。

### ⑤ `Space` 占位

```xml
<Space android:layout_width="0dp" android:layout_height="1dp" android:layout_weight="1" />
```

一个看不见的空白，专门用来**把两边的东西顶开**。项目里大量使用。

---

## 5.6 `layout_weight`：按比例分配剩余空间

这是新手最容易懵的属性。看这段：

```xml
<LinearLayout android:orientation="horizontal" android:layout_width="match_parent">
    <TextView android:layout_width="0dp" android:layout_weight="1" />
    <TextView android:layout_width="60dp" />
    <TextView android:layout_width="0dp" android:layout_weight="2" />
</LinearLayout>
```

**计算规则**：

1. 先把固定宽度的分掉：60dp
2. 剩下的空间按 weight 比例分：1 : 2
3. 第一个拿 1/3，第三个拿 2/3

**关键点：用了 `weight` 的控件，`layout_width` 要写 `0dp`**（横向时）或 `layout_height` 写 `0dp`（纵向时）。写 `0dp` 的意思是"我不预先占位，等分剩余空间"。

### 项目实例：列表项里的文字区域

```xml
<LinearLayout android:orientation="horizontal">
    <ImageView
        android:layout_width="76dp"
        android:layout_height="102dp" />        <!-- 固定宽度 -->

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="102dp"
        android:layout_weight="1">              <!-- 占满剩下的宽度 -->
        <TextView ... />
    </LinearLayout>
</LinearLayout>
```

图片固定 76dp，文字区域自动撑满剩下的——**不管屏幕多宽都成立**。

### 纵向用 weight 撑满剩余高度

```xml
<LinearLayout android:orientation="vertical" android:layout_height="match_parent">
    <include layout="@layout/view_toolbar" />           <!-- 高度 wrap_content -->

    <FrameLayout
        android:layout_height="0dp"
        android:layout_weight="1">                       <!-- 占满剩余高度 -->
    </FrameLayout>

    <LinearLayout android:layout_height="wrap_content">  <!-- 底部栏 -->
    </LinearLayout>
</LinearLayout>
```

这就是项目里所有带工具栏页面的标准骨架：**顶部工具栏 + 中间自适应 + 底部栏**。

---

## 5.7 `gravity` 与 `layout_gravity`

又是一对容易混的：

| 属性 | 控制谁 | 例子 |
|---|---|---|
| `gravity` | **自己内部**的内容对齐 | `TextView` 的 `gravity="center"` → 文字居中 |
| `layout_gravity` | **自己**在父容器里的位置 | `ImageView` 的 `layout_gravity="top\|end"` → 图片在父容器右上角 |

```xml
<TextView
    android:gravity="center"                    <!-- 文字在控件里居中 -->
    android:layout_gravity="bottom|center_horizontal"  <!-- 控件在父容器底部居中 -->
    android:text="3 秒后进入" />
```

> **记忆**：带 `layout_` 的，都是"我在父容器里怎么摆"。

---

## 5.8 常用控件速查

### `TextView` 文字

```xml
<TextView
    android:text="@string/app_name"      <!-- 文字内容 -->
    android:textSize="28sp"              <!-- 字号，用 sp -->
    android:textColor="@color/white"     <!-- 颜色 -->
    android:textStyle="bold"             <!-- 粗体 -->
    android:maxLines="2"                 <!-- 最多两行 -->
    android:ellipsize="end"              <!-- 超出显示省略号 -->
    android:lineSpacingExtra="4dp"       <!-- 行间距 -->
    android:letterSpacing="0.12" />      <!-- 字间距（启动页标题用了） -->
```

> **命名解读**：`ellipsize` = 省略号化，`end` 表示在末尾显示 `…`。

### `ImageView` 图片

```xml
<ImageView
    android:src="@drawable/ic_back"      <!-- 图片资源 -->
    android:scaleType="centerCrop"       <!-- 缩放方式 -->
    android:contentDescription="@string/back" />  <!-- 无障碍描述 -->
```

**`scaleType` 的四种常用值**：

| 值 | 效果 | 项目里用在哪 |
|---|---|---|
| `centerCrop` | 等比放大填满，**超出部分裁掉** | 书封、书店头图 |
| `fitCenter` | 等比缩放到完全显示，可能留白 | 一般不用于照片 |
| `centerInside` | 不放大，只缩小 | 图标 |
| `fitXY` | 拉伸填满，**会变形** | 几乎不用 |

> **`contentDescription` 是给读屏软件用的**。视障用户听不到图片，只能听这个描述。Google 会检查这个属性，缺了会警告。

### `EditText` 输入框

```xml
<EditText
    android:hint="@string/search_hint"          <!-- 占位提示文字 -->
    android:inputType="text"                    <!-- 输入类型（决定键盘样式） -->
    android:imeOptions="actionSearch"           <!-- 键盘右下角显示"搜索" -->
    android:singleLine="true"
    android:background="@null" />               <!-- 去掉默认下划线 -->
```

`inputType` 的常用值：

| 值 | 键盘 |
|---|---|
| `text` | 普通文字 |
| `textPassword` | 密码（显示圆点） |
| `number` | 数字键盘 |
| `phone` | 电话键盘 |

### `MaterialButton` 按钮

```xml
<com.google.android.material.button.MaterialButton
    android:text="@string/add_to_cart"
    android:textSize="14sp"
    app:cornerRadius="10dp"        <!-- 圆角，注意是 app: 前缀 -->
    android:insetTop="0dp"         <!-- 去掉默认内缩 -->
    android:insetBottom="0dp" />
```

`MaterialButton` 默认有 6dp 的内缩（`inset`），设成 0 才能和别的控件对齐。

### `CheckBox` 复选框

```xml
<CheckBox
    android:id="@+id/cb_select"
    android:text="@string/cart_select_all"
    android:minWidth="0dp"       <!-- 去掉默认最小宽度 -->
    android:minHeight="0dp" />
```

代码里监听：

```java
checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
    // isChecked = true 表示被勾上了
});
```

---

## 5.9 完整精读一：`activity_splash.xml`

```xml
<FrameLayout
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/brand_blue">          <!-- ① 整屏深蓝 -->

    <LinearLayout
        android:layout_gravity="center"              <!-- ② 在父容器居中 -->
        android:gravity="center"                     <!-- ③ 内部内容居中 -->
        android:orientation="vertical"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content">

        <ImageView
            android:layout_width="96dp"
            android:layout_height="96dp"
            android:src="@drawable/ic_logo" />       <!-- ④ 书本 Logo -->

        <TextView
            android:layout_marginTop="20dp"
            android:text="@string/app_name"
            android:textSize="28sp"
            android:textStyle="bold"
            android:textColor="@color/white"
            android:letterSpacing="0.12" />          <!-- ⑤ 字间距拉开 -->

        <TextView
            android:layout_marginTop="10dp"
            android:text="@string/app_slogan"
            android:textColor="#B8C6D8"              <!-- ⑥ 浅灰蓝 -->
            android:textSize="14sp" />
    </LinearLayout>

    <TextView
        android:id="@+id/tv_skip"
        android:layout_gravity="top|end"             <!-- ⑦ 右上角 -->
        android:layout_marginTop="12dp"
        android:layout_marginEnd="8dp"
        android:padding="12dp"                       <!-- ⑧ 加大点击区域 -->
        android:text="@string/splash_skip" />
</FrameLayout>
```

**讲解：**

① 根布局用 `FrameLayout` 而不是 `LinearLayout`，因为里面的三块内容是**叠放**的（居中 Logo、右上角跳过、底部倒计时），不是按顺序排列。

②③ `layout_gravity="center"` 让这个 LinearLayout 在父容器居中；`gravity="center"` 让它内部的三个控件水平居中。**两个都写才能"整体居中且内部对齐"**。

④ 96dp 是常见的 Logo 尺寸，太大显得笨重。

⑤ `letterSpacing="0.12"` 把字间距拉开 12%，中文标题拉开一点会显得更精致。可以试试改成 `0` 对比一下。

⑥ `#B8C6D8` 是直接写死的颜色值，没有走 `colors.xml`。**这是个小瑕疵**——规范做法是定义 `@color/text_on_dark_secondary`。你可以试着改过来，作为练习。

⑦ `top|end` = 顶部 + 结束边（中文环境就是右边）。`|` 是"或"的意思，可以组合多个位置。

⑧ `padding="12dp"` 让点击区域变大。文字本身只有 14sp 高，直接点很难点中；加了 padding，实际可点区域变成 12+文字+12。**这是移动端的基本体验要求：可点区域不小于 48dp**。

---

## 5.10 完整精读二：`item_book.xml`（列表项）

这是图书列表的一行，项目里 8 个列表项长得都差不多。

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_marginStart="14dp"
    android:layout_marginEnd="14dp"
    android:layout_marginBottom="10dp"
    android:clickable="true"
    android:focusable="true"
    app:cardCornerRadius="12dp"
    app:cardElevation="0dp"
    app:strokeColor="@color/divider"
    app:strokeWidth="1dp">

    <LinearLayout
        android:orientation="horizontal"        <!-- ① 横排：封面 + 信息 -->
        android:padding="12dp">

        <ImageView
            android:id="@+id/iv_book_cover"
            android:layout_width="76dp"          <!-- ② 封面固定尺寸 -->
            android:layout_height="102dp"
            android:scaleType="centerCrop" />

        <LinearLayout
            android:layout_width="0dp"           <!-- ③ weight 要配 0dp -->
            android:layout_height="102dp"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tv_book_title"
                android:maxLines="2"             <!-- ④ 书名最多两行 -->
                android:ellipsize="end"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tv_book_author"
                android:maxLines="1"
                android:ellipsize="end" />

            <Space
                android:layout_width="1dp"
                android:layout_height="0dp"
                android:layout_weight="1" />     <!-- ⑤ 弹簧，把价格推到底部 -->

            <LinearLayout
                android:orientation="horizontal">
                <TextView android:id="@+id/tv_book_price" />
                <Space android:layout_weight="1" />   <!-- ⑥ 价格靠左，销量靠右 -->
                <TextView android:id="@+id/tv_book_sales" />
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

**讲解：**

① 横排布局：左边封面，右边文字。

② 封面固定 76×102dp（大约是 3:4 的书本比例）。**固定尺寸能保证列表整齐**，不会因为书名长短而忽大忽小。

③ 文字区域 `0dp + weight=1`，自动占满剩余宽度。

④ `maxLines="2"` + `ellipsize="end"`：书名最多两行，超了显示 `…`。**为什么不是一行？** 因为中文书名普遍较长（比如"品味与阶层：漫画解读布尔迪厄的区分理论"），一行显示不下会全部省略，用户就不知道是什么书了。

⑤ `Space` 当"弹簧"用：`layout_weight="1"` 会吃掉所有剩余垂直空间，把价格和销量**顶到卡片底部**。这样即使书名只有一行、作者只有一行，价格也始终在最下面，多个卡片看起来对齐。

⑥ 价格和销量之间再来一个 `Space`，把销量推到最右边。

> **这个"弹簧 Space"技巧在项目里出现了几十次**，是 Android 布局的基本功。

---

## 5.11 `include`：复用一段布局

项目里 8 个页面都有"返回箭头 + 标题"的工具栏，长得一模一样。如果每个文件都复制一遍，改一次要改 8 个地方。

所以抽成了 `view_toolbar.xml`：

```xml
<LinearLayout android:orientation="horizontal">
    <ImageView android:id="@+id/iv_back" ... />
    <TextView android:id="@+id/tv_toolbar_title" ... />
</LinearLayout>
```

用的时候：

```xml
<include layout="@layout/view_toolbar" />
```

**注意**：`include` 进来的控件，ID 是**共用的**。所以在 `BookDetailActivity` 里可以直接：

```java
((TextView) findViewById(R.id.tv_toolbar_title)).setText(R.string.book_detail_title);
findViewById(R.id.iv_back).setOnClickListener(v -> finish());
```

> **局限**：`include` 不能覆盖被包含布局里的 ID。所以如果同一页面要放两个工具栏，ID 会冲突。项目里没有这种需求。

---

## 5.12 布局优化的两条经验

### ① 嵌套别太深

每多一层嵌套，测量和绘制就多一次开销。理想是不超过 **5 层**。

如果发现嵌套很深，可以考虑用 `ConstraintLayout`（约束布局）替代——它能在**一层**里搞定复杂定位。本项目没用它，因为 `LinearLayout` + `weight` 已经够用，而且更容易看懂。

### ② 列表项布局越简单越好

`item_book.xml` 会在一屏里出现 5~8 次，滚动时反复创建。所以：

- 不要用 `wrap_content` 频繁触发布局重算（项目里封面用了固定尺寸）
- 不要在列表项里放复杂的 `selector` 动画
- 背景用纯色或简单 shape，别用大图

---

## 5.13 本篇小结

| 概念 | 要点 |
|---|---|
| 布局本质 | 一棵 View 树，一个文件一个根节点 |
| 尺寸 | `match_parent` 撑满 / `wrap_content` 包内容 / 固定 dp |
| margin vs padding | 外推自己 vs 内缩自己 |
| `LinearLayout` | 按顺序排列，配 `orientation` |
| `FrameLayout` | 层叠，做遮罩和状态切换 |
| `layout_weight` | 分剩余空间，配 `0dp` |
| `gravity` vs `layout_gravity` | 内部对齐 vs 自己在父容器中的位置 |
| `include` | 复用布局片段 |
| `Space` | 当弹簧用，顶开两边 |

### 自测

1. `layout_width="0dp"` + `layout_weight="1"` 是什么意思？为什么不能写 `wrap_content`？
2. `margin` 和 `padding` 分别影响什么？
3. 为什么列表项的封面要用固定尺寸而不是 `wrap_content`？
4. `include` 复用布局有什么局限？

---

下一篇：[06 Intent 与页面跳转](06-Intent与页面跳转.md) —— 页面之间怎么"说话"，怎么把 `book_id` 带过去。
