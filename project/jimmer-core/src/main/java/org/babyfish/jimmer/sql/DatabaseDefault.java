package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a scalar column property whose insert-time default value is generated
 * by the database.
 *
 * <p>When inserting an object, if this property is unloaded, Jimmer omits the
 * column from insert SQL so that the database default can take effect. The
 * optional {@link #value()} is treated as database schema metadata/documentation
 * only; Jimmer does not parse it and does not render it into insert SQL.</p>
 *
 * @see Default
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface DatabaseDefault {

    String value() default "";
}
