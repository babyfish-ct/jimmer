package org.babyfish.jimmer.example.cloud.kt.book.cfg

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KInitializer
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import javax.sql.DataSource

@Component
class DatabaseInitializer(private val dataSource: DataSource) : KInitializer {

    override fun initialize(dsl: KSqlClient) {
        dataSource.connection.use { con ->
            val inputStream = JimmerConfig::class.java
                .classLoader
                .getResourceAsStream("book.sql") ?: throw RuntimeException("no `book.sql`")
            InputStreamReader(inputStream).use { reader ->
                con.createStatement().execute(reader.readText())
            }
        }
    }
}