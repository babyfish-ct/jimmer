package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
interface TreeNodeService {

    @GetMapping
    fun findRootTreeNodes(): List<@FetchBy("RECURSIVE_FETCHER") TreeNode>

    companion object {

        @JvmStatic
        val RECURSIVE_FETCHER = newFetcher(TreeNode::class).by {
            allScalarFields()
            `childNodes*`()
        }
    }
}