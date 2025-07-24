package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.storeId
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.fetchBy
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import kotlin.test.Test

class BaseQueryTest : AbstractQueryTest() {

    @Test
    fun testBaseQueryWithFetch() {
        val baseTable = sqlClient.createBaseQuery(BookStore::class) {
            selector()
                .add(table)
                .add(
                    sql(Int::class, "dense_rank() over(order by %e desc)") {
                        expression(
                            subQuery(Book::class) {
                                where(table.storeId eq parentTable.id)
                                select(rowCount())
                            }
                        )
                    }
                )
        }.asBaseTable()
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 le 2)
                where(table._1.name like "M")
                select(table._1.fetchBy {
                    allScalarFields()
                    books {
                        allScalarFields()
                    }
                })
            }
        ) {
            sql(
                """select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4 
                    |from (
                    |--->select 
                    |--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.VERSION c3, tb_2_.WEBSITE c4, 
                    |--->--->dense_rank() over(
                    |--->--->--->order by (
                    |--->--->--->--->select count(1) from BOOK tb_3_ 
                    |--->--->--->--->where tb_3_.STORE_ID = tb_2_.ID
                    |--->--->--->) desc
                    |--->--->) c5 
                    |--->from BOOK_STORE tb_2_
                    |) tb_1_ 
                    |where tb_1_.c5 <= ? and tb_1_.c2 like ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE 
                    |from BOOK tb_1_ where tb_1_.STORE_ID = ?""".trimMargin()
            )
            rows(
                """[{
                    |--->"id":2,
                    |--->"name":"MANNING",
                    |--->"version":0,
                    |--->"website":null,
                    |--->"books":[
                    |--->--->{"id":10,"name":"GraphQL in Action","edition":1,"price":80.00},
                    |--->--->{"id":11,"name":"GraphQL in Action","edition":2,"price":81.00},
                    |--->{"id":12,"name":"GraphQL in Action","edition":3,"price":80.00}
                    |--->]
                    |}]""".trimMargin()
            )
        }
    }
}