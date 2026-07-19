package io.github.oppsgo.json.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.oppsgo.json.convert.JsonFieldDeserializer;

/**
 * Selects a custom deserializer for this field.
 * Takes precedence over {@link JsonFormat} on deserialize.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonDeserialize {
    /** Deserializer class; must have a public no-arg constructor. */
    Class<? extends JsonFieldDeserializer<?>> using();
}
