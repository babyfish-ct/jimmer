package org.babyfish.jimmer.sql.kt.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.babyfish.jimmer.jackson.ImmutableModule
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.event.EntityEvent
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.createCache
import org.babyfish.jimmer.sql.kt.common.createParameterizedCache
import org.babyfish.jimmer.sql.kt.event.getUnchangedFieldRef
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs
import org.babyfish.jimmer.sql.kt.model.inheritance.*
import org.babyfish.jimmer.sql.runtime.EntityManager
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.expect

class ParameterizedCacheEvictTest : AbstractQueryTest() {

    private lateinit var _sqlClient: KSqlClient

    private lateinit var messages: MutableList<String>

    @BeforeTest
    fun initialize() {
        _sqlClient = sqlClient {
            addFilters(UndeleteFilter())
            addDisabledFilters(DeleteFilter())
            setEntityManager(
                EntityManager(
                    Administrator::class.java,
                    AdministratorMetadata::class.java,
                    Role::class.java,
                    Permission::class.java
                )
            )
            setCaches {
                setCacheFactory(
                    object : KCacheFactory {

                        override fun createObjectCache(type: ImmutableType): Cache<*, *>? =
                            createCache<Any, Any>()

                        override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *>? =
                            createParameterizedCache<Any, Any?>(prop, this@ParameterizedCacheEvictTest::onPropCacheDelete)

                        override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>? =
                            createParameterizedCache<Any, List<*>>(prop, this@ParameterizedCacheEvictTest::onPropCacheDelete)

                        override fun createResolverCache(prop: ImmutableProp): Cache<*, *>? =
                            createParameterizedCache<Any, Any?>(prop, this@ParameterizedCacheEvictTest::onPropCacheDelete)
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
                MAPPER.readTree("""{"id": 1, "deleted": false}"""),
                MAPPER.readTree("""{"id": 1, "deleted": true}""")
            )
        }) {
            sql(
                """select distinct tb_1_.ID 
                    |from ADMINISTRATOR_METADATA as tb_1_ 
                    |where tb_1_.ADMINISTRATOR_ID = ?""".trimMargin()
            ).variables(1L)
            statement(1).sql(
                """select distinct tb_1_.ID from ROLE as tb_1_ 
                    |inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ROLE_ID 
                    |where tb_2_.ADMINISTRATOR_ID = ?""".trimMargin()
            ).variables(1L)
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
                MAPPER.readTree("""{"id": 100, "deleted": false}"""),
                MAPPER.readTree("""{"id": 100, "deleted": true}""")
            )
        }) {
            sql(
                """select distinct tb_1_.ID 
                    |from ADMINISTRATOR as tb_1_ 
                    |inner join ADMINISTRATOR_ROLE_MAPPING as tb_2_ on tb_1_.ID = tb_2_.ADMINISTRATOR_ID 
                    |where tb_2_.ROLE_ID = ?""".trimMargin()
            ).variables(100L)
            statement(1).sql(
                """select distinct tb_1_.ID 
                    |from PERMISSION as tb_1_ 
                    |where tb_1_.ROLE_ID = ?""".trimMargin()
            ).variables(100L)
        }
        expect(
            listOf(
                "delete Administrator.roles-[1]",
                "delete Administrator.roles-[2]",
                "delete Administrator.roles-[3]",
                "delete Permission.role-[1000]",
                "delete Permission.role-[2000]"
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
                MAPPER.readTree("""{"id":1000, "deleted":false, "role_id": 100}"""),
                MAPPER.readTree("""{"id": 1000, "deleted": true}""")
            )
        }) {
        }
        expect(
            listOf(
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
                MAPPER.readTree("""{"id":1000, "deleted":false, "role_id": 100}"""),
                MAPPER.readTree("""{"id":1000, "deleted":false, "role_id": 200}""")
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

    private class UndeleteFilter : KCacheableFilter<NamedEntity> {

        override fun filter(args: KFilterArgs<NamedEntity>) {
            args.where(args.table.deleted eq false)
        }

        override fun getParameters(): SortedMap<String, Any> =
            sortedMapOf("deleted" to false)

        override fun isAffectedBy(e: EntityEvent<*>): Boolean =
            e.getUnchangedFieldRef(NamedEntity::deleted) == null
    }

    private class DeleteFilter : KCacheableFilter<NamedEntity> {

        override fun filter(args: KFilterArgs<NamedEntity>) {
            args.where(args.table.deleted eq true)
        }

        override fun getParameters(): SortedMap<String, Any> =
            sortedMapOf("deleted" to true)

        override fun isAffectedBy(e: EntityEvent<*>): Boolean =
            e.getUnchangedFieldRef(NamedEntity::deleted) == null
    }

    companion object {
        val MAPPER = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(ImmutableModule())
    }
}