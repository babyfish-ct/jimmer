package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Columns;
import org.jetbrains.annotations.NotNull;

public interface FilterArgs<C extends Columns> extends Sortable {

    @NotNull
    C getTable();
}
