package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Props;

public interface Filter<P extends Props> {

    void filter(FilterArgs<P> args);
}
