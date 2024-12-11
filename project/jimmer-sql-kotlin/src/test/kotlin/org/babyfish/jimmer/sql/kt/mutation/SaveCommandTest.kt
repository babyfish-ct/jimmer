package org.babyfish.jimmer.sql.kt.mutation

import junit.framework.TestSuite
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.dialect.PostgresDialect
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.common.NativeDatabases
import org.babyfish.jimmer.sql.kt.common.PreparedIdGenerator
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.model.classic.author.Gender
import org.babyfish.jimmer.sql.kt.model.classic.author.addBy
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.by
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.babyfish.jimmer.sql.kt.model.classic.store.version
import org.babyfish.jimmer.sql.kt.model.classic.store.website
import org.babyfish.jimmer.sql.kt.model.embedded.Dependency
import org.junit.Assume
import org.junit.Test
import java.math.BigDecimal


class SaveCommandTest : AbstractMutationTest() {

    @Test
    fun testSaveLonely() {
        executeAndExpectResult({ con ->
            sqlClient {
                setIdGenerator(Book::class, PreparedIdGenerator(100L))
            }.entities.save(
                new(Book::class).by {
                    name = "GraphQL in Action+"
                    edition = 4
                    price = BigDecimal(76)
                },
                con
            )
        }) {
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION 
                        |from BOOK tb_1_ 
                        |where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)""".trimMargin()
                )
                variables("GraphQL in Action+", 4)
            }
            statement {
                sql(
                    """insert into BOOK(ID, NAME, EDITION, PRICE) 
                        |values(?, ?, ?, ?)""".trimMargin()
                )
                variables(100L, "GraphQL in Action+", 4, BigDecimal(76))
            }
            entity {
                original(
                    """{"name":"GraphQL in Action+","edition":4,"price":76}"""
                )
                modified(
                    """{"id":100,"name":"GraphQL in Action+","edition":4,"price":76}"""
                )
            }
            totalRowCount(1)
            rowCount(Book::class, 1)
        }
    }

    @Test
    fun testShallowTree() {
        executeAndExpectResult({ con ->
            sqlClient.entities.save(
                new(Book::class).by {
                    name = "GraphQL in Action"
                    edition = 3
                    price = BigDecimal(76)
                    store().id = 1L
                    authors().addBy {
                        id = 3L
                    }
                    authors().addBy {
                        id = 4L
                    }
                },
                con
            )
        }) {
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION 
                        |from BOOK tb_1_ 
                        |where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)""".trimMargin()
                )
                variables("GraphQL in Action", 3)
            }
            statement {
                sql(
                    """update BOOK set PRICE = ?, STORE_ID = ? where ID = ?"""
                )
                variables(BigDecimal(76), 1L, 12L)
            }
            statement {
                sql(
                    """select AUTHOR_ID 
                        |from BOOK_AUTHOR_MAPPING 
                        |where BOOK_ID = ?""".trimMargin()
                )
                variables(12L)
            }
            statement {
                sql(
                    """delete from BOOK_AUTHOR_MAPPING 
                        |where BOOK_ID = ? and AUTHOR_ID = ?""".trimMargin()
                )
                variables(12L, 5L)
            }
            statement {
                sql(
                    """insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) 
                        |values(?, ?)""".trimMargin()
                )
                batchVariables(0, 12L, 3L)
                batchVariables(1, 12L, 4L)
            }
            entity {
                original(
                    """{
                        |--->"name":"GraphQL in Action",
                        |--->"edition":3,
                        |--->"price":76,
                        |--->"store":{"id":1},
                        |--->"authors":[{"id":3},{"id":4}]
                        |}""".trimMargin()
                )
                modified(
                    """{
                        |--->"id":12,
                        |--->"name":"GraphQL in Action",
                        |--->"edition":3,
                        |--->"price":76,
                        |--->"store":{"id":1},
                        |--->"authors":[{"id":3},{"id":4}]
                        |}""".trimMargin()
                )
            }
            totalRowCount(4)
            rowCount(Book::class, 1)
            rowCount(Book::authors, 3)
            rowCount(Author::books, 3)
        }
    }

    @Test
    fun testDeepTree() {
        executeAndExpectResult({ con ->
            sqlClient {
                setIdGenerator(BookStore::class, PreparedIdGenerator(100L))
                setIdGenerator(Author::class, PreparedIdGenerator(100L, 101L))
            }.entities.save(
                new(Book::class).by {
                    name = "GraphQL in Action"
                    edition = 3
                    price = BigDecimal(76)
                    store().apply {
                        name = "TURING"
                    }
                    authors().addBy {
                        firstName = "Kate"
                        lastName = "Green"
                        gender = Gender.FEMALE
                    }
                    authors().addBy {
                        firstName = "Tom"
                        lastName = "King"
                        gender = Gender.MALE
                    }
                },
                con
            )
        }) {
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from BOOK_STORE tb_1_ 
                        |where tb_1_.NAME = ?""".trimMargin()
                )
                variables("TURING")
            }
            statement {
                sql(
                    """insert into BOOK_STORE(ID, NAME, VERSION) 
                        |values(?, ?, ?)""".trimMargin()
                )
                variables(100L, "TURING", 0)
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION 
                        |from BOOK tb_1_ 
                        |where 
                        |--->(tb_1_.NAME, tb_1_.EDITION) = (?, ?)""".trimMargin()
                )
                variables("GraphQL in Action", 3)
            }
            statement {
                sql(
                    """update BOOK set PRICE = ?, STORE_ID = ? where ID = ?"""
                )
                variables(BigDecimal(76), 100L, 12L)
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME 
                        |from AUTHOR tb_1_ 
                        |where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) in ((?, ?), (?, ?))""".trimMargin()
                )
                variables("Kate", "Green", "Tom", "King")
            }
            statement {
                sql(
                    """insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) 
                        |values(?, ?, ?, ?)""".trimMargin()
                )
                batchVariables(0, 100L, "Kate", "Green", "F")
                batchVariables(1, 101L, "Tom", "King", "M")
            }
            statement {
                sql(
                    """select AUTHOR_ID 
                        |from BOOK_AUTHOR_MAPPING 
                        |where BOOK_ID = ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """delete from BOOK_AUTHOR_MAPPING 
                        |where BOOK_ID = ? and AUTHOR_ID = ?""".trimMargin()
                )
                variables(12L, 5L)
            }
            statement {
                sql(
                    """insert into 
                        |BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) 
                        |values(?, ?)""".trimMargin()
                )
                batchVariables(0, 12L, 100L)
                batchVariables(1, 12L, 101L)
            }
            entity {
                original(
                    """{
                        |--->"name":"GraphQL in Action",
                        |--->"edition":3,
                        |--->"price":76,
                        |--->"store":{
                        |--->--->"name":"TURING"
                        |--->},
                        |--->"authors":[
                        |--->--->{
                        |--->--->--->"firstName":"Kate",
                        |--->--->--->"lastName":"Green",
                        |--->--->--->"gender":"FEMALE"
                        |--->--->},
                        |--->--->{
                        |--->--->--->"firstName":"Tom",
                        |--->--->--->"lastName":"King",
                        |--->--->--->"gender":"MALE"
                        |--->--->}
                        |--->]
                        |}""".trimMargin()
                )
                modified(
                    """{
                        |--->"id":12,
                        |--->"name":"GraphQL in Action",
                        |--->"edition":3,
                        |--->"price":76,
                        |--->"store":{
                        |--->--->"id":100,
                        |--->--->"name":"TURING",
                        |--->--->"version":0
                        |--->},
                        |--->"authors":[
                        |--->--->{
                        |--->--->--->"id":100,
                        |--->--->--->"firstName":"Kate",
                        |--->--->--->"lastName":"Green",
                        |--->--->--->"gender":"FEMALE"
                        |--->--->},
                        |--->--->{
                        |--->--->--->"id":101,
                        |--->--->--->"firstName":"Tom",
                        |--->--->--->"lastName":"King",
                        |--->--->--->"gender":"MALE"
                        |--->--->}
                        |--->]
                        |}""".trimMargin()
                )
            }
            totalRowCount(7)
            rowCount(Book::class, 1)
            rowCount(BookStore::class, 1)
            rowCount(Author::class, 2)
            rowCount(Book::authors, 3)
            rowCount(Author::books, 3)
        }
    }

    @Test
    fun testOptimisticLock() {
        executeAndExpectResult({ con ->
            sqlClient.entities.save(
                Book {
                    id = 1L
                    name = "Learning GraphQL"
                    price = BigDecimal("49.9")
                },
                con
            ) {
                setMode(SaveMode.UPDATE_ONLY)
                setOptimisticLock(Book::class) {
                    table.edition eq 1
                }
            }
        }) {
            statement {
                sql(
                    """update BOOK 
                        |set NAME = ?, PRICE = ? 
                        |where ID = ? and EDITION = ?""".trimMargin()
                )
                variables("Learning GraphQL", BigDecimal("49.9"), 1L, 1)
            }
            entity {
                modified(
                    """{"id":1,"name":"Learning GraphQL","price":49.9}"""
                )
            }
        }
    }

    @Test
    fun testOptimisticLockAndVersion() {
        executeAndExpectResult({ con ->
            sqlClient.entities.save(
                BookStore {
                    id = 1L
                    name = "O'REILLY"
                    version = 0
                },
                con
            ) {
                setMode(SaveMode.UPDATE_ONLY)
                setOptimisticLock(BookStore::class) {
                    and(
                        table.version eq newNonNull(BookStore::version),
                        table.name eq "O'REILLY"
                    )
                }
            }
        }) {
            statement {
                sql(
                    """update BOOK_STORE set NAME = ?, VERSION = VERSION + 1 
                        |where ID = ? and VERSION = ? and NAME = ?""".trimMargin()
                )
                variables("O'REILLY", 1L, 0, "O'REILLY")
            }
            entity {
                modified(
                    """{"id":1,"name":"O'REILLY","version":1}"""
                )
            }
        }
    }

    @Test
    fun testComplexOptimisticLock() {
        val stores = listOf(
            BookStore {
                id = 1L
                website = "https://www.oreilly.com"
            },
            BookStore {
                id = 2L
                website = "https://www.manning.com"
            }
        )
        executeAndExpectResult({con ->
            sqlClient.updateEntities(
                stores,
                con = con
            ) {
                setOptimisticLock(BookStore::class) {
                    or(
                        table.website.isNull(),
                        sql(Boolean::class, "length(%e) <= length(%e)") {
                            expression(table.website)
                            expression(newNullable(BookStore::website))
                        }
                    )
                }
            }
        }) {
            statement {
                sql(
                    """update BOOK_STORE set WEBSITE = ? 
                        |where ID = ? and (
                        |--->--->WEBSITE is null 
                        |--->or 
                        |--->--->length(WEBSITE) <= length(?)
                        |)""".trimMargin()
                )
            }
            entity {  }
            entity {  }
        }
    }

    @Test
    fun testSaveDefaultEnum() {
        val dependency = Dependency {
            id().apply {
                groupId = "org.babyfish.jimmer"
                artifactId = "jimmer-sql-kotlin"
            }
            version = "0.8.177"
        }
        connectAndExpect({con ->
            sqlClient {
                setDialect(H2Dialect())
            }.entities.save(dependency, con)
        }) {
            statement {
                sql(
                    """merge into DEPENDENCY(GROUP_ID, ARTIFACT_ID, VERSION, SCOPE) 
                        |key(GROUP_ID, ARTIFACT_ID) values(?, ?, ?, ?)""".trimMargin()
                )
                variables(
                    "org.babyfish.jimmer",
                    "jimmer-sql-kotlin",
                    "0.8.177",
                    "C"
                )
            }
        }
    }

    @Test
    fun testSaveDefaultEnumByPostgres() {

        Assume.assumeTrue(NativeDatabases.isNativeAllowed())

        val dependency = Dependency {
            id().apply {
                groupId = "org.babyfish.jimmer"
                artifactId = "jimmer-sql-kotlin"
            }
            version = "0.8.177"
        }
        connectAndExpect(NativeDatabases.POSTGRES_DATA_SOURCE, {con ->
            sqlClient {
                setDialect(PostgresDialect())
            }.entities.save(dependency, con)
        }) {
            statement {
                sql(
                    """insert into DEPENDENCY(GROUP_ID, ARTIFACT_ID, VERSION, SCOPE) 
                        |values(?, ?, ?, ?) 
                        |on conflict(GROUP_ID, ARTIFACT_ID) 
                        |do update set VERSION = excluded.VERSION, SCOPE = excluded.SCOPE""".trimMargin()
                )
                variables(
                    "org.babyfish.jimmer",
                    "jimmer-sql-kotlin",
                    "0.8.177",
                    "C"
                )
            }
        }
    }
}