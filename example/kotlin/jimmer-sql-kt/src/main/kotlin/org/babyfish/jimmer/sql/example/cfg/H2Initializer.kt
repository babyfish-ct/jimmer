package org.babyfish.jimmer.sql.example.cfg

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KInitializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import java.sql.Connection
import javax.sql.DataSource

/*
 * Initialize H2 in-memory database if the application is started by default profile.
 */
@Component
class H2Initializer(
    private val dataSource: DataSource,
    @Value("\${spring.datasource.url}") private val url: String
) : KInitializer {

    override fun initialize(dsl: KSqlClient) {
        if (url.startsWith("jdbc:h2:")) {
            initH2()
        }
    }

    private fun initH2() {
        dataSource.connection.use { con: Connection ->
            val inputStream = H2Initializer::class.java
                .classLoader
                .getResourceAsStream("h2-database.sql") ?: throw RuntimeException("no h2-database.sql")
            val sql = InputStreamReader(inputStream).use { reader ->
                reader.readText()
            }
            con.createStatement().execute(sql)
        }
    }
}