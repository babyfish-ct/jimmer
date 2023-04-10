package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

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
