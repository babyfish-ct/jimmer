package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.jetbrains.annotations.NotNull;

public interface FilterArgs<P extends Props> extends Sortable {

    @NotNull
    P getTable();
}
