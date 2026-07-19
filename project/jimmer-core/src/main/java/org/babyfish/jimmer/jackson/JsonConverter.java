package org.babyfish.jimmer.jackson;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Note, this annotation is used to
 * decorate properties of jimmer entity,
 * nto property of DTO
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS})
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface JsonConverter {

    Class<? extends Converter<?, ?>> value();
}
