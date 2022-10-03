package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.common.PreparedIdGenerator
import org.babyfish.jimmer.sql.kt.model.inheritance.Permission
import org.babyfish.jimmer.sql.kt.model.inheritance.Role
import org.babyfish.jimmer.sql.kt.model.inheritance.addBy
import org.babyfish.jimmer.sql.kt.model.inheritance.by
import kotlin.test.Test

class InheritanceMutationTest : AbstractMutationTest() {

    @Test
    fun testSaveRole() {
        executeAndExpectResult({con ->
            sqlClient {
                setIdGenerator(Role::class, PreparedIdGenerator(101L))
                setIdGenerator(Permission::class, PreparedIdGenerator(101L, 102L))
            }.entities.save(
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
                        |where tb_1_.NAME = ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into ROLE(NAME, ID) values(?, ?)"""
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from PERMISSION as tb_1_ 
                        |where tb_1_.NAME = ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into PERMISSION(NAME, ROLE_ID, ID) values(?, ?, ?)"""
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from PERMISSION as tb_1_ 
                        |where tb_1_.NAME = ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into PERMISSION(NAME, ROLE_ID, ID) values(?, ?, ?)"""
                )
            }
            entity {
                original(
                    """{"name":"role","permissions":[{"name":"permission-1"},{"name":"permission-2"}]}"""
                )
                modified(
                    """{"name":"role","permissions":[{"name":"permission-1","role":{"id":101},"id":101},{"name":"permission-2","role":{"id":101},"id":102}],"id":101}"""
                )
            }
        }
    }

    @Test
    fun testSavePermission() {
        executeAndExpectResult({con ->
            sqlClient {
                setIdGenerator(Role::class, PreparedIdGenerator(101L))
                setIdGenerator(Permission::class, PreparedIdGenerator(101L))
            }.entities.save(
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
                    """select tb_1_.ID, tb_1_.NAME from ROLE as tb_1_ where tb_1_.NAME = ?"""
                )
            }
            statement {
                sql(
                    """insert into ROLE(NAME, ID) values(?, ?)"""
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME from PERMISSION as tb_1_ where tb_1_.NAME = ?"""
                )
            }
            statement {
                sql(
                    """insert into PERMISSION(NAME, ROLE_ID, ID) values(?, ?, ?)"""
                )
            }
            entity {
                original(
                    """{"name":"permission","role":{"name":"role"}}"""
                )
                modified(
                    """{"name":"permission","role":{"name":"role","id":101},"id":101}"""
                )
            }
        }
    }
}