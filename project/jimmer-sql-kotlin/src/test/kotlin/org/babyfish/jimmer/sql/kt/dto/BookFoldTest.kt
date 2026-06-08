package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFoldInput
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFoldInsideFlatView
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFoldSpecification
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookFoldView
import org.junit.Test
import java.math.BigDecimal

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
    fun testFoldSpecification() {
        val specification = BookFoldSpecification(
            summary = BookFoldSpecification.TargetOf_summary(
                name = "GraphQL",
                minPrice = BigDecimal("40"),
                maxPrice = BigDecimal("60")
            )
        )
        executeAndExpect(
            sqlClient {
                setDialect(H2Dialect())
            }.createQuery(Book::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where tb_1_.NAME ilike ? and tb_1_.PRICE >= ? and tb_1_.PRICE <= ?""".trimMargin()
            )
        }
    }
}
