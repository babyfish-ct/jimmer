package org.babyfish.jimmer.sql.dialect;

import com.fasterxml.jackson.databind.JavaType;
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
    public Object jsonToBaseValue(Object json) throws Exception {
        PGobject pgobject = new PGobject();
        pgobject.setType("jsonb");
        pgobject.setValue(JsonUtils.OBJECT_MAPPER.writeValueAsString(json));
        return pgobject;
    }

    @Override
    public Object baseValueToJson(Object baseValue, JavaType javaType) throws Exception {
        PGobject pgobject = (PGobject) baseValue;
        String json = pgobject.getValue();
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JsonUtils.OBJECT_MAPPER.readValue(json, javaType);
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
            return (rs, col) -> rs.getObject(col.get(), PGobject.class);
        }
        return null;
    }
}
