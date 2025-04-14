package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public class SqlServerDialect extends DefaultDialect {

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

    @Override
    public void renderLPad(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expression,
            Ast length,
            Ast padString
    ) {
        //right(replicate(padding, length) + expression, length)
        builder
                .sql("right(replicate(")
                .ast(padString, currentPrecedence)
                .sql(", ")
                .ast(length, currentPrecedence)
                .sql(") + ")
                .ast(expression, currentPrecedence)
                .sql(", ")
                .ast(length, currentPrecedence)
                .sql(")");
    }

    @Override
    public void renderRPad(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expression,
            Ast length,
            Ast padString
    ) {
        //left(expression + replicate(padding, length), length)
        builder.sql("left(")
                .ast(expression, currentPrecedence)
                .sql(" + replicate(")
                .ast(padString, currentPrecedence)
                .sql(", ")
                .ast(length, currentPrecedence)
                .sql("), ")
                .ast(length, currentPrecedence)
                .sql(")");
    }

    @Override
    public void renderPosition(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast subStrAst,
            Ast expressionAst,
            @Nullable Ast startAst
    ) {
        builder.sql("charindex(")
                .ast(subStrAst, currentPrecedence)
                .sql(", ")
                .ast(expressionAst, currentPrecedence);
        if (startAst != null) {
            builder.sql(", ")
                    .ast(startAst, currentPrecedence);
        }
        builder.sql(")");
    }
}
