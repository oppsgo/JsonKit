# JsonKit

[![Release](https://jitpack.io/v/oppsgo/json-kit.svg)](https://jitpack.io/#oppsgo/json-kit)

[English](README.md)

JsonKit 是面向 JVM / Android 的轻量 JSON 门面库。对外统一 `JsonAdapter` 契约，通过**手动注册**的 Factory 切换 Gson、Fastjson 1.x、Fastjson2、Moshi 等实现——无 SPI、无运行时自动发现，适配 Android 进程模型。

**包名：** `io.github.oppsgo.json`  
**Adapter：** `io.github.oppsgo.json.gson` · `.fastjson` · `.fastjson2` · `.moshi`  
**仓库：** [github.com/oppsgo/json-kit](https://github.com/oppsgo/json-kit) · [gitee.com/oppsgo/json-kit](https://gitee.com/oppsgo/json-kit)

## 特性

- **稳定门面** — 业务只依赖 `JsonAdapter` / `JsonKit`，与具体引擎解耦。
- **手动 Factory 注册表** — 启动时注册，运行时按名称取用；适合 Android。
- **可复用 Adapter** — 引擎侧 Factory 在创建时绑定 `JsonOptions`，`create()` 始终返回同一实例。
- **统一注解** — `@JsonProperty`、`@JsonIgnore`、`@JsonAlias`、`@JsonIgnoreProperties`，各引擎运行时解释。
- **流式 API** — 提供 `Reader` / `Writer` 重载；流生命周期由调用方管理（Adapter 不关闭流）。
- **泛型反序列化** — `JsonTypeReference<T>` 支持 `List` / `Map` 等参数化类型。

## 模块

| 模块 | 职责 |
|------|------|
| `:core` | `JsonKit`、`JsonAdapter`、`JsonOptions`、`JsonTypeReference`、统一注解 |
| `:adapter:json-gson` | Gson 实现 + `GsonAdapterFactory` |
| `:adapter:json-fastjson2` | Fastjson2 实现 + `Fastjson2AdapterFactory`（**推荐** Fastjson 线） |
| `:adapter:json-fastjson` | Fastjson 1.x 实现 + `FastjsonAdapterFactory`（兼容线） |
| `:adapter:json-moshi` | Moshi 实现 + `MoshiAdapterFactory`（反射桥接 JsonKit 注解） |

## 依赖

### JitPack（GitHub）

从 [github.com/oppsgo/json-kit](https://github.com/oppsgo/json-kit) 构建。

1. 添加仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

2. 添加依赖（将版本换成标签，例如 `1.0.4`）：

```kotlin
// 全部模块（聚合）
implementation("com.github.oppsgo:json-kit:1.0.4")

// 或按需选用单个模块：
implementation("com.github.oppsgo.json-kit:core:1.0.4")
implementation("com.github.oppsgo.json-kit:json-gson:1.0.4")       // Gson（含 core）
implementation("com.github.oppsgo.json-kit:json-fastjson2:1.0.4")  // Fastjson2（推荐）
implementation("com.github.oppsgo.json-kit:json-fastjson:1.0.4")   // Fastjson 1.x
implementation("com.github.oppsgo.json-kit:json-moshi:1.0.4")      // Moshi（含 core）
```

构建状态 / 产物：[jitpack.io/#oppsgo/json-kit](https://jitpack.io/#oppsgo/json-kit)

### 本地 / composite build

```kotlin
implementation(project(":core"))
implementation(project(":adapter:json-gson"))
// 或 :adapter:json-fastjson2 / :adapter:json-fastjson / :adapter:json-moshi
```

## 快速开始

### 1. 启动时注册 Factory

`name == null` 表示默认 Factory；具名条目用于另一套 options 或引擎。`XxxAdapterFactory.of()` / `of(JsonOptions)` 在 Factory 生命周期内复用同一个 Adapter。

```java
// 默认 options（serializeNulls = false）
JsonKit.setDefault(GsonAdapterFactory.of());
// 等价于：JsonKit.register(null, GsonAdapterFactory.of());

// 带 options
JsonOptions options = new JsonOptions.Builder()
        .setSerializeNulls(false)
        .build();
JsonKit.setDefault(GsonAdapterFactory.of(options));

JsonKit.register("api", Fastjson2AdapterFactory.of(apiOptions));
// 或：Fastjson2AdapterFactory.of() / MoshiAdapterFactory.of()
```

### 2. 业务侧取用

```java
JsonAdapter json = JsonKit.getDefault();
String payload = json.toJson(model);
User user = json.fromJson(payload, User.class);

JsonAdapter api = JsonKit.get("api");
```

### 3. 不经门面直接使用

```java
JsonAdapter local = new GsonAdapter();           // 默认
JsonAdapter custom = new GsonAdapter(options); // 或 MoshiAdapter / Fastjson2Adapter
```

## 注册表 API

| 方法 | 说明 |
|------|------|
| `setDefault(Factory)` | 注册默认 Factory（等同 `register(null, …)`） |
| `register(String, Factory)` | 按名称注册；`null` 表示默认 |
| `getDefault()` / `get(String)` | 通过 `Factory.create()` 获取 Adapter |
| `hasDefault()` / `has(String)` | 是否已注册 |
| `clear()` | 清空（测试 / 进程清理） |

## 注解

模型使用 `:core` 中的统一注解，各 Adapter 均在运行时生效：

```java
public class Profile {
    @JsonProperty("user_name")
    public String userName;

    @JsonIgnore
    public String password;

    @JsonAlias({"nick", "nickname"})
    public String displayName;
}
```

## 设计说明

- 是否缓存 Adapter 由 Factory 决定；库内 `XxxAdapterFactory` 固定复用单实例。
- 静态入口命名为 `of()` / `of(JsonOptions)`，避免与 `JsonAdapter.Factory#create()` 冲突。
- 新项目优先使用 **Fastjson2**；仅在兼容需求下保留 Fastjson 1.x。
- **Moshi** 通过反射字段桥接 JsonKit 注解（无需 Kotlin `@JsonClass` codegen）。与 Gson 类似，依赖 Moshi 自身的 adapter 缓存，不额外保留 JsonKit `BindingCache`。
