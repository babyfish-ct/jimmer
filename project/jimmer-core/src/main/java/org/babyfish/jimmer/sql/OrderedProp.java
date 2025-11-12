package org.babyfish.jimmer.sql;

import java.lang.annotation.Target;

@Target({})
@kotlin.annotation.Target(allowedTargets = {})
public @interface OrderedProp {
    String value();
    boolean desc() default false;
}
