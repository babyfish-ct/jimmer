package org.babyfish.jimmer.kt

import org.babyfish.jimmer.DraftObjects
import kotlin.reflect.KProperty1

fun <T, X> set(draft: T, prop: KProperty1<T, X>, value: X) {
    DraftObjects.set(draft, prop.toImmutableProp(), value)
}

fun <T> unload(draft: T, prop: KProperty1<T, *>) {
    DraftObjects.unload(draft, prop.toImmutableProp())
}