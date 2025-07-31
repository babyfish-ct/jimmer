package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public interface WeakJoinHandle {

    Predicate createPredicate(
            TableLike<?> source,
            TableLike<?> target,
            AbstractMutableStatementImpl statement
    );

    Class<? extends WeakJoin<?, ?>> getWeakJoinType();

    static WeakJoinHandle of(Class<? extends WeakJoin<?, ?>> weakJoinType) {
        return WeakJoinHandleImpl.get(weakJoinType);
    }

    static WeakJoinHandle of(
            WeakJoinLambda lambda,
            boolean hasSourceWrapper,
            boolean hasTargetWrapper,
            WeakJoin<TableLike<?>, TableLike<?>> weakJoin
    ) {
        if (BaseTable.class.isAssignableFrom(lambda.getTargetType())) {
            return new WeakJoinHandleImpl.BaseTableHandleImpl(lambda, weakJoin);
        }
        if (WeakJoinHandleImpl.K_BASE_TABLE_TYPE_SYMBOL != null &&
                WeakJoinHandleImpl.K_BASE_TABLE_TYPE_SYMBOL.isAssignableFrom(lambda.getTargetType())) {
            return new WeakJoinHandleImpl.BaseTableHandleImpl(lambda, weakJoin);
        }
        return new WeakJoinHandleImpl.EntityTableHandleImpl(
                lambda,
                hasSourceWrapper,
                hasTargetWrapper,
                weakJoin
        );
    }

    interface EntityTableHandle extends WeakJoinHandle {

        ImmutableType getSourceType();

        ImmutableType getTargetType();
    }

    interface BaseTableHandle extends WeakJoinHandle {
    }
}
