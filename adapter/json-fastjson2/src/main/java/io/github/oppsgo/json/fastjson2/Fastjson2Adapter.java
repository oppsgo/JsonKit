package io.github.oppsgo.json.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.filter.NameFilter;
import com.alibaba.fastjson2.filter.PropertyFilter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.util.TypeUtils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import io.github.oppsgo.json.JsonOptions;
import io.github.oppsgo.json.adapter.JsonAdapter;
import io.github.oppsgo.json.convert.FieldConverters;
import io.github.oppsgo.json.convert.StrategyInstanceCache;
import io.github.oppsgo.json.reflect.JsonTypeReference;
import io.github.oppsgo.json.support.BindingCache;
import io.github.oppsgo.json.support.BindingMeta;

/**
 * Fastjson2-backed {@link JsonAdapter} (recommended Fastjson line).
 * <p>
 * Honors JsonKit annotations ({@code @JsonProperty}, {@code @JsonIgnore},
 * {@code @JsonAlias}, {@code @JsonIgnoreProperties}). Options are snapshotted at
 * construction; {@code null} options mean {@link JsonOptions#defaults()}.
 * <p>
 * Deserialize uses BindingMeta-driven Fastjson2 {@code FieldReader}/{@code ObjectReader}
 * (engine-native) when JsonKit rules apply, or a direct {@code parseObject} when they
 * do not. Prefer a long-lived instance (e.g. {@link Fastjson2AdapterFactory#of()});
 * each adapter owns a {@link BindingCache}. Avoid per-request {@code new Fastjson2Adapter()}.
 */
public class Fastjson2Adapter implements JsonAdapter {

    private final BindingCache bindings;
    private final StrategyInstanceCache strategies;
    private final Fastjson2BindingSupport bindingSupport;
    private final Filter[] filters;
    private final JSONWriter.Feature[] writeFeatures;
    private final JsonOptions options;

    /**
     * Creates an adapter with {@link JsonOptions#defaults()}.
     */
    public Fastjson2Adapter() {
        this(JsonOptions.defaults());
    }

    /**
     * Creates an adapter with the given options.
     * {@code options == null} is treated as {@link JsonOptions#defaults()}.
     */
    public Fastjson2Adapter(JsonOptions options) {
        this(options, new BindingCache());
    }

    /**
     * Creates an adapter with the given options and binding cache.
     * Prefer {@link #Fastjson2Adapter(JsonOptions)} in applications.
     * Pass {@code new BindingCache(false)} only for benchmarks that force a rescan each time.
     * {@code options == null} is treated as {@link JsonOptions#defaults()}.
     * {@code bindings == null} is treated as {@code new BindingCache()}.
     */
    public Fastjson2Adapter(JsonOptions options, BindingCache bindings) {
        JsonOptions resolved = options != null ? new JsonOptions(options) : JsonOptions.defaults();
        this.options = resolved;
        this.bindings = bindings != null ? bindings : new BindingCache();
        this.strategies = new StrategyInstanceCache();
        this.bindingSupport = new Fastjson2BindingSupport(this.bindings, this.strategies);
        this.filters = new Filter[]{createNameFilter(), createPropertyFilter(), createValueFilter()};
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

    private NameFilter createNameFilter() {
        return new NameFilter() {
            @Override
            public String process(Object object, String name, Object value) {
                if (object == null || name == null) {
                    return name;
                }
                BindingMeta meta = bindings.get(object.getClass());
                Field field = meta.findField(name);
                return field != null ? meta.jsonName(field) : name;
            }
        };
    }

    private PropertyFilter createPropertyFilter() {
        return new PropertyFilter() {
            @Override
            public boolean apply(Object object, String name, Object value) {
                if (object == null || name == null) {
                    return true;
                }
                BindingMeta meta = bindings.get(object.getClass());
                Field field = meta.findField(name);
                if (field == null) {
                    return !meta.getIgnoredPropertyNames().contains(name);
                }
                return !meta.shouldIgnoreSerialize(field);
            }
        };
    }

    private ValueFilter createValueFilter() {
        return new ValueFilter() {
            @Override
            public Object apply(Object object, String name, Object value) {
                if (object == null || name == null || value == null) {
                    return value;
                }
                BindingMeta meta = bindings.get(object.getClass());
                Field field = meta.findFieldByJavaOrJsonName(name);
                if (field == null) {
                    return value;
                }
                BindingMeta.FieldBinding binding = meta.bindingOf(field);
                if (binding == null || !binding.hasSerializeConverter()) {
                    return value;
                }
                return FieldConverters.serialize(binding, value, strategies);
            }
        };
    }

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
        if (json == null) {
            return null;
        }
        if (!bindingSupport.needsJsonKitDeserializeBinding(type)) {
            return JSON.parseObject(json, type);
        }
        JSONReader.Context context;
        try {
            context = bindingSupport.bindingReadContext(type);
        } catch (RuntimeException setupFailed) {
            // ObjectReader cannot be built for a type in the graph — tree remap + cast.
            return fromJsonTreeFallback(json, type);
        }
        return JSON.parseObject(json, type, context);
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

    /**
     * Narrow safety net: one structural parse, tree remap, in-memory cast — never stringify.
     */
    private <T> T fromJsonTreeFallback(String json, Type type) {
        Object tree = JSON.parse(json);
        Object remapped = remapNode(tree, type);
        return TypeUtils.cast(remapped, type);
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

    private Object remapNode(Object node, Type type) {
        if (node == null || type == null) {
            return node;
        }
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            Class<?> raw = Fastjson2BindingSupport.rawClass(type);
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

    private JSONObject remapObject(JSONObject source, Class<?> type) {
        BindingMeta meta = bindings.get(type);
        Map<String, String> toField = meta.getKeyRemapToFieldName();
        Set<String> drop = meta.getKeysToDrop();
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
            Field field = meta.findField(fieldName);
            Object value = entry.getValue();
            if (field != null) {
                value = remapNode(value, field.getGenericType());
                BindingMeta.FieldBinding binding = meta.bindingOf(field);
                if (binding != null && binding.hasDeserializeConverter() && value != null) {
                    value = FieldConverters.deserialize(
                            binding, value, field.getGenericType(), strategies);
                }
            }
            out.put(fieldName, value);
        }
        return out;
    }
}
