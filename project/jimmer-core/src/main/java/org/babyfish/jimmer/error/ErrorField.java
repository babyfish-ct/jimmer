package org.babyfish.jimmer.error;

import java.lang.annotation.Target;

@Target({})
public @interface ErrorField {

    String name();

    Class<?> type();

    boolean nullable() default false;

    boolean isList() default false;
}
