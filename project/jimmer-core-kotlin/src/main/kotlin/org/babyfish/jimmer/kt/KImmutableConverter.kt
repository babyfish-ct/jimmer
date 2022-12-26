package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableConverter
import kotlin.reflect.KProperty1

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.map(
    prop: KProperty1<Dynamic, Static>
): ImmutableConverter.Builder<Dynamic, Static> =
    map(prop.toImmutableProp(), prop.name, null)

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.map(
    prop: KProperty1<Dynamic, *>,
    staticProp: KProperty1<Static, *>
): ImmutableConverter.Builder<Dynamic, Static> =
    map(prop.toImmutableProp(), staticProp.name, null)

@Suppress("UNCHECKED_CAST")
fun <Dynamic, Static, DynamicProp> ImmutableConverter.Builder<Dynamic, Static>.map(
    prop: KProperty1<Dynamic, DynamicProp?>,
    block: KtMappingDsl<Static, DynamicProp>.() -> Unit
): ImmutableConverter.Builder<Dynamic, Static> =
    map(prop.toImmutableProp(), prop.name) {
        block(KtMappingDsl(it as ImmutableConverter.Mapping<Static, DynamicProp>))
    }

@Suppress("UNCHECKED_CAST")
fun <Dynamic, Static, DynamicProp, StaticProp> ImmutableConverter.Builder<Dynamic, Static>.map(
    prop: KProperty1<Dynamic, DynamicProp?>,
    staticProp: KProperty1<Static, StaticProp?>,
    block: KtMappingWithConverterDsl<Static, StaticProp, DynamicProp>.() -> Unit
): ImmutableConverter.Builder<Dynamic, Static> =
    map(prop.toImmutableProp(), staticProp.name) {
        block(KtMappingWithConverterDsl(it as ImmutableConverter.Mapping<Static, DynamicProp>))
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
fun <Dynamic, Static, DynamicElement> ImmutableConverter.Builder<Dynamic, Static>.mapList(
    prop: KProperty1<Dynamic, List<DynamicElement>>,
    block: KtListMappingDsl<Static, DynamicElement>.() -> Unit
): ImmutableConverter.Builder<Dynamic, Static> =
    mapList(prop.toImmutableProp(), prop.name) {
        block(KtListMappingDsl(it) as KtListMappingDsl<Static, DynamicElement>)
    }

@Suppress("UNCHECKED_CAST")
fun <Dynamic, Static, DynamicElement, StaticElement> ImmutableConverter.Builder<Dynamic, Static>.mapList(
    prop: KProperty1<Dynamic, List<DynamicElement>>,
    staticProp: KProperty1<Static, List<StaticElement>>,
    block: KtListMappingWithConverterDsl<Static, StaticElement, DynamicElement>.() -> Unit
): ImmutableConverter.Builder<Dynamic, Static> =
    mapList(prop.toImmutableProp(), staticProp.name) {
        block(KtListMappingWithConverterDsl(it as ImmutableConverter.ListMapping<Static, DynamicElement>))
    }

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.unmapStaticProps(
    vararg staticProps: KProperty1<Static, *>
): ImmutableConverter.Builder<Dynamic, Static> =
    unmapStaticProps(staticProps.map { it.name })

fun <Dynamic, Static> ImmutableConverter.Builder<Dynamic, Static>.unmapStaticProps(
    staticProps: Collection<KProperty1<Static, *>>
): ImmutableConverter.Builder<Dynamic, Static> =
    unmapStaticProps(staticProps.map { it.name })

class KtMappingDsl<Static, DynamicProp>(
    private val javaMapping: ImmutableConverter.Mapping<Static, DynamicProp>
) {
    fun useIf(block: Static.() -> Boolean) {
        javaMapping.useIf(block)
    }

    fun defaultValue(defaultValue: DynamicProp) {
        javaMapping.defaultValue(defaultValue)
    }

    fun defaultValue(block: () -> DynamicProp) {
        javaMapping.defaultValue(block)
    }
}

class KtMappingWithConverterDsl<Static, StaticProp, DynamicProp>(
    private val javaMapping: ImmutableConverter.Mapping<Static, DynamicProp>
) {
    fun useIf(block: Static.() -> Boolean) {
        javaMapping.useIf(block)
    }

    @Suppress("UNCHECKED_CAST")
    fun valueConverter(block: (StaticProp) -> DynamicProp) {
        javaMapping.valueConverter { block(it as StaticProp) }
    }

    fun nestedConverter(valueConverter: ImmutableConverter<DynamicProp, StaticProp>) {
        javaMapping.nestedConverter(valueConverter)
    }

    fun defaultValue(defaultValue: DynamicProp) {
        javaMapping.defaultValue(defaultValue)
    }

    fun defaultValue(block: () -> DynamicProp) {
        javaMapping.defaultValue(block)
    }
}

class KtListMappingDsl<Static, DynamicElement>(
    private val javaMapping: ImmutableConverter.ListMapping<Static, DynamicElement>
) {
    fun useIf(block: Static.() -> Boolean) {
        javaMapping.useIf(block)
    }

    fun defaultElement(defaultElement: DynamicElement) {
        javaMapping.defaultElement(defaultElement);
    }

    fun defaultElement(block: () -> DynamicElement) {
        javaMapping.defaultElement(block)
    }
}

class KtListMappingWithConverterDsl<Static, StaticElement, DynamicElement>(
    private val javaMapping: ImmutableConverter.ListMapping<Static, DynamicElement>
) {
    fun useIf(block: Static.() -> Boolean) {
        javaMapping.useIf(block)
    }

    @Suppress("UNCHECKED_CAST")
    fun elementConverter(block: (StaticElement) -> DynamicElement) {
        javaMapping.elementConverter { block(it as StaticElement) }
    }

    fun nestedConverter(elementConverter: ImmutableConverter<DynamicElement, StaticElement>) {
        javaMapping.nestedConverter(elementConverter)
    }

    fun defaultElement(defaultElement: DynamicElement) {
        javaMapping.defaultElement(defaultElement);
    }

    fun defaultElement(block: () -> DynamicElement) {
        javaMapping.defaultElement(block)
    }
}
