package io.github.oppsgo.json.fastjson2;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.oppsgo.json.convert.StrategyInstanceCache;
import io.github.oppsgo.json.support.BindingCache;
import io.github.oppsgo.json.support.BindingMeta;

/**
 * Per-adapter ObjectReader registration and fail-closed binding predicates.
 * Uses a private {@link ObjectReaderProvider} (not the global Fastjson2 default).
 */
final class Fastjson2BindingSupport {

    private final BindingCache bindings;
    private final StrategyInstanceCache strategies;
    private final ObjectReaderProvider provider;
    private final JSONReader.Context readContext;
    private final ConcurrentHashMap<Class<?>, Boolean> registered;
    private final ConcurrentHashMap<Type, Boolean> needsBindingCache;
    private final ConcurrentHashMap<Type, Boolean> graphEnsured;

    Fastjson2BindingSupport(BindingCache bindings, StrategyInstanceCache strategies) {
        this.bindings = bindings;
        this.strategies = strategies;
        this.provider = new ObjectReaderProvider();
        this.readContext = JSONFactory.createReadContext(provider);
        this.registered = new ConcurrentHashMap<Class<?>, Boolean>();
        this.needsBindingCache = new ConcurrentHashMap<Type, Boolean>();
        this.graphEnsured = new ConcurrentHashMap<Type, Boolean>();
    }

    /**
     * Fail-closed: true when the type graph needs JsonKit deserialize remap, drop,
     * or field converters/format. Uncertain types return true.
     */
    boolean needsJsonKitDeserializeBinding(Type type) {
        if (type == null) {
            return true;
        }
        Boolean cached = needsBindingCache.get(type);
        if (cached != null) {
            return cached;
        }
        boolean needs = needsBinding(type, new HashSet<>());
        needsBindingCache.putIfAbsent(type, needs);
        return needs;
    }

    /**
     * Ensures ObjectReaders for every class in {@code type}'s graph that has local
     * JsonKit deserialize rules (once per type), then returns the shared read context.
     */
    JSONReader.Context bindingReadContext(Type type) {
        if (type != null) {
            ensureGraphOnce(type);
        }
        return readContext;
    }

    private void ensureGraphOnce(Type type) {
        if (graphEnsured.get(type) != null) {
            return;
        }
        synchronized (graphEnsured) {
            if (graphEnsured.get(type) != null) {
                return;
            }
            ensureReadersForGraph(type, new HashSet<>());
            graphEnsured.put(type, Boolean.TRUE);
        }
    }

    ObjectReaderProvider provider() {
        return provider;
    }

