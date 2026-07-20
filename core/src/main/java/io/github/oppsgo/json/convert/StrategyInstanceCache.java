package io.github.oppsgo.json.convert;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-adapter cache of strategy instances (public no-arg constructors).
 */
public final class StrategyInstanceCache {

    private final ConcurrentHashMap<Class<?>, Object> instances =
            new ConcurrentHashMap<Class<?>, Object>();

    public <T extends JsonFieldSerializer<?>> T serializer(Class<T> type) {
        return type.cast(getOrCreate(type));
    }

    public <T extends JsonFieldDeserializer<?>> T deserializer(Class<T> type) {
        return type.cast(getOrCreate(type));
    }

    private Object getOrCreate(Class<?> type) {
        Object existing = instances.get(type);
        if (existing != null) {
            return existing;
        }
        Object created = instantiate(type);
        Object raced = instances.putIfAbsent(type, created);
        return raced != null ? raced : created;
    }

    private static Object instantiate(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Cannot instantiate strategy " + type.getName()
                            + "; require a public no-arg constructor",
                    e);
        }
    }
}
