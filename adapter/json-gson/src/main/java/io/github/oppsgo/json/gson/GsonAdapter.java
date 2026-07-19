package io.github.oppsgo.json.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.convert.FieldConverters;
import io.github.oppsgo.json.convert.StrategyInstanceCache;
import io.github.oppsgo.json.reflect.JsonTypeReference;
import io.github.oppsgo.json.support.BindingMeta;
import io.github.oppsgo.json.support.JsonAnnotationSupport;

/**
 * Gson-backed {@link JsonAdapter}.
 * <p>
 * Honors JsonKit annotations ({@code @JsonProperty}, {@code @JsonIgnore},
 * {@code @JsonAlias}, {@code @JsonIgnoreProperties}, field strategies, {@code @JsonFormat}).
 * Options are snapshotted at construction; {@code null} options mean {@link JsonOptions#defaults()}.
 */
public class GsonAdapter implements JsonAdapter {

    private final Gson gson;
    private final JsonOptions options;

    /**
     * Creates an adapter with {@link JsonOptions#defaults()}.
     */
    public GsonAdapter() {
        this(JsonOptions.defaults());
    }

    /**
     * Creates an adapter with the given options.
     * {@code options == null} is treated as {@link JsonOptions#defaults()}.
     */
    public GsonAdapter(JsonOptions options) {
        JsonOptions resolved = options != null ? new JsonOptions(options) : JsonOptions.defaults();
        this.options = resolved;
        GsonBuilder builder = new GsonBuilder()
                .setFieldNamingStrategy(JSON_NAMING)
                .addSerializationExclusionStrategy(new AnnotationExclusionStrategy(true))
                .addDeserializationExclusionStrategy(new AnnotationExclusionStrategy(false))
                .registerTypeAdapterFactory(new ConvertingReflectiveTypeAdapterFactory(new StrategyInstanceCache(), resolved))
                .registerTypeAdapterFactory(new AliasRemappingTypeAdapterFactory());
        if (resolved.isSerializeNulls()) {
            builder.serializeNulls();
        }
        this.gson = builder.create();
    }

    @NotNull
    @Override
    public JsonOptions getOptions() {
        return new JsonOptions(options);
    }

    private static final FieldNamingStrategy JSON_NAMING = JsonAnnotationSupport::jsonName;

    @Override
    public String toJson(Object object) {
        return gson.toJson(object);
    }

    @Override
    public void toJson(Object object, @NotNull Writer writer) {
        gson.toJson(object, writer);
    }

