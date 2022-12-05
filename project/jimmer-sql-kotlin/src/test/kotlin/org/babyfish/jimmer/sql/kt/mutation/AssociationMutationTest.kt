package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.Author
import org.babyfish.jimmer.sql.kt.model.Book
import org.junit.Test

class AssociationMutationTest : AbstractMutationTest() {

    @Test
    fun testInsert() {
        executeAndExpectRowCount({ con ->
            sqlClient
                .getAssociations(Book::authors)
                .save(3L, 5L, con = con)
        }) {
            statement {
                sql(
                    """insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) 
                        |values (?, ?)""".trimMargin()
                )
                variables(3L, 5L)
            }
            rowCount(1)
        }
    }

    @Test
    fun testUpsert() {
        executeAndExpectRowCount({ con ->
            sqlClient
                .getAssociations(Book::authors)
                .batchSave(
                    listOf(4L, 5L, 6L),
                    listOf(5L),
                    true,
                    con = con
                )
        }) {
            statement {
                sql(
                    """select BOOK_ID, AUTHOR_ID 
                        |from BOOK_AUTHOR_MAPPING 
                        |where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?), (?, ?))""".trimMargin()
                )
                variables(4L, 5L, 5L, 5L, 6L, 5L)
            }
            statement {
                sql(
                    """insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) 
                        |values (?, ?), (?, ?), (?, ?)""".trimMargin()
                )
                variables(4L, 5L, 5L, 5L, 6L, 5L)
            }
            rowCount(3)
        }
    }

    @Test
    fun testDelete() {
        executeAndExpectRowCount({ con ->
            sqlClient
                .getAssociations(Author::books)
                .forConnection(con)
                .delete(5L, 12L)
        }) {
            statement {
                sql(
                    """delete from BOOK_AUTHOR_MAPPING 
                        |where (AUTHOR_ID, BOOK_ID) in ((?, ?))""".trimMargin()
                )
                variables(5L, 12L)
            }
            rowCount(1)
        }
    }
}