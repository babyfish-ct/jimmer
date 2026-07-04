package org.babyfish.jimmer.sql.kt.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.KClientType
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.KEnumClient
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.KEnumPerson
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.dto.KEnumClientDiscriminatorInput
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.dto.KInstantiableClientDefaultInput
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KOrganization
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KPerson
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.*
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientCustomJsonSubTypesInput
import kotlin.test.*
import org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.instantiable.KClient as KInstantiableClient

class PolymorphicDtoInputTest {

    private val mapper = ObjectMapper().registerKotlinModule()

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
    fun testExplicitBranchInputCreatesDerivedTypeEntityShape() {
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
    fun testJacksonTypeTagWithoutSelectedDiscriminatorCreatesExplicitBranch() {
        val jsonSubTypes = KClientPatchInput::class.java.getAnnotation(JsonSubTypes::class.java)
        val jsonTypeName = KClientPatchInput.Organization::class.java.getAnnotation(JsonTypeName::class.java)

        assertNotNull(jsonSubTypes)
        assertEquals(1, jsonSubTypes.value.size)
        assertEquals(KClientPatchInput.Organization::class.java, jsonSubTypes.value[0].value.java)
        assertEquals("", jsonSubTypes.value[0].name)
        assertNotNull(jsonTypeName)
        assertEquals("ORG", jsonTypeName.value)

        val input = mapper.readValue<KClientPatchInput>(
            """{"type":"ORG","id":21,"name":"Org json","taxCode":"T-21"}"""
        )

        assertEquals(KClientPatchInput.Organization::class, input::class)

        val entity = (input as KClientPatchInput.Organization).toEntity()
        assertEquals(KOrganization::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(21L, entity.id)
        assertEquals("Org json", entity.name)
        assertEquals("T-21", entity.taxCode)
    }

    @Test
    fun testJacksonSelectedDiscriminatorUsesDtoAlias() {
        val input = mapper.readValue<KClientAliasedDiscriminatorInput>(
            """{"kind":"ORG","id":22,"name":"Aliased org","taxCode":"T-22"}"""
        )

        assertEquals(KClientAliasedDiscriminatorInput.Organization::class, input::class)
        assertEquals("ORG", input.kind)

        val entity = (input as KClientAliasedDiscriminatorInput.Organization).toEntity()
        assertEquals(KOrganization::class.java, (entity as ImmutableSpi).__type().javaClass)
        assertEquals(22L, entity.id)
        assertEquals("Aliased org", entity.name)
        assertEquals("T-22", entity.taxCode)
    }

    @Test
    fun testUserJsonTypeInfoIsNotOverridden() {
        val jsonTypeInfo = KClientCustomJsonTypeInfoInput::class.java.getAnnotation(JsonTypeInfo::class.java)

        assertNotNull(jsonTypeInfo)
        assertEquals("customType", jsonTypeInfo.property)

        val input = mapper.readValue<KClientCustomJsonTypeInfoInput>(
            """{"customType":"ORG","id":23,"name":"Custom type info","taxCode":"T-23"}"""
        )

        assertEquals(KClientCustomJsonTypeInfoInput.Organization::class, input::class)
    }

    @Test
    fun testUserJsonTypeNameIsNotOverridden() {
        val jsonTypeName = KClientCustomJsonTypeNameInput.Organization::class.java
            .getAnnotation(JsonTypeName::class.java)

        assertNotNull(jsonTypeName)
        assertEquals("company", jsonTypeName.value)

        val input = mapper.readValue<KClientCustomJsonTypeNameInput>(
            """{"type":"company","id":24,"name":"Custom type name","taxCode":"T-24"}"""
        )

        assertEquals(KClientCustomJsonTypeNameInput.Organization::class, input::class)
    }

    @Test
    fun testUserJsonSubTypesOwnsBranchNames() {
        val jsonSubTypes = KClientCustomJsonSubTypesInput::class.java.getAnnotation(JsonSubTypes::class.java)
        val jsonTypeName = KClientCustomJsonSubTypesInput.Organization::class.java
            .getAnnotation(JsonTypeName::class.java)

        assertNotNull(jsonSubTypes)
        assertEquals(1, jsonSubTypes.value.size)
        assertEquals(String::class.java, jsonSubTypes.value[0].value.java)
        assertNull(jsonTypeName)
    }

    @Test
    fun testDefaultInputWithDiscriminatorCreatesDerivedTypeEntityShape() {
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
    fun testEnumDefaultInputWithDerivedTypeDiscriminatorCreatesDerivedTypeEntityShape() {
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
    fun testExhaustiveGeneratedDerivedTypeInputCreatesDerivedTypeEntityShape() {
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
