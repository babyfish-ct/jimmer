package org.babyfish.jimmer.example.kt.sql.cfg

import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.cache.CacheFactory
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.dialect.MySqlDialect
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.babyfish.jimmer.sql.runtime.ScalarProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    fun sqlClient(
        dataSource: DataSource,
        @Value("\${spring.datasource.url}") jdbcUrl: String,
        interceptors: List<DraftInterceptor<*>>,
        filters: List<KFilter<*>>,
        cacheFactory: CacheFactory? // Optional dependency
    ): KSqlClient {
        val isH2 = jdbcUrl.startsWith("jdbc:h2:")
        return newKSqlClient {

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

            setDialect(if (isH2) H2Dialect() else MySqlDialect())

            addDraftInterceptors(interceptors)

            addFilters(filters)

            setEntityManager(
                EntityManager(
                    BookStore::class.java,
                    Book::class.java,
                    Author::class.java,
                    TreeNode::class.java
                )
            )
            setCaches {
                if (cacheFactory != null) {
                    setCacheFactory(cacheFactory)
                }
            }
        }.also {
            if (isH2) {
                initializeH2Database(it)
            }
        }
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