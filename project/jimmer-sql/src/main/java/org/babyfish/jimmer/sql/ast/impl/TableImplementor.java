package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;

public interface TableImplementor<E> extends Table<E>, Ast {

    ImmutableType getImmutableType();

    @SuppressWarnings("unchecked")
    static <TI extends TableImplementor<?>> TI unwrap(Table<?> table) {
        if (table instanceof TableImplementor<?>) {
            return (TI) table;
        }
        if (table instanceof AbstractTableWrapper<?>) {
            return unwrap(((AbstractTableWrapper<?>) table).__unwrap());
        }
        throw new IllegalArgumentException("Unknown table implementation");
    }
}
