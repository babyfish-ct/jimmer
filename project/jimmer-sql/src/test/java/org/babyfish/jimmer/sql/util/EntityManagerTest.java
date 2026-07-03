package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.model.inheritance.Administrator;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorMetadata;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorProps;
import org.babyfish.jimmer.sql.model.inheritance.Permission;
import org.babyfish.jimmer.sql.model.inheritance.Role;
import org.babyfish.jimmer.sql.model.schema.SchemaATable;
import org.babyfish.jimmer.sql.model.schema.SchemaBTable;
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
    public void testBackPropsForInheritance() {
        EntityManager entityManager = EntityManager.fromResources(null, null);
        Assertions.assertEquals(
                Arrays.asList(
                        "org.babyfish.jimmer.sql.model.inheritance.singletable.ClientProject.client",
                        "org.babyfish.jimmer.sql.model.inheritance.singletable.OrganizationProject.organization"
                ),
                entityManager
                        .getAllBackProps(ImmutableType.get(org.babyfish.jimmer.sql.model.inheritance.singletable.Client.class))
                        .stream()
                        .map(Object::toString)
                        .filter(it -> it.startsWith("org.babyfish.jimmer.sql.model.inheritance.singletable."))
                        .sorted()
                        .collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Arrays.asList(
                        "org.babyfish.jimmer.sql.model.inheritance.singletable.ClientProject.client",
                        "org.babyfish.jimmer.sql.model.inheritance.singletable.OrganizationProject.organization"
                ),
                entityManager
                        .getAllBackProps(ImmutableType.get(org.babyfish.jimmer.sql.model.inheritance.singletable.Organization.class))
                        .stream()
                        .map(Object::toString)
                        .filter(it -> it.startsWith("org.babyfish.jimmer.sql.model.inheritance.singletable."))
                        .sorted()
                        .collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Arrays.asList(
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.ClientProject.client",
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationProject.organization"
                ),
                entityManager
                        .getAllBackProps(ImmutableType.get(org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client.class))
                        .stream()
                        .map(Object::toString)
                        .filter(it -> it.startsWith("org.babyfish.jimmer.sql.model.inheritance.joinedtable."))
                        .sorted()
                        .collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Arrays.asList(
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.ClientProject.client",
                        "org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationProject.organization"
                ),
                entityManager
                        .getAllBackProps(ImmutableType.get(org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization.class))
                        .stream()
                        .map(Object::toString)
                        .filter(it -> it.startsWith("org.babyfish.jimmer.sql.model.inheritance.joinedtable."))
                        .sorted()
                        .collect(Collectors.toList())
        );
    }

    @Test
    public void testTableName() {
        MetadataStrategy strategy = new MetadataStrategy(
                DatabaseSchemaStrategy.IMPLICIT,
                DefaultDatabaseNamingStrategy.UPPER_CASE,
                ForeignKeyStrategy.REAL,
                new H2Dialect(),
                prop -> null,
                MetaStringResolver.NO_OP
        );
        Assertions.assertEquals(
                ImmutableType.get(Role.class),
                EntityManager.fromResources(null, null).getTypeMapByServiceAndTable("", "`roLE`", strategy).get(null)
        );
        Assertions.assertEquals(
                ImmutableType.get(Permission.class),
                EntityManager.fromResources(null, null).getTypeMapByServiceAndTable("", "[PerMission]", strategy).get(null)
        );
        Assertions.assertEquals(
                ImmutableType.get(Administrator.class),
                EntityManager.fromResources(null, null).getTypeMapByServiceAndTable("", "\"Administrator\"", strategy).get(null)
        );
        Assertions.assertEquals(
                AssociationType.of(AdministratorProps.ROLES),
                EntityManager.fromResources(null, null).getTypeMapByServiceAndTable("", "`Administrator_Role_Mapping`", strategy).get(null)
        );
    }

    @Test
    public void testSameTableNameInDifferentSchemas() {
        MetadataStrategy strategy = new MetadataStrategy(
                DatabaseSchemaStrategy.IMPLICIT,
                DefaultDatabaseNamingStrategy.UPPER_CASE,
                ForeignKeyStrategy.REAL,
                new H2Dialect(),
                prop -> null,
                MetaStringResolver.NO_OP
        );
        EntityManager entityManager = new EntityManager(SchemaATable.class, SchemaBTable.class);

        entityManager.validate(strategy);

        Assertions.assertEquals(
                ImmutableType.get(SchemaATable.class),
                entityManager.getTypeMapByServiceAndTable("", "SCHEMA_A.MY_TABLE", strategy).get(null)
        );
        Assertions.assertEquals(
                ImmutableType.get(SchemaBTable.class),
                entityManager.getTypeMapByServiceAndTable("", "SCHEMA_B.MY_TABLE", strategy).get(null)
        );
        Assertions.assertTrue(
                entityManager.getTypeMapByServiceAndTable("", "MY_TABLE", strategy).isEmpty()
        );
    }

    @Test
    public void testImplicitAndExplicitSchemaConflict() {
        MetadataStrategy strategy = new MetadataStrategy(
                DatabaseSchemaStrategy.IMPLICIT,
                DefaultDatabaseNamingStrategy.UPPER_CASE,
                ForeignKeyStrategy.REAL,
                new H2Dialect(),
                prop -> null,
                it -> {
                    if ("SCHEMA_A".equals(it)) {
                        return "";
                    }
                    if ("SCHEMA_B".equals(it)) {
                        return "SCHEMA_A";
                    }
                    return it;
                }
        );
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new EntityManager(SchemaATable.class, SchemaBTable.class).validate(strategy)
        );

        Assertions.assertEquals(
                "Illegal entity manager, the table \"MY_TABLE\" is shared by both " +
                        "\"org.babyfish.jimmer.sql.model.schema.SchemaATable\" and " +
                        "\"org.babyfish.jimmer.sql.model.schema.SchemaBTable\"",
                ex.getMessage()
        );
    }
}
