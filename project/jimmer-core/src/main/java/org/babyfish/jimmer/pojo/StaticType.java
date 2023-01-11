package org.babyfish.jimmer.pojo;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(StaticTypes.class)
public @interface StaticType {

    String alias() default "";

    String topLevelName() default "";

    boolean allScalars() default true;

    StaticTypeStyle style() default StaticTypeStyle.AUTO;
}
