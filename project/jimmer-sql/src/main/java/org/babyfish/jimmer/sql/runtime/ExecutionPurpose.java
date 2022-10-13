package org.babyfish.jimmer.sql.runtime;

public enum ExecutionPurpose {
    QUERY,
    UPDATE,
    DELETE,
    DATA_LOADER,
    SAVE_COMMAND,
    EVICT_CACHE,
    JOIN_FOR_DELETE
}
