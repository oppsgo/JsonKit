package io.github.oppsgo.json.reflect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Captures a generic {@link Type} despite erasure (same idea as Fastjson / Gson type tokens).
 * <p>
 * Common usage (Fastjson / Gson style):
 * <pre>{@code
 * new JsonTypeReference<List<User>>() {}
 * }</pre>
 * <p>
 * When the type argument still contains a type variable, pass the concrete types
 * (Fastjson {@code TypeReference(Type...)} style):
 * <pre>{@code
 * Class<User> userClass = ...;
 * new JsonTypeReference<ApiResponse<User>>(userClass) {}
 * }</pre>
 * <p>
 * Prefer a direct anonymous subclass. Nested subclasses of a named {@code JsonTypeReference}
 * subclass lose the generic signature (Fastjson would {@code ClassCastException}; we fail earlier).
 * Keep generic signatures if you use R8/ProGuard.
 */
public abstract class JsonTypeReference<T> {

    private final Type type;
    private final Class<? super T> rawType;

    /**
     * Reads {@code T} from the subclass signature:
     * {@code new JsonTypeReference<List<User>>() {}}.
     */
    protected JsonTypeReference() {
        Type captured = extractTypeArgument();
        this.type = Objects.requireNonNull(captured, "type");
        @SuppressWarnings("unchecked")
        Class<? super T> raw = (Class<? super T>) resolveRawClass(this.type);
        this.rawType = raw;
    }

    /**
     * Fastjson-style: capture {@code T} from the subclass, then replace {@link TypeVariable}s
     * left-to-right with {@code actualTypeArguments}.
     * <p>
     * Example: {@code new JsonTypeReference<Box<E>>(String.class) {}} → {@code Box<String>}.
     */
    protected JsonTypeReference(@NotNull Type... actualTypeArguments) {
        Objects.requireNonNull(actualTypeArguments, "actualTypeArguments");
        if (actualTypeArguments.length == 0) {
            throw new IllegalArgumentException("actualTypeArguments must not be empty");
        }
        Type pattern = extractTypeArgument();
        if (!(pattern instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "Type... constructor requires a parameterized type argument on "
                            + "JsonTypeReference, e.g. new JsonTypeReference<Box<E>>(String.class) {}");
        }
        this.type = replaceTypeVariables(
                (ParameterizedType) pattern, actualTypeArguments, new int[]{0});
        @SuppressWarnings("unchecked")
        Class<? super T> raw = (Class<? super T>) resolveRawClass(this.type);
        this.rawType = raw;
    }

    /**
     * Captured generic {@link Type} for use with {@code fromJson(..., reference)}.
     */
    @NotNull
    public final Type getType() {
        return type;
    }

    /**
     * Raw class of {@link #getType()} (Fastjson2 {@code getRawType()} style).
     */
    @NotNull
    public final Class<? super T> getRawType() {
        return rawType;
    }

    @NotNull
    private Type extractTypeArgument() {
        return firstTypeArgument(requireDirectJsonTypeReferenceSuperclass());
    }

    @NotNull
    private ParameterizedType requireDirectJsonTypeReferenceSuperclass() {
        return requireDirectJsonTypeReference(getClass().getGenericSuperclass());
    }

    @NotNull
    private static ParameterizedType requireDirectJsonTypeReference(@Nullable Type superclass) {
        if (!(superclass instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    "Create with a type argument: new JsonTypeReference<...>() {}. "
                            + "If this is a subclass-of-subclass, put the type on a direct subclass instead. "
                            + "With R8/ProGuard, keep generic signatures.");
        }
        ParameterizedType parameterized = (ParameterizedType) superclass;
        if (parameterized.getRawType() != JsonTypeReference.class) {
            throw new IllegalStateException(
                    "JsonTypeReference type must be declared on a direct subclass "
                            + "(new JsonTypeReference<...>() {} or class X extends JsonTypeReference<...>).");
        }
        return parameterized;
    }

    @NotNull
    private static Type firstTypeArgument(@NotNull ParameterizedType parameterized) {
        Type[] arguments = parameterized.getActualTypeArguments();
        if (arguments.length == 0) {
            throw new IllegalStateException(
                    "JsonTypeReference has no type argument; use new JsonTypeReference<...>() {}.");
        }
        return arguments[0];
    }

    /**
     * Replace {@link TypeVariable} slots in {@code pattern} with {@code actuals} in encounter order
     * (same approach as Fastjson 1.x / Fastjson2 {@code TypeReference(Type...)}).
     */
    @NotNull
    private static Type replaceTypeVariables(
            @NotNull ParameterizedType pattern,
            @NotNull Type[] actuals,
            int[] index) {
        Type[] args = pattern.getActualTypeArguments().clone();
        for (int i = 0; i < args.length; i++) {
            Type arg = args[i];
            if (arg instanceof TypeVariable && index[0] < actuals.length) {
                args[i] = Objects.requireNonNull(actuals[index[0]++], "actualTypeArgument");
            } else if (arg instanceof ParameterizedType) {
                args[i] = replaceTypeVariables((ParameterizedType) arg, actuals, index);
            } else if (arg instanceof GenericArrayType) {
                // Keep as-is; engines handle GenericArrayType.
                args[i] = arg;
            }
        }
        return new SimpleParameterizedType(pattern.getRawType(), args, pattern.getOwnerType());
    }

    @NotNull
    private static Class<?> resolveRawClass(@NotNull Type type) {
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
            return Object[].class;
        }
        if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            if (bounds.length > 0) {
                return resolveRawClass(bounds[0]);
            }
        }
        return Object.class;
    }

    private static final class SimpleParameterizedType implements ParameterizedType {
        private final Type rawType;
        private final Type[] arguments;
        @Nullable
        private final Type ownerType;

        SimpleParameterizedType(Type rawType, Type[] arguments, @Nullable Type ownerType) {
            this.rawType = Objects.requireNonNull(rawType, "rawType");
            this.arguments = arguments.clone();
            this.ownerType = ownerType;
        }

        @Override
        public Type @NotNull [] getActualTypeArguments() {
            return arguments.clone();
        }

        @NotNull
        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        @Nullable
        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType other = (ParameterizedType) o;
            return Objects.equals(rawType, other.getRawType())
                    && Objects.equals(ownerType, other.getOwnerType())
                    && Arrays.equals(arguments, other.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arguments)
                    ^ Objects.hashCode(ownerType)
                    ^ Objects.hashCode(rawType);
        }

        @NotNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(rawType instanceof Class
                    ? ((Class<?>) rawType).getName()
                    : rawType.toString());
            if (arguments.length == 0) {
                return sb.toString();
            }
            sb.append('<');
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Type arg = arguments[i];
                sb.append(arg instanceof Class ? ((Class<?>) arg).getName() : String.valueOf(arg));
            }
            sb.append('>');
            return sb.toString();
        }
    }
}
