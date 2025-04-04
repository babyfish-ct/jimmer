package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleEntitySaveCommand
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult
import java.sql.Connection
import kotlin.reflect.KClass

internal class KSimpleEntitySaveCommandImpl<E: Any>(
    private val javaCommand: SimpleEntitySaveCommand<E>
): KSimpleEntitySaveCommand<E> {

    override fun execute(): KSimpleSaveResult<E> =
        KSimpleSaveResultImpl(javaCommand.execute())

    override fun execute(fetcher: Fetcher<E>?): KSimpleSaveResult<E> =
        KSimpleSaveResultImpl(javaCommand.execute(fetcher))

    override fun <V : View<E>> execute(viewType: KClass<V>): KSimpleSaveResult.View<E, V> =
        KSimpleSaveResultImpl.ViewImpl(javaCommand.execute(viewType.java))

    override fun execute(con: Connection?): KSimpleSaveResult<E> =
        KSimpleSaveResultImpl(javaCommand.execute(con))

    override fun execute(con: Connection?, fetcher: Fetcher<E>?): KSimpleSaveResult<E> =
        KSimpleSaveResultImpl(javaCommand.execute(con, fetcher))

    override fun <V : View<E>> execute(con: Connection?, viewType: KClass<V>): KSimpleSaveResult.View<E, V> =
        KSimpleSaveResultImpl.ViewImpl(javaCommand.execute(con, viewType.java))
}