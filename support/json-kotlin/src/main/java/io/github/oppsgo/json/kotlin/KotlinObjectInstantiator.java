package io.github.oppsgo.json.kotlin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.oppsgo.json.support.KotlinClasspath;
import io.github.oppsgo.json.support.ObjectInstantiator;

/**
 * Instantiator for Kotlin {@code kotlin.Metadata} types (STATE 1 / 2).
 * Hot path invokes a cached Java {@link Constructor} only.
 */
final class KotlinObjectInstantiator implements ObjectInstantiator {

    static final KotlinObjectInstantiator INSTANCE = new KotlinObjectInstantiator();

    private static final String MARKER_NAME = "kotlin.jvm.internal.DefaultConstructorMarker";

    /** Cached boxed zeros / false for missing Kotlin constructor args (API 19 safe). */
    private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS;

    static {
        Map<Class<?>, Object> defaults = new IdentityHashMap<>(16);
        defaults.put(boolean.class, Boolean.FALSE);
        defaults.put(byte.class, (byte) 0);
        defaults.put(short.class, (short) 0);
        defaults.put(int.class, 0);
        defaults.put(long.class, 0L);
        defaults.put(float.class, 0f);
        defaults.put(double.class, 0d);
        defaults.put(char.class, (char) 0);
        PRIMITIVE_DEFAULTS = defaults;
    }

    private final ConcurrentHashMap<Class<?>, KotlinClassBinder> binders =
            new ConcurrentHashMap<>();

    private KotlinObjectInstantiator() {
    }

    @Override
    public boolean supports(Class<?> type) {
        return KotlinClasspath.isKotlinClass(type) && KotlinClasspath.state() >= KotlinClasspath.STATE_STDLIB;
    }

    @Override
    public boolean constructsFromProperties(Class<?> type) {
        return supports(type);
    }

    @Override
    public Object instantiate(Class<?> type, Map<String, Object> properties)
            throws ReflectiveOperationException {
        KotlinClassBinder binder = binderFor(type);
        return binder.newInstance(properties);
    }

    KotlinClassBinder binderFor(Class<?> type) {
        KotlinClassBinder cached = binders.get(type);
        if (cached != null) {
            return cached;
        }
        KotlinClassBinder created = KotlinClassBinder.bind(type);
        KotlinClassBinder raced = binders.putIfAbsent(type, created);
        return raced != null ? raced : created;
    }

    /**
     * Cached per-type construction metadata.
     */
    static final class KotlinClassBinder {
        final Constructor<?> creator;
        final Constructor<?> marker;
        final String[] paramNames;
        final Class<?>[] paramTypes;
        final boolean[] hasDefault;
        final int paramCount;

        private KotlinClassBinder(
                Constructor<?> creator,
                Constructor<?> marker,
                String[] paramNames,
                boolean[] hasDefault) {
            this.creator = creator;
            this.marker = marker;
            this.paramNames = paramNames;
            this.paramTypes = creator.getParameterTypes();
            this.hasDefault = hasDefault;
            this.paramCount = paramNames.length;
            creator.setAccessible(true);
            if (marker != null) {
                marker.setAccessible(true);
            }
        }

        static KotlinClassBinder bind(Class<?> type) {
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            Constructor<?> marker = findMarkerConstructor(constructors);
            Constructor<?> creator = findCreatorConstructor(constructors, marker);
            if (creator == null) {
                throw new IllegalArgumentException("No Kotlin creator constructor for " + type.getName());
            }

            int count = creator.getParameterTypes().length;
            String[] names = resolveParameterNames(type, count);
            boolean[] defaults = new boolean[count];
            if (marker != null) {
                // Marker present ⇒ Kotlin defaults exist; treat missing JSON as defaultable.
                Arrays.fill(defaults, true);
            }

            return new KotlinClassBinder(creator, marker, names, defaults);
        }

        Object newInstance(Map<String, Object> properties) throws ReflectiveOperationException {
            Object[] args = new Object[paramCount];
            int missingMask = 0;
            boolean anyMissing = false;

            for (int i = 0; i < paramCount; i++) {
                String name = paramNames[i];
                Object value = null;
                boolean present = false;
                if (properties != null && name != null) {
                    if (properties.containsKey(name)) {
                        value = properties.get(name);
                        present = true;
                    }
                }
                if (!present && marker != null && hasDefault[i]) {
                    missingMask |= (1 << i);
                    anyMissing = true;
                    args[i] = defaultPrimitive(paramTypes[i]);
                } else if (!present) {
                    args[i] = defaultPrimitive(paramTypes[i]);
                } else {
                    args[i] = coerce(value, paramTypes[i]);
                }
            }

            if (anyMissing) {
                Object[] markerArgs = new Object[paramCount + 2];
                System.arraycopy(args, 0, markerArgs, 0, paramCount);
                markerArgs[paramCount] = missingMask;
                markerArgs[paramCount + 1] = null; // DefaultConstructorMarker
                return marker.newInstance(markerArgs);
            }
            return creator.newInstance(args);
        }

