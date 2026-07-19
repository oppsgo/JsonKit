package io.github.oppsgo.json.convert;

/**
 * Converts a Java field value to a JSON-tree value
 * ({@code null}, {@link Boolean}, {@link Number}, {@link String},
 * {@link java.util.List}, or {@link java.util.Map}).
 *
 * @param <T> Java field type
 */
public interface JsonFieldSerializer<T> {
    /**
     * Serializes {@code value} (never {@code null} when invoked by JsonKit adapters).
     */
    Object serialize(T value);
}
