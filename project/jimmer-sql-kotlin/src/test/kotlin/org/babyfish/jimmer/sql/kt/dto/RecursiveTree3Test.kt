package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.TreeNode
import org.babyfish.jimmer.sql.kt.model.id
import org.babyfish.jimmer.sql.kt.model.mydto.RecursiveTree3
import org.junit.Test

class RecursiveTree3Test : AbstractQueryTest() {

    @Test
    fun test() {
        executeAndExpect(
            sqlClient.createQuery(TreeNode::class) {
                where(table.id eq 10L)
                select(table.fetch(RecursiveTree3::class))
            }
        ) {
            sql(
                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NODE_ID = ?"
            ).variables(10L)
            statement(1).sql(
                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NODE_ID = ?"
            ).variables(9L)
            statement(2).sql(
                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NODE_ID = ?"
            ).variables(1L)
            statement(3).sql(
                "select tb_1_.NODE_ID, tb_1_.NAME " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.PARENT_ID = ? " +
                    "order by tb_1_.NODE_ID asc"
            ).variables(10L)
            statement(4).sql(
                "select " +
                    "tb_1_.PARENT_ID, " +
                    "tb_1_.NODE_ID, tb_1_.NAME " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.PARENT_ID in (?, ?) " +
                    "order by tb_1_.NODE_ID asc"
            ).variables(11L, 15L)
            statement(5).sql(
                "select " +
                    "tb_1_.PARENT_ID, " +
                    "tb_1_.NODE_ID, tb_1_.NAME " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?) " +
                    "order by tb_1_.NODE_ID asc"
            ).variables(12L, 13L, 14L, 16L, 17L)
            row(0) {
                assertContentEquals(
                    ("RecursiveTree3(" +
                        "--->id=10, " +
                        "--->name=Woman, " +
                        "--->parent=RecursiveTree3(" +
                        "--->--->id=9, " +
                        "--->--->name=Clothing, " +
                        "--->--->parent=RecursiveTree3(" +
                        "--->--->--->id=1, " +
                        "--->--->--->name=Home, " +
                        "--->--->--->parent=null, " +
                        "--->--->--->childNodes=null" +
                        "--->--->), " +
                        "--->--->childNodes=null" +
                        "--->), " +
                        "--->childNodes=[" +
                        "--->--->RecursiveTree3(" +
                        "--->--->--->id=11, " +
                        "--->--->--->name=Casual wear, " +
                        "--->--->--->parent=null, " +
                        "--->--->--->childNodes=[" +
                        "--->--->--->--->RecursiveTree3(" +
                        "--->--->--->--->--->id=12, name=Dress, parent=null, childNodes=[]" +
                        "--->--->--->--->), " +
                        "--->--->--->--->RecursiveTree3(" +
                        "--->--->--->--->--->id=13, name=Miniskirt, parent=null, childNodes=[]" +
                        "--->--->--->--->), " +
                        "--->--->--->--->RecursiveTree3(" +
                        "--->--->--->--->--->id=14, name=Jeans, parent=null, childNodes=[])" +
                        "--->--->--->]" +
                        "--->--->), " +
                        "--->--->RecursiveTree3(" +
                        "--->--->--->id=15, " +
                        "--->--->--->name=Formal wear, " +
                        "--->--->--->parent=null, " +
                        "--->--->--->childNodes=[" +
                        "--->--->--->--->RecursiveTree3(" +
                        "--->--->--->--->--->id=16, name=Suit, parent=null, childNodes=[]" +
                        "--->--->--->--->), " +
                        "--->--->--->--->RecursiveTree3(" +
                        "--->--->--->--->--->id=17, name=Shirt, parent=null, childNodes=[]" +
                        "--->--->--->--->)" +
                        "--->--->--->]" +
                        "--->--->)" +
                        "--->]" +
                        ")"),
                    it.toString()
                )
            }
        }
    }
}