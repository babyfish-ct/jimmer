package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.query.SubMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.table.SubQueryTable;

import javax.persistence.criteria.JoinType;

public interface SubQueryTableImplementor<E> extends TableImplementor<E>, SubQueryTable<E> {

    static SubQueryTableImplementor<?> create(
            SubMutableQueryImpl statement,
            ImmutableType immutableType
    ) {
        return new SubQueryTableImpl<>(
                statement,
                immutableType,
                null,
                false,
                null,
                JoinType.INNER
        );
    }
}
