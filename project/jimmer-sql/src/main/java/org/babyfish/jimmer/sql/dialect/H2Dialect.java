package org.babyfish.jimmer.sql.dialect;

public class H2Dialect extends DefaultDialect {

    @Override
    public String getSelectIdFromSequenceSql(String sequenceName) {
        return "select nextval('" + sequenceName + "')";
    }

    @Override
    public String getLastIdentitySql() {
        return "call scopeidentity()";
    }
}
