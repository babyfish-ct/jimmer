package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.fetcher.Fetcher
import java.sql.Connection
import kotlin.reflect.KClass

interface KSimpleEntitySaveCommand<E: Any> {

    fun execute(): KSimpleSaveResult<E>

    fun execute(fetcher: Fetcher<E>): KSimpleSaveResult<E>

    fun <V: View<E>> execute(viewType: KClass<V>): KSimpleSaveResult.View<E, V>

    fun execute(con: Connection?): KSimpleSaveResult<E>

    fun execute(con: Connection?, fetcher: Fetcher<E>): KSimpleSaveResult<E>

    fun <V: View<E>> execute(con: Connection?, viewType: KClass<V>): KSimpleSaveResult.View<E, V>
}