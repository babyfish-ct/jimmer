package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;

import javax.persistence.criteria.JoinType;

class SubQueryTableImpl<E> extends TableImpl<E> implements SubQueryTableImplementor<E> {

    public SubQueryTableImpl(
            AbstractMutableStatementImpl statement,
            ImmutableType immutableType,
            TableImpl<?> parent,
            boolean isInverse,
            ImmutableProp joinProp,
            JoinType joinType
    ) {
        super(statement, immutableType, parent, isInverse, joinProp, joinType);
    }

    @Override
    protected SubQueryTableImpl<?> createChildTable(
            boolean isInverse,
            ImmutableProp joinProp,
            JoinType joinType
    ) {
        return new SubQueryTableImpl<>(
                getStatement(),
                isInverse ? joinProp.getDeclaringType() : joinProp.getTargetType(),
                this,
                isInverse,
                joinProp,
                joinType
        );
    }
}
