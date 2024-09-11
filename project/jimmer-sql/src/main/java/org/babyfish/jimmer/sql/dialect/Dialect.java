package org.babyfish.jimmer.sql.dialect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.meta.SqlTypeStrategy;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.Reader;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public interface Dialect extends SqlTypeStrategy {

    String FAKE_UPDATE_COMMENT = "/* fake update to return all ids */";

    void paginate(PaginationContext ctx);

    @Nullable
    default UpdateJoin getUpdateJoin() {
        return null;
    }

    default String getSelectIdFromSequenceSql(String sequenceName) {
        throw new ExecutionException("Sequence is not supported by '" + getClass().getName() + "'");
    }

    @Nullable
    default String getOverrideIdentityIdSql() {
        return null;
    }

    default boolean isDeletedAliasRequired() { return false; }

    @Nullable
    default String getOffsetOptimizationNumField() {
        return null;
    }

    default boolean isMultiInsertionSupported() { return true; }

    default boolean isArraySupported() { return false; }

    default boolean isAnyEqualityOfArraySupported() {
        return isArraySupported();
    }

    default <T> T[] getArray(ResultSet rs, int col, Class<T[]> arrayType) throws SQLException {
        throw new UnsupportedOperationException("`Dialect.getArray` is not supported");
    }

    default boolean isTupleSupported() {
        return true;
    }

    default boolean isTupleCountSupported() {
        return false;
    }

    @Nullable
    default String getConstantTableName() { return null; }

    default Class<?> getJsonBaseType() {
        return String.class;
    }

    default Object jsonToBaseValue(Object json, ObjectMapper objectMapper) throws Exception {
        return objectMapper.writeValueAsString(json);
    }

    default Object baseValueToJson(Object baseValue, JavaType javaType, ObjectMapper objectMapper) throws Exception {
        return objectMapper.readValue((String) baseValue, javaType);
    }

    default boolean isForeignKeySupported() {
        return true;
    }

    default boolean isIgnoreCaseLikeSupported() { return false; }

    default int resolveJdbcType(Class<?> sqlType) {
        return Types.OTHER;
    }

    default Reader<?> unknownReader(Class<?> sqlType) {
        return null;
    }

    @Nullable
    default String getJsonLiteralSuffix() {
        return null;
    }

    default String transCacheOperatorTableDDL() {
        throw new UnsupportedOperationException(
                "The current dialect \"" +
                        getClass().getName() +
                        "\" does not know how to create table `JIMMER_TRANS_CACHE_OPERATOR`"
        );
    }

    default int getMaxInListSize() {
        return 1000;
    }

    @Override
    default String arrayTypeSuffix() {
        return "[]";
    }

    default boolean isIdFetchableByKeyUpdate() {
        return false;
    }

    default boolean isUpsertSupported() {
        return false;
    }

    default boolean isUpsertWithOptimisticLockSupported() {
        return false;
    }

    default boolean isUpsertWithMultipleUniqueConstraintSupported() {
        return true;
    }

    default boolean isUpsertWithNullableKeySupported() {
        return false;
    }

    default boolean isTransactionAbortedByError() {
        return false;
    }

    default boolean isBatchUpdateExceptionUnreliable() {
        return false;
    }

    void update(UpdateContext ctx);

    void upsert(UpsertContext ctx);

    interface UpdateContext {

        boolean isUpdatedByKey();

        UpdateContext sql(String sql);
        UpdateContext sql(ValueGetter getter);
        UpdateContext enter(AbstractSqlBuilder.ScopeType type);
        UpdateContext separator();
        UpdateContext leave();

        UpdateContext appendTableName();
        UpdateContext appendAssignments();
        UpdateContext appendPredicates();
        UpdateContext appendId();
    }

    interface UpsertContext {

        boolean hasUpdatedColumns();
        boolean hasOptimisticLock();
        boolean hasGeneratedId();
        List<ValueGetter> getConflictGetters();

        UpsertContext sql(String sql);
        UpsertContext sql(ValueGetter getter);
        UpsertContext enter(AbstractSqlBuilder.ScopeType type);
        UpsertContext separator();
        UpsertContext leave();

        UpsertContext appendTableName();
        UpsertContext appendInsertedColumns();
        UpsertContext appendConflictColumns();
        UpsertContext appendInsertingValues();
        UpsertContext appendUpdatingAssignments(String prefix, String suffix);
        UpsertContext appendOptimisticLockCondition();
        UpsertContext appendGeneratedId();
    }
}
