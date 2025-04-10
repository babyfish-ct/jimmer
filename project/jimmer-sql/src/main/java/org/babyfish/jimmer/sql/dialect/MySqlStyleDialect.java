package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;

public abstract class MySqlStyleDialect extends DefaultDialect {

    public void paginate(PaginationContext ctx) {
        ctx.origin().space().sql("limit ").variable(ctx.getOffset()).sql(", ").variable(ctx.getLimit());
    }

    public UpdateJoin getUpdateJoin() {
        return new UpdateJoin(true, UpdateJoin.From.UNNECESSARY);
    }

    public boolean isDeletedAliasRequired() {
        return true;
    }

    public String sqlType(Class<?> elementType) {
        if (elementType == String.class) {
            return "varchar";
        } else if (elementType == UUID.class) {
            return "char(36)";
        } else if (elementType == Boolean.TYPE) {
            return "boolean";
        } else if (elementType == Byte.TYPE) {
            return "tinyint";
        } else if (elementType == Short.TYPE) {
            return "smallint";
        } else if (elementType == Integer.TYPE) {
            return "int";
        } else if (elementType == Long.TYPE) {
            return "bigint";
        } else if (elementType == Float.TYPE) {
            return "float";
        } else if (elementType == Double.TYPE) {
            return "double";
        } else if (elementType == BigDecimal.class) {
            return "decimal";
        } else if (elementType != Date.class && elementType != LocalDate.class) {
            if (elementType != Time.class && elementType != LocalTime.class) {
                if (elementType == OffsetTime.class) {
                    return "datetime";
                } else if (elementType != java.util.Date.class && elementType != Timestamp.class) {
                    if (elementType == LocalDateTime.class) {
                        return "timestamp";
                    } else {
                        return elementType != OffsetDateTime.class && elementType != ZonedDateTime.class ? null : "timestamp";
                    }
                } else {
                    return "timestamp";
                }
            } else {
                return "datetime";
            }
        } else {
            return "date";
        }
    }

    public boolean isUpsertWithMultipleUniqueConstraintSupported() {
        return false;
    }

    public boolean isIdFetchableByKeyUpdate() {
        return true;
    }

    public boolean isUpsertSupported() {
        return false;
    }

    @Override
    public boolean isTableOfSubQueryMutable() {
        return false;
    }

    @Override
    public String transCacheOperatorTableDDL() {
        return "create table JIMMER_TRANS_CACHE_OPERATOR(\n" +
                "\tID bigint unsigned not null auto_increment primary key,\n" +
                "\tIMMUTABLE_TYPE varchar(128),\n" +
                "\tIMMUTABLE_PROP varchar(128),\n" +
                "\tCACHE_KEY varchar(64) not null,\n" +
                "\tREASON varchar(32)\n" +
                ") engine=innodb";
    }

    @Override
    public void renderPosition(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast subStrAst,
            Ast expressionAst,
            @Nullable Ast startAst
    ) {
        if (startAst != null) {
            builder.sql("locate(")
                    .ast(expressionAst, currentPrecedence)
                    .sql(", ")
                    .ast(subStrAst, currentPrecedence);
            builder.sql(", ").ast(startAst, currentPrecedence);
            builder.sql(")");
        } else {
            super.renderPosition(
                    builder,
                    currentPrecedence,
                    subStrAst,
                    expressionAst,
                    startAst
            );
        }
    }
}
