package io.github.oppsgo.json.moshi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;

/**
 * Reusable {@link JsonAdapter.Factory} for {@link MoshiAdapter}.
 * Options are fixed at construction; {@link #create()} always returns the same adapter instance.
 * <p>
 * Prefer registering this factory once so Moshi can keep its {@code JsonAdapter} cache warm;
 * avoid per-request {@code new MoshiAdapter()}. This factory does not retain a JsonKit
 * {@code BindingCache}.
 * <pre>{@code
 * JsonKit.setDefault(MoshiAdapterFactory.of());
 * JsonKit.setDefault(MoshiAdapterFactory.of(options));
 * }</pre>
 */
public class MoshiAdapterFactory implements JsonAdapter.Factory {

    private final JsonAdapter adapter;

    private MoshiAdapterFactory(@NotNull JsonAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Factory with {@link JsonOptions#defaults()}.
     */
    @NotNull
    public static MoshiAdapterFactory of() {
        return of(JsonOptions.defaults());
    }

    /**
     * Factory that reuses one {@link MoshiAdapter} built from {@code options}.
     * {@code options == null} is treated as {@link JsonOptions#defaults()}.
     */
    @NotNull
    public static MoshiAdapterFactory of(@Nullable JsonOptions options) {
        return new MoshiAdapterFactory(new MoshiAdapter(options));
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
