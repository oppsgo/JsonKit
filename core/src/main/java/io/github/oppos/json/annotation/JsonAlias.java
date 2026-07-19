package io.github.oppos.json.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alternate JSON names accepted during deserialization only (not used when serializing).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonAlias {
    /**
     * One or more alternate JSON keys that map to this field when deserializing.
     */
    String[] value();
}
