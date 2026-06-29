package org.babyfish.jimmer.sql;

/**
 * Physical delete dissociation action for {@link InheritanceType#JOINED}
 * inheritance subtype tables.
 */
public enum JoinedTableDissociateAction {

    /**
     * Jimmer deletes joined subtype table rows explicitly before deleting the root table row.
     *
     * <p>This is the portable default because not every database/schema uses {@code ON DELETE CASCADE}.</p>
     */
    DELETE,

    /**
     * Jimmer relies on database-level {@code ON DELETE CASCADE} from joined subtype tables to the root table.
     *
     * <p>This is the recommended action when the database and schema support it. It avoids extra delete
     * statements for joined subtype tables and has the same semantic role as {@link DissociateAction#LAX}
     * has for association foreign keys.</p>
     */
    LAX
}
