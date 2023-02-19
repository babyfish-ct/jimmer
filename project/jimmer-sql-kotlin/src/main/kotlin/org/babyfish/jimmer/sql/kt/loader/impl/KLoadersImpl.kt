package org.babyfish.jimmer.sql.kt.loader.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.TargetLevel
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.kt.loader.KListLoader
import org.babyfish.jimmer.sql.kt.loader.KLoaders
import org.babyfish.jimmer.sql.kt.loader.KReferenceLoader
import org.babyfish.jimmer.sql.kt.loader.KValueLoader
import org.babyfish.jimmer.sql.loader.graphql.impl.LoadersImpl
import kotlin.reflect.KProperty1

internal class KLoadersImpl(
    private val javaLoaders: LoadersImpl
) : KLoaders {

    @Suppress("UNCHECKED_CAST")
    override fun <S : Any, T : Any> batchLoad(
        prop: KProperty1<S, T?>,
        sources: Collection<S>
    ): Map<S, T> =
        prop.toImmutableProp().let {
            when {
                it.isReferenceList(TargetLevel.ENTITY) ->
                    javaLoaders.list<S, T, Table<T>>(it).batchLoad(sources)
                it.isReference(TargetLevel.ENTITY) ->
                    javaLoaders.reference<S, Any, Table<Any>>(it).batchLoad(sources)
                else ->
                    javaLoaders.value<S, T>(it).batchLoad(sources)
            }
        } as Map<S, T>

    override fun <S : Any, T : Any> value(prop: KProperty1<S, T>): KValueLoader<S, T> =
        javaLoaders.value<S, T>(
            prop.toImmutableProp()
        ).let {
            KValueLoaderImpl(it)
        }

    override fun <S : Any, T : Any> reference(prop: KProperty1<S, T?>): KReferenceLoader<S, T> =
        javaLoaders
            .reference<S, T, Table<T>>(
                prop.toImmutableProp()
            )
            .let {
                KReferenceLoaderImpl(it)
            }

    override fun <S : Any, T : Any> list(prop: KProperty1<S, List<T>>): KListLoader<S, T> =
        javaLoaders
            .list<S, T, Table<T>>(
                prop.toImmutableProp()
            )
            .let {
                KListLoaderImpl(it)
            }
}