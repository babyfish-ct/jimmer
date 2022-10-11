package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.ast.table.Table;

public interface Filter<T extends Table<?>> {

    void apply(FilterArgs<T> args);
}
