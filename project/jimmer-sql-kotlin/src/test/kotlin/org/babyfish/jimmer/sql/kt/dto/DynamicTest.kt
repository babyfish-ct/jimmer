package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.DynamicBookInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.DynamicBookInput2
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.DynamicBookStoreInput
import org.junit.Test
import java.math.BigDecimal


class DynamicTest {

    @Test
    fun testByDynamicBookStoreInput() {
        val input = DynamicBookStoreInput(name = "MANNING")
        input.isWebsiteLoaded = false
        assertContent(
            "{\"name\":\"MANNING\"}",
            input.toEntity()
        )
    }

    @Test
    fun testNullByDynamicInput() {
        val input = DynamicBookInput()
        assertContent(
            "{}",
            input.toEntity()
        )
    }

    @Test
    fun testNonNullByDynamicInput() {
        val input: DynamicBookInput =
            DynamicBookInput()
        input.name = "Book"
        input.edition = 7
        input.price = BigDecimal("59.99")
        input.storeId = 3L
        assertContent(
            "{" +
                "--->\"name\":\"Book\"," +
                "--->\"edition\":7," +
                "--->\"price\":59.99,\"" +
                "--->store\":{" +
                "--->--->\"id\":3" +
                "--->}" +
                "}",
            input.toEntity().toString()
        )
    }

    @Test
    fun testNullByDynamicInput2() {
        val input = DynamicBookInput2()
        assertContent(
            "{}",
            input.toEntity()
        )
    }

    @Test
    fun testNonNullByDynamicInput2() {
        val input = DynamicBookInput2()
        input.name = "Book"
        input.edition = 7
        input.price = BigDecimal("59.99")
        input.parentName = "Store"
        input.parentWebsite = "https://www.store.com"
        assertContent(
            ("{" +
                "--->\"name\":\"Book\"," +
                "--->\"edition\":7," +
                "--->\"price\":59.99," +
                "--->\"store\":{" +
                "--->--->\"name\":\"Store\"," +
                "--->--->\"website\":\"https://www.store.com\"" +
                "--->}" +
                "}"),
            input.toEntity().toString()
        )
    }
}

