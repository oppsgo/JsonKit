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
import com.google.gson.stream.JsonWriter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.reflect.JsonTypeReference;
import io.github.oppsgo.json.support.BindingMeta;
import io.github.oppsgo.json.support.JsonAnnotationSupport;

/**
 * Gson-backed {@link JsonAdapter}.
 * <p>
 * Honors JsonKit annotations ({@code @JsonProperty}, {@code @JsonIgnore},
 * {@code @JsonAlias}, {@code @JsonIgnoreProperties}). Options are snapshotted at
 * construction; {@code null} options mean {@link JsonOptions#defaults()}.
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
            Map<String, String> aliasToCanonical = meta.getAliasToCanonicalName();
            Set<String> drop = meta.getKeysToDrop();
            if (aliasToCanonical.isEmpty() && drop.isEmpty()) {
                return null;
            }
            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
            // Capture remap/drop into the TypeAdapter; BindingMeta itself is not retained.
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
