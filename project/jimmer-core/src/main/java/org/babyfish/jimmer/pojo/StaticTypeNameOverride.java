package org.babyfish.jimmer.pojo;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(StaticTypeNameOverrides.class)
public @interface StaticTypeNameOverride {

    String value();

    String alias() default "";
}
