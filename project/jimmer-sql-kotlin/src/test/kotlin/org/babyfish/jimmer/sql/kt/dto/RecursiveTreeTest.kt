package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.TreeNode
import org.babyfish.jimmer.sql.kt.model.addBy
import org.babyfish.jimmer.sql.kt.model.by
import org.babyfish.jimmer.sql.kt.model.dto.RecursiveTree
import org.junit.Test
import kotlin.test.expect

class RecursiveTreeTest {

    @Test
    fun testDtoToEntity() {
        val tree = RecursiveTree(
            name = "food",
            childNodes = listOf(
                RecursiveTree.TargetOf_childNodes(
                    name = "drinks",
                    childNodes = listOf(
                        RecursiveTree.TargetOf_childNodes(
                            name = "cocacola",
                            childNodes = null
                        ),
                        RecursiveTree.TargetOf_childNodes(
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
            "RecursiveTree(" +
                "--->name=food, " +
                "--->childNodes=[" +
                "--->--->TargetOf_childNodes(" +
                "--->--->--->name=drinks, " +
                "--->--->--->childNodes=[" +
                "--->--->--->--->TargetOf_childNodes(name=cocacola, childNodes=null), " +
                "--->--->--->--->TargetOf_childNodes(name=fenta, childNodes=null)" +
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
        assertContentEquals(
            "RecursiveTree(" +
                "--->name=food, " +
                "--->childNodes=[" +
                "--->--->TargetOf_childNodes(" +
                "--->--->--->name=drinks, " +
                "--->--->--->childNodes=[" +
                "--->--->--->--->TargetOf_childNodes(name=cocacola, childNodes=[]), " +
                "--->--->--->--->TargetOf_childNodes(name=fenta, childNodes=[])" +
                "--->--->--->]" +
                "--->--->)" +
                "--->]" +
                ")",
            RecursiveTree(treeNode)
        )
    }
}