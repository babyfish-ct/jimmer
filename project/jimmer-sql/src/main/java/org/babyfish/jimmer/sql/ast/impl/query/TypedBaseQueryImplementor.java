package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface TypedBaseQueryImplementor<T extends BaseTable>
        extends TypedBaseQuery<T>, TypedQueryImplementor {

    TableImplementor<?> resolveRootTable(Table<?> table);

    MergedBaseQueryImpl<T> getMergedBy();

    void setMergedBy(MergedBaseQueryImpl<T> mergedBaseQuery);

    T asBaseTable(byte[] kotlinSelectionTypes, boolean cte);
}
