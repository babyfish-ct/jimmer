package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.dialect.DefaultDialect
import org.babyfish.jimmer.sql.dialect.Dialect
import org.babyfish.jimmer.sql.runtime.ConnectionManager
import org.babyfish.jimmer.sql.runtime.DefaultExecutor
import org.babyfish.jimmer.sql.runtime.Executor

interface KSqlClient {

    companion object {

        fun create(block: DSL.() -> Unit): KSqlClient {
            TODO()
        }
    }

    class DSL internal constructor() {

        var connectionManager: ConnectionManager? = null

        var dialect: Dialect = DefaultDialect()

        var executor: Executor = DefaultExecutor()
    }
}