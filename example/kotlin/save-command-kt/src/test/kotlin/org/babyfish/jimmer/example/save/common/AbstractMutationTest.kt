package org.babyfish.jimmer.example.save.common

import org.babyfish.jimmer.example.save.model.ENTITY_MANAGER
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.babyfish.jimmer.sql.runtime.DefaultExecutor
import org.babyfish.jimmer.sql.runtime.Executor
import org.h2.Driver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import java.io.IOException
import java.io.InputStreamReader
import java.sql.Connection
import java.sql.SQLException
import java.util.*
import kotlin.math.min

abstract class AbstractMutationTest {

    private lateinit var connection: Connection

    private lateinit var sqlClient: KSqlClient

    private lateinit var executedStatements: MutableList<ExecutedStatement>

    @BeforeEach
    @Throws(SQLException::class)
    fun beforeTest() {
        connection = createConnection()
        createDatabase(connection)
        executedStatements = mutableListOf()
        sqlClient = newKSqlClient {
            setEntityManager(ENTITY_MANAGER)
            setDialect(H2Dialect())
            setConnectionManager {
                proceed(connection)
            }
            setExecutor(
                object : Executor {
                    override fun <R : Any?> execute(args: Executor.Args<R>): R {
                        executedStatements.add(
                            ExecutedStatement(args.sql, *args.variables.toTypedArray())
                        )
                        return DefaultExecutor.INSTANCE.execute(args)
                    }
                }
            )
            customize(this)
        }
    }

    @AfterEach
    @Throws(SQLException::class)
    fun afterTest() {
        connection.close()
    }

    protected val sql: KSqlClient
        get() = sqlClient

    protected fun jdbc(sql: String, vararg args: Any?) {
        connection.prepareStatement(sql).use { statement ->
            for (i in args.indices) {
                statement.setObject(i + 1, args[i])
            }
            statement.execute()
        }
    }

    protected fun assertExecutedStatements(vararg executedStatements: ExecutedStatement) {
        val count = min(this.executedStatements.size, executedStatements.size)
        for (i in 0 until count) {
            Assertions.assertEquals(
                executedStatements[i].sql,
                this.executedStatements[i].sql,
                "Failed to assert sql of statements[$i]"
            )
            Assertions.assertEquals(
                executedStatements[i].variables,
                this.executedStatements[i].variables,
                "Failed to assert variables of statements[$i]"
            )
        }
        Assertions.assertEquals(
            executedStatements.size,
            this.executedStatements.size,
            "Expected " +
                executedStatements.size +
                " statements, but " +
                this.executedStatements.size +
                " statements"
        )
    }

    protected open fun customize(dsl: KSqlClientDsl) {}

    companion object {

        private fun createConnection(): Connection {
            val properties = Properties()
            properties.setProperty("database_to_upper", "true")
            return Driver().connect(
                "jdbc:h2:mem:save-cmd-example",
                properties
            )
        }

        private fun createDatabase(con: Connection) {
            val stream = AbstractMutationTest::class.java.classLoader.getResourceAsStream("unit-test.sql")
            if (stream == null) {
                Assertions.fail<Unit>("Failed to initialize database, cannot load 'database.sql'")
            }
            try {
                InputStreamReader(stream).use { reader ->
                    val builder: StringBuilder = StringBuilder()
                    val buf: CharArray = CharArray(1024)
                    var len: Int
                    while ((reader.read(buf).also { len = it }) != -1) {
                        builder.append(buf, 0, len)
                    }
                    con.createStatement().execute(builder.toString())
                }
            } catch (ex: IOException) {
                Assertions.fail("Failed to initialize database", ex)
            } catch (ex: SQLException) {
                Assertions.fail("Failed to initialize database", ex)
            }
        }
    }
}