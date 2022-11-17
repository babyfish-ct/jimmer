package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.common.PreparedIdGenerator
import org.babyfish.jimmer.sql.kt.model.*
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
                        |from BOOK as tb_1_ 
                        |where tb_1_.NAME = ? 
                        |and tb_1_.EDITION = ? 
                        |for update""".trimMargin()
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
                        |from BOOK as tb_1_ 
                        |where tb_1_.NAME = ? 
                        |and tb_1_.EDITION = ? 
                        |for update""".trimMargin()
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
                        |where (BOOK_ID, AUTHOR_ID) in ((?, ?))""".trimMargin()
                )
                variables(12L, 5L)
            }
            statement {
                sql(
                    """insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) 
                        |values (?, ?), (?, ?)""".trimMargin()
                )
                variables(12L, 3L, 12L, 4L)
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
            ) {
                setAutoAttachingAll()
            }
        }) {
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from BOOK_STORE as tb_1_ 
                        |where tb_1_.NAME = ? 
                        |for update""".trimMargin()
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
                        |from BOOK as tb_1_ 
                        |where 
                        |--->tb_1_.NAME = ? 
                        |and 
                        |--->tb_1_.EDITION = ? 
                        |for update""".trimMargin()
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
                    """select 
                        |tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME 
                        |from AUTHOR as tb_1_ 
                        |where 
                        |--->tb_1_.FIRST_NAME = ? 
                        |and 
                        |--->tb_1_.LAST_NAME = ? 
                        |for update""".trimMargin()
                )
                variables("Kate", "Green")
            }
            statement {
                sql(
                    """insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) 
                        |values(?, ?, ?, ?)""".trimMargin()
                )
                variables(100L, "Kate", "Green", "F")
            }
            statement {
                sql(
                    """select 
                        |tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME 
                        |from AUTHOR as tb_1_ 
                        |where 
                        |--->tb_1_.FIRST_NAME = ? 
                        |and 
                        |--->tb_1_.LAST_NAME = ? 
                        |for update""".trimMargin()
                )
                variables("Tom", "King")
            }
            statement {
                sql(
                    """insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) 
                        |values(?, ?, ?, ?)""".trimMargin()
                )
                variables(101L, "Tom", "King", "M")
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
                        |where (BOOK_ID, AUTHOR_ID) in ((?, ?))""".trimMargin()
                )
                variables(12L, 5L)
            }
            statement {
                sql(
                    """insert into 
                        |BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) 
                        |values (?, ?), (?, ?)""".trimMargin()
                )
                variables(12L, 100L, 12L, 101L)
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
}