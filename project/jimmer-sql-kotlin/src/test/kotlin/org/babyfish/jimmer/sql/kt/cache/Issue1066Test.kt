package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.caffeine.CaffeineValueBinder
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import org.babyfish.jimmer.sql.kt.model.classic.book.fetchBy
import org.babyfish.jimmer.sql.kt.model.classic.book.id
import org.junit.Test
import java.time.Duration

// No problem, not bug
class Issue1066Test : AbstractQueryTest() {

    private val _sqlClient: KSqlClient =
        sqlClient {
            setCacheFactory(
                object: KCacheFactory {
                    override fun createObjectCache(type: ImmutableType): Cache<*, *>? {
                        return ChainCacheBuilder<Any, Any>()
                            .add(
                                CaffeineValueBinder
                                    .forObject<Any, Any>(type)
                                    .maximumSize(1024)
                                    .duration(Duration.ofHours(1))
                                    .build()
                            ).build()
                    }

                    override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>? {
                        return ChainCacheBuilder<Any, List<*>>()
                            .add(
                                CaffeineValueBinder
                                    .forProp<Any, List<*>>(prop)
                                    .maximumSize(1024)
                                    .duration(Duration.ofHours(1))
                                    .build()
                            ).build()
                    }

                    override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *>? {
                        return ChainCacheBuilder<Any, Any>()
                            .add(
                                CaffeineValueBinder
                                    .forProp<Any, Any>(prop)
                                    .maximumSize(1024)
                                    .duration(Duration.ofHours(1))
                                    .build()
                            ).build()
                    }
                }
            )
        }

    @Test
    fun test() {
        for (i in 0..2) {
            executeAndExpect(
                _sqlClient.createQuery(Book::class) {
                    where(table.edition eq 3)
                    orderBy(table.id)
                    select(
                        table.fetchBy {
                            name()
                            store {
                                name()
                            }
                        }
                    )
                }
            ) {
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID 
                        |from BOOK tb_1_ 
                        |where tb_1_.EDITION = ? 
                        |order by tb_1_.ID asc""".trimMargin()
                )
                // 0 load from cache; 1 load from cache
                if (i == 0) {
                    statement(1).sql(
                        """select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION, tb_1_.WEBSITE 
                        |from BOOK_STORE tb_1_ 
                        |where tb_1_.ID in (?, ?)""".trimMargin()
                    )
                }
                rows(
                    """[
                        |--->{
                        |--->--->"id":3,
                        |--->--->"name":"Learning GraphQL",
                        |--->--->"store":{
                        |--->--->--->"id":1,
                        |--->--->--->"name":"O'REILLY"
                        |--->--->}
                        |--->},{
                        |--->--->"id":6,
                        |--->--->"name":"Effective TypeScript",
                        |--->--->"store":{
                        |--->--->--->"id":1,
                        |--->--->--->"name":"O'REILLY"
                        |--->--->}
                        |--->},{
                        |--->--->"id":9,
                        |--->--->"name":"Programming TypeScript",
                        |--->--->"store":{
                        |--->--->--->"id":1,
                        |--->--->--->"name":"O'REILLY"
                        |--->--->}
                        |--->},{
                        |--->--->"id":12,
                        |--->--->"name":"GraphQL in Action",
                        |--->--->"store":{
                        |--->--->--->"id":2,
                        |--->--->--->"name":"MANNING"
                        |--->--->}
                        |--->}
                        |]""".trimMargin()
                )
            }
        }
    }
}