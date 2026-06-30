package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.KClientType
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.KEnumClient
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.KEnumPerson
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.dto.KEnumClientDiscriminatorInput
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.dto.KInstantiableClientDefaultInput
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KOrganization
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KPerson
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientDiscriminatorInput
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientExhaustiveInput
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientPatchInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.KClient as KInstantiableClient

class PolymorphicDtoInputTest {

    @Test
    fun testImplicitDefaultInputCreatesRootEntityShape() {
        val entity = KClientPatchInput.Default(
            id = 10L,
            name = "Base patch"
        ).toEntity()

        assertEquals(KClient::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(10L, entity.id)
        assertEquals("Base patch", entity.name)
    }

    @Test
    fun testExplicitBranchInputCreatesSubtypeEntityShape() {
        val entity = KClientPatchInput.Organization(
            id = 11L,
            name = "Org patch",
            taxCode = "T-11"
        ).toEntity()

        assertEquals(KOrganization::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(11L, entity.id)
        assertEquals("Org patch", entity.name)
        assertEquals("T-11", entity.taxCode)
    }

    @Test
    fun testDefaultInputWithDiscriminatorCreatesSubtypeEntityShape() {
        val entity = KClientDiscriminatorInput.Default(
            id = 12L,
            type = "KPerson",
            name = "Person patch"
        ).toEntity()

        assertEquals(KPerson::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(12L, entity.id)
        assertEquals("Person patch", entity.name)
    }

    @Test
    fun testBranchAndDiscriminatorInputCanAgree() {
        val entity = KClientDiscriminatorInput.Organization(
            id = 13L,
            type = "ORG",
            name = "Org patch",
            taxCode = "T-13"
        ).toEntity()

        assertEquals(KOrganization::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(13L, entity.id)
        assertEquals("Org patch", entity.name)
        assertEquals("T-13", entity.taxCode)
    }

    @Test
    fun testBranchAndDiscriminatorInputMismatchIsRejected() {
        val ex = assertFailsWith<IllegalArgumentException> {
            KClientDiscriminatorInput.Organization(
                id = 13L,
                type = "KPerson",
                name = "Org patch",
                taxCode = "T-13"
            ).toEntity()
        }

        assertEquals(
            "Discriminator value \"KPerson\" does not match polymorphic input DTO branch " +
                "\"org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto." +
                "KClientDiscriminatorInput.Organization\" whose entity type is " +
                "\"org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KOrganization\"",
            ex.message
        )
    }

    @Test
    fun testEnumDefaultInputWithRootDiscriminatorCreatesRootEntityShape() {
        val entity = KEnumClientDiscriminatorInput.Default(
            id = 130L,
            type = KClientType.CLIENT,
            name = "Enum client"
        ).toEntity()

        assertEquals(KEnumClient::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(130L, entity.id)
        assertEquals("Enum client", entity.name)
    }

    @Test
    fun testEnumDefaultInputWithSubtypeDiscriminatorCreatesSubtypeEntityShape() {
        val entity = KEnumClientDiscriminatorInput.Default(
            id = 131L,
            type = KClientType.PERSON,
            name = "Enum person"
        ).toEntity()

        assertEquals(KEnumPerson::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(131L, entity.id)
        assertEquals("Enum person", entity.name)
    }

    @Test
    fun testEnumBranchAndDiscriminatorInputMismatchIsRejected() {
        val ex = assertFailsWith<IllegalArgumentException> {
            KEnumClientDiscriminatorInput.Organization(
                id = 132L,
                type = KClientType.PERSON,
                name = "Enum org"
            ).toEntity()
        }

        assertEquals(
            "Discriminator value \"PERSON\" does not match polymorphic input DTO branch " +
                "\"org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.dto." +
                "KEnumClientDiscriminatorInput.Organization\" whose entity type is " +
                "\"org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.KEnumOrganization\"",
            ex.message
        )
    }

    @Test
    fun testExhaustiveGeneratedSubtypeInputCreatesSubtypeEntityShape() {
        val entity = KClientExhaustiveInput.Person(
            id = 14L,
            name = "Person patch",
            firstName = "Ann",
            lastName = "Smith"
        ).toEntity()

        assertEquals(KPerson::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(14L, entity.id)
        assertEquals("Person patch", entity.name)
        assertEquals("Ann", entity.firstName)
        assertEquals("Smith", entity.lastName)
    }

    @Test
    fun testInstantiableRootDefaultInputCreatesRootEntityShape() {
        val entity = KInstantiableClientDefaultInput.Base(
            id = 15L,
            name = "Joined root patch"
        ).toEntity()

        assertEquals(KInstantiableClient::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(15L, entity.id)
        assertEquals("Joined root patch", entity.name)
    }
}
