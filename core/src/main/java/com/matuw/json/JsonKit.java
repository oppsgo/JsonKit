package com.matuw.json;

import com.matuw.json.adapter.JsonAdapter;
import com.matuw.json.adapter.JsonAdapterFactory;
import com.matuw.json.config.JsonOptions;
import com.matuw.json.reflect.JsonTypeReference;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * @author Shihwan
 */
public class JsonKit implements JsonAdapter {

    private static JsonKit defaultInstance;

    public static void installDefault(@NotNull JsonKit instance) {
        defaultInstance = Utils.requireNonNull(instance);
    }

    @NotNull
    public static JsonKit getInstance() {
        return Utils.requireNonNull(defaultInstance);
    }

    /**
     * 使用默认的 {@link JsonAdapterFactory} 前提是需要先调用 {@link #installDefault(JsonKit)}
     */
    @NotNull
    public static JsonKit.Builder newBuilder() {
        return new Builder();
    }

    @NotNull
    public static JsonKit.Builder newBuilder(@NotNull JsonAdapterFactory factory) {
        return new Builder(factory);
    }

    private final JsonAdapterFactory factory;
    private final JsonOptions options;

    private JsonAdapter adapter;

    protected JsonKit(Builder builder) {
        this.factory = builder.factory;
        this.options = builder.options;
    }

    public JsonOptions getOptions() {
        return new JsonOptions(options);
    }

    private JsonAdapter getAdapter() {
        JsonAdapter adapter = this.adapter;
        if (adapter == null) {
            this.adapter = factory.create(options);
        }
        return Utils.requireNonNull(this.adapter);
    }

    @Override
    public String toJson(Object object) {
        return getAdapter().toJson(object);
    }

    @Override
    public <T> T fromJson(String json, @NotNull Class<T> clazz) {
        return getAdapter().fromJson(json, clazz);
    }

    @Override
    public <T> T fromJson(String json, @NotNull Type type) {
        return getAdapter().fromJson(json, type);
    }

    @Override
    public <T> T fromJson(String json, @NotNull JsonTypeReference<T> reference) {
        return getAdapter().fromJson(json, reference);
    }

    public static class Builder {
        private final JsonAdapterFactory factory;
        private JsonOptions options;

        protected Builder() {
            this.factory = getInstance().factory;
        }

        private Builder(JsonAdapterFactory factory) {
            this.factory = Utils.requireNonNull(factory);
        }

        public Builder setOptions(JsonOptions options) {
            this.options = options;
            return this;
        }

        public JsonKit build() {
            return new JsonKit(this);
        }
    }
}
