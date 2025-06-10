package org.babyfish.jimmer.sql.ast.table.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.NotNull;

public interface BaseTable1<S1 extends Selection<?>> extends BaseTable {

    @NotNull
    S1 get_1();

    <TT extends TableLike<?>, WJ extends WeakJoin<BaseTable1<S1>, TT>> TT weakJoin(Class<WJ> weakJoinType);
}
