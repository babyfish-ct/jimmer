package org.babyfish.jimmer.example.save

import org.babyfish.jimmer.example.save.common.AbstractMutationTest
import org.babyfish.jimmer.example.save.common.ExecutedStatement
import org.babyfish.jimmer.example.save.model.Book
import org.babyfish.jimmer.example.save.model.BookStore
import org.babyfish.jimmer.example.save.model.addBy
import org.babyfish.jimmer.example.save.model.by
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.runtime.SaveException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal


/**
 * Recommended learning sequence: 4
 *
 *
 * SaveModeTest -> IncompleteObjectTest -> ManyToOneTest ->
 * [current: OneToManyTest] -> ManyToManyTest -> RecursiveTest -> TriggerTest
 */
class OneToManyTest() : AbstractMutationTest() {
    
    /*
     * Noun explanation
     *
     * Short Association: Association object(s) with only id property.
     * Long Association: Association object(s) with non-id properties.
     */
    
    @Test
    fun testAttachChildByShortAssociation() {

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L,
            "SQL in Action",
            1,
            BigDecimal(45)
        )

        val result = sql.entities.save(
            new(BookStore::class).by {
                name = "MANNING"
                books().addBy {
                    id = 10L
                }
            }
        )

        assertExecutedStatements(

            // Select aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME " +
                    "from BOOK_STORE tb_1_ " +
                    "where tb_1_.NAME = ?",
                "MANNING"
            ),  
            
            // Aggregate does not exist, insert it
            ExecutedStatement(
                "insert into BOOK_STORE(NAME) values(?)",
                "MANNING"
            ),  
            
