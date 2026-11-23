package org.babyfish.jimmer.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Controls the JSON representation of a scalar model or DTO property. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface JsonFormat {

    /** Date pattern for java.util.Date, LocalDate or LocalDateTime. */
    String pattern() default "";

    Shape shape() default Shape.ANY;

    enum Shape {
        ANY,
        /** Write numeric values as JSON strings. */
        STRING
    }
}
