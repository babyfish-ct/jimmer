package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.dto.KInstantiableClientDefaultView
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.dto.KInstantiableClientExhaustiveView
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.dto.KInstantiableClientSimpleView
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KClientProject
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientDefaultBranchFieldView
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientImplicitCatchAllView
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientProjectWithClientView
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientRuntimeView
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.id
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.KClient as KInstantiableClient
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.id as instantiableId

class PolymorphicDtoViewTest : AbstractQueryTest() {

    @Test
    fun testSealedPolymorphicDtoRootSupportsExhaustiveWhen() {
        fun label(view: KClientRuntimeView): String =
            when (view) {
                is KClientRuntimeView.Organization -> "organization"
                is KClientRuntimeView.Person -> "person"
            }

        assertEquals("organization", label(KClientRuntimeView.Organization(id = 1L, name = "A", taxCode = "T")))
        assertEquals("person", label(KClientRuntimeView.Person(id = 2L, name = "B", firstName = "B")))
    }

    @Test
    fun testInstantiableRootWithoutTypesIsOrdinaryDtoClass() {
        executeAndExpect(
            sqlClient.createQuery(KInstantiableClient::class) {
                where(table.instantiableId eq 600L)
                select(table.fetch(KInstantiableClientSimpleView::class))
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                        "from JOINED_INST_CLIENT tb_1_ " +
                        "where tb_1_.ID = ?"
            )
            variables(600L)
            row(0) {
                val view = it as KInstantiableClientSimpleView
                assertEquals(600L, view.id)
                assertEquals("Joined Root", view.name)
            }
        }
    }

    @Test
    fun testDefaultBranchFields() {
        val defaultBranch = KClientDefaultBranchFieldView.Default(
            id = 20L,
            name = "Default branch fields"
        )
        val view: KClientDefaultBranchFieldView = defaultBranch
        assertEquals(20L, view.id)
        assertEquals("Default branch fields", defaultBranch.name)

        val organization = KClientDefaultBranchFieldView.Organization(
            id = 21L,
            taxCode = "T-21"
        )
        assertEquals(21L, organization.id)
        assertEquals("T-21", organization.taxCode)
    }

