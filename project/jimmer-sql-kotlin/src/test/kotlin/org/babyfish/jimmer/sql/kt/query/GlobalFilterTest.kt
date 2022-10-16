package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs
import org.babyfish.jimmer.sql.kt.model.inheritance.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class GlobalFilterTest : AbstractQueryTest() {

    private lateinit var _sqlClient: KSqlClient

    private lateinit var _sqlClientForDeleted: KSqlClient

    @BeforeTest
    fun initialize() {
        _sqlClient = sqlClient {
            addFilters(UNDELETED_FILTER)
            addDisabledFilters(DELETED_FILTER)
        }
        _sqlClientForDeleted = sqlClient.filters {
            disable(UNDELETED_FILTER)
            enable(DELETED_FILTER)
        }
    }

    @Test
    fun testQueryUndeletedRoleWithPermissions() {
        executeAndExpect(
            _sqlClient.createQuery(Role::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        permissions {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME 
                    |from ROLE as tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(false)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME 
                    |from PERMISSION as tb_1_ 
                    |where tb_1_.ROLE_ID = ? and tb_1_.DELETED = ?""".trimMargin()
            ).variables(100L, false)
        }
    }

    @Test
    fun testQueryUndeletedPermissionWithRole() {
        executeAndExpect(
            _sqlClient.createQuery(Permission::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        role {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID 
                    |from PERMISSION as tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(false)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME 
                    |from ROLE as tb_1_ where tb_1_.ID in (?, ?) 
                    |and tb_1_.DELETED = ?""".trimMargin()
            ).variables(100L, 200L, false)
        }
    }

    @Test
    fun testAdministratorWithRoles() {
        executeAndExpect(
            _sqlClient.createQuery(Administrator::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        roles {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME 
                    |from ADMINISTRATOR as tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(false)
            statement(1).sql(
                """select tb_2_.ADMINISTRATOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME 
                    |from ROLE as tb_1_ 
                    |inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ROLE_ID 
                    |where tb_2_.ADMINISTRATOR_ID in (?, ?) and tb_1_.DELETED = ?""".trimMargin()
            ).variables(1L, 3L, false)
        }
    }
}

private val UNDELETED_FILTER: KFilter<NamedEntity> =
    object: KFilter<NamedEntity> {

        override fun filter(args: KFilterArgs<NamedEntity>) =
            args.where(args.table.deleted eq false)
    }

private val DELETED_FILTER: KFilter<NamedEntity> =
    object: KFilter<NamedEntity> {

        override fun filter(args: KFilterArgs<NamedEntity>) =
            args.where(args.table.deleted eq true)
    }