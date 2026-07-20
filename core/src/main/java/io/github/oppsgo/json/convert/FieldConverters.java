package io.github.oppsgo.json.convert;

import java.lang.reflect.Type;

import io.github.oppsgo.json.support.BindingMeta;

/**
 * Applies effective field converters (custom strategy overrides {@link FormatSpec}).
 */
public final class FieldConverters {

    private FieldConverters() {
    }

    /**
     * Whether any field on {@code meta} needs value conversion on serialize or deserialize.
     */
    public static boolean hasAnyConverter(BindingMeta meta) {
        if (meta == null) {
            return false;
        }
        for (BindingMeta.FieldBinding binding : meta.getFieldBindings()) {
            if (binding.hasSerializeConverter() || binding.hasDeserializeConverter()) {
                return true;
            }
        }
        return false;
    }

    public static Object serialize(
            BindingMeta.FieldBinding binding,
            Object javaValue,
            StrategyInstanceCache cache) {
        if (javaValue == null) {
            throw new IllegalArgumentException("javaValue == null");
        }
        if (binding.serializeUsing != null) {
            JsonFieldSerializer<?> serializer = cache.serializer(binding.serializeUsing);
            return invokeSerialize(serializer, javaValue);
        }
        if (binding.format != null) {
            return JsonFormatConverter.serialize(javaValue, binding.format, binding.fieldType);
        }
        return javaValue;
    }

    public static Object deserialize(
            BindingMeta.FieldBinding binding,
            Object jsonValue,
            Type fieldType,
            StrategyInstanceCache cache) {
        if (jsonValue == null) {
            return null;
        }
        Type effectiveType = fieldType != null ? fieldType : binding.fieldType;
        if (binding.deserializeUsing != null) {
            JsonFieldDeserializer<?> deserializer = cache.deserializer(binding.deserializeUsing);
            return deserializer.deserialize(jsonValue, effectiveType);
        }
        if (binding.format != null) {
            return JsonFormatConverter.deserialize(jsonValue, binding.format, effectiveType);
        }
        return jsonValue;
    }

    /**
     * Bridges reflective {@link Object} values into {@link JsonFieldSerializer#serialize(Object)}.
     * The cast is required: field values are {@code Object} at the adapter boundary, while
     * strategies are parameterized by the declared field type.
     */
    @SuppressWarnings("unchecked")
    private static <T> Object invokeSerialize(JsonFieldSerializer<T> serializer, Object javaValue) {
        return serializer.serialize((T) javaValue);
    }
}
