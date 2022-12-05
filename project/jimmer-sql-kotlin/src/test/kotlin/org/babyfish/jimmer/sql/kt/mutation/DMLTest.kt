package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.*
import org.junit.Test

class DMLTest : AbstractMutationTest() {

    @Test
    fun testUpdate() {
        executeAndExpectRowCount(
            sqlClient.createUpdate(BookStore::class) {
                where(table.website.isNull())
                set(
                    table.website,
                    concat(
                        value("http://www."),
                        sql(String::class, "lower(%e)") {
                            expression(table.name)
                        },
                        value(".com")
                    )
                )
            }
        ) {
            statement {
                sql(
                    """update BOOK_STORE tb_1_ 
                        |set WEBSITE = concat(?, lower(tb_1_.NAME), ?) 
                        |where tb_1_.WEBSITE is null""".trimMargin()
                )
                variables("http://www.", ".com")
                rowCount(2)
            }
        }
    }

    @Test
    fun testDelete() {
        executeAndExpectRowCount(
            sqlClient.createDelete(Book::class) {
                where(table.id eq 12L)
            }
        ) {
            statement {
                sql("""delete from BOOK as tb_1_ where tb_1_.ID = ?""")
                variables(12L)
                rowCount(1)
            }
        }
    }

    @Test
    fun testDeleteByJoin() {
        executeAndExpectRowCount(
            sqlClient.createDelete(Book::class) {
                where(table.authors.firstName eq "Dan")
            }
        ) {
            statement {
                sql(
                    """select distinct tb_1_.ID 
                    |from BOOK as tb_1_ 
                    |inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID 
                    |inner join AUTHOR as tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID 
                    |where tb_3_.FIRST_NAME = ?""".trimMargin()
                )
                variables("Dan")
            }
            statement {
                sql(
                    """delete from BOOK_AUTHOR_MAPPING where BOOK_ID in (?, ?, ?)"""
                )
                variables(4L, 5L, 6L)
            }
            statement {
                sql(
                    """delete from BOOK 
                        |where ID in (?, ?, ?)""".trimMargin()
                )
                variables(4L, 5L, 6L)
            }
            rowCount(6)
        }
    }
}