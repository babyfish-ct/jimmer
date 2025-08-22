package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.classic.author.firstName
import org.babyfish.jimmer.sql.kt.model.classic.author.id
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.babyfish.jimmer.sql.kt.model.classic.store.website
import org.babyfish.jimmer.sql.kt.model.hr.Employee
import org.babyfish.jimmer.sql.kt.model.hr.departmentId
import org.babyfish.jimmer.sql.kt.model.inheritance.Administrator
import org.babyfish.jimmer.sql.kt.model.inheritance.name
import org.junit.Test
import java.math.BigDecimal

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
    fun testUpdateWithFilter() {
        executeAndExpectRowCount(
            sqlClient.createUpdate(Administrator::class) {
                set(table.name, concat(table.name, value("*")))
                where(table.name ilike "2")
            }
        ) {
            statement {
                sql(
                    """update ADMINISTRATOR tb_1_ 
                        |set NAME = concat(tb_1_.NAME, ?) 
                        |where lower(tb_1_.NAME) like ? 
                        |and tb_1_.DELETED <> ?""".trimMargin()
                )
            }
        }
    }

    @Test
    fun testDelete() {
        executeAndExpectRowCount(
            sqlClient.createDelete(Book::class) {
                where(table.id eq 12L)
                disableDissociation()
            }
        ) {
            statement {
                sql("""delete from BOOK tb_1_ where tb_1_.ID = ?""")
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
                    |from BOOK tb_1_ 
                    |inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.BOOK_ID 
                    |inner join AUTHOR tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID 
                    |where tb_3_.FIRST_NAME = ?""".trimMargin()
                )
                variables("Dan")
            }
            statement {
                sql(
                    """select BOOK_ID, AUTHOR_ID 
                        |from BOOK_AUTHOR_MAPPING where BOOK_ID in (?, ?, ?)""".trimMargin()
                )
                variables(4L, 5L, 6L)
            }
            statement {
                sql(
                    """delete from BOOK_AUTHOR_MAPPING 
                        |where BOOK_ID = ? and AUTHOR_ID = ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """delete from BOOK where ID in (?, ?, ?)""".trimMargin()
                )
                variables(4L, 5L, 6L)
            }
            rowCount(6)
        }
    }

    @Test
    fun testDeleteWithFilterWithoutDissociation() {
        executeAndExpectRowCount(
            sqlClient.createDelete(Administrator::class) {
                setMode(DeleteMode.PHYSICAL)
                where(table.name ilike "2")
                disableDissociation()
            }
        ) {
            statement {
                sql(
                    """delete from ADMINISTRATOR tb_1_ 
                        |where lower(tb_1_.NAME) like ? 
                        |and tb_1_.DELETED <> ?""".trimMargin()
                )
            }
        }
    }

    @Test
    fun testDeleteWithFilterAndDissociation() {
        executeAndExpectRowCount(
            sqlClient.createDelete(Administrator::class) {
                setMode(DeleteMode.PHYSICAL)
                where(table.name ilike "3")
            }
        ) {
            statement {
                sql(
                    """select distinct tb_1_.ID from ADMINISTRATOR tb_1_ 
                        |where lower(tb_1_.NAME) like ? and tb_1_.DELETED <> ?""".trimMargin()
                )
                queryReason(QueryReason.CANNOT_DELETE_DIRECTLY)
            }
            statement {
                sql(
                    """select ROLE_ID from ADMINISTRATOR_ROLE_MAPPING 
                        |where ADMINISTRATOR_ID = ?""".trimMargin()
                )
                queryReason(QueryReason.UPSERT_NOT_SUPPORTED)
            }
            statement {
                sql(
                    """delete from ADMINISTRATOR_ROLE_MAPPING 
                        |where ADMINISTRATOR_ID = ? and ROLE_ID = ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """delete from ADMINISTRATOR_METADATA where ADMINISTRATOR_ID = ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """delete from ADMINISTRATOR where ID = ?"""
                )
            }
            rowCount(4)
        }
    }

    @Test
    fun testDeleteByAssociatedId() {
        executeAndExpectRowCount(
            sqlClient.createDelete(Employee::class) {
                setMode(DeleteMode.PHYSICAL)
                where(table.departmentId eq 1L)
            }
        ) {
            statement {
                sql(
                    """delete from EMPLOYEE tb_1_ 
                        |where tb_1_.DEPARTMENT_ID = ? 
                        |and tb_1_.DELETED_UUID is null""".trimMargin()
                )
            }
            rowCount(2)
        }
    }

    @Test
    fun testSubQueryForIssue868() {
        executeAndExpectRowCount(
            sqlClient {
                setDialect(H2Dialect())
                setInListToAnyEqualityEnabled(true)
            }.createDelete(Book::class) {
                where(
                    table.storeId valueIn subQuery(BookStore::class) {
                        where(table.name eq "MANNING")
                        select(table.id)
                    }
                )
            }
        ) {
            statement {
                queryReason(QueryReason.CANNOT_DELETE_DIRECTLY)
                sql(
                    """select distinct tb_1_.ID from BOOK tb_1_ 
                        |where tb_1_.STORE_ID in (
                        |--->select tb_3_.ID 
                        |--->from BOOK_STORE tb_3_ 
                        |--->where tb_3_.NAME = ?
                        |)""".trimMargin()
                )
            }
            statement {
                sql(
                    """delete from BOOK_AUTHOR_MAPPING 
                        |where BOOK_ID = ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """delete from BOOK 
                        |where ID = any(?)""".trimMargin()
                )
            }
            rowCount(6)
        }
    }

    @Test
    fun testDeleteByImplicitSubQuery() {
        executeAndExpectRowCount(
            sqlClient {
                setDialect(H2Dialect())
                setInListToAnyEqualityEnabled(true)
            }.createDelete(Book::class) {
                where += table.authors {
                    id eq 2L
                }
            }
        ) {
            statement {
                sql(
                    """select distinct tb_1_.ID 
                        |from BOOK tb_1_ 
                        |where exists(
                        |--->select 1 
                        |--->from AUTHOR tb_2_ 
                        |--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID 
                        |--->where tb_3_.BOOK_ID = tb_1_.ID and tb_2_.ID = ?)""".trimMargin()
                )
                variables(2L)
            }
            statement {
                sql(
                    """delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ?"""
                )
                batchVariables(0, 1L)
                batchVariables(1, 2L)
                batchVariables(2, 3L)
            }
            statement {
                sql(
                    """delete from BOOK where ID = any(?)"""
                )
                variables(listOf(1L, 2L, 3L))
            }
            rowCount(9)
        }
    }

    @Test
    fun testUpdateByImplicitSubQuery() {
        executeAndExpectRowCount(
            sqlClient { setDialect(H2Dialect()) }.createUpdate(Book::class) {
                set(table.price, table.price + BigDecimal.ONE)
                where += table.authors {
                    id eq 2L
                }
            }
        ) {
            statement {
                sql(
                    """update BOOK tb_1_ 
                        |set PRICE = tb_1_.PRICE + ? 
                        |where exists(
                        |--->select 1 from AUTHOR tb_2_ 
                        |--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID 
                        |--->where tb_3_.BOOK_ID = tb_1_.ID and tb_2_.ID = ?
                        |)""".trimMargin()
                )
                variables(BigDecimal.ONE, 2L)
            }
            rowCount(3)
        }
    }
}