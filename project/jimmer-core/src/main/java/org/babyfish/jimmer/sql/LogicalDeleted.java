package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Logical deleted flag.
 *
 * <p>
 *     If property is decorated by this @annotation, its initial value is `restoredValue`
 *     so that developers do not need to use `DraftInterceptor` to assign an initial value to it
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface LogicalDeleted {

    /**
     * @return A value indicating that the current entity is logical deleted, it can be
     * <ul>
     *     <li>true</li>
     *     <li>false</li>
     *     <li>integer</li>
     *     <li>Constant name of the enum returned by the current decorated property</li>
     *     <li>null</li>
     *     <li>now</li>
     * </ul>
     */
    String value();
}
