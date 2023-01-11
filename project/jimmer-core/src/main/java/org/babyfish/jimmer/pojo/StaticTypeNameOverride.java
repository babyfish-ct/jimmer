package org.babyfish.jimmer.pojo;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(StaticTypeNameOverrides.class)
public @interface StaticTypeNameOverride {

    String alias();

    String topLevelName();
}
