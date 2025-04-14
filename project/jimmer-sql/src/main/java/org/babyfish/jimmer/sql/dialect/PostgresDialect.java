package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.runtime.Reader;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.List;
import java.util.UUID;

public class PostgresDialect extends DefaultDialect {

    private static final Reader<PGobject> PG_OBJECT_READER = new Reader<PGobject>() {
        @Override
        public PGobject read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), PGobject.class);
        }
    };

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
    public Object jsonToBaseValue(@Nullable String json) throws SQLException {
        PGobject pgobject = new PGobject();
        pgobject.setType("jsonb");
        pgobject.setValue(json);
        return pgobject;
    }

    @Override
    public @Nullable String baseValueToJson(@Nullable Object baseValue) throws SQLException {
        return baseValue == null ? null : ((PGobject) baseValue).getValue();
    }

    @Override
    public Reader<?> unknownReader(Class<?> sqlType) {
        if (sqlType == PGobject.class) {
            return PG_OBJECT_READER;
        }
        return null;
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
        if (elementType == Date.class || elementType == LocalDate.class) {
            return "date";
        }
        if (elementType == Time.class || elementType == LocalTime.class) {
            return "time";
        }
        if (elementType == OffsetTime.class) {
            return "time with time zone";
        }
        if (elementType == java.util.Date.class || elementType == Timestamp.class) {
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] getArray(ResultSet rs, int col, Class<T[]> arrayType) throws SQLException {
        Array array = rs.getArray(col);
        if (array != null) {
            return (T[]) array.getArray();
        }
        return null;
    }

    public Reader<String> jsonReader() {
        return (rs, col) -> {
            PGobject pgObject = rs.getObject(col.col(), PGobject.class);
            return pgObject == null ? null : pgObject.getValue();
        };
    }

    @Override
    public boolean isIdFetchableByKeyUpdate() {
        return true;
    }

    @Override
    public boolean isInsertedIdReturningRequired() {
        return true;
    }

    @Override
    public boolean isUpsertSupported() {
        return true;
    }

    @Override
    public boolean isUpsertWithOptimisticLockSupported() {
        return true;
    }

    @Override
    public boolean isUpsertWithNullableKeySupported() {
        return true;
    }

    @Override
    public boolean isTransactionAbortedByError() {
        return true;
    }

    @Override
    public boolean isBatchUpdateExceptionUnreliable() {
        return true;
    }

    @Override
    public void update(UpdateContext ctx) {
        if (!ctx.isUpdatedByKey()) {
            super.update(ctx);
            return;
        }
        ctx
                .sql("update ")
                .appendTableName()
                .enter(AbstractSqlBuilder.ScopeType.SET)
                .appendAssignments()
                .leave()
                .enter(AbstractSqlBuilder.ScopeType.WHERE)
                .appendPredicates()
                .leave()
                .sql(" returning ")
                .appendId();
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
            if (ctx.hasGeneratedId()) {
                ctx.sql(" returning ").appendGeneratedId();
            }
        } else if (ctx.hasUpdatedColumns()) {
            ctx.sql(" do update")
                    .enter(AbstractSqlBuilder.ScopeType.SET)
                    .appendUpdatingAssignments("excluded.", "")
                    .leave();
            if (ctx.hasOptimisticLock()) {
                ctx.sql(" where ").appendOptimisticLockCondition("excluded.");
            }
            if (ctx.hasGeneratedId()) {
                ctx.sql(" returning ").appendGeneratedId();
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
            ctx.sql(FAKE_UPDATE_COMMENT)
                    .sql(" ")
                    .sql(cheapestGetter)
                    .sql(" = excluded.")
                    .sql(cheapestGetter);
            ctx.sql(" returning ").appendGeneratedId();
        } else {
            ctx.sql(" do nothing");
        }
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

    @Override
    public void renderPosition(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast subStrAst,
            Ast expressionAst,
            @Nullable Ast startAst
    ) {
        if (startAst == null) {
            super.renderPosition(
                    builder,
                    currentPrecedence,
                    subStrAst,
                    expressionAst,
                    null
            );
            return;
        }

        // case
        //     when startAst > length(expressionAst) then 0
        //     else strpos(sbustring(expessionAst from startAst), subStrAst) + startAst - 1)
        // end
        builder
                .sql("case when ")
                .ast(startAst, currentPrecedence)
                .sql(" > length(")
                .ast(expressionAst, currentPrecedence)
                .sql(") then 0 else strpos(substring(")
                .ast(expressionAst, currentPrecedence)
                .sql(" from ")
                .ast(startAst, currentPrecedence)
                .sql("), ")
                .ast(subStrAst, currentPrecedence)
                .sql(") + ")
                .ast(startAst, currentPrecedence)
                .sql(" - 1 end");
    }

    @Override
    public void renderTimePlus(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expressionAst,
            Ast valueAst,
            SqlTimeUnit timeUnit
    ) {
        builder.ast(expressionAst, ExpressionPrecedences.PLUS);
        builder.sql(" + ");
        builder.ast(valueAst, ExpressionPrecedences.TIMES);

        switch (timeUnit) {
            case NANOSECONDS:
                builder.sql(" * interval '1 nanosecond");
                break;
            case MICROSECONDS:
                builder.sql(" * interval '1 microsecond'");
                break;
            case MILLISECONDS:
                builder.sql(" * interval '1 millisecond'");
                break;
            case SECONDS:
                builder.sql(" * interval '1 second'");
                break;
            case MINUTES:
                builder.sql(" * interval '1 minute'");
                break;
            case HOURS:
                builder.sql(" * interval '1 hour'");
                break;
            case DAYS:
                builder.sql(" * interval '1 day'");
                break;
            case WEEKS:
                builder.sql(" * interval '1 week'");
                break;
            case MONTHS:
                builder.sql(" * interval '1 month'");
                break;
            case QUARTERS:
                builder.sql(" * interval '3 month'");
                break;
            case YEARS:
                builder.sql(" * interval '1 year'");
                break;
            case DECADES:
                builder.sql(" * interval '10 year'");
                break;
            case CENTURIES:
                builder.sql(" * interval '100 year'");
                break;
            default:
                throw new IllegalStateException(
                        "Time plus/minus by unit \"" +
                                timeUnit +
                                "\" is not supported by \"" +
                                this.getClass().getName() +
                                "\""
                );
        }
    }
}
