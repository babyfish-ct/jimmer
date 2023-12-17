package org.babyfish.jimmer.internal;

import org.babyfish.jimmer.error.CodeBasedRuntimeException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ClientException {

    String family();

    String code() default "";

    Class<? extends CodeBasedRuntimeException>[] subTypes() default {};
}
