package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.createCache
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.expect

class LogicalDeletedCacheEvictTest : AbstractQueryTest() {

    private lateinit var _sqlClient: KSqlClient

    private lateinit var messages: MutableList<String>

    @BeforeTest
    fun initialize() {
        _sqlClient = sqlClient {
            setCaches {
                setCacheFactory(
                    object : KCacheFactory {

                        override fun createObjectCache(type: ImmutableType): Cache<*, *>? =
                            createCache<Any, Any>(type)

                        override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *>? =
                            createCache<Any, Any?>(prop, this@LogicalDeletedCacheEvictTest::onPropCacheDelete)

                        override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>? =
                            createCache<Any, List<*>>(prop, this@LogicalDeletedCacheEvictTest::onPropCacheDelete)

                        override fun createResolverCache(prop: ImmutableProp): Cache<*, *>? =
                            createCache<Any, Any?>(prop, this@LogicalDeletedCacheEvictTest::onPropCacheDelete)
                    }
                )
            }
            setConnectionManager {
                jdbc {
                    proceed(it)
                }
            }
        }
        messages = mutableListOf()
    }

    private fun onPropCacheDelete(prop: ImmutableProp, keys: Collection<*>) {
        messages.add(
            "delete " +
                    prop.declaringType.javaClass.simpleName +
                    "." +
                    prop.name +
                    "-" +
                    keys
        )
    }

    @Test
    fun testAdministratorBinLog() {
        connectAndExpect({
            _sqlClient.binLog.accept(
                "administrator",
                jsonCodec().treeReader().read("""{"id": 1, "deleted": false}"""),
                jsonCodec().treeReader().read("""{"id": 1, "deleted": true}""")
            )
        }) {
            sql(
                """select distinct tb_1_.ID 
                    |from ADMINISTRATOR_METADATA tb_1_ 
                    |where tb_1_.ADMINISTRATOR_ID = ? and tb_1_.DELETED <> ?""".trimMargin()
            ).variables(1L, true)
            statement(1).sql(
                """select distinct tb_1_.ID 
                    |from ROLE tb_1_ inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ on tb_1_.ID = tb_2_.ROLE_ID 
                    |where tb_2_.ADMINISTRATOR_ID = ? and tb_1_.DELETED <> ?""".trimMargin()
            ).variables(1L, true)
        }
        expect(
            listOf(
                "delete AdministratorMetadata.administrator-[10]",
                "delete Role.administrators-[100]"
            )
        ) {
            messages
        }
    }

    @Test
    fun testRoleBinLog() {
        connectAndExpect({
            _sqlClient.binLog.accept(
                "role",
                jsonCodec().treeReader().read("""{"id": 100, "deleted": false}"""),
                jsonCodec().treeReader().read("""{"id": 100, "deleted": true}""")
            )
        }) {
            sql(
                """select distinct tb_1_.ID 
                    |from ADMINISTRATOR tb_1_ 
                    |inner join ADMINISTRATOR_ROLE_MAPPING tb_2_ on tb_1_.ID = tb_2_.ADMINISTRATOR_ID 
                    |where tb_2_.ROLE_ID = ? and tb_1_.DELETED <> ?""".trimMargin()
            ).variables(100L, true)
            statement(1).sql(
                """select distinct tb_1_.ID 
                    |from PERMISSION tb_1_ 
                    |where tb_1_.ROLE_ID = ? and tb_1_.DELETED <> ?""".trimMargin()
            ).variables(100L, true)
        }
        expect(
            listOf(
                "delete Administrator.roles-[1]",
                "delete Administrator.roles-[3]",
                "delete Permission.role-[1000]"
            )
        ) {
            messages
        }
    }

    @Test
    fun testPermissionBinLog() {
        connectAndExpect({
            _sqlClient.binLog.accept(
                "permission",
                jsonCodec().treeReader().read("""{"id":1000, "deleted":false, "role_id": 100}"""),
                jsonCodec().treeReader().read("""{"id": 1000, "deleted": true}""")
            )
        }) {
        }
        expect(
            listOf(
                "delete Permission.role-[1000]",
                "delete Role.permissions-[100]",
                "delete Role.permissionCount-[100]"
            )
        ) {
            messages
        }
    }

    /*
     * When Foreign key is changed, use classic trigger,
     * not new trigger for parameterized cache only.
     */
    @Test
    fun testPermissionBinLogWithChangedForeignKey() {
        connectAndExpect({
            _sqlClient.binLog.accept(
                "permission",
                jsonCodec().treeReader().read("""{"id":1000, "deleted":false, "role_id": 100}"""),
                jsonCodec().treeReader().read("""{"id":1000, "deleted":false, "role_id": 200}""")
            )
        }) {
        }
        expect(
            listOf(
                "delete Permission.role-[1000]",
                "delete Role.permissions-[100]",
                "delete Role.permissionCount-[100]",
                "delete Role.permissions-[200]",
                "delete Role.permissionCount-[200]"
            )
        ) {
            messages
        }
    }
}