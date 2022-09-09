package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.table.Table;

public interface FluentTable<E> extends Table<E> {

    void bind(Table<E> raw);
}
