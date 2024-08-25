package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public class DefaultDialect implements Dialect {

    public static final DefaultDialect INSTANCE = new DefaultDialect();

    protected DefaultDialect() {
    }

    @Override
    public void paginate(PaginationContext ctx) {
        ctx.origin().space().sql("limit ").variable(ctx.getLimit());
        if (ctx.getOffset() > 0) {
            ctx.sql(" offset ").variable(ctx.getOffset());
        }
    }

    @Override
    public String sqlType(Class<?> elementType) {
        if (elementType == String.class) {
            return "varchar";
        }
        if (elementType == UUID.class) {
            return "varchar";
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
            return "double";
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
            return "time";
        }
        if (elementType == java.util.Date.class || elementType == java.sql.Timestamp.class) {
            return "timestamp";
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
    public void update(UpdateContext ctx) {
        ctx
                .sql("update ")
                .appendTableName()
                .enter(AbstractSqlBuilder.ScopeType.SET)
                .appendAssignments()
                .leave()
                .enter(AbstractSqlBuilder.ScopeType.WHERE)
                .appendPredicates()
                .leave();
    }

    @Override
    public void upsert(UpsertContext ctx) {
        throw new UnsupportedOperationException(
                "The upsert is not supported by \"" +
                        this.getClass().getName() +
                        "\""
        );
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
