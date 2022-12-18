package org.babyfish.jimmer.example.kt.sql.bll

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.table.isNull
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
class TreeController(
    private val sqlClient: KSqlClient,
    private val objectMapper: ObjectMapper
) {

    @GetMapping("/trees")
    fun trees(
        @RequestParam(defaultValue = "") rootName: String,
        @RequestParam(defaultValue = "") noRecursiveNames: String
    ): List<TreeNode> {

        val excludedNames =
            if (noRecursiveNames.isEmpty()) {
                emptySet()
            } else {
                noRecursiveNames
                    .trim()
                    .split("\\s*,\\s*")
                    .map {
                        it.lowercase()
                    }
                    .toSet()
            }

        return sqlClient
            .createQuery(TreeNode::class) {
                where(table.parent.isNull())
                rootName.takeIf { it.isNotEmpty() }?.let {
                    where(table.name ilike it)
                }
                orderBy(table.name)
                select(
                    table.fetchBy {
                        allScalarFields()
                        childNodes({
                            recursive {
                                !excludedNames.contains(
                                    entity.name.lowercase()
                                )
                            }
                        }) {
                            allScalarFields()
                        }
                    }
                )
            }
            .execute()
    }

    @PutMapping("/tree")
    fun saveTree(
        @RequestParam rootName: String,
        @RequestParam(defaultValue = "2") depth: Int,
        @RequestParam(defaultValue = "2") breadth: Int
    ): TreeNode {

        if (rootName.isEmpty()) {
            throw IllegalArgumentException("Illegal rootName")
        }
        if (depth > 5) {
            throw IllegalArgumentException("depth is too big")
        }
        if (breadth > 5) {
            throw IllegalArgumentException("breadth is too big")
        }

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
        if (LOGGER.isInfoEnabled) {
            LOGGER.info(
                "save the tree " +
                    objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(rootNode) +
                    " into database"
            )
        }
        return sqlClient
            .entities
            .save(rootNode) {
                // If child nodes do not exist, insert them automatically
                setAutoAttachingAll()

                // Need not `setAutoDetaching` because `on cascade delete`
                // is configured on model interface,
                // If user update a tree with less child nodes,
                // abandoned child nodes will be deleted automatically
            }
            .modifiedEntity
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

    @DeleteMapping("/tree/{id}")
    fun deleteTree(@PathVariable id: Long) {
        sqlClient.entities.delete(TreeNode::class, id)
    }
}

private val LOGGER = LoggerFactory.getLogger(TreeController::class.java)