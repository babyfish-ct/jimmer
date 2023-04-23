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

    private lateinit var _sqlClientForDeleted: KSqlClient

    @BeforeTest
    fun initialize() {
        _sqlClientForDeleted = sqlClient.filters {
            disable(sqlClient.filters.builtIns.getDeclaredNotDeletedFilter(NamedEntity::class))
            enable(sqlClient.filters.builtIns.getDeclaredAlreadyDeletedFilter(NamedEntity::class))
        }
    }

    @Test
    fun testQueryUndeletedRoleWithPermissions() {
        executeAndExpect(
            sqlClient.createQuery(Role::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        permissions {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ROLE tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(false)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from PERMISSION tb_1_ 
                    |where tb_1_.ROLE_ID = ? and tb_1_.DELETED = ?""".trimMargin()
            ).variables(100L, false)
            rows(
                """[
                    |--->{
                    |--->--->"name":"r_1",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"permissions":[
                    |--->--->--->{
                    |--->--->--->--->"name":"p_1",
                    |--->--->--->--->"deleted":false,
                    |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":1000
                    |--->--->--->}
                    |--->--->],
                    |--->--->"id":100
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryUndeletedPermissionWithRole() {
        executeAndExpect(
            sqlClient.createQuery(Permission::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        role {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED, tb_1_.ROLE_ID 
                    |from PERMISSION tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(false)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ROLE tb_1_ where tb_1_.ID in (?, ?) 
                    |and tb_1_.DELETED = ?""".trimMargin()
            ).variables(100L, 200L, false)
            rows(
                """[
                    |--->{
                    |--->--->"name":"p_1",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"role":{
                    |--->--->--->"name":"r_1",
                    |--->--->--->"deleted":false,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"id":100
                    |--->--->},
                    |--->--->"id":1000
                    |--->},{
                    |--->--->"name":"p_3",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"role":null,
                    |--->--->"id":3000
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryUndeletedAdministratorWithRoles() {
        executeAndExpect(
            sqlClient.createQuery(Administrator::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        roles {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ADMINISTRATOR tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(false)
            statement(1).sql(
                """select tb_2_.ADMINISTRATOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ROLE tb_1_ 
                    |inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ on tb_1_.ID = tb_2_.ROLE_ID 
                    |where tb_2_.ADMINISTRATOR_ID in (?, ?) and tb_1_.DELETED = ?""".trimMargin()
            ).variables(1L, 3L, false)
            rows(
                """[
                    |--->{
                    |--->--->"name":"a_1",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"roles":[
                    |--->--->--->{
                    |--->--->--->--->"name":"r_1",
                    |--->--->--->--->"deleted":false,
                    |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":100
                    |--->--->--->}
                    |--->--->],
                    |--->--->"id":1
                    |--->},{
                    |--->--->"name":"a_3",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"roles":[
                    |--->--->--->{
                    |--->--->--->--->"name":"r_1",
                    |--->--->--->--->"deleted":false,
                    |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":100
                    |--->--->--->}
                    |--->--->],
                    |--->--->"id":3
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryUndeletedRoleWithAdministrators() {
        executeAndExpect(
            sqlClient.createQuery(Role::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        administrators {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ROLE tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(false)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ADMINISTRATOR tb_1_ 
                    |inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ on tb_1_.ID = tb_2_.ADMINISTRATOR_ID 
                    |where tb_2_.ROLE_ID = ? and tb_1_.DELETED = ?""".trimMargin()
            ).variables(100L, false)
            rows(
                """[
                    |--->{
                    |--->--->"name":"r_1",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"administrators":[
                    |--->--->--->{
                    |--->--->--->--->"name":"a_1",
                    |--->--->--->--->"deleted":false,
                    |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":1
                    |--->--->--->},{
                    |--->--->--->--->"name":"a_3",
                    |--->--->--->--->"deleted":false,
                    |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":3
                    |--->--->--->}
                    |--->--->],
                    |--->--->"id":100
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryUndeletedAdministratorWithAdministratorMetadata() {
        executeAndExpect(
            sqlClient.createQuery(Administrator::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        metadata {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ADMINISTRATOR tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(false)
            statement(1).sql(
                """select tb_1_.ADMINISTRATOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.EMAIL, tb_1_.WEBSITE, tb_1_.DELETED 
                    |from ADMINISTRATOR_METADATA tb_1_ 
                    |where tb_1_.ADMINISTRATOR_ID in (?, ?) and tb_1_.DELETED = ?""".trimMargin()
            ).variables(1L, 3L, false)
            rows(
                """[
                    |--->{
                    |--->--->"name":"a_1",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"metadata":{
                    |--->--->--->"name":"am_1",
                    |--->--->--->"deleted":false,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"email":"email_1",
                    |--->--->--->"website":"website_1",
                    |--->--->--->"id":10
                    |--->--->},
                    |--->--->"id":1
                    |--->},{
                    |--->--->"name":"a_3",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"metadata":{
                    |--->--->--->"name":"am_3",
                    |--->--->--->"deleted":false,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"email":"email_3",
                    |--->--->--->"website":"website_3",
                    |--->--->--->"id":30
                    |--->--->},
                    |--->--->"id":3
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryUndeletedAdministratorMetadataWithAdministrator() {
        executeAndExpect(
            sqlClient.createQuery(AdministratorMetadata::class) {
                select(
                    table.fetchBy {
                        allTableFields()
                        deleted()
                        administrator {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.EMAIL, tb_1_.WEBSITE, tb_1_.DELETED, tb_1_.ADMINISTRATOR_ID 
                    |from ADMINISTRATOR_METADATA tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(false)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ADMINISTRATOR tb_1_ 
                    |where tb_1_.ID in (?, ?) and tb_1_.DELETED = ?""".trimMargin()
            ).variables(1L, 3L, false)
            rows(
                """[
                    |--->{
                    |--->--->"name":"am_1",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"email":"email_1",
                    |--->--->"website":"website_1",
                    |--->--->"administrator":{
                    |--->--->--->"name":"a_1",
                    |--->--->--->"deleted":false,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"id":1
                    |--->--->},
                    |--->--->"id":10
                    |--->},{
                    |--->--->"name":"am_3",
                    |--->--->"deleted":false,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"email":"email_3",
                    |--->--->"website":"website_3",
                    |--->--->"administrator":{
                    |--->--->--->"name":"a_3",
                    |--->--->--->"deleted":false,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"id":3
                    |--->--->},
                    |--->--->"id":30
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryDeletedRoleWithPermissions() {
        executeAndExpect(
            _sqlClientForDeleted.createQuery(Role::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        permissions {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ROLE tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(true)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from PERMISSION tb_1_ 
                    |where tb_1_.ROLE_ID = ? and tb_1_.DELETED = ?""".trimMargin()
            ).variables(200L, true)
            rows(
                """[
                    |--->{
                    |--->--->"name":"r_2",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"permissions":[
                    |--->--->--->{
                    |--->--->--->--->"name":"p_4",
                    |--->--->--->--->"deleted":true,
                    |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":4000
                    |--->--->--->}
                    |--->--->],
                    |--->--->"id":200
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryDeletedPermissionWithRole() {
        executeAndExpect(
            _sqlClientForDeleted.createQuery(Permission::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        role {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED, tb_1_.ROLE_ID 
                    |from PERMISSION tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(true)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ROLE tb_1_ where tb_1_.ID in (?, ?) 
                    |and tb_1_.DELETED = ?""".trimMargin()
            ).variables(100L, 200L, true)
            rows(
                """[
                    |--->{
                    |--->--->"name":"p_2",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"role":null,
                    |--->--->"id":2000
                    |--->},{
                    |--->--->"name":"p_4",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"role":{
                    |--->--->--->"name":"r_2",
                    |--->--->--->"deleted":true,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"id":200
                    |--->--->},
                    |--->--->"id":4000
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryDeletedAdministratorWithRoles() {
        executeAndExpect(
            _sqlClientForDeleted.createQuery(Administrator::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        roles {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ADMINISTRATOR tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(true)
            statement(1).sql(
                """select tb_2_.ADMINISTRATOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ROLE tb_1_ 
                    |inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ on tb_1_.ID = tb_2_.ROLE_ID 
                    |where tb_2_.ADMINISTRATOR_ID in (?, ?) and tb_1_.DELETED = ?""".trimMargin()
            ).variables(2L, 4L, true)
            rows(
                """[
                    |--->{
                    |--->--->"name":"a_2",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"roles":[
                    |--->--->--->{
                    |--->--->--->--->"name":"r_2",
                    |--->--->--->--->"deleted":true,
                    |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":200
                    |--->--->--->}
                    |--->--->],
                    |--->--->"id":2
                    |--->},{
                    |--->--->"name":"a_4",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"roles":[
                    |--->--->--->{
                    |--->--->--->--->"name":"r_2",
                    |--->--->--->--->"deleted":true,"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":200
                    |--->--->--->}
                    |--->--->],
                    |--->--->"id":4
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryDeletedRoleWithAdministrators() {
        executeAndExpect(
            _sqlClientForDeleted.createQuery(Role::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        administrators {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ROLE tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(true)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ADMINISTRATOR tb_1_ 
                    |inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ on tb_1_.ID = tb_2_.ADMINISTRATOR_ID 
                    |where tb_2_.ROLE_ID = ? and tb_1_.DELETED = ?""".trimMargin()
            ).variables(200L, true)
            rows(
                """[
                    |--->{
                    |--->--->"name":"r_2",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"administrators":[
                    |--->--->--->{
                    |--->--->--->--->"name":"a_2",
                    |--->--->--->--->"deleted":true,
                    |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":2
                    |--->--->--->},{
                    |--->--->--->--->"name":"a_4",
                    |--->--->--->--->"deleted":true,
                    |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->--->"id":4
                    |--->--->--->}
                    |--->--->],
                    |--->--->"id":200
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryDeletedAdministratorWithAdministratorMetadata() {
        executeAndExpect(
            _sqlClientForDeleted.createQuery(Administrator::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        deleted()
                        metadata {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ADMINISTRATOR tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(true)
            statement(1).sql(
                """select tb_1_.ADMINISTRATOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.EMAIL, tb_1_.WEBSITE, tb_1_.DELETED 
                    |from ADMINISTRATOR_METADATA tb_1_ 
                    |where tb_1_.ADMINISTRATOR_ID in (?, ?) and tb_1_.DELETED = ?""".trimMargin()
            ).variables(2L, 4L, true)
            rows(
                """[
                    |--->{
                    |--->--->"name":"a_2",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"metadata":{
                    |--->--->--->"name":"am_2",
                    |--->--->--->"deleted":true,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"email":"email_2",
                    |--->--->--->"website":"website_2",
                    |--->--->--->"id":20
                    |--->--->},
                    |--->--->"id":2
                    |--->},{
                    |--->--->"name":"a_4",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"metadata":{
                    |--->--->--->"name":"am_4",
                    |--->--->--->"deleted":true,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"email":"email_4",
                    |--->--->--->"website":"website_4",
                    |--->--->--->"id":40
                    |--->--->},
                    |--->--->"id":4
                    |--->}
                    |]""".trimMargin()
            )
        }
    }

    @Test
    fun testQueryDeletedAdministratorMetadataWithAdministrator() {
        executeAndExpect(
            _sqlClientForDeleted.createQuery(AdministratorMetadata::class) {
                select(
                    table.fetchBy {
                        allTableFields()
                        deleted()
                        administrator {
                            allScalarFields()
                            deleted()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.EMAIL, tb_1_.WEBSITE, tb_1_.DELETED, tb_1_.ADMINISTRATOR_ID 
                    |from ADMINISTRATOR_METADATA tb_1_ 
                    |where tb_1_.DELETED = ?""".trimMargin()
            ).variables(true)
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED 
                    |from ADMINISTRATOR tb_1_ 
                    |where tb_1_.ID in (?, ?) and tb_1_.DELETED = ?""".trimMargin()
            ).variables(2L, 4L, true)
            rows(
                """[
                    |--->{
                    |--->--->"name":"am_2",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"email":"email_2",
                    |--->--->"website":"website_2",
                    |--->--->"administrator":{
                    |--->--->--->"name":"a_2",
                    |--->--->--->"deleted":true,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"id":2
                    |--->--->},
                    |--->--->"id":20
                    |--->},{
                    |--->--->"name":"am_4",
                    |--->--->"deleted":true,
                    |--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->"email":"email_4",
                    |--->--->"website":"website_4",
                    |--->--->"administrator":{
                    |--->--->--->"name":"a_4",
                    |--->--->--->"deleted":true,
                    |--->--->--->"createdTime":"2022-10-03 00:00:00",
                    |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                    |--->--->--->"id":4
                    |--->--->},
                    |--->--->"id":40
                    |--->}
                    |]""".trimMargin()
            )
        }
    }
}
