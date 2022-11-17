package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.common.PreparedIdGenerator
import org.babyfish.jimmer.sql.kt.model.inheritance.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.BeforeTest
import kotlin.test.Test

class InheritanceMutationTest : AbstractMutationTest() {

    fun sqlClient(): KSqlClient =
        sqlClient {
            setIdGenerator(Role::class, PreparedIdGenerator(101L))
            setIdGenerator(Permission::class, PreparedIdGenerator(101L, 102L))
            addDraftInterceptor(NamedEntityDraftInterceptor)
        }

    @Test
    fun testSaveRole() {
        executeAndExpectResult({con ->
            sqlClient().entities.save(
                new(Role::class).by {
                    name = "role"
                    permissions().addBy {
                        name = "permission-1"
                    }
                    permissions().addBy {
                        name = "permission-2"
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
                        |from ROLE as tb_1_ 
                        |where tb_1_.NAME = ? 
                        |for update""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into ROLE(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ID) 
                        |values(?, ?, ?, ?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from PERMISSION as tb_1_ 
                        |where tb_1_.NAME = ? 
                        |for update""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into PERMISSION(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ROLE_ID, ID) 
                        |values(?, ?, ?, ?, ?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from PERMISSION as tb_1_ 
                        |where tb_1_.NAME = ? 
                        |for update""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into PERMISSION(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ROLE_ID, ID) 
                        |values(?, ?, ?, ?, ?, ?)""".trimMargin()
                )
            }
            entity {
                original(
                    """{"name":"role","permissions":[{"name":"permission-1"},{"name":"permission-2"}]}"""
                )
                modified(
                    """{
                        |--->"name":"role",
                        |--->"deleted":false,
                        |--->"createdTime":"2022-10-03 00:00:00",
                        |--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->"permissions":[
                        |--->--->{
                        |--->--->--->"name":"permission-1",
                        |--->--->--->"deleted":false,
                        |--->--->--->"createdTime":"2022-10-03 00:00:00",
                        |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->--->--->"role":{"id":101},"id":101
                        |--->--->},{
                        |--->--->--->"name":"permission-2",
                        |--->--->--->"deleted":false,
                        |--->--->--->"createdTime":"2022-10-03 00:00:00",
                        |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->--->--->"role":{"id":101},"id":102
                        |--->--->}
                        |--->
                        |],"id":101}""".trimMargin()
                )
            }
        }
    }

    @Test
    fun testSavePermission() {
        executeAndExpectResult({con ->
            sqlClient().entities.save(
                new(Permission::class).by {
                    name = "permission"
                    role().apply {
                        name = "role"
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
                        |from ROLE as tb_1_ 
                        |where tb_1_.NAME = ? 
                        |for update""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into ROLE(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ID) 
                        |values(?, ?, ?, ?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from PERMISSION as tb_1_ 
                        |where tb_1_.NAME = ? 
                        |for update""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into PERMISSION(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ROLE_ID, ID) 
                        |values(?, ?, ?, ?, ?, ?)""".trimMargin()
                )
            }
            entity {
                original(
                    """{"name":"permission","role":{"name":"role"}}"""
                )
                modified(
                    """{
                        |--->"name":"permission",
                        |--->"deleted":false,
                        |--->"createdTime":"2022-10-03 00:00:00",
                        |--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->"role":{
                        |--->--->"name":"role",
                        |--->--->"deleted":false,
                        |--->--->"createdTime":"2022-10-03 00:00:00",
                        |--->--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->--->"id":101
                        |--->},
                        |--->"id":101
                        |}""".trimMargin()
                )
            }
        }
    }

    companion object {

        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        private val CREATED_TIME = LocalDateTime.parse("2022-10-03 00:00:00", FORMATTER)

        private val MODIFIED_TIME = LocalDateTime.parse("2022-10-03 00:10:00", FORMATTER)
    }

    private object NamedEntityDraftInterceptor : DraftInterceptor<NamedEntityDraft> {
        override fun beforeSave(draft: NamedEntityDraft, isNew: Boolean) {
            if (!isLoaded(draft, NamedEntity::deleted)) {
                draft.deleted = false
            }
            if (!isLoaded(draft, NamedEntity::modifiedTime)) {
                draft.modifiedTime = MODIFIED_TIME
            }
            if (isNew && !isLoaded(draft, NamedEntity::createdTime)) {
                draft.createdTime = CREATED_TIME
            }
        }
    }
}