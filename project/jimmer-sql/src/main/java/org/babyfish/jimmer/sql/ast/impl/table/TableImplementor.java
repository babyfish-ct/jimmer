package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import javax.persistence.criteria.JoinType;
import java.util.Collection;

public interface TableImplementor<E> extends Table<E>, Ast {

    ImmutableType getImmutableType();

    TableImplementor<?> getParent();

    Collection<TableImplementor<?>> getChildren();

    ImmutableProp getJoinProp();

    JoinType getJoinType();

    String getAlias();

    void renderSelection(ImmutableProp prop, SqlBuilder builder);

    TableRowCountDestructive getDestructive();

    void renderJoinAsFrom(SqlBuilder builder, RenderMode mode);

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

    enum RenderMode {
        NORMAL,
        FROM_ONLY,
        WHERE_ONLY,
        DEEPER_JOIN_ONLY;
    }
}
