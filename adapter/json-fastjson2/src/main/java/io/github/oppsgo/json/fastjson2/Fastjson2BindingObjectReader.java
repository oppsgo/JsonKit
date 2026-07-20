package io.github.oppsgo.json.fastjson2;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderCreator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.oppsgo.json.convert.FieldConverters;
import io.github.oppsgo.json.convert.StrategyInstanceCache;
import io.github.oppsgo.json.support.BindingMeta;

/**
 * Builds Fastjson2-native {@link ObjectReader}s from {@link BindingMeta} (方案 C′).
 * <p>
 * Uses {@link ObjectReaderCreator} {@link FieldReader}s (same stack as default bean
 * binding) with JsonKit jsonName / aliases; converters read as {@link Object} then
 * {@link FieldConverters}. Avoids {@code java.util.function.*} so Animal Sniffer
 * API 19 stays clean (Supplier is created inside Fastjson2). Requires Fastjson2
 * {@code 2.0.53+}.
 */
final class Fastjson2BindingObjectReader {

    private Fastjson2BindingObjectReader() {
    }

    static <T> ObjectReader<T> create(
            Class<T> raw,
            BindingMeta meta,
            StrategyInstanceCache strategies) {
        // Touch no-arg ctor early for a clear error (Fastjson2 createSupplier does the same).
        resolveConstructor(raw);

        List<FieldReader<?>> fieldReaders = new ArrayList<>();
        Set<String> registeredNames = new HashSet<>();

        List<Field> fields = meta.getFields();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            BindingMeta.FieldBinding binding = meta.bindingOf(field);
            if (binding == null || binding.ignoreDeserialize) {
                continue;
            }
            field.setAccessible(true);

            if (binding.hasDeserializeConverter()) {
                addConverterFieldReaders(
                        fieldReaders, registeredNames, field, binding, strategies);
            } else {
                addOptimizedFieldReaders(fieldReaders, registeredNames, field, binding);
            }
        }

        FieldReader<?>[] array = fieldReaders.toArray(new FieldReader[0]);
        // createObjectReader(Class, FieldReader...) builds Supplier inside Fastjson2.
        return ObjectReaderCreator.INSTANCE.createObjectReader(raw, array);
    }

    private static void addOptimizedFieldReaders(
            List<FieldReader<?>> fieldReaders,
            Set<String> registeredNames,
            Field field,
            BindingMeta.FieldBinding binding) {
        addIfAbsent(fieldReaders, registeredNames, binding.jsonName, field);
        addIfAbsent(fieldReaders, registeredNames, field.getName(), field);
        for (int a = 0; a < binding.aliases.length; a++) {
            String alias = binding.aliases[a];
            if (alias != null && !alias.isEmpty()) {
                addIfAbsent(fieldReaders, registeredNames, alias, field);
            }
        }
    }

    private static void addIfAbsent(
            List<FieldReader<?>> fieldReaders,
            Set<String> registeredNames,
            String jsonName,
            Field field) {
        if (jsonName == null || jsonName.isEmpty() || !registeredNames.add(jsonName)) {
            return;
        }
        fieldReaders.add(ObjectReaderCreator.INSTANCE.createFieldReader(jsonName, field));
    }

    private static void addConverterFieldReaders(
            List<FieldReader<?>> fieldReaders,
            Set<String> registeredNames,
            Field field,
            BindingMeta.FieldBinding binding,
            StrategyInstanceCache strategies) {
        addConverterName(fieldReaders, registeredNames, binding.jsonName, field, binding, strategies);
        addConverterName(fieldReaders, registeredNames, field.getName(), field, binding, strategies);
        for (int a = 0; a < binding.aliases.length; a++) {
            String alias = binding.aliases[a];
            if (alias != null && !alias.isEmpty()) {
                addConverterName(fieldReaders, registeredNames, alias, field, binding, strategies);
            }
        }
    }

    private static void addConverterName(
            List<FieldReader<?>> fieldReaders,
            Set<String> registeredNames,
            String jsonName,
            Field field,
            BindingMeta.FieldBinding binding,
            StrategyInstanceCache strategies) {
        if (jsonName == null || jsonName.isEmpty() || !registeredNames.add(jsonName)) {
            return;
        }
        fieldReaders.add(new ConvertingFieldReader(jsonName, field, binding, strategies));
    }

    private static <T> void resolveConstructor(Class<T> raw) {
        try {
            raw.getDeclaredConstructor().setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Fastjson2 JsonKit ObjectReader requires a no-arg constructor: " + raw.getName(),
                    e);
        }
    }

    /**
     * FieldReader without {@code java.util.function.BiConsumer} (API 19 / Animal Sniffer).
     */
    private static final class ConvertingFieldReader extends FieldReader<Object> {
        private final BindingMeta.FieldBinding binding;
        private final StrategyInstanceCache strategies;

        ConvertingFieldReader(
                String jsonName,
                Field field,
                BindingMeta.FieldBinding binding,
                StrategyInstanceCache strategies) {
            super(jsonName, Object.class, Object.class, 0, 0L, null, null, null, null, null, field);
            this.binding = binding;
            this.strategies = strategies;
        }

        @Override
        public Object readFieldValue(JSONReader jsonReader) {
            return jsonReader.readAny();
        }

        @Override
        public void accept(Object object, Object value) {
            try {
                Object converted = FieldConverters.deserialize(binding, value, field.getGenericType(), strategies);
                field.set(object, converted);
            } catch (IllegalAccessException e) {
                throw new JSONException("Cannot set " + field.getName(), e);
            }
        }

        @Override
        public void readFieldValue(JSONReader jsonReader, Object object) {
            accept(object, readFieldValue(jsonReader));
        }
    }
}
