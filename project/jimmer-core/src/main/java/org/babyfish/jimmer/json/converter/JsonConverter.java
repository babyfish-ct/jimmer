package org.babyfish.jimmer.json.converter;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Converts an immutable model property between its Java and JSON representations. */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS})
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface JsonConverter {

    Class<? extends Converter<?, ?>> value();
}
