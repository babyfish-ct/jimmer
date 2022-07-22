package org.babyfish.jimmer.sql.kt.util

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaMethod

fun immutableProp(prop: KProperty1<*, *>): ImmutableProp {
    val javaMethod = prop.getter.javaMethod ?: error("$prop does not has java getter")
    return ImmutableType.get(javaMethod.declaringClass).getProp(prop.name)
}