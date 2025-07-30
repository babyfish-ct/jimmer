package org.babyfish.jimmer.sql.kt.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.babyfish.jimmer.jackson.ImmutableModule
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.babyfish.jimmer.sql.runtime.*
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose.Command
import org.babyfish.jimmer.sql.runtime.Executor.BatchContext
import org.junit.BeforeClass
import java.io.IOException
import java.io.InputStreamReader
import java.sql.Connection
import java.sql.SQLException
import java.util.function.Function
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

    protected open fun sqlClient(block: KSqlClientDsl.() -> Unit = {}): KSqlClient =
        newKSqlClient {
            setExecutor(object : Executor {
                override fun <R : Any?> execute(args: Executor.Args<R>): R {
                    _executions.add(
                        Execution(
                            args.sql,
                            (args.purpose as? Command)?.queryReason ?: QueryReason.NONE,
                            mutableListOf(args.variables)
                        )
                    )
                    return DefaultExecutor.INSTANCE.execute(args)
                }

                override fun executeBatch(
                    con: Connection,
                    sql: String,
                    generatedIdProp: ImmutableProp?,
                    purpose: ExecutionPurpose,
                    sqlClient: JSqlClientImplementor
                ): BatchContext {
                    val ctx = DefaultExecutor.INSTANCE.executeBatch(
                        con,
                        sql,
                        generatedIdProp,
                        purpose,
                        sqlClient
                    )
                    val execution = Execution(
                        sql,
                        QueryReason.NONE,
                        mutableListOf()
                    )
                    _executions += execution
                    return BatchContextWrapper(ctx, execution)
                }
            })
            // Don't set dialect here
            // Java uses H2Dialect to test `a ilke b`
            // Kotlin uses DefaultDialect ot test `lower(a) like b`
            block()
        }

    protected class Execution internal constructor(
        val sql: String,
        val queryReason: QueryReason,
        val variablesList: MutableList<List<Any>>
    )

    protected class TestConnectionManager : ConnectionManager {
        @Suppress("UNCHECKED_CAST")
        override fun <R> execute(con: Connection?, block: Function<Connection, R>): R {
            val ref = arrayOfNulls<Any>(1) as Array<R>
            if (con == null) {
                AbstractTest.jdbc { ref[0] = block.apply(it) }
            } else {
                ref[0] = block.apply(con)
            }
            return ref[0]
        }
    }

    protected val executions: List<Execution>
        get() = _executions

    protected fun clearExecutions() {
        _executions.clear()
    }

    private val _executions = mutableListOf<Execution>()

    companion object {

        private val JDBC_URL = "jdbc:h2:./build/h2/jimmer_kt_test_db;database_to_upper=true"

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
        protected val MAPPER: ObjectMapper = ObjectMapper()
            .registerModule(ImmutableModule())
            .registerModule(JavaTimeModule())

        @JvmStatic
        protected fun contentEquals(
            expect: String,
            actual: String,
            message: String? = null
        ) {
            val normalizedExpected = expect
                .replace("\r", "")
                .replace("\n", "")
                .replace("--->", "")

            // Try to parse as JSON and compare semantically to handle property ordering issues
            try {
                val expectedJson = MAPPER.readTree(normalizedExpected)
                val actualJson = MAPPER.readTree(actual)
                assertEquals(expectedJson, actualJson, message)
            } catch (e: Exception) {
                // Fall back to string comparison if JSON parsing fails
                assertEquals(normalizedExpected, actual, message)
            }
        }
    }

    private class BatchContextWrapper(
        private val raw: BatchContext,
        private val execution: Execution
    ) : BatchContext by raw {

        override fun add(variables: List<Any>) {
            execution.variablesList += variables
            raw.add(variables)
        }
    }
}