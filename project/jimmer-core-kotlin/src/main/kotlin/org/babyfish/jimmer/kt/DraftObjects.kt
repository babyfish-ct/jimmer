package org.babyfish.jimmer.kt

import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.DraftObjects
import kotlin.reflect.KProperty1

fun <T: Draft, X> set(draft: T, prop: KProperty1<T, X>, value: X) {
    DraftObjects.set(draft, prop.toImmutableProp(), value)
}

fun <T: Draft> unload(draft: T, prop: KProperty1<T, *>) {
    DraftObjects.unload(draft, prop.toImmutableProp())
}

fun <T: Draft> show(draft: T, prop: KProperty1<T, *>) {
    DraftObjects.show(draft, prop.toImmutableProp())
}

fun <T: Draft> hide(draft: T, prop: KProperty1<T, *>) {
    DraftObjects.hide(draft, prop.toImmutableProp())
}
