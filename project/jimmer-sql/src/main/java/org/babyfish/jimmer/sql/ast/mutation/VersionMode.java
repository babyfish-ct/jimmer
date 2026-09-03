package org.babyfish.jimmer.sql.ast.mutation;

/**
 * Determines how a save command treats the {@code @Version} property of its
 * root entity.
 */
public enum VersionMode {

    /**
     * Use the loaded version as an implicit optimistic-lock condition and
     * increase the version automatically after a successful update.
     */
    OPTIMISTIC_LOCK,

    /**
     * Treat the version as an ordinary assignment controlled by the entity
     * shape, upsert mask, custom save assignments, and update conditions.
     * A loaded version is neither an implicit optimistic-lock condition nor
     * increased automatically. On insertion, an unloaded version still uses
     * its framework-defined initial value.
     */
    ASSIGNMENT
}
