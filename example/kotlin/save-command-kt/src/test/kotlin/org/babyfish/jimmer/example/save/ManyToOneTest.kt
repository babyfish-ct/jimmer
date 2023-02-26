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
        
        assertExecutedStatements( // Select data by id
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK as tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),  // Data exists, update it.
            // The foreign key `store_id` is updated.
            ExecutedStatement(
                "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                BigDecimal(49), 1L, 10L
            )
        )
        Assertions.assertEquals(1, result.totalAffectedRowCount)
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
    fun testIllegalShortAssociation() {
        
        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )
        
        val ex = Assertions.assertThrows(ExecutionException::class.java) {
            sql
                .entities
                .save(
                    new(Book::class).by {
                        name = "SQL in Action"
                        edition = 1
                        price = BigDecimal(49)
                        store = makeIdOnly(99999L)
                    }
                )
        }
        
        Assertions.assertEquals(
            "Cannot execute SQL statement: " +
                "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?, " +
                "variables: [49, 99999, 10]",
            ex.message
        )
        
        /*
         * In the current Jimmer, the many-to-one property is based on the foreign key.
         * If the associated object holds an illegal id, the database will report an error.
         *
         * In the future, Jimmer will support fake foreign key(It should be understood
         * as a foreign key in business, but it is not a foreign key in the database.
         * It is suitable for the database sharding and table sharding), an additional
         * validation will be added here.
         */
        assertExecutedStatements(

            // Select aggregate-root object by
            ExecutedStatement(
                ("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK as tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?"),
                "SQL in Action", 1
            ),

            // Aggregate-root exists, update it with illegal foreign key
            ExecutedStatement(
                "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                BigDecimal(49), 99999L, 10L
            )
        )
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
    fun testAssociationByNonExistingParentAndNotAllowedToCreate() {
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
            "Save error caused by the path: \"<root>.store\": Cannot insert object because insert operation for this path is disabled",
            ex.message
        )

        assertExecutedStatements(

            // Select the parent object by key.
            //
            // If no data selected, report error because the switch to
            // automatically create associated objects has not been turned on
            ExecutedStatement(
                ("select tb_1_.ID, tb_1_.NAME " +
                    "from BOOK_STORE as tb_1_ " +
                    "where tb_1_.NAME = ?"),
                "TURING"
            )
        )
    }

    @Test
    fun testLongAssociationByNonExistingParentAndAllowToCreate() {

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
                 * If you use jimmer-spring-starter, it is unecessary to
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