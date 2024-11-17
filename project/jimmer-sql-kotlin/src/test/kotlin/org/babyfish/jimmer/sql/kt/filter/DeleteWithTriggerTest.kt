package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.kt.common.AbstractTriggerTest
import org.babyfish.jimmer.sql.kt.filter.common.FileFilter
import org.babyfish.jimmer.sql.kt.model.filter.File
import org.babyfish.jimmer.sql.kt.model.filter.User
import kotlin.test.Test

class DeleteWithTriggerTest : AbstractTriggerTest() {

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
                    queryReason(QueryReason.TRIGGER)
                }
                statement {
                    sql("delete from FILE_USER_MAPPING where FILE_ID = ? and USER_ID = ?")
                    batchVariables(0, 8L, 2L)
                    batchVariables(1, 8L, 3L)
                    batchVariables(2, 8L, 4L)
                }
                statement {
                    sql("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID from FILE tb_1_ where tb_1_.PARENT_ID = ?")
                    variables(8L)
                    queryReason(QueryReason.TRIGGER)
                }
                statement {
                    sql(
                        """select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID 
                            |from FILE tb_1_ 
                            |inner join FILE tb_2_ on tb_1_.PARENT_ID = tb_2_.ID 
                            |where tb_2_.ID in (?, ?, ?, ?, ?)""".trimMargin()
                    )
                    variables(9L, 10L, 11L, 12L, 13L)
                    queryReason(QueryReason.TRIGGER)
                }
                statement {
                    sql("select FILE_ID, USER_ID from FILE_USER_MAPPING where FILE_ID in (?, ?, ?, ?, ?)")
                    variables(9L, 10L, 11L, 12L, 13L)
                    queryReason(QueryReason.TRIGGER)
                }
                statement {
                    sql(
                        "delete from FILE_USER_MAPPING where FILE_ID = ? and USER_ID = ?"
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
                    sql("delete from FILE where ID in (?, ?, ?, ?, ?)")
                    variables(9L, 10L, 11L, 12L, 13L)
                }
                statement {
                    sql("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID from FILE tb_1_ where tb_1_.ID = ?")
                    variables(8L)
                    queryReason(QueryReason.TRIGGER)
                }
                statement {
                    sql("delete from FILE where ID = ?")
                    variables(8L)
                }
                rowCount(File::class, 6)
                rowCount(File::users, 13)
                rowCount(User::files, 13)
            }
        }

        assertEvents(
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=8, detachedTargetId=2, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=2, detachedTargetId=8, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=8, detachedTargetId=3, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=3, detachedTargetId=8, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=8, detachedTargetId=4, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=4, detachedTargetId=8, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=9, detachedTargetId=2, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=2, detachedTargetId=9, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=9, detachedTargetId=3, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=3, detachedTargetId=9, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=10, detachedTargetId=3, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=3, detachedTargetId=10, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=10, detachedTargetId=4, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=4, detachedTargetId=10, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=11, detachedTargetId=2, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=2, detachedTargetId=11, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=11, detachedTargetId=4, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=4, detachedTargetId=11, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=12, detachedTargetId=2, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=2, detachedTargetId=12, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=12, detachedTargetId=3, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=3, detachedTargetId=12, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=13, detachedTargetId=3, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=3, detachedTargetId=13, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.users, sourceId=13, detachedTargetId=4, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.User.files, sourceId=4, detachedTargetId=13, attachedTargetId=null, reason=null}",
            "Event{oldEntity={\"id\":9,\"name\":\"ipconfig\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.parent, sourceId=9, detachedTargetId=8, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.childFiles, sourceId=8, detachedTargetId=9, attachedTargetId=null, reason=null}",
            "Event{oldEntity={\"id\":10,\"name\":\"mtree\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.parent, sourceId=10, detachedTargetId=8, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.childFiles, sourceId=8, detachedTargetId=10, attachedTargetId=null, reason=null}",
            "Event{oldEntity={\"id\":11,\"name\":\"purge\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.parent, sourceId=11, detachedTargetId=8, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.childFiles, sourceId=8, detachedTargetId=11, attachedTargetId=null, reason=null}",
            "Event{oldEntity={\"id\":12,\"name\":\"ssh\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.parent, sourceId=12, detachedTargetId=8, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.childFiles, sourceId=8, detachedTargetId=12, attachedTargetId=null, reason=null}",
            "Event{oldEntity={\"id\":13,\"name\":\"tcpctl\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.parent, sourceId=13, detachedTargetId=8, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.childFiles, sourceId=8, detachedTargetId=13, attachedTargetId=null, reason=null}",
            "Event{oldEntity={\"id\":8,\"name\":\"sbin\",\"parent\":{\"id\":1}}, newEntity=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.parent, sourceId=8, detachedTargetId=1, attachedTargetId=null, reason=null}",
            "AssociationEvent{prop=org.babyfish.jimmer.sql.kt.model.filter.File.childFiles, sourceId=1, detachedTargetId=8, attachedTargetId=null, reason=null}"
        )
    }
}