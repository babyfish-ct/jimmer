package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.exception.SaveException;

/**
 * Notes, this only affect root objects, not associated objects
 *
 * <p>To control associated objects, please view {@link AssociatedSaveMode}</p>
 *
 * @see AssociatedSaveMode
 */
public enum SaveMode {

    /**
     * Insert or update the aggregate-root object(s).
     *
     * <ul>
     * <li>If it's possible to use low-level database upsert
     * capabilities, the corresponding SQL will be generated.</li>
     * <li>Otherwise, Jimmer will first execute a select
     * statement and decide whether the subsequent operation
     * should be an insert or update based on the query results.
     * <p>In this case, the {@code purpose} field of the
     * corresponding SQL statement in the SQL log will
     * provide the query reason. Users can refer to
     * {@link QueryReason}
     * to understand what happened, in an effort to
     * optimize to the former situation.</p>
     * </li>
     * </ul>
     *
     * <p>When a native upsert needs to return an existing row's id or other result fields,
     * it can use a technical update such as {@code SET column = column}, even if there are
     * no property values to update. This also applies to objects containing only keys and
     * associations: Jimmer can need the existing id to save their associations, without an
     * explicit returning request. An empty set of update assignments does not change this
     * mode into {@link #INSERT_IF_ABSENT} or require a preliminary select.</p>
     *
     * <p>The technical update allows conflict resolution and retrieval of the existing row
     * in one atomic database statement. It is still an SQL update and can contribute to
     * affected-row counts and invoke database update triggers. It does not make saving the
     * entire object graph a single atomic statement.</p>
     */
    UPSERT,

    INSERT_ONLY,

    /**
     * Insert the aggregate-root object(s) if absent, without updating conflicting rows.
     *
     * <p>A conflicting root row is not accepted: {@link MutationResultItem#isAccepted()}
     * is {@code false}. Requesting result fields does not return the existing row for such
     * an item; the caller can query it separately if needed. Conflict resolution and that
     * subsequent query are separate database statements.</p>
     */
    INSERT_IF_ABSENT,

    UPDATE_ONLY,

    /**
     * This mode is basically the same as
     * {@link #UPSERT},
     * but it handles wild objects differently.
     *
     * <p>A wild object is an object with neither
     * {@link org.babyfish.jimmer.sql.Id} nor
     * {@link org.babyfish.jimmer.sql.Key}.
     * The difference between this mode and the
     * {@link #UPSERT} mode is as follows:</p>
     *
     * <ul>
     *     <li>{@code UPSERT}: Considers wild
     *     objects illegal, throws
     *     {@link SaveException.NeitherIdNorKey}</li>
     *     <li>{@code NON_IDEMPOTENT_UPSERT}:
     *     Ignores the risk of breaking idempotency
     *     and unconditionally inserts wild objects.</li>
     * </ul>
     *
     * <p>This mode is actually similar to the
     * `{@code saveOrUpdate}` in other frameworks,
     * so most developers are very familiar with it.
     * However, Jimmer does not recommend this mode
     * because it is really not a good idea.</p>
     */
    NON_IDEMPOTENT_UPSERT,
}
