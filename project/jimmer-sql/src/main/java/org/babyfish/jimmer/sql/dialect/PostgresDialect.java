package org.babyfish.jimmer.sql.dialect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.runtime.Reader;
import org.postgresql.util.PGobject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

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
    public String getOverrideIdentityIdSql() {
        return "overriding system value";
    }

    @Override
    public Class<?> getJsonBaseType() {
        return PGobject.class;
    }

    @Override
    public Object jsonToBaseValue(Object json, ObjectMapper objectMapper) throws Exception {
        PGobject pgobject = new PGobject();
        pgobject.setType("jsonb");
        pgobject.setValue(objectMapper.writeValueAsString(json));
        return pgobject;
    }

    @Override
    public Object baseValueToJson(Object baseValue, JavaType javaType, ObjectMapper objectMapper) throws Exception {
        PGobject pgobject = (PGobject) baseValue;
        String json = pgobject.getValue();
        if (json == null || json.isEmpty()) {
            return null;
        }
        return objectMapper.readValue(json, javaType);
    }

    @Override
    public boolean isIgnoreCaseLikeSupported() {
        return true;
    }

    @Override
    public boolean isArraySupported() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] getArray(ResultSet rs, int col, Class<T[]> arrayType) throws SQLException {
        return (T[])rs.getArray(col).getArray();
    }

    @Override
    public int resolveUnknownJdbcType(Class<?> sqlType) {
        if (sqlType.getName().equals("org.postgresql.util.PGobject")) {
            return Types.NULL;
        }
        return Types.OTHER;
    }

    @Override
    public Reader<?> unknownReader(Class<?> sqlType) {
        if (sqlType == PGobject.class) {
            return (rs, col) -> rs.getObject(col.col(), PGobject.class);
        }
        return null;
    }

    @Override
    public String transCacheOperatorTableDDL() {
        return "create table JIMMER_TRANS_CACHE_OPERATOR(\n" +
                "\tID bigint generated always as identity,\n" +
                "\tIMMUTABLE_TYPE text,\n" +
                "\tIMMUTABLE_PROP text,\n" +
                "\tCACHE_KEY text not null,\n" +
                "\tREASON text\n" +
                ")";
    }
}
