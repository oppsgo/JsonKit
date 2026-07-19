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
import io.github.oppsgo.json.support.BindingMeta;
import io.github.oppsgo.json.support.JsonAnnotationSupport;

/**
 * Moshi {@link JsonAdapter.Factory} that binds JsonKit annotations onto Java fields
 * (no Kotlin codegen required).
 */
final class JsonKitMoshiAdapterFactory implements JsonAdapter.Factory {

    /**
     * Adapter-level options; new JsonOptions fields are read from here instead of being split out.
     */
    private final JsonOptions options;

    JsonKitMoshiAdapterFactory(JsonOptions options) {
        this.options = options;
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

        BindingMeta meta = BindingMeta.scan(raw);
        List<Field> fields = meta.getFields();
        if (fields.isEmpty() && !hasNoArgConstructor(raw)) {
            return null;
        }

        List<BoundField> boundFields = new ArrayList<BoundField>();
        Map<String, BoundField> nameToField = new LinkedHashMap<String, BoundField>();

        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            field.setAccessible(true);
            boolean ignoreSerialize = JsonAnnotationSupport.shouldIgnoreSerialize(field);
            boolean ignoreDeserialize = JsonAnnotationSupport.shouldIgnoreDeserialize(field);
            if (ignoreSerialize && ignoreDeserialize) {
                continue;
            }

            String jsonName = JsonAnnotationSupport.jsonName(field);
            JsonAdapter<Object> adapter = moshi.adapter(field.getGenericType());
            BoundField bound = new BoundField(field, jsonName, adapter, ignoreSerialize, ignoreDeserialize);
            boundFields.add(bound);

            if (!ignoreDeserialize) {
                putName(nameToField, jsonName, bound);
                String[] aliases = JsonAnnotationSupport.aliases(field);
                for (int a = 0; a < aliases.length; a++) {
                    String alias = aliases[a];
                    if (alias != null && !alias.isEmpty()) {
                        putName(nameToField, alias, bound);
                    }
                }
            }
        }

        Set<String> keysToDrop = meta.getKeysToDrop();
        Constructor<?> constructor = resolveConstructor(raw);
        return new FieldJsonAdapter(raw, constructor, boundFields, nameToField, keysToDrop, options)
                .nullSafe();
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

    /**
     * One reflected field plus its JsonKit binding and Moshi value adapter.
     * Built once when the enclosing {@link FieldJsonAdapter} is created; Moshi then caches that adapter.
     */
    private static final class BoundField {
        /**
         * Accessible Java field on the model (or superclass).
         */
        final Field field;
        /**
         * Canonical JSON name ({@code @JsonProperty} or the Java field name).
         */
        final String jsonName;
        /**
         * Moshi adapter for this field's declared type.
         */
        final JsonAdapter<Object> adapter;
        /**
         * When true, omit this field on serialize ({@code @JsonIgnore} / ignore-properties).
         */
        final boolean ignoreSerialize;
        /**
         * When true, do not bind incoming JSON into this field.
         */
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

    /**
     * Reflective object {@link JsonAdapter} for a concrete model type.
     * Writes/reads JSON objects using {@link BoundField}s; honors aliases via {@link #nameToField}
     * and drops keys listed in {@link #keysToDrop}.
     */
    private static final class FieldJsonAdapter extends JsonAdapter<Object> {
        /**
         * Concrete model class being adapted.
         */
        private final Class<?> raw;
        /**
         * No-arg constructor used on deserialize.
         */
        private final Constructor<?> constructor;
        /**
         * Fields in declaration order for serialize.
         */
        private final List<BoundField> boundFields;
        /**
         * JSON property name or {@code @JsonAlias} → field binding (first registration wins).
         * Used only on deserialize.
         */
        private final Map<String, BoundField> nameToField;
        /**
         * Keys to skip on deserialize (ignore-on-deserialize names/aliases and
         * {@code @JsonIgnoreProperties}).
         */
        private final Set<String> keysToDrop;
        /**
         * Snapshot of JsonKit options (e.g. serializeNulls) for this Moshi instance.
         */
        private final JsonOptions options;

        FieldJsonAdapter(
                Class<?> raw,
                Constructor<?> constructor,
                List<BoundField> boundFields,
                Map<String, BoundField> nameToField,
                Set<String> keysToDrop,
                JsonOptions options) {
            this.raw = raw;
            this.constructor = constructor;
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
            Object instance;
            try {
                instance = constructor.newInstance();
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
