# JsonKit

[![Release](https://jitpack.io/v/oppsgo/json-kit.svg)](https://jitpack.io/#oppsgo/json-kit)

[中文文档](README.zh-CN.md)

JsonKit is a lightweight JSON facade for JVM and Android. It exposes a single `JsonAdapter` contract and lets you swap backends (Gson, Fastjson 1.x, Fastjson2, Moshi) through manually registered factories—no SPI, no reflection-based discovery.

**Package:** `io.github.oppsgo.json`  
**Adapters:** `io.github.oppsgo.json.gson` · `.fastjson` · `.fastjson2` · `.moshi`  
**Repo:** [github.com/oppsgo/json-kit](https://github.com/oppsgo/json-kit) · [gitee.com/oppsgo/json-kit](https://gitee.com/oppsgo/json-kit)

## Features

- **Stable facade** — Call sites depend on `JsonAdapter` / `JsonKit`, not on a concrete engine.
- **Manual factory registry** — Android-friendly; register at process start, resolve by name at runtime.
- **Reusable adapters** — Engine factories close over `JsonOptions` and return the same `JsonAdapter` instance from `create()`.
- **Unified annotations** — `@JsonProperty`, `@JsonIgnore`, `@JsonAlias`, `@JsonIgnoreProperties` interpreted by every adapter.
- **Streaming API** — `Reader` / `Writer` overloads; callers own stream lifecycle (adapters never close them).
- **Generics** — `JsonTypeReference<T>` for `List` / `Map` and similar parameterized types.

## Modules

| Module | Role |
|--------|------|
| `:core` | `JsonKit`, `JsonAdapter`, `JsonOptions`, `JsonTypeReference`, annotations |
| `:adapter:json-gson` | Gson backend + `GsonAdapterFactory` |
| `:adapter:json-fastjson2` | Fastjson2 backend + `Fastjson2AdapterFactory` (**recommended** Fastjson line) |
| `:adapter:json-fastjson` | Fastjson 1.x backend + `FastjsonAdapterFactory` (compatibility) |
| `:adapter:json-moshi` | Moshi backend + `MoshiAdapterFactory` (reflective JsonKit annotation bridge) |

## Installation

### JitPack (GitHub)

Published from [github.com/oppsgo/json-kit](https://github.com/oppsgo/json-kit).

1. Add the repository:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

2. Add a dependency (replace version with a release tag such as `1.0.4`):

```kotlin
// All modules (aggregate)
implementation("com.github.oppsgo:json-kit:1.0.4")

// Or pick one module:
implementation("com.github.oppsgo.json-kit:core:1.0.4")
implementation("com.github.oppsgo.json-kit:json-gson:1.0.4")       // Gson (+ core)
implementation("com.github.oppsgo.json-kit:json-fastjson2:1.0.4")  // Fastjson2 (recommended)
implementation("com.github.oppsgo.json-kit:json-fastjson:1.0.4")   // Fastjson 1.x
implementation("com.github.oppsgo.json-kit:json-moshi:1.0.4")      // Moshi (+ core)
```

Build status / artifacts: [jitpack.io/#oppsgo/json-kit](https://jitpack.io/#oppsgo/json-kit)

### Local / composite build

```kotlin
implementation(project(":core"))
implementation(project(":adapter:json-gson"))
// or :adapter:json-fastjson2 / :adapter:json-fastjson / :adapter:json-moshi
```

## Quick start

### 1. Register factories at startup

`name == null` selects the default factory. Named entries hold alternate options or engines. Each `XxxAdapterFactory.of()` / `of(JsonOptions)` reuses one adapter for the lifetime of that factory.

```java
// Defaults (serializeNulls = false)
JsonKit.setDefault(GsonAdapterFactory.of());
// Equivalent: JsonKit.register(null, GsonAdapterFactory.of());

// With options
JsonOptions options = new JsonOptions.Builder()
        .setSerializeNulls(false)
        .build();
JsonKit.setDefault(GsonAdapterFactory.of(options));

JsonKit.register("api", Fastjson2AdapterFactory.of(apiOptions));
// Or: Fastjson2AdapterFactory.of() / MoshiAdapterFactory.of()
```

### 2. Resolve and use

```java
JsonAdapter json = JsonKit.getDefault();
String payload = json.toJson(model);
User user = json.fromJson(payload, User.class);

JsonAdapter api = JsonKit.get("api");
```

### 3. Use an adapter without the facade

```java
JsonAdapter local = new GsonAdapter();           // defaults
JsonAdapter custom = new GsonAdapter(options); // or MoshiAdapter / Fastjson2Adapter
```

## Registry API

| Method | Description |
|--------|-------------|
| `setDefault(Factory)` | Registers the default factory (`register(null, …)`) |
| `register(String, Factory)` | Registers under `name`; `null` means default |
| `getDefault()` / `get(String)` | Obtains an adapter via `Factory.create()` |
| `hasDefault()` / `has(String)` | Registration checks |
| `clear()` | Clears all entries (tests / teardown) |

## Annotations

Place models on the unified annotations in `:core`. All adapters honor them at runtime:

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

## Design notes

- Factories decide caching; library `XxxAdapterFactory` implementations always reuse a single adapter.
- Static factory methods are named `of()` / `of(JsonOptions)` so they do not clash with `JsonAdapter.Factory#create()`.
- Prefer **Fastjson2** over Fastjson 1.x for new work; keep 1.x only when compatibility requires it.
- **Moshi** uses a reflective field bridge for JsonKit annotations (no Kotlin `@JsonClass` codegen required). Like Gson, it relies on Moshi’s own adapter cache rather than a JsonKit `BindingCache`.
