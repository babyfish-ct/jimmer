package org.babyfish.jimmer.client.kotlin.model

/**
 * The page object
 * @property totalPageCount Total page count before paging
 */
interface KPage<E> : KSlice<E> {
    /**
     * Total row count before paging
     */
    val totalRowCount: Int

    val totalPageCount: Int
}
