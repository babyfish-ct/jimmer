package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.inheritance.*;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class EntityManagerTest {

    private EntityManager em = new EntityManager(
            EntityManagerTest.class.getClassLoader(),
            Book.class.getPackage().getName()
    );

    @Test
    public void testBackProps() {
        Assertions.assertEquals(
                Arrays.asList(
                        "org.babyfish.jimmer.sql.model.inheritance.AdministratorMetadata.administrator",
                        "org.babyfish.jimmer.sql.model.inheritance.Role.administrators"
                ),
                em.getAllBackProps(ImmutableType.get(Administrator.class)).stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Arrays.asList(
                        "org.babyfish.jimmer.sql.model.inheritance.Administrator.roles",
                        "org.babyfish.jimmer.sql.model.inheritance.Permission.role"
                ),
                em.getAllBackProps(ImmutableType.get(Role.class)).stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Collections.singletonList(
                        "org.babyfish.jimmer.sql.model.inheritance.Role.permissions"
                ),
                em.getAllBackProps(ImmutableType.get(Permission.class)).stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Collections.singletonList(
                        "org.babyfish.jimmer.sql.model.inheritance.Administrator.metadata"
                ),
                em.getAllBackProps(ImmutableType.get(AdministratorMetadata.class)).stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.toList())
        );
    }

    @Test
    public void testTableName() {
        Assertions.assertEquals(
                ImmutableType.get(Role.class),
                em.getTypeByTableName("`roLE`")
        );
        Assertions.assertEquals(
                ImmutableType.get(Permission.class),
                em.getTypeByTableName("[PerMission]")
        );
        Assertions.assertEquals(
                ImmutableType.get(Administrator.class),
                em.getTypeByTableName("\"Administrator\"")
        );
        Assertions.assertEquals(
                AssociationType.of(AdministratorProps.ROLES),
                em.getTypeByTableName("`Administrator_Role_Mapping`")
        );
    }
}
