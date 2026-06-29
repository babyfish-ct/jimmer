package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JoinedInheritanceQueryTest : AbstractQueryTest() {

    @Test
    fun testRootFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.id eq 201L)
                select(table.fetchBy {
                    name()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                    "from JOINED_CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(201L)
            row(0) {
                assertEquals(KPerson::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(201L, it.id)
                assertEquals("Alice", it.name)
                assertFalse(ImmutableObjects.isLoaded(it, KClientProps.TYPE))
            }
        }
    }

    @Test
    fun testRootFetcherMaterializesSubtype() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.id eq 200L)
                select(table.fetchBy {
                    type()
                    name()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                    "from JOINED_CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(200L)
            row(0) {
                assertEquals(KOrganization::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(200L, it.id)
                assertEquals("ORG", it.type)
                assertEquals("Globex", it.name)
            }
        }
    }

    @Test
    fun testReferenceFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        executeAndExpect(
            sqlClient.createQuery(KClientProject::class) {
                where(table.id eq 2000L)
                select(table.fetchBy {
                    name()
                    client {
                        name()
                    }
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_ID " +
                    "from JOINED_CLIENT_PROJECT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(2000L)
            statement(1).sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                    "from JOINED_CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            statement(1).variables(200L)
            row(0) {
                assertEquals(2000L, it.id)
                assertEquals("Joined root project", it.name)
                val client = it.client!!
                assertEquals(KOrganization::class.java, (client as ImmutableSpi).__type().javaClass)
                assertEquals(200L, client.id)
                assertEquals("Globex", client.name)
                assertFalse(ImmutableObjects.isLoaded(client, KClientProps.TYPE))
            }
        }
    }

    @Test
    fun testSubtypeQueryWithRootAndSubtypeFields() {
        executeAndExpect(
            sqlClient.createQuery(KOrganization::class) {
                select(table.type, table.name, table.taxCode)
            }
        ) {
            sql(
                "select tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1__sub.TAX_CODE " +
                    "from JOINED_CLIENT tb_1_ " +
                    "inner join JOINED_ORGANIZATION tb_1__sub " +
                    "on tb_1_.ID = tb_1__sub.ID " +
                    "where tb_1_.CLIENT_TYPE = ?"
            )
            variables("ORG")
            row(0) {
                assertEquals("ORG", it._1)
                assertEquals("Globex", it._2)
                assertEquals("GLOBEX-001", it._3)
            }
        }
    }

    @Test
    fun testSubtypeQueryWithRootFieldsOnly() {
        executeAndExpect(
            sqlClient.createQuery(KOrganization::class) {
                select(table.type, table.name)
            }
        ) {
            sql(
                "select tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                    "from JOINED_CLIENT tb_1_ " +
                    "where tb_1_.CLIENT_TYPE = ?"
            )
            variables("ORG")
            row(0) {
                assertEquals("ORG", it._1)
                assertEquals("Globex", it._2)
            }
        }
    }
}
