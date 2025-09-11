package org.babyfish.jimmer.benchmark.komapper

import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.dialect.h2.H2Dialect
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcDialects
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class KomapperSql(
    val dataSource: DataSource
) {
    val jdbcDatabase = JdbcDatabase(dataSource = dataSource, dialect = JdbcDialects.get(H2Dialect.driver))

    fun execute() {
        val data = Meta.komapperData
        jdbcDatabase.runQuery {
            QueryDsl.from(data).select()
        }
    }
}