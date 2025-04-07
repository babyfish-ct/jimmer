package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.by
import org.babyfish.jimmer.sql.kt.model.hr.Employee
import org.babyfish.jimmer.sql.kt.model.hr.by
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
}