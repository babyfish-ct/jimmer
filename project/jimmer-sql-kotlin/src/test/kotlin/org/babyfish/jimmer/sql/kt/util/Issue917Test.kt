package org.babyfish.jimmer.sql.kt.util

import org.babyfish.jimmer.sql.kt.model.TreeNode
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import kotlin.test.Test
import kotlin.test.expect

class Issue917Test {

    @Test
    fun test() {
        TreeNode {
            parentId = 2L
            val text = this.toString()
            expect("""{"parent":{"id":2}}""") {
                text
            }
        }
    }
}