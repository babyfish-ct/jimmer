package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.impl.mutation.QueryReason
import org.babyfish.jimmer.sql.collection.TypedList
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.common.PreparedIdGenerator
import org.babyfish.jimmer.sql.kt.filter.common.FileFilter
import org.babyfish.jimmer.sql.kt.model.filter.File
import org.babyfish.jimmer.sql.kt.model.filter.User
import org.babyfish.jimmer.sql.kt.model.filter.addBy
import org.babyfish.jimmer.sql.kt.model.filter.by
import org.babyfish.jimmer.sql.runtime.DbLiteral.DbNull
import java.time.LocalDateTime
import kotlin.test.Test

class SaveTest : AbstractMutationTest() {

    override fun sqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient {
        return super.sqlClient {
            block()
            addFilters(FileFilter())
        }
    }

    @Test
    fun testSaveWithOneToManyWithoutUpsert() {
        val sqlClient = sqlClient {
            setIdGenerator(File::class, PreparedIdGenerator(10000L))
        }
        val file = new(File::class).by {
            id = 8L
            childFiles().addBy {
                id = 9L
            }
            childFiles().addBy {
                name = "new_file"
            }
        }
        FileFilter.withUser(2L) {
            executeAndExpectResult({
                sqlClient.entities.save(file, con = it)
            }) {
                statement { 
                    sql(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                            "from FILE tb_1_ where tb_1_.ID = ?"
                    )
                    variables(8L)
                }
                statement {
                    sql(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID from FILE tb_1_ where tb_1_.ID = ?"
                    )
                    variables(9L)
                    queryReason(QueryReason.TARGET_NOT_TRANSFERABLE)
                }
                statement {
                    sql(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                            "from FILE tb_1_ " +
                            "inner join FILE tb_2_ on tb_1_.PARENT_ID = tb_2_.ID " +
                            "where (tb_1_.NAME, tb_2_.ID) = (?, ?)"
                    )
                    variables("new_file", 8L)
                }
                statement {
                    sql(
                        "insert into FILE(ID, NAME, PARENT_ID) values(?, ?, ?)"
                    )
                    variables(10000L, "new_file", 8L)
                }
                statement {
                    sql(
                        "select tb_1_.ID from FILE tb_1_ " +
                            "inner join FILE tb_2_ on tb_1_.PARENT_ID = tb_2_.ID " +
                            "inner join FILE tb_3_ on tb_2_.PARENT_ID = tb_3_.ID " +
                            "where tb_3_.PARENT_ID = ? and tb_3_.ID not in (?, ?)"
                    )
                    variables(8L, 9L, 10000L)
                    queryReason(QueryReason.TOO_DEEP)
                }
                statement {
                    sql(
                        "select FILE_ID, USER_ID from FILE_USER_MAPPING tb_1_ " +
                            "inner join FILE tb_2_ on tb_1_.FILE_ID = tb_2_.ID " +
                            "where exists(" +
                            "--->select * " +
                            "--->from FILE tb_3_ " +
                            "--->where tb_2_.PARENT_ID = tb_3_.ID " +
                            "--->and tb_3_.PARENT_ID = ? " +
                            "--->and tb_3_.ID not in (?, ?))"
                    )
                    variables(8L, 9L, 10000L)
                    queryReason(QueryReason.TOO_DEEP)
                }
                statement {
                    sql(
                        "delete from FILE tb_1_ where exists(" +
                            "--->select * " +
                            "--->from FILE tb_2_ " +
                            "--->where tb_1_.PARENT_ID = tb_2_.ID " +
                            "--->and tb_2_.PARENT_ID = ? " +
                            "--->and tb_2_.ID not in (?, ?)" +
                            ")"
                    )
                    variables(8L, 9L, 10000L)
                }
                statement {
                    sql(
                        "select FILE_ID, USER_ID from FILE_USER_MAPPING tb_1_ " +
                            "inner join FILE tb_2_ on tb_1_.FILE_ID = tb_2_.ID " +
                            "where tb_2_.PARENT_ID = ? and tb_2_.ID not in (?, ?)"
                    )
                    variables(8L, 9L, 10000L)
                    queryReason(QueryReason.UPSERT_NOT_SUPPORTED)
                }
                statement {
                    sql("delete from FILE_USER_MAPPING where FILE_ID = ? and USER_ID = ?")
                    batchVariables(0, 10L, 3L)
                    batchVariables(1, 10L, 4L)
                    batchVariables(2, 11L, 2L)
                    batchVariables(3, 11L, 4L)
                    batchVariables(4, 12L, 2L)
                    batchVariables(5, 12L, 3L)
                    batchVariables(6, 13L, 3L)
                    batchVariables(7, 13L, 4L)
                }
                statement {
                    sql(
                        "delete from FILE where PARENT_ID = ? and ID not in (?, ?)"
                    )
                    variables(8L, 9L, 10000L)
                }
                entity {
                    original("{\"id\":8,\"childFiles\":[{\"id\":9},{\"name\":\"new_file\"}]}")
                    modified(
                        "{" +
                            "--->\"id\":8," +
                            "--->\"childFiles\":[" +
                            "--->--->{\"id\":9,\"parent\":{\"id\":8}}," +
                            "--->--->{\"id\":10000,\"name\":\"new_file\",\"parent\":{\"id\":8}}" +
                            "--->]" +
                            "}"
                    )
                }
            }
        }
    }

