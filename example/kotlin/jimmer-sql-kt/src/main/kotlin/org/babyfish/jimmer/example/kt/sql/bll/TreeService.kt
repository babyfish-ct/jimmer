package org.babyfish.jimmer.example.kt.sql.bll

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.example.kt.sql.dal.TreeNodeRepository
import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*

@RestController
class TreeService(
    private val objectMapper: ObjectMapper,
    private val treeNodeRepository: TreeNodeRepository
) {

    @Transactional
    @GetMapping("/rootNodes")
    fun findRootNodes(
        @RequestParam(required = false) rootName: String,
        @RequestParam(required = false) noRecursiveNames: String
    ): List<TreeNode> =
        treeNodeRepository.findRootNodes(rootName, null)

    @Transactional
    @GetMapping("/rootTrees")
    fun findRootTrees(
        @RequestParam(defaultValue = "") rootName: String,
        @RequestParam(defaultValue = "") noRecursiveNames: String
    ): List<TreeNode> =
        treeNodeRepository.findRootNodes(
            rootName,
            if (!StringUtils.hasText(noRecursiveNames)) {
                RECURSIVE_TREE_NODE_FETCHER
            } else {
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
                newFetcher(TreeNode::class).by(RECURSIVE_TREE_NODE_FETCHER) {
                    childNodes({ // override `childNodes` of `RECURSIVE_TREE_NODE_FETCHER`
                        recursive {
                            !excludedNames.contains(
                                entity.name.lowercase()
                            )
                        }
                    }) {
                        allScalarFields()
                    }
                }
            }
        )

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
        return treeNodeRepository.save(rootNode)
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
        treeNodeRepository.deleteById(id)
    }

    companion object {

        private val RECURSIVE_TREE_NODE_FETCHER = newFetcher(TreeNode::class).by {
            allScalarFields()
            childNodes({
                recursive()
            }) {
                allScalarFields()
            }
        }

        private val LOGGER = LoggerFactory.getLogger(TreeService::class.java)
    }
}
