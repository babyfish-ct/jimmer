package org.babyfish.jimmer.sql.kt.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.babyfish.jimmer.jackson.ImmutableModule
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.kt.model.ENTITY_MANAGER
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.junit.BeforeClass
import java.io.IOException
import java.io.InputStreamReader
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class AbstractTest {

    @BeforeTest
    open fun beforeTest() {
        clearExecutions()
    }

    protected val sqlClient: KSqlClient = sqlClient()

    protected fun sqlClient(block: KSqlClientDsl.() -> Unit = {}): KSqlClient =
        newKSqlClient {
            setEntityManager(ENTITY_MANAGER)
            setExecutor {
                _executions.add(Execution(sql, variables))
                proceed()
            }
            block()
        }

    protected class Execution internal constructor(
        val sql: String,
        val variables: List<Any>
    )

    protected val executions: List<Execution>
        get() = _executions

    protected fun clearExecutions() {
        _executions.clear()
    }

    private val _executions = mutableListOf<Execution>()

    companion object {

        private val JDBC_URL = "jdbc:h2:~/jimmer_kt_test_db;database_to_upper=true"

        @JvmStatic
        fun jdbc(dataSource: DataSource? = null, rollback: Boolean = false, block: (Connection) -> Unit) {
            try {
                val con = if (dataSource != null) {
                    dataSource.connection
                } else {
                    org.h2.Driver().connect(JDBC_URL, null)
                }
                try {
                    if (rollback) {
                        con.autoCommit = false
                        try {
                            block(con)
                        } finally {
                            con.rollback()
                        }
                    } else {
                        block(con)
                    }
                } finally {
                    con.close()
                }
            } catch (ex: SQLException) {
                fail("SQL error", ex)
            }
        }

        protected fun initDatabase(con: Connection) {
            val stream = AbstractTest::class.java.classLoader.getResourceAsStream("database.sql")
                ?: fail("Failed to initialize database, cannot load 'database.sql'")
            try {
                InputStreamReader(stream).use { reader ->
                    val builder = StringBuilder()
                    val buf = CharArray(1024)
                    var len: Int
                    while (reader.read(buf).also { len = it } != -1) {
                        builder.append(buf, 0, len)
                    }
                    con.createStatement().execute(builder.toString())
                }
            } catch (ex: IOException) {
                fail("Failed to initialize database", ex)
            } catch (ex: SQLException) {
                fail("Failed to initialize database", ex)
            }
        }

        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            jdbc {con ->
                initDatabase(con)
            }
        }

        @JvmStatic
        protected val MAPPER = ObjectMapper()
            .registerModule(ImmutableModule())
            .registerModule(JavaTimeModule())

        @JvmStatic
        protected fun contentEquals(
            expect: String,
            actual: String,
            message: String? = null
        ) {
            assertEquals(
                expect
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("--->", ""),
                actual,
                message
            )
        }
    }
}