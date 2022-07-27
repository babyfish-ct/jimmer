package org.babyfish.jimmer.sql.kt

import java.sql.Connection

interface KExecutable<R> {
    fun execute(con: Connection? = null): R
}