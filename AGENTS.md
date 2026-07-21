# Agent instructions (JsonKit)

When editing this repository, follow the project conventions below. They apply to **all** coding agents (Cursor, Copilot, Claude, Codex, etc.).

## Conventions (source of truth)

- **Index:** [docs/conventions/README.md](docs/conventions/README.md)
- **Java JDK 1.7 style:** [docs/conventions/java-jdk7-style.md](docs/conventions/java-jdk7-style.md)
  - Use diamond `new ArrayList<>()` — do not repeat type args on the right
  - Prefer `XX<?>` (or `XX<T>`) over raw `XX` unless a concrete `T` receive truly requires otherwise
  - After null-check on wrappers, use values directly (no manual `.intValue()` / `.booleanValue()`)
  - Avoid Java 8+ language features (lambdas, Streams) in `:core` / adapter library sources

## Cursor-specific

Cursor also loads [`.cursor/rules/`](.cursor/rules/). Keep those rules aligned with `docs/conventions/`.

## OpenSpec

For feature work, prefer OpenSpec changes under `openspec/changes/` when the user asks for a proposal or structured change.
