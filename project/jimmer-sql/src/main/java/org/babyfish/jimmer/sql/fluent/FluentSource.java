package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.table.Table;

public interface FluentSource<E> {

    void bind(Table<E> raw);
}
