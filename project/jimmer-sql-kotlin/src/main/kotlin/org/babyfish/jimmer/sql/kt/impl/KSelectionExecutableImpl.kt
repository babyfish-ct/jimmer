package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.sql.ast.SelectionExecutable
import org.babyfish.jimmer.sql.kt.ast.KSelectionExecutable
import java.sql.Connection
import java.util.stream.Stream

internal class KSelectionExecutableImpl<R>(
    private val javaExecutable: SelectionExecutable<R>
) : KSelectionExecutable<R> {

    override fun execute(con: Connection?): List<R> =
        javaExecutable.execute(con)

    override fun stream(con: Connection?): Stream<R> =
        javaExecutable.stream(con)
}
