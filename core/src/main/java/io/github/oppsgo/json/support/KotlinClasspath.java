package io.github.oppsgo.json.support;

/**
 * Classpath capability for Kotlin binding (Fastjson2-style STATE tiers).
 * <p>
 * Detection uses {@link Class#forName(String)} only — {@code :core} has no hard
 * Kotlin dependency. Values:
 * <ul>
 *   <li>{@link #STATE_NONE} (0) — no {@code kotlin.Metadata} on classpath</li>
 *   <li>{@link #STATE_STDLIB} (1) — stdlib / Metadata present</li>
 *   <li>{@link #STATE_REFLECT} (2) — STATE 1 + {@code kotlin-reflect}</li>
 * </ul>
 */
public final class KotlinClasspath {

    public static final int STATE_NONE = 0;
    public static final int STATE_STDLIB = 1;
    public static final int STATE_REFLECT = 2;

    private static final int STATE;

    static {
        int state = STATE_NONE;
        try {
            Class.forName("kotlin.Metadata");
            state = STATE_STDLIB;
            Class.forName("kotlin.reflect.jvm.ReflectJvmMapping");
            state = STATE_REFLECT;
        } catch (Throwable ignored) {
            // Keep highest successfully detected tier.
        }
        STATE = state;
    }

    private KotlinClasspath() {
    }

    /**
     * Current classpath STATE (0 / 1 / 2).
     */
    public static int state() {
        return STATE;
    }

    /**
     * Whether {@code kotlin.Metadata} is loadable.
     */
    public static boolean hasStdlib() {
        return STATE >= STATE_STDLIB;
    }

    /**
     * Whether {@code kotlin-reflect} mapping APIs are loadable.
     */
    public static boolean hasReflect() {
        return STATE >= STATE_REFLECT;
    }

    /**
     * Whether {@code type} is annotated with {@code kotlin.Metadata}.
     * Safe when Kotlin is absent (returns {@code false}).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean isKotlinClass(Class<?> type) {
        if (type == null || STATE < STATE_STDLIB) {
            return false;
        }
        Class metadata = metadataClass();
        return metadata != null && type.getAnnotation(metadata) != null;
    }

    private static Class<?> metadataClass() {
        try {
            return Class.forName("kotlin.Metadata");
        } catch (Throwable e) {
            return null;
        }
    }
}
