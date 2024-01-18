package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PropExpressionImplementor<T> extends PropExpression<T>, ExpressionImplementor<T> {

    Table<?> getTable();

    ImmutableProp getProp();

    ImmutableProp getDeepestProp();

    PropExpressionImpl.EmbeddedImpl<?> getBase();

    boolean isRawId();

    @Nullable
    EmbeddedColumns.Partial getPartial(MetadataStrategy strategy);

    void renderTo(@NotNull SqlBuilder builder, boolean ignoreBrackets);
}
