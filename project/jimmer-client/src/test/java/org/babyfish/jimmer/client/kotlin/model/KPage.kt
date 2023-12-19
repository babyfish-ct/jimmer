package org.babyfish.jimmer.client.kotlin.model

/**
 * The page object
 * @property entities The entities in the current page
 */
data class KPage<E>(
    /**
     * Total row count before paging
     */
    val totalRowCount: Int,

    /**
     * Total page count before paging
     */
    val totalPageCount: Int,

    val entities: List<E>
) {

    override fun toString(): String {
        return "Page{" +
            "totalRowCount=" + totalRowCount +
            ", totalPageCount=" + totalPageCount +
            ", entities=" + entities +
            '}'
    }
}
