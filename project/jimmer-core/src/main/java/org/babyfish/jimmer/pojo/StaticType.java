package org.babyfish.jimmer.pojo;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(StaticTypes.class)
public @interface StaticType {

    String alias();

    String topLevelName() default "";

    AutoScalarStrategy autoScalarStrategy() default AutoScalarStrategy.ALL;

    boolean allOptional() default false;
}
