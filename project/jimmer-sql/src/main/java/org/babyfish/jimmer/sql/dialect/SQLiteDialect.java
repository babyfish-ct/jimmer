package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public void renderTimePlus(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expressionAst,
            Ast valueAst,
            SqlTimeUnit timeUnit
    ) {
        builder.sql("data(")
                .ast(expressionAst, 0)
                .sql(", case when ")
                .ast(valueAst, 0)
                .sql(" < 0 then '-' else '+' end || ")
                .ast(valueAst, ExpressionPrecedences.TIMES)
                .sql(" || '");
        String suffix;
        switch (timeUnit) {
            case NANOSECONDS:
                suffix = " / 1000000000 seconds";
                break;
            case MICROSECONDS:
                suffix = " / 1000000 seconds";
                break;
            case MILLISECONDS:
                suffix = " / 1000 seconds";
                break;
            default:
                suffix = timeUnit.name().toLowerCase();
        }
        builder.sql(suffix).sql("')");
    }
}
