@file:JvmName("ImmutableFactory")
package org.babyfish.jimmer.mapstruct

import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.runtime.Internal
import java.util.function.BiConsumer
import kotlin.reflect.KClass

inline fun <S: Any, reified T: Any, D: Draft> byDto(source: S, noinline draftFiller: (S, D) -> Unit): T =
    byDto(source, T::class, draftFiller)

@Suppress("UNCHECKED_CAST")
fun <S: Any, T: Any, D: Draft> byDto(source: S, targetType: KClass<T>, draftFiller: (S, D) -> Unit): T =
    Internal.produce(ImmutableType.get(targetType.java), null) { draft ->
        draftFiller(source, draft as D)
    } as T

@Suppress("UNCHECKED_CAST")
fun <S, T: Any, D: Draft> byDto(source: S, targetType: Class<T>, draftFiller: BiConsumer<S, D>): T =
    Internal.produce(ImmutableType.get(targetType), null) { draft ->
        draftFiller.accept(source, draft as D)
    } as T

inline fun <ID, reified T: Any> byId(id: ID?): T =
    byId(id, T::class)

fun <ID, T: Any> byId(id: ID, targetType: KClass<T>): T =
    byId(id, targetType.java)

fun <ID, T: Any> byId(id: ID, targetType: Class<T>): T =
    ImmutableObjects.makeIdOnly(targetType, id)!!
