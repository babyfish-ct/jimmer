package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.BatchEntitySaveCommand
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchEntitySaveCommand
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult
import java.sql.Connection
import kotlin.reflect.KClass

internal class KBatchEntitySaveCommandImpl<E: Any>(
    private val javaCommand: BatchEntitySaveCommand<E>
): KBatchEntitySaveCommand<E> {

    override fun execute(): KBatchSaveResult<E> =
        KBatchSaveResultImpl(javaCommand.execute())

    override fun execute(fetcher: Fetcher<E>?): KBatchSaveResult<E> =
        KBatchSaveResultImpl(javaCommand.execute(fetcher))

    override fun <V : View<E>> execute(viewType: KClass<V>): KBatchSaveResult.View<E, V> =
        KBatchSaveResultImpl.ViewImpl(javaCommand.execute(viewType.java))

    override fun execute(con: Connection?): KBatchSaveResult<E> =
        KBatchSaveResultImpl(javaCommand.execute(con))

    override fun execute(con: Connection?, fetcher: Fetcher<E>?): KBatchSaveResult<E> =
        KBatchSaveResultImpl(javaCommand.execute(con, fetcher))

    override fun <V : View<E>> execute(con: Connection?, viewType: KClass<V>): KBatchSaveResult.View<E, V> =
        KBatchSaveResultImpl.ViewImpl(javaCommand.execute(con, viewType.java))
}