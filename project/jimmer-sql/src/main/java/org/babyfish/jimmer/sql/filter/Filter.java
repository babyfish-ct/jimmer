package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Columns;

public interface Filter<C extends Columns> {

    void filter(FilterArgs<C> args);
}
