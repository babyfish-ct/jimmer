package org.babyfish.jimmer.pojo;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface Statics {

    Static[] value();
}
