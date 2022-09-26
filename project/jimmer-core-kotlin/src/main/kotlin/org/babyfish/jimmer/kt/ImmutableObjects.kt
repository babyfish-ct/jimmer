package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaMethod

fun <T> isLoaded(obj: T, prop: KProperty1<T, *>): Boolean =
    ImmutableObjects.isLoaded(obj, prop.toImmutableProp())

@SuppressWarnings("unchecked")
fun <T, X> get(obj: T, prop: KProperty1<T, X>): X =
    ImmutableObjects.get(obj, prop.toImmutableProp()) as X

fun KProperty1<*, *>.toImmutableProp(): ImmutableProp {
    val javaMethod = getter.javaMethod ?: error("$this does not has java getter")
    return ImmutableType.get(javaMethod.declaringClass).getProp(name)
}