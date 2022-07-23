package org.babyfish.jimmer.sql.kt.ast.table

interface KNonNullTableEx<E: Any> : KNonNullTable<E>, KTableEx<E> {
    override fun <X: Any> join(prop: String): KNonNullTableEx<X>
    override fun <X: Any> outerJoin(prop: String): KNullableTableEx<X>
    override fun <X: Any> inverseJoin(prop: String): KNonNullTableEx<X>
    override fun <X: Any> inverseOuterJoin(prop: String): KNullableTableEx<X>
}