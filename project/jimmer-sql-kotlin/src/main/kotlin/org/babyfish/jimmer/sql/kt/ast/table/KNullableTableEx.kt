package org.babyfish.jimmer.sql.kt.ast.table

import kotlin.reflect.KClass

interface KNullableTableEx<E: Any> : KNullableTable<E>, KTableEx<E> {

    override fun <X : Any> weakJoin(weakJoinType: KClass<out KWeakJoin<E, X>>): KNullableTableEx<X>
}