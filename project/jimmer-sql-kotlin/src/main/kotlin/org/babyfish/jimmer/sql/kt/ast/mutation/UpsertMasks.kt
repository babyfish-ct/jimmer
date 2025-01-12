package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.mutation.UpsertMask
import kotlin.reflect.KProperty1

@NewChain
fun <E: Any> UpsertMask<E>.addInsertableProp(
    prop: KProperty1<E, *>
): UpsertMask<E> =
    addInsertableProp(prop.toImmutableProp())

@NewChain
fun <E: Any> UpsertMask<E>.addUpdatableProp(
    prop: KProperty1<E, *>
): UpsertMask<E> =
    addUpdatableProp(prop.toImmutableProp())

@NewChain
fun <E: Any> UpsertMask<E>.addInsertablePath(
    prop: KProperty1<E, *>,
    vararg embeddedProps: KProperty1<*, *>
): UpsertMask<E> =
    addInsertablePath(
        *mutableListOf<ImmutableProp>().apply {
            this += prop.toImmutableProp()
            this += embeddedProps.map { it.toImmutableProp() }
        }.toTypedArray()
    )

@NewChain
fun <E: Any> UpsertMask<E>.addUpdatablePath(
    prop: KProperty1<E, *>,
    vararg embeddedProps: KProperty1<*, *>
): UpsertMask<E> =
    addUpdatablePath(
        *mutableListOf<ImmutableProp>().apply {
            this += prop.toImmutableProp()
            this += embeddedProps.map { it.toImmutableProp() }
        }.toTypedArray()
    )

