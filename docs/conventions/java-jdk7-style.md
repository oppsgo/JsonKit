# Java 编码风格（JDK 1.7 语法）

> 权威约定。Cursor / 其他 AI / 人工改代码均以本文为准。  
> Cursor 侧另有镜像：`.cursor/rules/java-jdk7-style.mdc`（改规范时请同步两边）。

库代码按 **JDK 1.7 语言风格** 编写（工程字节码仍可为 JVM 1.8）。`:core` / adapters 不要引入 Java 8+ 语言特性，除非该文件已有先例且 Animal Sniffer / API 19 允许。

## 菱形泛型：右侧不要重复写类型参数

```java
// ❌ 不推荐
List<String> list = new ArrayList<String>();
Map<String, Object> map = new HashMap<String, Object>();

// ✅ 推荐（Java 7 diamond）
List<String> list = new ArrayList<>();
Map<String, Object> map = new HashMap<>();
```

`LinkedHashMap`、`LinkedHashSet`、`ArrayList` 等同样适用。

## 能写 `XX<?>` 就不要用裸类型 `XX`

泛型类型在变量、字段、参数、返回值上，**只要能带通配符就写出 `<?>`**（或更具体的 `<T>` / `<? extends …>`），不要写裸类型。裸类型仅在确实无法通过编译、或后续必须用具体 `T` 接收且通配符会挡住赋值时才允许。

```java
// ❌ 不推荐 — 裸类型
List list = ...;
Class clazz = obj.getClass();
Constructor ctor = raw.getDeclaredConstructor();

// ✅ 推荐
List<?> list = ...;
Class<?> clazz = obj.getClass();
Constructor<?> ctor = raw.getDeclaredConstructor();
```

```java
// ❌ 特殊情况才用裸类型 / 丢掉通配 — 例如后面必须用具体 T 接收
Class a = ...;
T t = a.getXxx(); // 若写成 Class<?> 会导致 getXxx 返回捕获通配，无法赋给 T

// ✅ 这种场景：用 Class<T> / 带 T 的签名，而不是偷懒写裸 Class
Class<T> a = ...;
T t = a.getXxx();
```

原则：优先 `XX<?>` 或 `XX<T>`；**禁止无必要的裸 `XX`**。

## 包装类：判空后直接用，不要手动拆箱

判空之后依赖自动拆箱，不要写 `.intValue()` / `.longValue()` / `.booleanValue()` / `.doubleValue()` 等，除非当前表达式无法自动拆箱。

```java
// ❌ 不推荐
Integer count = obj.getCount();
if (count != null) {
    int n = count.intValue();
    use(n);
}

// ✅ 推荐
Integer count = obj.getCount();
if (count != null) {
    use(count); // 或 int n = count;
}
```

```java
// ❌ 不推荐
Boolean flag = obj.getFlag();
if (flag != null && flag.booleanValue()) { ... }

// ✅ 推荐
Boolean flag = obj.getFlag();
if (flag != null && flag) { ... }
```

## 库源码避免 Java 8+ 语言特性

主库（`:core`、adapters）中不要使用：

- lambda / 方法引用
- 可用普通循环替代的 `Stream`
- 在公共 API 或热路径上随意使用 `java.util.function.*`（注意 Animal Sniffer / API 19）

优先匿名内部类或普通循环，与现有 JsonKit 风格一致。

## Kotlin

Kotlin 模块可用 Kotlin 惯用法；**与 Java 互通 / 共享的 Java 代码**仍遵守本文。不要写出右侧重复泛型的 Java 构造写法。
