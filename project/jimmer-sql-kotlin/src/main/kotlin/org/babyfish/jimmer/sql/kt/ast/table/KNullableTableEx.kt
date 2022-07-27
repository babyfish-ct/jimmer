package org.babyfish.jimmer.sql.kt.ast.table

import kotlin.reflect.KClass

interface KNullableTableEx<E: Any> : KNullableTable<E>, KTableEx<E> {
    override fun <X: Any> join(prop: String): KNullableTableEx<X>
    override fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KNullableTableEx<X>
}