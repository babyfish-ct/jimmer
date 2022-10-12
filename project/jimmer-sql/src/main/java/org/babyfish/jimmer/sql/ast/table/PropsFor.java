package org.babyfish.jimmer.sql.ast.table;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PropsFor {

    Class<?> value();
}
