package org.babyfish.jimmer.kt

import org.babyfish.jimmer.Draft
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
        builder.map(prop.toImmutableProp(), prop.name)
    }

    fun <X> map(
        prop: KProperty1<T, X?>,
        staticProp: KProperty1<Static, X?>
    ) {
        builder.map(prop.toImmutableProp(), staticProp.name)
    }

    @Suppress("UNCHECKED_CAST")
    fun <X: Any, Y: Any> map(
        prop: KProperty1<T, Y?>,
        staticProp: KProperty1<Static, X?>,
        defaultValueProvider: (() -> Y)? = null,
        valueConverter: (X) -> Y
    ) {
        builder.map(prop.toImmutableProp(), staticProp.name, object : ImmutableConverter.ValueConverter {
            override fun convert(value: Any): Any =
                valueConverter(value as X)
            override fun defaultValue(): Any? =
                defaultValueProvider?.invoke()
        })
    }

    @Suppress("UNCHECKED_CAST")
    fun <X: Any, Y> mapList(
        prop: KProperty1<T, List<Y>>,
        staticProp: KProperty1<Static, List<X>>,
        elementConverter: (X) -> Y
    ) {
        builder.mapList(prop.toImmutableProp(), staticProp.name) {
            elementConverter(it as X)
        }
    }

    fun <X> mapIf(
        cond: (Static) -> Boolean,
        prop: KProperty1<T, X?>,
        staticProp: KProperty1<Static, X?>
    ) {
        builder.mapIf(cond, prop.toImmutableProp(), staticProp.name)
    }

    fun <X> mapIf(
        cond: (Static) -> Boolean,
        prop: KProperty1<T, X?>
    ) {
        builder.mapIf(cond, prop.toImmutableProp(), prop.name)
    }

    @Suppress("UNCHECKED_CAST")
    fun <X: Any, Y: Any> mapIf(
        cond: (Static) -> Boolean,
        prop: KProperty1<T, Y?>,
        staticProp: KProperty1<Static, X?>,
        defaultValueProvider: (() -> Y)? = null,
        valueConverter: (X) -> Y
    ) {
        builder.mapIf(cond, prop.toImmutableProp(), staticProp.name, object : ImmutableConverter.ValueConverter {
            override fun convert(value: Any): Any =
                valueConverter(value as X)
            override fun defaultValue(): Any? =
                defaultValueProvider?.invoke()
        })
    }

    @Suppress("UNCHECKED_CAST")
    fun <X: Any, Y> mapListIf(
        cond: (Static) -> Boolean,
        prop: KProperty1<T, List<Y>>,
        staticProp: KProperty1<Static, List<X>>,
        elementConverter: (X) -> Y
    ) {
        builder.mapListIf(cond, prop.toImmutableProp(), staticProp.name) {
            elementConverter(it as X)
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
}
