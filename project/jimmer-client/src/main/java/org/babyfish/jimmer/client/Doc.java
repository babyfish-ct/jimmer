package org.babyfish.jimmer.client;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {
        AnnotationTarget.CLASS,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.FIELD,
        AnnotationTarget.VALUE_PARAMETER}
)
@Repeatable(Docs.class)
public @interface Doc {
    String value();
}
