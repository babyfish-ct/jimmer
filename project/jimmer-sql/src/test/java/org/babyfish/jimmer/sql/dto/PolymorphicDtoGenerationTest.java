package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.inheritance.joinedtable.dto.ClientExhaustiveView;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.dto.InstantiableClientDefaultView;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.dto.InstantiableClientExhaustiveView;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.dto.InstantiableClientSimpleView;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientDefaultView;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientImplicitCatchAllView;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientImplicitDefaultView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testpkg.Named;

import java.util.Arrays;

public class PolymorphicDtoGenerationTest {

    @Test
    public void testAbstractRootWithExhaustiveBranches() throws ClassNotFoundException {
        Assertions.assertTrue(ClientExhaustiveView.class.isInterface());
        Assertions.assertTrue(ClientExhaustiveView.class.isAnnotationPresent(Deprecated.class));
        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName(ClientExhaustiveView.class.getName() + "$Default")
        );

        ClientExhaustiveView.OrganizationBranch organization =
                new ClientExhaustiveView.OrganizationBranch();
        organization.setId(1L);
        organization.setName("Acme");
        organization.setTaxCode("T-1");
        organization.setProjectIds(Arrays.asList(10L, 11L));

        ClientExhaustiveView view = organization;
        Assertions.assertEquals(1L, view.getId());
        Assertions.assertEquals("Acme", view.getName());
        Assertions.assertEquals("T-1", organization.getTaxCode());
        Assertions.assertEquals(Arrays.asList(10L, 11L), organization.getProjectIds());
        Assertions.assertTrue(Named.class.isAssignableFrom(ClientExhaustiveView.OrganizationBranch.class));
        Assertions.assertTrue(ClientExhaustiveView.OrganizationBranch.class.isAnnotationPresent(Deprecated.class));
        Assertions.assertThrows(
                NoSuchFieldException.class,
                () -> ClientExhaustiveView.OrganizationBranch.class.getDeclaredField("METADATA")
        );
    }

    @Test
    public void testAbstractRootWithDefaultBranch() {
        ClientDefaultView.Other other = new ClientDefaultView.Other();
        other.setId(2L);
        other.setName("Base fields only");

        ClientDefaultView view = other;
        Assertions.assertEquals(2L, view.getId());
        Assertions.assertEquals("Base fields only", view.getName());

        ClientDefaultView.Organization organization = new ClientDefaultView.Organization();
        organization.setId(3L);
        organization.setName("Org");
        organization.setTaxCode("T-2");
        Assertions.assertEquals("T-2", organization.getTaxCode());
    }

    @Test
    public void testAbstractRootWithImplicitDefaultBranchName() {
        ClientImplicitDefaultView.Default defaultBranch = new ClientImplicitDefaultView.Default();
        defaultBranch.setId(8L);
        defaultBranch.setName("Implicit default");

        ClientImplicitDefaultView view = defaultBranch;
        Assertions.assertEquals(8L, view.getId());
        Assertions.assertEquals("Implicit default", view.getName());

        ClientImplicitDefaultView.Person person = new ClientImplicitDefaultView.Person();
        person.setId(9L);
        person.setName("Person");
        person.setFirstName("Sam");
        Assertions.assertEquals("Sam", person.getFirstName());
    }

    @Test
    public void testAbstractRootWithImplicitCatchAllBranch() {
        ClientImplicitCatchAllView.Default defaultBranch = new ClientImplicitCatchAllView.Default();
        defaultBranch.setId(10L);
        defaultBranch.setName("Implicit catch all");

        ClientImplicitCatchAllView view = defaultBranch;
        Assertions.assertEquals(10L, view.getId());
        Assertions.assertEquals("Implicit catch all", view.getName());

        ClientImplicitCatchAllView.Organization organization = new ClientImplicitCatchAllView.Organization();
        organization.setId(11L);
        organization.setName("Organization");
        organization.setTaxCode("T-4");
        Assertions.assertEquals("T-4", organization.getTaxCode());
    }

    @Test
    public void testConcreteRootWithoutTypesIsOrdinaryDtoClass() {
        Assertions.assertFalse(InstantiableClientSimpleView.class.isInterface());
        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName(InstantiableClientSimpleView.class.getName() + "$Default")
        );

        InstantiableClientSimpleView view = new InstantiableClientSimpleView();
        view.setId(12L);
        view.setName("Ordinary concrete root view");

        Assertions.assertEquals(12L, view.getId());
        Assertions.assertEquals("Ordinary concrete root view", view.getName());
    }

    @Test
    public void testConcreteRootWithDefaultBranch() {
        InstantiableClientDefaultView.Base base = new InstantiableClientDefaultView.Base();
        base.setId(4L);
        base.setName("Concrete root");

        InstantiableClientDefaultView view = base;
        Assertions.assertEquals(4L, view.getId());
        Assertions.assertEquals("Concrete root", view.getName());

        InstantiableClientDefaultView.Organization organization =
                new InstantiableClientDefaultView.Organization();
        organization.setId(5L);
        organization.setName("Concrete org");
        organization.setTaxCode("T-3");
        Assertions.assertEquals("T-3", organization.getTaxCode());
    }

    @Test
    public void testConcreteRootWithExhaustiveBranches() throws ClassNotFoundException {
        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName(InstantiableClientExhaustiveView.class.getName() + "$Default")
        );

        InstantiableClientExhaustiveView.Root root = new InstantiableClientExhaustiveView.Root();
        root.setId(6L);
        root.setName("Root");

        InstantiableClientExhaustiveView.Person person = new InstantiableClientExhaustiveView.Person();
        person.setId(7L);
        person.setName("Person");
        person.setFirstName("Alex");

        Assertions.assertEquals("Root", ((InstantiableClientExhaustiveView) root).getName());
        Assertions.assertEquals("Alex", person.getFirstName());
    }
}
