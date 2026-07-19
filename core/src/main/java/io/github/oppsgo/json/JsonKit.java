package io.github.oppsgo.json;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.github.oppsgo.json.adapter.JsonAdapter;

/**
 * Manual registry of {@link JsonAdapter.Factory} instances (Android-friendly; no SPI).
 * <p>
 * API-wise, {@code name == null} means the default factory. Internally the default is stored
 * under {@link #DEFAULT_KEY} ({@link ConcurrentHashMap} cannot use null keys).
 * <pre>{@code
 * JsonKit.setDefault(defaultFactory);          // same as register(null, defaultFactory)
 * JsonKit.register("api", apiFactory);
 *
 * JsonKit.getDefault();                        // same as get(null)
 * JsonKit.get("api");
 * }</pre>
 */
public final class JsonKit {

    /**
     * Internal map key for the default factory. Not part of the public name space;
     * prefer {@code null} / {@link #setDefault} at the API level.
     */
    private static final String DEFAULT_KEY = "io.github.oppsgo.json.JsonKit.DEFAULT";

    private static final ConcurrentHashMap<String, JsonAdapter.Factory> FACTORIES = new ConcurrentHashMap<>();

    private JsonKit() {
    }

    /**
     * Registers the default factory. Equivalent to {@code register(null, factory)}.
     */
    public static void setDefault(@NotNull JsonAdapter.Factory factory) {
        register(null, factory);
    }

    /**
     * Registers a factory. {@code name == null} means the default factory;
     * any non-null name (including {@code ""}) is stored under that key as-is.
     */
    public static void register(@Nullable String name, @NotNull JsonAdapter.Factory factory) {
        FACTORIES.put(mapKey(name), Objects.requireNonNull(factory));
    }

    /**
     * Adapter from the default factory. Equivalent to {@code get(null)}.
     */
    @NotNull
    public static JsonAdapter getDefault() {
        return get(null);
    }

    /**
     * Adapter from the factory registered under {@code name}.
     * {@code name == null} selects the default factory (same as {@link #getDefault()}).
     * <p>
     * Each call invokes {@link JsonAdapter.Factory#create()}; whether the same adapter
     * instance is returned depends on the factory (library {@code XxxAdapterFactory}
     * implementations reuse one instance).
     */
    @NotNull
    public static JsonAdapter get(@Nullable String name) {
        JsonAdapter.Factory factory = FACTORIES.get(mapKey(name));
        if (factory == null) {
            if (name == null) {
                throw new IllegalStateException(
                        "No default factory. Call JsonKit.setDefault(factory) "
                                + "or JsonKit.register(null, factory) first.");
            }
            throw new IllegalStateException(
                    "No factory named \"" + name + "\". Call JsonKit.register(name, factory) first.");
        }
        return factory.create();
    }

    /**
     * Whether the default factory is registered. Equivalent to {@code has(null)}.
     */
    public static boolean hasDefault() {
        return has(null);
    }

    /**
     * Whether a factory is registered for {@code name}.
     * {@code name == null} checks the default factory.
     */
    public static boolean has(@Nullable String name) {
        return FACTORIES.containsKey(mapKey(name));
    }

    /**
     * Clears the default and all named factories (tests / process teardown).
     */
    public static void clear() {
        FACTORIES.clear();
    }

    @NotNull
    private static String mapKey(@Nullable String name) {
        return name == null ? DEFAULT_KEY : name;
    }
}
