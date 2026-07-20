# JsonKit 使用教程

面向 JVM / Android 的完整用法说明。仓库概览见 [README.zh-CN.md](../README.zh-CN.md)；English guide: [guide.md](guide.md)。

---

## 目录

1. [它解决什么问题](#1-它解决什么问题)
2. [安装依赖](#2-安装依赖)
3. [核心概念](#3-核心概念)
4. [选择引擎](#4-选择引擎)
5. [启动注册（推荐）](#5-启动注册推荐)
6. [序列化与反序列化](#6-序列化与反序列化)
7. [JsonOptions](#7-jsonoptions)
8. [统一注解](#8-统一注解)
9. [字段策略与日期格式](#9-字段策略与日期格式)
10. [不经 JsonKit 直接使用](#10-不经-jsonkit-直接使用)
11. [最佳实践](#11-最佳实践)
12. [常见问题](#12-常见问题)
13. [API 速查](#13-api-速查)

---

## 1. 它解决什么问题

业务代码只依赖 `JsonAdapter` / `JsonKit`，不直接依赖 Gson / Fastjson / Moshi。换引擎时改**启动注册**即可，调用点不用改。

| 方式 | 说明 |
|------|------|
| **推荐** | 进程启动时 `JsonKit.setDefault(...)` / `register(...)`，业务侧 `getDefault()` / `get(name)` |
| **可选** | 直接 `new GsonAdapter()` / `new MoshiAdapter(options)` 等，不走注册表 |

**不会**自动 SPI 扫描 classpath；必须手动注册（适合 Android 多进程、ProGuard 场景）。

---

## 2. 安装依赖

### 2.1 JitPack

在 `settings.gradle.kts`（或根 `build.gradle`）加入：

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

在模块中引入（版本换成实际 tag，例如 `1.0.4`）：

```kotlin
// 方案 A：聚合坐标（会拉到仓库发布的各模块，生产更建议只选一个 adapter）
implementation("com.github.oppsgo:json-kit:1.0.4")

// 方案 B：只选需要的模块（推荐）
implementation("com.github.oppsgo.json-kit:json-fastjson2:1.0.4") // 含 core 传递依赖
// 或
implementation("com.github.oppsgo.json-kit:json-gson:1.0.4")
implementation("com.github.oppsgo.json-kit:json-moshi:1.0.4")
implementation("com.github.oppsgo.json-kit:json-fastjson:1.0.4") // Fastjson 1.x 兼容
```

构建状态：[jitpack.io/#oppsgo/json-kit](https://jitpack.io/#oppsgo/json-kit)

### 2.2 本地 composite

```kotlin
implementation(project(":core"))
implementation(project(":adapter:json-fastjson2"))
```

---

## 3. 核心概念

```
┌─────────────┐     create()      ┌──────────────┐
│ JsonKit     │ ───────────────► │ JsonAdapter  │ ──► toJson / fromJson
│ (注册表)     │   Factory        │ (门面契约)     │
└─────────────┘                   └──────────────┘
                                        ▲
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
              GsonAdapter        Fastjson2Adapter      MoshiAdapter
              FastjsonAdapter
```

| 类型 | 作用 |
|------|------|
| `JsonKit` | 静态注册表：默认 / 具名 Factory |
| `JsonAdapter.Factory` | `create()` 返回 Adapter；库内 `XxxAdapterFactory` **始终复用同一实例** |
| `JsonAdapter` | 统一序列化 API |
| `JsonOptions` | 创建 Adapter / Factory 时绑定，**不能按次调用改** |
| `JsonTypeReference<T>` | 捕获 `List<T>` / `Map<K,V>` 等泛型 |

包名：`io.github.oppsgo.json`（注解在 `.annotation`，策略在 `.convert`，引擎在 `.gson` / `.fastjson2` / …）。

---

## 4. 选择引擎

| 引擎 | 模块 / Factory | 适合 |
|------|----------------|------|
| **Fastjson2** | `json-fastjson2` / `Fastjson2AdapterFactory` | 新项目默认推荐（Fastjson 线）。带注解反序列化走 BindingMeta 驱动的 `ObjectReader`（单次绑定）；无注解 DTO 走原生 `parseObject`。 |
| **Gson** | `json-gson` / `GsonAdapterFactory` | 已有 Gson 栈、偏保守依赖 |
| **Moshi** | `json-moshi` / `MoshiAdapterFactory` | Android / OkHttp 生态；反射桥接 JsonKit 注解，无需 Kotlin codegen |
| **Fastjson 1.x** | `json-fastjson` / `FastjsonAdapterFactory` | 仅兼容旧依赖 |

同一进程可注册多个：默认一个引擎，具名再挂另一套 options 或另一引擎。

---

## 5. 启动注册（推荐）

### 5.1 最小例子

```java
import io.github.oppsgo.json.JsonKit;
import io.github.oppsgo.json.fastjson2.Fastjson2AdapterFactory;

public final class JsonBootstrap {
    public static void init() {
        // serializeNulls 默认 false：null 字段不写出
        JsonKit.setDefault(Fastjson2AdapterFactory.of());
    }
}
```

Android：

```java
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        JsonKit.setDefault(Fastjson2AdapterFactory.of());
    }
}
```

### 5.2 带 Options + 具名实例

```java
import io.github.oppsgo.json.JsonKit;
import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.fastjson2.Fastjson2AdapterFactory;
import io.github.oppsgo.json.gson.GsonAdapterFactory;

JsonOptions omitNulls = new JsonOptions.Builder()
        .setSerializeNulls(false)
        .build();
JsonOptions keepNulls = new JsonOptions.Builder()
        .setSerializeNulls(true)
        .build();

JsonKit.setDefault(Fastjson2AdapterFactory.of(omitNulls));
JsonKit.register("debug", GsonAdapterFactory.of(keepNulls));
```

- `setDefault(factory)` ≡ `register(null, factory)`
- `get("debug")` 取具名；`getDefault()` / `get(null)` 取默认
- **Options 在 `of(options)` 时固化**，之后改 Builder 不影响已创建的 Factory

### 5.3 业务侧取用

```java
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.JsonKit;

JsonAdapter json = JsonKit.getDefault();
String body = json.toJson(user);
User again = json.fromJson(body, User.class);
```

---

## 6. 序列化与反序列化

模型请使用 **public 字段**（契约测试与 Moshi 反射桥均按字段绑定；不要依赖仅有的 private + getter 假设）。

```java
public class User {
    public String name;
    public int age;
    public String email;

    public User() {} // Moshi / 部分转换路径需要无参构造

    public User(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }
}
```

### 6.1 对象 ↔ String

```java
JsonAdapter json = JsonKit.getDefault();

String text = json.toJson(user);
User u = json.fromJson(text, User.class);
```

### 6.2 泛型：List / Map

```java
import io.github.oppsgo.json.reflect.JsonTypeReference;
import java.util.List;
import java.util.Map;

List<User> list = json.fromJson(arrayJson, new JsonTypeReference<List<User>>() {});
Map<String, User> map = json.fromJson(objectJson, new JsonTypeReference<Map<String, User>>() {});
```

也可用 `fromJson(json, type)` 传入反射 `Type`。

### 6.3 Reader / Writer（不关闭流）

```java
import java.io.StringReader;
import java.io.StringWriter;

StringWriter writer = new StringWriter();
json.toJson(user, writer);           // 不会 close writer
String encoded = writer.toString();

User decoded = json.fromJson(new StringReader(encoded), User.class); // 不会 close reader
```

适合与网络缓冲、文件流对接；**关闭由调用方负责**。

### 6.4 null 行为

- 根值为 `null`：各引擎可能返回 `"null"` 字符串或 `null`，反序列化 `null` / `"null"` 通常得到 Java `null`
- 字段为 `null`：由 `JsonOptions.serializeNulls` 控制是否写出（默认省略）

---

## 7. JsonOptions

当前仅有一个开关：

| 选项 | 默认 | 含义 |
|------|------|------|
| `serializeNulls` | `false` | `true` 时把值为 null 的字段写进 JSON |

```java
JsonOptions options = new JsonOptions.Builder()
        .setSerializeNulls(true)
        .build();

JsonKit.setDefault(GsonAdapterFactory.of(options));
// 或 new MoshiAdapter(options);
```

`adapter.getOptions()` 返回**防御性拷贝**，改拷贝不会影响 Adapter。

---

## 8. 统一注解

注解在 `:core` 的 `io.github.oppsgo.json.annotation`，**四个引擎都认**。只作用在 **字段**（`@JsonIgnoreProperties` 在类型上）。

### 8.1 `@JsonProperty` — 改名

```java
public class Profile {
    @JsonProperty("user_name")
    public String userName;
}
// 序列化 / 反序列化 JSON 键均为 user_name
```

### 8.2 `@JsonAlias` — 反序列化别名

```java
public class Profile {
    @JsonProperty("user_name")
    @JsonAlias({"userName", "uname"})
    public String userName;
}
// 写出仍是 user_name；读入可接受 user_name / userName / uname
```

### 8.3 `@JsonIgnore` — 忽略

```java
public class Account {
    public String name;

    @JsonIgnore                 // 双向忽略（默认 serialize=true, deserialize=true）
    public String password;

    @JsonIgnore(serialize = true, deserialize = false)  // 只出不进示意：按需组合
    public String writeOnlyHint;
}
```

### 8.4 `@JsonIgnoreProperties` — 类级忽略名

```java
@JsonIgnoreProperties({"debug", "internalToken"})
public class Person {
    public String name;
    public String debug;           // 按字段名或 JSON 名匹配后双向忽略
}
```

子类会继承该注解（`@Inherited`）。

### 8.5 组合示例

```java
@JsonIgnoreProperties({"debug"})
public class Person {
    @JsonProperty("person_id")
    @JsonAlias({"pid"})
    public String id;

    @JsonProperty("display_name")
    @JsonAlias({"name"})
    public String displayName;

    @JsonIgnore
    public String internalToken;

    public String debug;
}

public class Employee extends Person {
    public String title;
}
```

---

## 9. 字段策略与日期格式

### 9.1 `@JsonSerialize` / `@JsonDeserialize`

对**单个字段**自定义值转换。策略工作在 **JSON-tree** 上（`String` / `Number` / `Boolean` / `List` / `Map`），与引擎无关。

```java
import io.github.oppsgo.json.annotation.JsonSerialize;
import io.github.oppsgo.json.annotation.JsonDeserialize;
import io.github.oppsgo.json.convert.JsonFieldSerializer;
import io.github.oppsgo.json.convert.JsonFieldDeserializer;
import java.lang.reflect.Type;

public class UpperSerializer implements JsonFieldSerializer<String> {
    @Override
    public Object serialize(String value) {
        return value.toUpperCase();
    }
}

public class LowerDeserializer implements JsonFieldDeserializer<String> {
    @Override
    public String deserialize(Object jsonValue, Type fieldType) {
        return String.valueOf(jsonValue).toLowerCase();
    }
}

public class Payload {
    @JsonSerialize(using = UpperSerializer.class)
    @JsonDeserialize(using = LowerDeserializer.class)
    public String secret;
}
```

规则：

- 策略类必须有 **public 无参构造**；Adapter 会缓存实例
- 字段 Java 值为 `null`、或 JSON 为 null 时 **不会**调用策略（仍走 `serializeNulls` / ignore）
- 可只标序列化或只标反序列化
- 可与 `@JsonProperty` 同用：注解管键名，策略管值

Android R8：请 keep 被注解引用的策略类及其无参构造。

### 9.2 `@JsonFormat` — 日期时间

```java
import io.github.oppsgo.json.annotation.JsonFormat;
import java.util.Date;

public class Event {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    public Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER) // epoch 毫秒
    public Date updatedAt;
}
```

| 属性 | 说明 |
|------|------|
| `shape` | `STRING`（默认）或 `NUMBER` |
| `pattern` | `STRING` 时必填非空，如 `yyyy-MM-dd HH:mm:ss` |
| `timezone` | 空则按 **UTC** |

支持类型：`java.util.Date`、`Calendar`、`java.time.Instant`。用在不支持的类型上会抛错（不会静默忽略）。

**优先级（同一方向）：** `@JsonSerialize` / `@JsonDeserialize` **高于** `@JsonFormat`。

---

## 10. 不经 JsonKit 直接使用

适合单测或局部临时实例：

```java
import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.gson.GsonAdapter;
import io.github.oppsgo.json.moshi.MoshiAdapter;
import io.github.oppsgo.json.fastjson2.Fastjson2Adapter;

JsonAdapter a = new GsonAdapter();
JsonAdapter b = new MoshiAdapter(new JsonOptions.Builder().setSerializeNulls(true).build());
JsonAdapter c = new Fastjson2Adapter(options);
```

注意：短生命周期反复 `new XxxAdapter()` 在 Fastjson 路径上会重复建 `BindingCache`，生产环境请用 Factory 长驻。

---

## 11. 最佳实践

1. **进程启动注册一次**，业务只 `getDefault()` / `get(name)`。
2. **优先 `XxxAdapterFactory.of(options)`**，不要每个请求 `new` Adapter。
3. 模型用 **public 字段 + 无参构造**（Moshi 桥、部分转换路径需要）。
4. Fastjson / Fastjson2：每个 Adapter 自带注解 `BindingCache`，务必长生命周期复用。
5. Gson / Moshi：依赖引擎自身 TypeAdapter 缓存；有字段策略 / `@JsonFormat` 的类型走反射桥接路径。
6. 新项目 Fastjson 线用 **Fastjson2**；1.x 仅兼容。
7. ProGuard / R8：`:core` 的 JAR 已内嵌极简规则（保留注解属性与 JsonKit 注解类型）。Gson / Moshi / Fastjson 等引擎规则由各库自带。**业务 Model、策略类**仍需在 App 中自行 keep（与单独用 Gson 相同）。

---

## 12. 常见问题

**Q: 为什么没有自动发现 Gson？**  
A: 设计为手动注册，避免 Android 多 dex / 混淆下的脆弱 SPI。

**Q: `getDefault()` 抛了？**  
A: 未 `setDefault` / `register(null, …)`。先注册再取用。

**Q: 改了 JsonOptions 为什么没生效？**  
A: Options 在 Factory/`new Adapter` 时已快照。需重新 `of(新 options)` 并重新注册。

**Q: 泛型 List 变成 LinkedTreeMap / JSONObject？**  
A: 请用 `JsonTypeReference` 或带泛型实参的 `Type`，不要只传裸 `List.class`。

**Q: Moshi 报 Date 需要 JsonAdapter？**  
A: 给 `Date` 字段加 `@JsonFormat`，或用 `@JsonSerialize`/`@JsonDeserialize`；不要对平台类型裸依赖默认反射。

**Q: 策略类 Instantiation 失败？**  
A: 必须提供 public 无参构造；异常信息会带策略类名。

---

### R8 / 混淆

`:core` JAR 内含 `META-INF/proguard/jsonkit-core.pro`（R8 自动合并）：

- 保留 `RuntimeVisibleAnnotations` / `AnnotationDefault` / `Signature`
- 保留 `io.github.oppsgo.json.annotation.**` 注解类型

引擎规则勿重复添加。业务 DTO / `@JsonSerialize` 策略类请在 App 中 keep，例如：

```proguard
-keepclassmembers class com.example.app.model.** {
    <fields>;
    <init>();
}
-keep class * implements io.github.oppsgo.json.convert.JsonFieldSerializer { <init>(); }
-keep class * implements io.github.oppsgo.json.convert.JsonFieldDeserializer { <init>(); }
```

---

## 13. API 速查

### JsonKit

| 方法 | 说明 |
|------|------|
| `setDefault(Factory)` | 注册默认 |
| `register(String, Factory)` | 具名注册；`null` 名 = 默认 |
| `getDefault()` / `get(String)` | `factory.create()` |
| `hasDefault()` / `has(String)` | 是否已注册 |
| `clear()` | 清空（测试） |

### JsonAdapter

| 方法 | 说明 |
|------|------|
| `toJson(Object)` / `toJson(Object, Writer)` | 序列化；不关闭 Writer |
| `fromJson(String\|Reader, Class\|Type\|JsonTypeReference)` | 反序列化；不关闭 Reader |
| `getOptions()` | Options 拷贝 |

### 注解一览

| 注解 | 目标 | 作用 |
|------|------|------|
| `@JsonProperty` | 字段 | JSON 键名 |
| `@JsonAlias` | 字段 | 反序列化别名 |
| `@JsonIgnore` | 字段 | 忽略（可分方向） |
| `@JsonIgnoreProperties` | 类型 | 按名忽略 |
| `@JsonSerialize` | 字段 | 自定义序列化策略 |
| `@JsonDeserialize` | 字段 | 自定义反序列化策略 |
| `@JsonFormat` | 字段 | 日期 pattern / epoch |

---

更多设计背景见仓库 [README.zh-CN.md](../README.zh-CN.md)。问题与示例欢迎提 Issue。
