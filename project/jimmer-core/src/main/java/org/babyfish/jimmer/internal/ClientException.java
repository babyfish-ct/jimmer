package org.babyfish.jimmer.internal;

import org.babyfish.jimmer.error.CodeBasedException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClientException {

    String family() default "";

    String code() default "";

    Class<?>[] subTypes() default {};
}
