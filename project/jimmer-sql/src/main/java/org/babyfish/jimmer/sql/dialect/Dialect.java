package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.jetbrains.annotations.Nullable;

public interface Dialect {

    void paginate(PaginationContext ctx);

    @Nullable
    default UpdateJoin getUpdateJoin() {
        return null;
    }

    default String getSelectIdFromSequenceSql(String sequenceName) {
        throw new ExecutionException("Sequence is not supported by '" + getClass().getName() + "'");
    }

    @Nullable
    default String getOverrideIdentityIdSql() {
        return null;
    }

    default boolean needDeletedAlias() { return false; }

    default boolean isMultiInsertionSupported() { return true; }

    @Nullable
    default String getConstantTableName() { return null; }
}
