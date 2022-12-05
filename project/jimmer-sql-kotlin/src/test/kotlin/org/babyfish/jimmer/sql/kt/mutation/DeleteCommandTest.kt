package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.Book
import org.babyfish.jimmer.sql.kt.model.BookStore
import org.junit.Test

class DeleteCommandTest : AbstractMutationTest() {

    @Test
    fun test() {
        executeAndExpectResult({ con ->
            sqlClient.entities.delete(BookStore::class, 2L, con) {
                setDissociateAction(Book::store, DissociateAction.SET_NULL)
            }
        }) {
            statement {
                sql(
                    """update BOOK set STORE_ID = null where STORE_ID = ?"""
                )
                variables(2L)
            }
            statement {
                sql(
                    """delete from BOOK_STORE where ID in (?)"""
                )
                variables(2L)
            }
            totalRowCount(4)
            rowCount(Book::class, 3)
            rowCount(BookStore::class, 1)
        }
    }
}