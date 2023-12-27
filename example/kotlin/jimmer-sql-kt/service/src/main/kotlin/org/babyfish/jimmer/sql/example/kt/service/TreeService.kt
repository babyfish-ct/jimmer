package org.babyfish.jimmer.sql.example.kt.service

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.sql.example.kt.repository.TreeNodeRepository
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.example.kt.model.TreeNode
import org.babyfish.jimmer.sql.example.kt.model.by
import org.babyfish.jimmer.sql.example.kt.service.dto.FlatTreeNodeView
import org.babyfish.jimmer.sql.example.kt.service.dto.RecursiveTreeInput
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.runtime.SaveErrorCode
import org.babyfish.jimmer.sql.runtime.SaveException
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

/*
 * Why add spring web annotations to the service class?
 *
 * The success and popularity of rich client technologies represented by React, Vue and Angular
 * have greatly reduced the significance of the Controller layer on the spring server side.
 *
 * Moreover, over-bloated code structures are not conducive to demonstrating the capabilities
 * of the framework with small examples. Therefore, this example project no longer adheres to
 * dogmatism and directly adds spring web annotations to the service class.
 */
@RestController
@RequestMapping("/tree")
@Transactional
class TreeService(
    private val treeNodeRepository: TreeNodeRepository
) {

    @GetMapping("/flatNodes")
    fun findFlatNodes(
        @RequestParam(required = false) name: String?
    ): List<FlatTreeNodeView> =
        treeNodeRepository.findByNameLikeIgnoreCase(name, FlatTreeNodeView::class)

    @GetMapping("/roots/recursive")
    fun findRootTrees(
        @RequestParam(required = false) rootName: String?
    ): List<@FetchBy("RECURSIVE_FETCHER") TreeNode> = // ❶
        treeNodeRepository.findByParentIsNullAndName(rootName, RECURSIVE_FETCHER)

    @PutMapping("/root/recursive")
    @Throws(SaveException::class)
    fun saveTree(
        @RequestBody input: RecursiveTreeInput // ❷
    ): TreeNode {
        val treeNode = new(TreeNode::class).by(
            input.toEntity()
        ) {
            /*
             * `TreeNode` has two key properties: `name` and `parent`,
             * this means `name` and `parent` must be specified when `id` is missing.
             *
             * One-to-many association is special, parent object can specify the
             * many-to-one association of its child objects implicitly.
             * In this demo, Associations named `childNodes` specify `parent`
             * for child objects implicitly so that all child objects do not require
             * the `parent`.
             *
             * However, the `parent` of ROOT cannot be specified implicitly,
             * so that it must be specified manually
             */
            parent = null
        }
        return treeNodeRepository.save(treeNode)
    }

    @DeleteMapping("/tree/{id}")
    fun deleteTree(@PathVariable id: Long) {
        treeNodeRepository.deleteById(id)
    }

    companion object {

        private val RECURSIVE_FETCHER = newFetcher(TreeNode::class).by {
            allScalarFields()
            childNodes({
                recursive()
            }) {
                allScalarFields()
            }
        }
    }
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/spring/client/api#declare-fetchby
  https://babyfish-ct.github.io/jimmer/docs/query/object-fetcher/recursive

❷ https://babyfish-ct.github.io/jimmer/docs/mutation/save-command/input-dto/
  https://babyfish-ct.github.io/jimmer/docs/object/view/dto-language#92-recursive-association
---------------------------------------------------*/
