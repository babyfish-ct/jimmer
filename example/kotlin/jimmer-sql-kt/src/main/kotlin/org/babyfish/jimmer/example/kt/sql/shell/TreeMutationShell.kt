package org.babyfish.jimmer.example.kt.sql.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.babyfish.jimmer.example.kt.sql.model.TreeNode
import org.babyfish.jimmer.example.kt.sql.model.TreeNodeDraft
import org.babyfish.jimmer.example.kt.sql.model.addBy
import org.babyfish.jimmer.example.kt.sql.model.by
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption


@ShellComponent
class TreeMutationShell(
    private val sqlClient: KSqlClient,
    private val prettyWriter: ObjectWriter
) {

    @ShellMethod("Save tree into database(Example: save-tree --root-name Hello)")
    fun saveTree(
        rootName: String,
        @ShellOption(defaultValue = "2") depth: Int,
        @ShellOption(defaultValue = "2") breadth: Int
    ) {
        val rootNode = new(TreeNode::class).by {
            name = rootName
            parent = null
            createChildNodes(
                this,
                0,
                depth,
                breadth
            )
        }
        println(
            "save the tree " +
                prettyWriter.writeValueAsString(rootNode) +
                " into database"
        )
        sqlClient
            .entities
            .save(rootNode) {
                // If child nodes do not exist, insert them automatically
                setAutoAttachingAll()

                // Need not `setAutoDetaching` because `on cascade delete`
                // is configured on model interface,
                // If user update a tree with less child nodes,
                // abandoned child nodes will be deleted automatically
            }
    }

    private fun createChildNodes(
        parent: TreeNodeDraft,
        currentDepth: Int,
        maxDepth: Int,
        childCount: Int
    ) {
        if (currentDepth >= maxDepth) {
            parent.childNodes = emptyList()
            return
        }
        val prefix: String = parent.name + "-"
        for (i in 1..childCount) {
            parent.childNodes().addBy {
                name = prefix + i
                createChildNodes(
                    this,
                    currentDepth + 1,
                    maxDepth,
                    childCount
                )
            }
        }
    }

    @ShellMethod("Delete tree by id(Example: save-tree --id 100)")
    fun deleteTree(id: Long) {
        sqlClient.entities.delete(TreeNode::class, id)
    }
}