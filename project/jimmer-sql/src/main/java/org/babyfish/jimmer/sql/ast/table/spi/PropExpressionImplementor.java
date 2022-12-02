package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public interface PropExpressionImplementor<T> extends PropExpression<T> {

    void renderTo(@NotNull SqlBuilder builder, boolean ignoreEmbeddedTuple);
}
