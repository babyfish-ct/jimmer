package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
@Repeatable(ColumnOverrides.class)
public @interface ColumnOverride {

    /**
     * @return Property name chain with the separator "."
     */
    String prop();

    String columnName();
}
