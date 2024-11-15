package org.babyfish.jimmer.sql.dialect;

public class SQLiteDialect extends DefaultDialect {
    @Override
    public boolean isDeleteAliasSupported() {
        return false;
    }

    @Override
    public boolean isUpdateAliasSupported() {
        return false;
    }

    @Override
    public UpdateJoin getUpdateJoin() {
        return new UpdateJoin(false, UpdateJoin.From.AS_JOIN);
    }

    @Override
    public boolean isUpsertSupported() {
        return true;
    }
}
