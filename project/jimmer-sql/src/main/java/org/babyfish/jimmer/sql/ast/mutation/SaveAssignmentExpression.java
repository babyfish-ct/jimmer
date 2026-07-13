package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.table.Table;

@FunctionalInterface
public interface SaveAssignmentExpression<E, T extends Table<E>, V> {

    /**
     * @param target The existing database row; joins are not supported
     * @param values Factory for scalar values of the saved object
     * @return The value assigned to the already selected update target
     */
    Expression<V> value(T target, ValueExpressionFactory<E> values);
}
