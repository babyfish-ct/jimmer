package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SQLiteDialect extends DefaultDialect {
    @Override
    public boolean isDeleteAliasSupported() {
        return false;
    }

    @Override
    public boolean isUpdateAliasSupported() {
        return false;
    }

    @Override
    public UpdateJoin getUpdateJoin() {
        return new UpdateJoin(false, UpdateJoin.From.AS_JOIN);
    }

    @Override
    public boolean isUpsertSupported() {
        return true;
    }

    @Override
    public boolean isNoIdUpsertSupported() {
        return false;
    }

    @Override
    public boolean isBatchDumb() {
        return true;
    }

    @Override
    public void upsert(UpsertContext ctx) {
        ctx.sql("insert into ")
                .appendTableName()
                .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                .appendInsertedColumns("")
                .leave()
                .sql(" values")
                .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                .appendInsertingValues()
                .leave()
                .sql(" on conflict")
                .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                .appendConflictColumns()
                .leave();
        if (ctx.isUpdateIgnored()) {
            ctx.sql(" do nothing");
        } else if (ctx.hasUpdatedColumns()) {
            ctx.sql(" do update")
                    .enter(AbstractSqlBuilder.ScopeType.SET)
                    .appendUpdatingAssignments("excluded.", "")
                    .leave();
            if (ctx.hasOptimisticLock()) {
                ctx.sql(" where ").appendOptimisticLockCondition("excluded.");
            }
        } else if (ctx.hasGeneratedId()) {
            ctx.sql(" do update set ");
            List<ValueGetter> conflictGetters = ctx.getConflictGetters();
            ValueGetter cheapestGetter = conflictGetters.get(0);
            for (ValueGetter getter : conflictGetters) {
                Class<?> type = getter.metadata().getValueProp().getReturnClass();
                type = Classes.boxTypeOf(type);
                if (type == Boolean.class || Number.class.isAssignableFrom(type)) {
                    cheapestGetter = getter;
                    break;
                }
            }
            ctx.sql(cheapestGetter).sql(" = excluded.").sql(cheapestGetter);
        } else {
            ctx.sql(" do nothing");
        }
    }

    @Override
    public void renderPosition(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast subStrAst,
            Ast expressionAst,
            @Nullable Ast startAst
    ) {
        if (startAst == null) {
            builder.sql("instr(")
                    .ast(expressionAst, currentPrecedence)
                    .sql(", ")
                    .ast(subStrAst, currentPrecedence)
                    .sql(")");
        } else {
            // case
            //    when startAst > length(expressionAst) then 0
            //    else instr(substr(expressionAst, startAst), subStrAst) + startAst - 1
            // end
            builder.sql("case when ")
                    .ast(startAst, currentPrecedence)
                    .sql(" > length(")
                    .ast(expressionAst, currentPrecedence)
                    .sql(") then 0 else instr(substr(")
                    .ast(expressionAst, currentPrecedence)
                    .sql(", ")
                    .ast(startAst, currentPrecedence)
                    .sql(", ")
                    .ast(subStrAst, currentPrecedence)
                    .sql(") + ")
                    .ast(startAst, currentPrecedence)
                    .sql(" - 1 end");
        }
    }

    @Override
    public void renderLeft(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expressionAst,
            Ast lengthAst
    ) {
        // case
        //     when lengthAst <= 0 then ''
        //     when lengthAst >= length(expressionAst) then expressionAst
        //     else substring(1, lengthAst)
        // end
        builder.sql("case when ")
                .ast(lengthAst, currentPrecedence)
                .sql(" <= 0 then '' ")
                .sql("when ")
                .ast(lengthAst, currentPrecedence)
                .sql(" >= length(")
                .ast(expressionAst, currentPrecedence)
                .sql(") then ")
                .ast(expressionAst, currentPrecedence)
                .sql(" else substring(1, ")
                .ast(lengthAst, currentPrecedence)
                .sql(") end");
    }

    @Override
    public void renderRight(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expressionAst,
            Ast lengthAst
    ) {
        // case
        //     when lengthAst <= 0 then ''
        //     when lengthAst >= length(expressionAst) then expressionAst
        //     else substring(1, -lengthAst)
        // end
        builder.sql("case when ")
                .ast(lengthAst, currentPrecedence)
                .sql(" <= 0 then '' ")
                .sql("when ")
                .ast(lengthAst, currentPrecedence)
                .sql(" >= length(")
                .ast(expressionAst, currentPrecedence)
                .sql(") then ")
                .ast(expressionAst, currentPrecedence)
                .sql(" else substring(1, -")
                .ast(lengthAst, currentPrecedence)
                .sql(") end");
    }

    @Override
    public void renderSubString(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast expressionAst, Ast startAst, @Nullable Ast lengthAst) {
        builder.sql("substr(")
                .ast(expressionAst, currentPrecedence)
                .sql(", ")
                .ast(startAst, currentPrecedence);
        if (lengthAst != null) {
            builder.sql(", ")
                    .ast(lengthAst, currentPrecedence);
        }
        builder.sql(")");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void renderTimePlus(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expressionAst,
            Ast valueAst,
            SqlTimeUnit timeUnit
    ) {
        long value;
        if (valueAst instanceof LiteralExpressionImplementor<?>) {
            value = ((LiteralExpressionImplementor<Long>) valueAst).getValue();
        } else if (valueAst instanceof ConstantExpressionImplementor<?>) {
            value = ((ConstantExpressionImplementor<Long>) valueAst).getValue();
        } else {
            throw new IllegalStateException("The time plus/minus only accept constant changed value");
        }
        switch (timeUnit) {
            case WEEKS:
                value *= 7;
                timeUnit = SqlTimeUnit.DAYS;
                break;
            case QUARTERS:
                value *= 3;
                timeUnit = SqlTimeUnit.MONTHS;
                break;
            case DECADES:
                value *= 10;
                timeUnit = SqlTimeUnit.YEARS;
                break;
            case CENTURIES:
                value *= 100;
                timeUnit = SqlTimeUnit.YEARS;
                break;
        }
        String delta = "'";
        delta += value < 0 ? Long.toString(value) : "+" + value;
        switch (timeUnit) {
            case NANOSECONDS:
            case MICROSECONDS:
            case MILLISECONDS:
                throw new IllegalStateException(
                        "Time plus/minus by unit \"" +
                                timeUnit +
                                "\" is not supported by \"" +
                                this.getClass().getName() +
                                "\""
                );
            default:
                delta += " " + timeUnit.name().toLowerCase();
                break;
        }
        delta += "'";
        Class<?> type = ((ExpressionImplementor<?>) expressionAst).getType();
        if (OffsetDateTime.class.isAssignableFrom(type) || ZonedDateTime.class.isAssignableFrom(type)) {
            builder
                    .sql("case when substr(")
                    .ast(expressionAst, 0)
                    .sql(", -6, 1) = '+' or substr(")
                    .ast(expressionAst, 0)
                    .sql(", -6, 1) = '-' then datetime(substr(")
                    .ast(expressionAst, 0)
                    .sql(", 1, length(")
                    .ast(expressionAst, 0)
                    .sql(") - 6), ")
                    .sql(delta)
                    .sql(") || substr(")
                    .ast(expressionAst, 0)
                    .sql(", -6, 6) else datetime(")
                    .ast(expressionAst, 0)
                    .sql(", ")
                    .sql(delta)
                    .sql(") end");
        } else {
            builder.sql("datetime(")
                    .ast(expressionAst, 0)
                    .sql(", ")
                    .sql(delta)
                    .sql(")");
        }
    }

    @Override
    public void renderTimeDiff(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast expressionAst, Ast otherAst, SqlTimeUnit timeUnit) {
        builder
                .sql("(julianday(")
                .ast(expressionAst, 0)
                .sql(") - julianday(")
                .ast(otherAst, ExpressionPrecedences.PLUS)
                .sql("))");
        switch (timeUnit) {
            case NANOSECONDS:
                builder.sql(" * 86400000000000");
                break;
            case MICROSECONDS:
                builder.sql(" * 86400000000");
                break;
            case MILLISECONDS:
                builder.sql(" * 86400000");
                break;
            case SECONDS:
                builder.sql(" * 86400");
                break;
            case MINUTES:
                builder.sql(" * 1440");
                break;
            case HOURS:
                builder.sql(" * 24");
                break;
            case DAYS:
                break;
            case WEEKS:
                builder.sql(" / 7");
                break;
            case MONTHS:
                builder.sql(" / 30.44");
                break;
            case QUARTERS:
                builder.sql(" / 91.31");
                break;
            case YEARS:
                builder.sql(" / 365.24");
                break;
            case DECADES:
                builder.sql(" / 3652.4");
                break;
            case CENTURIES:
                builder.sql(" / 36524");
                break;
        }
    }

    @Override
    public Timestamp getTimestamp(ResultSet rs, int col) throws SQLException {
        String text = rs.getString(col);
        if (text == null) {
            return null;
        }
        if (text.length() > 6) {
            char c = text.charAt(text.length() - 6);
            if (c == '+' || c == '-') {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(text, formatter);
                return Timestamp.from(offsetDateTime.toInstant());
            }
        }
        return rs.getTimestamp(col);
    }
}
