package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Client;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Person;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class InheritanceMetadataTest {

    @Test
    public void testInheritanceInfo() {
        ImmutableType clientType = ImmutableType.get(Client.class);
        ImmutableType organizationType = ImmutableType.get(Organization.class);
        ImmutableType personType = ImmutableType.get(Person.class);

        InheritanceInfo info = clientType.getInheritanceInfo();
        assertSame(clientType, clientType.getInheritanceRoot());
        assertSame(clientType, organizationType.getInheritanceRoot());
        assertSame(clientType, personType.getInheritanceRoot());
        assertSame(info, organizationType.getInheritanceInfo());
        assertSame(info, personType.getInheritanceInfo());
        assertEquals(InheritanceType.SINGLE_TABLE, info.getStrategy());
        assertEquals("type", info.getDiscriminatorProp().getName());
        assertEquals("CLIENT_TYPE", info.getDiscriminatorProp().getAnnotation(Column.class).name());

        assertNull(clientType.getDiscriminatorValue());
        assertEquals("ORG", organizationType.getDiscriminatorValue());
        assertEquals("Person", personType.getDiscriminatorValue());
        assertEquals(
                "[Organization, Person]",
                info
                        .getConcreteTypes()
                        .stream()
                        .map(it -> it.getJavaClass().getSimpleName())
                        .collect(Collectors.toList())
                        .toString()
        );
        assertEquals(
                "{ORG=org.babyfish.jimmer.sql.model.inheritance.singletable.Organization, " +
                        "Person=org.babyfish.jimmer.sql.model.inheritance.singletable.Person}",
                info.getDiscriminatorTypeMap().toString()
        );
    }

}
