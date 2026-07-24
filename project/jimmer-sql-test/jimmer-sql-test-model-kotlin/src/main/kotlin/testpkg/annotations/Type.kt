package testpkg.annotations

import kotlin.reflect.KClass

annotation class Type(
    val value: KClass<*>
)