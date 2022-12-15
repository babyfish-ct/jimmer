package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableConverter
import kotlin.reflect.KProperty1

fun <T, Static> ImmutableConverter.Builder<T, Static>.map(
    prop: KProperty1<T, Static>
): ImmutableConverter.Builder<T, Static> =
    map(prop.toImmutableProp(), prop.name, null)

fun <T, Static> ImmutableConverter.Builder<T, Static>.map(
    prop: KProperty1<T, *>, staticProp: KProperty1<Static, *>
): ImmutableConverter.Builder<T, Static> =
    map(prop.toImmutableProp(), staticProp.name, null)

@Suppress("UNCHECKED_CAST")
fun <T, Static, Y> ImmutableConverter.Builder<T, Static>.map(
    prop: KProperty1<T, Y?>,
    block: ImmutableConverter.Mapping<Static, *, Y>.() -> Unit
): ImmutableConverter.Builder<T, Static> =
    map(prop.toImmutableProp(), prop.name) {
        block(it as ImmutableConverter.Mapping<Static, *, Y>)
    }

@Suppress("UNCHECKED_CAST")
fun <T, Static, X, Y> ImmutableConverter.Builder<T, Static>.map(
    prop: KProperty1<T, Y?>,
    staticProp: KProperty1<Static, X?>,
    block: ImmutableConverter.Mapping<Static, X, Y>.() -> Unit
): ImmutableConverter.Builder<T, Static> =
    map(prop.toImmutableProp(), staticProp.name) {
        block(it as ImmutableConverter.Mapping<Static, X, Y>)
    }

fun <T, Static> ImmutableConverter.Builder<T, Static>.mapList(
    prop: KProperty1<T, List<*>>
): ImmutableConverter.Builder<T, Static> =
    mapList(prop.toImmutableProp(), prop.name, null)

fun <T, Static> ImmutableConverter.Builder<T, Static>.mapList(
    prop: KProperty1<T, List<*>>,
    staticProp: KProperty1<Static, List<*>>
): ImmutableConverter.Builder<T, Static> =
    mapList(prop.toImmutableProp(), staticProp.name, null)

@Suppress("UNCHECKED_CAST")
fun <T, Static, Y> ImmutableConverter.Builder<T, Static>.mapList(
    prop: KProperty1<T, List<Y>>, 
    block: ImmutableConverter.ListMapping<Static, *, Y>.() -> Unit
): ImmutableConverter.Builder<T, Static> =
    mapList(prop.toImmutableProp(), prop.name) {
        block(it as ImmutableConverter.ListMapping<Static, *, Y>)
    }

@Suppress("UNCHECKED_CAST")
fun <T, Static, X, Y> ImmutableConverter.Builder<T, Static>.mapList(
    prop: KProperty1<T, List<Y>>,
    staticProp: KProperty1<Static, List<X>>,
    block: ImmutableConverter.ListMapping<Static, X, Y>.() -> Unit
): ImmutableConverter.Builder<T, Static> =
    mapList(prop.toImmutableProp(), staticProp.name) {
        block(it as ImmutableConverter.ListMapping<Static, X, Y>)
    }

fun <T, Static> ImmutableConverter.Builder<T, Static>.unmapStaticProps(
    vararg staticProps: KProperty1<Static, *>
): ImmutableConverter.Builder<T, Static> =
    unmapStaticProps(staticProps.map { it.name })

fun <T, Static> ImmutableConverter.Builder<T, Static>.unmapStaticProps(
    staticProps: Collection<KProperty1<Static, *>>
): ImmutableConverter.Builder<T, Static> =
    unmapStaticProps(staticProps.map { it.name })

