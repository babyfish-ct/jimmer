package org.babyfish.jimmer.sql.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.ClientType;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumClient;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumPerson;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.dto.EnumClientDiscriminatorInput;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.dto.InstantiableClientDefaultInput;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Client;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Person;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PolymorphicDtoInputTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testImplicitDefaultInputCreatesRootEntityShape() {
        ClientPatchInput.Default input = new ClientPatchInput.Default();
        input.setId(10L);
        input.setName("Base patch");

        Client entity = input.toEntity();

        Assertions.assertEquals(Client.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(10L, entity.id());
        Assertions.assertEquals("Base patch", entity.name());
    }

    @Test
    public void testExplicitBranchInputCreatesDerivedTypeEntityShape() {
        ClientPatchInput.Organization input = new ClientPatchInput.Organization();
        input.setId(11L);
        input.setName("Org patch");
        input.setTaxCode("T-11");

        Organization entity = input.toEntity();

        Assertions.assertEquals(Organization.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(11L, entity.id());
        Assertions.assertEquals("Org patch", entity.name());
        Assertions.assertEquals("T-11", entity.taxCode());
    }

    @Test
    public void testJacksonTypeTagWithoutSelectedDiscriminatorCreatesExplicitBranch() throws Exception {
        JsonSubTypes jsonSubTypes = ClientPatchInput.class.getAnnotation(JsonSubTypes.class);
        JsonTypeName jsonTypeName = ClientPatchInput.Organization.class.getAnnotation(JsonTypeName.class);

        Assertions.assertNotNull(jsonSubTypes);
        Assertions.assertEquals(1, jsonSubTypes.value().length);
        Assertions.assertEquals(ClientPatchInput.Organization.class, jsonSubTypes.value()[0].value());
        Assertions.assertEquals("", jsonSubTypes.value()[0].name());
        Assertions.assertNotNull(jsonTypeName);
        Assertions.assertEquals("ORG", jsonTypeName.value());

        ClientPatchInput input = mapper.readValue(
                "{\"type\":\"ORG\",\"id\":21,\"name\":\"Org json\",\"taxCode\":\"T-21\"}",
                ClientPatchInput.class
        );

        Assertions.assertTrue(input instanceof ClientPatchInput.Organization);

        Organization entity = ((ClientPatchInput.Organization) input).toEntity();
        Assertions.assertEquals(Organization.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(21L, entity.id());
        Assertions.assertEquals("Org json", entity.name());
        Assertions.assertEquals("T-21", entity.taxCode());
    }

    @Test
    public void testJacksonMissingTypeTagWithoutSelectedDiscriminatorUsesDefaultBranch() throws Exception {
        ClientPatchInput input = mapper.readValue(
                "{\"id\":22,\"name\":\"Default json\"}",
                ClientPatchInput.class
        );

        Assertions.assertTrue(input instanceof ClientPatchInput.Default);

        Client entity = input.toEntity();
        Assertions.assertEquals(Client.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(22L, entity.id());
        Assertions.assertEquals("Default json", entity.name());
    }

    @Test
    public void testDefaultInputWithDiscriminatorCreatesDerivedTypeEntityShape() {
        ClientDiscriminatorInput.Default input = new ClientDiscriminatorInput.Default();
        input.setId(12L);
        input.setType("Person");
        input.setName("Person patch");

        Client entity = input.toEntity();

        Assertions.assertEquals(Person.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(12L, entity.id());
        Assertions.assertEquals("Person patch", entity.name());
    }

    @Test
    public void testJacksonUnknownTypeTagWithSelectedDiscriminatorUsesDefaultBranchAndKeepsValue() throws Exception {
        ClientDiscriminatorInput input = mapper.readValue(
                "{\"type\":\"Person\",\"id\":23,\"name\":\"Person json\"}",
                ClientDiscriminatorInput.class
        );

        Assertions.assertTrue(input instanceof ClientDiscriminatorInput.Default);
        Assertions.assertEquals("Person", input.getType());

        Client entity = input.toEntity();
        Assertions.assertEquals(Person.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(23L, entity.id());
        Assertions.assertEquals("Person json", entity.name());
    }

    @Test
    public void testJacksonSelectedDiscriminatorUsesDtoAlias() throws Exception {
        ClientAliasedDiscriminatorInput input = mapper.readValue(
                "{\"kind\":\"ORG\",\"id\":24,\"name\":\"Aliased org\",\"taxCode\":\"T-24\"}",
                ClientAliasedDiscriminatorInput.class
        );

        Assertions.assertTrue(input instanceof ClientAliasedDiscriminatorInput.Organization);
        Assertions.assertEquals("ORG", input.getKind());

        Organization entity = ((ClientAliasedDiscriminatorInput.Organization) input).toEntity();
        Assertions.assertEquals(Organization.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(24L, entity.id());
        Assertions.assertEquals("Aliased org", entity.name());
        Assertions.assertEquals("T-24", entity.taxCode());
    }

    @Test
    public void testUserJsonTypeInfoIsNotOverridden() throws Exception {
        JsonTypeInfo jsonTypeInfo = ClientCustomJsonTypeInfoInput.class.getAnnotation(JsonTypeInfo.class);

        Assertions.assertNotNull(jsonTypeInfo);
        Assertions.assertEquals("customType", jsonTypeInfo.property());

        ClientCustomJsonTypeInfoInput input = mapper.readValue(
                "{\"customType\":\"ORG\",\"id\":25,\"name\":\"Custom type info\",\"taxCode\":\"T-25\"}",
                ClientCustomJsonTypeInfoInput.class
        );

        Assertions.assertTrue(input instanceof ClientCustomJsonTypeInfoInput.Organization);
    }

    @Test
    public void testUserJsonTypeNameIsNotOverridden() throws Exception {
        JsonTypeName jsonTypeName =
                ClientCustomJsonTypeNameInput.Organization.class.getAnnotation(JsonTypeName.class);

        Assertions.assertNotNull(jsonTypeName);
        Assertions.assertEquals("company", jsonTypeName.value());

        ClientCustomJsonTypeNameInput input = mapper.readValue(
                "{\"type\":\"company\",\"id\":26,\"name\":\"Custom type name\",\"taxCode\":\"T-26\"}",
                ClientCustomJsonTypeNameInput.class
        );

        Assertions.assertTrue(input instanceof ClientCustomJsonTypeNameInput.Organization);
    }

    @Test
    public void testUserJsonSubTypesOwnsBranchNames() {
        JsonSubTypes jsonSubTypes = ClientCustomJsonSubTypesInput.class.getAnnotation(JsonSubTypes.class);
        JsonTypeName jsonTypeName =
                ClientCustomJsonSubTypesInput.Organization.class.getAnnotation(JsonTypeName.class);

        Assertions.assertNotNull(jsonSubTypes);
        Assertions.assertEquals(1, jsonSubTypes.value().length);
        Assertions.assertEquals(String.class, jsonSubTypes.value()[0].value());
        Assertions.assertNull(jsonTypeName);
    }

    @Test
    public void testBranchAndDiscriminatorInputCanAgree() {
        ClientDiscriminatorInput.Organization input = new ClientDiscriminatorInput.Organization();
        input.setId(13L);
        input.setType("ORG");
        input.setName("Org patch");
        input.setTaxCode("T-13");

        Organization entity = input.toEntity();

        Assertions.assertEquals(Organization.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(13L, entity.id());
        Assertions.assertEquals("Org patch", entity.name());
        Assertions.assertEquals("T-13", entity.taxCode());
    }

    @Test
    public void testBranchAndDiscriminatorInputMismatchIsRejected() {
        ClientDiscriminatorInput.Organization input = new ClientDiscriminatorInput.Organization();
        input.setId(13L);
        input.setType("Person");
        input.setName("Org patch");
        input.setTaxCode("T-13");

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                input::toEntity
        );

        Assertions.assertEquals(
                "Discriminator value \"Person\" does not match polymorphic input DTO branch " +
                        "\"org.babyfish.jimmer.sql.model.inheritance.singletable.dto." +
                        "ClientDiscriminatorInput.Organization\" whose entity type is " +
                        "\"org.babyfish.jimmer.sql.model.inheritance.singletable.Organization\"",
                ex.getMessage()
        );
    }

    @Test
    public void testEnumDefaultInputWithRootDiscriminatorCreatesRootEntityShape() {
        EnumClientDiscriminatorInput.Default input = new EnumClientDiscriminatorInput.Default();
        input.setId(130L);
        input.setType(ClientType.CLIENT);
        input.setName("Enum client");

        EnumClient entity = input.toEntity();

        Assertions.assertEquals(EnumClient.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(130L, entity.id());
        Assertions.assertEquals("Enum client", entity.name());
    }

    @Test
    public void testEnumDefaultInputWithDerivedTypeDiscriminatorCreatesDerivedTypeEntityShape() {
        EnumClientDiscriminatorInput.Default input = new EnumClientDiscriminatorInput.Default();
        input.setId(131L);
        input.setType(ClientType.PERSON);
        input.setName("Enum person");

        EnumClient entity = input.toEntity();

        Assertions.assertEquals(EnumPerson.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(131L, entity.id());
        Assertions.assertEquals("Enum person", entity.name());
    }

    @Test
    public void testEnumBranchAndDiscriminatorInputMismatchIsRejected() {
        EnumClientDiscriminatorInput.Organization input = new EnumClientDiscriminatorInput.Organization();
        input.setId(132L);
        input.setType(ClientType.PERSON);
        input.setName("Enum org");

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                input::toEntity
        );

        Assertions.assertEquals(
                "Discriminator value \"PERSON\" does not match polymorphic input DTO branch " +
                        "\"org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.dto." +
                        "EnumClientDiscriminatorInput.Organization\" whose entity type is " +
                        "\"org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumOrganization\"",
                ex.getMessage()
        );
    }

    @Test
    public void testExhaustiveGeneratedDerivedTypeInputCreatesDerivedTypeEntityShape() {
        ClientExhaustiveInput.Person input = new ClientExhaustiveInput.Person();
        input.setId(14L);
        input.setName("Person patch");
        input.setFirstName("Ann");
        input.setLastName("Smith");

        Person entity = input.toEntity();

        Assertions.assertEquals(Person.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(14L, entity.id());
        Assertions.assertEquals("Person patch", entity.name());
        Assertions.assertEquals("Ann", entity.firstName());
        Assertions.assertEquals("Smith", entity.lastName());
    }

    @Test
    public void testInstantiableRootDefaultInputCreatesRootEntityShape() {
        InstantiableClientDefaultInput.Base input = new InstantiableClientDefaultInput.Base();
        input.setId(15L);
        input.setName("Joined root patch");

        org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.Client entity =
                input.toEntity();

        Assertions.assertEquals(
                org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.Client.class,
                ((ImmutableSpi) entity).__type().getJavaClass()
        );
        Assertions.assertEquals(15L, entity.id());
        Assertions.assertEquals("Joined root patch", entity.name());
    }
}
