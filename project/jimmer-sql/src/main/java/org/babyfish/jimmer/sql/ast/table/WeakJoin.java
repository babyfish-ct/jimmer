package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.Predicate;

import java.io.Serializable;

public interface WeakJoin<ST extends Table<?>, TT extends Table<?>> extends Serializable {

    Predicate on(ST source, TT target);
}
