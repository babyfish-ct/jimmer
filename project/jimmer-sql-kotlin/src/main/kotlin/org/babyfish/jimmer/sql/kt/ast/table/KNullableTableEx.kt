package org.babyfish.jimmer.sql.kt.ast.table

interface KNullableTableEx<E: Any> : KNullableTable<E>, KTableEx<E> {
    override fun <X: Any> join(prop: String): KNullableTableEx<X>
    override fun <X: Any> inverseJoin(prop: String): KNullableTableEx<X>
}