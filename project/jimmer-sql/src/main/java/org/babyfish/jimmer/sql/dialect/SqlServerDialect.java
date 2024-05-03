package org.babyfish.jimmer.sql.dialect;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public class SqlServerDialect implements Dialect {

    @Override
    public boolean isTupleSupported() {
        return false;
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
            return "float";
        }
        if (elementType == double.class) {
            return "real";
        }
        if (elementType == BigDecimal.class) {
            return "decimal";
        }
        if (elementType == java.sql.Date.class || elementType == LocalDate.class) {
            return "date";
        }
        if (elementType == java.sql.Time.class || elementType == LocalTime.class) {
            return "time";
        }
        if (elementType == OffsetTime.class) {
            return "datetime";
        }
        if (elementType == java.util.Date.class || elementType == java.sql.Timestamp.class) {
            return "datetime";
        }
        if (elementType == LocalDateTime.class) {
            return "datetime";
        }
        if (elementType == OffsetDateTime.class || elementType == ZonedDateTime.class) {
            return "datetime";
        }
        return null;
    }

    @Override
    public void paginate(PaginationContext ctx) {
        ctx
                .origin()
                .sql(" offset ")
                .variable(ctx.getOffset())
                .sql(" rows fetch next ")
                .variable(ctx.getLimit())
                .sql(" rows only");
    }
}
