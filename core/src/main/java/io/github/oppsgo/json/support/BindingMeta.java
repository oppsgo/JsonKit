package io.github.oppsgo.json.support;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.oppsgo.json.annotation.JsonAlias;
import io.github.oppsgo.json.annotation.JsonDeserialize;
import io.github.oppsgo.json.annotation.JsonFormat;
import io.github.oppsgo.json.annotation.JsonIgnore;
import io.github.oppsgo.json.annotation.JsonIgnoreProperties;
import io.github.oppsgo.json.annotation.JsonProperty;
import io.github.oppsgo.json.annotation.JsonSerialize;
import io.github.oppsgo.json.convert.FormatSpec;
import io.github.oppsgo.json.convert.JsonFieldDeserializer;
import io.github.oppsgo.json.convert.JsonFieldSerializer;

/**
 * Immutable, per-{@link Class} view of JsonKit annotation binding data.
 * Built by {@link #scan(Class)}; typically retrieved via {@link BindingCache}.
 */
public final class BindingMeta {

    private static final String[] NO_ALIASES = new String[0];

    /**
     * Scanned type (including superclass fields collected under this type).
     */
    private final Class<?> type;
    /**
     * Declared instance fields of {@link #type} and its superclasses (static/transient excluded).
     */
    private final List<Field> fields;
    /**
     * Java field name → {@link Field}; last-wins if a subclass shadows a name.
     */
    private final Map<String, Field> fieldByJavaName;
    /**
     * Per-field annotation snapshot ({@link JsonProperty}, {@link JsonAlias}, {@link JsonIgnore}, …).
     */
    private final Map<Field, FieldBinding> bindingByField;
    /**
     * Property names from {@link JsonIgnoreProperties} on {@link #type} and superclasses;
     * used for class-level ignore and merge into {@link #keysToDrop}.
     */
    private final Set<String> ignoredPropertyNames;
    /**
     * Deserialize key remap: JSON name or alias → Java field name
     * (e.g. {@code user_name} → {@code userName}).
     */
    private final Map<String, String> keyRemapToFieldName;
    /**
     * Alias → canonical JSON name from {@link JsonProperty} (for engines that rename after parse).
     */
    private final Map<String, String> aliasToCanonicalName;
    /**
     * JSON keys to strip on deserialize: ignore-on-deserialize fields, their aliases,
     * and {@link #ignoredPropertyNames}.
     */
    private final Set<String> keysToDrop;

    private BindingMeta(
            Class<?> type,
            List<Field> fields,
            Map<String, Field> fieldByJavaName,
            Map<Field, FieldBinding> bindingByField,
            Set<String> ignoredPropertyNames,
            Map<String, String> keyRemapToFieldName,
            Map<String, String> aliasToCanonicalName,
            Set<String> keysToDrop) {
        this.type = type;
        this.fields = fields;
        this.fieldByJavaName = fieldByJavaName;
        this.bindingByField = bindingByField;
        this.ignoredPropertyNames = ignoredPropertyNames;
        this.keyRemapToFieldName = keyRemapToFieldName;
        this.aliasToCanonicalName = aliasToCanonicalName;
        this.keysToDrop = keysToDrop;
    }

    /**
     * Scans {@code type} and its superclasses using the same rules as
     * {@link JsonAnnotationSupport}. Does not cache; callers should use {@link BindingCache}.
     */
    public static BindingMeta scan(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type == null");
        }

        Set<String> ignoredNames = scanIgnoredPropertyNames(type);
        List<Field> collected = collectInstanceFields(type);

        Map<String, Field> byJavaName = new LinkedHashMap<String, Field>();
        Map<Field, FieldBinding> byField = new HashMap<Field, FieldBinding>();
        Map<String, String> keyRemap = new LinkedHashMap<String, String>();
        Map<String, String> aliasToCanonical = new LinkedHashMap<String, String>();
        Set<String> drop = new LinkedHashSet<String>();

