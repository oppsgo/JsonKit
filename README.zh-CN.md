# JsonKit

[![Release](https://jitpack.io/v/oppsgo/json-kit.svg)](https://jitpack.io/#oppsgo/json-kit)

[English](README.md) · **[使用教程（中文）](docs/guide.zh-CN.md)** · **[Usage guide (EN)](docs/guide.md)** · [Changelog](CHANGELOG.md)

JsonKit 是面向 JVM / Android 的轻量 JSON 门面库。对外统一 `JsonAdapter` 契约，通过**手动注册**的 Factory 切换 Gson、Fastjson 1.x、Fastjson2、Moshi 等实现——无 SPI、无运行时自动发现，适配 Android 进程模型。

**包名：** `io.github.oppsgo.json`  
**Adapter：** `io.github.oppsgo.json.gson` · `.fastjson` · `.fastjson2` · `.moshi`  
**仓库：** [github.com/oppsgo/json-kit](https://github.com/oppsgo/json-kit) · [gitee.com/oppsgo/json-kit](https://gitee.com/oppsgo/json-kit)

## 特性

- **稳定门面** — 业务只依赖 `JsonAdapter` / `JsonKit`，与具体引擎解耦。
- **手动 Factory 注册表** — 启动时注册，运行时按名称取用；适合 Android。
- **可复用 Adapter** — Factory 创建时绑定 `JsonOptions`，`create()` 始终返回同一实例。
- **统一注解** — 改名 / 忽略 / 别名，以及 `@JsonSerialize` / `@JsonDeserialize` / `@JsonFormat`。
- **流式 API** — `Reader` / `Writer` 重载；Adapter 不关闭调用方的流。
- **泛型反序列化** — `JsonTypeReference<T>` 支持 `List` / `Map` 等。

## 模块

| 模块 | 职责 |
|------|------|
| `:core` | `JsonKit`、`JsonAdapter`、`JsonOptions`、`JsonTypeReference`、注解、字段转换 SPI |
| `:adapter:json-gson` | Gson + `GsonAdapterFactory` |
| `:adapter:json-fastjson2` | Fastjson2 + `Fastjson2AdapterFactory`（**推荐** Fastjson 线） |
| `:adapter:json-fastjson` | Fastjson 1.x + `FastjsonAdapterFactory`（兼容） |
| `:adapter:json-moshi` | Moshi + `MoshiAdapterFactory`（反射桥接 JsonKit 注解） |

## 依赖

### JitPack（GitHub）

1. 添加仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

2. 添加依赖（版本换成实际 tag，例如 `1.0.4`）：

```kotlin
// 生产环境建议只选一个 adapter：
implementation("com.github.oppsgo.json-kit:json-fastjson2:1.0.4")

// 另有：json-gson / json-moshi / json-fastjson
// 聚合坐标：com.github.oppsgo:json-kit:1.0.4
```

构建状态：[jitpack.io/#oppsgo/json-kit](https://jitpack.io/#oppsgo/json-kit)

### 本地 / composite build

```kotlin
implementation(project(":core"))
implementation(project(":adapter:json-fastjson2"))
```

## 快速开始

```java
// 1）进程启动时注册一次
JsonKit.setDefault(Fastjson2AdapterFactory.of());

// 2）业务侧统一取用
JsonAdapter json = JsonKit.getDefault();
String payload = json.toJson(user);
User again = json.fromJson(payload, User.class);
```

带 Options / 第二套具名引擎：

```java
JsonOptions options = new JsonOptions.Builder().setSerializeNulls(false).build();
JsonKit.setDefault(GsonAdapterFactory.of(options));
JsonKit.register("api", Fastjson2AdapterFactory.of());
JsonAdapter api = JsonKit.get("api");
```

**完整教程**（泛型、流、全部注解、字段策略、日期、FAQ）：  
→ **[docs/guide.zh-CN.md](docs/guide.zh-CN.md)** · [English guide](docs/guide.md)

## 文档

| 文档 | 说明 |
|------|------|
| [docs/guide.zh-CN.md](docs/guide.zh-CN.md) | 完整中文使用教程 |
| [docs/guide.md](docs/guide.md) | Complete English usage guide |
| [CHANGELOG.md](CHANGELOG.md) | 版本变更记录 |
| [README.md](README.md) | English project overview |

## 设计说明

- 优先长生命周期 `XxxAdapterFactory.of(...)`，避免每次请求 `new XxxAdapter()`。
- 静态入口命名为 `of()`，避免与 `JsonAdapter.Factory#create()` 冲突。
- Fastjson 线优先 **Fastjson2**；Moshi 用反射字段桥（无需 Kotlin codegen）。
- Fastjson 适配器为每个实例持有注解 `BindingCache`。
