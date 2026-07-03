package org.babyfish.jimmer.internal;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.CLASS)
public @interface GeneratedPolymorphicDtoBranch {

    Class<?> value();
}
