package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.dto.InstantiableClientDefaultInput;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Client;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Person;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientDiscriminatorInput;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientExhaustiveInput;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientPatchInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PolymorphicDtoInputTest {

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
    public void testExplicitBranchInputCreatesSubtypeEntityShape() {
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
    public void testExplicitDiscriminatorInputKeepsDiscriminatorLoaded() {
        ClientDiscriminatorInput.Default input = new ClientDiscriminatorInput.Default();
        input.setId(12L);
        input.setType("Person");
        input.setName("Person patch");

        Client entity = input.toEntity();

        Assertions.assertEquals(Client.class, ((ImmutableSpi) entity).__type().getJavaClass());
        Assertions.assertEquals(12L, entity.id());
        Assertions.assertEquals("Person", entity.type());
        Assertions.assertEquals("Person patch", entity.name());
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
        Assertions.assertEquals("ORG", entity.type());
        Assertions.assertEquals("Org patch", entity.name());
        Assertions.assertEquals("T-13", entity.taxCode());
    }

    @Test
    public void testExhaustiveGeneratedSubtypeInputCreatesSubtypeEntityShape() {
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
