package org.babyfish.jimmer.sql.kt.ast.table

interface KNonNullTable<E: Any> : KTable<E> {
    override fun <X: Any> join(prop: String): KNonNullTable<X>
    override fun <X: Any> outerJoin(prop: String): KNullableTable<X>
    override fun <X: Any> inverseJoin(prop: String): KNonNullTable<X>
    override fun <X: Any> inverseOuterJoin(prop: String): KNullableTable<X>
}