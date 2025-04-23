package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.BaseTableQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;

public interface BaseTableQueryImplementor<R, B extends BaseTable<R>>
        extends BaseTableQuery<R, B>, TypedRootQueryImplementor<R> {
}
