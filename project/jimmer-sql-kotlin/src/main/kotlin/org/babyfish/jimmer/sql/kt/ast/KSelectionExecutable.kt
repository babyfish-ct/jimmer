package org.babyfish.jimmer.sql.kt.ast

import java.sql.Connection
import java.util.stream.Stream

interface KSelectionExecutable<R> : KExecutable<List<R>> {

    fun stream(con: Connection? = null): Stream<R>
}
