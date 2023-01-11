package org.babyfish.jimmer.pojo;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(StaticTypes.class)
public @interface StaticType {

    String alias();

    String topLevelName() default "";

    boolean allScalars() default true;

    boolean allOptional() default false;
}
