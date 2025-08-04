package org.babyfish.jimmer.sql.ast.table.base;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.NotNull;

public interface BaseTable1<S1 extends Selection<?>> extends BaseTable {

    @NotNull
    S1 get_1();

    default <TT extends BaseTable> TT weakJoin(
            TT targetBaseTable,
            WeakJoin<BaseTable1<S1>, TT> weakJoinLambda
    ) {
        return weakJoin(targetBaseTable, JoinType.INNER, weakJoinLambda);
    }

    <TT extends BaseTable> TT weakJoin(
            TT targetBaseTable,
            JoinType joinType,
            WeakJoin<BaseTable1<S1>, TT> weakJoinLambda
    );

    default <TT extends BaseTable, WJ extends WeakJoin<BaseTable1<S1>, TT>> TT weakJoin(
            TT targetBaseTable,
            Class<WJ> weakJoinType
    ) {
        return weakJoin(targetBaseTable, weakJoinType, JoinType.INNER);
    }

    <TT extends BaseTable, WJ extends WeakJoin<BaseTable1<S1>, TT>> TT weakJoin(
            TT targetBaseTable,
            Class<WJ> weakJoinType,
            JoinType joinType
    );
}
