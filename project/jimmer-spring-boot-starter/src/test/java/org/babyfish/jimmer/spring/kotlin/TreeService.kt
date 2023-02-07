package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.spring.kotlin.dto.TreeDto
import org.babyfish.jimmer.spring.kotlin.dto.TreeInput
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TreeService(
    private val treeNodeRepository: TreeNodeRepository
) {

    @GetMapping
    fun findTrees(): List<TreeDto> =
        treeNodeRepository.findTreesByParentIsNull(TreeDto::class)

    @PutMapping
    fun saveTree(input: TreeInput): TreeNode =
        treeNodeRepository.save(input)
}