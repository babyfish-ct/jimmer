package org.babyfish.jimmer.pojo;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface StaticTypes {

    StaticType[] value();
}
