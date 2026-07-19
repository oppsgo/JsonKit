package io.github.oppsgo.json.fastjson2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;

/**
 * Reusable {@link JsonAdapter.Factory} for {@link Fastjson2Adapter}.
 * Options are fixed at construction; {@link #create()} always returns the same adapter instance.
 * <p>
 * Static entry points are {@link #of()} / {@link #of(JsonOptions)} to avoid clashing with
 * {@link JsonAdapter.Factory#create()}.
 * <pre>{@code
 * JsonKit.setDefault(Fastjson2AdapterFactory.of());
 * JsonKit.setDefault(Fastjson2AdapterFactory.of(options));
 * }</pre>
 */
public class Fastjson2AdapterFactory implements JsonAdapter.Factory {

    private final JsonAdapter adapter;

    private Fastjson2AdapterFactory(@NotNull JsonAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Factory with {@link JsonOptions#defaults()}.
     */
    @NotNull
    public static Fastjson2AdapterFactory of() {
        return of(JsonOptions.defaults());
    }

    /**
     * Factory that reuses one {@link Fastjson2Adapter} built from {@code options}.
     * {@code options == null} is treated as {@link JsonOptions#defaults()}.
     */
    @NotNull
    public static Fastjson2AdapterFactory of(@Nullable JsonOptions options) {
        return new Fastjson2AdapterFactory(new Fastjson2Adapter(options));
    }

    /**
     * Returns the shared adapter instance for this factory.
     */
    @NotNull
    @Override
    public JsonAdapter create() {
        return adapter;
    }
}
