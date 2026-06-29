package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.*
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

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
    fun testEnumDiscriminatorRootFetcherMaterializesInstantiableRootWithoutUserDiscriminator() {
        executeAndExpect(
            sqlClient.createQuery(KEnumClient::class) {
                where(table.id eq 111L)
                select(table.fetchBy {
                    name()
                })
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_TYPE " +
                    "from K_ENUM_CLIENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            variables(111L)
            row(0) {
                assertEquals(KEnumClient::class.java, (it as ImmutableSpi).__type().javaClass)
                assertEquals(111L, it.id)
                assertEquals("Enum Root", it.name)
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

    @Test
    fun testInstanceOfRootRendersDiscriminatorPredicate() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.asTableEx().instanceOf<KClient>())
                orderBy(table.id)
                select(table.id)
            }
        ) {
            sql(
                "select tb_1_.ID " +
                    "from CLIENT tb_1_ " +
                    "where tb_1_.CLIENT_TYPE in (?, ?) " +
                    "order by tb_1_.ID asc"
            )
            variables("ORG", "KPerson")
            rows("[100,101,102]")
        }
    }

    @Test
    fun testTreatAsRootRendersDiscriminatorPredicate() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                val client = table.asTableEx().treatAs<KClient>()
                orderBy(table.id)
                select(table.id, client.name)
            }
        ) {
            sql(
                "select tb_1_.ID, tb_2_.NAME " +
                    "from CLIENT tb_1_ " +
                    "inner join CLIENT tb_2_ " +
                    "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE in (?, ?) " +
                    "order by tb_1_.ID asc"
            )
            variables("ORG", "KPerson")
            row(0) {
                assertEquals(100L, it._1)
                assertEquals("Acme", it._2)
            }
            row(1) {
                assertEquals(101L, it._1)
                assertEquals("Bob", it._2)
            }
            row(2) {
                assertEquals(102L, it._1)
                assertEquals("Umbrella", it._2)
            }
        }
    }

    @Test
    fun testTryTreatAsOnNullableAssociationPath() {
        executeAndExpect(
            sqlClient.createQuery(KClientProject::class) {
                val organization = table.asTableEx().`client?`.tryTreatAs<KOrganization>()
                where(table.id valueIn listOf(1000L, 1002L))
                orderBy(table.id)
                select(table.id, organization.taxCode)
            }
        ) {
            sql(
                "select tb_1_.ID, tb_3_.TAX_CODE " +
                    "from SINGLE_CLIENT_PROJECT tb_1_ " +
                    "left join CLIENT tb_2_ on tb_1_.CLIENT_ID = tb_2_.ID " +
                    "left join CLIENT tb_3_ " +
                    "on tb_2_.ID = tb_3_.ID and tb_3_.CLIENT_TYPE = ? " +
                    "where tb_1_.ID in (?, ?) " +
                    "order by tb_1_.ID asc"
            )
            variables("ORG", 1000L, 1002L)
            row(0) {
                assertEquals(1000L, it._1)
                assertEquals("ACME-001", it._2)
            }
            row(1) {
                assertEquals(1002L, it._1)
                assertNull(it._2)
            }
        }
    }
}
