package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PropExpressionImplementor<T> extends PropExpression<T>, ExpressionImplementor<T> {

    Table<?> getTable();

    ImmutableProp getProp();

    ImmutableProp getDeepestProp();

    @Nullable
    PropExpressionImpl.EmbeddedImpl<?> getBase();

    @Nullable
    String getPath();

    boolean isRawId();

    @Nullable
    EmbeddedColumns.Partial getPartial(MetadataStrategy strategy);

    void renderTo(@NotNull AbstractSqlBuilder<?> builder, boolean ignoreBrackets);

    PropExpression<T> unwrap();

    default boolean isNullable() {
        return getProp().isNullable();
    }
}
