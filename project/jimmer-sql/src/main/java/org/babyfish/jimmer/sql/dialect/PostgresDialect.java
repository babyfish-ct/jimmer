package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.impl.util.Classes;
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
                .enter(AbstractSqlBuilder.ScopeType.LIST)
                .appendInsertedColumns("")
                .leave()
                .enter(AbstractSqlBuilder.ScopeType.VALUES)
                .enter(AbstractSqlBuilder.ScopeType.LIST)
                .appendInsertingValues()
                .leave()
                .leave()
                .sql(" on conflict")
                .enter(AbstractSqlBuilder.ScopeType.LIST)
                .appendConflictColumns()
                .leave();
        if (ctx.hasUpdatedColumns()) {
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
            ctx.sql(" do update set ").sql(FAKE_UPDATE_COMMENT).sql(" ");
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
}
