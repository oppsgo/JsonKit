package io.github.oppsgo.json.support;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared reflection helpers for reading JsonKit JSON annotations.
 * Intended for adapter implementations, not typical application code.
 * <p>
 * Class-level helpers delegate to an uncached {@link BindingMeta#scan(Class)}.
 * Hot paths should use an adapter-owned {@link BindingCache} instead.
 */
public final class JsonAnnotationSupport {

    private JsonAnnotationSupport() {
    }

    /**
     * JSON property name for {@code field}: {@link io.github.oppsgo.json.annotation.JsonProperty#value()}
     * when non-empty, otherwise the Java field name.
     */
    public static String jsonName(Field field) {
        return BindingMeta.FieldBinding.from(field).jsonName;
    }

    /**
     * Whether {@code field} should be omitted when serializing.
     */
    public static boolean shouldIgnoreSerialize(Field field) {
        return BindingMeta.FieldBinding.from(field).ignoreSerialize;
    }

    /**
     * Whether {@code field} should be ignored when deserializing.
     */
    public static boolean shouldIgnoreDeserialize(Field field) {
        return BindingMeta.FieldBinding.from(field).ignoreDeserialize;
    }

    /**
     * Names listed by {@link io.github.oppsgo.json.annotation.JsonIgnoreProperties} on {@code type}
     * and its superclasses.
     */
    public static Set<String> ignoredPropertyNames(Class<?> type) {
        if (type == null) {
            return Collections.emptySet();
        }
        return BindingMeta.scan(type).getIgnoredPropertyNames();
    }

    /**
     * Deserialize-only alternate names from {@link io.github.oppsgo.json.annotation.JsonAlias},
     * or an empty array.
     */
    public static String[] aliases(Field field) {
        String[] aliases = BindingMeta.FieldBinding.from(field).aliases;
        return aliases.length == 0 ? aliases : aliases.clone();
    }

    /**
     * Instance fields of {@code type} and superclasses suitable for JSON binding
     * (skips {@code static} and {@code transient}).
     */
    public static List<Field> jsonFields(Class<?> type) {
        if (type == null) {
            return Collections.emptyList();
        }
        return BindingMeta.scan(type).getFields();
    }

    /**
     * Maps incoming JSON keys to the Java field name.
     */
    public static Map<String, String> deserializeKeyRemapToFieldName(Class<?> type) {
        if (type == null) {
            return Collections.emptyMap();
        }
        return BindingMeta.scan(type).getKeyRemapToFieldName();
    }

    /**
     * Maps {@link io.github.oppsgo.json.annotation.JsonAlias} keys to the canonical JSON name.
     */
    public static Map<String, String> deserializeAliasToCanonicalName(Class<?> type) {
        if (type == null) {
            return Collections.emptyMap();
        }
        return BindingMeta.scan(type).getAliasToCanonicalName();
    }

    /**
     * JSON / field / alias keys that should be dropped before deserialize.
     */
    public static Set<String> deserializeKeysToDrop(Class<?> type) {
        if (type == null) {
            return Collections.emptySet();
        }
        return BindingMeta.scan(type).getKeysToDrop();
    }

    /**
     * Per-field binding snapshot (name, ignore, aliases, converters, format).
     */
    public static BindingMeta.FieldBinding fieldBinding(Field field) {
        return BindingMeta.FieldBinding.from(field);
    }
}
