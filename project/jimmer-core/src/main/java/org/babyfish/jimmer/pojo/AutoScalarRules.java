package org.babyfish.jimmer.pojo;

import kotlin.annotation.AnnotationRetention;
import kotlin.annotation.Retention;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(AnnotationRetention.SOURCE)
@java.lang.annotation.Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AutoScalarRules {

    AutoScalarRule[] value();
}
