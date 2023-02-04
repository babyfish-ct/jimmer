package org.babyfish.jimmer.sql.example.bll

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.example.dal.TreeNodeRepository
import org.babyfish.jimmer.sql.example.model.TreeNode
import org.babyfish.jimmer.sql.example.model.by
import org.babyfish.jimmer.sql.example.model.dto.RecursiveTree
import org.babyfish.jimmer.sql.example.model.dto.RecursiveTreeInput
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tree")
@Transactional
class TreeService(
    private val treeNodeRepository: TreeNodeRepository
) {

    @GetMapping("/roots/recursive")
    fun findRootTrees(
        @RequestParam(required = false) rootName: String?
    ): List<RecursiveTree> =
        treeNodeRepository.findByParentIsNullAndName(rootName, RecursiveTree::class)

    @PutMapping("/root/recursive")
    fun saveTree(
        @RequestBody input: RecursiveTreeInput
    ): TreeNode {

        val rootNode = new(TreeNode::class).by(
            input.toEntity()
        ) {
            // `parent` must be loaded because it is a part of key
            parent = null
        }
        return treeNodeRepository.save(rootNode)
    }

    @DeleteMapping("/{id}")
    fun deleteTree(@PathVariable id: Long) {
        treeNodeRepository.deleteById(id)
    }
}
