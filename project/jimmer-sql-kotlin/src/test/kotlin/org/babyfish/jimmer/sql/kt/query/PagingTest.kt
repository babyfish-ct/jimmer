package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.like
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.junit.Test

class PagingTest : AbstractQueryTest() {

    @Test
    fun testPaging() {

        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.name like "GraphQL")
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }.fetchPage(-1, 3, it) { entities, totalCount, _ ->
                Page(entities, totalCount)
            }
        }) {
            row(0) {
                expectJson(
                    "Page(entities=[], totalCount=0)",
                    it
                )
            }
        }

        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.name like "GraphQL")
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }.fetchPage(0, 3, it) { entities, totalCount, _ ->
                Page(entities, totalCount)
            }
        }) {
            sql(
                """select count(1) 
                    |from BOOK tb_1_ 
                    |where tb_1_.NAME like ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where tb_1_.NAME like ? 
                    |order by tb_1_.NAME asc, tb_1_.EDITION desc 
                    |limit ?""".trimMargin()
            ).variables("%GraphQL%", 3)
            row(0) {
                expectJson(
                    """Page(
                        |--->entities=[
                        |--->--->{
                        |--->--->--->"id":12,
                        |--->--->--->"name":
                        |--->--->--->"GraphQL in Action",
                        |--->--->--->"edition":3,
                        |--->--->--->"price":80.00,
                        |--->--->--->"store":{"id":2}
                        |--->--->}, {
                        |--->--->--->"id":11,
                        |--->--->--->"name":"GraphQL in Action",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":81.00,
                        |--->--->--->"store":{"id":2}
                        |--->--->}, {
                        |--->--->--->"id":10,
                        |--->--->--->"name":"GraphQL in Action",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":80.00,
                        |--->--->--->"store":{"id":2}
                        |--->--->}
                        |--->], 
                        |--->totalCount=6
                        |)""".trimMargin(),
                    it
                )
            }
        }

        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.name like "GraphQL")
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }.fetchPage(1, 3, it) { entities, totalCount, _ ->
                Page(entities, totalCount)
            }
        }) {
            sql(
                """select count(1) 
                    |from BOOK tb_1_ 
                    |where tb_1_.NAME like ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where tb_1_.NAME like ? 
                    |order by tb_1_.NAME desc, tb_1_.EDITION asc 
                    |limit ?""".trimMargin()
            ).variables("%GraphQL%", 3)
            row(0) {
                expectJson(
                    """Page(
                        |--->entities=[
                        |--->--->{
                        |--->--->--->"id":3,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":3,
                        |--->--->--->"price":51.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, {
                        |--->--->--->"id":2,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":55.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}, {
                        |--->--->--->"id":1,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":50.00,
                        |--->--->--->"store":{"id":1}
                        |--->--->}
                        |--->], 
                        |--->totalCount=6
                        |)""".trimMargin(),
                    it
                )
            }
        }

        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.storeId eq 2L)
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }.fetchPage(1, 2, it) { entities, totalCount, _ ->
                Page(entities, totalCount)
            }
        }) {
            sql(
                """select count(1) 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID = ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID = ? 
                    |order by tb_1_.NAME desc, tb_1_.EDITION asc 
                    |limit ?""".trimMargin()
            ).variables(2L, 1)
            row(0) {
                expectJson(
                    """Page(
                        |--->entities=[
                        |--->--->{
                        |--->--->--->"id":10,
                        |--->--->--->"name":"GraphQL in Action",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":80.00,
                        |--->--->--->"store":{"id":2}
                        |--->--->}
                        |--->], 
                        |--->totalCount=3
                        |)""".trimMargin(),
                    it
                )
            }
        }

        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                    where(table.name like "GraphQL")
                    orderBy(table.name.asc(), table.edition.desc())
                    select(table)
            }.fetchPage(-1, 3, it) { entities, totalCount, _ ->
                Page(entities, totalCount)
            }
        }) {
            row(0) {
                expectJson(
                    "Page(entities=[], totalCount=0)",
                    it
                )
            }
        }
    }

    data class Page<E>(
        val entities: List<E>,
        val totalCount: Long
    )
}