package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.table.BaseTable;

import java.util.List;

public interface BaseTableImplementor extends BaseTable, TableLikeImplementor<Object> {

    @Override
    TableLikeImplementor<?> getParent();

    List<Selection<?>> getSelections();

    TypedBaseQueryImplementor<?> getQuery();

    BaseTableSymbol toSymbol();

    BaseTableImplementor getRecursive();

    boolean isCte();
}
