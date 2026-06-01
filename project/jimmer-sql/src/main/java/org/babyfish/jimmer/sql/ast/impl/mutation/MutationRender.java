package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

final class MutationRender {

    private MutationRender() {}

    static String alias(SqlBuilder builder, TableLikeImplementor<?> table) {
        return alias(builder, table.realTable(builder.getAstContext()));
    }

    static String alias(SqlBuilder builder, RealTable table) {
        return builder.alias(table);
    }
}
