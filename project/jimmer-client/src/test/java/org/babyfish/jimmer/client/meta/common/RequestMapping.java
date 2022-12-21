package org.babyfish.jimmer.client.meta.common;

import org.babyfish.jimmer.client.meta.Operation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequestMapping {

    String value();

    Operation.HttpMethod[] method() default {};
}
