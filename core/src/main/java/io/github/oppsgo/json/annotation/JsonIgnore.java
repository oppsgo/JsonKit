package io.github.oppsgo.json.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ignores a field on serialize and/or deserialize.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonIgnore {
    /** When {@code true}, the field is omitted from serialized JSON. */
    boolean serialize() default true;

    /** When {@code true}, incoming JSON for this field is ignored. */
    boolean deserialize() default true;
}
