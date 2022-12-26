package org.babyfish.jimmer.example.kt.sql.cfg

import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.sql.dialect.Dialect
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.dialect.MySqlDialect
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.InputStreamReader
import java.sql.Connection
import javax.sql.DataSource

@Configuration
class JimmerConfig {

    /*
     * 1. Jimmer requires this entity manager,
     *    if you do not want to directly create JSqlClient/KSqlClient
     *
     * 2. Kotlin requires `jimmer.language=kotlin` in `application.yml`
     */
    @Bean
    fun entityManager(): EntityManager =
        ENTITY_MANAGER

    /*
     * This bean is used to determine dialect at runtime.
     *
     * If this runtime determination is unnecessary,
     * please remove this bean and directly configure
     * `jimmer.dialect` in `application.yml` or `application.properties`
     */
    @Bean
    fun dialect(
        @Value("\${spring.datasource.url}") jdbcUrl: String,
        dataSource: DataSource
    ): Dialect =
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            initializeH2Database(dataSource)
            H2Dialect()
        } else {
            MySqlDialect()
        }

    private fun initializeH2Database(dataSource: DataSource) {
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