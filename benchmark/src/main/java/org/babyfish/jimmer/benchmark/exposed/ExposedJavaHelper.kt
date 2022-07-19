package org.babyfish.jimmer.benchmark.exposed

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource

/*
 * In addition to exposed, other frameworks participating in the test
 * allow querying data without opening a transaction.
 *
 * If all frameworks open transactions, it will defeat the original purpose of testing.
 * The reason why the test uses an in-memory database is to minimize the overhead of the database
 * to highlight the performance indicators of the ORM itself.
 *
 * SpringTransactionManager is too heavy, so use thread local transaction manager.
 */
fun connect(dataSource: DataSource) {

    // Like other case, use spring friendly data source.
    if (dataSource !is TransactionAwareDataSourceProxy) {
        throw IllegalArgumentException("Internal bug")
    }
    Database.connect(
        dataSource,
        // Fake transaction manager, this is very fast
        manager = { db -> FakeTransactionManager(db) }
    )
}

fun executeJavaRunnable(runnable: Runnable) {
    transaction { // light transaction manager, not heavy spring transaction
        runnable.run()
    }
}