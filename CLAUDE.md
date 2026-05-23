# CLAUDE.md — Accenture Booking Demo

> 编码规范参考 [kodecocodes/swift-style-guide](https://github.com/kodecocodes/swift-style-guide)，结合 Kotlin/Android 项目特点制定。

---

## 命名规范

### 基本原则
- **描述性优先**：名称应清晰表达意图，避免缩写（如 `onCreateViewHolder` 而非 `onCreateVH`）
- **长度与作用域匹配**：作用域越大，名称越具描述性；局部变量可适当简短

### 类型与文件
| 类型 | 命名风格 | 示例 |
|------|---------|------|
| 类 / 接口 / 对象 | PascalCase | `BookingDataManager`, `DataState` |
| 函数 / 方法 | camelCase | `loadMore()`, `updateSegments()` |
| 变量 / 参数 / 属性 | camelCase | `bookingData`, `lastVisible` |
| 常量 (const / top-level) | UPPER_SNAKE_CASE | `MAX_PAGES`, `PAGE_SIZE` |
| 伴生对象 | `companion object` | 常量放伴生对象内 |
| 包名 | 小写 + 点分隔 | `com.accenture.booking.data` |

### 布尔属性
使用 `is` / `has` / `can` 前缀：`isLoading`, `hasMore`, `canIssueTicketChecking`

### 缩写规则
- 常见缩写首字母大写时保持一致：`Id` 而非 `ID`，`Xml` 而非 `XML`
- 避免生僻缩写：`originAndDestinationPair` 而非 `odPair`

---

## 代码格式

### 缩进与换行
- **缩进**：4 空格（Kotlin 标准），不使用 Tab
- **行宽**：建议不超过 120 字符
- **换行**：参数过多时每个参数一行（trailing comma 推荐）

```kotlin
// 推荐：多参数换行
data class Segment(
    val id: Int,
    val originAndDestinationPair: OriginAndDestinationPair,
)

// 避免：单行过长
fun someMethod(a: Int, b: String, c: Boolean, d: List<String>, e: Map<String, Any>)
```

### 大括号
- 左大括号在同一行（K&R 风格）
- `if/else/for/when/try` 即使单行也必须用大括号

```kotlin
// 推荐
if (condition) {
    doSomething()
} else {
    doOther()
}

// 避免：单行省略大括号
if (condition) doSomething()
```

### 空格
- 二元运算符两侧加空格：`a + b`, `x = y`
- 冒号前无空格、后有空格的类型标注：`val name: String`
- 函数参数逗号后加空格：`fun foo(a: Int, b: String)`
- 泛型尖括号内无空格：`List<Segment>`

### 空行
- 类内方法之间用空行分隔
- 逻辑段落之间用空行分隔
- 文件末尾保留一个空行

---

## 项目结构

### 包组织（按 MVVM 分层）

```
com.accenture.booking/
├── model/          # 数据模型（data class）
├── data/           # 数据层
│   ├── cache/      #   本地缓存
│   └── service/    #   数据源（API / 本地文件）
├── view/           # UI 层
│   └── adapter/    #   RecyclerView Adapter
└── viewmodel/      # ViewModel 层
```

### 文件规则
- **一个文件一个类**（顶级类），内部类除外
- 文件名与主类名一致
- 相关联的 data class 可放在同一文件（如 `BookingModels.kt`）

---

## MVVM 架构规范

```
View (Activity/Fragment) ──observes──> ViewModel ──delegates──> DataManager ──reads──> Service/Cache
```

### View 层职责
- 仅负责 UI 绑定和用户交互
- 通过 LiveData 观察 ViewModel
- 不包含业务逻辑
- 使用 ViewBinding（非 DataBinding）

### ViewModel 层职责
- 暴露 LiveData 给 View
- 持有 DataManager 引用
- 不持有 Context（使用 `AndroidViewModel` 时除外）
- 不引用 View 对象

### Data 层职责
- 封装数据获取策略（缓存优先、分页等）
- 使用 `sealed class` 统一状态表达

```kotlin
sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
    data class Error(val message: String) : DataState<Nothing>()
}
```

---

## Kotlin 最佳实践

### 不可变性优先
- 优先使用 `val` 而非 `var`
- 优先使用 `data class` 配合 `copy()` 实现不可变数据

### Lateinit 使用
- `lateinit var` 仅用于无法在构造函数初始化的场景（如 Activity 的 binding）
- 能用 `by lazy {}` 解决的优先用 lazy

### 协程
- ViewModel 层使用 `viewModelScope`
- Data 层使用自定义 `CoroutineScope` + `SupervisorJob()`
- 耗时操作统一用 `Dispatchers.IO`
- 使用 `withContext` 切换调度器，避免嵌套 launch

### 空安全
- 优先使用 `?.`、`?:`、`let` 等安全操作符
- 避免 `!!` 强制解包

---

## 注释与文档

### 原则
- **解释"为什么"而非"是什么"** — 代码自说明做了什么
- 复杂算法或非直观逻辑必须注释
- 不保留被注释掉的代码，直接删除

### KDoc
- 公开 API（public 函数/类）使用 KDoc
- 格式：`/** 描述 */`

### 日志
- 使用 `android.util.Log`，按级别：`Log.d`（调试）、`Log.e`（错误）
- TAG 常量定义在伴生对象中
- 生产环境 remove 或降级调试日志

---

## Code Review 检查清单

- [ ] 命名是否清晰描述意图？
- [ ] 函数是否单一职责、足够简短？
- [ ] 是否优先使用 `val` / 不可变数据？
- [ ] 是否正确处理空安全（无 `!!`）？
- [ ] View 层是否包含业务逻辑？（不应包含）
- [ ] 协程作用域是否正确？
- [ ] 硬编码字符串/数字是否提取为常量？
- [ ] 文件组织是否符合包结构规范？
