package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.fetcher.Fetcher
import java.sql.Connection
import kotlin.reflect.KClass

interface KBatchEntitySaveCommand<E: Any> {

    fun execute(): KBatchSaveResult<E>

    fun execute(fetcher: Fetcher<E>?): KBatchSaveResult<E>

    fun <V: View<E>> execute(viewType: KClass<V>): KBatchSaveResult.View<E, V>

    fun execute(con: Connection?): KBatchSaveResult<E>

    fun execute(con: Connection?, fetcher: Fetcher<E>?): KBatchSaveResult<E>

    fun <V: View<E>> execute(con: Connection?, viewType: KClass<V>): KBatchSaveResult.View<E, V>
}