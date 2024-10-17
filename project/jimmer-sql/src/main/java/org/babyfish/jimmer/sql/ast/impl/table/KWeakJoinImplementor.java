package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface KWeakJoinImplementor<S, T> {

    Predicate on(Table<S> source, Table<T> target, AbstractMutableStatementImpl statement);
}
