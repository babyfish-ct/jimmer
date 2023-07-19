package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KMutationResult {

    val totalAffectedRowCount: Int

    val affectedRowCountMap: Map<AffectedTable, Int>

    fun affectedRowCount(entityType: KClass<*>): Int

    fun affectedRowCount(prop: KProperty1<*, *>): Int
}