            // Change the foreign key of child object
            ExecutedStatement(
                "update BOOK set STORE_ID = ? where ID in (?)",
                1L, 10L
            )
        )

        Assertions.assertEquals(2, result.totalAffectedRowCount)
        Assertions.assertEquals(1, result.affectedRowCount(BookStore::class))
        Assertions.assertEquals(1, result.affectedRowCount(Book::class))
    }

    @Test
    fun testAttachChildByAssociationBasedOnKey() {

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10,
            "SQL in Action",
            1,
            BigDecimal(45)
        )

        val result = sql.entities.save(
            new(BookStore::class).by {
                name = "MANNING"
                books().addBy {
                    name = "SQL in Action"
                    edition = 1
                }
            }
        )

        assertExecutedStatements(

            // Select aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME " +
                    "from BOOK_STORE tb_1_ " +
                    "where tb_1_.NAME = ?",
                "MANNING"
            ),

            // Aggregate does not exist, insert it
            ExecutedStatement(
                "insert into BOOK_STORE(NAME) values(?)",
                "MANNING"
            ),

            // Select child object by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Child object exists, update it, include foreign key
            ExecutedStatement(
                "update BOOK set STORE_ID = ? where ID = ?",
                1L, 10L
            )
        )

        Assertions.assertEquals(2, result.totalAffectedRowCount)
        Assertions.assertEquals(1, result.affectedRowCount(BookStore::class))
        Assertions.assertEquals(1, result.affectedRowCount(Book::class))
    }

    @Test
    fun testUpdateWithAssociation() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING")
        jdbc(
            "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
            10,
            "SQL in Action",
            1,
            BigDecimal(45),
            1L
        )

        val result = sql.entities.save(
            new(BookStore::class).by {
                name = "MANNING"
                books().addBy {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                }
            }
        )

        assertExecutedStatements(

            // Select aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ " +
                    "where tb_1_.NAME = ?",
                "MANNING"
            ),

            // Aggregate-root exists, but not changed, do nothing

            // Select child object by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Child object exists, update it, include foreign key
            ExecutedStatement(
                "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                BigDecimal(49), 1L, 10L
            ),

            // The aggregate-root exists, so there may be more child objects in the database,
            // query whether there are other child objects that need to be dissociated
            // besides the saved child objects
            //
            // In this test case, no more child objects will be found
            ExecutedStatement(
                "select 1 from BOOK " +
                    "where STORE_ID = ? and ID not in(?) " +
                    "limit ?",
                1L, 10L, 1
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)
    }

    @Test
    fun testAttachChildFailed() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING")

        val ex = Assertions.assertThrows(SaveException::class.java) {
            sql.entities.save(
                new(BookStore::class).by {
                    name = "MANNING"
                    books().addBy {
                        name = "SQL in Action"
                        edition = 1
                        price = BigDecimal(49)
                    }
                }
            )
        }

        Assertions.assertEquals(
            "Save error caused by the path: \"<root>.books\": " +
                "Cannot insert object because insert operation for this path is disabled, " +
                "please call `setAutoAttaching(BookStore::books)` " +
                "or `setAutoAttachingAll()` of the save command",
            ex.message
        )

        assertExecutedStatements(

            // Query aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ " +
                    "where tb_1_.NAME = ?",
                "MANNING"
            ),

            // Aggregate-root exists, but not changed, do nothing

            // Query child object by key
            // In this test case, nothing will be found, it need to be inserted.
            // However, the switch to automatically create associated objects
            // has not been turned on so that error will be raised
            ExecutedStatement(
                "select " +
                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            )
        )
    }

    @Test
    fun testAttachChild() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING")

        val result = sql
            .entities
            .save(
                new(BookStore::class).by {
                    name = "MANNING"
                    books().addBy {
                        name = "SQL in Action"
                        edition = 1
                        price = BigDecimal(49)
                    }
                }
            ) {
                /*
                 * You can also use `setAutoAttachingAll()`.
                 *
                 * If you use jimmer-spring-starter, it is unnecessary to
                 * do it because this switch is turned on.
                 */
                setAutoAttaching(BookStore::books)
            }

        assertExecutedStatements(

            // Select aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ " +
                    "where tb_1_.NAME = ?",
                "MANNING"
            ),

            // Aggregate-root exists, but not changed, do nothing

            // Select child object by key
            ExecutedStatement(
                "select " +
                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Child object does not exist, insert it
            ExecutedStatement(
                "insert into BOOK(NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?)",
                "SQL in Action", 1, BigDecimal(49), 1L
            ),

            // The aggregate-root exists, so there may be more child objects in the database,
            // query whether there are other child objects that need to be dissociated
            // besides the saved child objects
            //
            // In this test case, no more child objects will be found
            ExecutedStatement(
                "select 1 from BOOK where STORE_ID = ? and ID not in(?) limit ?",
                1L, 10L, 1
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)
    }

    @Test
    fun testDetachChildFailed() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING")

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10,
            "SQL in Action",
            1,
            BigDecimal(45)
        )

        jdbc(
            "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
            20,
            "GraphQL in Action",
            1,
            BigDecimal(39),
            1L
        )

        val ex = Assertions.assertThrows(SaveException::class.java) {
            sql.entities.save(
                new(BookStore::class).by {
                    name = "MANNING"
                    books().addBy {
                        id = 10L
                    }
                }
            )
        }

        Assertions.assertEquals(
            ("Save error caused by the path: \"<root>.books\": " +
                "Cannot dissociate child objects because the dissociation action of the many-to-one property " +
                "\"org.babyfish.jimmer.example.save.model.Book.store\" is not configured as \"set null\" or \"cascade\". " +
                "There are two ways to resolve this issue: " +
                "Decorate the many-to-one property \"org.babyfish.jimmer.example.save.model.Book.store\" " +
                "by @org.babyfish.jimmer.sql.OnDissociate whose argument is " +
                "`DissociateAction.SET_NULL` or `DissociateAction.DELETE` , " +
                "or use save command's runtime configuration to override it"),
            ex.message
        )

        assertExecutedStatements(

            // Query aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ where tb_1_.NAME = ?",
                "MANNING"
            ),

            // Aggregate-root exists, but not changed, do nothing

            // Update the foreign key of child object(s)
            ExecutedStatement(
                "update BOOK set STORE_ID = ? where ID in (?)",
                1L, 10L
            ),

            // The aggregate-root exists, so there may be more child objects in the database,
            // query whether there are other child objects that need to be dissociated
            // besides the saved child objects
            //
            // In this test case, child objects will be found but jimmer doesn't know
            // how to dissociate them so that error is raised
            ExecutedStatement(
                "select 1 from BOOK " +
                    "where STORE_ID = ? and ID not in(?) " +
                    "limit ?",
                1L, 10L, 1
            )
        )
    }

    @Test
    fun testDetachChildByClearingForeignKey() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING")
        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L,
            "SQL in Action",
            1,
            BigDecimal(45)
        )
        jdbc(
            "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
            20L,
            "GraphQL in Action",
            1,
            BigDecimal(39),
            1L
        )

        val result = sql
            .entities
            .save(
                new(BookStore::class).by {
                    name = "MANNING"
                    books().addBy {
                        id = 10L
                    }
                }
            ) {
                setDissociateAction(Book::store, DissociateAction.SET_NULL)
            }

        assertExecutedStatements(

            // Query aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ where tb_1_.NAME = ?",
                "MANNING"
            ),

            // Aggregate-root exists, but not changed, do nothing

            // Update foreign key of child object(s)
            ExecutedStatement(
                "update BOOK set STORE_ID = ? where ID in (?)",
                1L, 10L
            ),

            // The aggregate-root exists, so there may be more child objects in the database,
            // clear the foreign key of them.
            ExecutedStatement(
                "update BOOK set STORE_ID = null " +
                    "where STORE_ID = ? and ID not in (?)",
                1L, 10L
            )
        )

        Assertions.assertEquals(2, result.totalAffectedRowCount)
        Assertions.assertEquals(0, result.affectedRowCount(BookStore::class))
        Assertions.assertEquals(2, result.affectedRowCount(Book::class))
    }

    @Test
    fun testDetachChildByDeletingChild() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING")
        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L,
            "SQL in Action",
            1,
            BigDecimal(45)
        )
        jdbc(
            "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
            20L,
            "GraphQL in Action",
            1,
            BigDecimal(39),
            1L
        )

        val result = sql
            .entities
            .save(
                new(BookStore::class).by {
                    name = "MANNING"
                    books().addBy {
                        id = 10L
                    }
                }
            ) {
                setDissociateAction(Book::store, DissociateAction.DELETE)
            }

        assertExecutedStatements(

            // Query aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ where tb_1_.NAME = ?",
                "MANNING"
            ),

            // Aggregate-root exists, but not changed, do nothing

            // Update foreign key of child objects.
            ExecutedStatement(
                "update BOOK set STORE_ID = ? where ID in (?)",
                1L, 10L
            ),

            // The aggregate-root exists, so there may be more child objects in the database,
            // select id of them.
            ExecutedStatement(
                "select ID from BOOK where STORE_ID = ? and ID not in (?)",
                1L, 10L
            ),

            // Jimmer found `book-20` must be deleted,
            // Before doing this, the reference to `books-20` in the middle table of
            // the many-to-many association `Book.authors` must be removed.
            ExecutedStatement(
                "delete from BOOK_AUTHOR_MAPPING where BOOK_ID in (?)",
                20L
            ),

            // Now, `book-20` can be deleted safely
            ExecutedStatement(
                "delete from BOOK where ID in (?)",
                20L
            )
        )

        Assertions.assertEquals(2, result.totalAffectedRowCount)
        Assertions.assertEquals(0, result.affectedRowCount(BookStore::class))
        Assertions.assertEquals(2, result.affectedRowCount(Book::class))
    }
}