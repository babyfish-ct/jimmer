package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.filter.common.FileFilter
import org.babyfish.jimmer.sql.kt.model.filter.File
import kotlin.test.Test

class DeleteTest : AbstractMutationTest() {

    private val _sqlClient = sqlClient {
        addFilters(FileFilter())
    }

    @Test
    fun test() {
        FileFilter.withUser(2L) {
            executeAndExpectResult({
                _sqlClient.entities.delete(File::class, 8L, it)
            }) {
                statement {
                    sql("select USER_ID from FILE_USER_MAPPING where FILE_ID = ?")
                    variables(8L)
                    queryReason(QueryReason.UPSERT_NOT_SUPPORTED)
                }
                statement {
                    sql("delete from FILE_USER_MAPPING where FILE_ID = ? and USER_ID = ?")
                    batchVariables(0, 8L, 2L)
                    batchVariables(1, 8L, 3L)
                }
                statement {
                    sql(
                        """select tb_1_.ID 
                            |from FILE tb_1_ 
                            |inner join FILE tb_2_ on tb_1_.PARENT_ID = tb_2_.ID 
                            |inner join FILE tb_3_ on tb_2_.PARENT_ID = tb_3_.ID 
                            |where tb_3_.PARENT_ID = ?""".trimMargin()
                    )
                    variables(8L)
                    queryReason(QueryReason.TOO_DEEP)
                }
                statement {
                    sql(
                        """select tb_1_.FILE_ID, tb_1_.USER_ID 
                            |from FILE_USER_MAPPING tb_1_ 
                            |inner join FILE tb_2_ on tb_1_.FILE_ID = tb_2_.ID 
                            |where exists(
                            |--->select * 
                            |--->from FILE tb_3_ 
                            |--->where tb_2_.PARENT_ID = tb_3_.ID and tb_3_.PARENT_ID = ?
                            |)""".trimMargin()
                    )
                    variables(8L)
                }
                statement {
                    sql(
                        """delete from FILE tb_1_ 
                            |where exists(
                            |--->select * 
                            |--->from FILE tb_2_ 
                            |--->where tb_1_.PARENT_ID = tb_2_.ID and tb_2_.PARENT_ID = ?
                            |)""".trimMargin()
                    )
                    variables(8L)
                }
                statement {
                    sql(
                        "select tb_1_.FILE_ID, tb_1_.USER_ID " +
                            "from FILE_USER_MAPPING tb_1_ " +
                            "inner join FILE tb_2_ on tb_1_.FILE_ID = tb_2_.ID " +
                            "where tb_2_.PARENT_ID = ?"
                    )
                    variables(8L)
                }
                statement {
                    sql(
                        """delete from FILE_USER_MAPPING where FILE_ID = ? and USER_ID = ?""".trimMargin()
                    )
                    batchVariables(0, 9L, 2L)
                    batchVariables(1, 9L, 3L)
                    batchVariables(2, 10L, 3L)
                    batchVariables(3, 10L, 4L)
                    batchVariables(4, 11L, 2L)
                    batchVariables(5, 11L, 4L)
                    batchVariables(6, 12L, 2L)
                    batchVariables(7, 12L, 3L)
                    batchVariables(8, 13L, 3L)
                    batchVariables(9, 13L, 4L)
                }
                statement {
                    sql("delete from FILE where PARENT_ID = ?")
                    variables(8L)
                }
                statement {
                    sql("delete from FILE where ID = ?")
                    variables(8L)
                }
                rowCount(File::class, 6)
                rowCount(File::users, 13)
            }
        }
    }
}