package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableConverter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun <T: Any, Static: Any> newImmutableConverter(
    immutableType: KClass<T>,
    staticType: KClass<Static>,
    block: KImmutableConverterDsl<T, Static>.() -> Unit
): ImmutableConverter<T, Static> =
    ImmutableConverter
        .newBuilder(immutableType.java, staticType.java)
        .apply {
            KImmutableConverterDsl(this).block()
        }
        .build()

class KImmutableConverterDsl<T: Any, Static: Any> internal constructor(
    private val builder: ImmutableConverter.Builder<T, Static>
) {
    fun map(prop: KProperty1<T, *>) {
        builder.map(prop.toImmutableProp(), prop.name, null)
    }

    fun map(prop: KProperty1<T, *>, staticProp: KProperty1<Static, *>) {
        builder.map(prop.toImmutableProp(), staticProp.name, null)
    }

    @Suppress("UNCHECKED_CAST")
    fun <Y> map(prop: KProperty1<T, Y?>, block: Mapping<Static, *, Y>.() -> Unit) {
        builder.map(prop.toImmutableProp(), prop.name) {
            block(Mapping(it as ImmutableConverter.Mapping<Static, *, Y>))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <X, Y> map(
        prop: KProperty1<T, Y?>,
        staticProp: KProperty1<Static, X?>,
        block: Mapping<Static, X, Y>.() -> Unit
    ) {
        builder.map(prop.toImmutableProp(), staticProp.name) {
            block(Mapping(it as ImmutableConverter.Mapping<Static, X, Y>))
        }
    }

    fun mapList(prop: KProperty1<T, List<*>>) {
        builder.mapList(prop.toImmutableProp(), prop.name, null)
    }

    fun mapList(prop: KProperty1<T, List<*>>, staticProp: KProperty1<Static, List<*>>) {
        builder.mapList(prop.toImmutableProp(), staticProp.name, null)
    }

    @Suppress("UNCHECKED_CAST")
    fun <Y> mapList(prop: KProperty1<T, List<Y>>, block: ListMapping<Static, *, Y>.() -> Unit) {
        builder.mapList(prop.toImmutableProp(), prop.name) {
            block(ListMapping(it as ImmutableConverter.ListMapping<Static, *, Y>))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <X, Y> mapList(
        prop: KProperty1<T, List<Y>>,
        staticProp: KProperty1<Static, List<X>>,
        block: ListMapping<Static, X, Y>.() -> Unit
    ) {
        builder.mapList(prop.toImmutableProp(), staticProp.name) {
            block(ListMapping(it as ImmutableConverter.ListMapping<Static, X, Y>))
        }
    }

    fun unmap(vararg props: KProperty1<T, *>) {
        builder.unmap(*props.map { it.toImmutableProp() }.toTypedArray())
    }

    fun autoMapOtherScalars(partial: Boolean = false) {
        builder.autoMapOtherScalars(partial)
    }

    @Suppress("UNCHECKED_CAST")
    fun <D> setDraftModifier(block: D.(Static) -> Unit) {
        builder.setDraftModifier { draft, staticObj ->
            (draft as D).block(staticObj)
        }
    }

    class Mapping<Static, X, Y>(
        private val javaMapping: ImmutableConverter.Mapping<Static, X, Y>
    ) {
        fun useIf(cond: (Static) -> Boolean) {
            javaMapping.useIf(cond)
        }

        fun valueConverter(converter: (X) -> Y) {
            javaMapping.valueConverter(converter)
        }

        fun immutableValueConverter(converter: ImmutableConverter<Y, X>) {
            javaMapping.immutableValueConverter(converter)
        }

        fun defaultValue(defaultValue: Y) {
            javaMapping.defaultValue(defaultValue)
        }

        fun defaultValue(defaultValueSupplier: () -> Y) {
            javaMapping.defaultValue(defaultValueSupplier)
        }
    }

    class ListMapping<Static, X, Y>(
        private val javaMapping: ImmutableConverter.ListMapping<Static, X, Y>
    ) {
        fun useIf(cond: (Static) -> Boolean) {
            javaMapping.useIf(cond)
        }

        fun elementConverter(converter: (X) -> Y) {
            javaMapping.elementConverter(converter)
        }

        fun immutableValueConverter(converter: ImmutableConverter<Y, X>) {
            javaMapping.immutableValueConverter(converter)
        }

        fun defaultElement(defaultValue: Y) {
            javaMapping.defaultElement(defaultValue)
        }

        fun defaultElement(defaultValueSupplier: () -> Y) {
            javaMapping.defaultElement(defaultValueSupplier)
        }
    }
}
