package org.babyfish.jimmer.sql.kt.common

import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.ast.mutation.MutationResultItem
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutationResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult
import java.sql.Connection
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.test.*

abstract class AbstractMutationTest : AbstractTest() {

    protected fun executeAndExpectRowCount(
        executable: KExecutable<Int>,
        block: ExpectDSLWithRowCount.() -> Unit
    ) {
        executeAndExpectRowCount(null, executable, block)
    }

    protected fun executeAndExpectRowCount(
        rowCountBlock: (Connection) -> Int,
        block: ExpectDSLWithRowCount.() -> Unit
    ) {
        executeAndExpectRowCount(null, { con -> rowCountBlock(con) }, block)
    }

    protected fun executeAndExpectRowCount(
        dataSource: DataSource? = null,
        executable: KExecutable<Int>,
        block: ExpectDSLWithRowCount.() -> Unit
    ) {
        return executeAndExpectRowCount(
            dataSource,
            { con -> executable.execute(con) },
            block
        )
    }

    protected fun executeAndExpectRowCount(
        dataSource: DataSource? = null,
        rowCountBlock: (Connection) -> Int,
        block: ExpectDSLWithRowCount.() -> Unit
    ) {
        jdbc(dataSource, true) { con ->
            clearExecutions()
            var affectedRowCount = 0
            var throwable: Throwable? = null
            try {
                affectedRowCount = rowCountBlock(con)
            } catch (ex: Throwable) {
                throwable = ex
            }
            assertRowCount(throwable, affectedRowCount, block)
        }
    }

    protected fun executeAndExpectResult(
        executeBlock: (Connection) -> KMutationResult,
        block: ExpectDSLWithResult.() -> Unit
    ) {
        executeAndExpectResult(null, executeBlock, block)
    }

    protected fun executeAndExpectResult(
        dataSource: DataSource?,
        executeBlock: (Connection) -> KMutationResult,
        block: ExpectDSLWithResult.() -> Unit
    ) {
        jdbc(dataSource, true) { con ->
            clearExecutions()
            var result: KMutationResult?
            var throwable: Throwable? = null
            try {
                result = executeBlock(con)
            } catch (ex: Throwable) {
                throwable = ex
                result = null
            }
            assertResult(throwable, result, block)
        }
    }

    protected fun <T> connectAndExpect(
        action: (Connection) -> T,
        block: ExpectDSLWithValue<T>.() -> Unit
    ) {
        connectAndExpect(null, action, block)
    }

    protected fun <T> connectAndExpect(
        dataSource: DataSource?,
        action: (Connection) -> T,
        block: ExpectDSLWithValue<T>.() -> Unit
    ) {
        jdbc(dataSource, true) { con ->
            clearExecutions()
            var value: T?
            var throwable: Throwable? = null
            try {
                value = action(con)
            } catch (ex: Throwable) {
                throwable = ex
                value = null
            }
            val dsl = ExpectDSLWithValue(executions, throwable, value)
            block(dsl)
            dsl.close()
        }
    }

    private fun assertRowCount(
        throwable: Throwable?,
        rowCount: Int,
        block: ExpectDSLWithRowCount.() -> Unit
    ) {
        val dsl = ExpectDSLWithRowCount(executions, throwable, rowCount)
        block(dsl)
        dsl.close()
    }

    private fun assertResult(
        throwable: Throwable?,
        result: KMutationResult?,
        block: ExpectDSLWithResult.() -> Unit
    ) {
        val dsl = ExpectDSLWithResult(executions, throwable, result)
        block(dsl)
        dsl.close()
    }

    protected open class ExpectDSL(
        executions: List<Execution>,
        throwable: Throwable?
    ) {
        private val executions: List<Execution>
        protected var throwable: Throwable?
        private var statementCount = 0
        private var throwableChecked = false

        init {
            this.executions = executions
            this.throwable = throwable
        }

        fun statement(block: StatementDSL.() -> Unit) {
            val index = statementCount++
            if (index < executions.size) {
                block(StatementDSL(index, executions[index]))
            } else if (throwable != null) {
                throw throwable!!
            } else {
                fail("Two many statements, max statement count: " + executions.size)
            }
        }

        fun throwable(block: ThrowableDSL.() -> Unit) {
            assertNotNull(throwable, "No throwable.")
            block(ThrowableDSL(throwable))
            throwableChecked = true
        }

        open fun close() {
            assertEquals(
                statementCount,
                executions.size,
                "Error statement count."
            )
            if (throwable != null) {
                if (!throwableChecked) {
                    throw throwable!!
                }
            }
        }
    }


    protected class ExpectDSLWithRowCount(
        executions: List<Execution>,
        throwable: Throwable?,
        private val rowCount: Int
    ) : ExpectDSL(executions, throwable) {
        fun rowCount(rowCount: Int) {
            if (throwable == null) {
                assertEquals(rowCount, this.rowCount, "bad row count")
            }
        }
    }

