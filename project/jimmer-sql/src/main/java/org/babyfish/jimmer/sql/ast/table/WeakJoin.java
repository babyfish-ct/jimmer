package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.Predicate;

public interface WeakJoin<ST extends Table<?>, TT extends Table<?>> {

    Predicate on(ST source, TT target);
}
