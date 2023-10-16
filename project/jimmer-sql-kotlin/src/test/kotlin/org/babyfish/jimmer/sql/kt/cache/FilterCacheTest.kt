package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.table.isNull
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.createCache
import org.babyfish.jimmer.sql.kt.common.createParameterizedCache
import org.babyfish.jimmer.sql.kt.filter.common.CacheableFileFilter
import org.babyfish.jimmer.sql.kt.filter.common.FileFilter
import org.babyfish.jimmer.sql.kt.model.filter.File
import org.babyfish.jimmer.sql.kt.model.filter.fetchBy
import org.babyfish.jimmer.sql.kt.model.filter.id
import org.babyfish.jimmer.sql.kt.model.filter.parent
import org.babyfish.jimmer.sql.runtime.ConnectionManager
import java.sql.Connection
import java.util.*
import java.util.function.Function
import kotlin.test.BeforeTest
import kotlin.test.Test

class FilterCacheTest : AbstractQueryTest() {

    private lateinit var _sqlClient: KSqlClient

    private lateinit var valueMap: MutableMap<Any, MutableMap<SortedMap<String, Any>, Any?>>

    @BeforeTest
    fun initialize() {
        valueMap = mutableMapOf()
        _sqlClient = sqlClient {
            addFilters(CacheableFileFilter())
            setConnectionManager(
                object : ConnectionManager {
                    @Suppress("UNCHECKED_CAST")
                    override fun <R> execute(block: Function<Connection, R>): R {
                        val resultBox = arrayOf<Any?>(null) as Array<R?>
                        jdbc {
                            resultBox[0] = block.apply(it)
                        }
                        return resultBox[0]!!
                    }
                }
            )
            setCacheFactory(
                object : KCacheFactory {

                    override fun createObjectCache(type: ImmutableType): Cache<*, *> =
                        createCache<Any, Any>()

                    override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *> =
                        createParameterizedCache(prop, valueMap = valueMap)

                    @Suppress("UNCHECKED_CAST")
                    override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>> =
                        createParameterizedCache(prop, valueMap = valueMap) as Cache<*, List<*>>

                    override fun createResolverCache(prop: ImmutableProp): Cache<*, *> =
                        createParameterizedCache(prop, valueMap = valueMap)
                }
            )
        }
    }

