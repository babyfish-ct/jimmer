package org.babyfish.jimmer.sql.meta;

public interface SqlContext {

    /**
     * If the current `SqlContext` is wrapper,
     */
    <T extends SqlContext> T unwrap();

    UserIdGenerator<?> getUserIdGenerator(String ref) throws Exception;

    UserIdGenerator<?> getUserIdGenerator(Class<?> userIdGenerator) throws Exception;

    MetadataStrategy getMetadataStrategy();
}
