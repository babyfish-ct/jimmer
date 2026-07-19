package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.createCache
import org.babyfish.jimmer.sql.kt.common.createParameterizedCache
import org.babyfish.jimmer.sql.kt.filter.common.CacheableFileFilter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterCacheEvictTest : AbstractQueryTest() {

    private lateinit var _sqlClient: KSqlClient

    private val deleteMessages = mutableListOf<String>()

    @BeforeTest
    fun initialize() {
        deleteMessages.clear()
        _sqlClient = sqlClient {
            addFilters(CacheableFileFilter())
            setConnectionManager(TestConnectionManager())
            setCacheFactory(
                object : KCacheFactory {

                    override fun createObjectCache(type: ImmutableType): Cache<*, *> =
                        createCache<Any, Any>(type)

                    override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *> =
                        createParameterizedCache<Any, Any?>(prop, this@FilterCacheEvictTest::onPropCacheDelete)

                    @Suppress("UNCHECKED_CAST")
                    override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>> =
                        createParameterizedCache<Any, List<Any>>(
                            prop,
                            this@FilterCacheEvictTest::onPropCacheDelete
                        ) as Cache<*, List<*>>

                    override fun createResolverCache(prop: ImmutableProp): Cache<*, *> =
                        createParameterizedCache<Any, Any?>(prop, this@FilterCacheEvictTest::onPropCacheDelete)
                }
            )
        }
    }

    private fun onPropCacheDelete(prop: ImmutableProp, keys: Collection<*>) {
        for (key in keys) {
            deleteMessages.add(
                "${prop.declaringType.javaClass.simpleName}.${prop.name}-$key"
            )
        }
    }

    @Test
    fun testChangeForeignKey() {
        connectAndExpect({
            _sqlClient.binLog.accept(
                "file",
                jsonCodec().treeReader().read("{\"id\":9, \"parent_id\":8}"),
                jsonCodec().treeReader().read("{\"id\":9, \"parent_id\":2}")
            )
        }) {

        }
        assertEquals(
            "[File.parent-9, File.childFiles-8, File.childFiles-2]",
            deleteMessages.toString()
        )
    }

    @Test
    fun testChangeUserAssociations() {
        connectAndExpect({
            _sqlClient.binLog.accept(
                "file_user_mapping",
                jsonCodec().treeReader().read("{\"file_id\":28, \"user_id\":2}"),
                null
            )
        }) {
            sql(
                "select tb_1_.PARENT_ID " +
                        "from FILE tb_1_ " +
                        "where tb_1_.ID = ? " +
                        "and tb_1_.PARENT_ID is not null"
            ).variables(28L)
            statement(1).sql(
                "select distinct tb_1_.ID " +
                        "from FILE tb_1_ " +
                        "where tb_1_.PARENT_ID = ?"
            ).variables(28L)
        }
        assertEquals(
            "[" +
                    "File.childFiles-27, " +
                    "File.parent-29, " +
                    "File.parent-30, " +
                    "File.parent-31, " +
                    "File.parent-32, " +
                    "File.parent-33, " +
                    "File.users-28, " +
                    "User.files-2" +
                    "]",
            deleteMessages.toString()
        )
    }
}