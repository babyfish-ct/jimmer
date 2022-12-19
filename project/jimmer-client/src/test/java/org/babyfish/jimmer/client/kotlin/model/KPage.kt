package org.babyfish.jimmer.client.kotlin.model

data class KPage<E>(val totalRowCount: Int, val totalPageCount: Int, val entities: List<E>) {

    override fun toString(): String {
        return "Page{" +
            "totalRowCount=" + totalRowCount +
            ", totalPageCount=" + totalPageCount +
            ", entities=" + entities +
            '}'
    }
}
