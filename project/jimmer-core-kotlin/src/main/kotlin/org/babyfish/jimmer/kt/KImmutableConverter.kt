package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableConverter
import kotlin.reflect.KProperty1

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.map(
    prop: KProperty1<Dynamic, Static>
): ImmutableConverter.Builder<Dynamic, Static> =
    map(prop.toImmutableProp(), prop.name, null)

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.map(
    prop: KProperty1<Dynamic, *>, staticProp: KProperty1<Static, *>
): ImmutableConverter.Builder<Dynamic, Static> =
    map(prop.toImmutableProp(), staticProp.name, null)

@Suppress("UNCHECKED_CAST")
fun <Dynamic, Static, DynamicProp> ImmutableConverter.Builder<Dynamic, Static>.map(
    prop: KProperty1<Dynamic, DynamicProp?>,
    block: ImmutableConverter.Mapping<Static, DynamicProp>.() -> Unit
): ImmutableConverter.Builder<Dynamic, Static> =
    map(prop.toImmutableProp(), prop.name) {
        block(it as ImmutableConverter.Mapping<Static, DynamicProp>)
    }

@Suppress("UNCHECKED_CAST")
fun <Dynamic, Static, DynamicProp> ImmutableConverter.Builder<Dynamic, Static>.map(
    prop: KProperty1<Dynamic, DynamicProp?>,
    staticProp: KProperty1<Static, Any?>,
    block: ImmutableConverter.Mapping<Static, DynamicProp>.() -> Unit
): ImmutableConverter.Builder<Dynamic, Static> =
    map(prop.toImmutableProp(), staticProp.name) {
        block(it as ImmutableConverter.Mapping<Static, DynamicProp>)
    }

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.mapList(
    prop: KProperty1<Dynamic, List<*>>
): ImmutableConverter.Builder<Dynamic, Static> =
    mapList(prop.toImmutableProp(), prop.name, null)

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.mapList(
    prop: KProperty1<Dynamic, List<*>>,
    staticProp: KProperty1<Static, List<*>>
): ImmutableConverter.Builder<Dynamic, Static> =
    mapList(prop.toImmutableProp(), staticProp.name, null)

@Suppress("UNCHECKED_CAST")
fun <Dynamic, Static, DynamicProp> ImmutableConverter.Builder<Dynamic, Static>.mapList(
    prop: KProperty1<Dynamic, List<DynamicProp>>, 
    block: ImmutableConverter.ListMapping<Static, DynamicProp>.() -> Unit
): ImmutableConverter.Builder<Dynamic, Static> =
    mapList(prop.toImmutableProp(), prop.name) {
        block(it as ImmutableConverter.ListMapping<Static, DynamicProp>)
    }

@Suppress("UNCHECKED_CAST")
fun <Dynamic, Static, DynamicProp> ImmutableConverter.Builder<Dynamic, Static>.mapList(
    prop: KProperty1<Dynamic, List<DynamicProp>>,
    staticProp: KProperty1<Static, List<Any>>,
    block: ImmutableConverter.ListMapping<Static, DynamicProp>.() -> Unit
): ImmutableConverter.Builder<Dynamic, Static> =
    mapList(prop.toImmutableProp(), staticProp.name) {
        block(it as ImmutableConverter.ListMapping<Static, DynamicProp>)
    }

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.unmapStaticProps(
    vararg staticProps: KProperty1<Static, *>
): ImmutableConverter.Builder<Dynamic, Static> =
    unmapStaticProps(staticProps.map { it.name })

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.unmapStaticProps(
    staticProps: Collection<KProperty1<Static, *>>
): ImmutableConverter.Builder<Dynamic, Static> =
    unmapStaticProps(staticProps.map { it.name })

