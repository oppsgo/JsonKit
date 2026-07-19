package io.github.oppsgo.json.convert;

import java.lang.reflect.Type;

/**
 * Converts a JSON-tree value into a Java field value.
 *
 * @param <T> Java field type
 */
public interface JsonFieldDeserializer<T> {
    /**
     * Deserializes {@code jsonValue} (never JSON null when invoked by JsonKit adapters).
     *
     * @param jsonValue JSON-tree value
     * @param fieldType declared field type
     */
    T deserialize(Object jsonValue, Type fieldType);
}
