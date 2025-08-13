package org.babyfish.jimmer.sql.ddl.dialect;

import org.babyfish.jimmer.sql.EnumType;
import org.babyfish.jimmer.sql.ast.SqlTimeUnit;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ddl.DDLUtils;
import org.babyfish.jimmer.sql.ddl.DatabaseVersion;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.PaginationContext;
import org.babyfish.jimmer.sql.dialect.UpdateJoin;
import org.babyfish.jimmer.sql.runtime.JdbcTypes;
import org.babyfish.jimmer.sql.runtime.Reader;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

import static java.sql.Types.*;

/**
 * @author honhimW
 */

public abstract class DefaultDDLDialect implements Dialect, DDLDialect {

    protected final Dialect dialect;

    protected final DatabaseVersion version;

    protected DefaultDDLDialect(Dialect dialect) {
        this(dialect, null);
    }

    protected DefaultDDLDialect(Dialect dialect, final DatabaseVersion version) {
        this.dialect = dialect;
        this.version = version;
    }

    protected boolean isSameOrAfter(int major) {
        if (this.version == null) {
            return true;
        } else {
            return this.version.isSameOrAfter(major);
        }
    }

    protected boolean isSameOrAfter(int major, int minor) {
        if (this.version == null) {
            return true;
        } else {
            return this.version.isSameOrAfter(major, minor);
        }
    }

    @Override
    public String columnType(int jdbcType, Long length, Integer precision, Integer scale) {
        length = getLength(jdbcType, length);
        precision = getPrecision(jdbcType, precision);
        scale = getScale(jdbcType, scale);

        switch (jdbcType) {
            case ROWID:
                return "rowid";
            case BOOLEAN:
                return "boolean";
            case TINYINT:
                return "tinyint";
            case SMALLINT:
                return "smallint";
            case INTEGER:
                return "integer";
            case BIGINT:
                return "bigint";
            case FLOAT:
                // this is the floating point type we prefer!
                return DDLUtils.replace("float($p)", null, precision, null);
            case REAL:
                // this type has very unclear semantics in ANSI SQL,
                // so we avoid it and prefer float with an explicit
                // precision
                return "real";
            case DOUBLE:
                // this is just a more verbose way to write float(19)
                return "double precision";

            // these are pretty much synonyms, but are considered
            // separate types by the ANSI spec, and in some dialects
            case NUMERIC:
                return DDLUtils.replace("numeric($p,$s)", null, precision, scale);
            case DECIMAL:
                return DDLUtils.replace("decimal($p,$s)", null, precision, scale);
            case DATE:
                return "date";
            case TIME:
                return DDLUtils.replace("time($p)", null, precision, null);
            case TIME_WITH_TIMEZONE:
                // type included here for completeness but note that
                // very few databases support it, and the general
                // advice is to caution against its use (for reasons,
                // check the comments in the Postgres documentation).
                return DDLUtils.replace("time($p) with time zone", null, precision, null);
            case TIMESTAMP:
                return DDLUtils.replace("timestamp($p)", null, precision, null);
            case TIMESTAMP_WITH_TIMEZONE:
                return DDLUtils.replace("timestamp($p) with time zone", null, precision, null);
            case CHAR:
                return DDLUtils.replace("char($l)", length, null, null);
            case VARCHAR:
                return DDLUtils.replace("varchar($l)", length, null, null);
            case CLOB:
                return "clob";
            case NCHAR:
                return DDLUtils.replace("nchar($l)", length, null, null);
            case NVARCHAR:
                return DDLUtils.replace("nvarchar($l)", length, null, null);
            case NCLOB:
                return "nclob";
            case BINARY:
                return DDLUtils.replace("binary($l)", length, null, null);
            case VARBINARY:
                return DDLUtils.replace("varbinary($l)", length, null, null);
            case BLOB:
                return "blob";
            default:
                throw new IllegalArgumentException("unknown type: " + jdbcType);
        }
    }

