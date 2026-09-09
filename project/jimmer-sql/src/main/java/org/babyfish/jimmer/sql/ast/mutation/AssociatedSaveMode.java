package org.babyfish.jimmer.sql.ast.mutation;

/**
 * If the current entity object has associated objects, 
 * and these associated objects have properties beyond just an id, 
 * then these associated objects will also be automatically saved.
 * 
 * <p>The associations of an entity object can be divided into two types:</p>
 * <ul>
 * <li>Associations based on foreign keys. 
 * These associated objects are saved before the current object.</li>
 * <li>Associations not based on foreign keys (
 * such as associations based on intermediate tables, one-to-many associations). 
 * These associated objects are saved after the current object.</li>
 * </ul>
 * 
 * This enumeration specifies how to cascade save the second type of associations.
 *
 * @see SaveMode
 */
public enum AssociatedSaveMode {

    /**
     * On the basis of {@link #MERGE}, perform dissociation processing for
     * associated objects that are no longer needed by the current entity.
     *
     * <p>Note that this mode requires the associated object to have either an
     * {@link org.babyfish.jimmer.sql.Id} or {@link org.babyfish.jimmer.sql.Key} properties
     * otherwise an error will be reported.</p>
     */
    REPLACE,

    /**
     * If the associated object exists, update it; otherwise, insert it.
     *
     * <p>Like {@link SaveMode#UPSERT}, a native upsert can use a technical update such as
     * {@code SET column = column} to return the existing id needed for associations,
     * even when no property values need updating and no result fields were explicitly
     * requested. This is still an SQL update, with the corresponding database side effects.</p>
     *
     * <p>Note that this mode requires the associated object to have either an
     * {@link org.babyfish.jimmer.sql.Id} or {@link org.babyfish.jimmer.sql.Key} properties
     * otherwise an error will be reported.</p>
     */
    MERGE,

    /**
     * Unconditionally insert the associated object. If the associated object already exists,
     * repeated insertion will result in an error.
     */
    APPEND,

    /**
     * Insert the associated object if it is absent.
     * The operation will be ignored if the associated object already exists.
     *
     * <p>An existing target's fields and dependent associations are not saved. Its id
     * can still be looked up to link it to an accepted owner when the ignored insert
     * does not return that id. Existing links of the owner are preserved; this mode
     * adds missing links rather than replacing the association.</p>
     */
    APPEND_IF_ABSENT,

    /**
     * Unconditionally update the associated object. If the associated object doesn't already exist,
     * update will result in an error.
     */
    UPDATE,

    /**
     * It functions the same as {@link #REPLACE},
     * but has lower usage requirements and lower performance.
     *
     * <p>{@link #REPLACE} uses the {@link org.babyfish.jimmer.sql.Id} or
     * {@link org.babyfish.jimmer.sql.Key} properties of the associated object to
     * cleverly compare the data structure in the database with the data structure
     * that the user is trying to save, finding the differences between the two.
     * It's similar to the `virtual DOM diff` in React in the web domain.</p>
     *
     * <p>The operation method is very simple, first clearing all old associated
     * objects<i>(If the association is NOT parent based on foreign key,
     * because parent object(s) will NEVER be detached)</i>,
     * then reinserting new associated objects. The performance is not as good
     * as {@link #REPLACE} and may even cause event storms when using triggers.
     * However, it can accept associated objects that have neither
     * {@link org.babyfish.jimmer.sql.Id} nor {@link org.babyfish.jimmer.sql.Key}.</p>
     *
     * <p>Unless you really need to accept associated objects that have neither
     * {@link org.babyfish.jimmer.sql.Id} nor {@link org.babyfish.jimmer.sql.Key},
     * please do NOT use this mode.</p>
     */
    VIOLENTLY_REPLACE
}
