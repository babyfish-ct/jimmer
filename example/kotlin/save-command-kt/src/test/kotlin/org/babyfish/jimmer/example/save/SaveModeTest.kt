package org.babyfish.jimmer.example.save

import org.babyfish.jimmer.example.save.common.AbstractMutationTest
import org.babyfish.jimmer.example.save.common.ExecutedStatement
import org.babyfish.jimmer.example.save.model.Book
import org.babyfish.jimmer.example.save.model.by
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal


/**
 * Recommended learning sequence: 1
 *
 *
 * [Current: SaveModeTest] -> IncompleteObjectTest -> ManyToOneTest ->
 * OneToManyTest -> ManyToManyTest -> RecursiveTest -> TriggerTest
 */
class SaveModeTest : AbstractMutationTest() {

    @Test
    fun testInsertOnly() {
        val result = sql
            .entities
            .save(
                new(Book::class).by {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                }
            ) {
                setMode(SaveMode.INSERT_ONLY)
            }

        // `INSERT_ONLY` represents direct insertion regardless of whether the data exists
        assertExecutedStatements(
            ExecutedStatement(
                "insert into BOOK(NAME, EDITION, PRICE) values(?, ?, ?)",
                "SQL in Action",
                1, BigDecimal(49)
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)

        // `identity(10, 10)` in DDL
        Assertions.assertEquals(10L, result.modifiedEntity.id)
    }

    @Test
    fun testUpdateOnlyById() {

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )

        val result = sql
            .entities
            .save(
                new(Book::class).by { 
                    id = 10L
                    name = "SQL in Action"
                    edition = 2
                    price = BigDecimal(49)
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }

        // `UPDATE_ONLY` represents direct update regardless of whether the data exists
        assertExecutedStatements(
            ExecutedStatement(
                "update BOOK set NAME = ?, EDITION = ?, PRICE = ? where ID = ?",
                "SQL in Action", 2, BigDecimal(49), 10L
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)
    }

    @Test
    fun testUpdateExistingDataByKey() {

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
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }

        assertExecutedStatements(

            //Although `UPDATE_ONLY` is specified, the id attribute of the object is missing
            // so that it will still result in a key-based query.
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Update the selected data
            ExecutedStatement(
                "update BOOK set PRICE = ? where ID = ?",
                BigDecimal(49), 10L
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)
        Assertions.assertEquals(10L, result.modifiedEntity.id)
    }

    @Test
    fun testUpdateNonExistingDataByKey() {

        val result = sql
            .entities
            .save(
                new(Book::class).by {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }

        assertExecutedStatements(

            //Although `UPDATE_ONLY` is specified, the id attribute of the object is missing
            // so that it will still result in a key-based query.
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ) // No data can be selected, do nothing(affected row count is 0)
        )

        // Nothing updated
        Assertions.assertEquals(0, result.totalAffectedRowCount)
    }

    @Test
    fun testUpsertExistingDataById() {

        jdbc(
            "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
            10L, "SQL in Action", 1, BigDecimal(45)
        )

        val result = sql
            .entities
            .save(
                new(Book::class).by {
                    id = 10L
                    name = "PL/SQL in Action"
                    edition = 2
                }
            )

        assertExecutedStatements(

            // Query whether the data exists by id
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.ID = ?",
                10L
            ),

            // Data exist, update it
            ExecutedStatement(
                ("update BOOK " +
                    "set NAME = ?, EDITION = ? " +
                    "where ID = ?"),
                "PL/SQL in Action", 2, 10L
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)
    }

    @Test
    fun testUpsertExistingDataByKey() {
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
                }
            )

        assertExecutedStatements(

            // Query whether the data exists by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Data exists, update it
            ExecutedStatement(
                "update BOOK set PRICE = ? where ID = ?",
                BigDecimal(49), 10L
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)
    }

    @Test
    fun testUpsertNonExistingDataById() {
        val result = sql
            .entities
            .save(
                new(Book::class).by {
                    id = 10L
                    name = "SQL in Action"
                    edition = 2
                    price = BigDecimal(49)
                }
            )

        assertExecutedStatements(

            // Query whether the data exists by id
            ExecutedStatement(
                ("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.ID = ?"),
                10L
            ),

            // Data does not exists, insert it
            ExecutedStatement(
                "insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)",
                10L, "SQL in Action", 2, BigDecimal(49)
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)

        // `identity(10, 10)` in DDL
        Assertions.assertEquals(10L, result.modifiedEntity.id)
    }

    @Test
    fun testUpsertNonExistingDataByKey() {
        val result = sql
            .entities
            .save(
                new(Book::class).by {
                    name = "SQL in Action"
                    edition = 1
                    price = BigDecimal(49)
                }
            )

        assertExecutedStatements(

            // Query whether the data exists by key
            ExecutedStatement(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                "SQL in Action", 1
            ),

            // Data does not exists, insert it
            ExecutedStatement(
                "insert into BOOK(NAME, EDITION, PRICE) values(?, ?, ?)",
                "SQL in Action", 1, BigDecimal(49)
            )
        )

        Assertions.assertEquals(1, result.totalAffectedRowCount)

        // `identity(10, 10)` in DDL
        Assertions.assertEquals(10L, result.modifiedEntity.id)
    }
}