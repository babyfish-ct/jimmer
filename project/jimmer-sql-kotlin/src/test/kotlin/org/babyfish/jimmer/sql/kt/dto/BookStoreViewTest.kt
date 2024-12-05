package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.classic.book.addBy
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.BookStoreView
import kotlin.test.Test

class BookStoreViewTest {
    @Test
    fun testDtoToEntity() {
        val dto = BookStoreView(
            avgPrice = 1.toBigDecimal(),
            newestBooks = listOf(
                BookStoreView.TargetOf_newestBooks(
                    name = "book1",
                    edition = 1
                )
            ),
            newestBookIds = listOf(1)
        )
        assertContent(
            "{" +
                    "--->\"avgPrice\":1," +
                    "--->\"newestBooks\":[" +
                    "--->--->{" +
                    "--->--->--->\"name\":\"book1\"," +
                    "--->--->--->\"edition\":1" +
                    "--->--->}" +
                    "--->]," +
                    "--->\"newestBookIds\":[" +
                    "--->--->" + 1 +
                    "--->]" +
                    "}",
            dto.toEntity()
        )
    }

    @Test
    fun testEntityToDto() {
        val entity = BookStore {
            avgPrice = 1.toBigDecimal()
            newestBooks().addBy {
                name = "book1"
                edition = 1
            }
            newestBookIds = listOf(1)
        }

        val view = BookStoreView(entity)
        assertContent(
            "BookStoreView(" +
                    "--->avgPrice=1, " +
                    "--->newestBooks=[" +
                    "--->--->BookStoreView.TargetOf_newestBooks(" +
                    "--->--->--->name=book1, " +
                    "--->--->--->edition=1" +
                    "--->--->)" +
                    "--->], " +
                    "--->newestBookIds=[" +
                    "--->--->" + 1 +
                    "--->]" +
                    ")",
            view
        )
    }
}