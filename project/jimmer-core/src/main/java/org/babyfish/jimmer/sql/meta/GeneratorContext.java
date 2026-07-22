package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.jetbrains.annotations.Nullable;

public interface GeneratorContext {

    IdGenerator getIdGenerator(ImmutableType type);

    @Nullable
    LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(@Nullable LogicalDeletedInfo info);

    UserIdGenerator<?> getUserIdGenerator(String ref) throws Exception;

    UserIdGenerator<?> getUserIdGenerator(Class<?> userIdGeneratorType) throws Exception;

    LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(String ref) throws Exception;

    LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(Class<?> logicalDeletedValueGeneratorType) throws Exception;
}
