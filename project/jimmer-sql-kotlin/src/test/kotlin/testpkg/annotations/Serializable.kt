package testpkg.annotations

import kotlin.reflect.KClass

// Like `kotlinx.serialization.Serializable`
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Serializable(val with: KClass<*> = Unit::class)