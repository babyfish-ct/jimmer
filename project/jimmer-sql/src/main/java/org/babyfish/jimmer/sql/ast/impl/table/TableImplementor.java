package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import javax.persistence.criteria.JoinType;

public interface TableImplementor<E> extends Table<E>, Ast {

    ImmutableType getImmutableType();

    TableImplementor<?> getParent();

    void renderSelection(ImmutableProp prop, SqlBuilder builder);

    TableRowCountDestructive getDestructive();

    @SuppressWarnings("unchecked")
    static TableImplementor<?> unwrap(Table<?> table) {
        if (table instanceof TableImplementor<?>) {
            return (TableImplementor<?>) table;
        }
        if (table instanceof AbstractTableWrapper<?>) {
            return unwrap(((AbstractTableWrapper<?>) table).__unwrap());
        }
        throw new IllegalArgumentException("Unknown table implementation");
    }

    static TableImplementor<?> create(
            AbstractMutableStatementImpl statement,
            ImmutableType immutableType
    ) {
        return new TableImpl<>(
                statement,
                immutableType,
                null,
                false,
                null,
                JoinType.INNER
        );
    }
}
