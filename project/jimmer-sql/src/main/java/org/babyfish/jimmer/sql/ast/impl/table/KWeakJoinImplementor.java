package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public interface KWeakJoinImplementor<S, T> {

    Predicate on(TableLike<S> source, TableLike<T> target, AbstractMutableStatementImpl statement);
}
