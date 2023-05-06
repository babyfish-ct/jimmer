package org.babyfish.jimmer.example.save

import org.babyfish.jimmer.example.save.common.AbstractMutationTest
import org.babyfish.jimmer.example.save.common.ExecutedStatement
import org.babyfish.jimmer.example.save.model.*
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.runtime.ExecutionException
import org.babyfish.jimmer.sql.runtime.SaveException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal


/**
 * Recommended learning sequence: 5
 *
 *
 * SaveModeTest -> IncompleteObjectTest -> ManyToOneTest ->
 * OneToManyTest -> [Current: ManyToManyTest] -> RecursiveTest -> TriggerTest
 */
class ManyToManyTest : AbstractMutationTest() {
    
    /*
     * Noun explanation
     *
     * Short Association: Association object(s) with only id property.
     * Long Association: Association object(s) with non-id properties.
     */
    
    @Test
    fun testInsertMiddleTableByShortAssociation() {
        
        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )
        jdbc(
            "insert into author(id, first_name, last_name, gender) values(?, ?, ?, ?)",
            100L, "Ben", "Brumm", "M"
        )
        
        val result = sql.entities.save(
            new(Book::class).by {
                name = "SQL in Action"
                edition = 1
                price = BigDecimal(49)
                authors().addBy { 
                    id = 100L
                }
            }
        )
        
