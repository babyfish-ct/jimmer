package org.babyfish.jimmer.sql.dialect;

public class PostgresDialect extends DefaultDialect {

    @Override
    public UpdateJoin getUpdateJoin() {
        return new UpdateJoin(false, UpdateJoin.From.AS_JOIN);
    }

    @Override
    public String getSelectIdFromSequenceSql(String sequenceName) {
        return "select nextval('" + sequenceName + "')";
    }

    @Override
    public String getLastIdentitySql() {
        return "select lastval()";
    }

    @Override
    public String getOverrideIdentityIdSql() {
        return "overriding system value";
    }
}
