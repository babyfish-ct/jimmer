package org.babyfish.jimmer.sql.runtime;

public enum ExecutionPurpose {
    QUERY,
    UPDATE,
    DELETE,
    LOADER,
    MUTATE,
    EVICT,
}
