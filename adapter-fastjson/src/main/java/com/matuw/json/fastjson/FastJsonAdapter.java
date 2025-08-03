package com.matuw.json.fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.matuw.json.adapter.JsonAdapter;
import com.matuw.json.config.JsonOptions;
import com.matuw.json.reflect.JsonTypeReference;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shihwan
 */
public class FastJsonAdapter implements JsonAdapter {

    private final SerializerFeature[] serializeFeatures;

    public FastJsonAdapter(JsonOptions options) {
        if (options == null) {
            this.serializeFeatures = null;
        } else {
            List<SerializerFeature> features = new ArrayList<SerializerFeature>();

            // 处理空值序列化配置
            if (options.isSerializeNulls()) {
                // FastJSON 1.x中序列化null的核心特性
                features.add(SerializerFeature.WriteMapNullValue);
            }
            this.serializeFeatures = features.toArray(new SerializerFeature[0]);
        }
    }

    @Override
    public String toJson(Object object) {
        if (serializeFeatures == null) {
            return JSON.toJSONString(object);
        }

        return JSON.toJSONString(object, serializeFeatures);
    }

    @Override
    public <T> T fromJson(String json, @NotNull Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    @Override
    public <T> T fromJson(String json, @NotNull Type type) {
        return JSON.parseObject(json, type);
    }

    @Override
    public <T> T fromJson(String json, @NotNull final JsonTypeReference<T> reference) {
        return JSON.parseObject(json, reference.getType());
    }
}
