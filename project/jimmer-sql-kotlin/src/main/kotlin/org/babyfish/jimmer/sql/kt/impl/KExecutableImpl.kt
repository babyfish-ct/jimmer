package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.sql.ast.Executable
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import java.sql.Connection

internal class KExecutableImpl<R>(
    private val javaExecutable: Executable<R>
) : KExecutable<R> {

    override fun execute(con: Connection?): R =
        javaExecutable.execute(con)
}