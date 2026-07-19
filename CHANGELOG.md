# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.4] - 2026-07-19

### Added

- Moshi adapter module (`json-moshi`) with reflective JsonKit annotation bridge
- Per-adapter `BindingCache` for Fastjson / Fastjson2 annotation metadata
- Field strategies: `@JsonSerialize` / `@JsonDeserialize` and JSON-tree SPI (`JsonFieldSerializer` / `JsonFieldDeserializer`)
- `@JsonFormat` for date/time pattern or epoch millis (`Date`, `Calendar`, `Instant`)
- Usage guides: [docs/guide.md](docs/guide.md), [docs/guide.zh-CN.md](docs/guide.zh-CN.md)
- Minimal R8 consumer rules in `:core` JAR (`META-INF/proguard/jsonkit-core.pro`)
- Shared contract tests for strategies, format, and stronger `Reader` / `Writer` coverage

### Changed

- README streamlined; points to full guides under `docs/`
- Prefer long-lived `XxxAdapterFactory.of(...)` (documented for BindingCache)

## [1.0.3] - 2026-07-19

Includes work previously shipped as 1.0.1 / 1.0.2 (JitPack bootstrap) plus 1.0.3 fixes.

### Added

- Package `io.github.oppsgo.json` with core + adapter module split
- Manual `JsonKit` Factory registry and JitPack multi-module publishing

### Fixed

- Commit `gradle-wrapper.jar` so JitPack can build the project
- Disable Gradle module metadata so JitPack sources resolve correctly in IDEs

### Changed

- JitPack documentation coordinates pointed at GitHub `oppsgo/json-kit`

## [0.0.1] - 2025-08-03

### Added

- Initial JsonKit facade and early engine adapters

---

[Unreleased]: https://github.com/oppsgo/json-kit/compare/1.0.4...HEAD
[1.0.4]: https://github.com/oppsgo/json-kit/releases/tag/1.0.4
[1.0.3]: https://github.com/oppsgo/json-kit/releases/tag/1.0.3
[0.0.1]: https://github.com/oppsgo/json-kit/releases/tag/0.0.1
