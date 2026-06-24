package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * Marks an owning reference whose target id is mapped into the id of the declaring entity.
 *
 * <p>If {@link #value()} is empty, the whole id of the declaring entity is mapped.
 * Otherwise, the value is a dot-separated path inside the declaring id.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface MapsId {

    String value() default "";
}
