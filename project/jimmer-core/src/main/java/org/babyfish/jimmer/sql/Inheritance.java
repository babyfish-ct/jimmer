package org.babyfish.jimmer.sql;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Inheritance {

    InheritanceType strategy() default InheritanceType.SINGLE_TABLE;
}
