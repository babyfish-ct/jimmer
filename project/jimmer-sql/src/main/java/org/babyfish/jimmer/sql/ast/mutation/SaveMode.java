package org.babyfish.jimmer.sql.ast.mutation;

/**
 * Notes, this only affect root objects, not associated objects
 *
 * <p>To control associated objects, please view {@link AssociatedSaveMode}</p>
 */
public enum SaveMode {
    UPSERT,
    INSERT_ONLY,
    UPDATE_ONLY
}
