package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.by
import org.babyfish.jimmer.sql.kt.model.hr.Department
import org.babyfish.jimmer.sql.kt.model.hr.Employee
import org.babyfish.jimmer.sql.kt.model.hr.by
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator
import java.math.BigDecimal
import kotlin.test.Test

class ModifiedFetcherTest : AbstractMutationTest() {

    @Test
    fun testIssue982ByQueryDraft() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                Employee {
                    id = 1L
                    employeeName = "Johe"
                },
                newFetcher(Employee::class).by {
                    employeeName()
                    deletedUUID()
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }.modifiedEntity
        }) {
            statement {
                sql("update EMPLOYEE set NAME = ? where ID = ?")
            }
            statement {
                sql("select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_UUID from EMPLOYEE tb_1_ where tb_1_.ID = ? and tb_1_.DELETED_UUID is null")
            }
            value("""{"id":"1","employeeName":"Johe","deletedUUID":null}""")
        }
    }

    @Test
    fun testIssue982ByQueryImmutable() {
        connectAndExpect({ con ->
            sqlClient.entities.forConnection(con).save(
                Book {
                    id = 1L
                    name = "Learning GraphQL protocol"
                },
                newFetcher(Book::class).by {
                    name()
                    edition()
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }.modifiedEntity
        }) {
            statement {
                sql("update BOOK set NAME = ? where ID = ?")
            }
            statement {
                sql("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK tb_1_ where tb_1_.ID = ?")
            }
            value("""{"id":1,"name":"Learning GraphQL protocol","edition":1}""")
        }
    }

    @Test
    fun testIssue1000BySimple() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
                setIdGenerator(IdentityIdGenerator.INSTANCE)
            }.saveCommand(
                Book {
                    name = "GraphQL in Action"
                    edition = 3
                    price = BigDecimal("73.9")
                    storeId = 2L
                }
            ) {
                setMode(SaveMode.INSERT_IF_ABSENT)
            }.execute(con, newFetcher(Book::class).by {
                allScalarFields()
            }).modifiedEntity
        }) {
            statement {
                sql(
                    """merge into BOOK tb_1_ 
                        |using(values(?, ?, ?, ?)) tb_2_(NAME, EDITION, PRICE, STORE_ID) 
                        |--->on tb_1_.NAME = tb_2_.NAME and tb_1_.EDITION = tb_2_.EDITION 
                        |when matched then 
                        |--->update set /* fake update to return all ids */ EDITION = tb_2_.EDITION 
                        |when not matched then 
                        |--->insert(NAME, EDITION, PRICE, STORE_ID) 
                        |--->values(tb_2_.NAME, tb_2_.EDITION, tb_2_.PRICE, tb_2_.STORE_ID)""".trimMargin()
                )
            }
            // 1. `INSERT_IF_ABSENT` will be translated to
            //    `UPSERT + UpsertMask` when fetcher is specified
            // 2, `UpsertMask` or `SaveRule` may cause
            //    jimmer cannot do non-query optimization
            //    even if the shape of modified object is enough
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE 
                        |from BOOK tb_1_ 
                        |where tb_1_.ID = ?""".trimMargin()
                )
            }
            value(
                """{"id":12,"name":"GraphQL in Action","edition":3,"price":80.00}""".trimMargin()
            )
        }
    }

    @Test
    fun testIssue1000ByBatch() {
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
                setIdGenerator(IdentityIdGenerator.INSTANCE)
            }.saveEntitiesCommand(
                listOf(
                    Book {
                        name = "GraphQL in Action"
                        edition = 3
                        price = BigDecimal("73.9")
                        storeId = 2L
                    },
                    Book {
                        name = "GraphQL in Action"
                        edition = 4
                        price = BigDecimal("78.9")
                        storeId = 2L
                    }
                )
            ) {
                setMode(SaveMode.INSERT_IF_ABSENT)
            }.execute(con, newFetcher(Book::class).by {
                allScalarFields()
            }).items.map { it.modifiedEntity }
        }) {
            statement {
                sql(
                    """merge into BOOK tb_1_ 
                        |using(values(?, ?, ?, ?)) tb_2_(NAME, EDITION, PRICE, STORE_ID) 
                        |--->on tb_1_.NAME = tb_2_.NAME and tb_1_.EDITION = tb_2_.EDITION 
                        |when matched then 
                        |--->update set /* fake update to return all ids */ EDITION = tb_2_.EDITION 
                        |when not matched then 
                        |--->insert(NAME, EDITION, PRICE, STORE_ID) 
                        |--->values(tb_2_.NAME, tb_2_.EDITION, tb_2_.PRICE, tb_2_.STORE_ID)""".trimMargin()
                )
            }
            // 1. `INSERT_IF_ABSENT` will be translated to
            //    `UPSERT + UpsertMask` when fetcher is specified
            // 2, `UpsertMask` or `SaveRule` may cause
            //    jimmer cannot do non-query optimization
            //    even if the shape of modified object is enough
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE 
                        |from BOOK tb_1_ 
                        |where tb_1_.ID = any(?)""".trimMargin()
                )
            }
            value(
                """[
                    |{"id":12,"name":"GraphQL in Action","edition":3,"price":80.00}, 
                    |{"id":100,"name":"GraphQL in Action","edition":4,"price":78.90}
                    |]""".trimMargin()
            )
        }
    }
}