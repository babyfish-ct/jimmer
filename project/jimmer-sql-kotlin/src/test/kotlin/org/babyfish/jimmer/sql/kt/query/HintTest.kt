package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.NativeDatabases
import org.babyfish.jimmer.sql.kt.model.TreeNode
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.name
import org.babyfish.jimmer.sql.kt.model.name
import org.junit.jupiter.api.Assumptions
import kotlin.test.Test

class HintTest : AbstractQueryTest() {

    @Test
    fun testIssue1272() {
        Assumptions.assumeTrue {
            NativeDatabases.isNativeAllowed()
        }
        executeAndExpect(
            NativeDatabases.MYSQL_DATA_SOURCE,
            sqlClient.createQuery(TreeNode::class) {
                where(table.name eq "Coca Cola")
                select(table)
            }.hint("SET_VAR(query_timeout=3600)")
        ) {
            sql(
                """select /*+ SET_VAR(query_timeout=3600) */ 
                    |tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID 
                    |from TREE_NODE tb_1_ 
                    |where tb_1_.NAME = ?""".trimMargin()
            )
            rows(
                """[{"id":4,"name":"Coca Cola","parent":{"id":3}}]"""
            )
        }
    }
}