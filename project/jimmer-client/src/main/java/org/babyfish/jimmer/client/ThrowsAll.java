package org.babyfish.jimmer.client;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ThrowsAll {

    Class<? extends Enum<?>> value();
}
