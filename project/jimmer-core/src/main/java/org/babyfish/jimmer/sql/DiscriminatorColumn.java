package org.babyfish.jimmer.sql;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DiscriminatorColumn {

    String name() default "DTYPE";

    String sqlType() default "";

    int length() default 31;

    boolean nullable() default false;
}
