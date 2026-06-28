package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.*
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SingleTableInheritanceQueryTest : AbstractQueryTest() {

    @Test
    fun testRootFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.id eq 101L)
                select(table.fetchBy {
                    name()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_TYPE " +
                "from CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(101L)
            row(0) {
                assertEquals(KPerson::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(101L, it.id)
                assertEquals("Bob", it.name)
                assertFalse(ImmutableObjects.isLoaded(it, KClientProps.TYPE))
            }
        }
    }

    @Test
    fun testRootFetcherMaterializesSubtype() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.id eq 100L)
                select(table.fetchBy {
                    type()
                    name()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                    "from CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(100L)
            row(0) {
                assertEquals(KOrganization::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(100L, it.id)
                assertEquals("ORG", it.type)
                assertEquals("Acme", it.name)
            }
        }
    }

    @Test
    fun testReferenceFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        executeAndExpect(
            sqlClient.createQuery(KClientProject::class) {
                where(table.id eq 1000L)
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
                    "from SINGLE_CLIENT_PROJECT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(1000L)
            statement(1).sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_TYPE " +
                    "from CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            statement(1).variables(100L)
            row(0) {
                assertEquals(1000L, it.id)
                assertEquals("Single root project", it.name)
                val client = it.client!!
                assertEquals(KOrganization::class.java, (client as ImmutableSpi).__type().javaClass)
                assertEquals(100L, client.id)
                assertEquals("Acme", client.name)
                assertFalse(ImmutableObjects.isLoaded(client, KClientProps.TYPE))
            }
        }
    }

    @Test
    fun testEnumDiscriminatorRootFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        executeAndExpect(
            sqlClient.createQuery(KEnumClient::class) {
                where(table.id eq 110L)
                select(table.fetchBy {
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                    "from K_ENUM_CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(110L)
            row(0) {
                assertEquals(KEnumOrganization::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(110L, it.id)
                assertFalse(ImmutableObjects.isLoaded(it, KEnumClientProps.TYPE))
            }
        }
    }

    @Test
    fun testEnumDiscriminatorRootFetcherMaterializesSubtype() {
        executeAndExpect(
            sqlClient.createQuery(KEnumClient::class) {
                where(table.id eq 110L)
                select(table.fetchBy {
                    type()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                    "from K_ENUM_CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(110L)
            row(0) {
                assertEquals(KEnumOrganization::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(110L, it.id)
                assertEquals(KClientType.ORG, it.type)
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
                "select tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1_.TAX_CODE " +
                    "from CLIENT tb_1_ " +
                    "where tb_1_.CLIENT_TYPE = ?"
            )
            variables("ORG")
            row(0) {
                assertEquals("ORG", it._1)
                assertEquals("Acme", it._2)
                assertEquals("ACME-001", it._3)
            }
        }
    }
}
