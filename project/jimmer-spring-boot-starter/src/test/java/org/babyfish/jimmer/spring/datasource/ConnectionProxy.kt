package org.babyfish.jimmer.spring.datasource

import java.sql.Connection

class ConnectionProxy(
    private val con: Connection,
    private val txCallback: TxCallback
) : Connection by con {

    override fun setAutoCommit(autoCommit: Boolean) {
        con.autoCommit = autoCommit
    }

    override fun commit() {
        con.commit()
        txCallback.commit()
    }

    override fun rollback() {
        con.rollback()
        txCallback.rollback()
    }
}