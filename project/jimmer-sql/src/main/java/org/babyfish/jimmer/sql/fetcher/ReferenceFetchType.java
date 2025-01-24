package org.babyfish.jimmer.sql.fetcher;

/**
 * FetchType for associations decorated by
 * {@link org.babyfish.jimmer.sql.ManyToOne}
 * or {@link org.babyfish.jimmer.sql.OneToOne}
 */
public enum ReferenceFetchType {

    /**
     * Use `jimmer.default-reference-fetch-type`
     */
    AUTO,

    /**
     * Use another batch query to fetch associated objects.
     */
    SELECT,

    /**
     * <ul>
     *  <li>If the association cache is enabled,
     *  query associated objects from cache</li>
     *
     *  <li>otherwise, it is same to {@link #JOIN_ALWAYS}</li>
     * </ul>
     */
    JOIN_IF_NO_CACHE,

    /**
     * Use `left join` to fetch nullable associated objects,
     * or `inner join` to fetch nonnull associated objects.
     *
     * <p>Note</p>
     * <ul>
     *     <li>The max join fetch depth is limited by
     *     `jimmer.max-join-fetch-depth`</li>
     *     <li>If here is conflict predicate join
     *     which is NOT in `or` predicate,
     *     the fetch join and predicate join can be merged</li>
     * </ul>
     */
    JOIN_ALWAYS
}
