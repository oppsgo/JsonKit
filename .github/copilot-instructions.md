# Copilot instructions (JsonKit)

Follow the shared project conventions:

- [docs/conventions/README.md](../docs/conventions/README.md)
- Especially [docs/conventions/java-jdk7-style.md](../docs/conventions/java-jdk7-style.md)

Summary:

1. Diamond generics: `List<T> x = new ArrayList<>();` — no redundant `<T>` on the right.
2. Prefer `XX<?>` / `XX<T>` over raw `XX` (e.g. `Class<?>`, `List<?>`, `Constructor<?>`) unless a concrete `T` assignment truly needs a typed `Class<T>`.
3. After null-check on `Integer`/`Boolean`/…, use the wrapper directly; do not manually unbox.
4. Library Java (`:core`, adapters): JDK 1.7 language style — no lambdas/Streams unless the file already requires them.

Full detail: `docs/conventions/`. Repo-wide agent entry: `AGENTS.md`.
