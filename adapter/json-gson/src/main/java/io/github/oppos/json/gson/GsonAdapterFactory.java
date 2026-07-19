package io.github.oppos.json.gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.oppos.json.JsonOptions;
import io.github.oppos.json.adapter.JsonAdapter;

/**
 * Reusable {@link JsonAdapter.Factory} for {@link GsonAdapter}, similar to Retrofit's
 * {@code GsonConverterFactory}: options are fixed at construction; {@link #create()} always
 * returns the same adapter instance.
 * <p>
 * Static entry points are named {@link #of()} (not {@code create()}) so they do not clash
 * with {@link JsonAdapter.Factory#create()}.
 * <pre>{@code
 * JsonKit.setDefault(GsonAdapterFactory.of());
 * JsonKit.setDefault(GsonAdapterFactory.of(options));
 * }</pre>
 */
public class GsonAdapterFactory implements JsonAdapter.Factory {

    private final JsonAdapter adapter;

    private GsonAdapterFactory(@NotNull JsonAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Factory with {@link JsonOptions#defaults()}.
     */
    @NotNull
    public static GsonAdapterFactory of() {
        return of(JsonOptions.defaults());
    }

    /**
     * Factory that reuses one {@link GsonAdapter} built from {@code options}.
     * {@code options == null} is treated as {@link JsonOptions#defaults()}.
     */
    @NotNull
    public static GsonAdapterFactory of(@Nullable JsonOptions options) {
        return new GsonAdapterFactory(new GsonAdapter(options));
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
