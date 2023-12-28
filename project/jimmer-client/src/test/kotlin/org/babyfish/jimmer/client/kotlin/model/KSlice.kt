package org.babyfish.jimmer.client.kotlin.model

interface KSlice<E> {

    /**
     * The entities in the current page
     */
    val entities: List<E>
}