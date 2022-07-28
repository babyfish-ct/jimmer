package org.babyfish.jimmer.example.kt.graphql.cfg

import org.babyfish.jimmer.example.kt.graphql.entities.Gender
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.babyfish.jimmer.sql.runtime.*
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceUtils
import java.io.InputStreamReader
import java.sql.Connection
import javax.sql.DataSource

@Configuration
class SqlClientConfig {

    private val LOGGER = LoggerFactory.getLogger(SqlClientConfig::class.java)

    @Bean
    fun sqlClient(dataSource: DataSource): KSqlClient =
        newKSqlClient {

            setConnectionManager {
                /*
                 * It's very important to use
                 *      "org.springframework.jdbc.datasource.DataSourceUtils"!
                 * This is spring transaction aware ConnectionManager
                 */
                val con: Connection = DataSourceUtils.getConnection(dataSource)
                try {
                    proceed(con)
                } finally {
                    DataSourceUtils.releaseConnection(con, dataSource)
                }
            }

            setExecutor {
                /*
                 * Log SQL and variables
                 */
                LOGGER.info("Execute sql : \"{}\", with variables: {}", sql, variables)
                proceed()
            }

            setDialect(H2Dialect())

            addScalarProvider(
                ScalarProvider.enumProviderByString(
                    Gender::class.java
                ) {
                    it
                        .map(Gender.MALE, "M")
                        .map(Gender.FEMALE, "F")
                }
            )
        }.also {
            initializeH2Database(it)
        }

    private fun initializeH2Database(sqlClient: KSqlClient) {
        sqlClient.executeNativeSql { con: Connection ->
            val inputStream = SqlClientConfig::class.java
                .classLoader
                .getResourceAsStream("h2-database.sql") ?: throw RuntimeException("no h2-database.sql")
            val sql = InputStreamReader(inputStream).use { reader ->
                reader.readText()
            }
            con.createStatement().execute(sql)
        }
    }
}