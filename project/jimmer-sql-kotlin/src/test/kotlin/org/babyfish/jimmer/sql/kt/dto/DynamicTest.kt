package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.DynamicBookInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.DynamicBookInput2
import org.junit.Test
import java.math.BigDecimal
import java.util.*


class DynamicTest {
    @Test
    fun testNullByDynamicInput() {
        val input = DynamicBookInput()
        assertContentEquals(
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
        assertContentEquals(
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
        assertContentEquals(
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
        assertContentEquals(
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

