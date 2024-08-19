package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.common.PreparedIdGenerator
import org.babyfish.jimmer.sql.kt.filter.common.FileFilter
import org.babyfish.jimmer.sql.kt.model.filter.File
import org.babyfish.jimmer.sql.kt.model.filter.User
import org.babyfish.jimmer.sql.kt.model.filter.addBy
import org.babyfish.jimmer.sql.kt.model.filter.by
import kotlin.test.Test

class SaveTest : AbstractMutationTest() {

    override fun sqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient {
        return super.sqlClient {
            block()
            addFilters(FileFilter())
        }
    }

    @Test
    fun saveWithOneToMany() {
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
                        "update FILE set PARENT_ID = ? where ID = ?"
                    )
                    variables(8L, 9L)
                }
                statement {
                    sql(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                            "from FILE tb_1_ " +
                            "where tb_1_.NAME = ? and tb_1_.PARENT_ID = ? and exists(" +
                            "--->select 1 " +
                            "--->from FILE_USER_MAPPING tb_3_ " +
                            "--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                            ")"
                    )
                    variables("new_file", 8L, 2L)
                }
                statement {
                    sql(
                        "insert into FILE(ID, NAME, PARENT_ID) values(?, ?, ?)"
                    )
                    variables(10000L, "new_file", 8L)
                }
                statement {
                    sql(
                        "select tb_1_.ID " +
                            "from FILE tb_1_ " +
                            "where tb_1_.PARENT_ID = ? and tb_1_.ID not in (?, ?) and exists(" +
                            "--->select 1 " +
                            "--->from FILE_USER_MAPPING tb_3_ " +
                            "--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                            ")"
                    )
                    variables(8L, 9L, 10000L, 2L)
                }
                statement {
                    sql(
                        "delete from FILE_USER_MAPPING where FILE_ID in (?, ?)"
                    )
                    variables(11L, 12L)
                }
                statement {
                    sql(
                        "select ID from FILE where PARENT_ID in (?, ?)"
                    )
                    variables(11L, 12L)
                }
                statement {
                    sql(
                        "delete from FILE where ID in (?, ?)"
                    )
                    variables(11L, 12L)
                }
                entity {
                    original("{\"id\":8,\"childFiles\":[{\"id\":9},{\"name\":\"new_file\"}]}")
                    modified(
                        "{" +
                            "--->\"id\":8," +
                            "--->\"childFiles\":[" +
                            "--->--->{\"id\":9}," +
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
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                            "from FILE tb_1_ " +
                            "where tb_1_.ID = ? and exists(" +
                            "--->select 1 " +
                            "--->from FILE_USER_MAPPING tb_2_ " +
                            "--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                            ")"
                    )
                    variables(20L, 2L)
                }
                statement {
                    sql(
                        "select tb_1_.ID, tb_1_.NAME " +
                            "from file_user tb_1_ " +
                            "where tb_1_.NAME = ? and tb_1_.DELETED_TIME is null"
                    )
                    variables("Andrew")
                }
                statement {
                    sql(
                        "insert into file_user(ID, NAME) values(?, ?)"
                    )
                    variables(10000L, "Andrew")
                }
                statement {
                    sql(
                        "select tb_1_.ID " +
                            "from file_user tb_1_ " +
                            "inner join FILE_USER_MAPPING tb_2_ on tb_1_.ID = tb_2_.USER_ID " +
                            "where tb_2_.FILE_ID = ? and tb_1_.DELETED_TIME is null"
                    )
                    variables(20L)
                }
                statement {
                    sql(
                        "delete from FILE_USER_MAPPING where (FILE_ID, USER_ID) in ((?, ?), (?, ?))"
                    )
                    variables(20L, 2L, 20L, 4L)
                }
                statement {
                    sql(
                        "insert into FILE_USER_MAPPING(FILE_ID, USER_ID) values(?, ?), (?, ?)"
                    )
                    variables(20L, 3L, 20L, 10000L)
                }
                entity {
                    original("{\"id\":20,\"users\":[{\"id\":3},{\"name\":\"Andrew\"}]}")
                    modified(
                        "{" +
                            "--->\"id\":20," +
                            "--->\"users\":[" +
                            "--->--->{\"id\":3}," +
                            "--->--->{\"id\":10000,\"name\":\"Andrew\"}" +
                            "--->]" +
                            "}"
                    )
                }
            }
        }
    }
}