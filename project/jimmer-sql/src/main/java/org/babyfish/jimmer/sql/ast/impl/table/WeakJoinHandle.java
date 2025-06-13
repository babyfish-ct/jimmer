package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
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

    interface EntityTableHandle extends WeakJoinHandle {

        ImmutableType getSourceType();

        ImmutableType getTargetType();
    }

    interface BaseTableHandle extends WeakJoinHandle {
    }
}
