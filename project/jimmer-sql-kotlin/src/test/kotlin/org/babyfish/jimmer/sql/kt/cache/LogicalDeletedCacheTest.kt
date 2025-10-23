package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.createCache
import org.babyfish.jimmer.sql.kt.model.inheritance.*
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior
import kotlin.test.BeforeTest
import kotlin.test.Test

class LogicalDeletedCacheTest : AbstractQueryTest() {

    private lateinit var _sqlClient: KSqlClient

    private lateinit var _sqlClientForDeletedData: KSqlClient

    @BeforeTest
    fun initialize() {
        _sqlClient = sqlClient {
            setCaches {
                setCacheFactory(
                    object : KCacheFactory {

                        override fun createObjectCache(type: ImmutableType): Cache<*, *>? =
                            createCache<Any, Any>(type)

                        override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *>? =
                            createCache<Any, Any?>(prop)

                        override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>? =
                            createCache<Any, List<*>>(prop)

                        override fun createResolverCache(prop: ImmutableProp): Cache<*, *>? =
                            createCache<Any, Any?>(prop)
                    }
                )
            }
            setConnectionManager {
                jdbc {
                    proceed(it)
                }
            }
        }
        _sqlClientForDeletedData = _sqlClient.filters {
            setBehavior(LogicalDeletedBehavior.REVERSED)
        }
    }

    @Test
    fun testRoleWithPermission() {
        for (i in 0..1) {
            val useSql = i == 0
            executeAndExpect(
                _sqlClient.createQuery(Role::class) {
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
                        |where tb_1_.DELETED <> ?""".trimMargin()
                ).variables(true)
                if (useSql) {
                    statement(1).sql(
                        """select tb_1_.ID 
                            |from PERMISSION tb_1_ 
                            |where tb_1_.ROLE_ID = ? and tb_1_.DELETED <> ?""".trimMargin()
                    ).variables(100L, true)
                    statement(2).sql(
                        """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.ROLE_ID 
                        |from PERMISSION tb_1_ where tb_1_.ID = ? and tb_1_.DELETED <> ?""".trimMargin()
                    ).variables(1000L, true)
                }
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
            executeAndExpect(
                _sqlClientForDeletedData.createQuery(Role::class) {
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
    }

    @Test
    fun testPermissionWithRole() {
        for (i in 0..1) {
            val useSql = i == 0
            executeAndExpect(
                _sqlClient.createQuery(Permission::class) {
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
                        |where tb_1_.DELETED <> ?""".trimMargin()
                ).variables(true)
                if (useSql) {
                    statement(1).sql(
                        """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME 
                        |from ROLE tb_1_ where tb_1_.ID in (?, ?) and tb_1_.DELETED <> ?""".trimMargin()
                    ).variables(100L, 200L, true)
                }
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
            executeAndExpect(
                _sqlClientForDeletedData.createQuery(Permission::class) {
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
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.DELETED from ROLE tb_1_ where tb_1_.ID in (?, ?) and tb_1_.DELETED = ?""".trimMargin()
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
    }

    @Test
    fun testAdministratorWithRoles() {
        for (i in 0..1) {
            val useSql = i == 0
            executeAndExpect(
                _sqlClient.createQuery(Administrator::class) {
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
                        |where tb_1_.DELETED <> ?""".trimMargin()
                ).variables(true)
                if (useSql) {
                    statement(1).sql(
                        """select tb_1_.ADMINISTRATOR_ID, tb_1_.ROLE_ID 
                        |from ADMINISTRATOR_ROLE_MAPPING tb_1_ 
                        |inner join ROLE tb_3_ on tb_1_.ROLE_ID = tb_3_.ID 
                        |where tb_1_.ADMINISTRATOR_ID in (?, ?) 
                        |and tb_3_.DELETED <> ?""".trimMargin()
                    ).variables(1L, 3L, true)
                    statement(2).sql(
                        """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME 
                        |from ROLE tb_1_ 
                        |where tb_1_.ID = ? and tb_1_.DELETED <> ?""".trimMargin()
                    ).variables(100L, true)
                }
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
            executeAndExpect(
                _sqlClientForDeletedData.createQuery(Administrator::class) {
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
                        |--->--->--->--->"deleted":true,
                        |--->--->--->--->"createdTime":"2022-10-03 00:00:00",
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
    }

    @Test
    fun testRoleWithAdministrators() {
        for (i in 0..1) {
            val useSql = i == 0
            executeAndExpect(
                _sqlClient.createQuery(Role::class) {
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
                        |where tb_1_.DELETED <> ?""".trimMargin()
                ).variables(true)
                if (useSql) {
                    statement(1).sql(
                        """select tb_1_.ADMINISTRATOR_ID 
                        |from ADMINISTRATOR_ROLE_MAPPING tb_1_ 
                        |inner join ADMINISTRATOR tb_3_ on tb_1_.ADMINISTRATOR_ID = tb_3_.ID 
                        |where tb_1_.ROLE_ID = ? 
                        |and tb_3_.DELETED <> ?""".trimMargin()
                    ).variables(100L, true)
                    statement(2).sql(
                        """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME 
                        |from ADMINISTRATOR tb_1_ 
                        |where tb_1_.ID in (?, ?) and tb_1_.DELETED <> ?""".trimMargin()
                    ).variables(1L, 3L, true)
                }
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
            executeAndExpect(
                _sqlClientForDeletedData.createQuery(Role::class) {
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
    }

    @Test
    fun testAdministratorWithAdministratorMetadata() {
        for (i in 0..1) {
            val useSql = i == 0
            executeAndExpect(
                _sqlClient.createQuery(Administrator::class) {
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
                        |where tb_1_.DELETED <> ?""".trimMargin()
                ).variables(true)
                if (useSql) {
                    statement(1).sql(
                        """select tb_1_.ADMINISTRATOR_ID, tb_1_.ID 
                        |from ADMINISTRATOR_METADATA tb_1_ 
                        |where tb_1_.ADMINISTRATOR_ID in (?, ?) 
                        |and tb_1_.DELETED <> ?""".trimMargin()
                    ).variables(1L, 3L, true)
                    statement(2).sql(
                        """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.EMAIL, tb_1_.WEBSITE, tb_1_.ADMINISTRATOR_ID 
                        |from ADMINISTRATOR_METADATA tb_1_ 
                        |where tb_1_.ID in (?, ?) and tb_1_.DELETED <> ?""".trimMargin()
                    ).variables(10L, 30L, true)
                }
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
            executeAndExpect(
                _sqlClientForDeletedData.createQuery(Administrator::class) {
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
                    """select tb_1_.ADMINISTRATOR_ID, 
                        |tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.EMAIL, tb_1_.WEBSITE, tb_1_.DELETED 
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
    }
}