package org.babyfish.jimmer.sql.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;

public class H2Dialect extends DefaultDialect {

    @Override
    public boolean isIgnoreCaseLikeSupported() {
        return true;
    }

    @Override
    public boolean isArraySupported() {
        return true;
    }

    @Override
    public <T> T[] getArray(ResultSet rs, int col, Class<T[]> arrayType) throws SQLException {
        return rs.getObject(col, arrayType);
    }

    @Override
    public boolean isTupleCountSupported() {
        return true;
    }

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
