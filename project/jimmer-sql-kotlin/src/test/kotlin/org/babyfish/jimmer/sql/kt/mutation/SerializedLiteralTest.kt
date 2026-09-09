package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.tuple
import org.babyfish.jimmer.sql.kt.ast.expression.value
import org.babyfish.jimmer.sql.kt.ast.query.baseTableSymbol
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.pg.JsonWrapper
import org.babyfish.jimmer.sql.kt.model.pg.complexMap
import org.babyfish.jimmer.sql.kt.model.pg.id
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerializedLiteralTest : AbstractMutationTest() {

    @Test
    fun testUpdateWithPropertySerialization() {
        val client = sqlClient { setDialect(H2Dialect()) }
        val data = mapOf("key" to mapOf("nested-key" to "value"))
        jdbc { con ->
            con.createStatement().use {
                it.execute("create local temporary table pg_json_wrapper(id bigint primary key, json_5 json)")
                it.execute("insert into pg_json_wrapper(id) values(1)")
            }
            val count = client.createUpdate(JsonWrapper::class) {
                set(table.complexMap, value(data))
            }.execute(con)
            assertEquals(1, count)
            val rows = client.createQuery(JsonWrapper::class) {
                where(tuple(table.id, table.complexMap) eq Tuple2(1L, data))
                select(table.complexMap)
            }.execute(con)
            assertEquals(listOf(data), rows)
        }
    }

    @Test
    fun testInsertWithPropertySerialization() {
        val client = sqlClient { setDialect(H2Dialect()) }
        val source = baseTableSymbol {
            client.createBaseQuery(BookStore::class) {
                select(table)
            }
        }
        val data = mapOf("key" to mapOf("nested-key" to "value"))
        jdbc { con ->
            con.createStatement().use {
                it.execute("create local temporary table pg_json_wrapper(id bigint primary key, json_5 json)")
            }
            val count = client.createInsert(JsonWrapper::class, source) {
                set(table.id, sourceTable.id)
                set(table.complexMap, value(data))
            }.execute(con)
            assertTrue(count > 0)
            val rows = client.createQuery(JsonWrapper::class) {
                select(table.complexMap)
            }.execute(con)
            assertEquals(List(count) { data }, rows)
        }
    }
}
