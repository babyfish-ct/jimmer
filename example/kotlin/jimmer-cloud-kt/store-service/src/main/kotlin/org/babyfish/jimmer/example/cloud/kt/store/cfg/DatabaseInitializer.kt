package org.babyfish.jimmer.example.cloud.kt.store.cfg

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KInitializer
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import javax.sql.DataSource

@Component
class DatabaseInitializer(private val dataSource: DataSource) : KInitializer {

    override fun initialize(dsl: KSqlClient) {
        dataSource.connection.use { con ->
            val inputStream = DatabaseInitializer::class.java
                .classLoader
                .getResourceAsStream("store.sql") ?: throw RuntimeException("no `store.sql`")
            InputStreamReader(inputStream).use { reader ->
                con.createStatement().execute(reader.readText())
            }
        }
    }
}