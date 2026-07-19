package io.github.oppsgo.json.moshi;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.reflect.JsonTypeReference;
import okio.Buffer;

/**
 * Moshi-backed {@link io.github.oppsgo.json.adapter.JsonAdapter}.
 * <p>
 * Honors JsonKit annotations via a reflective Moshi factory (no Kotlin codegen).
 * Prefer a long-lived instance (e.g. {@link MoshiAdapterFactory#of()}); Moshi caches
 * its own {@code JsonAdapter}s — this class does not retain a {@code BindingCache}.
 */
public class MoshiAdapter implements io.github.oppsgo.json.adapter.JsonAdapter {

    private final Moshi moshi;
    private final JsonOptions options;

    /**
     * Creates an adapter with {@link JsonOptions#defaults()}.
     */
    public MoshiAdapter() {
        this(JsonOptions.defaults());
    }

    /**
     * Creates an adapter with the given options.
     * {@code options == null} is treated as {@link JsonOptions#defaults()}.
     */
    public MoshiAdapter(JsonOptions options) {
        JsonOptions resolved = options != null ? new JsonOptions(options) : JsonOptions.defaults();
        this.options = resolved;
        this.moshi = new Moshi.Builder()
                .add(new JsonKitMoshiAdapterFactory(resolved))
                .build();
    }

    @NotNull
    @Override
    public JsonOptions getOptions() {
        return new JsonOptions(options);
    }

    private <T> JsonAdapter<T> adapter(Type type) {
        JsonAdapter<T> adapter = moshi.adapter(type);
        if (options.isSerializeNulls()) {
            adapter = adapter.serializeNulls();
        }
        return adapter;
    }

    @Override
    public String toJson(Object object) {
        if (object == null) {
            return "null";
        }
        return adapter(typeForValue(object)).toJson(object);
    }

    @Override
    public void toJson(Object object, @NotNull Writer writer) throws IOException {
        if (object == null) {
            writer.write("null");
            return;
        }
        Buffer buffer = new Buffer();
        adapter(typeForValue(object)).toJson(buffer, object);
        writer.write(buffer.readUtf8());
    }

    @Override
    public <T> T fromJson(String json, @NotNull Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            T value = (T) adapter(clazz).fromJson(json);
            return value;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromJson(String json, @NotNull Type type) {
        if (json == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            T value = (T) adapter(type).fromJson(json);
            return value;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromJson(String json, @NotNull JsonTypeReference<T> reference) {
        return fromJson(json, reference.getType());
    }

    @Override
    public <T> T fromJson(@NotNull Reader reader, @NotNull Class<T> clazz) throws IOException {
        return fromJson(reader, (Type) clazz);
    }

    @Override
    public <T> T fromJson(@NotNull Reader reader, @NotNull Type type) throws IOException {
        Buffer buffer = readToBuffer(reader);
        @SuppressWarnings("unchecked")
        T value = (T) adapter(type).fromJson(buffer);
        return value;
    }

    @Override
    public <T> T fromJson(@NotNull Reader reader, @NotNull JsonTypeReference<T> reference)
            throws IOException {
        return fromJson(reader, reference.getType());
    }

    private static Buffer readToBuffer(Reader reader) throws IOException {
        Buffer buffer = new Buffer();
        char[] cbuf = new char[2048];
        int n;
        while ((n = reader.read(cbuf)) != -1) {
            buffer.writeUtf8(new String(cbuf, 0, n));
        }
        return buffer;
    }

    /**
     * Moshi prefers collection/map interfaces over concrete runtime classes (e.g. ArrayList).
     * Infer a parameterized {@link Type} from a non-null value for {@link #toJson(Object)}.
     */
    private static Type typeForValue(Object object) {
        if (object instanceof List) {
            return Types.newParameterizedType(List.class, elementType((Collection<?>) object));
        }
        if (object instanceof Set) {
            return Types.newParameterizedType(Set.class, elementType((Collection<?>) object));
        }
        if (object instanceof Collection) {
            return Types.newParameterizedType(Collection.class, elementType((Collection<?>) object));
        }
        if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            Type keyType = Object.class;
            Type valueType = Object.class;
            if (!map.isEmpty()) {
                Map.Entry<?, ?> entry = map.entrySet().iterator().next();
                if (entry.getKey() != null) {
                    keyType = entry.getKey().getClass();
                }
                if (entry.getValue() != null) {
                    valueType = typeForValue(entry.getValue());
                }
            }
            return Types.newParameterizedType(Map.class, keyType, valueType);
        }
        return object.getClass();
    }

    private static Type elementType(Collection<?> collection) {
        for (Object element : collection) {
            if (element != null) {
                return typeForValue(element);
            }
        }
        return Object.class;
    }
}
