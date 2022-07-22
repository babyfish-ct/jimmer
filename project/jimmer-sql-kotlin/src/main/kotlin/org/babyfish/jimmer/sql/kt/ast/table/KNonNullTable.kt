package org.babyfish.jimmer.sql.kt.ast.table

interface KNonNullTable<E> : KTable<E> {

    override fun <X: Any> join(prop: String): KNonNullTable<X>
}