package org.babyfish.jimmer.sql.dialect;

public class H2Dialect extends DefaultDialect {

    @Override
    public String getSelectIdFromSequenceSql(String sequenceName) {
        return "select nextval('" + sequenceName + "')";
    }

    @Override
    public String transCacheOperatorTableDDL() {
        return "create table JIMMER_TRANS_CACHE_OPERATOR(" +
                "ID identity not null primary key," +
                "IMMUTABLE_TYPE varchar," +
                "IMMUTABLE_PROP varchar," +
                "CACHE_KEY varchar not null," +
                "REASON varchar" +
                ")";
    }
}
