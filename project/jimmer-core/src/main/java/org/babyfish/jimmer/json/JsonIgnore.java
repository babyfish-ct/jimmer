package org.babyfish.jimmer.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Excludes a logical model property from JSON input and output. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface JsonIgnore {

    /** Set to false to include a property ignored by its model declaration. */
    boolean value() default true;
}
