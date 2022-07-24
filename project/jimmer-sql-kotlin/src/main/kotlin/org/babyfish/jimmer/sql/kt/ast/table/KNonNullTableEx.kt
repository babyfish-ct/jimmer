package org.babyfish.jimmer.sql.kt.ast.table

import kotlin.reflect.KClass

interface KNonNullTableEx<E: Any> : KNonNullTable<E>, KTableEx<E> {
    override fun <X: Any> join(prop: String): KNonNullTableEx<X>
    override fun <X: Any> outerJoin(prop: String): KNullableTableEx<X>
    override fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KNonNullTableEx<X>
    override fun <X: Any> inverseOuterJoin(targetType: KClass<X>, backProp: String): KNullableTableEx<X>
}