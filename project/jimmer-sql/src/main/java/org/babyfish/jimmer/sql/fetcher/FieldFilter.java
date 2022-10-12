package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.ast.table.Table;

public interface FieldFilter<T extends Table<?>> {

    void apply(FieldFilterArgs<T> args);
}
