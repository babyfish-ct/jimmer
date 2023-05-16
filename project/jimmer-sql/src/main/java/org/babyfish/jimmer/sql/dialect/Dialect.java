package org.babyfish.jimmer.sql.dialect;

import com.fasterxml.jackson.databind.JavaType;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.Reader;
import org.jetbrains.annotations.Nullable;

import java.sql.Types;

public interface Dialect {

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

    @Nullable
    default String getConstantTableName() { return null; }

    default Class<?> getJsonBaseType() {
        return String.class;
    }

    default Object jsonToBaseValue(Object json) throws Exception {
        return JsonUtils.OBJECT_MAPPER.writeValueAsString(json);
    }

    default Object baseValueToJson(Object baseValue, JavaType javaType) throws Exception {
        return JsonUtils.OBJECT_MAPPER.readValue((String) baseValue, javaType);
    }

    default boolean isForeignKeySupported() {
        return true;
    }

    default int resolveUnknownJdbcType(Class<?> sqlType) {
        return Types.OTHER;
    }

    default Reader<?> unknownReader(Class<?> sqlType) {
        return null;
    }
}
