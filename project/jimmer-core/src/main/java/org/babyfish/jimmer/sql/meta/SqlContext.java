package org.babyfish.jimmer.sql.meta;

public interface SqlContext {

    UserIdGenerator<?> getUserIdGenerator(Class<?> userIdGenerator) throws Exception;

    MetadataStrategy getMetadataStrategy();
}
