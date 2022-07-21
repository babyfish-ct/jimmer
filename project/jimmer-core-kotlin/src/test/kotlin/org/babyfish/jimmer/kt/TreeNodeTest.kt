package org.babyfish.jimmer.kt

import org.babyfish.jimmer.kt.model.TreeNode
import org.babyfish.jimmer.kt.model.addBy
import org.babyfish.jimmer.kt.model.by
import kotlin.test.Test
import kotlin.test.expect

class TreeNodeTest {
    
    @Test
    fun test() {

        val treeNode = new(TreeNode::class).by {
            name = "Root"
            childNodes().addBy {
                name = "Food"
                childNodes().addBy {
                    name = "Drink"
                    childNodes().addBy {
                        name = "Coco Cola"
                    }
                    childNodes().addBy {
                        name = "Fanta"
                    }
                }
            }
        }

        val newTreeNode = new(TreeNode::class).by(treeNode) {
            childNodes()[0]
                .childNodes()[0]
                .childNodes()[0]
                .name += " plus"
        }

        val treeNodeText = """{
            |--->"name":"Root",
            |--->"childNodes":[
            |--->--->{
            |--->--->--->"name":"Food",
            |--->--->--->"childNodes":[
            |--->--->--->--->{
            |--->--->--->--->--->"name":"Drink",
            |--->--->--->--->--->"childNodes":[
            |--->--->--->--->--->--->{"name":"Coco Cola"},
            |--->--->--->--->--->--->{"name":"Fanta"}
            |--->--->--->--->--->]
            |--->--->--->--->}
            |--->--->--->]
            |--->--->}
            |--->]
            |}""".trimMargin().toSimpleJson()

        val newTreeNodeText = """{
            |--->"name":"Root",
            |--->"childNodes":[
            |--->--->{
            |--->--->--->"name":"Food",
            |--->--->--->"childNodes":[
            |--->--->--->--->{
            |--->--->--->--->--->"name":"Drink",
            |--->--->--->--->--->"childNodes":[
            |--->--->--->--->--->--->{"name":"Coco Cola plus"},
            |--->--->--->--->--->--->{"name":"Fanta"}
            |--->--->--->--->--->]
            |--->--->--->--->}
            |--->--->--->]
            |--->--->}
            |--->]
            |}""".trimMargin().toSimpleJson()

        expect(treeNodeText) { treeNode.toString() }
        expect(newTreeNodeText) { newTreeNode.toString() }
    }
}