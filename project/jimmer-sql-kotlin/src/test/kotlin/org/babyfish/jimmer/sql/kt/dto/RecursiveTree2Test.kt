package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.*
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.model.classic.author.id
import org.babyfish.jimmer.sql.kt.model.mydto.RecursiveTree2
import org.junit.Test
import kotlin.test.assertFailsWith

class RecursiveTree2Test : AbstractQueryTest() {

    @Test
    fun testDtoToEntity() {
        val tree = RecursiveTree2(
            name = "food",
            childNodes = listOf(
                RecursiveTree2.TargetOf_childNodes(
                    name = "drinks",
                    childNodes = listOf(
                        RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2(
                            name = "cocacola",
                            childNodes = null
                        ),
                        RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2(
                            name = "fenta",
                            childNodes = null
                        )
                    )
                )
            )
        )

        assertContentEquals(
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
        assertContentEquals(
            "RecursiveTree2(" +
                "--->name=food, " +
                "--->childNodes=[" +
                "--->--->TargetOf_childNodes(" +
                "--->--->--->name=drinks, " +
                "--->--->--->childNodes=[" +
                "--->--->--->--->TargetOf_childNodes_2(name=cocacola, childNodes=null), " +
                "--->--->--->--->TargetOf_childNodes_2(name=fenta, childNodes=null)" +
                "--->--->--->]" +
                "--->--->)" +
                "--->]" +
                ")",
            RecursiveTree2(treeNode)
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
        assertContentEquals(
            "RecursiveTree2(" +
                "--->name=food, " +
                "--->childNodes=[" +
                "--->--->TargetOf_childNodes(" +
                "--->--->--->name=drinks, " +
                "--->--->--->childNodes=[" +
                "--->--->--->--->TargetOf_childNodes_2(name=cocacola, childNodes=[]), " +
                "--->--->--->--->TargetOf_childNodes_2(name=fenta, childNodes=[])" +
                "--->--->--->]" +
                "--->--->)" +
                "--->]" +
                ")",
            RecursiveTree2(treeNode)
        )
    }

    @Test
    fun testFindTree() {
        executeAndExpect(
            sqlClient.createQuery(TreeNode::class) {
                where(table.parentId.isNull())
                select(table.fetch(RecursiveTree2::class))
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

    @Test
    fun nonNullIsNull() {
        assertFailsWith<IllegalArgumentException> {
            sqlClient.createQuery(TreeNode::class) {
                where(table.parent.id.isNull())
                select(table)
            }
        }
    }
}