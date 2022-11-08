package org.babyfish.jimmer.benchmark.exposed

import org.jetbrains.annotations.TestOnly
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedConnection
import org.jetbrains.exposed.sql.transactions.*
import java.lang.UnsupportedOperationException
import java.sql.Connection

class FakeTransactionManager(
    private val db: Database
) : TransactionManager {

    @Volatile
    override var defaultRepetitionAttempts: Int = db.config.defaultRepetitionAttempts
        @Deprecated("Use DatabaseConfig to define the defaultRepetitionAttempts")
        @TestOnly
        set

    @Volatile
    override var defaultIsolationLevel: Int = db.config.defaultIsolationLevel
        get() {
            if (field == -1) {
                field = Database.getDefaultIsolationLevel(db)
            }
            return field
        }
        @Deprecated("Use DatabaseConfig to define the defaultIsolationLevel")
        @TestOnly
        set

    override fun newTransaction(isolation: Int, readOnly: Boolean, outerTransaction: Transaction?): Transaction =
        (
            outerTransaction?.takeIf { !db.useNestedTransactions } ?: Transaction(
                FakeTransaction(
                    db = db,
                    outerTransaction = outerTransaction
                )
            )
        ).apply {
            bindTransactionToThread(this)
        }

    override fun currentOrNull(): Transaction? = currentTransaction

    override fun bindTransactionToThread(transaction: Transaction?) {
        currentTransaction = transaction
    }

    override var defaultReadOnly: Boolean
        get() = true
        set(value) { throw UnsupportedOperationException() }
}

private var currentTransaction: Transaction? = null

private class FakeTransaction(
    override val db: Database,
    override val outerTransaction: Transaction?,
): TransactionInterface {

    override val readOnly: Boolean
        get() = true

    private val connectionLazy = lazy {
        db.connector()
    }

    override val connection: ExposedConnection<*>
        get() = connectionLazy.value

    override val transactionIsolation: Int
        get() = Connection.TRANSACTION_NONE

    override fun commit() {

    }

    override fun rollback() {

    }

    override fun close() {
        try {
            connection.close()
        } finally {
            currentTransaction = outerTransaction
        }
    }
}