package org.babyfish.jimmer.sql.kt.ast

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.ast.JdbcConfigurable

@DslScope
class KJdbcOptionsDsl internal constructor(
    private val target: JdbcConfigurable<*>
) {

    var fetchSize: Int? = null

    var queryTimeout: Int? = null

    internal fun apply() {
        fetchSize?.let(target::jdbcFetchSize)
        queryTimeout?.let(target::jdbcQueryTimeout)
    }
}
