package org.babyfish.jimmer.sql.kt.event

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.lang.Ref
import org.babyfish.jimmer.sql.event.ChangedRef
import org.babyfish.jimmer.sql.event.EntityEvent
import kotlin.reflect.KProperty1

fun <R> EntityEvent<*>.getUnchangedRef(prop: KProperty1<*, R>): Ref<R>? =
    getUnchangedRef(prop.toImmutableProp())

fun <R> EntityEvent<*>.getChangedRef(prop: KProperty1<*, R>): ChangedRef<R> ?=
    getChangedRef(prop.toImmutableProp())

fun EntityEvent<*>.isChanged(prop: KProperty1<*, *>): Boolean =
    isChanged(prop.toImmutableProp())

fun <R> EntityEvent<*>.getUnchangedValue(prop: KProperty1<*, R>): R =
    getUnchangedValue(prop.toImmutableProp())