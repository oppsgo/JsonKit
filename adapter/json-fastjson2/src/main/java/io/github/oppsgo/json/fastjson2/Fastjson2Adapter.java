package io.github.oppsgo.json.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.filter.NameFilter;
import com.alibaba.fastjson2.filter.PropertyFilter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.reflect.JsonTypeReference;
import io.github.oppsgo.json.support.JsonAnnotationSupport;

/**
 * Fastjson2-backed {@link JsonAdapter} (recommended Fastjson line).
 * <p>
 * Honors JsonKit annotations ({@code @JsonProperty}, {@code @JsonIgnore},
 * {@code @JsonAlias}, {@code @JsonIgnoreProperties}). Options are snapshotted at
 * construction; {@code null} options mean {@link JsonOptions#defaults()}.
 */
public class Fastjson2Adapter implements JsonAdapter {

    private final Filter[] filters;
    private final JSONWriter.Feature[] writeFeatures;
    private final JsonOptions options;

    /** Creates an adapter with {@link JsonOptions#defaults()}. */
    public Fastjson2Adapter() {
        this(JsonOptions.defaults());
    }

    /**
     * Creates an adapter with the given options.
     * {@code options == null} is treated as {@link JsonOptions#defaults()}.
     */
    public Fastjson2Adapter(JsonOptions options) {
        JsonOptions resolved = options != null ? new JsonOptions(options) : JsonOptions.defaults();
        this.options = resolved;
        this.filters = new Filter[]{NAME_FILTER, PROPERTY_FILTER};
        if (resolved.isSerializeNulls()) {
            this.writeFeatures = new JSONWriter.Feature[]{JSONWriter.Feature.WriteMapNullValue};
        } else {
            this.writeFeatures = new JSONWriter.Feature[0];
        }
    }

    @NotNull
    @Override
    public JsonOptions getOptions() {
        return new JsonOptions(options);
    }

    private static final NameFilter NAME_FILTER = new NameFilter() {
        @Override
        public String process(Object object, String name, Object value) {
            if (object == null || name == null) {
                return name;
            }
            Field field = findField(object.getClass(), name);
            return field != null ? JsonAnnotationSupport.jsonName(field) : name;
        }
    };

    private static final PropertyFilter PROPERTY_FILTER = new PropertyFilter() {
        @Override
        public boolean apply(Object object, String name, Object value) {
            if (object == null || name == null) {
                return true;
            }
            Field field = findField(object.getClass(), name);
            if (field == null) {
                Set<String> ignored = JsonAnnotationSupport.ignoredPropertyNames(object.getClass());
                return !ignored.contains(name);
            }
            return !JsonAnnotationSupport.shouldIgnoreSerialize(field);
        }
    };

    @Override
    public String toJson(Object object) {
        return JSON.toJSONString(object, filters, writeFeatures);
    }

    @Override
    public void toJson(Object object, @NotNull Writer writer) throws IOException {
        String json = toJson(object);
        writer.write(json != null ? json : "null");
    }

    @Override
    public <T> T fromJson(String json, @NotNull Class<T> clazz) {
        return fromJson(json, (Type) clazz);
    }

    @Override
    public <T> T fromJson(String json, @NotNull Type type) {
        Object tree = JSON.parse(json);
        Object remapped = remapNode(tree, type);
        return JSON.parseObject(JSON.toJSONString(remapped), type, JSONReader.Feature.SupportSmartMatch);
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
        return fromJson(readFully(reader), type);
    }

    @Override
    public <T> T fromJson(@NotNull Reader reader, @NotNull JsonTypeReference<T> reference)
            throws IOException {
        return fromJson(reader, reference.getType());
    }

    private static String readFully(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[2048];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    private static Object remapNode(Object node, Type type) {
        if (node == null || type == null) {
            return node;
        }
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            Class<?> raw = rawClass(type);
            if (raw == null || Map.class.isAssignableFrom(raw)) {
                if (type instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    if (args.length == 2) {
                        JSONObject copy = new JSONObject();
                        for (Map.Entry<String, Object> entry : obj.entrySet()) {
                            copy.put(entry.getKey(), remapNode(entry.getValue(), args[1]));
                        }
                        return copy;
                    }
                }
                return node;
            }
            return remapObject(obj, raw);
        }
        if (node instanceof JSONArray && type instanceof ParameterizedType) {
            JSONArray array = (JSONArray) node;
            Type[] args = ((ParameterizedType) type).getActualTypeArguments();
            if (args.length == 1) {
                JSONArray copy = new JSONArray(array.size());
                for (int i = 0; i < array.size(); i++) {
                    copy.add(remapNode(array.get(i), args[0]));
                }
                return copy;
            }
        }
        return node;
    }

    private static JSONObject remapObject(JSONObject source, Class<?> type) {
        Map<String, String> toField = JsonAnnotationSupport.deserializeKeyRemapToFieldName(type);
        Set<String> drop = JsonAnnotationSupport.deserializeKeysToDrop(type);
        JSONObject out = new JSONObject();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (drop.contains(key)) {
                continue;
            }
            String remapped = toField.get(key);
            String fieldName = remapped != null ? remapped : key;
            if (drop.contains(fieldName)) {
                continue;
            }
            Field field = findField(type, fieldName);
            Object value = entry.getValue();
            if (field != null) {
                value = remapNode(value, field.getGenericType());
            }
            out.put(fieldName, value);
        }
        return out;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            if (raw instanceof Class) {
                return (Class<?>) raw;
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        List<Field> fields = JsonAnnotationSupport.jsonFields(type);
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }
}
