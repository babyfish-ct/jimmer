package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.like
import org.babyfish.jimmer.sql.kt.ast.expression.sql
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
                        |--->--->--->"storeId":2
                        |--->--->}, {
                        |--->--->--->"id":11,
                        |--->--->--->"name":"GraphQL in Action",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":81.00,
                        |--->--->--->"storeId":2
                        |--->--->}, {
                        |--->--->--->"id":10,
                        |--->--->--->"name":"GraphQL in Action",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":80.00,
                        |--->--->--->"storeId":2
                        |--->--->}
                        |--->], 
                        |--->totalCount=6
                        |)""".trimMargin(),
                    it
                )
            }
        }

        connectAndExpect({
            sqlClient {
                setReverseSortOptimizationEnabled(true)
            }.createQuery(Book::class) {
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
                    |/* reverse sorting optimization */ 
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
                        |--->--->--->"storeId":1
                        |--->--->}, {
                        |--->--->--->"id":2,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":55.00,
                        |--->--->--->"storeId":1
                        |--->--->}, {
                        |--->--->--->"id":1,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":50.00,
                        |--->--->--->"storeId":1
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
            }.setReverseSortOptimizationEnabled(true).fetchPage(1, 2, it) { entities, totalCount, _ ->
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
                    |/* reverse sorting optimization */ 
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
                        |--->--->--->"storeId":2
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

    @Test
    fun testIssue1192() {
        connectAndExpect({
            sqlClient { setReverseSortOptimizationEnabled(true) }
                .createQuery(Book::class) {
                    orderBy(table.name.asc())
                    orderBy(table.edition.desc())
                    select(table.fetchBy { name() })
                }
                .setReverseSortOptimizationEnabled(false)
                .fetchPage(5, 2, it)
        }) {
            sql("select count(1) from BOOK tb_1_")
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME 
                    |from BOOK tb_1_ 
                    |order by tb_1_.NAME asc, tb_1_.EDITION desc 
                    |limit ? offset ?""".trimMargin()
            )
        }
    }

    @Test
    fun testIssue1297() {
        val baseTable = sqlClient.createBaseQuery(Book::class) {
            selections
                .add(table)
                .add(
                    sql<Int>(
                        "row_number() over(partition by %e order by %e desc)",
                        table.storeId,
                        table.price
                    )
                )
        }.asBaseTable()
        connectAndExpect({
            sqlClient.createQuery(baseTable) {
                where(table._2 eq 1)
                select(table._1.fetchBy { name() })
            }.fetchPage(0, 2, it)
        }) {
            sql(
                """select count(1) 
                    |from (
                    |--->select 
                    |--->--->tb_1_.c1, tb_1_.c2 
                    |--->from (
                    |--->--->select 
                    |--->--->--->tb_2_.ID c1, tb_2_.NAME c2, 
                    |--->--->--->row_number() over(
                    |--->--->--->--->partition by tb_2_.STORE_ID 
                    |--->--->--->--->order by tb_2_.PRICE desc
                    |--->--->--->) c3 
                    |--->--->from BOOK tb_2_
                    |--->) tb_1_ 
                    |--->where tb_1_.c3 = ?
                    |) tb_simple_count__""".trimMargin()
            )
            statement(1).sql(
                """select 
                    |--->tb_1_.c1, tb_1_.c2 
                    |from (
                    |--->select 
                    |--->--->tb_2_.ID c1, tb_2_.NAME c2, 
                    |--->--->row_number() over(
                    |--->--->--->partition by tb_2_.STORE_ID 
                    |--->--->--->order by tb_2_.PRICE desc
                    |--->--->) c3 
                    |--->from BOOK tb_2_
                    |) tb_1_ 
                    |where tb_1_.c3 = ? limit ?""".trimMargin()
            )
        }
    }

    @Test
    fun testIssue1302() {
        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.storeId eq 1)
                orderBy(table.name.asc(), table.edition.desc())
                select(table.fetchBy {
                    name()
                })
            }.fetchPage(0, 3, it)
        }) {
            sql(
                """select count(1) 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID = ?""".trimMargin()
            )
            statement(1).sql(
                """select 
                    |--->tb_1_.ID, 
                    |--->tb_1_.NAME 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID = ? 
                    |order by tb_1_.NAME asc, tb_1_.EDITION desc 
                    |limit ?""".trimMargin()
            ).variables(1L, 3)
            rows(
                """[{
                    |--->"rows":[
                    |--->--->{"id":6,"name":"Effective TypeScript"},
                    |--->--->{"id":5,"name":"Effective TypeScript"},
                    |--->--->{"id":4,"name":"Effective TypeScript"}
                    |--->],
                    |--->"totalRowCount":9,
                    |--->"totalPageCount":3
                    |}]""".trimMargin()
            )
        }

        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.storeId eq 1)
                orderBy(table.name.asc(), table.edition.desc())
                select(table.fetchBy {
                    name()
                })
            }.fetchPage(1, 3, it)
        }) {
            sql(
                """select count(1) 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID = ?""".trimMargin()
            )
            statement(1).sql(
                """select 
                    |--->tb_1_.ID, 
                    |--->tb_1_.NAME 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID = ? 
                    |order by tb_1_.NAME asc, tb_1_.EDITION desc 
                    |limit ? offset ?""".trimMargin()
            ).variables(1L, 3, 3L)
            rows(
                """[{
                    |--->"rows":[
                    |--->--->{"id":3,"name":"Learning GraphQL"},
                    |--->--->{"id":2,"name":"Learning GraphQL"},
                    |--->--->{"id":1,"name":"Learning GraphQL"}
                    |--->],
                    |--->"totalRowCount":9,
                    |--->"totalPageCount":3
                    |}]""".trimMargin()
            )
        }

        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.storeId eq 1)
                orderBy(table.name.asc(), table.edition.desc())
                select(table.fetchBy {
                    name()
                })
            }.fetchPage(2, 3, it)
        }) {
            sql(
                """select count(1) 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID = ?""".trimMargin()
            )
            statement(1).sql(
                """select 
                    |--->tb_1_.ID, 
                    |--->tb_1_.NAME 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID = ? 
                    |order by tb_1_.NAME asc, tb_1_.EDITION desc 
                    |limit ? offset ?""".trimMargin()
            ).variables(1L, 3, 6L)
            rows(
                """[{
                    |--->"rows":[
                    |--->--->{"id":9,"name":"Programming TypeScript"},
                    |--->--->{"id":8,"name":"Programming TypeScript"},
                    |--->--->{"id":7,"name":"Programming TypeScript"}
                    |--->],
                    |--->"totalRowCount":9,
                    |--->"totalPageCount":3
                    |}]""".trimMargin()
            )
        }
    }

    data class Page<E>(
        val entities: List<E>,
        val totalCount: Long
    )
}