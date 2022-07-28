package org.babyfish.jimmer.example.kt.sql.cfg

import org.babyfish.jimmer.example.kt.sql.model.Gender
import org.babyfish.jimmer.sql.SqlClient
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
import java.sql.PreparedStatement
import java.util.function.Function
import javax.sql.DataSource

@Configuration
class SqlClientConfig {

    private val LOGGER = LoggerFactory.getLogger(SqlClientConfig::class.java)

    @Bean
    fun sqlClient(dataSource: DataSource): KSqlClient =
        newKSqlClient {

            /*
             * It's very important to use
             *      "org.springframework.jdbc.datasource.DataSourceUtils"!
             * This is spring transaction aware ConnectionManager
             */
            connectionManager = object : ConnectionManager {
                override fun <R> execute(block: Function<Connection, R>): R {
                    val con: Connection = DataSourceUtils.getConnection(dataSource)
                    return try {
                        block.apply(con)
                    } finally {
                        DataSourceUtils.releaseConnection(con, dataSource)
                    }
                }
            }

            dialect = H2Dialect()

            /*
             * Log SQL and variables
             */
            executor = object : Executor {
                override fun <R> execute(
                    con: Connection,
                    sql: String,
                    variables: List<Any>,
                    block: SqlFunction<PreparedStatement, R>
                ): R {
                    LOGGER.info("Execute sql : \"{}\", with variables: {}", sql, variables)
                    return DefaultExecutor.INSTANCE.execute(
                        con,
                        sql,
                        variables,
                        block
                    )
                }
            }

            scalarProviders {
                add(
                    ScalarProvider.enumProviderByString(
                        Gender::class.java
                    ) {
                        it
                            .map(Gender.MALE, "M")
                            .map(Gender.FEMALE, "F")
                    }
                )
            }
        }.also {
            initializeH2Database(it)
        }

    private fun initializeH2Database(sqlClient: KSqlClient) {
        sqlClient.javaClient.connectionManager.execute { con: Connection ->
            val inputStream = SqlClientConfig::class.java
                .classLoader
                .getResourceAsStream("h2-database.sql") ?: throw RuntimeException("no h2-database.sql")
            val sql = InputStreamReader(inputStream).use { reader ->
                reader.readText()
            }
            con.createStatement().execute(sql)
            null
        }
    }
}