        assertExecutedStatements( 
            
            // Select aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),  
            
            // Aggregate exists, update it
            ExecutedStatement(
                "update BOOK set PRICE = ? where ID = ?",
                BigDecimal(49), 10L
            ),  
            
            // Query mapping from middle table
            ExecutedStatement(
                "select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?",
                10L
            ),  
            
            // Mapping does not exist, insert it
            ExecutedStatement(
                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values (?, ?)",
                10L, 100L
            )
        )
        Assertions.assertEquals(2, result.totalAffectedRowCount)
        Assertions.assertEquals(1, result.affectedRowCount(Book::class))
        Assertions.assertEquals(1, result.affectedRowCount(Book::authors))
    }

    @Test
    fun testIllegalShortAssociation() {

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )
        jdbc(
            "insert into author(id, first_name, last_name, gender) values(?, ?, ?, ?)",
            100L, "Ben", "Brumm", "M"
        )

        val ex = Assertions.assertThrows(SaveException::class.java) {
            sql.entities.save(
                new(Book::class).by {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                    authors().addBy {
                        id = 100L
                    }
                    authors().addBy {
                        id = 88888L
                    }
                    authors().addBy {
                        id = 99999L
                    }
                }
            ) {
                /*
                 * You can also use `setAutoIdOnlyTargetCheckingAll()`.
                 *
                 * If you use jimmer-spring-starter, it is unnecessary to
                 * do it because this switch is turned on.
                 *
                 * If the underlying `BOOK_AUTHOR_MAPPING.AUTHOR_ID`
                 * has foreign key constraints,
                 * even if this configuration is not used, error still will be
                 * raised by database so that you can choose not to use this
                 * configuration when you have strict performance requirements.
                 * However, this configuration can bring better error message.
                 *
                 * Sometimes it is not possible to add foreign key constraints,
                 * such table sharding. At this time, this configuration is
                 * very important.
                 */
                setAutoIdOnlyTargetChecking(Book::authors)
            }
        }

        Assertions.assertEquals(
            "Save error caused by the path: \"<root>.authors\": " +
                "Illegal ids: [88888, 99999]",
            ex.message
        )

        assertExecutedStatements(

            // Query aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Aggregate exists, update it
            ExecutedStatement(
                "update BOOK set PRICE = ? where ID = ?",
                BigDecimal(49), 10L
            ),

            // Are target ids valid
            ExecutedStatement(
                "select tb_1_.ID from AUTHOR tb_1_ where tb_1_.ID in (?, ?, ?)",
                100L, 88888L, 99999L
            )
        )
    }

    @Test
    fun deleteMiddleTable() {

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )
        jdbc(
            "insert into author(id, first_name, last_name, gender) values(?, ?, ?, ?)",
            100L, "Ben", "Brumm", "M"
        )
        jdbc(
            "insert into author(id, first_name, last_name, gender) values(?, ?, ?, ?)",
            200L, "Prabath", "Siriwardena", "M"
        )
        jdbc("insert into book_author_mapping(book_id, author_id) values(?, ?)", 10L, 100L)
        jdbc("insert into book_author_mapping(book_id, author_id) values(?, ?)", 10L, 200L)

        val result = sql.entities.save(
            new(Book::class).by {
                name = "SQL in Action"
                edition = 1
                price = BigDecimal(49)
                authors().addBy {
                    id = 100L
                }
            }
        )

        assertExecutedStatements(

            // Query aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Aggregate-root exists, update it
            ExecutedStatement(
                "update BOOK set PRICE = ? where ID = ?",
                BigDecimal(49), 10L
            ),

            // Query mapping from middle table
            ExecutedStatement(
                "select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?",
                10L
            ),

            // The mapping references to `Author-200` must be deleted
            ExecutedStatement(
                "delete from BOOK_AUTHOR_MAPPING " +
                    "where (BOOK_ID, AUTHOR_ID) in ((?, ?))",
                10L, 200L
            )
        )

        Assertions.assertEquals(2, result.totalAffectedRowCount)
        Assertions.assertEquals(1, result.affectedRowCount(Book::class))
        Assertions.assertEquals(1, result.affectedRowCount(Book::authors))
    }

    @Test
    fun testAttachAuthorFailed() {

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )

        val ex = Assertions.assertThrows(SaveException::class.java) {
            sql.entities.save(
                new(Book::class).by {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                    authors().addBy {
                        firstName = "Ben"
                        lastName = "Brumm"
                        gender = Gender.MALE
                    }
                }
            )
        }
        Assertions.assertEquals(
            "Save error caused by the path: \"<root>.authors\": " +
                "Cannot insert object because insert operation for this path is disabled, " +
                "please call `setAutoAttaching(Book::authors)` " +
                "or `setAutoAttachingAll()` of the save command",
            ex.message
        )
        assertExecutedStatements(

            // Query aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Aggregate-root exists, update it
            ExecutedStatement(
                "update BOOK set PRICE = ? where ID = ?",
                BigDecimal(49), 10L
            ),

            // Query associated object by key.
            // In this test case, nothing will be found, it need to be inserted.
            // However, the switch to automatically create associated objects
            // has not been turned on so that error will be raised
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                    "from AUTHOR tb_1_ " +
                    "where tb_1_.FIRST_NAME = ? and tb_1_.LAST_NAME = ?",
                "Ben", "Brumm"
            )
        )
    }

    @Test
    fun testAttachAuthor() {

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
                    authors().addBy {
                        firstName = "Ben"
                        lastName = "Brumm"
                        gender = Gender.MALE
                    }
                }
            ) {
                /*
                 * You can also use `setAutoAttachingAll()`.
                 *
                 * If you use jimmer-spring-starter, it is unnecessary to
                 * do it because this switch is turned on.
                 */
                setAutoAttaching(Book::authors)
            }

        assertExecutedStatements(

            // Query aggregate-root by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Aggregate exists, update it
            ExecutedStatement(
                "update BOOK set PRICE = ? where ID = ?",
                BigDecimal(49), 10L
            ),

            // Select associated object by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                    "from AUTHOR tb_1_ " +
                    "where tb_1_.FIRST_NAME = ? and tb_1_.LAST_NAME = ?",
                "Ben", "Brumm"
            ),

            // Associated object does not exists, insert it
            ExecutedStatement(
                "insert into AUTHOR(FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?)",
                "Ben", "Brumm", "M"
            ),

            // Query mapping from middle table
            ExecutedStatement(
                "select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?",
                10L
            ),

            // Mapping does not exist, insert it
            ExecutedStatement(
                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values (?, ?)",
                10L, 100L
            )
        )

        Assertions.assertEquals(3, result.totalAffectedRowCount)
        Assertions.assertEquals(1, result.affectedRowCount(Book::class))
        Assertions.assertEquals(1, result.affectedRowCount(Author::class))
        Assertions.assertEquals(1, result.affectedRowCount(Book::authors))
    }
}