package org.babyfish.jimmer.jackson;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
public @interface JsonConverter {

    Class<? extends Converter<?>> value();
}
