package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun <T: Any> isLoaded(obj: T, prop: KProperty1<T, *>): Boolean =
    ImmutableObjects.isLoaded(obj, prop.toImmutableProp())

@Suppress("UNCHECKED_CAST")
fun <T: Any, X> get(obj: T, prop: KProperty1<T, X>): X =
    ImmutableObjects.get(obj, prop.toImmutableProp()) as X

inline fun <reified T: Any> makeNullableIdOnly(id: Any?): T? =
    ImmutableObjects.makeIdOnly(ImmutableType.get(T::class.java), id)

fun <T: Any> makeNullableIdOnly(type: KClass<T>, id: Any?): T? =
    ImmutableObjects.makeIdOnly(ImmutableType.get(type.java), id)

inline fun <reified T: Any> makeIdOnly(id: Any): T =
    ImmutableObjects.makeIdOnly(ImmutableType.get(T::class.java), id) ?: error("Internal bug")

fun <T: Any> makeIdOnly(type: KClass<T>, id: Any): T =
    ImmutableObjects.makeIdOnly(ImmutableType.get(type.java), id) ?: error("Internal bug")

fun KProperty1<*, *>.toImmutableProp(): ImmutableProp {
    val immutableType = ImmutableType.get(
        (parameters[0].type.classifier as KClass<*>).java
    )
    return immutableType.getProp(name)
}