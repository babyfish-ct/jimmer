package org.babyfish.jimmer.sql.kt.filter

interface KFilter<E: Any> {

    fun filter(args: KFilterArgs<E>)
}