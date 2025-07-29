package org.babyfish.jimmer.sql.ast.table.spi;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface UsingWeakJoinMetadataParser {

    Class<? extends WeakJoinMetadataParser> value();
}
