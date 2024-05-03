package org.babyfish.jimmer.sql.dialect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.runtime.Reader;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.UUID;

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

    @Override
    public String arrayTypeSuffix() {
        return "[]";
    }

    @Override
    public String sqlType(Class<?> elementType) {
        if (elementType == String.class) {
            return "text";
        }
        if (elementType == UUID.class) {
            return "uuid";
        }
        if (elementType == boolean.class) {
            return "boolean";
        }
        if (elementType == byte.class) {
            return "tinyint";
        }
        if (elementType == short.class) {
            return "smallint";
        }
        if (elementType == int.class) {
            return "int";
        }
        if (elementType == long.class) {
            return "bigint";
        }
        if (elementType == float.class) {
            return "numeric";
        }
        if (elementType == double.class) {
            return "numeric";
        }
        if (elementType == BigDecimal.class) {
            return "numeric";
        }
        if (elementType == java.sql.Date.class || elementType == LocalDate.class) {
            return "date";
        }
        if (elementType == java.sql.Time.class || elementType == LocalTime.class) {
            return "time";
        }
        if (elementType == OffsetTime.class) {
            return "time with time zone";
        }
        if (elementType == java.util.Date.class || elementType == java.sql.Timestamp.class) {
            return "timestamp";
        }
        if (elementType == LocalDateTime.class) {
            return "timestamp";
        }
        if (elementType == OffsetDateTime.class || elementType == ZonedDateTime.class) {
            return "timestamp with time zone";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] getArray(ResultSet rs, int col, Class<T[]> arrayType) throws SQLException {
        Array array = rs.getArray(col);
        if (array != null) {
            return (T[]) array.getArray();
        }
        return null;
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
