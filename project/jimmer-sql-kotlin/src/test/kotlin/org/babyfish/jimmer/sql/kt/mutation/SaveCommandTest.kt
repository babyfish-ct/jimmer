package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.dialect.PostgresDialect
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.common.NativeDatabases
import org.babyfish.jimmer.sql.kt.common.PreparedIdGenerator
import org.babyfish.jimmer.sql.kt.model.TreeNode
import org.babyfish.jimmer.sql.kt.model.addBy
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.model.classic.author.Gender
import org.babyfish.jimmer.sql.kt.model.classic.author.addBy
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.addBy
import org.babyfish.jimmer.sql.kt.model.classic.book.by
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.babyfish.jimmer.sql.kt.model.classic.store.version
import org.babyfish.jimmer.sql.kt.model.classic.store.website
import org.babyfish.jimmer.sql.kt.model.embedded.Dependency
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator
import org.junit.Assume
import org.junit.Test
import java.math.BigDecimal

class SaveCommandTest : AbstractMutationTest() {

    @Test
    fun testSaveLonely() {
        executeAndExpectResult({ con ->
            sqlClient {
                setIdGenerator(Book::class, PreparedIdGenerator(100L))
            }.entities.forConnection(con).save(
                new(Book::class).by {
                    name = "GraphQL in Action+"
                    edition = 4
                    price = BigDecimal(76)
                }
            )
        }) {
            statement {
                queryReason(QueryReason.UPSERT_NOT_SUPPORTED)
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
    fun testSaveLonelyWithPessimisticLock() {
        executeAndExpectResult({ con ->
            sqlClient {
                setIdGenerator(Book::class, PreparedIdGenerator(100L))
            }.entities.forConnection(con).save(
                new(Book::class).by {
                    name = "GraphQL in Action+"
                    edition = 4
                    price = BigDecimal(76)
                }
            ) {
                setPessimisticLock(Book::class, true)
            }
        }) {
            statement {
                queryReason(QueryReason.UPSERT_NOT_SUPPORTED)
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION 
                        |from BOOK tb_1_ 
                        |where (tb_1_.NAME, tb_1_.EDITION) = (?, ?) 
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
            sqlClient.entities.forConnection(con).save(
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
                }
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
            }.entities.forConnection(con).save(
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
                }
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
            sqlClient.entities.forConnection(con).save(
                Book {
                    id = 1L
                    name = "Learning GraphQL"
                    price = BigDecimal("49.9")
                }
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
            sqlClient.entities.forConnection(con).save(
                BookStore {
                    id = 1L
                    name = "O'REILLY"
                    version = 0
                }
            ) {
                setMode(SaveMode.UPDATE_ONLY)
                setOptimisticLock(BookStore::class) {
                    and(
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
            sqlClient.entities.forConnection(con).saveEntities(
                stores,
                SaveMode.UPDATE_ONLY,
                AssociatedSaveMode.UPDATE
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
    fun testBug957() {
        val stores = setOf( // Set, not list, for issue#968
            BookStore {
                id = 1L
                website = "https://www.oreilly.com"
            },
            BookStore {
                id = 2L
                website = "https://www.manning.com"
            }
        )
        executeAndExpectResult({ con->
            sqlClient {
                setDialect(H2Dialect())
            }.entities.forConnection(con).saveEntities(stores)
        }) {
            statement {
                sql(
                    """merge into BOOK_STORE tb_1_ 
                        |using(values(?, ?, ?)) tb_2_(ID, WEBSITE, VERSION) 
                        |--->on tb_1_.ID = tb_2_.ID 
                        |when matched then 
                        |--->update set WEBSITE = tb_2_.WEBSITE 
                        |when not matched then 
                        |--->insert(ID, WEBSITE, VERSION) 
                        |--->values(tb_2_.ID, tb_2_.WEBSITE, tb_2_.VERSION)""".trimMargin()
                )
                batchVariables(0, 1L, "https://www.oreilly.com", 0)
                batchVariables(1, 2L, "https://www.manning.com", 0)
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
            }.entities.forConnection(con).save(dependency)
        }) {
            statement {
                sql(
                    """merge into DEPENDENCY tb_1_ 
                        |using(values(?, ?, ?, ?)) tb_2_(GROUP_ID, ARTIFACT_ID, VERSION, SCOPE) 
                        |--->on tb_1_.GROUP_ID = tb_2_.GROUP_ID and tb_1_.ARTIFACT_ID = tb_2_.ARTIFACT_ID 
                        |when matched then update set VERSION = tb_2_.VERSION 
                        |when not matched then insert(GROUP_ID, ARTIFACT_ID, VERSION, SCOPE) 
                        |--->values(tb_2_.GROUP_ID, tb_2_.ARTIFACT_ID, tb_2_.VERSION, tb_2_.SCOPE)""".trimMargin()
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
            }.entities.forConnection(con).save(dependency)
        }) {
            statement {
                sql(
                    """insert into DEPENDENCY(GROUP_ID, ARTIFACT_ID, VERSION, SCOPE) 
                        |values(?, ?, ?, ?) 
                        |on conflict(GROUP_ID, ARTIFACT_ID) 
                        |do update set VERSION = excluded.VERSION""".trimMargin()
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
    fun testIssue933() {
        val rootNode = TreeNode {
            name = "transaction"
            childNodes().addBy {
                name = "version-1"
                childNodes().addBy {
                    name = "operation-1"
                }
                childNodes().addBy {
                    name = "operation-2"
                }
            }
            childNodes().addBy {
                name = "version-2"
                childNodes().addBy {
                    name = "operation-3"
                }
            }
        }
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
                setIdGenerator(IdentityIdGenerator.INSTANCE)
            }.entities.forConnection(con).save(
                rootNode,
                SaveMode.INSERT_ONLY,
                AssociatedSaveMode.APPEND
            )
        }) {
            statement {
                sql("insert into TREE_NODE(NAME) values(?)")
                variables("transaction")
            }
            statement {
                sql("insert into TREE_NODE(NAME, PARENT_ID) values(?, ?)")
                batchVariables(0, "version-1", 100L)
                batchVariables(1, "version-2", 100L)
            }
            statement {
                sql("insert into TREE_NODE(NAME, PARENT_ID) values(?, ?)")
                batchVariables(0, "operation-1", 101L)
                batchVariables(1, "operation-2", 101L)
                batchVariables(2, "operation-3", 102L)
            }
        }
    }

    @Test
    fun testBug956() {
        val dependency = Dependency {
            id().apply {
                groupId = "org.babyfish.jimmer"
                artifactId = "jimmer-sql-kotlin"
            }
        }
        connectAndExpect({ con ->
            sqlClient {
                setDialect(H2Dialect())
            }.entities.forConnection(con).save(dependency) {
                setIdOnlyAsReferenceAll(false)
            }
        }) {
            statement {
                sql(
                    """merge into DEPENDENCY tb_1_ 
                        |using(values(?, ?, ?)) tb_2_(GROUP_ID, ARTIFACT_ID, SCOPE) 
                        |--->on tb_1_.GROUP_ID = tb_2_.GROUP_ID and tb_1_.ARTIFACT_ID = tb_2_.ARTIFACT_ID 
                        |when not matched then insert(GROUP_ID, ARTIFACT_ID, SCOPE) 
                        |--->values(tb_2_.GROUP_ID, tb_2_.ARTIFACT_ID, tb_2_.SCOPE)""".trimMargin()
                )
            }
        }
    }

    @Test
    fun testIssue1071ByOneAssociation() {
        resetIdentity(null, "AUTHOR")
        resetIdentity(null, "BOOK")
        val author = Author {
            firstName = "Michael"
            lastName = "Simpson"
            gender = Gender.MALE
            books().addBy {
                name = "Learning GraphQL"
                edition = 1
                price = BigDecimal.valueOf(1.0)
            }
        }
        executeAndExpectResult({ con ->
            sqlClient {
                setDialect(H2Dialect())
                setIdGenerator(IdentityIdGenerator.INSTANCE)
            }.entities.forConnection(con).save(author) {
                setAssociatedMode(Author::books, AssociatedSaveMode.APPEND_IF_ABSENT)
            }
        }) {
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME 
                        |from AUTHOR tb_1_ 
                        |where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) = (?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into AUTHOR(FIRST_NAME, LAST_NAME, GENDER) 
                        |values(?, ?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """merge into BOOK tb_1_ 
                        |using(values(?, ?, ?)) tb_2_(NAME, EDITION, PRICE) 
                        |--->on tb_1_.NAME = tb_2_.NAME and tb_1_.EDITION = tb_2_.EDITION 
                        |when not matched then 
                        |--->insert(NAME, EDITION, PRICE) values(tb_2_.NAME, tb_2_.EDITION, tb_2_.PRICE)""".trimMargin()
                )
            }
            statement {
                queryReason(QueryReason.GET_ID_FOR_PRE_SAVED_ENTITIES)
                sql(
                    """select tb_1_.ID 
                        |from BOOK tb_1_ 
                        |where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) 
                        |values(?, ?)""".trimMargin()
                )
            }
            entity {
                modified(
                    """{
                        |--->"id":100,
                        |--->"firstName":"Michael",
                        |--->"lastName":"Simpson",
                        |--->"gender":"MALE",
                        |--->"books":[
                        |--->--->{
                        |--->--->--->"id":1,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":1.0
                        |--->--->}
                        |--->]
                        |}""".trimMargin()
                )
            }
        }
    }

    @Test
    fun testIssue1071ByTwoAssociations() {
        resetIdentity(null, "AUTHOR")
        resetIdentity(null, "BOOK")
        val author = Author {
            firstName = "Michael"
            lastName = "Simpson"
            gender = Gender.MALE
            books().addBy {
                name = "Learning GraphQL"
                edition = 3
                price = BigDecimal.valueOf(1.0)
            }
            books().addBy {
                name = "Learning GraphQL"
                edition = 4
                price = BigDecimal.valueOf(1.0)
            }
        }
        executeAndExpectResult({ con ->
            sqlClient {
                setDialect(H2Dialect())
                setIdGenerator(IdentityIdGenerator.INSTANCE)
            }.entities.forConnection(con).save(author) {
                setAssociatedMode(Author::books, AssociatedSaveMode.APPEND_IF_ABSENT)
            }
        }) {
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME 
                        |from AUTHOR tb_1_ 
                        |where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) = (?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into AUTHOR(FIRST_NAME, LAST_NAME, GENDER) 
                        |values(?, ?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """merge into BOOK tb_1_ 
                        |using(values(?, ?, ?)) tb_2_(NAME, EDITION, PRICE) 
                        |--->on tb_1_.NAME = tb_2_.NAME and tb_1_.EDITION = tb_2_.EDITION 
                        |when not matched then 
                        |--->insert(NAME, EDITION, PRICE) values(tb_2_.NAME, tb_2_.EDITION, tb_2_.PRICE)""".trimMargin()
                )
            }
            statement {
                queryReason(QueryReason.GET_ID_FOR_PRE_SAVED_ENTITIES)
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION 
                        |from BOOK tb_1_ 
                        |where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) 
                        |values(?, ?)""".trimMargin()
                )
            }
            entity {
                modified(
                    """{
                        |--->"id":100,
                        |--->"firstName":"Michael",
                        |--->"lastName":"Simpson",
                        |--->"gender":"MALE",
                        |--->"books":[
                        |--->--->{
                        |--->--->--->"id":3,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":3,
                        |--->--->--->"price":1.0
                        |--->--->},{
                        |--->--->--->"id":100,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":4,
                        |--->--->--->"price":1.0
                        |--->--->}
                        |--->]
                        |}""".trimMargin()
                )
            }
        }
    }
}