package io.github.oppos.json.adapter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import io.github.oppos.json.JsonOptions;
import io.github.oppos.json.reflect.JsonTypeReference;

/**
 * Engine-backed JSON serialize/deserialize contract.
 * <p>
 * {@link Reader}/{@link Writer} overloads must not close the streams; the caller owns lifecycle.
 * Null handling and parse/IO failures follow the backing engine (typically {@code null} for
 * null inputs where applicable, and unchecked exceptions on malformed JSON).
 */
public interface JsonAdapter {

    /**
     * Supplies a {@link JsonAdapter}. Options (if any) are closed over when the factory is
     * created/registered — not passed at {@link io.github.oppos.json.JsonKit#getDefault()} time.
     * <p>
     * {@link #create()} may return a cached instance or a fresh one; library
     * {@code XxxAdapterFactory} implementations always reuse one adapter.
     * Defined in-library (not {@code java.util.function.Supplier}) for Android API levels
     * below 24 without core library desugaring.
     */
    interface Factory {
        /**
         * Returns an adapter; may be shared across calls.
         */
        @NotNull
        JsonAdapter create();
    }

    /**
     * Options this adapter was created with. Returned value is a defensive copy.
     */
    @NotNull
    JsonOptions getOptions();

    /**
     * Serializes {@code object} to a JSON string.
     */
    String toJson(Object object);

    /**
     * Serializes {@code object} to {@code writer}. Must not close the writer.
     */
    void toJson(Object object, @NotNull Writer writer) throws IOException;

    /**
     * Deserializes JSON text to {@code clazz}.
     */
    <T> T fromJson(String json, @NotNull Class<T> clazz);

    /**
     * Deserializes JSON text to a reflective {@link Type} (including parameterized types).
     */
    <T> T fromJson(String json, @NotNull Type type);

    /**
     * Deserializes JSON text using a captured generic type from {@link JsonTypeReference}.
     */
    <T> T fromJson(String json, @NotNull JsonTypeReference<T> reference);

    /**
     * Deserializes from {@code reader} to {@code clazz}. Must not close the reader.
     */
    <T> T fromJson(@NotNull Reader reader, @NotNull Class<T> clazz) throws IOException;

    /**
     * Deserializes from {@code reader} to a reflective {@link Type}. Must not close the reader.
     */
    <T> T fromJson(@NotNull Reader reader, @NotNull Type type) throws IOException;

    /**
     * Deserializes from {@code reader} using {@link JsonTypeReference}. Must not close the reader.
     */
    <T> T fromJson(@NotNull Reader reader, @NotNull JsonTypeReference<T> reference)
            throws IOException;
}
