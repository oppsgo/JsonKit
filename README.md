# JsonKit

[![Release](https://jitpack.io/v/oppsgo/json-kit.svg)](https://jitpack.io/#oppsgo/json-kit)

[中文文档](README.zh-CN.md) · **[Usage guide (EN)](docs/guide.md)** · **[使用教程（中文）](docs/guide.zh-CN.md)** · [Changelog](CHANGELOG.md)

JsonKit is a lightweight JSON facade for JVM and Android. It exposes a single `JsonAdapter` contract and lets you swap backends (Gson, Fastjson 1.x, Fastjson2, Moshi) through manually registered factories—no SPI, no reflection-based discovery.

**Package:** `io.github.oppsgo.json`  
**Adapters:** `io.github.oppsgo.json.gson` · `.fastjson` · `.fastjson2` · `.moshi`  
**Repo:** [github.com/oppsgo/json-kit](https://github.com/oppsgo/json-kit) · [gitee.com/oppsgo/json-kit](https://gitee.com/oppsgo/json-kit)

## Features

- **Stable facade** — Call sites depend on `JsonAdapter` / `JsonKit`, not on a concrete engine.
- **Manual factory registry** — Android-friendly; register at process start, resolve by name at runtime.
- **Reusable adapters** — Engine factories close over `JsonOptions` and return the same `JsonAdapter` instance from `create()`.
- **Unified annotations** — rename / ignore / alias, plus `@JsonSerialize` / `@JsonDeserialize` / `@JsonFormat`.
- **Streaming API** — `Reader` / `Writer` overloads; adapters never close caller streams.
- **Generics** — `JsonTypeReference<T>` for `List` / `Map` and similar types.

## Modules

| Module | Role |
|--------|------|
| `:core` | `JsonKit`, `JsonAdapter`, `JsonOptions`, `JsonTypeReference`, annotations, field converters |
| `:adapter:json-gson` | Gson + `GsonAdapterFactory` |
| `:adapter:json-fastjson2` | Fastjson2 + `Fastjson2AdapterFactory` (**recommended** Fastjson line) |
| `:adapter:json-fastjson` | Fastjson 1.x + `FastjsonAdapterFactory` (compatibility) |
| `:adapter:json-moshi` | Moshi + `MoshiAdapterFactory` (reflective JsonKit bridge) |

## Installation

### JitPack (GitHub)

1. Add the repository:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

2. Add a dependency (replace with a release tag such as `1.0.4`):

```kotlin
// Prefer one adapter module in production:
implementation("com.github.oppsgo.json-kit:json-fastjson2:1.0.4")

// Also available: json-gson / json-moshi / json-fastjson
// Aggregate (all modules): com.github.oppsgo:json-kit:1.0.4
```

Build status: [jitpack.io/#oppsgo/json-kit](https://jitpack.io/#oppsgo/json-kit)

### Local / composite build

```kotlin
implementation(project(":core"))
implementation(project(":adapter:json-fastjson2"))
```

## Quick start

```java
// 1) Once at process start
JsonKit.setDefault(Fastjson2AdapterFactory.of());

// 2) Everywhere else
JsonAdapter json = JsonKit.getDefault();
String payload = json.toJson(user);
User again = json.fromJson(payload, User.class);
```

With options / a second named engine:

```java
JsonOptions options = new JsonOptions.Builder().setSerializeNulls(false).build();
JsonKit.setDefault(GsonAdapterFactory.of(options));
JsonKit.register("api", Fastjson2AdapterFactory.of());
JsonAdapter api = JsonKit.get("api");
```

**Full walkthrough** (generics, streams, every annotation, field strategies, dates, FAQ):  
→ **[docs/guide.md](docs/guide.md)** · [中文教程](docs/guide.zh-CN.md)

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/guide.md](docs/guide.md) | Complete English usage guide |
| [docs/guide.zh-CN.md](docs/guide.zh-CN.md) | 完整中文使用教程 |
| [CHANGELOG.md](CHANGELOG.md) | Version history |
| [README.zh-CN.md](README.zh-CN.md) | Chinese project overview |

## Design notes

- Prefer long-lived `XxxAdapterFactory.of(...)` instances; avoid per-request `new XxxAdapter()`.
- Factory methods are named `of()` so they do not clash with `JsonAdapter.Factory#create()`.
- Prefer **Fastjson2** over Fastjson 1.x; Moshi uses a reflective field bridge (no Kotlin codegen).
- Fastjson adapters own a per-instance annotation `BindingCache`.
