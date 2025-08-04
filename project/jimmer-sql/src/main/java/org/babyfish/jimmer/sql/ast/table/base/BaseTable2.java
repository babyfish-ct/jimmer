package org.babyfish.jimmer.sql.ast.table.base;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.jetbrains.annotations.NotNull;

public interface BaseTable2<S1 extends Selection<?>, S2 extends Selection<?>> extends BaseTable {

    @NotNull
    S1 get_1();

    @NotNull
    S2 get_2();

    default <TT extends BaseTable> TT weakJoin(
            TT targetBaseTable,
            WeakJoin<BaseTable2<S1, S2>, TT> weakJoinLambda
    ) {
        return weakJoin(targetBaseTable, JoinType.INNER, weakJoinLambda);
    }

    <TT extends BaseTable> TT weakJoin(
            TT targetBaseTable,
            JoinType joinType,
            WeakJoin<BaseTable2<S1, S2>, TT> weakJoinLambda
    );

    default <TT extends BaseTable, WJ extends WeakJoin<BaseTable2<S1, S2>, TT>> TT weakJoin(
            TT targetBaseTable,
            Class<WJ> weakJoinType
    ) {
        return weakJoin(targetBaseTable, weakJoinType, JoinType.INNER);
    }

    <TT extends BaseTable, WJ extends WeakJoin<BaseTable2<S1, S2>, TT>> TT weakJoin(
            TT targetBaseTable,
            Class<WJ> weakJoinType,
            JoinType joinType
    );
}
