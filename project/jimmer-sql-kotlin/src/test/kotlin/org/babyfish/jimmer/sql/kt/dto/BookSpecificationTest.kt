package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.author.Gender
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookSpecification2
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookSpecification3
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookSpecification4
import org.junit.Test
import java.math.BigDecimal

class BookSpecificationTest : AbstractQueryTest() {

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
        val specification = BookSpecification2()
        specification.ids = listOf(1L, 2L, 3L, 10L, 11L, 12L)
        specification.name = "GraphQL in Action"
        specification.edition = 3
        specification.minPrice = BigDecimal(20)
        specification.maxPrice = BigDecimal(50)
        specification.storeIds = listOf(1L, 2L)
        specification.excludedStoreIds = listOf(99998L, 99999L)
        specification.authorIds = listOf(1, 2L, 3L)
        specification.excludedAuthorIds = listOf(100000L, 100001L)
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_1_.ID = tb_3_.BOOK_ID 
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
                    |--->tb_1_.STORE_ID not in (?, ?) 
                    |and 
                    |--->tb_3_.AUTHOR_ID in (?, ?, ?) 
                    |and 
                    |--->tb_3_.AUTHOR_ID not in (?, ?)""".trimMargin()
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
        val specification = BookSpecification3()
        specification.ids = listOf(1L, 2L, 3L, 10, 11L, 12L)
        specification.edition = 3
        specification.name = "GraphQL in Action"
        specification.minPrice = BigDecimal(20)
        specification.maxPrice = BigDecimal(50)
        specification.store = BookSpecification3.TargetOf_store().apply {
            minName = "A"
            maxName = "X"
            version = 1
            website = "https://www.manning.com"
        }
        specification.authors = BookSpecification3.TargetOf_authors().apply {
            id = 3L
            gender = Gender.MALE
            name = "B"
        }
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
        val specification = BookSpecification4()
        specification.ids = listOf(1L, 2L, 3L, 10, 11L, 12L)
        specification.edition = 3
        specification.name = "GraphQL in Action"
        specification.minPrice = BigDecimal(20)
        specification.maxPrice = BigDecimal(50)
        specification.parentMinName = "A"
        specification.parentMaxName = "X"
        specification.authorName = "B"
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
}