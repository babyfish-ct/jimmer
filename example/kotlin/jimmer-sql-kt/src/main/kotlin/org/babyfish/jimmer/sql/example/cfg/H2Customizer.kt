package org.babyfish.jimmer.sql.example.cfg

import org.babyfish.jimmer.spring.cfg.JimmerCustomizer
import org.babyfish.jimmer.sql.JSqlClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import java.sql.Connection
import javax.sql.DataSource

/*
 * Initialize H2 in-memory database if the application is started by default profile.
 *
 * This class must implement `JimmerCustomizer`(before the sql client is created),
 * not JimmerInitializer(after the sql client is created),
 * because `jimmer.database-validation-mode` in `application.yml` validates database
 * before the sql client is created.
 */
@Component
class H2Customizer(
    private val dataSource: DataSource,
    @Value("\${spring.datasource.url}") private val url: String
) : JimmerCustomizer {

    override fun customize(builder: JSqlClient.Builder) {
        if (url.startsWith("jdbc:h2:")) {
            initH2()
        }
    }

    private fun initH2() {
        dataSource.connection.use { con: Connection ->
            val inputStream = JimmerConfig::class.java
                .classLoader
                .getResourceAsStream("h2-database.sql") ?: throw RuntimeException("no h2-database.sql")
            val sql = InputStreamReader(inputStream).use { reader ->
                reader.readText()
            }
            con.createStatement().execute(sql)
        }
    }
}