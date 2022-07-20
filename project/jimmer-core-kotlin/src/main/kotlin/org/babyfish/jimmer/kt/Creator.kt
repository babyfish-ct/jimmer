package org.babyfish.jimmer.kt

import kotlin.reflect.KClass

@JvmInline
value class ImmutableCreator<T: Any> internal constructor(val type: KClass<T>)

fun <T: Any> new(type: KClass<T>): ImmutableCreator<T> =
    ImmutableCreator(type)