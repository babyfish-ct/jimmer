package org.babyfish.jimmer.sql.kt.microservice

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractTest
import org.babyfish.jimmer.sql.kt.model.ENTITY_MANAGER
import org.babyfish.jimmer.sql.kt.newKSqlClient
import org.babyfish.jimmer.sql.runtime.ConnectionManager
import org.babyfish.jimmer.sql.runtime.MicroServiceExchange
import org.babyfish.jimmer.sql.runtime.MicroServiceExporter
import java.sql.Connection
import java.util.function.Function

class MicroServiceExchangeImpl() : MicroServiceExchange {

    private val orderClient = newKSqlClient {
        setEntityManager(ENTITY_MANAGER)
        setConnectionManager(CONNECTION_MANAGER)
        setMicroServiceName("order-service")
        setMicroServiceExchange(this@MicroServiceExchangeImpl)
    }

    private val orderItemClient = newKSqlClient {
        setEntityManager(ENTITY_MANAGER)
        setConnectionManager(CONNECTION_MANAGER)
        setMicroServiceName("order-item-service")
        setMicroServiceExchange(this@MicroServiceExchangeImpl)
    }

    private val productClient = newKSqlClient {
        setEntityManager(ENTITY_MANAGER)
        setConnectionManager(CONNECTION_MANAGER)
        setMicroServiceName("product-service")
        setMicroServiceExchange(this@MicroServiceExchangeImpl)
    }

    override fun findByIds(
        microServiceName: String,
        ids: Collection<*>,
        fetcher: Fetcher<*>
    ): List<ImmutableSpi> {
        return MicroServiceExporter(sqlClient(microServiceName).javaClient)
            .findByIds(ids, fetcher)
    }

    override fun findByAssociatedIds(
        microServiceName: String,
        prop: ImmutableProp,
        targetIds: Collection<*>,
        fetcher: Fetcher<*>
    ): List<Tuple2<Any, ImmutableSpi>> {
        return MicroServiceExporter(sqlClient(microServiceName).javaClient)
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
            override fun <R> execute(block: Function<Connection, R>): R {
                val ref = arrayOfNulls<Any>(1) as Array<R>
                AbstractTest.jdbc { con: Connection ->
                    ref[0] = block.apply(con)
                }
                return ref[0]
            }
        }
    }
}