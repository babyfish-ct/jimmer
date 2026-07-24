package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableProxies;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbols;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public abstract class AbstractBaseTable<B extends BaseTable> implements BaseTable, BaseTableProxy {

    private final BaseTable baseTable;

    protected AbstractBaseTable(BaseTable baseTable) {
        this.baseTable = BaseTableProxies.unwrap(baseTable);
    }

    @Override
    public final BaseTable __unwrap() {
        return baseTable;
    }

    @SuppressWarnings("unchecked")
    protected final <S extends Selection<?>> S selection(int index) {
        return (S) ((BaseTableSymbol) baseTable).getSelections().get(index);
    }

    public final <TT extends BaseTable> TT weakJoin(
            TT targetBaseTable,
            WeakJoin<B, TT> weakJoinLambda
    ) {
        return weakJoin(targetBaseTable, JoinType.INNER, weakJoinLambda);
    }

    public final <TT extends BaseTable> TT weakJoin(
            TT targetBaseTable,
            JoinType joinType,
            WeakJoin<B, TT> weakJoinLambda
    ) {
        WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
        WeakJoinHandle handle = WeakJoinHandle.of(
                lambda,
                true,
                true,
                cast(weakJoinLambda)
        );
        return joinedTarget(targetBaseTable, joinType, handle);
    }

    public final <TT extends BaseTable, WJ extends WeakJoin<B, TT>> TT weakJoin(
            TT targetBaseTable,
            Class<WJ> weakJoinType
    ) {
        return weakJoin(targetBaseTable, weakJoinType, JoinType.INNER);
    }

    public final <TT extends BaseTable, WJ extends WeakJoin<B, TT>> TT weakJoin(
            TT targetBaseTable,
            Class<WJ> weakJoinType,
            JoinType joinType
    ) {
        return joinedTarget(
                targetBaseTable,
                joinType,
                WeakJoinHandle.of(weakJoinType)
        );
    }

    @SuppressWarnings("unchecked")
    private <TT extends BaseTable> TT joinedTarget(
            TT targetBaseTable,
            JoinType joinType,
            WeakJoinHandle handle
    ) {
        BaseTable joined = BaseTableSymbols.of(
                (BaseTableSymbol) BaseTableProxies.unwrap(targetBaseTable),
                (TableLike<?>) baseTable,
                handle,
                joinType,
                null
        );
        return (TT) BaseTableProxies.wrap(joined, false);
    }

    @SuppressWarnings("unchecked")
    private static WeakJoin<TableLike<?>, TableLike<?>> cast(WeakJoin<?, ?> weakJoin) {
        return (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoin;
    }

    @Override
    public int hashCode() {
        return baseTable.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseTableProxy)) {
            return false;
        }
        return baseTable.equals(((BaseTableProxy) o).__unwrap());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + baseTable + ')';
    }
}
