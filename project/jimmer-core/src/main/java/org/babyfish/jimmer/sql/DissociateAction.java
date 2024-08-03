package org.babyfish.jimmer.sql;

/**
 * Configuration for many-to-one (or one-to-one) associations directly based on foreign keys.
 *
 * <p>In the following two cases, the parent object will disassociate from the child objects:</p>
 *
 * <ul>
 *     <li>When the parent object is deleted (either physically or logically)</li>
 *     <li>When the user overrides the parent object using a save command,
 *     the collection of child objects in the saved data structure is specified,
 *     but does not contain some child objects that already exist in the database,
 *     the exists child objects will be deleted (either physically or logically).
 *     In this case, each child object will dissociate the grandchild objects.
 *     </li>
 * </ul>
 */
public enum DissociateAction {

    /**
     * The default option, it is equivalent to {@link #LAX} or {@link #CHECK}
     * according to the global configuration `jimmer.default-dissociate-action-checking`
     */
    NONE,

    /**
     * Do nothing, or let database execute the cascade delete.
     *
     * <p>For physical deletion, if the foreign key constraint exists, `on delete cascade` is required</p>
     *
     * <p>This argument can be used config the database-level cascade deletion for foreign key in entity table,
     * for foreign key in middle table, please view {@link JoinTable#cascadeDeletedBySource()} and
     * {@link JoinTable#cascadeDeletedByTarget()}
     * </p>
     */
    LAX,

    /**
     * If the deleted parent object has some child objects,
     * throw an exception to prevent the operation.
     */
    CHECK,

    /**
     * If the deleted parent object has some child objects,
     * clear foreign key of child objects.
     */
    SET_NULL,

    /**
     * <p>For physical deletion, if the deleted parent object has
     * some child objects, physically delete child objects too.</p>
     *
     * <p>For logical deletion, if the deleted parent object has
     * some child objects, logically delete child objects too.</p>
     *
     * <p>Notes: If you hope the child objects should be deleted by
     * database through "on delete cascade" option of foreign key of
     * constraint, please don't let ORM to delete them and {@link #LAX}
     * should be used</p>
     */
    DELETE
}
