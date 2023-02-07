package org.babyfish.jimmer.error;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(ErrorFields.class)
public @interface ErrorField {

    String name();

    Class<?> type();

    boolean list() default false;

    boolean nullable() default false;
}