    @Override
    public int resolveJdbcType(Class<?> sqlType, EnumType.Strategy strategy) {
        if (sqlType.isEnum()) {
            switch (strategy) {
                case NAME:
                    return Types.VARCHAR;
                case ORDINAL:
                    return Types.SMALLINT;
            }
        }
        return resolveJdbcType(sqlType);
    }

    // Delegating

    @Override
    public int resolveJdbcType(Class<?> sqlType) {
        int i = dialect.resolveJdbcType(sqlType);
        if (i != OTHER) {
            return i;
        } else if (sqlType == Instant.class) {
            return Types.TIMESTAMP;
        }
        return JdbcTypes.toJdbcType(sqlType, this.dialect);
    }

    @Override
    public String jdbcParameter(Class<?> sqlType) {
        return dialect.jdbcParameter(sqlType);
    }

    @Override
    public void paginate(PaginationContext ctx) {
        dialect.paginate(ctx);
    }

    @Override
    public @Nullable UpdateJoin getUpdateJoin() {
        return dialect.getUpdateJoin();
    }

    @Override
    public String getSelectIdFromSequenceSql(String sequenceName) {
        return dialect.getSelectIdFromSequenceSql(sequenceName);
    }

    @Override
    public @Nullable String getOverrideIdentityIdSql() {
        return dialect.getOverrideIdentityIdSql();
    }

    @Override
    public boolean isDeletedAliasRequired() {
        return dialect.isDeletedAliasRequired();
    }

    @Override
    public boolean isDeleteAliasSupported() {
        return dialect.isDeleteAliasSupported();
    }

    @Override
    public boolean isUpdateAliasSupported() {
        return dialect.isUpdateAliasSupported();
    }

    @Override
    public @Nullable String getOffsetOptimizationNumField() {
        return dialect.getOffsetOptimizationNumField();
    }

    @Override
    public boolean isMultiInsertionSupported() {
        return dialect.isMultiInsertionSupported();
    }

    @Override
    public boolean isArraySupported() {
        return dialect.isArraySupported();
    }

    @Override
    public boolean isAnyEqualityOfArraySupported() {
        return dialect.isAnyEqualityOfArraySupported();
    }

    @Override
    public <T> T[] getArray(ResultSet rs, int col, Class<T[]> arrayType) throws SQLException {
        return dialect.getArray(rs, col, arrayType);
    }

    @Override
    public boolean isTupleSupported() {
        return dialect.isTupleSupported();
    }

    @Override
    public boolean isTupleComparisonSupported() {
        return dialect.isTupleComparisonSupported();
    }

    @Override
    public boolean isTupleCountSupported() {
        return dialect.isTupleCountSupported();
    }

    @Override
    public boolean isTableOfSubQueryMutable() {
        return dialect.isTableOfSubQueryMutable();
    }

    @Override
    public @Nullable String getConstantTableName() {
        return dialect.getConstantTableName();
    }

    @Override
    public Class<?> getJsonBaseType() {
        return dialect.getJsonBaseType();
    }

    @Override
    public @Nullable Object jsonToBaseValue(@Nullable String json) throws SQLException {
        return dialect.jsonToBaseValue(json);
    }

    @Override
    public @Nullable String baseValueToJson(@Nullable Object baseValue) throws SQLException {
        return dialect.baseValueToJson(baseValue);
    }

    @Override
    public boolean isForeignKeySupported() {
        return dialect.isForeignKeySupported();
    }

    @Override
    public boolean isIgnoreCaseLikeSupported() {
        return dialect.isIgnoreCaseLikeSupported();
    }

    @Override
    public Reader<?> unknownReader(Class<?> sqlType) {
        return dialect.unknownReader(sqlType);
    }

    @Override
    public String transCacheOperatorTableDDL() {
        return dialect.transCacheOperatorTableDDL();
    }

    @Override
    public int getMaxInListSize() {
        return dialect.getMaxInListSize();
    }

    @Override
    public String arrayTypeSuffix() {
        return dialect.arrayTypeSuffix();
    }

    @Override
    public boolean isIdFetchableByKeyUpdate() {
        return dialect.isIdFetchableByKeyUpdate();
    }

