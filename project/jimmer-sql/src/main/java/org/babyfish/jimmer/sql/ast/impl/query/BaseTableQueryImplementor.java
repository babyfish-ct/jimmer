package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.BaseTableQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface BaseTableQueryImplementor<R, B extends BaseTable<R>>
        extends BaseTableQuery<R, B>, TypedRootQueryImplementor<R> {
    TableImplementor<?> resolveRootTable(Table<?> table);
}
