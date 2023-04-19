package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * In general, developers should use {@link ManyToMany} to map many-to-many associations,
 * and the corresponding middle table should only have two foreign key fields.
 *
 * However, if developer want to add more business fields to the middle table,
 * {@link ManyToMany} is no longer applicable and this annotation is unique choice.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface ManyToManyView {

    /**
     * The name of a one-to-many association property
     * pointing from the current entity to the middle entity
     */
    String prop();

    /**
     * The name of a many-to-one association property
     * pointing from the middle entity to target entity
     */
    String deeperProp() default "";
}
