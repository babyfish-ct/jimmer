package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Table;

public interface Filter<T extends Table<?>> {

    void filter(FilterArgs<T> args);
}
