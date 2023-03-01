package org.babyfish.jimmer.example.save

import org.babyfish.jimmer.example.save.common.AbstractMutationTest
import org.babyfish.jimmer.example.save.common.ExecutedStatement
import org.babyfish.jimmer.example.save.model.BookStore
import org.babyfish.jimmer.example.save.model.by
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


/**
 * Recommended learning sequence: 2
 *
 *
 * SaveModeTest -> [Current: IncompleteObjectTest] -> ManyToOneTest ->
 * OneToManyTest -> ManyToManyTest -> RecursiveTest -> TriggerTest
 */
class IncompleteObjectTest : AbstractMutationTest() {
    @Test
    fun testCompleteObject() {

        jdbc(
            "insert into book_store(id, name, website) values(?, ?, ?)",
            1, "O'REILLY", "http://www.oreilly.com"
        )

        val result = sql
            .entities
            .save(
                new(BookStore::class).by {
                    id = 1L
                    name = "O'REILLY+"
                    website = null // `website` is specified
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY) 
            }
            
        assertExecutedStatements(

            // `WEBSITE` is updated to be null
            ExecutedStatement(
                "update BOOK_STORE " +
                    "set NAME = ?, WEBSITE = ? " +
                    "where ID = ?",
                "O'REILLY+", null, 1L
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)
    }

    @Test
    fun testIncompleteObject() {
        jdbc(
            "insert into book_store(id, name, website) values(?, ?, ?)",
            1, "O'REILLY", "http://www.oreilly.com"
        )

        val result = sql
            .entities
            .save(
                new(BookStore::class).by {
                    id = 1L
                    name = "O'REILLY+"
                    // `website` is not specified
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }

        assertExecutedStatements(

            // Unspecified property `website` will not be updated
            ExecutedStatement(
                "update BOOK_STORE " +
                    "set NAME = ? " +
                    "where ID = ?",
                "O'REILLY+", 1L
            )
        )

        /*
         * Objects can be incomplete, and unspecified properties will not be updated.
         *
         * This is a very important feature.
         *
         * - In traditional ORM, if you want to modify some properties of an object,
         *   you need to query the old object, modify the properties you want to modify,
         *   and finally save it.
         *
         * - In Jimmer, create an object and specify the properties you want to modify,
         * save it.
         */

        Assertions.assertEquals(1, result.totalAffectedRowCount)
    }
}