package org.babyfish.jimmer.sql.example.business

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.ThrowsAll
import org.babyfish.jimmer.sql.example.repository.TreeNodeRepository
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.example.model.TreeNode
import org.babyfish.jimmer.sql.example.model.by
import org.babyfish.jimmer.sql.example.model.dto.FlatTreeNodeView
import org.babyfish.jimmer.sql.example.model.dto.RecursiveTreeInput
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.runtime.SaveErrorCode
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

/**
 * A real project should be a three-tier architecture consisting
 * of repository, service, and controller.
 *
 * This demo has no business logic, its purpose is only to tell users
 * how to use jimmer with the <b>least</b> code. Therefore, this demo
 * does not follow this convention, and let services be directly
 * decorated by `@RestController`, not `@Service`.
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
    @ThrowsAll(SaveErrorCode::class)
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
