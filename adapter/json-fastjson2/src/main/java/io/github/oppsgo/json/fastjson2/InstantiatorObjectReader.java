package io.github.oppsgo.json.fastjson2;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.oppsgo.json.convert.FieldConverters;
import io.github.oppsgo.json.convert.StrategyInstanceCache;
import io.github.oppsgo.json.support.BindingMeta;
import io.github.oppsgo.json.support.ObjectInstantiator;

/**
 * Fastjson2 {@link ObjectReader} that collects JSON properties then constructs via
 * {@link ObjectInstantiator} (Kotlin primary/marker constructor). Avoids
 * {@code java.util.function.*} for Animal Sniffer API 19.
 */
final class InstantiatorObjectReader<T> implements ObjectReader<T> {

    private final Class<T> raw;
    private final BindingMeta meta;
    private final ObjectInstantiator instantiator;
    private final StrategyInstanceCache strategies;
    private final Map<String, BoundProp> nameToProp;

    InstantiatorObjectReader(
            Class<T> raw,
            BindingMeta meta,
            ObjectInstantiator instantiator,
            StrategyInstanceCache strategies) {
        this.raw = raw;
        this.meta = meta;
        this.instantiator = instantiator;
        this.strategies = strategies;
        this.nameToProp = new LinkedHashMap<String, BoundProp>();

        List<java.lang.reflect.Field> fields = meta.getFields();
        for (int i = 0; i < fields.size(); i++) {
            java.lang.reflect.Field field = fields.get(i);
            BindingMeta.FieldBinding binding = meta.bindingOf(field);
            if (binding == null || binding.ignoreDeserialize) {
                continue;
            }
            BoundProp prop = new BoundProp(field, binding);
            putName(binding.jsonName, prop);
            putName(field.getName(), prop);
            for (int a = 0; a < binding.aliases.length; a++) {
                String alias = binding.aliases[a];
                if (alias != null && !alias.isEmpty()) {
                    putName(alias, prop);
                }
            }
        }
    }

    private void putName(String name, BoundProp prop) {
        if (name != null && !name.isEmpty() && !nameToProp.containsKey(name)) {
            nameToProp.put(name, prop);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        if (jsonReader.nextIfNull()) {
            return null;
        }
        if (!jsonReader.nextIfObjectStart()) {
            throw new JSONException(jsonReader.info("expect object for " + raw.getName()));
        }

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        while (!jsonReader.nextIfObjectEnd()) {
            String name = jsonReader.readFieldName();
            if (name == null) {
                jsonReader.skipValue();
                continue;
            }
            if (meta.getKeysToDrop().contains(name)) {
                jsonReader.skipValue();
                continue;
            }
            BoundProp prop = nameToProp.get(name);
            if (prop == null) {
                jsonReader.skipValue();
                continue;
            }
            Object value = jsonReader.read(prop.field.getGenericType());
            if (prop.binding.hasDeserializeConverter() && value != null) {
                value = FieldConverters.deserialize(
                        prop.binding, value, prop.field.getGenericType(), strategies);
            }
            properties.put(prop.field.getName(), value);
        }

        try {
            return (T) instantiator.instantiate(raw, properties);
        } catch (ReflectiveOperationException e) {
            throw new JSONException("Cannot construct " + raw.getName(), e);
        }
    }

    @Override
    public T createInstance(long features) {
        try {
            return (T) instantiator.instantiate(raw, java.util.Collections.<String, Object>emptyMap());
        } catch (ReflectiveOperationException e) {
            throw new JSONException("Cannot construct " + raw.getName(), e);
        }
    }

    @Override
    public T createInstance(Collection collection) {
        throw new JSONException("InstantiatorObjectReader does not support collection create for "
                + raw.getName());
    }

    @Override
    public T createInstance(Map map, long features) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        if (map != null) {
            for (Object entryObj : map.entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObj;
                String name = String.valueOf(entry.getKey());
                BoundProp prop = nameToProp.get(name);
                if (prop == null) {
                    continue;
                }
                Object value = entry.getValue();
                if (prop.binding.hasDeserializeConverter() && value != null) {
                    value = FieldConverters.deserialize(
                            prop.binding, value, prop.field.getGenericType(), strategies);
                }
                properties.put(prop.field.getName(), value);
            }
        }
        try {
            return (T) instantiator.instantiate(raw, properties);
        } catch (ReflectiveOperationException e) {
            throw new JSONException("Cannot construct " + raw.getName(), e);
        }
    }

    private static final class BoundProp {
        final java.lang.reflect.Field field;
        final BindingMeta.FieldBinding binding;

        BoundProp(java.lang.reflect.Field field, BindingMeta.FieldBinding binding) {
            this.field = field;
            this.binding = binding;
        }
    }
}
