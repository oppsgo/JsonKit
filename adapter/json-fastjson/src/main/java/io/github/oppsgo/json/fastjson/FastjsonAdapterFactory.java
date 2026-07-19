package io.github.oppsgo.json.fastjson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;

/**
 * Reusable {@link JsonAdapter.Factory} for {@link FastjsonAdapter}.
 * Options are fixed at construction; {@link #create()} always returns the same adapter instance.
 * <p>
 * Static entry points are {@link #of()} / {@link #of(JsonOptions)} to avoid clashing with
 * {@link JsonAdapter.Factory#create()}.
 * <pre>{@code
 * JsonKit.setDefault(FastjsonAdapterFactory.of());
 * JsonKit.setDefault(FastjsonAdapterFactory.of(options));
 * }</pre>
 */
public class FastjsonAdapterFactory implements JsonAdapter.Factory {

    private final JsonAdapter adapter;

    private FastjsonAdapterFactory(@NotNull JsonAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Factory with {@link JsonOptions#defaults()}.
     */
    @NotNull
    public static FastjsonAdapterFactory of() {
        return of(JsonOptions.defaults());
    }

    /**
     * Factory that reuses one {@link FastjsonAdapter} built from {@code options}.
     * {@code options == null} is treated as {@link JsonOptions#defaults()}.
     */
    @NotNull
    public static FastjsonAdapterFactory of(@Nullable JsonOptions options) {
        return new FastjsonAdapterFactory(new FastjsonAdapter(options));
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
