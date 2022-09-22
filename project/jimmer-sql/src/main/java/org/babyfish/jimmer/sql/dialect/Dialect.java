package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.runtime.ExecutionException;

public interface Dialect {

    void paginate(PaginationContext ctx);

    default UpdateJoin getUpdateJoin() {
        return null;
    }

    default String getSelectIdFromSequenceSql(String sequenceName) {
        throw new ExecutionException("Sequence is not supported by '" + getClass().getName() + "'");
    }

    default String getOverrideIdentityIdSql() {
        return null;
    }

    default boolean needDeletedAlias() { return false; }
}
