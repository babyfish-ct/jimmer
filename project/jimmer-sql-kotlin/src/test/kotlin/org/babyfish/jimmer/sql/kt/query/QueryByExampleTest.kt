package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.kt.ast.query.KExample
import org.babyfish.jimmer.sql.kt.ast.query.example
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.by
import org.babyfish.jimmer.sql.kt.model.classic.book.by
import org.junit.Test

class QueryByExampleTest : AbstractQueryTest() {

    @Test
    fun testMatchEmpty() {
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = ""
                    }
                )
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_"
            ).variables()
        }

        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = "X"
                    }
                )
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ?"
            ).variables("X")
        }

        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = ""
                    }
                ) {
                    match(KExample.MatchMode.NOT_NULL)
                }
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ?"
            ).variables("")
        }
    }

    @Test
    fun testMatchNull() {
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        store = null
                    }
                )
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_"
            ).variables()
        }

        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        store = null
                    }
                ) {
                    match(KExample.MatchMode.NULLABLE)
                }
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.STORE_ID is null"
            ).variables()
        }
    }

    @Test
    fun testTrim() {
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = " X "
                    }
                )
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ?"
            ).variables(" X ")
        }
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = " X "
                    }
                ) {
                    trim()
                }
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ?"
            ).variables("X")
        }
    }

    @Test
    fun testPropMatchEmpty() {
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = ""
                    }
                )
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_"
            ).variables()
        }

        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = "X"
                    }
                )
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ?"
            ).variables("X")
        }

        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = ""
                    }
                ) {
                    match(Book::name, KExample.MatchMode.NOT_NULL)
                }
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ?"
            ).variables("")
        }
    }

    @Test
    fun testPropMatchNull() {
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        store = null
                    }
                )
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_"
            ).variables()
        }

        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        store = null
                    }
                ) {
                    match(Book::store, KExample.MatchMode.NULLABLE)
                }
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.STORE_ID is null"
            ).variables()
        }
    }

    @Test
    fun testPropTrim() {
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = " X "
                    }
                )
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ?"
            ).variables(" X ")
        }

        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = " X "
                    }
                ) {
                    trim(Book::name)
                }
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ?"
            ).variables("X")
        }
    }

    @Test
    fun testPropZero() {
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        edition = 0
                    }
                )
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.EDITION = ?"
            ).variables(0)
        }

        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        edition = 0
                    }
                ) {
                    ignoreZero(Book::edition)
                }
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_"
            ).variables()
        }
    }

    @Test
    fun testLike() {
        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = "G"
                    }
                ) {
                    like(Book::name, LikeMode.START)
                }
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME like ?"
            ).variables("G%")
        }

        connectAndExpect({
            sqlClient.entities.forConnection(it).findByExample(
                example(
                    new(Book::class).by {
                        name = "G"
                    }
                ) {
                    ilike(Book::name, LikeMode.START)
                }
            )
        }) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where lower(tb_1_.NAME) like ?"
            ).variables("g%")
        }
    }
}