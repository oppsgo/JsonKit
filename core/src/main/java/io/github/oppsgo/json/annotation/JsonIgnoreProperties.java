package io.github.oppsgo.json.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level ignore list for serialize and deserialize.
 * <p>
 * Each entry may match either the Java field name or the JSON property name
 * ({@link JsonProperty}).
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsonIgnoreProperties {
    /**
     * Field names and/or JSON names to ignore.
     */
    String[] value() default {};
}