    @Test
    fun testById() {
        FileFilter.withUser(2L) {
            for (i in 0..1) {
                val useSql = i == 0
                connectAndExpect({
                    _sqlClient.entities.forConnection(it).findByIds(File::class, listOf(1L, 2L, 3L, 4L, 11L, 12L, 13L, 14L, 100L))
                }) {
                    if (useSql) {
                        sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.ID in (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_2_ " +
                                "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                "--->)"
                        )
                    }
                    rows(
                        ("[" +
                            "--->{\"id\":1,\"name\":\"usr\",\"parent\":null}," +
                            "--->{\"id\":2,\"name\":\"bin\",\"parent\":{\"id\":1}}," +
                            "--->{\"id\":3,\"name\":\"cd\",\"parent\":{\"id\":2}}," +
                            "--->{\"id\":4,\"name\":\"vim\",\"parent\":{\"id\":2}}," +
                            "--->{\"id\":11,\"name\":\"purge\",\"parent\":{\"id\":8}}," +
                            "--->{\"id\":12,\"name\":\"ssh\",\"parent\":{\"id\":8}}" +
                            "]")
                    )
                }
            }
        }
    }

    @Test
    fun testRecursive() {
        FileFilter.withUser(2L) {
            for (i in 0..1) {
                val useSql = i == 0
                executeAndExpect(
                    _sqlClient.createQuery(File::class) {
                        where(table.parent.isNull())
                        orderBy(table.id)
                        select(
                            table.fetchBy {
                                allScalarFields()
                                childFiles({
                                    recursive()
                                }) {
                                    allScalarFields()
                                }
                            }
                        )
                    }
                ) {
                    sql(
                        "select tb_1_.ID, tb_1_.NAME " +
                            "from FILE tb_1_ " +
                            "where " +
                            "--->tb_1_.PARENT_ID is null " +
                            "and " +
                            "--->exists(" +
                            "--->--->select 1 " +
                            "--->--->from FILE_USER_MAPPING tb_3_ " +
                            "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                            "--->) " +
                            "order by tb_1_.ID asc"
                    )
                    if (useSql) {
                        statement(1).sql(
                            ("select tb_1_.PARENT_ID, tb_1_.ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.PARENT_ID in (?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                "--->) " +
                                "order by tb_1_.ID asc")
                        )
                        statement(2).sql(
                            ("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.ID in (?, ?, ?, ?, ?, ?, ?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_2_ " +
                                "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                "--->)")
                        )
                        statement(3).sql(
                            ("select tb_1_.PARENT_ID, tb_1_.ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                "--->) " +
                                "order by tb_1_.ID asc")
                        )
                        statement(4).sql(
                            ("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_2_ " +
                                "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                "--->)")
                        )
                        statement(5).sql(
                            ("select tb_1_.PARENT_ID, tb_1_.ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                "--->) " +
                                "order by tb_1_.ID asc")
                        )
                        statement(6).sql(
                            ("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.ID in (?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_2_ " +
                                "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                "--->)")
                        )
                        statement(7).sql(
                            ("select tb_1_.PARENT_ID, tb_1_.ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.PARENT_ID in (?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                "--->) " +
                                "order by tb_1_.ID asc")
                        )
                        statement(8).sql(
                            ("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.ID in (?, ?, ?, ?, ?, ?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_2_ " +
                                "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                "--->)")
                        )
                        statement(9).sql(
                            ("select tb_1_.PARENT_ID, tb_1_.ID " +
                                "from FILE tb_1_ " +
                                "where " +
                                "--->tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?) " +
                                "and " +
                                "--->exists(" +
                                "--->--->select 1 " +
                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                "--->) " +
                                "order by tb_1_.ID asc")
                        )
                    }
                    rows(
                        ("[" +
                            "--->{" +
                            "--->--->\"id\":1," +
                            "--->--->\"name\":\"usr\"," +
                            "--->--->\"childFiles\":[" +
                            "--->--->--->{" +
                            "--->--->--->--->\"id\":2," +
                            "--->--->--->--->\"name\":\"bin\"," +
                            "--->--->--->--->\"childFiles\":[" +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":3," +
                            "--->--->--->--->--->--->\"name\":\"cd\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}," +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":4," +
                            "--->--->--->--->--->--->\"name\":\"vim\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}," +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":6," +
                            "--->--->--->--->--->--->\"name\":\"wait\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}," +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":7," +
                            "--->--->--->--->--->--->\"name\":\"which\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}" +
                            "--->--->--->--->]" +
                            "--->--->--->}," +
                            "--->--->--->{" +
                            "--->--->--->--->\"id\":8," +
                            "--->--->--->--->\"name\":\"sbin\"," +
                            "--->--->--->--->\"childFiles\":[" +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":9," +
                            "--->--->--->--->--->--->\"name\":\"ipconfig\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}," +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":11," +
                            "--->--->--->--->--->--->\"name\":\"purge\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}," +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":12," +
                            "--->--->--->--->--->--->\"name\":\"ssh\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}" +
                            "--->--->--->--->]" +
                            "--->--->--->}," +
                            "--->--->--->{" +
                            "--->--->--->--->\"id\":20," +
                            "--->--->--->--->\"name\":\"share\"," +
                            "--->--->--->--->\"childFiles\":[" +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":22," +
                            "--->--->--->--->--->--->\"name\":\"dict\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}," +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":23," +
                            "--->--->--->--->--->--->\"name\":\"sandbox\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}," +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":25," +
                            "--->--->--->--->--->--->\"name\":\"locale\"," +
                            "--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->}" +
                            "--->--->--->--->]" +
                            "--->--->--->}," +
                            "--->--->--->{" +
                            "--->--->--->--->\"id\":26," +
                            "--->--->--->--->\"name\":\"local\"," +
                            "--->--->--->--->\"childFiles\":[" +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":27," +
                            "--->--->--->--->--->--->\"name\":\"include\"," +
                            "--->--->--->--->--->--->\"childFiles\":[" +
                            "--->--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->--->\"id\":28," +
                            "--->--->--->--->--->--->--->--->\"name\":\"node\"," +
                            "--->--->--->--->--->--->--->--->\"childFiles\":[" +
                            "--->--->--->--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->--->--->--->\"id\":29," +
                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"v8-external.h\"," +
                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->--->--->--->--->}," +
                            "--->--->--->--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->--->--->--->\"id\":30," +
                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"v8-internal.h\"," +
                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->--->--->--->--->}," +
                            "--->--->--->--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->--->--->--->\"id\":32," +
                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"v8-object.h\"," +
                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->--->--->--->--->}," +
                            "--->--->--->--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->--->--->--->\"id\":33," +
                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"v8-platform.h\"," +
                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->--->--->--->--->}" +
                            "--->--->--->--->--->--->--->--->]" +
                            "--->--->--->--->--->--->--->}" +
                            "--->--->--->--->--->--->]" +
                            "--->--->--->--->--->}," +
                            "--->--->--->--->--->{" +
                            "--->--->--->--->--->--->\"id\":34," +
                            "--->--->--->--->--->--->\"name\":\"lib\"," +
                            "--->--->--->--->--->--->\"childFiles\":[" +
                            "--->--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->--->\"id\":35," +
                            "--->--->--->--->--->--->--->--->\"name\":\"node_modules\"," +
                            "--->--->--->--->--->--->--->--->\"childFiles\":[" +
                            "--->--->--->--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->--->--->--->\"id\":36," +
                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"npm\"," +
                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->--->--->--->--->}," +
                            "--->--->--->--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->--->--->--->\"id\":37," +
                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"corepack\"," +
                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->--->--->--->--->}," +
                            "--->--->--->--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->--->--->--->\"id\":39," +
                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"docsify-cli\"," +
                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->--->--->--->--->--->--->}" +
                            "--->--->--->--->--->--->--->--->]" +
                            "--->--->--->--->--->--->--->}" +
                            "--->--->--->--->--->--->]" +
                            "--->--->--->--->--->}" +
                            "--->--->--->--->]" +
                            "--->--->--->}" +
                            "--->--->]" +
                            "--->}," +
                            "--->{" +
                            "--->--->\"id\":40," +
                            "--->--->\"name\":\"etc\"," +
                            "--->--->\"childFiles\":[" +
                            "--->--->--->{" +
                            "--->--->--->--->\"id\":41," +
                            "--->--->--->--->\"name\":\"passwd\"," +
                            "--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->}," +
                            "--->--->--->{" +
                            "--->--->--->--->\"id\":43," +
                            "--->--->--->--->\"name\":\"ssh\"," +
                            "--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->}," +
                            "--->--->--->{" +
                            "--->--->--->--->\"id\":44," +
                            "--->--->--->--->\"name\":\"profile\"," +
                            "--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->}," +
                            "--->--->--->{" +
                            "--->--->--->--->\"id\":45," +
                            "--->--->--->--->\"name\":\"services\"," +
                            "--->--->--->--->\"childFiles\":[]" +
                            "--->--->--->}" +
                            "--->--->]" +
                            "--->}" +
                            "]")
                    )
                }
            }
        }
    }
}