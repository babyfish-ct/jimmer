package org.babyfish.jimmer.sql.kt.microservice

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.kt.model.microservice.OrderItem
import org.babyfish.jimmer.sql.kt.model.microservice.by
import kotlin.test.Test

class MicroServiceQueryTest : AbstractQueryTest() {

    @Test
    fun testFetch() {
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
                |--->}
                |]""".trimMargin()
            )
        }
    }
}