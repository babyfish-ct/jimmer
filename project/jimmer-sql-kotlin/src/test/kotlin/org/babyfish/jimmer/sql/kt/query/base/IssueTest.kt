package org.babyfish.jimmer.sql.kt.query.base

import org.babyfish.jimmer.sql.kt.ast.query.baseTableSymbol
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.store
import org.junit.Assume
import kotlin.test.Test

class IssueTest : AbstractQueryTest() {

    @Test
    fun testIssue1258() {
        Assume.assumeTrue("Difficult bug, waiting", false)
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                select(table._1, table._1.store)
            }
        ) {
            sql(
                """"""
            )
        }
    }
}