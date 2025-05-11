package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Slice
import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.ast.query.PageFactory
import org.babyfish.jimmer.sql.kt.ast.expression.constant
import org.babyfish.jimmer.sql.kt.ast.expression.rowCount
import java.sql.Connection

interface KConfigurableRootQuery<E: Any, R> : KTypedRootQuery<R> {

    /**
     * Ignore the sorting and pagination settings of the current query,
     * query the total number of data before pagination
     *
     * <p>
     *     In general, users do not need to directly use this method,
     *      but call the `fetchPage` method instead
     * </p>
     *
     * @param con The explicit jdbc connection, null means using default connection
     * @return Total row count before pagination
     */
    fun fetchUnlimitedCount(con: Connection? = null): Long =
        reselect { select(rowCount()) }
            .withoutSortingAndPaging()
            .execute(con)[0]

    fun exists(con: Connection? = null): Boolean =
        limit(1)
            .reselect { select(constant(1)) }
            .execute(con).isNotEmpty()

    fun <P: Any> fetchPage(
        pageIndex: Int,
        pageSize: Int,
        con: Connection? = null,
        pageFactory: PageFactory<R, P>
    ): P

    fun fetchPage(
        pageIndex: Int,
        pageSize: Int,
        con: Connection? = null
    ): Page<R> = fetchPage(pageIndex, pageSize, con, PageFactory.standard())

    fun fetchSlice(
        limit: Int,
        offset: Int,
        con: Connection? = null
    ) : Slice<R>

    @NewChain
    fun <X> reselect(
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, X>
    ): KConfigurableRootQuery<E, X>

    @NewChain
    fun distinct(): KConfigurableRootQuery<E, R>

    @NewChain
    fun limit(limit: Int): KConfigurableRootQuery<E, R>

    @NewChain
    fun offset(offset: Long): KConfigurableRootQuery<E, R>

    @NewChain
    fun limit(limit: Int, offset: Long): KConfigurableRootQuery<E, R>

    @NewChain
    fun withoutSortingAndPaging(): KConfigurableRootQuery<E, R>

    /**
     * @return If the original query does not have `order by` clause, returns null
     */
    @NewChain
    fun reverseSorting(): KConfigurableRootQuery<E, R>?

    @NewChain
    fun setReverseSortOptimizationEnabled(enabled: Boolean): KConfigurableRootQuery<E, R>?

    @NewChain
    fun forUpdate(forUpdate: Boolean = true): KConfigurableRootQuery<E, R>

    /**
     * Set the hint
     * @param hint Optional hint, both /&#42;+ sth &#42;/ and **sth** are OK.
     * @return A new query object
     */
    @NewChain
    fun hint(hint: String?): KConfigurableRootQuery<E, R>
}
