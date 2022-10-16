package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.meta.impl.RedirectedProp
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaMethod

fun <T> isLoaded(obj: T, prop: KProperty1<T, *>): Boolean =
    ImmutableObjects.isLoaded(obj, prop.toImmutableProp())

@SuppressWarnings("unchecked")
fun <T, X> get(obj: T, prop: KProperty1<T, X>): X =
    ImmutableObjects.get(obj, prop.toImmutableProp()) as X

fun KProperty1<*, *>.toImmutableProp(): ImmutableProp {
    val javaMethod = getter.javaMethod ?: error("$this does not has java getter")
    val immutableType = ImmutableType.get(javaMethod.declaringClass)
    return immutableType.getProp(name)!!.let {
        RedirectedProp.source(it, immutableType)
    }
}