package org.babyfish.jimmer.example.save

import org.babyfish.jimmer.example.save.common.AbstractMutationTest
import org.babyfish.jimmer.example.save.common.ExecutedStatement
import org.babyfish.jimmer.example.save.model.Book
import org.babyfish.jimmer.example.save.model.BookStore
import org.babyfish.jimmer.example.save.model.by
import org.babyfish.jimmer.kt.makeIdOnly
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.runtime.ExecutionException
import org.babyfish.jimmer.sql.runtime.SaveException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal


/**
 * Recommended learning sequence: 3
 *
 *
 * SaveModeTest -> IncompleteObjectTest -> [current: ManyToOneTest] ->
 * OneToManyTest -> ManyToManyTest -> RecursiveTest -> TriggerTest
 */
class ManyToOneTest : AbstractMutationTest() {
    
    /*
     * Noun explanation
     *
     * Short Association: Association object(s) with only id property.
     * Long Association: Association object(s) with non-id properties.
     */
    
    @Test
    fun testShortAssociation() {
        
        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING")
        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )
        val result = sql
            .entities
            .save(
                new(Book::class).by {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                    store = makeIdOnly(1L)
                }
            )
        
        assertExecutedStatements(

            // Select data by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK as tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Data exists, update it.
            // The foreign key `store_id` is updated.
            ExecutedStatement(
                "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                BigDecimal(49), 1L, 10L
            )
        )
        Assertions.assertEquals(1, result.totalAffectedRowCount)
    }

    @Test
    fun testIllegalShortAssociation() {

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )

        val ex = Assertions.assertThrows(SaveException::class.java) {
            sql
                .entities
                .save(
                    new(Book::class).by {
                        name = "SQL in Action"
                        edition = 1
                        price = BigDecimal(49)
                        store = makeIdOnly(99999L)
                    }
                ) {
                    /*
                     * You can also use `setAutoIdOnlyTargetCheckingAll()`.
                     *
                     * If you use jimmer-spring-starter, it is unnecessary to
                     * do it because this switch is turned on.
                     *
                     * If the underlying `BOOK.STORE_ID` has foreign key constraints,
                     * even if this configuration is not used, error still will be
                     * raised by database so that you can choose not to use this
                     * configuration when you have strict performance requirements.
                     * However, this configuration can bring better error message.
                     *
                     * Sometimes it is not possible to add foreign key constraints,
                     * such as table sharding. At this time, this configuration is
                     * very important.
                     */
                    setAutoIdOnlyTargetChecking(Book::store)
                }
        }

        Assertions.assertEquals(
            "Save error caused by the path: \"<root>.store\": " +
                "Illegal ids: [99999]",
            ex.message
        )

        assertExecutedStatements(

            // Is targetId valid?
            ExecutedStatement(
                "select tb_1_.ID from BOOK_STORE as tb_1_ where tb_1_.ID in (?)",
                99999L
            )
        )
    }

    @Test
    fun testAssociationByKey() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING")
        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )

        val result = sql
            .entities
            .save(
                new(Book::class).by {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                    store().apply {
                        name = "MANNING"
                    }
                }
            )
        
        assertExecutedStatements(

            // Select parent object by key.
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME " +
                    "from BOOK_STORE as tb_1_ " +
                    "where tb_1_.NAME = ?",
                "MANNING"
            ),

            // select aggregation-root object by key
            ExecutedStatement(
                ("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK as tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?"),
                "SQL in Action", 1
            ),

            // Aggregation-root object exists, update it, include the foreign key
            ExecutedStatement(
                "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                BigDecimal(49), 1L, 10L
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)
    }

    @Test
    fun testAssociationByExistingParent() {
        
        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING")
        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )
        
        val result = sql.entities.save(
            new(Book::class).by {
                name = "SQL in Action"
                edition = 1
                price = BigDecimal(49)
                store().apply {
                    name = "MANNING"
                    website = "https://www.manning.com"
                }
            }
        )

        assertExecutedStatements(

            // Select parent by key
            ExecutedStatement(
                ("select tb_1_.ID, tb_1_.NAME " +
                    "from BOOK_STORE as tb_1_ " +
                    "where tb_1_.NAME = ?"),
                "MANNING"
            ),

            // Parent exists, update it
            ExecutedStatement(
                "update BOOK_STORE set WEBSITE = ? where ID = ?",
                "https://www.manning.com",
                1L
            ),

            // Select aggregate-root object by key
            ExecutedStatement(
                ("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK as tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?"),
                "SQL in Action", 1
            ),

            // Aggregate-root object exists, update it
            ExecutedStatement(
                "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                BigDecimal(49), 1L, 10L
            )
        )

        Assertions.assertEquals(2, result.totalAffectedRowCount)
        Assertions.assertEquals(1, result.affectedRowCount(BookStore::class))
        Assertions.assertEquals(1, result.affectedRowCount(Book::class))
    }

    @Test
    fun testAttachParentFailed() {
        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            200L, "SQL in Action", 1, BigDecimal(45)
        )
        val ex = Assertions.assertThrows(SaveException::class.java) {
            sql.entities.save(
                new(Book::class).by {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                    store().apply {
                        name = "TURING"
                        website = "http://www.turing.com"
                    }
                }
            )
        }

        Assertions.assertEquals(
            "Save error caused by the path: \"<root>.store\": " +
                "Cannot insert object because insert operation for this path is disabled, " +
                "please call `setAutoAttaching(Book::store)` " +
                "or `setAutoAttachingAll()` of the save command",
            ex.message
        )

        assertExecutedStatements(

            // Select the parent object by key.
            //
            // If no data selected, report error because the switch to
            // automatically create associated objects has not been turned on
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME " +
                    "from BOOK_STORE as tb_1_ " +
                    "where tb_1_.NAME = ?",
                "TURING"
            )
        )
    }

    @Test
    fun testAttachParent() {

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )

        val result = sql
            .entities
            .save(
                new(Book::class).by {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                    store().apply {
                        name = "TURING"
                        website = "https://www.turing.com"
                    }
                }
            ) {
                /*
                 * You can also use `setAutoAttachingAll()`.
                 *
                 * If you use jimmer-spring-starter, it is unnecessary to
                 * do it because this switch is turned on.
                 */
                setAutoAttaching(Book::store)
            }

        assertExecutedStatements(

            // Select parent by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME " +
                    "from BOOK_STORE as tb_1_ " +
                    "where tb_1_.NAME = ?",
                "TURING"
            ),

            // Parent does not exist, however, the switch to automatically create
            // associated objects has not been turned on, so insert parent object.
            ExecutedStatement(
                "insert into BOOK_STORE(NAME, WEBSITE) values(?, ?)",
                "TURING", "https://www.turing.com"
            ),

            // Select the aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK as tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Aggregate-root exists, update it, include the foreign key
            ExecutedStatement(
                "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                BigDecimal(49), 1L, 10L
            )
        )
        Assertions.assertEquals(2, result.totalAffectedRowCount)
        Assertions.assertEquals(1, result.affectedRowCount(BookStore::class))
        Assertions.assertEquals(1, result.affectedRowCount(Book::class))
    }
}