        for (int i = 0; i < collected.size(); i++) {
            Field field = collected.get(i);
            FieldBinding binding = FieldBinding.from(field);
            byField.put(field, binding);
            byJavaName.put(field.getName(), field);

            String fieldName = field.getName();
            String canonical = binding.jsonName;

            if (binding.ignoreDeserialize) {
                drop.add(fieldName);
                drop.add(canonical);
                Collections.addAll(drop, binding.aliases);
                continue;
            }

            if (!canonical.equals(fieldName)) {
                keyRemap.put(canonical, fieldName);
            }
            for (int j = 0; j < binding.aliases.length; j++) {
                String alias = binding.aliases[j];
                if (alias == null || alias.isEmpty()) {
                    continue;
                }
                if (!alias.equals(fieldName)) {
                    keyRemap.put(alias, fieldName);
                }
                if (!alias.equals(canonical)) {
                    aliasToCanonical.put(alias, canonical);
                }
            }
        }
        drop.addAll(ignoredNames);

        return new BindingMeta(
                type,
                Collections.unmodifiableList(collected),
                Collections.unmodifiableMap(byJavaName),
                Collections.unmodifiableMap(byField),
                ignoredNames.isEmpty()
                        ? Collections.<String>emptySet()
                        : Collections.unmodifiableSet(ignoredNames),
                keyRemap.isEmpty()
                        ? Collections.<String, String>emptyMap()
                        : Collections.unmodifiableMap(keyRemap),
                aliasToCanonical.isEmpty()
                        ? Collections.<String, String>emptyMap()
                        : Collections.unmodifiableMap(aliasToCanonical),
                drop.isEmpty()
                        ? Collections.<String>emptySet()
                        : Collections.unmodifiableSet(drop));
    }

    public Class<?> getType() {
        return type;
    }

    public List<Field> getFields() {
        return fields;
    }

    public Field findField(String javaName) {
        return fieldByJavaName.get(javaName);
    }

    /**
     * Resolves a field by Java name or canonical JSON name.
     */
    public Field findFieldByJavaOrJsonName(String name) {
        if (name == null) {
            return null;
        }
        Field byJava = fieldByJavaName.get(name);
        if (byJava != null) {
            return byJava;
        }
        for (Map.Entry<Field, FieldBinding> entry : bindingByField.entrySet()) {
            if (name.equals(entry.getValue().jsonName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Binding snapshot for {@code field}, or {@code null} if unknown.
     */
    public FieldBinding bindingOf(Field field) {
        return bindingByField.get(field);
    }

    /**
     * All per-field binding snapshots for this type.
     */
    public Collection<FieldBinding> getFieldBindings() {
        return bindingByField.values();
    }

    public Set<String> getIgnoredPropertyNames() {
        return ignoredPropertyNames;
    }

    public Map<String, String> getKeyRemapToFieldName() {
        return keyRemapToFieldName;
    }

    public Map<String, String> getAliasToCanonicalName() {
        return aliasToCanonicalName;
    }

    public Set<String> getKeysToDrop() {
        return keysToDrop;
    }

    public String jsonName(Field field) {
        FieldBinding binding = bindingByField.get(field);
        if (binding != null) {
            return binding.jsonName;
        }
        return JsonAnnotationSupport.jsonName(field);
    }

    public boolean shouldIgnoreSerialize(Field field) {
        FieldBinding binding = bindingByField.get(field);
        if (binding != null) {
            return binding.ignoreSerialize;
        }
        return JsonAnnotationSupport.shouldIgnoreSerialize(field);
    }

    public boolean shouldIgnoreDeserialize(Field field) {
        FieldBinding binding = bindingByField.get(field);
        if (binding != null) {
            return binding.ignoreDeserialize;
        }
        return JsonAnnotationSupport.shouldIgnoreDeserialize(field);
    }

    /**
     * Collects non-static, non-transient instance fields from {@code type} and its superclasses
     * (subclass fields first, then ancestors).
     */
    private static List<Field> collectInstanceFields(Class<?> type) {
        List<Field> collected = new ArrayList<Field>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Field[] declared = current.getDeclaredFields();
            for (int i = 0; i < declared.length; i++) {
                Field field = declared[i];
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }
                collected.add(field);
            }
            current = current.getSuperclass();
        }
        return collected;
    }

    private static Set<String> scanIgnoredPropertyNames(Class<?> type) {
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
     * Per-field annotation binding snapshot.
     */
    public static final class FieldBinding {
        public final String jsonName;
        public final String[] aliases;
        public final boolean ignoreSerialize;
        public final boolean ignoreDeserialize;
        /**
         * Declared generic type of the field (for converters).
         */
        public final Type fieldType;
        /**
         * Custom serializer class, or {@code null}.
         */
        public final Class<? extends JsonFieldSerializer<?>> serializeUsing;
        /**
         * Custom deserializer class, or {@code null}.
         */
        public final Class<? extends JsonFieldDeserializer<?>> deserializeUsing;
        /**
         * {@link JsonFormat} snapshot, or {@code null}.
         */
        public final FormatSpec format;

        private FieldBinding(
                String jsonName,
                String[] aliases,
                boolean ignoreSerialize,
                boolean ignoreDeserialize,
                Type fieldType,
                Class<? extends JsonFieldSerializer<?>> serializeUsing,
                Class<? extends JsonFieldDeserializer<?>> deserializeUsing,
                FormatSpec format) {
            this.jsonName = jsonName;
            this.aliases = aliases;
            this.ignoreSerialize = ignoreSerialize;
            this.ignoreDeserialize = ignoreDeserialize;
            this.fieldType = fieldType;
            this.serializeUsing = serializeUsing;
            this.deserializeUsing = deserializeUsing;
            this.format = format;
        }

        public boolean hasSerializeConverter() {
            return serializeUsing != null || format != null;
        }

        public boolean hasDeserializeConverter() {
            return deserializeUsing != null || format != null;
        }

        static FieldBinding from(Field field) {
            JsonProperty property = field.getAnnotation(JsonProperty.class);
            String jsonName = field.getName();
            if (property != null && property.value() != null && !property.value().isEmpty()) {
                jsonName = property.value();
            }

            JsonAlias alias = field.getAnnotation(JsonAlias.class);
            String[] aliases = NO_ALIASES;
            if (alias != null && alias.value() != null && alias.value().length > 0) {
                aliases = Arrays.copyOf(alias.value(), alias.value().length);
            }

            // Match JsonAnnotationSupport#isIgnoredByClass: ignored names from declaring class hierarchy.
            Set<String> ignoredByDeclaring = scanIgnoredPropertyNames(field.getDeclaringClass());
            boolean ignoredByClass = !ignoredByDeclaring.isEmpty()
                    && (ignoredByDeclaring.contains(field.getName())
                    || ignoredByDeclaring.contains(jsonName));

            JsonIgnore ignore = field.getAnnotation(JsonIgnore.class);
            boolean ignoreSerialize = ignoredByClass || (ignore != null && ignore.serialize());
            boolean ignoreDeserialize = ignoredByClass || (ignore != null && ignore.deserialize());

            JsonSerialize serialize = field.getAnnotation(JsonSerialize.class);
            Class<? extends JsonFieldSerializer<?>> serializeUsing =
                    serialize != null ? serialize.using() : null;

            JsonDeserialize deserialize = field.getAnnotation(JsonDeserialize.class);
            Class<? extends JsonFieldDeserializer<?>> deserializeUsing =
                    deserialize != null ? deserialize.using() : null;

            FormatSpec format = FormatSpec.from(field.getAnnotation(JsonFormat.class));

            return new FieldBinding(
                    jsonName,
                    aliases,
                    ignoreSerialize,
                    ignoreDeserialize,
                    field.getGenericType(),
                    serializeUsing,
                    deserializeUsing,
                    format);
        }
    }
}
