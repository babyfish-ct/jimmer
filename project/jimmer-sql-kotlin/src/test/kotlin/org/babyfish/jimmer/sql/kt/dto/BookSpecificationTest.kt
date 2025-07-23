package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.author.Gender
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookSpecification
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookSpecification2
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookSpecification3
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookSpecification4
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.dto.BookStoreSpecificationForIssue562
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class BookSpecificationTest : AbstractQueryTest() {

    @Test
    fun testSpecificationWithNullity() {
        val specification = BookSpecification(
            isStoreNotNull = true,
            tag1 = 0,
            tag2 = 0,
            tag3 = emptyMap(),
            tag4 = mutableMapOf(),
            tag5 = intArrayOf(),
            tag6 = emptyArray(),
            tag7 = LocalDateTime.now(),
            tag8 = Any(),
            tag9 = emptyArray<Any>(),
            tag10 = arrayOf(),
            tag11 = emptyList<Any>(),
            tag12 = mutableListOf<Any>(),
            tag13 = emptyList(),
            tag14 = mutableListOf()
        )
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where tb_1_.STORE_ID is not null""".trimMargin()
            )
        }
    }

    @Test
    fun testSpecification2WithoutValue() {
        val specification = BookSpecification2()
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID from BOOK tb_1_"""
            )
        }
    }

    @Test
    fun testSpecification2WithValue() {
        val specification = BookSpecification2(
            ids = listOf(1L, 2L, 3L, 10L, 11L, 12L),
            name = "GraphQL in Action",
            edition = 3,
            minPrice = BigDecimal(20),
            maxPrice = BigDecimal(50),
            storeIds = listOf(1L, 2L),
            excludedStoreIds = listOf(99998L, 99999L),
            authorIds = listOf(1, 2L, 3L),
            excludedAuthorIds = listOf(100000L, 100001L)
        )
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where 
                    |--->tb_1_.NAME = ? 
                    |and 
                    |--->tb_1_.EDITION = ? 
                    |and 
                    |--->tb_1_.ID in (?, ?, ?, ?, ?, ?) 
                    |and 
                    |--->tb_1_.PRICE >= ? 
                    |and 
                    |--->tb_1_.PRICE <= ? 
                    |and 
                    |--->tb_1_.STORE_ID in (?, ?) 
                    |and 
                    |--->not exists(
                    |--->--->select 1 
                    |--->--->from BOOK_STORE tb_3_ 
                    |--->--->inner join BOOK tb_4_ on tb_3_.ID = tb_4_.STORE_ID 
                    |--->--->where tb_4_.ID = tb_1_.ID and tb_3_.ID in (?, ?)
                    |--->) 
                    |and 
                    |--->exists(
                    |--->--->select 1 
                    |--->--->from AUTHOR tb_5_ 
                    |--->--->inner join BOOK_AUTHOR_MAPPING tb_6_ on tb_5_.ID = tb_6_.AUTHOR_ID 
                    |--->--->where tb_6_.BOOK_ID = tb_1_.ID and tb_5_.ID in (?, ?, ?)
                    |--->) 
                    |and 
                    |--->not exists(
                    |--->--->select 1 from AUTHOR tb_8_ 
                    |--->--->inner join BOOK_AUTHOR_MAPPING tb_9_ on tb_8_.ID = tb_9_.AUTHOR_ID 
                    |--->--->where tb_9_.BOOK_ID = tb_1_.ID and tb_8_.ID in (?, ?)
                    |--->)""".trimMargin()
            ).variables(
                "GraphQL in Action",
                3,
                1L,
                2L,
                3L,
                10L,
                11L,
                12L,
                BigDecimal(20),
                BigDecimal(50),
                1L,
                2L,
                99998L,
                99999L,
                1L,
                2L,
                3L,
                100000L,
                100001L
            )
        }
    }

    @Test
    fun testSpecification3WithoutValue() {
        val specification = BookSpecification3()
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID from BOOK tb_1_"""
            )
        }
    }

    @Test
    fun testSpecification3WithValue() {
        val specification = BookSpecification3(
            ids = listOf(1L, 2L, 3L, 10, 11L, 12L),
            edition = 3,
            name = "GraphQL in Action",
            minPrice = BigDecimal(20),
            maxPrice = BigDecimal(50),
            store = BookSpecification3.TargetOf_store(
                minName = "A",
                maxName = "X",
                version = 1,
                website = "https://www.manning.com",
            ),
            authors = BookSpecification3.TargetOf_authors(
                id = 3L,
                gender = Gender.MALE,
                name = "B"
            )
        )
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |where 
                    |--->tb_1_.NAME = ? 
                    |and 
                    |--->tb_1_.EDITION = ? 
                    |and 
                    |--->tb_1_.ID in (?, ?, ?, ?, ?, ?) 
                    |and 
                    |--->tb_1_.PRICE >= ? 
                    |and 
                    |--->tb_1_.PRICE <= ? 
                    |and 
                    |--->tb_2_.VERSION = ? 
                    |and 
                    |--->tb_2_.WEBSITE = ? 
                    |and 
                    |--->tb_2_.NAME >= ? 
                    |and 
                    |--->tb_2_.NAME <= ? 
                    |and exists(
                    |--->select 1 
                    |--->from AUTHOR tb_3_ 
                    |--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID 
                    |--->where 
                    |--->--->tb_1_.ID = tb_4_.BOOK_ID 
                    |--->and 
                    |--->--->tb_3_.ID = ? 
                    |--->and 
                    |--->--->tb_3_.GENDER = ? 
                    |--->and 
                    |--->--->(lower(tb_3_.FIRST_NAME) like ? or lower(tb_3_.LAST_NAME) like ?)
                    |--->)""".trimMargin()
            ).variables(
                "GraphQL in Action",
                3,
                1L,
                2L,
                3L,
                10L,
                11L,
                12L,
                BigDecimal(20),
                BigDecimal(50),
                1,
                "https://www.manning.com",
                "A",
                "X",
                3L,
                "M",
                "%b%",
                "%b%"
            )
        }
    }

    @Test
    fun testSpecification4WithoutValue() {
        val specification = BookSpecification4()
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID from BOOK tb_1_"""
            )
        }
    }

    @Test
    fun testSpecification4WithValue() {
        val specification = BookSpecification4(
            ids = listOf(1L, 2L, 3L, 10, 11L, 12L),
            edition = 3,
            name = "GraphQL in Action",
            minPrice = BigDecimal(20),
            maxPrice = BigDecimal(50),
            parentMinName = "A",
            parentMaxName = "X",
            authorName = "B",
        )
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |where 
                    |--->tb_1_.NAME = ? 
                    |and 
                    |--->tb_1_.EDITION = ? 
                    |and 
                    |--->tb_1_.ID in (?, ?, ?, ?, ?, ?) 
                    |and 
                    |--->tb_1_.PRICE >= ? 
                    |and 
                    |--->tb_1_.PRICE <= ? 
                    |and 
                    |--->tb_2_.NAME >= ? 
                    |and 
                    |--->tb_2_.NAME <= ? 
                    |and exists(
                    |--->select 1 
                    |--->from AUTHOR tb_3_ 
                    |--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID 
                    |--->where 
                    |--->--->tb_1_.ID = tb_4_.BOOK_ID 
                    |--->and 
                    |--->--->(lower(tb_3_.FIRST_NAME) like ? or lower(tb_3_.LAST_NAME) like ?)
                    |)""".trimMargin()
            ).variables(
                "GraphQL in Action",
                3,
                1L,
                2L,
                3L,
                10L,
                11L,
                12L,
                BigDecimal(20),
                BigDecimal(50),
                "A",
                "X",
                "%b%",
                "%b%"
            )
        }
    }

    @Test
    fun testIssue562() {
        val specification = BookStoreSpecificationForIssue562(
            name = "E",
            bookName = "G",
            bookAuthorFirstName = "A"
        )
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE tb_1_ 
                    |where lower(tb_1_.NAME) like ? and 
                    |exists(
                    |--->select 1 
                    |--->from BOOK tb_2_ 
                    |--->where 
                    |--->--->tb_1_.ID = tb_2_.STORE_ID 
                    |--->and 
                    |--->--->lower(tb_2_.NAME) like ? 
                    |--->and 
                    |--->--->exists(
                    |--->--->--->select 1 
                    |--->--->--->from AUTHOR tb_4_ 
                    |--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ 
                    |--->--->--->--->on tb_4_.ID = tb_5_.AUTHOR_ID 
                    |--->--->--->where tb_2_.ID = tb_5_.BOOK_ID 
                    |--->--->--->and lower(tb_4_.FIRST_NAME) like ?
                    |--->--->)
                    |)""".trimMargin()
            ).variables("%e%", "%g%", "%a%")
            rows(
                "[{\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null}]"
            )
        }
    }
}