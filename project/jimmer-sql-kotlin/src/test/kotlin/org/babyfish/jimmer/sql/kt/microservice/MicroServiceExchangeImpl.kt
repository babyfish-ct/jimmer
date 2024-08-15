package org.babyfish.jimmer.sql.kt.microservice

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractTest
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImplementor
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.babyfish.jimmer.sql.runtime.ConnectionManager
import org.babyfish.jimmer.sql.runtime.MicroServiceExchange
import org.babyfish.jimmer.sql.runtime.MicroServiceExporter
import java.sql.Connection
import java.util.function.Function

class MicroServiceExchangeImpl() : MicroServiceExchange {

    private val orderClient = newKSqlClient {
        setConnectionManager(CONNECTION_MANAGER)
        setMicroServiceName("order-service")
        setMicroServiceExchange(this@MicroServiceExchangeImpl)
    }

    private val orderItemClient = newKSqlClient {
        setConnectionManager(CONNECTION_MANAGER)
        setMicroServiceName("order-item-service")
        setMicroServiceExchange(this@MicroServiceExchangeImpl)
    }

    private val productClient = newKSqlClient {
        setConnectionManager(CONNECTION_MANAGER)
        setMicroServiceName("product-service")
        setMicroServiceExchange(this@MicroServiceExchangeImpl)
    }

    override fun findByIds(
        microServiceName: String,
        ids: Collection<*>,
        fetcher: Fetcher<*>
    ): List<ImmutableSpi> {
        return MicroServiceExporter((sqlClient(microServiceName) as KSqlClientImplementor).javaClient)
            .findByIds(ids, fetcher)
    }

    override fun findByAssociatedIds(
        microServiceName: String,
        prop: ImmutableProp,
        targetIds: Collection<*>,
        fetcher: Fetcher<*>
    ): List<Tuple2<Any, ImmutableSpi>> {
        return MicroServiceExporter((sqlClient(microServiceName) as KSqlClientImplementor).javaClient)
            .findByAssociatedIds(prop, targetIds, fetcher)
    }

    private fun sqlClient(microServiceName: String): KSqlClient =
        when (microServiceName) {
            "order-service" -> orderClient
            "order-item-service" -> orderItemClient
            "product-service" -> productClient
            else -> throw IllegalArgumentException(
                "Illegal microservice name \"" +
                    microServiceName +
                    "\""
            )
        }

    companion object {

        @Suppress("UNCHECKED_CAST")
        private val CONNECTION_MANAGER: ConnectionManager = object : ConnectionManager {
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
    }
}