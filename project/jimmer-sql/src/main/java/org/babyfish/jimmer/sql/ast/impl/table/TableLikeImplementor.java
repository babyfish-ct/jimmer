package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public interface TableLikeImplementor<E> extends TableLike<E> {

    default TableLikeImplementor<?> getParent() {
        return null;
    }

    default WeakJoinHandle getWeakJoinHandle() {
        return null;
    }

    default JoinType getJoinType() {
        return JoinType.INNER;
    }
}
