package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.jetbrains.annotations.NotNull;

public interface FilterArgs<T extends Table<?>> extends Sortable {

    @NotNull
    T getTable();
}
