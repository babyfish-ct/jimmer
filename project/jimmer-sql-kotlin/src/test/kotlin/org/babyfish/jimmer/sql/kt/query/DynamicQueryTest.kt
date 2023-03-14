package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.table.makeOrders
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.embedded.Transform
import kotlin.test.Test

class DynamicQueryTest : AbstractQueryTest() {

    @Test
    fun testReferenceProp() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                orderBy(
                    table.makeOrders("store.name asc;\n name, edition desc")
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK as tb_1_ 
                    |left join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |--->order by tb_2_.NAME asc, tb_1_.NAME asc, tb_1_.EDITION desc""".trimMargin()
            )
        }
    }

    @Test
    fun testEmbedded() {
        executeAndExpect(
            sqlClient.createQuery(Transform::class) {
                orderBy(
                    table.makeOrders("source.leftTop.x asc;\n target.rightBottom.y")
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, 
                    |tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, 
                    |tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM 
                    |from TRANSFORM as tb_1_ 
                    |order by tb_1_.`LEFT` asc, tb_1_.TARGET_BOTTOM asc""".trimMargin()
            )
        }
    }
}