    @Override
    public boolean isInsertedIdReturningRequired() {
        return dialect.isInsertedIdReturningRequired();
    }

    @Override
    public boolean isExplicitBatchRequired() {
        return dialect.isExplicitBatchRequired();
    }

    @Override
    public boolean isBatchDumb() {
        return dialect.isBatchDumb();
    }

    @Override
    public boolean isUpsertSupported() {
        return dialect.isUpsertSupported();
    }

    @Override
    public boolean isNoIdUpsertSupported() {
        return dialect.isNoIdUpsertSupported();
    }

    @Override
    public boolean isUpsertWithOptimisticLockSupported() {
        return dialect.isUpsertWithOptimisticLockSupported();
    }

    @Override
    public boolean isUpsertWithMultipleUniqueConstraintSupported() {
        return dialect.isUpsertWithMultipleUniqueConstraintSupported();
    }

    @Override
    public boolean isUpsertWithNullableKeySupported() {
        return dialect.isUpsertWithNullableKeySupported();
    }

    @Override
    public boolean isTransactionAbortedByError() {
        return dialect.isTransactionAbortedByError();
    }

    @Override
    public boolean isBatchUpdateExceptionUnreliable() {
        return dialect.isBatchUpdateExceptionUnreliable();
    }

    @Override
    public void update(UpdateContext ctx) {
        dialect.update(ctx);
    }

    @Override
    public void upsert(UpsertContext ctx) {
        dialect.upsert(ctx);
    }

    @Override
    public void renderLPad(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast expression, Ast length, Ast padString) {
        dialect.renderLPad(builder, currentPrecedence, expression, length, padString);
    }

    @Override
    public void renderRPad(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast expression, Ast length, Ast padString) {
        dialect.renderRPad(builder, currentPrecedence, expression, length, padString);
    }

    @Override
    public void renderPosition(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast subStrAst, Ast expressionAst, @Nullable Ast startAst) {
        dialect.renderPosition(builder, currentPrecedence, subStrAst, expressionAst, startAst);
    }

    @Override
    public void renderLeft(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast expressionAst, Ast lengthAst) {
        dialect.renderLeft(builder, currentPrecedence, expressionAst, lengthAst);
    }

    @Override
    public void renderRight(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast expressionAst, Ast lengthAst) {
        dialect.renderRight(builder, currentPrecedence, expressionAst, lengthAst);
    }

    @Override
    public void renderSubString(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast expressionAst, Ast startAst, @Nullable Ast lengthAst) {
        dialect.renderSubString(builder, currentPrecedence, expressionAst, startAst, lengthAst);
    }

    @Override
    public void renderTimePlus(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast expressionAst, Ast valueAst, SqlTimeUnit timeUnit) {
        dialect.renderTimePlus(builder, currentPrecedence, expressionAst, valueAst, timeUnit);
    }

    @Override
    public void renderTimeDiff(AbstractSqlBuilder<?> builder, int currentPrecedence, Ast expressionAst, Ast otherAst, SqlTimeUnit timeUnit) {
        dialect.renderTimeDiff(builder, currentPrecedence, expressionAst, otherAst, timeUnit);
    }

    @Override
    public Timestamp getTimestamp(ResultSet rs, int col) throws SQLException {
        return dialect.getTimestamp(rs, col);
    }

    @Override
    public String sqlType(Class<?> elementType) {
        return dialect.sqlType(elementType);
    }

    protected Long getLength(int jdbcType, Long length) {
        if (length == null || length < 0) {
            return getDefaultLength(jdbcType);
        } else {
            return length;
        }
    }

    protected Integer getScale(int jdbcType, Integer scale) {
        if (scale == null || scale < 0) {
            return getDefaultScale(jdbcType);
        } else {
            return scale;
        }
    }

    protected Integer getPrecision(int jdbcType, Integer precision) {
        if (precision == null || precision < 0) {
            return DDLUtils.resolveDefaultPrecision(jdbcType, this);
        } else {
            return precision;
        }
    }
}
