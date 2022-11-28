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

interface KNonNullImmutableConverter<T: Any, Static: Any> {
    fun convert(staticObj: Static): T
}

fun <T: Any, Static: Any> ImmutableConverter<T, Static>.toNonNull() =
    object: KNonNullImmutableConverter<T, Static> {
        override fun convert(staticObj: Static): T =
            this@toNonNull.convert(staticObj)
                ?: throw IllegalArgumentException("staticObj cannot be null")
    }

class KImmutableConverterDsl<T: Any, Static: Any> internal constructor(
    private val builder: ImmutableConverter.Builder<T, Static>
) {

    fun <X> map(
        prop: KProperty1<T, X?>,
        staticProp: KProperty1<T, X?>
    ) {
        builder.map(prop.toImmutableProp(), staticProp.name)
    }

    @Suppress("UNCHECKED_CAST")
    fun <X, Y> map(
        prop: KProperty1<T, Y?>,
        staticProp: KProperty1<Static, X>,
        valueConverter: (X) -> Y
    ) {
        builder.map(prop.toImmutableProp(), staticProp.name) {
            valueConverter(it as X)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <X, Y> mapList(
        prop: KProperty1<T, List<Y>>,
        staticProp: KProperty1<Static, List<X>>,
        elementConverter: (X) -> Y
    ) {
        builder.mapList(prop.toImmutableProp(), staticProp.name) {
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
