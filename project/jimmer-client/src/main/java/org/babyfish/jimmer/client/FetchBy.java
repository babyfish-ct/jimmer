package org.babyfish.jimmer.client;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface FetchBy {

    String value();

    Class<?> ownerType() default void.class;

    /**
     * Only for java, let the decorated generic argument be nullable
     */
    boolean nullable() default false;
}
