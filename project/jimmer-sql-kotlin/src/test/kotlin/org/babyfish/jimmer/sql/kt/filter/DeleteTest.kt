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
                    sql("select ID from FILE where PARENT_ID = ?")
                    variables(8L)
                }
                statement {
                    sql("delete from FILE_USER_MAPPING where FILE_ID in (?, ?, ?, ?, ?)")
                    variables(9L, 10L, 11L, 12L, 13L)
                }
                statement {
                    sql("select ID from FILE where PARENT_ID in (?, ?, ?, ?, ?)")
                    variables(9L, 10L, 11L, 12L, 13L)
                }
                statement {
                    sql("delete from FILE where ID in (?, ?, ?, ?, ?)")
                    variables(9L, 10L, 11L, 12L, 13L)
                }
                statement {
                    sql("delete from FILE where ID = ?")
                    variables(8L)
                }
            }
        }
    }
}