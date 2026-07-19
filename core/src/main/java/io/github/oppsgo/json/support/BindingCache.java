package io.github.oppsgo.json.support;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter-owned cache of {@link BindingMeta} by {@link Class}.
 * <p>
 * Prefer one long-lived adapter (e.g. via {@code XxxAdapterFactory.of()}); each adapter
 * instance holds its own cache. Creating many short-lived adapters multiplies memory use.
 */
public final class BindingCache {

    private final ConcurrentHashMap<Class<?>, BindingMeta> cache = new ConcurrentHashMap<>();
    private final boolean enabled;

    /**
     * Creates a caching {@link BindingCache} (normal production use).
     */
    public BindingCache() {
        this(true);
    }

    /**
     * @param enabled when {@code false}, every {@link #get(Class)} rescans and does not store
     *                (for benchmarks / diagnostics only; prefer {@link #BindingCache()} in apps)
     */
    public BindingCache(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns metadata for {@code type}. When caching is enabled, scans once on first miss.
     * <p>
     * Uses {@code get} + {@code putIfAbsent} (not {@code computeIfAbsent}) so the code stays
     * within Android API 19 / Animal Sniffer signatures. Concurrent first-miss races may scan
     * twice; only one meta is retained.
     */
    public BindingMeta get(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type == null");
        }
        if (!enabled) {
            return BindingMeta.scan(type);
        }
        BindingMeta cached = cache.get(type);
        if (cached != null) {
            return cached;
        }
        BindingMeta created = BindingMeta.scan(type);
        BindingMeta raced = cache.putIfAbsent(type, created);
        return raced != null ? raced : created;
    }
}
