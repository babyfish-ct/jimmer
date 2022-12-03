package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public interface PropExpressionImplementor<T> extends PropExpression<T> {

    Table<?> getTable();

    ImmutableProp getProp();

    EmbeddedColumns.Partial getPartial();

    void renderTo(@NotNull SqlBuilder builder, boolean ignoreEmbeddedTuple);
}
