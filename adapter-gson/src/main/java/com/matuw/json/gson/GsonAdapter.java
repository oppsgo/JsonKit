package com.matuw.json.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.matuw.json.adapter.JsonAdapter;
import com.matuw.json.config.JsonOptions;
import com.matuw.json.reflect.JsonTypeReference;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * @author Shihwan
 */
public class GsonAdapter implements JsonAdapter {
    private final Gson gson;

    public GsonAdapter(JsonOptions options) {
        if (options == null) {
            this.gson = new Gson();
        } else {
            GsonBuilder builder = new GsonBuilder();

            // 应用空值序列化配置
            if (options.isSerializeNulls()) {
                builder.serializeNulls();
            }
            this.gson = builder.create();
        }

    }

    @Override
    public String toJson(Object object) {
        return gson.toJson(object);
    }

    @Override
    public <T> T fromJson(String json, @NotNull Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    @Override
    public <T> T fromJson(String json, @NotNull Type type) {
        return gson.fromJson(json, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromJson(String json, @NotNull JsonTypeReference<T> reference) {
        return gson.fromJson(json, (TypeToken<? extends T>) TypeToken.get(reference.getType()));
    }
}
