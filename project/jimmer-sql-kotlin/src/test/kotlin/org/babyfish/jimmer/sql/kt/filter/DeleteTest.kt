package org.babyfish.jimmer.sql.kt.filter

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
                    sql("delete from FILE_USER_MAPPING where FILE_ID = ?")
                    variables(8L)
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
                }
                statement {
                    sql(
                        """select FILE_ID, USER_ID 
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
                        """delete from FILE_USER_MAPPING tb_1_ 
                            |where exists (
                            |--->select * 
                            |--->from FILE tb_2_ 
                            |--->where tb_1_.FILE_ID = tb_2_.ID and tb_2_.PARENT_ID = ?
                            |)""".trimMargin()
                    )
                    variables(8L)
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