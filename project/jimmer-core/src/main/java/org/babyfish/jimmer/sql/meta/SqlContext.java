package org.babyfish.jimmer.sql.meta;

public interface SqlContext {

    /**
     * If the current `SqlContext` is wrapper,
     */
    <T extends SqlContext> T unwrap();

    UserIdGenerator<?> getUserIdGenerator(String ref) throws Exception;

    UserIdGenerator<?> getUserIdGenerator(Class<?> userIdGeneratorType) throws Exception;

    LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(String ref) throws Exception;

    LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(Class<?> logicalDeletedValueGeneratorType) throws Exception;

    MetadataStrategy getMetadataStrategy();
}