        private static Object coerce(Object value, Class<?> target) {
            if (value == null) {
                return defaultPrimitive(target);
            }
            if (target.isInstance(value)) {
                return value;
            }
            if (value instanceof Number) {
                Number n = (Number) value;
                if (target == int.class || target == Integer.class) {
                    return n.intValue();
                }
                if (target == long.class || target == Long.class) {
                    return n.longValue();
                }
                if (target == short.class || target == Short.class) {
                    return n.shortValue();
                }
                if (target == byte.class || target == Byte.class) {
                    return n.byteValue();
                }
                if (target == double.class || target == Double.class) {
                    return n.doubleValue();
                }
                if (target == float.class || target == Float.class) {
                    return n.floatValue();
                }
            }
            return value;
        }

        private static Object defaultPrimitive(Class<?> type) {
            return PRIMITIVE_DEFAULTS.get(type);
        }

        private static Constructor<?> findMarkerConstructor(Constructor<?>[] constructors) {
            for (Constructor<?> constructor : constructors) {
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length >= 2
                        && types[types.length - 2] == int.class
                        && MARKER_NAME.equals(types[types.length - 1].getName())) {
                    return constructor;
                }
            }
            return null;
        }

        private static Constructor<?> findCreatorConstructor(
                Constructor<?>[] constructors, Constructor<?> marker) {
            Constructor<?> best = null;
            int bestParams = -1;
            for (Constructor<?> constructor : constructors) {
                if (constructor == marker) {
                    continue;
                }
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length > 0 && MARKER_NAME.equals(types[types.length - 1].getName())) {
                    continue;
                }
                if (types.length > bestParams) {
                    bestParams = types.length;
                    best = constructor;
                }
            }
            return best;
        }

        private static String[] resolveParameterNames(Class<?> type, int count) {
            if (KotlinClasspath.hasReflect()) {
                String[] fromReflect = resolveViaReflect(type, count);
                if (fromReflect != null) {
                    return fromReflect;
                }
            }

            // STATE 1 (API 19): avoid Constructor.getParameters() / Parameter (API 26+).
            // Align constructor args with instance field declaration order.
            return namesFromFields(type, count);
        }

        private static String[] namesFromFields(Class<?> type, int count) {
            List<String> fieldNames = new ArrayList<>();
            Class<?> current = type;
            while (current != null && current != Object.class) {
                Field[] declared = current.getDeclaredFields();
                for (Field field : declared) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                        continue;
                    }
                    if (field.isSynthetic() || field.getName().indexOf('$') >= 0) {
                        continue;
                    }
                    fieldNames.add(field.getName());
                }
                current = current.getSuperclass();
            }
            String[] names = new String[count];
            for (int i = 0; i < count; i++) {
                names[i] = i < fieldNames.size() ? fieldNames.get(i) : ("arg" + i);
            }
            return names;
        }

        private static String[] resolveViaReflect(Class<?> type, int count) {
            try {
                Class<?> reflectionClass = Class.forName("kotlin.jvm.internal.Reflection");
                Method getOrCreate = reflectionClass.getMethod("getOrCreateKotlinClass", Class.class);
                Object kClass = getOrCreate.invoke(null, type);

                Method getConstructors = kClass.getClass().getMethod("getConstructors");
                Iterable<?> constructors = (Iterable<?>) getConstructors.invoke(kClass);

                Object best = null;
                List<?> bestParams = null;
                for (Object function : constructors) {
                    Method getParameters = function.getClass().getMethod("getParameters");
                    List<?> parameters = (List<?>) getParameters.invoke(function);
                    if (bestParams == null || parameters.size() == count) {
                        best = function;
                        bestParams = parameters;
                    }
                }
                if (best == null || bestParams == null || bestParams.size() != count) {
                    return null;
                }

                String[] names = new String[count];
                for (int i = 0; i < count; i++) {
                    Object param = bestParams.get(i);
                    Method getName = param.getClass().getMethod("getName");
                    names[i] = (String) getName.invoke(param);
                }
                return names;
            } catch (Throwable e) {
                return null;
            }
        }
    }
}
