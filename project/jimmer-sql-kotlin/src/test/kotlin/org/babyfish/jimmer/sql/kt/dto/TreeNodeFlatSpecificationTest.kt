package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.TreeNode
import org.babyfish.jimmer.sql.kt.model.dto.TreeNodeFlatSpecification
import kotlin.test.Test

class TreeNodeFlatSpecificationTest : AbstractQueryTest() {

    @Test
    fun test() {
        executeAndExpect(
            sqlClient.createQuery(TreeNode::class) {
                where(TreeNodeFlatSpecification())
                select(table)
            }
        ) {
            sql(
                """select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID from TREE_NODE tb_1_"""
            )
        }
    }
}