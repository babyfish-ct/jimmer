package org.babyfish.jimmer.sql.kt.ast.table

interface KTableEx<E: Any> : KTable<E> {
    override fun <X: Any> join(prop: String): KTableEx<X>
    override fun <X: Any> outerJoin(prop: String): KNullableTableEx<X>
    override fun <X: Any> inverseJoin(prop: String): KTableEx<X>
    override fun <X: Any> inverseOuterJoin(prop: String): KNullableTableEx<X>
}