    protected class ExpectDSLWithResult(
        executions: List<Execution>,
        throwable: Throwable?,
        private val result: KMutationResult?
    ) : ExpectDSL(executions, throwable) {
        private var entityCount = 0
        fun totalRowCount(totalRowCount: Int): ExpectDSLWithResult {
            assertNotNull(result)
            assertEquals(totalRowCount, result!!.totalAffectedRowCount)
            return this
        }

        fun rowCount(entityType: KClass<*>, rowCount: Int): ExpectDSLWithResult {
            assertNotNull(result)
            assertEquals(
                rowCount,
                result!!.affectedRowCount(entityType),
                "rowCountMap['$entityType']"
            )
            return this
        }

        fun rowCount(prop: KProperty1<*, *>, rowCount: Int): ExpectDSLWithResult {
            assertNotNull(result)
            assertEquals(
                rowCount,
                result!!.affectedRowCount(prop),
                "rowCountMap['$prop']"
            )
            return this
        }

        fun entity(block: EntityDSL.() -> Unit): ExpectDSLWithResult {
            if (throwable != null) {
                throw throwable!!
            }
            return entity(entityCount++, block)
        }

        private fun entity(
            index: Int,
            block: EntityDSL.() -> Unit
        ): ExpectDSLWithResult {
            val item: MutationResultItem<*> = if (index == 0) {
                if (result is KSimpleSaveResult<*>) {
                    result
                } else {
                    (result as KBatchSaveResult<*>).items[0]
                }
            } else {
                (result as KBatchSaveResult<*>).items[index]
            }
            block(EntityDSL(index, item))
            return this
        }

        override fun close() {
            super.close()
            val actualEntityCount: Int = if (result is KSimpleSaveResult<*>) {
                1
            } else if (result is KBatchSaveResult<*>) {
                result.items.size
            } else {
                0
            }
            assertEquals(
                entityCount,
                actualEntityCount,
                "entity.count"
            )
        }
    }

    protected class ExpectDSLWithValue<T>(
        executions: List<Execution>,
        throwable: Throwable?,
        private val value: T?
    ) : ExpectDSL(executions, throwable) {
        private var entityCount = 0
        fun value(value: String): ExpectDSLWithValue<T> {
            assertContent(
                value,
                this.value?.toString() ?: ""
            )
            return this
        }

        override fun close() {
            super.close()
            assertTrue(
                value != null,
                "value"
            )
        }
    }

    protected class StatementDSL internal constructor(
        private val index: Int,
        private val execution: Execution
    ) {
        fun sql(value: String) {
            contentEquals(
                value,
                execution.sql,
                "statements[$index].sql"
            )
        }

        fun variables(vararg values: Any?) {
            batchVariables(0, *values)
            batches(1)
        }

        fun queryReason(reason: QueryReason) {
            expect(reason) {
                execution.queryReason
            }
        }

        fun batches(count: Int) {
            expect(
                count,
                "statements[$index].batches is illegal, expected $count, actual ${execution.variablesList.count()}"
            ) {
                execution.variablesList.size
            }
        }

        fun batchVariables(batchIndex: Int, vararg values: Any?) {
            assertEquals(
                values.size,
                execution.variablesList[batchIndex].size,
                "statements[$index].batch[$batchIndex].variables.size. " +
                    "expected ${values.contentToString()}, actual: ${execution.variablesList[batchIndex]}"
            )
            for (i in values.indices) {
                val exp = values[i]
                val act: Any = execution.variablesList[batchIndex][i]
                if (exp is ByteArray) {
                    assertTrue(
                        Arrays.equals(exp as ByteArray?, act as ByteArray),
                        "statements[$index].batch[$batchIndex].variables[$i]. " +
                            "expected ${values.contentToString()}, actual: ${execution.variablesList[batchIndex]}"
                    )
                } else {
                    assertEquals(
                        exp,
                        act,
                        "statements[$index].batch[$batchIndex].variables[$i]. " +
                            "expected ${values.contentToString()}, actual: ${execution.variablesList[batchIndex]}"
                    )
                }
            }
        }

        fun unorderedVariables(vararg values: Any?) {
            assertEquals(
                values.toSet(),
                execution.variablesList[0].toSet(),
                "statements[$index].variables."
            )
        }

        fun variables(block: List<Any?>.() -> Unit) {
            block(execution.variablesList[0])
        }
    }

    protected class ThrowableDSL internal constructor(private val throwable: Throwable?) {
        fun type(type: Class<out Throwable?>?) {
            assertSame(type, throwable!!.javaClass)
        }

        fun message(message: String?) {
            assertEquals(message, throwable!!.message)
        }

        fun detail(block: Throwable.() -> Unit) {
            block(throwable!!)
        }
    }

    protected class EntityDSL internal constructor(
        private val index: Int,
        private val item: MutationResultItem<*>
    ) {
        fun original(json: String) {
            contentEquals(
                json,
                item.originalEntity.toString(),
                "originalEntities[$index]"
            )
        }

        fun modified(json: String) {
            contentEquals(
                json,
                item.modifiedEntity.toString(),
                "modifiedEntities[$index]"
            )
        }
    }
}