    @Test
    fun testSaveWithManyToMany() {
        val sqlClient = sqlClient {
            setIdGenerator(User::class, PreparedIdGenerator(10000L))
            setDialect(H2Dialect())
        }
        val file = new(File::class).by {
            id = 20L
            users().addBy {
                id = 3L
            }
            users().addBy {
                name = "Andrew"
            }
        }
        FileFilter.withUser(2L) {
            executeAndExpectResult({
                sqlClient.entities.save(file, it)
            }) {
                statement {
                    sql(
                        "select tb_1_.ID, tb_1_.NAME from file_user tb_1_ " +
                            "where tb_1_.NAME = ? and tb_1_.DELETED_TIME is null"
                    )
                    variables("Andrew")
                    queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED)
                }
                statement {
                    sql(
                        "insert into file_user(ID, NAME, DELETED_TIME) values(?, ?, ?)"
                    )
                    variables(10000L, "Andrew", DbNull(LocalDateTime::class.java))
                }
                statement {
                    sql(
                        "delete from FILE_USER_MAPPING where FILE_ID = ? and not (USER_ID = any(?))"
                    )
                    variables(20L, TypedList("bigint", arrayOf(3L, 10000L)))
                }
                statement {
                    sql(
                        "merge into FILE_USER_MAPPING(FILE_ID, USER_ID) " +
                            "key(FILE_ID, USER_ID) values(?, ?)"
                    )
                    batchVariables(0, 20L, 3L)
                    batchVariables(1, 20L, 10000L)
                }
                entity {
                    original("{\"id\":20,\"users\":[{\"id\":3},{\"name\":\"Andrew\"}]}")
                    modified(
                        "{" +
                            "--->\"id\":20," +
                            "--->\"users\":[" +
                            "--->--->{\"id\":3}," +
                            "--->--->{\"id\":10000,\"name\":\"Andrew\",\"deletedTime\":null}" +
                            "--->]" +
                            "}"
                    )
                }
            }
        }
    }

    @Test
    fun testSaveWithManyToManyWithoutDefaultDialect() {
        val sqlClient = sqlClient {
            setIdGenerator(User::class, PreparedIdGenerator(10000L))
        }
        val file = new(File::class).by {
            id = 20L
            users().addBy {
                id = 3L
            }
            users().addBy {
                name = "Andrew"
            }
        }
        FileFilter.withUser(2L) {
            executeAndExpectResult({
                sqlClient.entities.save(file, it)
            }) {
                statement {
                    sql(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID from FILE tb_1_ where tb_1_.ID = ?"
                    )
                    variables(20L)
                    queryReason(QueryReason.UPSERT_NOT_SUPPORTED)
                }
                statement {
                    sql(
                        "select tb_1_.ID, tb_1_.NAME from file_user tb_1_ " +
                            "where tb_1_.NAME = ? and tb_1_.DELETED_TIME is null"
                    )
                    variables("Andrew")
                    queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED)
                }
                statement {
                    sql(
                        "insert into file_user(ID, NAME, DELETED_TIME) values(?, ?, ?)",
                    )
                    variables(10000L, "Andrew", DbNull(LocalDateTime::class.java))
                }
                statement {
                    sql(
                        "select USER_ID from FILE_USER_MAPPING where FILE_ID = ?"
                    )
                    variables(20L)
                    queryReason(QueryReason.UPSERT_NOT_SUPPORTED)
                }
                statement {
                    sql(
                        "delete from FILE_USER_MAPPING where FILE_ID = ? and USER_ID = ?"
                    )
                    batchVariables(0, 20L, 1L)
                    batchVariables(1, 20L, 2L)
                    batchVariables(2, 20L, 4L)
                }
                statement {
                    sql(
                        "insert into FILE_USER_MAPPING(FILE_ID, USER_ID) values(?, ?)"
                    )
                    batchVariables(0, 20L, 3L)
                    batchVariables(1, 20L, 10000L)
                }
                entity {
                    original("{\"id\":20,\"users\":[{\"id\":3},{\"name\":\"Andrew\"}]}")
                    modified(
                        "{" +
                            "--->\"id\":20," +
                            "--->\"users\":[" +
                            "--->--->{\"id\":3}," +
                            "--->--->{\"id\":10000,\"name\":\"Andrew\",\"deletedTime\":null}" +
                            "--->]" +
                            "}"
                    )
                }
            }
        }
    }
}