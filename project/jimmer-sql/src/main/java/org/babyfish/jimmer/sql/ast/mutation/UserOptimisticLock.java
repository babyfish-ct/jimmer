package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface UserOptimisticLock<E, T extends Table<E>> {

    Predicate predicate(T table, ValueExpressionFactory<E> valueExpressionFactory);
}
