package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class PropExpressionTest : AbstractQueryTest() {

    @Test
    fun testScalarPropertyEquality() {
        sqlClient.createQuery(BookStore::class) {
            assertEquivalent(table.name, table.name, table.name.asNullable(), table.name.asNullable().asNonNull())
            assertEquivalent(table.website, table.website, table.website.asNonNull(), table.website.asNonNull().asNullable())
            assertNotEquals<Any>(table.name, table.website)
            select(table)
        }
    }

    @Test
    fun testEmbeddedPropertyEquality() {
        sqlClient.createQuery(Transform::class) {
            assertEquivalent(table.source, table.source, table.source.asNullable())
            assertEquivalent(table.source.leftTop, table.source.leftTop, table.source.leftTop.asNullable())
            assertEquivalent(table.source.rightBottom, table.source.rightBottom, table.source.rightBottom.asNonNull())
            assertEquivalent(table.source.leftTop.x, table.source.leftTop.x, table.source.leftTop.x.asNullable())
            assertEquivalent(table.source.rightBottom.x, table.source.rightBottom.x, table.source.rightBottom.x.asNonNull())
            assertNotEquals<Any>(table.source.leftTop, table.source.rightBottom)
            assertNotEquals<Any>(table.source.leftTop.x, table.source.rightBottom.x)
            select(table)
        }
    }

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

    private fun assertEquivalent(vararg expressions: KPropExpression<*>) {
        val variants: List<Any> = expressions.toList() + expressions.map { (it as PropExpressionImplementor<*>).unwrap() }
        for (stored in variants) {
            assertFalse(stored.equals(null))
            assertNotEquals(stored, Any())
            val map = hashMapOf(stored to "value")
            val set = hashSetOf(stored)
            for (expression in variants) {
                assertEquals(stored, expression)
                assertEquals(stored.hashCode(), expression.hashCode())
                assertEquals("value", map[expression])
                assertFalse(set.add(expression))
            }
        }
    }
}
