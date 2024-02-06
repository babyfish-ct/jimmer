package org.babyfish.jimmer.sql.ast.mutation;

/**
 * Notes, this only affect root objects, not deeper objects
 */
public enum SaveMode {
    UPSERT,
    INSERT_ONLY,
    UPDATE_ONLY
}