    @Test
    fun testImplicitDefaultBranchRouting() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.id valueIn listOf(100L, 101L))
                orderBy(table.id)
                select(table.fetch(KClientImplicitCatchAllView::class))
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_2_.TAX_CODE " +
                        "from CLIENT tb_1_ " +
                        "left join CLIENT tb_2_ " +
                        "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                        "where tb_1_.ID in (?, ?) " +
                        "order by tb_1_.ID asc"
            )
            variables("ORG", 100L, 101L)
            row(0) {
                assertTrue(it is KClientImplicitCatchAllView.Organization)
                val organization = it as KClientImplicitCatchAllView.Organization
                assertEquals(100L, organization.id)
                assertEquals("Acme", organization.name)
                assertEquals("ACME-001", organization.taxCode)
            }
            row(1) {
                assertTrue(it is KClientImplicitCatchAllView.Default)
                val default = it as KClientImplicitCatchAllView.Default
                assertEquals(101L, default.id)
                assertEquals("Bob", default.name)
            }
        }
    }

    @Test
    fun testExhaustiveBranchRouting() {
        executeAndExpect(
            sqlClient.createQuery(KClient::class) {
                where(table.id valueIn listOf(100L, 101L))
                orderBy(table.id)
                select(table.fetch(KClientRuntimeView::class))
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_2_.TAX_CODE, tb_3_.FIRST_NAME " +
                        "from CLIENT tb_1_ " +
                        "left join CLIENT tb_2_ " +
                        "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                        "left join CLIENT tb_3_ " +
                        "on tb_1_.ID = tb_3_.ID and tb_3_.CLIENT_TYPE = ? " +
                        "where tb_1_.ID in (?, ?) " +
                        "order by tb_1_.ID asc"
            )
            variables("ORG", "KPerson", 100L, 101L)
            row(0) {
                assertTrue(it is KClientRuntimeView.Organization)
                val organization = it as KClientRuntimeView.Organization
                assertEquals(100L, organization.id)
                assertEquals("Acme", organization.name)
                assertEquals("ACME-001", organization.taxCode)
            }
            row(1) {
                assertTrue(it is KClientRuntimeView.Person)
                val person = it as KClientRuntimeView.Person
                assertEquals(101L, person.id)
                assertEquals("Bob", person.name)
                assertEquals("Bob", person.firstName)
            }
        }
    }

    @Test
    fun testNestedPolymorphicAssociationRouting() {
        executeAndExpect(
            sqlClient.createQuery(KClientProject::class) {
                where(table.id eq 1000L)
                select(table.fetch(KClientProjectWithClientView::class))
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_ID " +
                        "from SINGLE_CLIENT_PROJECT tb_1_ " +
                        "where tb_1_.ID = ?"
            )
            variables(1000L)
            statement(1).sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_2_.TAX_CODE " +
                        "from CLIENT tb_1_ " +
                        "left join CLIENT tb_2_ " +
                        "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                        "where tb_1_.ID = ?"
            )
            statement(1).variables("ORG", 100L)
            row(0) {
                val project = it as KClientProjectWithClientView
                assertEquals(1000L, project.id)
                assertEquals("Single root project", project.name)
                val client = project.client
                assertTrue(client is KClientProjectWithClientView.TargetOf_client.Organization)
                val organization = client as KClientProjectWithClientView.TargetOf_client.Organization
                assertEquals(100L, organization.id)
                assertEquals("Acme", organization.name)
                assertEquals("ACME-001", organization.taxCode)
            }
        }
    }

    @Test
    fun testInstantiableRootDefaultBranchRouting() {
        executeAndExpect(
            sqlClient.createQuery(KInstantiableClient::class) {
                where(table.instantiableId valueIn listOf(600L, 601L))
                orderBy(table.instantiableId)
                select(table.fetch(KInstantiableClientDefaultView::class))
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_2_.TAX_CODE " +
                        "from JOINED_INST_CLIENT tb_1_ " +
                        "left join JOINED_INST_ORGANIZATION tb_2_ " +
                        "on tb_1_.ID = tb_2_.ID and tb_1_.CLIENT_TYPE = ? " +
                        "where tb_1_.ID in (?, ?) " +
                        "order by tb_1_.ID asc"
            )
            variables("ORG", 600L, 601L)
            row(0) {
                assertTrue(it is KInstantiableClientDefaultView.Base)
                val base = it as KInstantiableClientDefaultView.Base
                assertEquals(600L, base.id)
                assertEquals("Joined Root", base.name)
            }
            row(1) {
                assertTrue(it is KInstantiableClientDefaultView.Organization)
                val organization = it as KInstantiableClientDefaultView.Organization
                assertEquals(601L, organization.id)
                assertEquals("Joined Inst Org", organization.name)
                assertEquals("J-ORG-001", organization.taxCode)
            }
        }
    }

    @Test
    fun testInstantiableRootExhaustiveBranchRouting() {
        executeAndExpect(
            sqlClient.createQuery(KInstantiableClient::class) {
                where(table.instantiableId valueIn listOf(600L, 602L))
                orderBy(table.instantiableId)
                select(table.fetch(KInstantiableClientExhaustiveView::class))
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_2_.FIRST_NAME " +
                        "from JOINED_INST_CLIENT tb_1_ " +
                        "left join JOINED_INST_PERSON tb_2_ " +
                        "on tb_1_.ID = tb_2_.ID and tb_1_.CLIENT_TYPE = ? " +
                        "where tb_1_.ID in (?, ?) " +
                        "order by tb_1_.ID asc"
            )
            variables("KPerson", 600L, 602L)
            row(0) {
                assertTrue(it is KInstantiableClientExhaustiveView.Root)
                val root = it as KInstantiableClientExhaustiveView.Root
                assertEquals(600L, root.id)
                assertEquals("Joined Root", root.name)
            }
            row(1) {
                assertTrue(it is KInstantiableClientExhaustiveView.Person)
                val person = it as KInstantiableClientExhaustiveView.Person
                assertEquals(602L, person.id)
                assertEquals("Joined Inst Person", person.name)
                assertEquals("Joined", person.firstName)
            }
        }
    }
}
