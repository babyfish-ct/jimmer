package org.babyfish.jimmer.sql.ast.mutation;

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
     * {@link org.babyfish.jimmer.sql.ast.impl.mutation.QueryReason}
     * to understand what happened, in an effort to
     * optimize to the former situation.</p>
     * </li>
     * </ul>
     */
    UPSERT,

    INSERT_ONLY,

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
     *     {@link org.babyfish.jimmer.sql.runtime.SaveException.NeitherIdNorKey}</li>
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

