package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.DatabaseMetadata;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PropExpressionImplementor<T> extends PropExpression<T> {

    Table<?> getTable();

    ImmutableProp getProp();

    @Nullable
    EmbeddedColumns.Partial getPartial(MetadataStrategy strategy);

    void renderTo(@NotNull SqlBuilder builder, boolean ignoreEmbeddedTuple);
}
