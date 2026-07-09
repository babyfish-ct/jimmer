package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.kt.cache.KCacheFactory
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.createCache
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.fetchBy
import org.junit.Test

class TypedTransientResolverTest : AbstractQueryTest() {

    @Test
    fun testTypedNameWithVersion() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        nameWithVersion()
                    }
                )
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE " +
                    "from BOOK_STORE tb_1_"
            )
            rows(
                "[" +
                    "--->{\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null,\"nameWithVersion\":\"O'REILLY#0\"}," +
                    "--->{\"id\":2,\"name\":\"MANNING\",\"version\":0,\"website\":null,\"nameWithVersion\":\"MANNING#0\"}" +
                    "]"
            )
        }
    }

    @Test
    fun testKTransientResolverContext() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        resolverPropName()
                    }
                )
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE " +
                    "from BOOK_STORE tb_1_"
            )
            rows(
                "[" +
                    "--->{\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null,\"resolverPropName\":\"resolverPropName\"}," +
                    "--->{\"id\":2,\"name\":\"MANNING\",\"version\":0,\"website\":null,\"resolverPropName\":\"resolverPropName\"}" +
                    "]"
            )
        }
    }

    @Test
    fun testTypedNameWithVersionCacheFallback() {
        val cachedSqlClient = sqlClient {
            setCacheFactory(
                object : KCacheFactory {
                    override fun createResolverCache(prop: ImmutableProp): Cache<*, *> =
                        createCache<Any, Any>(prop)
                }
            )
        }
        for (i in 0..1) {
            val useSql = i == 0
            executeAndExpect(
                cachedSqlClient.createQuery(BookStore::class) {
                    select(
                        table.fetchBy {
                            allScalarFields()
                            nameWithVersion()
                        }
                    )
                }
            ) {
                sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE " +
                        "from BOOK_STORE tb_1_"
                )
                if (useSql) {
                    statement(1).sql(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION " +
                            "from BOOK_STORE tb_1_ " +
                            "where tb_1_.ID in (?, ?)"
                    )
                }
                rows(
                    "[" +
                        "--->{\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null,\"nameWithVersion\":\"O'REILLY#0\"}," +
                        "--->{\"id\":2,\"name\":\"MANNING\",\"version\":0,\"website\":null,\"nameWithVersion\":\"MANNING#0\"}" +
                        "]"
                )
            }
        }
    }
}
