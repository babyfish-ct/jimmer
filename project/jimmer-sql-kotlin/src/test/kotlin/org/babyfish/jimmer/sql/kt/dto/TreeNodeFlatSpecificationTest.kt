package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.TreeNode
import org.babyfish.jimmer.sql.kt.model.bug1126.WorkUser
import org.babyfish.jimmer.sql.kt.model.bug1126.dto.UserSpec
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

    @Test
    fun test2() {
        executeAndExpect(
            sqlClient.createQuery(WorkUser::class) {
                where(UserSpec())
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME from WORK_USER tb_1_"""
            )
        }
    }
}