package org.babyfish.jimmer.client;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Repeatable(Docs.class)
public @interface Doc {
    String value();
}
