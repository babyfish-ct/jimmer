package org.babyfish.jimmer.sql;

/**
 * Physical delete cleanup mode for {@link InheritanceType#JOINED} inheritance subtype tables.
 */
public enum JoinedTableDeleteMode {

    /**
     * Jimmer deletes joined subtype table rows explicitly before deleting the root table row.
     *
     * <p>This is the portable default because not every database/schema uses {@code ON DELETE CASCADE}.</p>
     */
    EXPLICIT,

    /**
     * Jimmer relies on database-level {@code ON DELETE CASCADE} from joined subtype tables to the root table.
     *
     * <p>This is the recommended mode when the database and schema support it. It avoids extra delete
     * statements for joined subtype tables and has the same semantic role as {@link DissociateAction#LAX}
     * has for association foreign keys.</p>
     */
    DB_CASCADE
}
