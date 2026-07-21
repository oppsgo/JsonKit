package io.github.oppsgo.json.moshi;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.convert.FieldConverters;
import io.github.oppsgo.json.convert.StrategyInstanceCache;
import io.github.oppsgo.json.support.BindingMeta;
import io.github.oppsgo.json.support.DefaultObjectInstantiator;
import io.github.oppsgo.json.support.KotlinSupport;
import io.github.oppsgo.json.support.ObjectInstantiator;

/**
 * Moshi {@link JsonAdapter.Factory} that binds JsonKit annotations onto Java fields
 * (optional Kotlin Instantiator when {@link KotlinSupport} is active).
 */
final class JsonKitMoshiAdapterFactory implements JsonAdapter.Factory {

    private final JsonOptions options;
    private final StrategyInstanceCache strategies;

    JsonKitMoshiAdapterFactory(JsonOptions options, StrategyInstanceCache strategies) {
        this.options = options;
        this.strategies = strategies;
    }

    @Override
    public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
        if (!annotations.isEmpty()) {
            return null;
        }
        Class<?> raw = Types.getRawType(type);
        if (shouldSkip(raw)) {
            return null;
        }

        ObjectInstantiator instantiator = KotlinSupport.resolve(options);
        BindingMeta meta = BindingMeta.scan(raw);
        List<Field> fields = meta.getFields();
        boolean useKotlin = instantiator.supports(raw) && instantiator.constructsFromProperties(raw);
        if (fields.isEmpty() && !useKotlin && !hasNoArgConstructor(raw)) {
            return null;
        }

        List<BoundField> boundFields = new ArrayList<BoundField>();
        Map<String, BoundField> nameToField = new LinkedHashMap<String, BoundField>();
        JsonAdapter<Object> treeAdapter = moshi.adapter(Object.class);

        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            field.setAccessible(true);
            BindingMeta.FieldBinding binding = meta.bindingOf(field);
            if (binding == null) {
                continue;
            }
            if (binding.ignoreSerialize && binding.ignoreDeserialize) {
                continue;
            }

            JsonAdapter<Object> typeAdapter = null;
            if (!binding.hasSerializeConverter() || !binding.hasDeserializeConverter()) {
                typeAdapter = moshi.adapter(field.getGenericType());
            }
            JsonAdapter<Object> adapter = wrapConverter(
                    typeAdapter, treeAdapter, binding, field.getGenericType());
            BoundField bound = new BoundField(
                    field, binding.jsonName, adapter, binding.ignoreSerialize, binding.ignoreDeserialize);
            boundFields.add(bound);

