package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface UpdateCondition<E, T extends Table<E>> {

    @Nullable
    Predicate predicate(T table, ValueExpressionFactory<E> values);
}
