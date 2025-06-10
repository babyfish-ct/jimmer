package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public interface WeakJoin<ST extends TableLike<?>, TT extends TableLike<?>> {

    Predicate on(ST source, TT target);
}
