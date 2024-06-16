package org.babyfish.jimmer.sql.dialect;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.UUID;

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
    public String arrayTypeSuffix() {
        return " array";
    }

    @Override
    public String sqlType(Class<?> elementType) {
        if (elementType == String.class) {
            return "varchar";
        }
        if (elementType == UUID.class) {
            return "char(36)";
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
            return "float(24)";
        }
        if (elementType == double.class) {
            return "float(53)";
        }
        if (elementType == BigDecimal.class) {
            return "decimal";
        }
        if (elementType == java.sql.Date.class || elementType == LocalDate.class) {
            return "date";
        }
        if (elementType == java.sql.Time.class || elementType == LocalTime.class) {
            return "time without time zone";
        }
        if (elementType == OffsetTime.class) {
            return "time";
        }
        if (elementType == java.util.Date.class || elementType == java.sql.Timestamp.class) {
            return "timestamp";
        }
        if (elementType == LocalDateTime.class) {
            return "timestamp";
        }
        if (elementType == OffsetDateTime.class || elementType == ZonedDateTime.class || elementType == Instant.class) {
            return "timestamp with time zone";
        }
        return null;
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
    public @Nullable String getJsonLiteralSuffix() {
        return "format json";
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

    @Override
    public boolean isUpsertSupported() {
        return true;
    }

    @Override
    public boolean isAffectCountOfInsertIgnoreWrong() {
        return true;
    }

    @Override
    public void upsert(UpsertContext ctx) {
        ctx.sql("merge into ")
                .appendTableName()
                .sql("(")
                .appendInsertedColumns()
                .sql(") key(")
                .appendConflictColumns()
                .sql(") values(")
                .appendInsertingValues()
                .sql(")");

    }
}
