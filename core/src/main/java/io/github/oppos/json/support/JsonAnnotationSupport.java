package io.github.oppos.json.support;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.oppos.json.annotation.JsonAlias;
import io.github.oppos.json.annotation.JsonIgnore;
import io.github.oppos.json.annotation.JsonIgnoreProperties;
import io.github.oppos.json.annotation.JsonProperty;

/**
 * Shared reflection helpers for reading JsonKit JSON annotations.
 * Intended for adapter implementations, not typical application code.
 */
public final class JsonAnnotationSupport {

    private JsonAnnotationSupport() {
    }

    /**
     * JSON property name for {@code field}: {@link JsonProperty#value()} when non-empty,
     * otherwise the Java field name.
     */
    public static String jsonName(Field field) {
        JsonProperty property = field.getAnnotation(JsonProperty.class);
        if (property != null && property.value() != null && !property.value().isEmpty()) {
            return property.value();
        }
        return field.getName();
    }

    /**
     * Whether {@code field} should be omitted when serializing.
     */
    public static boolean shouldIgnoreSerialize(Field field) {
        if (isIgnoredByClass(field)) {
            return true;
        }
        JsonIgnore ignore = field.getAnnotation(JsonIgnore.class);
        return ignore != null && ignore.serialize();
    }

    /**
     * Whether {@code field} should be ignored when deserializing.
     */
    public static boolean shouldIgnoreDeserialize(Field field) {
        if (isIgnoredByClass(field)) {
            return true;
        }
        JsonIgnore ignore = field.getAnnotation(JsonIgnore.class);
        return ignore != null && ignore.deserialize();
    }

    private static boolean isIgnoredByClass(Field field) {
        Set<String> ignored = ignoredPropertyNames(field.getDeclaringClass());
        if (ignored.isEmpty()) {
            return false;
        }
        return ignored.contains(field.getName()) || ignored.contains(jsonName(field));
    }

    /**
     * Names listed by {@link JsonIgnoreProperties} on {@code type} and its superclasses.
     * Entries may match either the Java field name or the JSON name.
     */
    public static Set<String> ignoredPropertyNames(Class<?> type) {
        if (type == null) {
            return Collections.emptySet();
        }
        Set<String> names = new LinkedHashSet<String>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            JsonIgnoreProperties annotation = current.getAnnotation(JsonIgnoreProperties.class);
            if (annotation != null) {
                Collections.addAll(names, annotation.value());
            }
            current = current.getSuperclass();
        }
        return names;
    }

    /**
     * Deserialize-only alternate names from {@link JsonAlias}, or an empty array.
     */
    public static String[] aliases(Field field) {
        JsonAlias alias = field.getAnnotation(JsonAlias.class);
        if (alias == null) {
            return new String[0];
        }
        return alias.value();
    }

    /**
     * Instance fields of {@code type} and superclasses suitable for JSON binding
     * (skips {@code static} and {@code transient}).
     */
    public static List<Field> jsonFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Field[] declared = current.getDeclaredFields();
            for (int i = 0; i < declared.length; i++) {
                Field field = declared[i];
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Maps incoming JSON keys ({@link JsonProperty} name or {@link JsonAlias}) to the
     * Java <em>field name</em>. Used when the engine binds by field name (e.g. Gson paths
     * that rewrite the JSON object before parsing).
     */
    public static Map<String, String> deserializeKeyRemapToFieldName(Class<?> type) {
        Map<String, String> remap = new LinkedHashMap<String, String>();
        List<Field> fields = jsonFields(type);
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (shouldIgnoreDeserialize(field)) {
                continue;
            }
            String fieldName = field.getName();
            String canonical = jsonName(field);
            if (!canonical.equals(fieldName)) {
                remap.put(canonical, fieldName);
            }
            String[] aliasValues = aliases(field);
            for (int j = 0; j < aliasValues.length; j++) {
                String alias = aliasValues[j];
                if (alias != null && !alias.isEmpty() && !alias.equals(fieldName)) {
                    remap.put(alias, fieldName);
                }
            }
        }
        return remap;
    }

    /**
     * Maps {@link JsonAlias} keys to the canonical JSON name ({@link JsonProperty} or field name).
     * Used when the engine already honors the canonical name and only aliases need rewriting.
     */
    public static Map<String, String> deserializeAliasToCanonicalName(Class<?> type) {
        Map<String, String> remap = new LinkedHashMap<String, String>();
        List<Field> fields = jsonFields(type);
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (shouldIgnoreDeserialize(field)) {
                continue;
            }
            String canonical = jsonName(field);
            String[] aliasValues = aliases(field);
            for (int j = 0; j < aliasValues.length; j++) {
                String alias = aliasValues[j];
                if (alias != null && !alias.isEmpty() && !alias.equals(canonical)) {
                    remap.put(alias, canonical);
                }
            }
        }
        return remap;
    }

    /**
     * JSON / field / alias keys that should be dropped before deserialize because the
     * corresponding fields are ignored.
     */
    public static Set<String> deserializeKeysToDrop(Class<?> type) {
        Set<String> drop = new LinkedHashSet<String>();
        List<Field> fields = jsonFields(type);
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (!shouldIgnoreDeserialize(field)) {
                continue;
            }
            drop.add(field.getName());
            drop.add(jsonName(field));
            Collections.addAll(drop, aliases(field));
        }
        drop.addAll(ignoredPropertyNames(type));
        return drop;
    }
}
