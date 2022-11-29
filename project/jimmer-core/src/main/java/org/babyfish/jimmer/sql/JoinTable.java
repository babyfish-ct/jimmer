package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface JoinTable {
    String name() default "";
    String joinColumnName() default "";
    String inverseJoinColumnName() default "";
    JoinColumn[] joinColumns() default {};
    JoinColumn[] inverseColumns() default {};
}
