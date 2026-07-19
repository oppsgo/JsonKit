# JsonKit

[中文文档](README.zh-CN.md)

JsonKit is a lightweight JSON facade for JVM and Android. It exposes a single `JsonAdapter` contract and lets you swap backends (Gson, Fastjson 1.x, Fastjson2) through manually registered factories—no SPI, no reflection-based discovery.

**Package:** `io.github.oppsgo.json`  
**Adapters:** `io.github.oppsgo.json.gson` · `.fastjson` · `.fastjson2`

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

## Installation

### JitPack

1. Add the repository:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

2. Add a module (replace `Tag` with a release tag such as `1.0.1`):

```kotlin
// Core only
implementation("com.github.oppsgo.JsonKit:core:Tag")

// Gson backend (pulls in :core transitively)
implementation("com.github.oppsgo.JsonKit:json-gson:Tag")

// Fastjson2 (recommended)
implementation("com.github.oppsgo.JsonKit:json-fastjson2:Tag")

// Fastjson 1.x (compatibility)
implementation("com.github.oppsgo.JsonKit:json-fastjson:Tag")
```

Build status / artifacts: [jitpack.io/#oppsgo/JsonKit](https://jitpack.io/#oppsgo/JsonKit)

### Local / composite build

```kotlin
implementation(project(":core"))
implementation(project(":adapter:json-gson"))
// or :adapter:json-fastjson2 / :adapter:json-fastjson
```

## Quick start

### 1. Register factories at startup

`name == null` selects the default factory. Named entries hold alternate options or engines. Each `XxxAdapterFactory.of(...)` reuses one adapter for the lifetime of that factory.

```java
JsonOptions options = new JsonOptions.Builder()
        .setSerializeNulls(false)
        .build();

JsonKit.setDefault(GsonAdapterFactory.of(options));
// Equivalent: JsonKit.register(null, GsonAdapterFactory.of(options));

JsonKit.register("api", Fastjson2AdapterFactory.of(apiOptions));
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
JsonAdapter local = new GsonAdapter(options);
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

Place models on the unified annotations in `:core`. All three adapters honor them at runtime:

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

## Roadmap

- Compile-time rewriting of unified annotations into engine-native annotations
