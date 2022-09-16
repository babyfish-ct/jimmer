package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.sql.ast.mutation.AbstractMutationResult
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutationResult
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal abstract class KMutationResultImpl(
    protected val javaResult: AbstractMutationResult
) : KMutationResult {

    override val totalAffectedRowCount: Int
        get() = javaResult.totalAffectedRowCount

    override fun affectedRowCount(entityType: KClass<*>): Int =
        javaResult.getAffectedRowCount(AffectedTable.of(entityType.java))

    override fun affectedRowCount(prop: KProperty1<*, *>): Int =
        javaResult.getAffectedRowCount(
            AffectedTable.of(prop.toImmutableProp())
        )

    override fun hashCode(): Int {
        return javaResult.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is KMutationResultImpl) {
            return false
        }
        return javaResult == other.javaResult
    }

    override fun toString(): String {
        return javaResult.toString()
    }
}