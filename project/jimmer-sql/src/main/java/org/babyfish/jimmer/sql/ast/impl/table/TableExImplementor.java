package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl;
import org.babyfish.jimmer.sql.ast.impl.query.SubMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import javax.persistence.criteria.JoinType;

public interface TableExImplementor<E> extends TableImplementor<E>, TableEx<E> {

    static TableExImplementor<?> create(
            SubMutableQueryImpl statement,
            ImmutableType immutableType
    ) {
        return new TableExImpl<>(
                statement,
                immutableType,
                null,
                false,
                null,
                JoinType.INNER
        );
    }

    static TableExImplementor<?> create(
            MutableUpdateImpl statement,
            ImmutableType immutableType
    ) {
        return new TableExImpl<>(
                statement,
                immutableType,
                null,
                false,
                null,
                JoinType.INNER
        );
    }
}