    @Override
    public <T> T fromJson(String json, @NotNull Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    @Override
    public <T> T fromJson(String json, @NotNull Type type) {
        return gson.fromJson(json, type);
    }

    @Override
    public <T> T fromJson(String json, @NotNull JsonTypeReference<T> reference) {
        return gson.fromJson(json, reference.getType());
    }

    @Override
    public <T> T fromJson(@NotNull Reader reader, @NotNull Class<T> clazz) {
        return gson.fromJson(reader, clazz);
    }

    @Override
    public <T> T fromJson(@NotNull Reader reader, @NotNull Type type) {
        return gson.fromJson(reader, type);
    }

    @Override
    public <T> T fromJson(@NotNull Reader reader, @NotNull JsonTypeReference<T> reference) {
        return gson.fromJson(reader, reference.getType());
    }

    private static final class AnnotationExclusionStrategy implements ExclusionStrategy {
        private final boolean serializeSide;

        AnnotationExclusionStrategy(boolean serializeSide) {
            this.serializeSide = serializeSide;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            Field field = findField(f);
            if (field == null) {
                return false;
            }
            return serializeSide
                    ? JsonAnnotationSupport.shouldIgnoreSerialize(field)
                    : JsonAnnotationSupport.shouldIgnoreDeserialize(field);
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }

        private static Field findField(FieldAttributes attributes) {
            try {
                return attributes.getDeclaringClass().getDeclaredField(attributes.getName());
            } catch (NoSuchFieldException e) {
                Class<?> current = attributes.getDeclaringClass().getSuperclass();
                while (current != null && current != Object.class) {
                    try {
                        return current.getDeclaredField(attributes.getName());
                    } catch (NoSuchFieldException ignored) {
                        current = current.getSuperclass();
                    }
                }
                return null;
            }
        }
    }

    /**
     * Full reflective binding when any field has a strategy or {@code @JsonFormat}.
     */
    private static final class ConvertingReflectiveTypeAdapterFactory implements TypeAdapterFactory {
        private final StrategyInstanceCache strategies;
        private final JsonOptions options;

        ConvertingReflectiveTypeAdapterFactory(StrategyInstanceCache strategies, JsonOptions options) {
            this.strategies = strategies;
            this.options = options;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<? super T> raw = type.getRawType();
            if (raw.isPrimitive() || raw.isEnum() || raw.isArray()
                    || raw.getName().startsWith("java.")
                    || raw.getName().startsWith("javax.")) {
                return null;
            }
            BindingMeta meta = BindingMeta.scan(raw);
            if (!FieldConverters.hasAnyConverter(meta)) {
                return null;
            }
            Constructor<?> constructor;
            try {
                constructor = raw.getDeclaredConstructor();
                constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(
                        "JsonKit converting Gson path requires a no-arg constructor: " + raw.getName(),
                        e);
            }

            List<BoundField> boundFields = new ArrayList<BoundField>();
            Map<String, BoundField> nameToField = new LinkedHashMap<String, BoundField>();
            TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

            for (Field field : meta.getFields()) {
                field.setAccessible(true);
                BindingMeta.FieldBinding binding = meta.bindingOf(field);
                if (binding == null || (binding.ignoreSerialize && binding.ignoreDeserialize)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                TypeAdapter<Object> fieldAdapter =
                        (TypeAdapter<Object>) gson.getAdapter(TypeToken.get(field.getGenericType()));
                BoundField bound = new BoundField(field, binding, fieldAdapter);
                boundFields.add(bound);
                if (!binding.ignoreDeserialize) {
                    if (!nameToField.containsKey(binding.jsonName)) {
                        nameToField.put(binding.jsonName, bound);
                    }
                    for (String alias : binding.aliases) {
                        if (alias != null && !alias.isEmpty() && !nameToField.containsKey(alias)) {
                            nameToField.put(alias, bound);
                        }
                    }
                }
            }

            Set<String> keysToDrop = meta.getKeysToDrop();
            final Constructor<?> ctor = constructor;
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                        return;
                    }
                    out.beginObject();
                    for (BoundField bound : boundFields) {
                        if (bound.binding.ignoreSerialize) {
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
                        out.name(bound.binding.jsonName);
                        if (fieldValue == null) {
                            out.nullValue();
                            continue;
                        }
                        if (bound.binding.hasSerializeConverter()) {
                            Object tree = FieldConverters.serialize(bound.binding, fieldValue, strategies);
                            elementAdapter.write(out, gson.toJsonTree(tree));
                        } else {
                            bound.fieldAdapter.write(out, fieldValue);
                        }
                    }
                    out.endObject();
                }

                @Override
                @SuppressWarnings("unchecked")
                public T read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    Object instance;
                    try {
                        instance = ctor.newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new IOException("Cannot construct " + raw.getName(), e);
                    }
                    in.beginObject();
                    while (in.hasNext()) {
                        String name = in.nextName();
                        if (keysToDrop.contains(name)) {
                            in.skipValue();
                            continue;
                        }
                        BoundField bound = nameToField.get(name);
                        if (bound == null || bound.binding.ignoreDeserialize) {
                            in.skipValue();
                            continue;
                        }
                        Object fieldValue;
                        if (bound.binding.hasDeserializeConverter()) {
                            JsonElement element = elementAdapter.read(in);
                            if (element == null || element.isJsonNull()) {
                                fieldValue = null;
                            } else {
                                Object tree = gson.fromJson(element, Object.class);
                                fieldValue = FieldConverters.deserialize(
                                        bound.binding, tree, bound.field.getGenericType(), strategies);
                            }
                        } else {
                            fieldValue = bound.fieldAdapter.read(in);
                        }
                        try {
                            bound.field.set(instance, fieldValue);
                        } catch (IllegalAccessException e) {
                            throw new IOException("Cannot set " + bound.field.getName(), e);
                        }
                    }
                    in.endObject();
                    return (T) instance;
                }
            };
        }

        private static final class BoundField {
            final Field field;
            final BindingMeta.FieldBinding binding;
            final TypeAdapter<Object> fieldAdapter;

            BoundField(Field field, BindingMeta.FieldBinding binding, TypeAdapter<Object> fieldAdapter) {
                this.field = field;
                this.binding = binding;
                this.fieldAdapter = fieldAdapter;
            }
        }
    }

    private static final class AliasRemappingTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<? super T> raw = type.getRawType();
            if (raw.isPrimitive() || raw.isEnum() || raw.isArray()
                    || raw.getName().startsWith("java.")
                    || raw.getName().startsWith("javax.")) {
                return null;
            }
            BindingMeta meta = BindingMeta.scan(raw);
            if (FieldConverters.hasAnyConverter(meta)) {
                // Handled by ConvertingReflectiveTypeAdapterFactory.
                return null;
            }
            Map<String, String> aliasToCanonical = meta.getAliasToCanonicalName();
            Set<String> drop = meta.getKeysToDrop();
            if (aliasToCanonical.isEmpty() && drop.isEmpty()) {
                return null;
            }
            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
            final Map<String, String> remap = aliasToCanonical;
            final Set<String> dropKeys = drop;
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    delegate.write(out, value);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    JsonElement element = elementAdapter.read(in);
                    if (element != null && element.isJsonObject()) {
                        remapObject(element.getAsJsonObject(), remap, dropKeys);
                    }
                    return delegate.fromJsonTree(element);
                }
            };
        }

        private static void remapObject(
                JsonObject object,
                Map<String, String> aliasToCanonical,
                Set<String> drop) {
            for (String key : drop) {
                object.remove(key);
            }
            String[] keys = object.keySet().toArray(new String[0]);
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                String canonical = aliasToCanonical.get(key);
                if (canonical != null && !object.has(canonical)) {
                    object.add(canonical, object.get(key));
                    object.remove(key);
                }
            }
        }
    }
}
