package org.babyfish.jimmer.sql.kt.util

import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookInput
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertIsNot

class Issue969Test {

    @Test
    fun testIssue969() {
        val input = BookInput(
            name = "Book",
            edition = 1,
            price = BigDecimal("23.3"),
            storeId = 1L,
            authorIds = listOf(1L)
        )
        val book = input.toEntity {
            store()
            authors()
        }
        assertIsNot<Draft>(book.store)
        for (author in book.authors) {
            assertIsNot<Draft>(author)
        }
        assertContent(
            """{"name":"Book",
                |"edition":1,
                |"price":23.3,
                |"store":{"id":1},
                |"authors":[{"id":1}]}""".trimMargin(),
            book
        )
    }

    @Test
    fun testChanged() {
        val input = BookInput(
            name = "Book",
            edition = 1,
            price = BigDecimal("23.3"),
            storeId = 1L,
            authorIds = listOf(1L)
        )
        val book = input.toEntity {
            store().name = "MANNING"
            authors()[0].firstName = "Alex"
        }
        assertIsNot<Draft>(book.store)
        for (author in book.authors) {
            assertIsNot<Draft>(author)
        }
        assertContent(
            """{"name":"Book",
                |"edition":1,
                |"price":23.3,
                |"store":{"id":1,"name":"MANNING"},
                |"authors":[{"id":1,"firstName":"Alex"}]}""".trimMargin(),
            book
        )
    }
}