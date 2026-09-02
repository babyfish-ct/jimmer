package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.table.Table;

/**
 * @deprecated Use {@link UpdateCondition}.
 */
@Deprecated
@FunctionalInterface
public interface UserOptimisticLock<E, T extends Table<E>> extends UpdateCondition<E, T> {
}