            if (!binding.ignoreDeserialize) {
                putName(nameToField, binding.jsonName, bound);
                for (int a = 0; a < binding.aliases.length; a++) {
                    String alias = binding.aliases[a];
                    if (alias != null && !alias.isEmpty()) {
                        putName(nameToField, alias, bound);
                    }
                }
            }
        }

        Set<String> keysToDrop = meta.getKeysToDrop();
        Constructor<?> constructor = useKotlin ? null : resolveConstructor(raw);
        return new FieldJsonAdapter(
                raw, constructor, useKotlin, instantiator, boundFields, nameToField, keysToDrop, options)
                .nullSafe();
    }

    private JsonAdapter<Object> wrapConverter(
            final JsonAdapter<Object> delegate,
            final JsonAdapter<Object> treeAdapter,
            final BindingMeta.FieldBinding binding,
            final Type fieldType) {
        if (!binding.hasSerializeConverter() && !binding.hasDeserializeConverter()) {
            return delegate;
        }
        return new JsonAdapter<Object>() {
            @Override
            public Object fromJson(JsonReader reader) throws IOException {
                if (reader.peek() == JsonReader.Token.NULL) {
                    return reader.nextNull();
                }
                if (binding.hasDeserializeConverter()) {
                    Object tree = treeAdapter.fromJson(reader);
                    return FieldConverters.deserialize(binding, tree, fieldType, strategies);
                }
                if (delegate == null) {
                    throw new IOException("Missing Moshi adapter for " + fieldType);
                }
                return delegate.fromJson(reader);
            }

            @Override
            public void toJson(JsonWriter writer, Object value) throws IOException {
                if (value == null) {
                    writer.nullValue();
                    return;
                }
                if (binding.hasSerializeConverter()) {
                    Object tree = FieldConverters.serialize(binding, value, strategies);
                    treeAdapter.toJson(writer, tree);
                    return;
                }
                if (delegate == null) {
                    throw new IOException("Missing Moshi adapter for " + fieldType);
                }
                delegate.toJson(writer, value);
            }
        };
    }

    private static void putName(Map<String, BoundField> map, String name, BoundField bound) {
        if (!map.containsKey(name)) {
            map.put(name, bound);
        }
    }

    private static boolean shouldSkip(Class<?> raw) {
        if (raw.isInterface() || raw.isPrimitive() || raw.isEnum() || raw.isArray()) {
            return true;
        }
        int modifiers = raw.getModifiers();
        if (Modifier.isAbstract(modifiers)) {
            return true;
        }
        String name = raw.getName();
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("android.")
                || name.startsWith("androidx.")
                || name.startsWith("kotlin.")
                || name.startsWith("scala.");
    }

    private static boolean hasNoArgConstructor(Class<?> raw) {
        try {
            resolveConstructor(raw);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Constructor<?> resolveConstructor(Class<?> raw) {
        try {
            Constructor<?> constructor = raw.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Moshi JsonKit bridge requires a no-arg constructor: " + raw.getName(), e);
        }
    }

    private static final class BoundField {
        final Field field;
        final String jsonName;
        final JsonAdapter<Object> adapter;
        final boolean ignoreSerialize;
        final boolean ignoreDeserialize;

        BoundField(
                Field field,
                String jsonName,
                JsonAdapter<Object> adapter,
                boolean ignoreSerialize,
                boolean ignoreDeserialize) {
            this.field = field;
            this.jsonName = jsonName;
            this.adapter = adapter;
            this.ignoreSerialize = ignoreSerialize;
            this.ignoreDeserialize = ignoreDeserialize;
        }
    }

    private static final class FieldJsonAdapter extends JsonAdapter<Object> {
        private final Class<?> raw;
        private final Constructor<?> constructor;
        private final boolean useKotlin;
        private final ObjectInstantiator instantiator;
        private final List<BoundField> boundFields;
        private final Map<String, BoundField> nameToField;
        private final Set<String> keysToDrop;
        private final JsonOptions options;

        FieldJsonAdapter(
                Class<?> raw,
                Constructor<?> constructor,
                boolean useKotlin,
                ObjectInstantiator instantiator,
                List<BoundField> boundFields,
                Map<String, BoundField> nameToField,
                Set<String> keysToDrop,
                JsonOptions options) {
            this.raw = raw;
            this.constructor = constructor;
            this.useKotlin = useKotlin;
            this.instantiator = instantiator;
            this.boundFields = boundFields;
            this.nameToField = nameToField;
            this.keysToDrop = keysToDrop;
            this.options = options;
        }

        @Override
        public Object fromJson(JsonReader reader) throws IOException {
            if (reader.peek() == JsonReader.Token.NULL) {
                return reader.nextNull();
            }

            if (useKotlin) {
                Map<String, Object> properties = new LinkedHashMap<String, Object>();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (keysToDrop.contains(name)) {
                        reader.skipValue();
                        continue;
                    }
                    BoundField bound = nameToField.get(name);
                    if (bound == null || bound.ignoreDeserialize) {
                        reader.skipValue();
                        continue;
                    }
                    properties.put(bound.field.getName(), bound.adapter.fromJson(reader));
                }
                reader.endObject();
                try {
                    return instantiator.instantiate(raw, properties);
                } catch (ReflectiveOperationException e) {
                    throw new IOException("Cannot construct " + raw.getName(), e);
                }
            }

            Object instance;
            try {
                instance = constructor != null
                        ? constructor.newInstance()
                        : instantiator.instantiate(raw, DefaultObjectInstantiator.noProperties());
            } catch (ReflectiveOperationException e) {
                throw new IOException("Cannot construct " + raw.getName(), e);
            }

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (keysToDrop.contains(name)) {
                    reader.skipValue();
                    continue;
                }
                BoundField bound = nameToField.get(name);
                if (bound == null || bound.ignoreDeserialize) {
                    reader.skipValue();
                    continue;
                }
                Object value = bound.adapter.fromJson(reader);
                try {
                    bound.field.set(instance, value);
                } catch (IllegalAccessException e) {
                    throw new IOException("Cannot set " + bound.field.getName(), e);
                }
            }
            reader.endObject();
            return instance;
        }

        @Override
        public void toJson(JsonWriter writer, Object value) throws IOException {
            writer.beginObject();
            for (int i = 0; i < boundFields.size(); i++) {
                BoundField bound = boundFields.get(i);
                if (bound.ignoreSerialize) {
                    continue;
                }
                Object fieldValue;
                try {
                    fieldValue = bound.field.get(value);
                } catch (IllegalAccessException e) {
                    throw new IOException("Cannot get " + bound.field.getName(), e);
                }
                if (fieldValue == null && !options.isSerializeNulls()) {
                    continue;
                }
                writer.name(bound.jsonName);
                bound.adapter.toJson(writer, fieldValue);
            }
            writer.endObject();
        }

        @Override
        public String toString() {
            return "JsonKitMoshiAdapter(" + raw.getName() + ")";
        }
    }
}
