package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.sql.ast.mutation.AbstractMutationResult
import org.babyfish.jimmer.sql.runtime.SavePath
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun SavePath.contains(type: KClass<*>): Boolean =
    contains(type.java)

fun SavePath.contains(prop: KProperty1<*, *>): Boolean =
    contains(prop.toImmutableProp())

fun AbstractMutationResult.getAffectedRowCount(type: KClass<*>): Int =
    getAffectedRowCount(type.java)

fun AbstractMutationResult.getAffectedRowCount(prop: KProperty1<*, *>): Int =
    getAffectedRowCount(prop.toImmutableProp())