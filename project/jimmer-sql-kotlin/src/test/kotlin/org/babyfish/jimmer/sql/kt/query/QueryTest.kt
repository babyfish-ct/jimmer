package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.BookStore
import org.babyfish.jimmer.sql.kt.model.id
import org.babyfish.jimmer.sql.kt.model.name
import org.babyfish.jimmer.sql.kt.model.website
import kotlin.test.Test
import kotlin.test.expect

class QueryTest : AbstractQueryTest() {

    @Test
    fun testTable() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                select(table)
            }
        ) {
            sql("select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE from BOOK_STORE as tb_1_")
            rows(
                """[
                    |--->{
                    |--->--->"id":1,"name":
                    |--->--->"O'REILLY",
                    |--->--->"version":0,
                    |--->--->"website":null
                    |--->},{
                    |--->--->"id":2,
                    |--->--->"name":"MANNING",
                    |--->--->"version":0,
                    |--->--->"website":null
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testFields() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                select(
                    table.id,
                    table.name,
                    table.website
                )
            }
        ) {
            sql("select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE from BOOK_STORE as tb_1_")
            rows {
                expect("[Tuple3{_1=1, _2=O'REILLY, _3=null}, Tuple3{_1=2, _2=MANNING, _3=null}]") {
                    it.toString()
                }
            }
        }
    }
}