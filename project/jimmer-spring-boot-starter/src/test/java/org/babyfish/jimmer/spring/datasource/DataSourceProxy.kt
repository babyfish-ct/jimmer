package org.babyfish.jimmer.spring.datasource

import java.sql.Connection
import javax.sql.DataSource

class DataSourceProxy(
    private val dataSource: DataSource,
    private val txCallback: TxCallback
) : DataSource by(dataSource) {

    override fun getConnection(): Connection =
        ConnectionProxy(dataSource.connection, txCallback).also {
            txCallback.open()
        }

    override fun getConnection(username: String?, password: String?): Connection =
        ConnectionProxy(dataSource.getConnection(username, password), txCallback).also {
            txCallback.open()
        }
}