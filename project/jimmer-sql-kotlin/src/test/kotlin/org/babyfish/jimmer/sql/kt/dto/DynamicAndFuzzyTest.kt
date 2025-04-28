package org.babyfish.jimmer.sql.kt.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.DynamicBookInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.DynamicBookInput2
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.FuzzyBookInput
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.DynamicBookStoreInput
import org.junit.Test
import java.math.BigDecimal


class DynamicAndFuzzyTest {

    @Test
    fun testByDynamicBookStoreInput() {
        val input = DynamicBookStoreInput(name = "MANNING", isWebsiteLoaded = false)
        assertContent(
            "{\"name\":\"MANNING\"}",
            input.toEntity()
        )
        val store = jacksonObjectMapper().readValue(
            "{\"name\":\"MANNING\"}",
            DynamicBookStoreInput::class.java
        ).toEntity()
        assertContent(
            "{\"name\":\"MANNING\"}",
            store
        )
    }

    @Test
    fun testNullByDynamicInput() {
        val input = DynamicBookInput()
        assertContent(
            "{}",
            input.toEntity()
        )
        val book = jacksonObjectMapper().readValue(
            "{}",
            DynamicBookInput::class.java
        ).toEntity()
        assertContent("{}", book)
    }

    @Test
    fun testNonNullByDynamicInput() {
        val input: DynamicBookInput =
            DynamicBookInput(
                name = "Book",
                edition = 7,
                price = BigDecimal("59.99"),
                storeId = 3L
            )
        assertContent(
            "{" +
                "--->\"name\":\"Book\"," +
                "--->\"edition\":7," +
                "--->\"price\":59.99," +
                "--->\"store\":{" +
                "--->--->\"id\":3" +
                "--->}" +
                "}",
            input.toEntity().toString()
        )
        val book = jacksonObjectMapper()
            .readValue(
                "{" +
                    "\"name\":\"Book\"," +
                    "\"edition\":7," +
                    "\"price\":59.99," +
                    "\"storeId\":3" +
                    "}",
                DynamicBookInput::class.java
            ).toEntity()
        assertContent(
            "{" +
                "--->\"name\":\"Book\"," +
                "--->\"edition\":7," +
                "--->\"price\":59.99," +
                "--->\"store\":{" +
                "--->--->\"id\":3" +
                "--->}" +
                "}",
            book
        )
    }

    @Test
    fun testNullByDynamicInput2() {
        val input = DynamicBookInput2(
            parentName = "MANNING",
            parentWebsite = null,
            isParentWebsiteLoaded = true
        )
        assertContent(
            "{\"store\":{\"name\":\"MANNING\",\"website\":null}}",
            input.toEntity()
        )
        val book = jacksonObjectMapper()
            .readValue<DynamicBookInput2>("{\"parentName\":\"MANNING\",\"parentWebsite\":null}")
            .toEntity()
        assertContent(
            "{\"store\":{\"name\":\"MANNING\",\"website\":null}}",
            book
        )
    }

    @Test
    fun testNonNullByDynamicInput2() {
        val input = DynamicBookInput2(
            name = "Book",
            edition = 7,
            price = BigDecimal("59.99"),
            parentName = "Store",
            parentWebsite = "https://www.store.com",
        )
        assertContent(
            "{" +
                "--->\"name\":\"Book\"," +
                "--->\"edition\":7," +
                "--->\"price\":59.99," +
                "--->\"store\":{" +
                "--->--->\"name\":\"Store\"," +
                "--->--->\"website\":\"https://www.store.com\"" +
                "--->}" +
                "}",
            input.toEntity().toString()
        )
        val book = jacksonObjectMapper()
            .readValue<DynamicBookInput2>(
                "{" +
                    "\"name\":\"Book\"," +
                    "\"edition\":7," +
                    "\"price\":59.99," +
                    "\"parentName\":\"Store\"," +
                    "\"parentWebsite\":\"https://www.store.com\"" +
                    "}"
            ).toEntity()
        assertContent(
            "{" +
                "--->\"name\":\"Book\"," +
                "--->\"edition\":7," +
                "--->\"price\":59.99," +
                "--->\"store\":{" +
                "--->--->\"name\":\"Store\"," +
                "--->--->\"website\":\"https://www.store.com\"" +
                "--->}" +
                "}",
            book
        )
    }

    @Test
    fun testIssue994() {
        val input = DynamicBookInput(name = "MANNING")
        assertContent(
            "{\"name\":\"MANNING\"}",
            ObjectMapper().writeValueAsString(input)
        )
        val book = jacksonObjectMapper()
            .readValue<DynamicBookInput>("{\"name\":\"MANNING\"}")
            .toEntity()
        assertContent(
            "{\"name\":\"MANNING\"}",
            book
        )
    }

    @Test
    fun testFuzzyInput() {
        val input = FuzzyBookInput(name = "SQL in Action")
        assertContent(
            "{\"name\":\"SQL in Action\"," +
                "\"edition\":null," +
                "\"price\":null," +
                "\"storeId\":null," +
                "\"authorIds\":null}",
            jacksonObjectMapper().writeValueAsString(input)
        )
        val book = jacksonObjectMapper()
            .readValue<FuzzyBookInput>(
                "{\"name\":\"SQL in Action\"," +
                    "\"edition\":null," +
                    "\"price\":null," +
                    "\"storeId\":null," +
                    "\"authorIds\":null}",
            ).toEntity()
        assertContent("{\"name\":\"SQL in Action\"}", book)
    }
}

