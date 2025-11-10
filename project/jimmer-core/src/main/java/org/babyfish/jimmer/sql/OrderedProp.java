package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.NullOrderMode;

import java.lang.annotation.Target;

@Target({})
@kotlin.annotation.Target(allowedTargets = {})
public @interface OrderedProp {
    String value();
    boolean desc() default false;
    NullOrderMode nullsOrder() default NullOrderMode.UNSPECIFIED;
}
