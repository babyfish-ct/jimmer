package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.like
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.junit.Test

class ReverseSortBugTest : AbstractQueryTest() {

    @Test
    fun testReverseSortDisabled() {
        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.name like "GraphQL")
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }.setReverseSortOptimizationEnabled(false).fetchPage(1, 3, it) { entities, totalCount, _ ->
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
                    |limit ? offset ?""".trimMargin()
            ).variables("%GraphQL%", 3, 3L)
        }
    }

    @Test
    fun testReverseSortEnabled() {
        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.name like "GraphQL")
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }.setReverseSortOptimizationEnabled(true).fetchPage(1, 3, it) { entities, totalCount, _ ->
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
        }
    }

    @Test
    fun testShallowPaging() {
        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.name like "GraphQL")
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }.setReverseSortOptimizationEnabled(true).fetchPage(0, 3, it) { entities, totalCount, _ ->
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
        }
    }

    data class Page<E>(
        val entities: List<E>,
        val totalCount: Long
    )
}
