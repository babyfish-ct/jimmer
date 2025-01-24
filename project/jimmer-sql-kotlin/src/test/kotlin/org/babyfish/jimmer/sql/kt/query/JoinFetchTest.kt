package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContent
import org.babyfish.jimmer.sql.kt.model.TreeNode
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.babyfish.jimmer.sql.kt.model.fetchBy
import org.babyfish.jimmer.sql.kt.model.id
import kotlin.test.Test

class JoinFetchTest : AbstractQueryTest() {

    @Test
    fun testExplicitJoinFetch() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.name.eq("GraphQL in Action"))
                orderBy(table.edition)
                select(
                    table.fetchBy {
                        allScalarFields()
                        store(ReferenceFetchType.JOIN_ALWAYS) {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, 
                    |tb_2_.ID, tb_2_.NAME, tb_2_.VERSION, tb_2_.WEBSITE 
                    |from BOOK tb_1_ 
                    |left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |where tb_1_.NAME = ? 
                    |order by tb_1_.EDITION asc""".trimMargin()
            )
            rows(
                """[{
                    |--->"id":10,"name":"GraphQL in Action","edition":1,"price":80.00,
                    |--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |},{
                    |--->"id":11,"name":"GraphQL in Action","edition":2,"price":81.00,
                    |--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |},{
                    |--->"id":12,"name":"GraphQL in Action","edition":3,"price":80.00,
                    |--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |}]""".trimMargin()
            )
        }
    }

    @Test
    fun testImplicitJoinFetch() {
        executeAndExpect(
            sqlClient{
                setDefaultReferenceFetchType(ReferenceFetchType.JOIN_ALWAYS)
            }.createQuery(Book::class) {
                where(table.name.eq("GraphQL in Action"))
                orderBy(table.edition)
                select(
                    table.fetchBy {
                        // Here, `allScalars` is not used
                        // to test `select tb_1_...., tb_2_...., tb_1_.... from ... `
                        name()
                        store {
                            allScalarFields()
                        }
                        edition()
                        price()
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, 
                    |tb_2_.ID, tb_2_.NAME, tb_2_.VERSION, tb_2_.WEBSITE, 
                    |tb_1_.EDITION, tb_1_.PRICE 
                    |from BOOK tb_1_ 
                    |left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |where tb_1_.NAME = ? 
                    |order by tb_1_.EDITION asc""".trimMargin()
            )
            rows(
                """[{
                    |--->"id":10,"name":"GraphQL in Action","edition":1,"price":80.00,
                    |--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |},{
                    |--->"id":11,"name":"GraphQL in Action","edition":2,"price":81.00,
                    |--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |},{
                    |--->"id":12,"name":"GraphQL in Action","edition":3,"price":80.00,
                    |--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |}]""".trimMargin()
            )
        }
    }

    @Test
    fun testMergeJoinAndJoinFetch() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.store.name.eq("MANNING"))
                orderBy(table.edition)
                select(
                    table.fetchBy {
                        // Here, `allScalars` is not used
                        // to test `select tb_1_...., tb_2_...., tb_1_.... from ... `
                        name()
                        store(ReferenceFetchType.JOIN_ALWAYS) {
                            allScalarFields()
                        }
                        edition()
                        price()
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, 
                    |tb_2_.ID, tb_2_.NAME, tb_2_.VERSION, tb_2_.WEBSITE, 
                    |tb_1_.EDITION, tb_1_.PRICE 
                    |from BOOK tb_1_ 
                    |inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |where tb_2_.NAME = ? 
                    |order by tb_1_.EDITION asc""".trimMargin()
            )
            rows(
                """[{
                    |--->"id":10,"name":"GraphQL in Action","edition":1,"price":80.00,
                    |--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |},{
                    |--->"id":11,"name":"GraphQL in Action","edition":2,"price":81.00,
                    |--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |},{
                    |--->"id":12,"name":"GraphQL in Action","edition":3,"price":80.00,
                    |--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                    |}]""".trimMargin()
            )
        }
    }

    @Test
    fun testMaxJoinFetchDepth() {
        executeAndExpect(
            sqlClient {
                setMaxJoinFetchDepth(2)
            }.createQuery(TreeNode::class) {
                where(table.id eq 24L)
                select(
                    table.fetchBy {
                        allScalarFields()
                        parent(ReferenceFetchType.JOIN_ALWAYS) {
                            allScalarFields()
                            parent(ReferenceFetchType.JOIN_ALWAYS) {
                                allScalarFields()
                                parent(ReferenceFetchType.JOIN_ALWAYS) {
                                    allScalarFields()
                                    parent(ReferenceFetchType.JOIN_ALWAYS) {
                                        allScalarFields()
                                    }
                                }
                            }
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.NODE_ID, tb_1_.NAME, 
                    |tb_2_.NODE_ID, tb_2_.NAME, 
                    |tb_3_.NODE_ID, tb_3_.NAME, tb_3_.PARENT_ID 
                    |from TREE_NODE tb_1_ 
                    |left join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID 
                    |left join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID 
                    |where tb_1_.NODE_ID = ?""".trimMargin()
            ).variables(24L)
            statement(1).sql(
                """select tb_1_.NODE_ID, tb_1_.NAME, 
                    |tb_2_.NODE_ID, tb_2_.NAME 
                    |from TREE_NODE tb_1_ 
                    |left join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID 
                    |where tb_1_.NODE_ID = ?""".trimMargin()
            ).variables(9L)
            row(0) {
                assertContent(
                    """{"id":24,"name":"Shirt","parent":{"id":22,"name":"Formal wear","parent":{"id":18,"name":"Man","parent":{"id":9,"name":"Clothing","parent":{"id":1,"name":"Home"}}}}}""",
                    it
                )
            }
        }
    }

    @Test
    fun testPage() {
        // Data query uses fetch
        // Count query ignore fetch
        for (i in 0..1) {
            connectAndExpect({ con ->
                sqlClient.createQuery(Book::class) {
                    where(table.name ilike "graphql")
                    orderBy(table.name, table.edition)
                    select(
                        table.fetchBy {
                            allScalarFields()
                            store(ReferenceFetchType.JOIN_ALWAYS) {
                                allScalarFields()
                            }
                        }
                    )
                }.fetchPage(1, 2, con)
            }) {
                sql(
                    """select count(1) 
                        |from BOOK tb_1_ where 
                        |lower(tb_1_.NAME) like ?""".trimMargin()
                )
                statement(1).sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, 
                        |tb_2_.ID, tb_2_.NAME, tb_2_.VERSION, tb_2_.WEBSITE 
                        |from BOOK tb_1_ 
                        |left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                        |where lower(tb_1_.NAME) like ? 
                        |order by tb_1_.NAME asc, tb_1_.EDITION asc 
                        |limit ? offset ?""".trimMargin()
                )
                row(0) {
                    assertContent(
                        """Page{
                            |--->rows=[{
                            |--->--->--->"id":12,"name":"GraphQL in Action","edition":3,"price":80.00,
                            |--->--->--->"store":{"id":2,"name":"MANNING","version":0,"website":null}
                            |--->--->}, {
                            |--->--->--->"id":1,"name":"Learning GraphQL","edition":1,"price":50.00,
                            |--->--->--->"store":{"id":1,"name":"O'REILLY","version":0,"website":null}
                            |--->--->}
                            |--->], 
                            |--->totalRowCount=6, 
                            |--->totalPageCount=3
                            |}""".trimMargin(),
                        it
                    )
                }
            }
        }
    }
}