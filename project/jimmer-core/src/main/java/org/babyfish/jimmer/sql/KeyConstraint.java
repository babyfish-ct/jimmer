package org.babyfish.jimmer.sql;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KeyConstraint {

    Type type();

    enum Type {
        NONE,
        KEY_PROPS,
        KEY_PROPS_AND_LOGICAL_DELETED
    }
}
