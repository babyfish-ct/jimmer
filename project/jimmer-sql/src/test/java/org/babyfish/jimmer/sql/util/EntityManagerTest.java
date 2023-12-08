package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.meta.ForeignKeyStrategy;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.model.inheritance.*;
import org.babyfish.jimmer.sql.runtime.DefaultDatabaseNamingStrategy;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class EntityManagerTest {

    @Test
    public void testBackProps() {
        Assertions.assertEquals(
                Arrays.asList(
                        "org.babyfish.jimmer.sql.model.inheritance.AdministratorMetadata.administrator",
                        "org.babyfish.jimmer.sql.model.inheritance.Role.administrators"
                ),
                EntityManager.fromResources(null, null)
                        .getAllBackProps(ImmutableType.get(Administrator.class)).stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Arrays.asList(
                        "org.babyfish.jimmer.sql.model.inheritance.Administrator.roles",
                        "org.babyfish.jimmer.sql.model.inheritance.Permission.role"
                ),
                EntityManager.fromResources(null, null).getAllBackProps(ImmutableType.get(Role.class)).stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Collections.singletonList(
                        "org.babyfish.jimmer.sql.model.inheritance.Role.permissions"
                ),
                EntityManager.fromResources(null, null).getAllBackProps(ImmutableType.get(Permission.class)).stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Collections.singletonList(
                        "org.babyfish.jimmer.sql.model.inheritance.Administrator.metadata"
                ),
                EntityManager.fromResources(null, null).getAllBackProps(ImmutableType.get(AdministratorMetadata.class)).stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.toList())
        );
    }

    @Test
    public void testTableName() {
        MetadataStrategy strategy = new MetadataStrategy(
                DefaultDatabaseNamingStrategy.UPPER_CASE,
                ForeignKeyStrategy.REAL
        );
        Assertions.assertEquals(
                ImmutableType.get(Role.class),
                EntityManager.fromResources(null, null).getNonNullTypeByServiceAndTable("", "`roLE`", strategy)
        );
        Assertions.assertEquals(
                ImmutableType.get(Permission.class),
                EntityManager.fromResources(null, null).getNonNullTypeByServiceAndTable("", "[PerMission]", strategy)
        );
        Assertions.assertEquals(
                ImmutableType.get(Administrator.class),
                EntityManager.fromResources(null, null).getNonNullTypeByServiceAndTable("", "\"Administrator\"", strategy)
        );
        Assertions.assertEquals(
                AssociationType.of(AdministratorProps.ROLES),
                EntityManager.fromResources(null, null).getNonNullTypeByServiceAndTable("", "`Administrator_Role_Mapping`", strategy)
        );
    }
}