    private boolean needsBinding(Type type, Set<Class<?>> visiting) {
        if (type == null) {
            return true;
        }
        if (type instanceof WildcardType) {
            Type[] upper = ((WildcardType) type).getUpperBounds();
            return upper.length == 0 || needsBinding(upper[0], visiting);
        }
        if (type instanceof TypeVariable) {
            return true;
        }
        if (type instanceof GenericArrayType) {
            return needsBinding(((GenericArrayType) type).getGenericComponentType(), visiting);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            Class<?> raw = rawClass(parameterized.getRawType());
            if (raw == null) {
                return true;
            }
            if (Map.class.isAssignableFrom(raw)) {
                Type[] args = parameterized.getActualTypeArguments();
                return args.length == 2 && needsBinding(args[1], visiting);
            }
            if (Collection.class.isAssignableFrom(raw)) {
                Type[] args = parameterized.getActualTypeArguments();
                return args.length >= 1 && needsBinding(args[0], visiting);
            }
            if (needsBindingClass(raw, visiting)) {
                return true;
            }
            Type[] args = parameterized.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                if (needsBinding(args[i], visiting)) {
                    return true;
                }
            }
            return false;
        }
        if (type instanceof Class) {
            Class<?> raw = (Class<?>) type;
            if (raw.isArray()) {
                return needsBinding(raw.getComponentType(), visiting);
            }
            return needsBindingClass(raw, visiting);
        }
        return true;
    }

    private boolean needsBindingClass(Class<?> raw, Set<Class<?>> visiting) {
        if (shouldSkip(raw)) {
            return false;
        }
        if (!visiting.add(raw)) {
            return false;
        }
        try {
            BindingMeta meta = bindings.get(raw);
            if (hasLocalDeserializeBinding(meta)) {
                return true;
            }
            for (Field field : meta.getFields()) {
                BindingMeta.FieldBinding binding = meta.bindingOf(field);
                if (binding != null && binding.ignoreDeserialize) {
                    continue;
                }
                if (needsBinding(field.getGenericType(), visiting)) {
                    return true;
                }
            }
            return false;
        } finally {
            visiting.remove(raw);
        }
    }

    static boolean hasLocalDeserializeBinding(BindingMeta meta) {
        if (meta == null) {
            return false;
        }
        if (!meta.getKeyRemapToFieldName().isEmpty()) {
            return true;
        }
        if (!meta.getKeysToDrop().isEmpty()) {
            return true;
        }
        for (BindingMeta.FieldBinding binding : meta.getFieldBindings()) {
            if (binding.hasDeserializeConverter()) {
                return true;
            }
        }
        return false;
    }

    private void ensureReadersForGraph(Type type, Set<Class<?>> visiting) {
        if (type == null) {
            return;
        }
        if (type instanceof WildcardType) {
            Type[] upper = ((WildcardType) type).getUpperBounds();
            if (upper.length > 0) {
                ensureReadersForGraph(upper[0], visiting);
            }
            return;
        }
        if (type instanceof TypeVariable) {
            return;
        }
        if (type instanceof GenericArrayType) {
            ensureReadersForGraph(((GenericArrayType) type).getGenericComponentType(), visiting);
            return;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            ensureReadersForClass(rawClass(parameterized.getRawType()), visiting);
            Type[] args = parameterized.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                ensureReadersForGraph(args[i], visiting);
            }
            return;
        }
        if (type instanceof Class) {
            Class<?> raw = (Class<?>) type;
            if (raw.isArray()) {
                ensureReadersForGraph(raw.getComponentType(), visiting);
            } else {
                ensureReadersForClass(raw, visiting);
            }
        }
    }


    private void ensureReadersForClass(Class<?> raw, Set<Class<?>> visiting) {
        if (raw == null || shouldSkip(raw) || !visiting.add(raw)) {
            return;
        }
        BindingMeta meta = bindings.get(raw);
        if (hasLocalDeserializeBinding(meta) && registered.putIfAbsent(raw, Boolean.TRUE) == null) {
            try {
                ObjectReader<?> reader = Fastjson2BindingObjectReader.create(raw, meta, strategies);
                provider.register(raw, reader);
            } catch (RuntimeException e) {
                registered.remove(raw);
                throw e;
            }
        }
        for (Field field : meta.getFields()) {
            BindingMeta.FieldBinding binding = meta.bindingOf(field);
            if (binding != null && binding.ignoreDeserialize) {
                continue;
            }
            ensureReadersForGraph(field.getGenericType(), visiting);
        }
    }

    private static boolean shouldSkip(Class<?> raw) {
        if (raw == null || raw.isInterface() || raw.isPrimitive() || raw.isEnum()) {
            return true;
        }
        if (raw.isArray()) {
            return true;
        }
        if (Map.class.isAssignableFrom(raw) || Collection.class.isAssignableFrom(raw)) {
            return true;
        }
        if (raw == Object.class) {
            return true;
        }
        String name = raw.getName();
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("android.")
                || name.startsWith("androidx.")
                || name.startsWith("kotlin.")
                || name.startsWith("scala.")
                || name.startsWith("com.alibaba.fastjson2.");
    }

    static Class<?> rawClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            if (raw instanceof Class) {
                return (Class<?>) raw;
            }
        }
        if (type instanceof GenericArrayType) {
            Type component = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = rawClass(component);
            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            }
        }
        return null;
    }
}
