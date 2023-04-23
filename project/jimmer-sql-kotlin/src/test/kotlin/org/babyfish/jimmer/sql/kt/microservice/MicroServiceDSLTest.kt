package org.babyfish.jimmer.sql.kt.microservice

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.microservice.OrderItem
import org.babyfish.jimmer.sql.kt.model.microservice.id
import org.babyfish.jimmer.sql.kt.model.microservice.order
import org.babyfish.jimmer.sql.kt.model.microservice.products
import kotlin.test.Test

class MicroServiceDSLTest : AbstractQueryTest() {

    @Test
    fun testFindByManyToOne() {
        val sqlClient = sqlClient {
            setMicroServiceName("order-item-service")
            setMicroServiceExchange(MicroServiceExchangeImpl())
        }
        executeAndExpect(
            sqlClient.createQuery(OrderItem::class) {
                where(table.order.id eq 1)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.ORDER_ID 
                    |from MS_ORDER_ITEM tb_1_ 
                    |where tb_1_.ORDER_ID = ?""".trimMargin()
            )
            rows(
                "[" +
                                    "--->{\"id\":1,\"name\":\"ms-order-1.item-1\",\"order\":{\"id\":1}}," +
                                    "--->{\"id\":2,\"name\":\"ms-order-1.item-2\",\"order\":{\"id\":1}}" +
                                    "]"
            )
        }
    }

    @Test
    fun testFindByManyToMany() {
        val sqlClient = sqlClient {
            setMicroServiceName("order-item-service")
            setMicroServiceExchange(MicroServiceExchangeImpl())
        }
        executeAndExpect(
            sqlClient.createQuery(OrderItem::class) {
                where(table.asTableEx().products.id eq 2)
                select(table.id)
            }.distinct()
        ) {
            sql(
                """select distinct tb_1_.ID 
                    |from MS_ORDER_ITEM tb_1_ 
                    |inner join MS_ORDER_ITEM_PRODUCT_MAPPING tb_2_ 
                    |--->on tb_1_.ID = tb_2_.ORDER_ITEM_ID 
                    |where tb_2_.PRODUCT_ID = ?""".trimMargin()
            )
            rows("[1,2,4]")
        }
    }
}