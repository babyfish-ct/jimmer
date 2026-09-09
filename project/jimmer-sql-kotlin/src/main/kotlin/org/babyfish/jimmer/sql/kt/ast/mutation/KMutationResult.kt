package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/** Counts of rows affected by a mutation, including cascaded changes and association-table changes. */
interface KMutationResult {

    /** The sum of all counts in [affectedRowCountMap]. */
    val totalAffectedRowCount: Int

    /** Affected entity and association tables mapped to their reported affected-row counts. */
    val affectedRowCountMap: Map<AffectedTable, Int>

    /** The count for the entity table of [entityType], or zero if it is absent from the map. */
    fun affectedRowCount(entityType: KClass<*>): Int

    /** The count for the association table of [prop], or zero if it is absent from the map. */
    fun affectedRowCount(prop: KProperty1<*, *>): Int
}
