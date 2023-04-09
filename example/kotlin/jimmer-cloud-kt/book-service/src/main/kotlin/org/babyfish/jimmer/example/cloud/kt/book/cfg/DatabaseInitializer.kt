package org.babyfish.jimmer.example.cloud.kt.book.cfg

import org.babyfish.jimmer.spring.cfg.JimmerCustomizer
import org.babyfish.jimmer.sql.JSqlClient
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import javax.sql.DataSource

@Component
class DatabaseInitializer(private val dataSource: DataSource) : JimmerCustomizer {

    override fun customize(builder: JSqlClient.Builder) {
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