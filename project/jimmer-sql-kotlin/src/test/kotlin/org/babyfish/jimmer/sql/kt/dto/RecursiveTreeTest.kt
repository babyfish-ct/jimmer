package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.TreeNode
import org.babyfish.jimmer.sql.kt.model.addBy
import org.babyfish.jimmer.sql.kt.model.by
import org.babyfish.jimmer.sql.kt.model.dto.RecursiveTree
import org.babyfish.jimmer.sql.kt.model.parentId
import org.junit.Test

class RecursiveTreeTest : AbstractQueryTest() {

    @Test
    fun testDtoToEntity() {
        val tree = RecursiveTree(
            name = "food",
            childNodes = listOf(
                RecursiveTree(
                    name = "drinks",
                    childNodes = listOf(
                        RecursiveTree(
                            name = "cocacola",
                            childNodes = emptyList()
                        ),
                        RecursiveTree(
                            name = "fenta",
                            childNodes = emptyList()
                        )
                    )
                )
            )
        )

        assertContent(
            """{
                |--->"name":"food",
                |--->"childNodes":[
                |--->--->{
                |--->--->--->"name":"drinks",
                |--->--->--->"childNodes":[
                |--->--->--->--->{"name":"cocacola","childNodes":[]}
                |--->--->--->--->,{"name":"fenta","childNodes":[]}
                |--->--->--->]
                |--->--->}
                |--->]
                |}""".trimMargin(),
            tree.toEntity()
        )
    }

    @Test
    fun testEntityToDto() {
        val treeNode = new(TreeNode::class).by {
            name = "food"
            childNodes().addBy {
                name = "drinks"
                childNodes().addBy {
                    name = "cocacola"
                }
                childNodes().addBy {
                    name = "fenta"
                }
            }
        }
        assertContent(
            "RecursiveTree(" +
                "--->name=food, " +
                "--->childNodes=[" +
                "--->--->RecursiveTree(" +
                "--->--->--->name=drinks, " +
                "--->--->--->childNodes=[" +
                "--->--->--->--->RecursiveTree(name=cocacola, childNodes=null), " +
                "--->--->--->--->RecursiveTree(name=fenta, childNodes=null)" +
                "--->--->--->]" +
                "--->--->)" +
                "--->]" +
                ")",
            RecursiveTree(treeNode)
        )
    }

    @Test
    fun testEntityToDto2() {
        val treeNode = new(TreeNode::class).by {
            name = "food"
            childNodes().addBy {
                name = "drinks"
                childNodes().addBy {
                    name = "cocacola"
                    childNodes = emptyList()
                }
                childNodes().addBy {
                    name = "fenta"
                    childNodes = emptyList()
                }
            }
        }
        assertContent(
            "RecursiveTree(" +
                "--->name=food, " +
                "--->childNodes=[" +
                "--->--->RecursiveTree(" +
                "--->--->--->name=drinks, " +
                "--->--->--->childNodes=[" +
                "--->--->--->--->RecursiveTree(name=cocacola, childNodes=[]), " +
                "--->--->--->--->RecursiveTree(name=fenta, childNodes=[])" +
                "--->--->--->]" +
                "--->--->)" +
                "--->]" +
                ")",
            RecursiveTree(treeNode)
        )
    }

    @Test
    fun testFindTree() {
        executeAndExpect(
            sqlClient.createQuery(TreeNode::class) {
                where(table.parentId.isNull())
                select(table.fetch(RecursiveTree::class))
            }
        ) {
            sql(
                "select tb_1_.NODE_ID, tb_1_.NAME " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.PARENT_ID is null"
            )
            statement(1).sql(
                "select tb_1_.NODE_ID, tb_1_.NAME " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.PARENT_ID = ? " +
                    "order by tb_1_.NODE_ID asc"
            )
            statement(2).sql(
                "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.PARENT_ID in (?, ?) " +
                    "order by tb_1_.NODE_ID asc"
            )
            statement(3).sql(
                "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.PARENT_ID in (?, ?, ?, ?) " +
                    "order by tb_1_.NODE_ID asc"
            )
            statement(4).sql(
                "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "order by tb_1_.NODE_ID asc"
            )
            statement(5).sql(
                "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "order by tb_1_.NODE_ID asc"
            )
            rows(
                "[" +
                    "--->{" +
                    "--->--->\"name\":\"Home\"," +
                    "--->--->\"childNodes\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"name\":\"Food\"," +
                    "--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->{" +
                    "--->--->--->--->--->--->\"name\":\"Drinks\"," +
                    "--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->\"name\":\"Coca Cola\"," +
                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->}," +
                    "--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->\"name\":\"Fanta\"," +
                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->}" +
                    "--->--->--->--->--->--->]" +
                    "--->--->--->--->--->}," +
                    "--->--->--->--->--->{" +
                    "--->--->--->--->--->--->\"name\":\"Bread\"," +
                    "--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->\"name\":\"Baguette\"," +
                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->}," +
                    "--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->\"name\":\"Ciabatta\"," +
                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->}" +
                    "--->--->--->--->--->--->]" +
                    "--->--->--->--->--->}" +
                    "--->--->--->--->]" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"name\":\"Clothing\"," +
                    "--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->{" +
                    "--->--->--->--->--->--->\"name\":\"Woman\"," +
                    "--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->\"name\":\"Casual wear\"," +
                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Dress\"," +
                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->--->--->}," +
                    "--->--->--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Miniskirt\"," +
                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->--->--->}," +
                    "--->--->--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Jeans\"," +
                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->--->--->}" +
                    "--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->}," +
                    "--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->\"name\":\"Formal wear\"," +
                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Suit\"," +
                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->--->--->}," +
                    "--->--->--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Shirt\"," +
                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->--->--->}" +
                    "--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->}" +
                    "--->--->--->--->--->--->]" +
                    "--->--->--->--->--->}," +
                    "--->--->--->--->--->{" +
                    "--->--->--->--->--->--->\"name\":\"Man\"," +
                    "--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->\"name\":\"Casual wear\"," +
                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Jacket\"," +
                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->--->--->}," +
                    "--->--->--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Jeans\"," +
                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->--->--->}" +
                    "--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->}," +
                    "--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->\"name\":\"Formal wear\"," +
                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Suit\"," +
                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->--->--->}," +
                    "--->--->--->--->--->--->--->--->--->{" +
                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Shirt\"," +
                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                    "--->--->--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->--->--->}" +
                    "--->--->--->--->--->--->--->--->]" +
                    "--->--->--->--->--->--->--->}" +
                    "--->--->--->--->--->--->]" +
                    "--->--->--->--->--->}" +
                    "--->--->--->--->]" +
                    "--->--->--->}" +
                    "--->--->]" +
                    "--->}" +
                    "]"
            )
        }
    }
}