package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.query.UseTableVisitor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public interface RealTable extends Iterable<RealTable> {

    TableLikeImplementor<?> getTableLikeImplementor();

    RealTable getParent();

    String getAlias();

    String getMiddleTableAlias();

    String getFinalAlias(
            ImmutableProp prop,
            boolean rawId,
            JSqlClientImplementor sqlClient
    );

    void allocateAliases();

    void use(UseTableVisitor visitor);

    void renderTo(@NotNull AbstractSqlBuilder<?> builder);

    void renderJoinAsFrom(SqlBuilder builder, TableImplementor.RenderMode mode);
}
