package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.sql.ast.Executable
import org.babyfish.jimmer.sql.kt.KExecutable
import java.sql.Connection

internal class KExecutableImpl<R>(
    private val javaExecutable: Executable<R>
) : KExecutable<R> {

    override fun execute(con: Connection?): R =
        if (con === null) {
            javaExecutable.execute()
        } else {
            javaExecutable.execute(con)
        }
}