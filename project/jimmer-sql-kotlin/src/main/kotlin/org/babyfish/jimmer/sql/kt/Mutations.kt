package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.sql.ast.mutation.AbstractMutationResult
import org.babyfish.jimmer.sql.runtime.MutationPath
import org.babyfish.jimmer.sql.exception.SaveException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun MutationPath.contains(type: KClass<*>): Boolean =
    contains(type.java)

fun MutationPath.contains(prop: KProperty1<*, *>): Boolean =
    contains(prop.toImmutableProp())

fun AbstractMutationResult.getAffectedRowCount(type: KClass<*>): Int =
    getAffectedRowCount(type.java)

fun AbstractMutationResult.getAffectedRowCount(prop: KProperty1<*, *>): Int =
    getAffectedRowCount(prop.toImmutableProp())

fun <T: Any> SaveException.NotUnique.isMatched(
    vararg props: KProperty1<T, *>
): Boolean = isMatched(
    *props.map { it.toImmutableProp() }.toTypedArray()
)

@Suppress("UNCHECKED_CAST")
operator fun <T: Any> SaveException.NotUnique.get(prop: KProperty1<*, T>): T =
    getValue(prop.toImmutableProp()) as T