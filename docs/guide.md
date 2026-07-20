# JsonKit User Guide

Complete usage for JVM / Android. Project overview: [README.md](../README.md). 中文教程：[guide.zh-CN.md](guide.zh-CN.md).

---

## Contents

1. [What it solves](#1-what-it-solves)
2. [Installation](#2-installation)
3. [Core concepts](#3-core-concepts)
4. [Choosing an engine](#4-choosing-an-engine)
5. [Register at startup](#5-register-at-startup)
6. [Serialize & deserialize](#6-serialize--deserialize)
7. [JsonOptions](#7-jsonoptions)
8. [Unified annotations](#8-unified-annotations)
9. [Field strategies & date format](#9-field-strategies--date-format)
10. [Use without JsonKit](#10-use-without-jsonkit)
11. [Best practices](#11-best-practices)
12. [FAQ](#12-faq)
13. [API cheat sheet](#13-api-cheat-sheet)

---

## 1. What it solves

Call sites depend only on `JsonAdapter` / `JsonKit`, not on Gson / Fastjson / Moshi. Swap engines by changing **startup registration**.

| Style | Description |
|-------|-------------|
| **Recommended** | `JsonKit.setDefault(...)` / `register(...)` once; use `getDefault()` / `get(name)` |
| **Optional** | `new GsonAdapter()` / `new MoshiAdapter(options)` without the registry |

There is **no** SPI auto-discovery—registration is always explicit (Android-friendly).

---

## 2. Installation

### 2.1 JitPack

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

```kotlin
// Aggregate (prefer a single adapter module in production)
implementation("com.github.oppsgo:json-kit:1.0.4")

// Or pick modules (recommended)
implementation("com.github.oppsgo.json-kit:json-fastjson2:1.0.4") // pulls core
implementation("com.github.oppsgo.json-kit:json-gson:1.0.4")
implementation("com.github.oppsgo.json-kit:json-moshi:1.0.4")
implementation("com.github.oppsgo.json-kit:json-fastjson:1.0.4") // Fastjson 1.x
```

Status: [jitpack.io/#oppsgo/json-kit](https://jitpack.io/#oppsgo/json-kit)

### 2.2 Local composite

```kotlin
implementation(project(":core"))
implementation(project(":adapter:json-fastjson2"))
```

---

## 3. Core concepts

| Type | Role |
|------|------|
| `JsonKit` | Static registry (default + named factories) |
| `JsonAdapter.Factory` | `create()` → adapter; library `XxxAdapterFactory` **always reuses one instance** |
| `JsonAdapter` | Shared serialize/deserialize contract |
| `JsonOptions` | Bound when the factory/adapter is created—not per call |
| `JsonTypeReference<T>` | Captures `List<T>` / `Map<K,V>` generics |

Package root: `io.github.oppsgo.json` (annotations `.annotation`, converters `.convert`, engines `.gson` / `.fastjson2` / …).

---

## 4. Choosing an engine

| Engine | Module / factory | When |
|--------|------------------|------|
| **Fastjson2** | `json-fastjson2` / `Fastjson2AdapterFactory` | Default Fastjson line for new work. Annotated deserialize binds via BindingMeta-driven `ObjectReader` (single pass); plain DTOs use native `parseObject`. |
| **Gson** | `json-gson` / `GsonAdapterFactory` | Existing Gson stacks |
| **Moshi** | `json-moshi` / `MoshiAdapterFactory` | Android / OkHttp; reflective JsonKit bridge (no Kotlin codegen) |
| **Fastjson 1.x** | `json-fastjson` / `FastjsonAdapterFactory` | Compatibility only |

You can register several factories (default + named) in one process.

---

## 5. Register at startup

### 5.1 Minimal

```java
JsonKit.setDefault(Fastjson2AdapterFactory.of());
```

Android `Application.onCreate()` is a good place.

### 5.2 Options + named entry

```java
JsonOptions omitNulls = new JsonOptions.Builder().setSerializeNulls(false).build();
JsonOptions keepNulls = new JsonOptions.Builder().setSerializeNulls(true).build();

JsonKit.setDefault(Fastjson2AdapterFactory.of(omitNulls));
JsonKit.register("debug", GsonAdapterFactory.of(keepNulls));
```

- `setDefault(f)` ≡ `register(null, f)`
- Options are **snapshotted** in `of(options)`

### 5.3 Call sites

```java
JsonAdapter json = JsonKit.getDefault();
String body = json.toJson(user);
User again = json.fromJson(body, User.class);
```

---

## 6. Serialize & deserialize

Prefer **public fields** (contract tests and the Moshi bridge bind fields). Provide a **no-arg constructor** for Moshi / some conversion paths.

### 6.1 Object ↔ String

```java
String text = json.toJson(user);
User u = json.fromJson(text, User.class);
```

### 6.2 Generics

```java
List<User> list = json.fromJson(arrayJson, new JsonTypeReference<List<User>>() {});
Map<String, User> map = json.fromJson(objectJson, new JsonTypeReference<Map<String, User>>() {});
```

### 6.3 Reader / Writer

Adapters **must not close** caller-owned streams:

```java
json.toJson(user, writer);
User decoded = json.fromJson(reader, User.class);
```

### 6.4 Nulls

Field-level nulls follow `JsonOptions.serializeNulls` (default: omit).

---

## 7. JsonOptions

| Option | Default | Meaning |
|--------|---------|---------|
| `serializeNulls` | `false` | Write null-valued fields when `true` |

`getOptions()` returns a defensive copy.

---

## 8. Unified annotations

All live in `:core` and are honored by every adapter. Field-targeted except `@JsonIgnoreProperties` (type).

### `@JsonProperty` — rename

```java
@JsonProperty("user_name")
public String userName;
```

### `@JsonAlias` — deserialize-only alternates

```java
@JsonProperty("user_name")
@JsonAlias({"userName", "uname"})
public String userName;
```

### `@JsonIgnore` — skip

```java
@JsonIgnore  // both directions by default
public String password;

@JsonIgnore(serialize = true, deserialize = false)
public String hint;
```

### `@JsonIgnoreProperties` — class-level names

```java
@JsonIgnoreProperties({"debug"})
public class Person {
    public String name;
    public String debug;
}
```

Inherited by subclasses (`@Inherited`).

---

## 9. Field strategies & date format

### `@JsonSerialize` / `@JsonDeserialize`

Per-field conversion via JSON-tree values (`String` / `Number` / `Boolean` / `List` / `Map`):

```java
public class UpperSerializer implements JsonFieldSerializer<String> {
    @Override public Object serialize(String value) { return value.toUpperCase(); }
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

Rules: public no-arg ctor; null Java / JSON null skips callbacks; may be one-sided; works with `@JsonProperty` (name vs value). Keep strategy classes for R8.

### `@JsonFormat`

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
public Date createdAt;

@JsonFormat(shape = JsonFormat.Shape.NUMBER) // epoch millis
public Date updatedAt;
```

- `STRING` requires non-empty `pattern`; empty `timezone` → **UTC**
- Supported: `Date`, `Calendar`, `Instant`
- Unsupported types fail clearly
- **Per direction:** custom strategy **overrides** `@JsonFormat`

---

## 10. Use without JsonKit

```java
JsonAdapter a = new GsonAdapter();
JsonAdapter b = new MoshiAdapter(options);
JsonAdapter c = new Fastjson2Adapter(options);
```

Avoid per-request `new` for Fastjson adapters (each owns a `BindingCache`).

---

## 11. Best practices

1. Register once at process start.
2. Prefer `XxxAdapterFactory.of(options)` over short-lived adapters.
3. Public fields + no-arg constructors on models.
4. Prefer **Fastjson2** over Fastjson 1.x.
5. R8: `:core` embeds minimal `META-INF/proguard` rules (annotation attributes + JsonKit annotation types). Engine rules come from Gson/Moshi/Fastjson. **Keep your own models and strategy classes** in the app (same as plain Gson).

---

## 12. FAQ

**No automatic Gson discovery?** By design—manual registry only.

**`getDefault()` fails?** Register a default factory first.

**Options change ignored?** Snapshot at factory creation; re-`of` and re-register.

**List loses generics?** Use `JsonTypeReference`, not raw `List.class`.

**Moshi + Date?** Use `@JsonFormat` or a field strategy.

**Strategy InstantiationException?** Need a public no-arg constructor.

---

### R8 / ProGuard

`:core` ships `META-INF/proguard/jsonkit-core.pro` (merged by R8 automatically):

- Keep `RuntimeVisibleAnnotations` / `AnnotationDefault` / `Signature`
- Keep `io.github.oppsgo.json.annotation.**` annotation types

Do not duplicate engine rules. Keep app DTOs / field-strategy classes yourself, e.g.:

```proguard
-keepclassmembers class com.example.app.model.** {
    <fields>;
    <init>();
}
-keep class * implements io.github.oppsgo.json.convert.JsonFieldSerializer { <init>(); }
-keep class * implements io.github.oppsgo.json.convert.JsonFieldDeserializer { <init>(); }
```

---

## 13. API cheat sheet

### JsonKit

| Method | Role |
|--------|------|
| `setDefault` / `register` | Install factories (`null` name = default) |
| `getDefault` / `get` | `Factory.create()` |
| `hasDefault` / `has` / `clear` | Query / reset |

### JsonAdapter

| Method | Role |
|--------|------|
| `toJson` | String or Writer (never closes Writer) |
| `fromJson` | String/Reader × Class/Type/JsonTypeReference |
| `getOptions` | Defensive copy |

### Annotations

| Annotation | Target | Role |
|------------|--------|------|
| `@JsonProperty` | field | JSON name |
| `@JsonAlias` | field | Deserialize aliases |
| `@JsonIgnore` | field | Skip (per direction) |
| `@JsonIgnoreProperties` | type | Ignore by name |
| `@JsonSerialize` / `@JsonDeserialize` | field | Custom converters |
| `@JsonFormat` | field | Date pattern / epoch |

---

See also [README.md](../README.md).
