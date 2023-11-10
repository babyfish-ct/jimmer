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
     * This is a special option. If the parent object is physically deleted and the foreign key of the
     * child object is true (foreign key constraint exists in the database), it will be ignored,
     * equivalent to {@link #CHECK}.
     *
     * <p>In other cases, this option takes effect. When the parent object is deleted (either physically
     * or logically), regardless whether there are some existing child objects in the database or not.</p>
     */
    LAX,

    /**
     * If the deleted parent object has some child objects, throw an exception to prevent the operation.
     */
    CHECK,

    /**
     * If the deleted parent object has some child objects, clear foreign key of child objects.
     */
    SET_NULL,

    /**
     * If the deleted parent object has some child objects, delete child objects too.
     */
    DELETE
}
