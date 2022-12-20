package org.babyfish.jimmer.client;

import kotlin.Unit;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface FetchBy {

    String value();

    // This is `default Unit.class`, not `default void.class`,
    // otherwise, kotlin reflection will crash
    Class<?> ownerType() default void.class;
}
