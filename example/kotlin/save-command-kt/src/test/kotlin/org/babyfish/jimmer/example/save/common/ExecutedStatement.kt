package org.babyfish.jimmer.example.save.common

import org.babyfish.jimmer.sql.runtime.DbNull

data class ExecutedStatement private constructor(
    val sql: String,
    val variables: List<Any?>
) {
    constructor(sql: String, vararg variables: Any?) :
        this(
            sql,
            variables.map {
                if (it is DbNull) {
                    null
                } else {
                    it
                }
            }
        )
}