package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.author.*
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.authors
import org.babyfish.jimmer.sql.kt.model.classic.book.name
import org.babyfish.jimmer.sql.kt.model.classic.book.store
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.books
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import kotlin.test.Test

class VirtualPredicateTest : AbstractQueryTest() {

    @Test
    fun testMergeAnd() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where += table.authors {
                    firstName eq "Alex"
                }
                where += table.authors {
                    gender eq Gender.MALE
                }
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where exists(
                    |--->select 1 
                    |--->from AUTHOR tb_2_ 
                    |--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID 
                    |--->where 
                    |--->--->tb_3_.BOOK_ID = tb_1_.ID 
                    |--->and 
                    |--->--->tb_2_.FIRST_NAME = ? 
                    |--->and 
                    |--->--->tb_2_.GENDER = ?
                    |)""".trimMargin()
            )
        }
    }

    @Test
    fun testMergeOr() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    or(
                        table.authors {
                            firstName ilike "a"
                        },
                        table.authors {
                            lastName ilike "a"
                        },
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where exists(
                    |--->select 1 
                    |--->from AUTHOR tb_2_ 
                    |--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID 
                    |--->where 
                    |--->--->tb_3_.BOOK_ID = tb_1_.ID 
                    |--->and 
                    |--->--->(
                    |--->--->--->lower(tb_2_.FIRST_NAME) like ? 
                    |--->--->--->or 
                    |--->--->--->lower(tb_2_.LAST_NAME) like ?
                    |--->--->)
                    |)""".trimMargin()
            )
        }
    }

    @Test
    fun testMixed() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where += table.authors {
                    gender eq Gender.MALE
                }
                where(
                    or(
                        table.authors {
                            firstName ilike "a"
                        },
                        table.authors {
                            lastName ilike "a"
                        },
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where 
                    |--->exists(
                    |--->--->select 1 
                    |--->--->from AUTHOR tb_5_ 
                    |--->--->inner join BOOK_AUTHOR_MAPPING tb_6_ on tb_5_.ID = tb_6_.AUTHOR_ID 
                    |--->--->where 
                    |--->--->--->tb_6_.BOOK_ID = tb_1_.ID 
                    |--->--->and 
                    |--->--->--->tb_5_.GENDER = ?
                    |--->) 
                    |and 
                    |--->exists(
                    |--->--->select 1 from AUTHOR tb_2_ 
                    |--->--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID 
                    |--->--->where 
                    |--->--->--->tb_3_.BOOK_ID = tb_1_.ID 
                    |--->--->and (
                    |--->--->--->--->lower(tb_2_.FIRST_NAME) like ? 
                    |--->--->--->or 
                    |--->--->--->--->lower(tb_2_.LAST_NAME) like ?
                    |--->--->)
                    |--->)""".trimMargin()
            )
        }
    }

    @Test
    fun testDeep() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                where += table.books {
                    and(
                        name eq "GraphQL",
                        authors {
                            or(
                                firstName ilike "a",
                                lastName ilike "a"
                            )
                        }
                    )
                }
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE tb_1_ 
                    |where 
                    |--->exists(
                    |--->--->select 1 from BOOK tb_2_ 
                    |--->--->where 
                    |--->--->--->tb_2_.STORE_ID = tb_1_.ID 
                    |--->--->and 
                    |--->--->--->tb_2_.NAME = ? 
                    |--->--->and 
                    |--->--->--->exists(
                    |--->--->--->--->select 1 from AUTHOR tb_4_ 
                    |--->--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_4_.ID = tb_5_.AUTHOR_ID 
                    |--->--->--->--->where 
                    |--->--->--->--->--->tb_5_.BOOK_ID = tb_2_.ID 
                    |--->--->--->--->and (
                    |--->--->--->--->--->--->lower(tb_4_.FIRST_NAME) like ? 
                    |--->--->--->--->--->or 
                    |--->--->--->--->--->--->lower(tb_4_.LAST_NAME) like ?
                    |--->--->--->--->)
                    |--->--->--->)
                    |--->)""".trimMargin()
            )
        }
    }

    @Test
    fun testMixedDeep() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                where(
                    exists(
                        wildSubQuery(Book::class) {
                            where(table.store eq parentTable)
                            where(table.name eq "GraphQL")
                            where(table.authors {
                                or(
                                    firstName ilike "a",
                                    lastName ilike "a"
                                )
                            })
                        }
                    )
                )
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE tb_1_ 
                    |where 
                    |--->exists(
                    |--->--->select 1 from BOOK tb_2_ 
                    |--->--->where 
                    |--->--->--->tb_2_.STORE_ID = tb_1_.ID 
                    |--->--->and 
                    |--->--->--->tb_2_.NAME = ? 
                    |--->--->and 
                    |--->--->--->exists(
                    |--->--->--->--->select 1 from AUTHOR tb_4_ 
                    |--->--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_4_.ID = tb_5_.AUTHOR_ID 
                    |--->--->--->--->where 
                    |--->--->--->--->--->tb_5_.BOOK_ID = tb_2_.ID 
                    |--->--->--->--->and (
                    |--->--->--->--->--->--->lower(tb_4_.FIRST_NAME) like ? 
                    |--->--->--->--->--->or 
                    |--->--->--->--->--->--->lower(tb_4_.LAST_NAME) like ?
                    |--->--->--->--->)
                    |--->--->--->)
                    |--->)""".trimMargin()
            )
        }
    }

    @Test
    fun testIgnoreEmpty() {
        executeAndExpect(
            sqlClient
                .createQuery(BookStore::class) {
                    where(table.books { table.name `eq?` null })
                    select(table)
                }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                    |from BOOK_STORE tb_1_""".trimMargin()
            )
        }
    }

    @Test
    fun testTwo() {
        executeAndExpect(
            sqlClient
                .createQuery(Book::class) {
                    where += table.authors {
                        or(
                            firstName ilike "a",
                            lastName ilike "a"
                        )
                    }
                    where += table.authors {
                        gender eq Gender.MALE
                    }
                    select(table)
                }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where exists(" +
                    "--->select 1 " +
                    "--->from AUTHOR tb_2_ " +
                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                    "--->where " +
                    "--->--->tb_3_.BOOK_ID = tb_1_.ID " +
                    "--->and (" +
                    "--->--->--->lower(tb_2_.FIRST_NAME) like ? " +
                    "--->--->or " +
                    "--->--->--->lower(tb_2_.LAST_NAME) like ?" +
                    "--->) " +
                    "--->and " +
                    "--->--->tb_2_.GENDER = ?" +
                    ")"
            )
        }
    }
}