package org.babyfish.jimmer.sql.kt.ast.mutation

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KMutationResult {

    val totalAffectedRowCount: Int

    fun affectedRowCount(entityType: KClass<*>): Int

    fun affectedRowCount(prop: KProperty1<*, *>): Int
}