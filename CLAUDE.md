# Accenture Booking Demo — Kotlin/Android 编码规范

> 本规范结构仿照 [Kodeco Swift Style Guide](https://github.com/kodecocodes/swift-style-guide)，针对 Kotlin / Android 项目制定。
> 每个规则都有 **推荐（Preferred）** 与 **不推荐（Not Preferred）** 的代码示例。

---

## 首要目标

**清晰、一致、简洁** — 按此优先级排序。

- **清晰（Clarity）**：代码被阅读的次数远多于被编写的次数，优先为读者优化
- **一致（Consistency）**：整个项目保持统一的风格，降低认知负担
- **简洁（Brevity）**：在清晰和一致的前提下，尽量精简

---

## 1. 正确性

> 将正确性问题尽早暴露，避免在运行时才发现。

### 前置条件检查

使用 `require()` 和 `check()` 验证参数和状态，而非静默地产生错误结果。

```kotlin
// 推荐
fun calculateDiscount(price: Double, percent: Int): Double {
    require(price > 0) { "Price must be positive, got $price" }
    require(percent in 0..100) { "Percent must be 0-100, got $percent" }
    return price * (1 - percent / 100.0)
}

// 不推荐
fun calculateDiscount(price: Double, percent: Int): Double {
    return price * (1 - percent / 100.0) // 负数会悄悄产生 bug
}
```

### 避免强制解包（!!）

`!!` 会使程序崩溃且不留有用信息。优先使用安全调用和 Elvis 操作符。

```kotlin
// 推荐
val name = user?.name ?: return
val length = list?.size ?: 0
data?.let { processData(it) }

// 不推荐
val name = user!!.name // 崩溃时只有 NullPointerException
val length = list!!.size
```

### 避免隐式解包（lateinit var）

`lateinit var` 本质上是一种"隐式解包"——编译器不强制检查，访问未初始化的属性会抛 `UninitializedPropertyAccessException`。

```kotlin
// 推荐：使用 by lazy
private val adapter: BookingAdapter by lazy { BookingAdapter() }

// 推荐：构造函数注入
class BookingCache(private val context: Context)

// 可接受：Android 框架强制要求时
@Inject lateinit var repository: BookingRepository // Hilt 注入

// 不推荐：能构造传参却用 lateinit
class MyService {
    lateinit var context: Context // 应通过构造函数传入
}
```

### 使用 sealed class 统一错误处理

```kotlin
// 推荐
sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
    data class Error(val message: String) : DataState<Nothing>()
}

// 消费端必须处理所有情况
when (state) {
    is DataState.Loading -> showLoading()
    is DataState.Success -> showData(state.data)
    is DataState.Error -> showError(state.message)
}

// 不推荐：分散的 null 检查和 try-catch
var data: BookingResponse? = null
var error: String? = null
var loading = false
```

### 异常处理

捕获具体异常类型，避免捕获 `Throwable`（它会吞掉 `OutOfMemoryError` 等严重错误）。

```kotlin
// 推荐
try {
    val data = service.fetchBookingData()
    cache.save(data)
} catch (e: IOException) {
    Log.e(TAG, "Network error", e)
    _data.postValue(DataState.Error("Failed to load data"))
}

// 不推荐
try {
    val data = service.fetchBookingData()
} catch (e: Exception) { // 过于宽泛
    // 静默吞掉
}
```

---

## 2. 命名规范

### 2.1 基本原则

> 参考 Kotlin 官方 [Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html#naming-rules) 及 Apple API Design Guidelines。

| 规则 | 说明 |
|------|------|
| **清晰优先于简洁** | `originAndDestinationPair` 优于 `odPair` |
| **camelCase** | 变量、函数、参数、属性使用 `camelCase` |
| **PascalCase** | 类、接口、对象、枚举使用 `PascalCase` |
| **UPPER_SNAKE_CASE** | `const val` 常量、顶层不可变常量 |
| **包含必要单词，省略多余单词** | `remove(item:)` 而非 `removeElementAtIndex(_:)` |
| **基于角色命名，而非类型** | `segments` 而非 `segmentList` |
| **弱类型信息用参数标签补偿** | `getUser(byId: String)` 而非 `getUser(String)` |
| **追求流畅的调用站点** | `cache.isValid()` 和 `cache.save(data)` |
| **副作用命名** | 非修改用 `-ed`/`-ing`（`sorted()`）；修改用动词（`sort()`） |
| **布尔属性** | 读起来像断言：`isEmpty`, `isEnabled`, `hasMore`, `canIssueTicketChecking` |
| **接口描述是什么** | 名词：`Repository`, `DataSource`, `BookingService` |
| **接口描述能做什么** | 形容词/able：`Parcelable`, `Serializable` |
| **避免缩写** | `numberOfItems` 而非 `numItems` |
| **使用先例** | 遵循 Android / Kotlin 标准库的命名模式 |
| **优先方法/属性而非顶层函数** | 当有明显 `this` 时不要用顶层函数 |
| **首字母缩写大小写一致** | 全大写或全小写：`XMLParser`, `parseXml()`；`URL` → `val urlString` |
| **相同含义使用相同基名** | 不要用返回类型区分重载 |
| **参数名作为文档** | 选择有意义的参数标签 |
| **lambda 参数应加标签** | 不要用裸 `(String) -> Unit` |
| **默认参数简化常见场景** | `fun load(force: Boolean = false)` |

### 2.2 书面引用方法（Prose）

在文档或注释中引用方法时，按以下优先顺序选择最简明确的形式：

1. **无参数** — `load`
2. **参数标签** — `loadMore(page:)`
3. **完整签名** — `loadMore(page: Int, force: Boolean)`

### 2.3 类前缀

**不要使用类前缀**（如 `AccBookingDataManager`）。Kotlin 包名已经提供了命名空间。

```kotlin
// 推荐
import com.accenture.booking.data.BookingDataManager

// 不推荐
class AccBookingDataManager // 包名已经标识了模块
```

### 2.4 委托 / 回调

第一个参数（通常是未命名的）必须是事件源对象：

```kotlin
// 推荐
fun onBookingClicked(view: View, booking: Segment)
fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int)

// 不推荐
fun onBookingClicked(booking: Segment, view: View)
fun onScrollStateChanged(newState: Int, recyclerView: RecyclerView)
```

### 2.5 利用类型推断

当编译器能推断类型时省略显式类型标注，使代码更简洁：

```kotlin
// 推荐
val selector = R.id.buttonSubmit
view.setBackgroundColor(Color.RED)
val view = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT)
val toView = context.getView(R.id.toView)

// 不推荐
val selector: Int = R.id.buttonSubmit
view.setBackgroundColor(android.graphics.Color.RED)
val view: ViewGroup.LayoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT)
val toView: View = context.getView(R.id.toView) as View
```

### 2.6 泛型

泛型参数使用有意义的大驼峰命名；没有明确角色时用传统的 `T`、`U`、`V`：

```kotlin
// 推荐
class Stack<Element> { /* ... */ }
fun <T : OutputStream> write(to target: T)
fun <T> swap(a: T, b: T): Pair<T, T>

// 不推荐
class Stack<T> { /* ... */ } // 当 Element 更有意义时
fun <T : OutputStream> write(to target: T) // T 可以更具体
```

### 2.7 语言

使用**美式英语**拼写：`color` 而非 `colour`，`initialize` 而非 `initialise`。

---

## 3. 代码组织

### 3.1 使用分组组织代码

将相关功能放在一起，使用注释或区域标记分隔：

```kotlin
// 推荐：用注释分隔逻辑区块
class BookingDataManager(context: Context) {

    // ---- 依赖 ----
    private val service = BookingService(context)
    private val cache = BookingCache(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ---- 公开状态 ----
    private val _data = MutableLiveData<DataState<BookingResponse>>()
    val data: LiveData<DataState<BookingResponse>> = _data

    // ---- 公开操作 ----
    fun load() { /* ... */ }
    fun refresh() { /* ... */ }
    fun loadMore() { /* ... */ }

    // ---- 内部实现 ----
    private fun fetch() { /* ... */ }
    private fun generatePage(p: Int): List<Segment> { /* ... */ }

    // ---- 常量与单例 ----
    companion object {
        private const val PAGE_SIZE = 10
    }
}
```

### 3.2 接口实现放独立区域

为接口实现创建独立的分组块，不要全部塞在类声明中：

```kotlin
// 推荐
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }
}

// ---- RecyclerView.OnScrollListener ----
private fun MainActivity.setupRecyclerView() {
    binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy > 0 && shouldLoadMore()) vm.loadMore()
        }
    })
}
```

### 3.3 删除未使用（死）代码

- 删除被注释掉的代码块
- 删除空的 `override` 方法（仅调用 `super` 的也应删除）
- 删除 IDE 生成的模板占位符（`TODO` 除外）
- 例外：教学的保留代码需有明确注释说明

```kotlin
// 不推荐：保留被注释掉的代码
// fun oldMethod() {
//     // 之前这样实现，先留着
// }

// 不推荐：空的 override
override fun onDestroy() {
    super.onDestroy() // 仅调用 super，应删除
}
```

### 3.4 最小化导入

只导入文件实际使用的类，**禁止使用 `*` 通配符导入**：

```kotlin
// 推荐
import com.accenture.booking.model.BookingResponse
import com.accenture.booking.model.Segment
import com.accenture.booking.model.Location

// 不推荐
import com.accenture.booking.model.*
```

---

## 4. 格式（空格与缩进）

### 4.1 缩进

**4 个空格**缩进（Kotlin 标准）。不使用 Tab 字符。在 IDE 和 `.editorconfig` 中设置。

### 4.2 大括号

左大括号与语句在同一行，右大括号独占一行。`else` / `catch` / `finally` 与右大括号同行：

```kotlin
// 推荐
if (user.isHappy) {
    doSomething()
} else {
    doSomethingElse()
}

// 不推荐
if (user.isHappy)
{
    doSomething()
}
else {
    doSomethingElse()
}
```

> 即使单行语句也必须使用大括号（Kotlin 官方风格例外：`when` 分支的单行表达式可省略）。

```kotlin
// 推荐
if (error != null) return

// 也推荐：Kotlin 风格允许 when 单行省略
when (state) {
    is DataState.Loading -> showLoading()
    is DataState.Success -> showData(state.data)
    is DataState.Error -> showError(state.message)
}
```

### 4.3 空行

- 方法之间：**恰好一个空行**
- 类型声明之间：**最多一个空行**
- 方法内部：用空行分隔逻辑段落（段落太多说明该重构为多个方法）
- 左大括号后、右大括号前：**零个空行**

```kotlin
// 推荐
fun load() {
    page = 0
    _hasMore.postValue(true)

    val cached = if (cache.isValid()) cache.get() else null
    if (cached != null) {
        currentData = cached
        _data.postValue(DataState.Success(cached))
    } else {
        _data.postValue(DataState.Loading)
    }

    fetch()
}

// 不推荐
fun load() {

    page = 0
    _hasMore.postValue(true)
    val cached = if (cache.isValid()) cache.get() else null
    if (cached != null) {

        currentData = cached
        _data.postValue(DataState.Success(cached))

    } else {
        _data.postValue(DataState.Loading)
    }
    fetch()

}
```

### 4.4 闭括号

**不要让闭括号单独占一行**：

```kotlin
// 推荐
val user = getUser(
    for: userID,
    on: connection,
)

// 也推荐（单行够用时）
val user = getUser(for: userID, on: connection)

// 不推荐
val user = getUser(
    for: userID,
    on: connection
)
```

### 4.5 冒号

| 场景 | 空格规则 | 示例 |
|------|---------|------|
| 类型声明 | 左边无空格，右边一个空格 | `class Foo : Bar`, `val name: String` |
| 泛型约束 | 左边无空格，右边一个空格 | `class Stack<T : Serializable>` |
| Map 字面量 | 左边无空格，右边一个空格 | `mapOf("A" to 1, "B" to 2)` |
| 三元用 if-else | 两边都有空格 | `val x = if (cond) a else b` |

```kotlin
// 推荐
class TestDatabase : Database {
    val data: Map<String, Double> = mapOf("A" to 1.2, "B" to 3.2)
}

// 不推荐
class TestDatabase : Database {
    val data : Map<String, Double> = mapOf("A" to 1.2, "B":3.2)
}
```

### 4.6 行宽

建议不超过 **120** 字符。超过时适当换行，每个参数一行。

### 4.7 尾随空格 & 文件末尾

- **禁止行尾空白字符**（IDE 设置自动 trim）
- 文件末尾**恰好一个**换行符

---

## 5. 注释

### 原则

- **解释"为什么"而非"是什么"** — 有意义的命名已经说明了"做什么"
- 注释必须保持与代码同步更新，过时的注释比没有注释更危险
- 代码应尽可能自解释
- **不保留被注释掉的代码**，直接删除（Git 历史可以找回）

### 行内注释

避免使用 `/* ... */` 块注释在行内。使用 `//` 或 `/** */` 文档注释。

```kotlin
// 推荐：解释非直观的决策
// expiryTime 是秒级 Unix 时间戳，需要乘以 1000 转换为毫秒再比较
return System.currentTimeMillis() < expiry * 1000L

// 不推荐：复述代码
// 把 System.currentTimeMillis() 和 expiry 乘以 1000 做比较
return System.currentTimeMillis() < expiry * 1000L
```

### KDoc

公开 API（public 函数、类、属性）使用 KDoc 格式：

```kotlin
/**
 * 加载预订数据，采用缓存优先策略：
 * 若有有效缓存则立即展示，同时后台拉取最新数据。
 */
fun load() { /* ... */ }
```

---

## 6. 类与数据结构

### 6.1 选择正确的类型

| 需求 | Kotlin 类型 |
|------|------------|
| 纯数据持有，需要 `equals`/`hashCode`/`copy` | `data class` |
| 需要继承 | `open class` |
| 单例 | `object` |
| 封闭类型层次 | `sealed class` / `sealed interface` |
| 无状态工具函数 | `object` 或顶层函数 |
| 仅值语义，无身份 | `inline class` / `value class` |

```kotlin
// 推荐
data class Segment(
    val id: Int,
    val originAndDestinationPair: OriginAndDestinationPair,
)

sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
    data class Error(val message: String) : DataState<Nothing>()
}

// 不推荐：当一个普通 class 本该是 data class 时
class Segment(
    val id: Int,
    val originAndDestinationPair: OriginAndDestinationPair,
) // 丢失了 copy(), equals(), componentN()...
```

### 6.2 this 的使用

仅在编译器要求时使用 `this`（构造函数参数消歧、lambda 中引用接收者）。

```kotlin
// 推荐
class Person(val name: String) {
    fun greet() = "Hello, $name"
}

// 也推荐：需要消歧时
class Person(val name: String) {
    constructor(name: String, age: Int) : this(name) {
        // this 用来区分参数和属性
    }
}

// 不推荐：不必要的 this
class Person(val name: String) {
    fun greet() = "Hello, ${this.name}" // this 多余
}
```

### 6.3 计算属性（val/get）

只读计算属性省略 `get()`：

```kotlin
// 推荐
val diameter: Double
    get() = radius * 2

// 不推荐
val diameter: Double
    get() {
        return radius * 2
    }
```

### 6.4 final / open

Kotlin 中类和方法**默认是 final** 的。保持默认，仅在确实需要继承时显式标记 `open`。

```kotlin
// 推荐：默认 final，不需要额外标注
class BookingAdapter : RecyclerView.Adapter<BookingAdapter.VH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = /* ... */
}

// 需要被子类化时才加 open
open class BaseViewModel {
    open fun onCleared() { /* ... */ }
}
```

---

## 7. 函数声明

### 7.1 短函数放一行

```kotlin
// 推荐
fun clear() = prefs.edit().clear().apply()

override fun getItemCount() = segments.size
```

### 7.2 长签名每个参数一行

```kotlin
// 推荐
fun loadData(
    page: Int,
    pageSize: Int = PAGE_SIZE,
    forceRefresh: Boolean = false,
    onResult: (DataState<BookingResponse>) -> Unit,
): Job {
    // 实现
}

// 不推荐：单行超长
fun loadData(page: Int, pageSize: Int = PAGE_SIZE, forceRefresh: Boolean = false, onResult: (DataState<BookingResponse>) -> Unit): Job {
```

### 7.3 返回类型

- 公开 API 函数**必须**显式声明返回类型
- 私有函数可使用类型推断
- 无返回值的函数省略 `: Unit`

```kotlin
// 推荐
fun fetchBookingData(): BookingResponse { /* ... */ }
fun updateView() { /* 隐式 Unit */ }

// 不推荐
fun fetchBookingData() = service.fetchData() // 公开函数不应依赖推断
fun updateView(): Unit { /* ... */ } // 冗余的 : Unit
```

---

## 8. Lambda 表达式

### 8.1 尾随 lambda

最后一个参数是 lambda 时，使用尾随 lambda 语法：

```kotlin
// 推荐
viewModel.data.observe(this) { state ->
    when (state) {
        is DataState.Success -> updateUI(state.data)
    }
}

scope.launch {
    val data = withContext(Dispatchers.IO) { service.fetchBookingData() }
    _data.postValue(DataState.Success(data))
}

// 不推荐
viewModel.data.observe(this, Observer { state ->
    updateUI(state)
})
```

### 8.2 lambda 参数命名

lambda 参数使用描述性名称：

```kotlin
// 推荐
segments.filter { segment -> segment.id > 5 }

// 可接受：单参数简单场景用 it
segments.filter { it.id > 5 }

// 不推荐：多层嵌套时仍用 it（歧义）
list.map { it.segments.filter { it.id > 5 } } // 哪个 it？
```

### 8.3 隐式返回

单表达式 lambda 利用隐式返回：

```kotlin
// 推荐
segments.sortedBy { it.id }

// 不推荐
segments.sortedBy { segment -> return@sortedBy segment.id }
```

### 8.4 链式调用换行

链式 lambda 调用保持缩进清晰：

```kotlin
// 推荐
bookingList
    .filter { it.segments.isNotEmpty() }
    .map { it.segments.first() }
    .sortedBy { it.id }

// 不推荐
bookingList.filter { it.segments.isNotEmpty() }.map { it.segments.first() }.sortedBy { it.id }
```

---

## 9. 类型

### 9.1 优先使用 Kotlin 原生类型

```kotlin
// 推荐
val width = 120.0                           // Double
val widthString = "$width"                 // String
val items = listOf("A", "B")               // List<String>

// 不推荐
val width: java.lang.Double = 120.0
val widthString = java.lang.String.format("%f", width)
val items = java.util.ArrayList<String>()
```

### 9.2 类型推断

能明确推断时省略类型标注：

```kotlin
// 推荐
val message = "Click me"
val segments = viewModel.data.value
val adapter = BookingAdapter()

// 不推荐
val message: String = "Click me"
val segments: List<Segment>? = viewModel.data.value
val adapter: BookingAdapter = BookingAdapter()
```

### 9.3 空集合

优先使用工厂方法而非构造函数：

```kotlin
// 推荐
val empty = emptyList<Segment>()
val mutable = mutableListOf<Segment>()

// 不推荐
val empty = listOf<Segment>()
val mutable = ArrayList<Segment>()
```

### 9.4 类型别名

用 `typealias` 简化复杂类型签名：

```kotlin
// 推荐
typealias BookingCallback = (DataState<BookingResponse>) -> Unit

fun loadData(onResult: BookingCallback) { /* ... */ }

// 不推荐
fun loadData(onResult: (DataState<BookingResponse>) -> Unit) { /* ... */ }
```

---

## 10. 内存管理

### 10.1 避免 Activity / Fragment 引用泄漏

```kotlin
// 推荐：ViewModel 不持有 View/Context 引用
class BookingViewModel(app: Application) : AndroidViewModel(app) {
    private val dm = BookingDataManager.getInstance(app)
    // 不持有 Activity、View、Drawable 等
}

// 不推荐：ViewModel 持有 Context
class BookingViewModel(private val context: Context) : ViewModel() {
    // Context 可能指向已销毁的 Activity
}
```

### 10.2 协程作用域

使用正确的协程作用域，避免泄漏：

```kotlin
// 推荐：ViewModel 用 viewModelScope
class BookingViewModel(app: Application) : AndroidViewModel(app) {
    fun load() {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) { fetchData() }
            _data.postValue(DataState.Success(data))
        }
    }
}

// 推荐：DataManager 用自定义 SupervisorJob
class BookingDataManager(context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun destroy() {
        scope.cancel() // 手动管理生命周期
    }
}
```

### 10.3 lambda 中的捕获

lambda 隐式捕获外部引用时注意泄漏风险：

```kotlin
// 推荐：Fragment 中使用 viewLifecycleOwner
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.data.collect { state -> updateUI(state) }
}

// 不推荐：可能泄漏 Fragment
lifecycleScope.launch {
    viewModel.data.collect { state -> updateUI(state) }
}
```

---

## 11. 访问控制

### 11.1 优先使用 private

```kotlin
// 推荐
class BookingDataManager(context: Context) {
    private val service = BookingService(context)
    private val cache = BookingCache(context)

    val data: LiveData<DataState<BookingResponse>> // 公开，只读
        get() = _data
    private val _data = MutableLiveData<DataState<BookingResponse>>()
}

// 不推荐
class BookingDataManager(context: Context) {
    val service = BookingService(context)   // 不应该暴露内部
    val _data = MutableLiveData<DataState<BookingResponse>>() // 可变状态不应该直接暴露
}
```

### 11.2 访问修饰符放在最前面

```kotlin
// 推荐
private val message = "Great Scott!"

class TimeMachine {
    private lateinit var fluxCapacitor: FluxCapacitor
}

// 不推荐
val private message = "Great Scott!" // 语法错误; Android 注解放 private 前
@Volatile private var instance: BookingDataManager? = null // @Volatile 在 private 前
```

### 11.3 internal 用于模块内公开

跨模块使用时，不想完全公开的 API 用 `internal`：

```kotlin
// 推荐
internal fun generateMockSegments(count: Int): List<Segment> { /* ... */ }

// 不推荐：完全公开内部工具函数
fun generateMockSegments(count: Int): List<Segment> { /* ... */ }
```

---

## 12. 控制流

### 12.1 for-in 优于 while

```kotlin
// 推荐
for (i in 0 until 3) { println("Hello three times") }
for ((index, person) in attendeeList.withIndex()) { /* ... */ }
for (i in (0..3).reversed()) { /* ... */ }

// 不推荐
var i = 0
while (i < 3) {
    println("Hello three times")
    i++
}
```

### 12.2 when 优于长 if-else

```kotlin
// 推荐
when (state) {
    is DataState.Loading -> showLoading()
    is DataState.Success -> showData(state.data)
    is DataState.Error -> showError(state.message)
}

// 不推荐
if (state is DataState.Loading) {
    showLoading()
} else if (state is DataState.Success) {
    showData(state.data)
} else if (state is DataState.Error) {
    showError(state.message)
}
```

### 12.3 if-else 表达式（Kotlin 三元等价物）

仅在增加清晰度时使用，不要嵌套过深：

```kotlin
// 推荐
val label = if (count == 1) "item" else "items"

// 推荐：复杂时用明确的多分支
val playerMark = if (player == current) "X" else "O"

// 不推荐
val result = if (a) { if (b) { x } else { y } } else { if (c) { z } else { w } }
// 应提取为函数或 when
```

---

## 13. 黄金路径

> 条件判断时，左侧应是"黄金"路径或"快乐"路径。不要嵌套 `if` 语句。允许多个 return。
> Kotlin 中用 `?:`、`?.let`、`require()` / `check()` 和提前 `return` 实现 Guard 语义。

```kotlin
// 推荐：提前 return，左侧是主线逻辑
fun processBooking(data: BookingResponse?): Frequencies {
    val response = data ?: throw IllegalStateException("No booking data")
    val segments = response.segments
    if (segments.isEmpty()) {
        Log.w(TAG, "Empty segments")
        return emptyList()
    }
    // 使用 response 和 segments 做核心计算
    return computeFrequencies(segments)
}

// 推荐：?.let 链式处理可选值
fun updateCache(data: BookingResponse?) {
    data?.let { cache.save(it) } ?: Log.d(TAG, "No data to cache")
}

// 不推荐：深层嵌套
fun processBooking(data: BookingResponse?) {
    if (data != null) {
        val segments = data.segments
        if (segments.isNotEmpty()) {
            // 三层缩进了才到核心逻辑
            computeFrequencies(segments)
        }
    }
}
```

多重提前退出的守卫语句，每个条件一行：

```kotlin
// 推荐
fun validateAndProcess(price: Double, percent: Int, user: User?) {
    require(price > 0) { "Price must be positive" }
    require(percent in 0..100) { "Invalid percent" }
    val customer = user ?: return
    // 此时所有前提条件都已满足
    applyDiscount(customer, price, percent)
}
```

---

## 14. 分号

**不使用分号。** Kotlin 不需要分号结尾，也不要在同一行写多个语句。

```kotlin
// 推荐
val swift = "not a scripting language"

// 不推荐
val swift = "not a scripting language";
```

---

## 15. 括号

条件表达式**不需要**括号，应当省略。仅在复杂表达式中有助于可读性时保留：

```kotlin
// 推荐
if (name == "Hello") {
    println("World")
}

// 不推荐
if ((name == "Hello") && (age > 18)) {
    println("World")
}

// 可接受：括号增加复杂表达式的清晰度
val isEligible = (age >= 18 && hasLicense) || (isSupervisor && age >= 16)
```

---

## 16. 项目特定约定

### 16.1 MVVM 架构

```
View (Activity/Fragment) ──observes──> ViewModel ──delegates──> DataManager ──reads──> Service/Cache
```

- **View** — 仅 UI 绑定和用户交互，不包含业务逻辑。使用 ViewBinding。
- **ViewModel** — 暴露 LiveData，委托给 DataManager。不持有 View 引用。
- **Data** — 封装缓存策略、分页逻辑、数据源。用 sealed class 统一状态。

### 16.2 资源命名

| 资源类型 | 命名格式 | 示例 |
|---------|---------|------|
| Layout（Activity） | `activity_*.xml` | `activity_main.xml` |
| Layout（Item） | `item_*.xml` | `item_segment.xml` |
| ID（View） | camelCase | `recyclerView`, `swipeRefresh`, `tvError` |
| 字符串 | snake_case | `booking_title`, `error_network` |
| 颜色 | 按用途命名 | `colorPrimary`, `textError` |

### 16.3 依赖注入

当前项目使用手动单例（`getInstance()`），后续扩展推荐使用 Hilt。

### 16.4 日志规范

- TAG 常量定义在伴生对象中
- `Log.d` 用于调试信息，`Log.e` 用于错误
- 生产代码应移除或降级详细日志

```kotlin
companion object {
    private const val TAG = "BookingDataManager"
}
```

### 16.5 包结构

```
com.accenture.booking/
├── model/          # 数据模型（data class）
├── data/           # 数据层
│   ├── cache/      #   本地缓存
│   └── service/    #   数据源
├── view/           # UI 层
│   └── adapter/    #   RecyclerView Adapter
└── viewmodel/      # ViewModel 层
```

---

## 17. Code Review 检查清单

- [ ] 命名是否清晰描述意图？（无缩写、无语病）
- [ ] 是否优先使用 `val` 和不可变数据？
- [ ] 是否有 `!!` 强制解包？（应当没有）
- [ ] 函数是否单一职责、足够简短（< 30 行为佳）？
- [ ] 是否正确处理了空安全和异常？
- [ ] View 层是否混入了业务逻辑？
- [ ] 协程作用域是否正确？（无泄漏风险）
- [ ] 硬编码字符串/数字是否提取为常量？
- [ ] 是否有未使用的导入或死代码？
- [ ] 是否使用了 `*` 通配符导入？
- [ ] `when` / `if-else` 分支是否穷尽？
- [ ] 文件组织是否符合包结构规范？

---

> 本规范参照 [Kodeco Swift Style Guide](https://github.com/kodecocodes/swift-style-guide) 的完整结构和精神，
> 针对 Kotlin / Android 平台进行等价适配，供项目团队参考执行。
