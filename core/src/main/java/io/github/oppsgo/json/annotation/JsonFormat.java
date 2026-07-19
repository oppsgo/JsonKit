package io.github.oppsgo.json.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Formats a temporal field as a patterned string or epoch milliseconds.
 * Overridden by {@link JsonSerialize} / {@link JsonDeserialize} on the same direction.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonFormat {
    /**
     * Date/time pattern for {@link Shape#STRING}. Required (non-empty) when shape is STRING.
     */
    String pattern() default "";

    /**
     * Timezone id for patterned formatting. Empty means UTC.
     */
    String timezone() default "";

    /**
     * JSON representation shape.
     */
    Shape shape() default Shape.STRING;

    /**
     * How the temporal value is written to JSON.
     */
    enum Shape {
        /** Patterned string ({@link #pattern()}). */
        STRING,
        /** Epoch milliseconds as a JSON number. */
        NUMBER
    }
}
