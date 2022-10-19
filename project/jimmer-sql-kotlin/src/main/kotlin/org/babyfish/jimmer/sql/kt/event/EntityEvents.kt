package org.babyfish.jimmer.sql.kt.event

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.lang.Ref
import org.babyfish.jimmer.sql.event.ChangedRef
import org.babyfish.jimmer.sql.event.EntityEvent
import kotlin.reflect.KProperty1

fun <R> EntityEvent<*>.getUnchangedFieldRef(prop: KProperty1<*, R>): Ref<R>? =
    getUnchangedFieldRef(prop.toImmutableProp().id)

fun <R> EntityEvent<*>.getChangedFieldRef(prop: KProperty1<*, R>): ChangedRef<R> ?=
    getChangedFieldRef(prop.toImmutableProp().id)