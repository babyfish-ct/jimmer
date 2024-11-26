package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.h2.value.ValueJson;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.List;
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

    @Nullable
    @Override
    public Object jsonToBaseValue(@Nullable String json) throws SQLException {
        return json == null ? null : ValueJson.fromJson(json);
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
    public boolean isIdFetchableByKeyUpdate() {
        return true;
    }

    @Override
    public boolean isUpsertSupported() {
        return true;
    }

    @Override
    public void upsert(UpsertContext ctx) {
        if (!ctx.isUpdateIgnored()) {
            ctx.sql("merge into ")
                    .appendTableName()
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendInsertedColumns("")
                    .leave()
                    .sql(" key")
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendConflictColumns()
                    .leave()
                    .sql(" values")
                    .enter(AbstractSqlBuilder.ScopeType.LIST)
                    .appendInsertingValues()
                    .leave();
            return;
        }
        ctx.sql("merge into ")
                .appendTableName()
                .sql(" tb_1_ using")
                .enter(AbstractSqlBuilder.ScopeType.LIST)
                .sql("values")
                .enter(AbstractSqlBuilder.ScopeType.LIST)
                .appendInsertingValues()
                .leave()
                .leave()
                .sql(" tb_2_")
                .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                .appendInsertedColumns("")
                .leave()
                .sql(" on ");

        ctx.enter(AbstractSqlBuilder.ScopeType.AND);
        for (ValueGetter getter : ctx.getConflictGetters()) {
            ctx.separator().sql("tb_1_.").sql(getter).sql(" = tb_2_.").sql(getter);
        }
        ctx.leave();
        if (ctx.hasGeneratedId() && !ctx.isUpdateIgnored()) {
            ctx.sql(" when matched then update set ").sql(FAKE_UPDATE_COMMENT).sql(" ");
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
            ctx.sql(cheapestGetter).sql(" = tb_2_.").sql(cheapestGetter);
        }
        ctx.sql(" when not matched then insert")
                .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                .appendInsertedColumns("")
                .leave()
                .enter(AbstractSqlBuilder.ScopeType.VALUES)
                .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                .appendInsertedColumns("tb_2_.")
                .leave()
                .leave();
    }
}
