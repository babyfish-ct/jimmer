package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookDynamicFoldInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFixedFoldInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFoldInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFoldInsideFlatView
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFoldSpecification
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFoldView
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFuzzyFoldInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookNestedFoldView
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookNullableFoldInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookNullableFoldView
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookStaticFoldInput
import org.babyfish.jimmer.sql.kt.model.classic.book.id
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertNull

class BookFoldTest : AbstractQueryTest() {

    @Test
    fun testFoldView() {
        val view = BookFoldView(
            id = 1L,
            summary = BookFoldView.TargetOf_summary(
                name = "Programming TypeScript",
                edition = 2
            )
        )
        assertContent(
            """{"id":1,"summary":{"name":"Programming TypeScript","edition":2}}""",
            jsonCodec().writer().writeAsString(view)
        )
    }

    @Test
    fun testNullableFoldView() {
        val view = BookNullableFoldView(
            id = 1L,
            summary = null
        )
        assertContent(
            """{"id":1,"summary":null}""",
            jsonCodec().writer().writeAsString(view)
        )
    }

    @Test
    fun testNestedFoldView() {
        val view = BookNestedFoldView(
            id = 12L,
            name = "GraphQL in Action",
            summary = BookNestedFoldView.TargetOf_summary(
                name = "GraphQL in Action",
                detail = BookNestedFoldView.TargetOf_summary.TargetOf_detail(
                    name = "GraphQL in Action",
                    edition = 3
                )
            )
        )
        assertContent(
            """{
                |--->"id":12,
                |--->"name":"GraphQL in Action",
                |--->"summary":{
                |--->--->"name":"GraphQL in Action",
                |--->--->"detail":{"name":"GraphQL in Action","edition":3}
                |--->}
                |}""".trimMargin(),
            jsonCodec().writer().writeAsString(view)
        )
    }

    @Test
    fun testFoldInsideFlatView() {
        val view = BookFoldInsideFlatView(
            id = 1L,
            storeKey = BookFoldInsideFlatView.TargetOf_storeKey(
                name = "MANNING"
            )
        )
        assertContent(
            """{"id":1,"storeKey":{"name":"MANNING"}}""",
            jsonCodec().writer().writeAsString(view)
        )
    }

    @Test
    fun testFoldInsideFlatViewFromEntityWithNullFlatHead() {
        val book = Book {
            id = 1L
            name = "Programming TypeScript"
            edition = 2
            price = BigDecimal("59.99")
        }

        val view = BookFoldInsideFlatView(book)

        assertNull(view.storeKey)
    }

    @Test
    fun testNullableFoldInputWithoutSummary() {
        val input = BookNullableFoldInput(
            id = 1L,
            summary = null
        )
        assertContent(
            """{"id":1}""",
            input.toEntity()
        )
    }

    @Test
    fun testFoldInput() {
        val input = BookFoldInput(
            id = 1L,
            summary = BookFoldInput.TargetOf_summary(
                name = "SQL in Action",
                edition = 1
            )
        )
        assertContent(
            """{"id":1,"name":"SQL in Action","edition":1}""",
            input.toEntity()
        )
        assertContent(
            "BookFoldInput(id=1, summary=BookFoldInput.TargetOf_summary(name=SQL in Action, edition=1))",
            BookFoldInput(input.toEntity())
        )
    }

    @Test
    fun testFoldInputStrategies() {
        assertBookFoldEntity(
            BookFixedFoldInput(
                id = 1L,
                summary = BookFixedFoldInput.TargetOf_summary(
                    name = "SQL in Action",
                    edition = 1
                )
            ).toEntity()
        )
        assertBookFoldEntity(
            BookStaticFoldInput(
                id = 1L,
                summary = BookStaticFoldInput.TargetOf_summary(
                    name = "SQL in Action",
                    edition = 1
                )
            ).toEntity()
        )
        assertBookFoldEntity(
            BookDynamicFoldInput(
                id = 1L,
                summary = BookDynamicFoldInput.TargetOf_summary(
                    name = "SQL in Action",
                    edition = 1
                )
            ).toEntity()
        )
        assertBookFoldEntity(
            BookFuzzyFoldInput(
                id = 1L,
                summary = BookFuzzyFoldInput.TargetOf_summary(
                    name = "SQL in Action",
                    edition = 1
                )
            ).toEntity()
        )
    }

    @Test
    fun testFoldSpecificationWithoutValues() {
        val specification = BookFoldSpecification()
        executeAndExpect(
            sqlClient {
                setDialect(H2Dialect())
            }.createQuery(Book::class) {
                where(specification)
                orderBy(table.id)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |order by tb_1_.ID asc""".trimMargin()
            )
        }
    }

    @Test
    fun testFoldSpecificationWithValues() {
        val specification = BookFoldSpecification(
            summary = BookFoldSpecification.TargetOf_summary(
                name = "GraphQL",
                minPrice = BigDecimal("70"),
                maxPrice = BigDecimal("90")
            )
        )
        executeAndExpect(
            sqlClient {
                setDialect(H2Dialect())
            }.createQuery(Book::class) {
                where(specification)
                orderBy(table.id)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where tb_1_.NAME ilike ? and tb_1_.PRICE >= ? and tb_1_.PRICE <= ? 
                    |order by tb_1_.ID asc""".trimMargin()
            ).variables("%graphql%", BigDecimal("70"), BigDecimal("90"))
            rows {
                assertContent(
                    """[
                        |--->{"id":10,"name":"GraphQL in Action","edition":1,"price":80.00,"storeId":2}, 
                        |--->{"id":11,"name":"GraphQL in Action","edition":2,"price":81.00,"storeId":2}, 
                        |--->{"id":12,"name":"GraphQL in Action","edition":3,"price":80.00,"storeId":2}
                        |]""".trimMargin(),
                    it
                )
            }
        }
    }

    private fun assertBookFoldEntity(book: Book) {
        assertContent(
            """{"id":1,"name":"SQL in Action","edition":1}""",
            book
        )
    }
}
