package org.babyfish.jimmer.sql.kt.microservice

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.kt.model.microservice.OrderItem
import org.babyfish.jimmer.sql.kt.model.microservice.by
import org.babyfish.jimmer.sql.kt.model.microservice.fetchBy
import kotlin.test.Test

class MicroServiceQueryTest : AbstractQueryTest() {

    @Test
    fun testFetchManyToOne() {
        val sqlClient = sqlClient {
                setMicroServiceName("order-item-service")
                setMicroServiceExchange(MicroServiceExchangeImpl())
            }
        executeAndExpect(
            sqlClient
                .createQuery(OrderItem::class) {
                    select(
                        table.fetch(
                            newFetcher(OrderItem::class).by {
                                allScalarFields()
                                order {
                                    allScalarFields()
                                }
                            }
                        )
                    )
                }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.ORDER_ID 
                    |from MS_ORDER_ITEM tb_1_""".trimMargin()
            )
            rows(
                """[
                |--->{
                |--->--->"id":1,
                |--->--->"name":"ms-order-1.item-1",
                |--->--->"order":{
                |--->--->--->"id":1,
                |--->--->--->"name":"ms-order-1"
                |--->--->}
                |--->},
                |--->{
                |--->--->"id":2,
                |--->--->"name":"ms-order-1.item-2",
                |--->--->"order":{
                |--->--->--->"id":1,
                |--->--->--->"name":"ms-order-1"
                |--->--->}
                |--->},
                |--->{
                |--->--->"id":3,
                |--->--->"name":"ms-order-2.item-1",
                |--->--->"order":{
                |--->--->--->"id":2,
                |--->--->--->"name":"ms-order-2"
                |--->--->}
                |--->},
                |--->{
                |--->--->"id":4,
                |--->--->"name":"ms-order-2.item-2",
                |--->--->"order":{
                |--->--->--->"id":2,
                |--->--->--->"name":"ms-order-2"
                |--->--->}
                |--->},{
                |--->--->"id":999,
                |--->--->"name":"ms-order-X.item-X",
                |--->--->"order":null
                |--->}
                |]""".trimMargin()
            )
        }
    }

    @Test
    fun testFetchManyToMany() {
        val sqlClient = sqlClient {
            setMicroServiceName("order-item-service")
            setMicroServiceExchange(MicroServiceExchangeImpl())
        }
        executeAndExpect(
            sqlClient.createQuery(OrderItem::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        products {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME from MS_ORDER_ITEM tb_1_"""
            )
            statement(1).sql(
                """select tb_1_.ORDER_ITEM_ID, tb_1_.PRODUCT_ID 
                    |from MS_ORDER_ITEM_PRODUCT_MAPPING tb_1_ 
                    |where tb_1_.ORDER_ITEM_ID in (?, ?, ?, ?, ?)""".trimMargin()
            )
            rows(
                """[
                    |--->{
                    |--->--->"id":1,
                    |--->--->"name":"ms-order-1.item-1",
                    |--->--->"products":[
                    |--->--->--->{"id":1,"name":"ms-product-1"},
                    |--->--->--->{"id":2,"name":"ms-product-2"}
                    |--->--->]
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"ms-order-1.item-2",
                    |--->--->"products":[
                    |--->--->--->{"id":2,"name":"ms-product-2"},
                    |--->--->--->{"id":3,"name":"ms-product-3"}
                    |--->--->]
                    |--->},{
                    |--->--->"id":3,
                    |--->--->"name":"ms-order-2.item-1",
                    |--->--->"products":[
                    |--->--->--->{"id":1,"name":"ms-product-1"},
                    |--->--->--->{"id":3,"name":"ms-product-3"}
                    |--->--->]
                    |--->},{
                    |--->--->"id":4,
                    |--->--->"name":"ms-order-2.item-2",
                    |--->--->"products":[
                    |--->--->--->{"id":1,"name":"ms-product-1"},
                    |--->--->--->{"id":2,"name":"ms-product-2"},
                    |--->--->--->{"id":3,"name":"ms-product-3"}
                    |--->--->]
                    |--->},{
                    |--->--->"id":999,
                    |--->--->"name":"ms-order-X.item-X",
                    |--->--->"products":[]
                    |--->}
                    |]""".trimMargin()
            )
        }
    }
}