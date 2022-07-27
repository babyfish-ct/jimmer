package org.babyfish.jimmer.sql.kt.ast.table

import kotlin.reflect.KClass

interface KTableEx<E: Any> : KTable<E> {
    override fun <X: Any> join(prop: String): KTableEx<X>
    override fun <X: Any> outerJoin(prop: String): KNullableTableEx<X>
    override fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KTableEx<X>
    override fun <X: Any> inverseOuterJoin(targetType: KClass<X>, backProp: String): KNullableTableEx<X>
}