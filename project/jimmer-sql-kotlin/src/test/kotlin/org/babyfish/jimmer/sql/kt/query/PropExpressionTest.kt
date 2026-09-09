package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.asNonNull
import org.babyfish.jimmer.sql.kt.ast.expression.asNullable
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.babyfish.jimmer.sql.kt.model.classic.store.website
import org.babyfish.jimmer.sql.kt.model.embedded.*
import org.babyfish.jimmer.sql.kt.model.embedded.p4bug524.x
import kotlin.test.Test
import kotlin.test.assertEquals

class PropExpressionTest : AbstractQueryTest() {

    @Test
    fun testScalarPropertyToString() {
        sqlClient.createQuery(BookStore::class) {
            assertEquals("BookStore.name", table.name.toString())
            assertEquals("BookStore.website", table.website.toString())
            assertEquals("BookStore.name", table.name.asNullable().toString())
            assertEquals("BookStore.website", table.website.asNonNull().toString())
            select(table)
        }
    }

    @Test
    fun testEmbeddedPropertyToString() {
        sqlClient.createQuery(Transform::class) {
            assertEquals("Transform.source", table.source.toString())
            assertEquals("Transform.source.leftTop", table.source.leftTop.toString())
            assertEquals("Transform.source.leftTop.x", table.source.leftTop.x.toString())
            assertEquals("Transform.source.rightBottom", table.source.rightBottom.toString())
            assertEquals("Transform.source.rightBottom.x", table.source.rightBottom.x.toString())
            select(table)
        }
    }
}
