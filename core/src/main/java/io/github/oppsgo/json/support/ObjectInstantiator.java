package io.github.oppsgo.json.support;

import java.util.Map;

/**
 * Constructs model instances during deserialize.
 * <p>
 * The default implementation uses a no-arg constructor; callers then apply
 * fields via reflection. Optional Kotlin support registers an Instantiator that
 * builds {@code kotlin.Metadata} types from constructor arguments instead.
 */
public interface ObjectInstantiator {

    /**
     * Whether this Instantiator should construct {@code type}.
     */
    boolean supports(Class<?> type);

    /**
     * When {@code true}, {@link #instantiate} builds a complete object from
     * {@code properties} (keyed by Java field name) and callers must not
     * {@link java.lang.reflect.Field#set} afterwards. When {@code false},
     * {@link #instantiate} returns an empty shell and callers apply fields.
     */
    boolean constructsFromProperties(Class<?> type);

    /**
     * Creates an instance of {@code type}.
     *
     * @param properties Java field name → value (may be empty; ignored when
     *                   {@link #constructsFromProperties} is false)
     */
    Object instantiate(Class<?> type, Map<String, Object> properties) throws ReflectiveOperationException;
}
