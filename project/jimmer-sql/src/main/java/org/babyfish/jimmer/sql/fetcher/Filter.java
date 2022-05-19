package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.ast.table.Table;

public interface Filter<E, T extends Table<E>> {

    void apply(FilterArgs<E, T> args);
}
