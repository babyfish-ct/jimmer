package org.babyfish.jimmer.pojo;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
@Repeatable(Statics.class)
public @interface Static {

    String ownerAlias() default "";

    boolean enabled() default true;

    boolean optional() default false;

    String name() default "";

    boolean asTargetId() default false;

    String targetAlias() default "";
}
