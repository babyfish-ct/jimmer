package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.util.List;

public interface BaseTableSymbol extends BaseTable {

    ConfigurableBaseQueryImpl<?> getQuery();

    List<Selection<?>> getSelections();

    TableLike<?> getParent();

    WeakJoinHandle getWeakJoinHandle();

    JoinType getJoinType();

    boolean isCte();

    BaseTableSymbol getRecursive();
}
