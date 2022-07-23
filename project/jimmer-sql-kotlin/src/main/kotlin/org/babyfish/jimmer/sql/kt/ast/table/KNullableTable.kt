package org.babyfish.jimmer.sql.kt.ast.table

interface KNullableTable<E: Any> : KTable<E> {
    override fun <X: Any> join(prop: String): KNullableTable<X>
    override fun <X: Any> inverseJoin(prop: String): KNullableTable<X>
}