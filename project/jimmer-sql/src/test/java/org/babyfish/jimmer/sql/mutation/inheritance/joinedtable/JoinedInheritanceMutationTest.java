package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.*;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class JoinedInheritanceMutationTest extends AbstractMutationTest {

    private static String joinedClientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                        "from JOINED_CLIENT c " +
                        "left join JOINED_ORGANIZATION o on c.ID = o.ID " +
                        "left join JOINED_PERSON p on c.ID = p.ID " +
                        "where c.ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return "[" +
                        rs.getString(1) +
                        ", " +
                        rs.getString(2) +
                        ", " +
                        rs.getString(3) +
                        ", " +
                        rs.getString(4) +
                        ", " +
                        rs.getString(5) +
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String joinedClientProjectTargetId(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_ID from JOINED_CLIENT_PROJECT where ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Object value = rs.getObject(1);
                return value != null ? value.toString() : null;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String joinedOrgProjectTargetId(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select ORGANIZATION_ID from JOINED_ORG_PROJECT where ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Object value = rs.getObject(1);
                return value != null ? value.toString() : null;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testInsertSubtype() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(300L);
                                    organization.setName("New Org");
                                    organization.setTaxCode("NEW-001");
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_CLIENT(ID, CLIENT_TYPE, NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(300L, "ORG", "New Org");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(300L, "NEW-001");
                    });
                    ctx.rowCount(AffectedTable.of(Client.class), 1);
                    ctx.rowCount(AffectedTable.of(Organization.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                        it.modified("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                    });
                }
        );
    }

    @Test
    public void testUpdateAbstractRootWithUserOptimisticLock() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientDraft.$.produce(client -> {
                                        client.setId(200L);
                                        client.setName("Globex Base+");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setOptimisticLock(
                                    ClientTable.class,
                                    (table, it) -> table.name().eq("Globex")
                            )
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and NAME = ?"
                        );
                        it.variables("Globex Base+", 200L, "Globex");
                    });
                    ctx.value("[ORG, Globex Base+, GLOBEX-001, null, null]");
                }
        );
    }

    @Test
    public void testUpsertSubtype() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(300L);
                                    organization.setName("New Org");
                                    organization.setTaxCode("NEW-001");
                                })
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(300L, "ORG", "New Org");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "key(ID) values(?, ?)"
                        );
                        it.variables(300L, "NEW-001");
                    });
                    ctx.rowCount(AffectedTable.of(Client.class), 1);
                    ctx.rowCount(AffectedTable.of(Organization.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                        it.modified("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                    });
                }
        );
    }

    @Test
    public void testUpsertSubtypeWithChangingDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    PersonDraft.$.produce(person -> {
                                        person.setId(200L);
                                        person.setName("Globex Person");
                                        person.setFirstName("Gary");
                                        person.setLastName("Stone");
                                    })
                            )
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Person", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.variables(200L, "Gary", "Stone");
                    });
                    ctx.value("[Person, Globex Person, null, Gary, Stone]");
                }
        );
    }

    @Test
    public void testUpdateSubtypeWithChangingDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    PersonDraft.$.produce(person -> {
                                        person.setId(200L);
                                        person.setName("Globex Person");
                                        person.setFirstName("Gary");
                                        person.setLastName("Stone");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Person", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.variables(200L, "Gary", "Stone");
                    });
                    ctx.value("[Person, Globex Person, null, Gary, Stone]");
                }
        );
    }

    @Test
    public void testUpdateSubtypeWithSubtypeChangeAllowedButSameDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(200L);
                                        organization.setName("Globex Same");
                                        organization.setTaxCode("GLOBEX-SAME");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Globex Same", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "key(ID) values(?, ?)"
                        );
                        it.variables(200L, "GLOBEX-SAME");
                    });
                    ctx.value("[ORG, Globex Same, GLOBEX-SAME, null, null]");
                }
        );
    }

    @Test
    public void testUpdateSubtypeWithSubtypeChangeAllowedMixedBatch() {
        connectAndExpect(
                con -> {
                    Person changed = PersonDraft.$.produce(person -> {
                        person.setId(200L);
                        person.setName("Globex Person Batch");
                        person.setFirstName("Gary");
                        person.setLastName("Stone");
                    });
                    Person same = PersonDraft.$.produce(person -> {
                        person.setId(201L);
                        person.setName("Alice Person Batch");
                        person.setFirstName("Alice+");
                        person.setLastName("Smith+");
                    });
                    Person missing = PersonDraft.$.produce(person -> {
                        person.setId(399L);
                        person.setName("Missing Person Batch");
                        person.setFirstName("Missing");
                        person.setLastName("Person");
                    });
                    getSqlClient()
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(changed, same, missing))
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedClientRow(con, 399L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID in (?, ?, ?) order by ID");
                        it.variables(200L, 201L, 399L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Person Batch", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Alice Person Batch", 201L, "Person");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.batchVariables(0, 200L, "Gary", "Stone");
                        it.batchVariables(1, 201L, "Alice+", "Smith+");
                    });
                    ctx.value("[Person, Globex Person Batch, null, Gary, Stone]; " +
                            "[Person, Alice Person Batch, null, Alice+, Smith+]; " +
                            "null");
                }
        );
    }

    @Test
    public void testUpdateSubtypeWithChangingDiscriminatorMissingSkipsChildAndPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(399L);
                                        organization.setName("Missing Org");
                                        organization.setTaxCode("MISSING-ORG");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2313L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setSubtypeChangeAllowed(true)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 399L) + "; " + joinedOrgProjectTargetId(con, 2313L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(399L);
                    });
                    ctx.value("null; null");
                }
        );
    }

    @Test
    public void testUpsertSubtypeWithSubtypeChangeAllowedMixedBatch() {
        connectAndExpect(
                con -> {
                    Person changed = PersonDraft.$.produce(person -> {
                        person.setId(200L);
                        person.setName("Globex Person Upsert");
                        person.setFirstName("Gary");
                        person.setLastName("Stone");
                    });
                    Person same = PersonDraft.$.produce(person -> {
                        person.setId(201L);
                        person.setName("Alice Person Upsert");
                        person.setFirstName("Alice+");
                        person.setLastName("Smith+");
                    });
                    Person inserted = PersonDraft.$.produce(person -> {
                        person.setId(399L);
                        person.setName("Inserted Person Upsert");
                        person.setFirstName("Inserted");
                        person.setLastName("Person");
                    });
                    getSqlClient()
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(changed, same, inserted))
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedClientRow(con, 399L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID in (?, ?, ?) order by ID");
                        it.variables(200L, 201L, 399L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Person Upsert", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Alice Person Upsert", 201L, "Person");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(399L, "Person", "Inserted Person Upsert");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.batchVariables(0, 200L, "Gary", "Stone");
                        it.batchVariables(1, 201L, "Alice+", "Smith+");
                        it.batchVariables(2, 399L, "Inserted", "Person");
                    });
                    ctx.value("[Person, Globex Person Upsert, null, Gary, Stone]; " +
                            "[Person, Alice Person Upsert, null, Alice+, Smith+]; " +
                            "[Person, Inserted Person Upsert, null, Inserted, Person]");
                }
        );
    }

    @Test
    public void testInsertIfAbsentIgnoresSubtypeChangeAllowed() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2314L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setSubtypeChangeAllowed(true)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 201L) + "; " + joinedOrgProjectTargetId(con, 2314L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(201L, "ORG", "Should not update");
                    });
                    ctx.value("[Person, Alice, null, Alice, Smith]; null");
                }
        );
    }

    @Test
    public void testUpdateSubtypeWithoutChangingDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(200L);
                                        organization.setName("Globex+");
                                        organization.setTaxCode("GLOBEX-002");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Globex+", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT " +
                                        "where JOINED_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                        );
                        it.variables("GLOBEX-002", 200L, 200L, "ORG");
                    });
                    ctx.value("[ORG, Globex+, GLOBEX-002, null, null]");
                }
        );
    }

    @Test
    public void testUpdateRootAssociationToSubtypeTarget() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                ClientProjectDraft.$.produce(project -> {
                                    project.setId(2000L);
                                    project.setName("Joined root project+");
                                    project.setClientId(201L);
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT_PROJECT " +
                                        "set NAME = ?, CLIENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Joined root project+", 201L, 2000L);
                    });
                    ctx.rowCount(AffectedTable.of(ClientProject.class), 1);
                    ctx.entity(it -> {
                    });
                }
        );
    }

    @Test
    public void testAssociatedSubtypeChangeDefaultNoOp() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientProjectDraft.$.produce(project -> {
                                        project.setId(2000L);
                                        project.setName("Joined associated no-op");
                                        project.setClient(
                                                PersonDraft.$.produce(person -> {
                                                    person.setId(200L);
                                                    person.setName("Should not replace");
                                                    person.setFirstName("Should");
                                                    person.setLastName("NotReplace");
                                                })
                                        );
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedClientProjectTargetId(con, 2000L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(200L, "Person", "Should not replace");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT_PROJECT " +
                                        "set NAME = ?, CLIENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Joined associated no-op", 200L, 2000L);
                    });
                    ctx.value("[ORG, Globex, GLOBEX-001, null, null]; 200");
                }
        );
    }

    @Test
    public void testAssociatedSubtypeChangeAllowedByClass() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientProjectDraft.$.produce(project -> {
                                        project.setId(2000L);
                                        project.setName("Joined associated replace");
                                        project.setClient(
                                                PersonDraft.$.produce(person -> {
                                                    person.setId(200L);
                                                    person.setName("Globex Associated Person");
                                                    person.setFirstName("Gary");
                                                    person.setLastName("Stone");
                                                })
                                        );
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedSubtypeChangeAllowed(Client.class)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedClientProjectTargetId(con, 2000L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", "Globex Associated Person", 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.variables(200L, "Gary", "Stone");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT_PROJECT " +
                                        "set NAME = ?, CLIENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Joined associated replace", 200L, 2000L);
                    });
                    ctx.value("[Person, Globex Associated Person, null, Gary, Stone]; 200");
                }
        );
    }

    @Test
    public void testUpdateSubtypeAssociationToSubtypeTarget() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationProjectDraft.$.produce(project -> {
                                    project.setId(2001L);
                                    project.setName("Joined organization project+");
                                    project.setOrganizationId(202L);
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORG_PROJECT " +
                                        "set NAME = ?, ORGANIZATION_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Joined organization project+", 202L, 2001L);
                    });
                    ctx.rowCount(AffectedTable.of(OrganizationProject.class), 1);
                    ctx.entity(it -> {
                    });
                }
        );
    }

    @Test
    public void testUpdateSubtypeWithAcceptedPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(200L);
                                        organization.setTaxCode("GLOBEX-003");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2300L);
                                            project.setName("Accepted project");
                                        });
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedOrgProjectTargetId(con, 2300L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT " +
                                        "where JOINED_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                        );
                        it.variables("GLOBEX-003", 200L, 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2300L, "Accepted project", 200L);
                    });
                    ctx.value("[ORG, Globex, GLOBEX-003, null, null]; 200");
                }
        );
    }

    @Test
    public void testUpdateSubtypeMismatchSkipsPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2301L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedClientRow(con, 201L) + "; " + joinedOrgProjectTargetId(con, 2301L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(201L, "ORG");
                    });
                    ctx.value("[Person, Alice, null, Alice, Smith]; null");
                }
        );
    }

    @Test
    public void testInsertIfAbsentSubtypeWithAcceptedPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(300L);
                                        organization.setName("New Org");
                                        organization.setTaxCode("NEW-001");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2302L);
                                            project.setName("Inserted project");
                                        });
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 300L) + "; " + joinedOrgProjectTargetId(con, 2302L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(300L, "ORG", "New Org");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(300L, "NEW-001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2302L, "Inserted project", 300L);
                    });
                    ctx.value("[ORG, New Org, NEW-001, null, null]; 300");
                }
        );
    }

    @Test
    public void testInsertIfAbsentSubtypeExistingSameSkipsPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(200L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2303L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedOrgProjectTargetId(con, 2303L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(200L, "ORG", "Should not update");
                    });
                    ctx.value("[ORG, Globex, GLOBEX-001, null, null]; null");
                }
        );
    }

    @Test
    public void testInsertIfAbsentSubtypeExistingDifferentSkipsPostAssociation() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(201L);
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                        organization.addIntoProjects(project -> {
                                            project.setId(2304L);
                                            project.setName("Should not be saved");
                                        });
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 201L) + "; " + joinedOrgProjectTargetId(con, 2304L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(201L, "ORG", "Should not update");
                    });
                    ctx.value("[Person, Alice, null, Alice, Smith]; null");
                }
        );
    }

    @Test
    public void testUpdateSubtypeBatchRoutesOnlyAcceptedRows() {
        connectAndExpect(
                con -> {
                    Organization accepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setTaxCode("GLOBEX-004");
                        organization.addIntoProjects(project -> {
                            project.setId(2305L);
                            project.setName("Accepted batch project");
                        });
                    });
                    Organization rejected = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2306L);
                            project.setName("Rejected batch project");
                        });
                    });
                    getSqlClient()
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(accepted, rejected))
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2305L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2306L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.batchVariables(0, 200L, "ORG");
                        it.batchVariables(1, 201L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT " +
                                        "where JOINED_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                        );
                        it.variables("GLOBEX-004", 200L, 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2305L, "Accepted batch project", 200L);
                    });
                    ctx.value("[ORG, Globex, GLOBEX-004, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]; " +
                            "200; null");
                }
        );
    }

    @Test
    public void testUpdateSubtypeDumbBatchUsesSelfGuardedChildDml() {
        connectAndExpect(
                con -> {
                    Organization accepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setTaxCode("GLOBEX-DUMB-UPDATE");
                    });
                    Organization rejected = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                    });
                    getSqlClient(it -> it.setDialect(new H2Dialect() {
                        @Override
                        public boolean isBatchDumb() {
                            return true;
                        }
                    }))
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(accepted, rejected))
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setDumbBatchAcceptable(true)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT " +
                                        "where JOINED_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                        );
                        it.batchVariables(0, "GLOBEX-DUMB-UPDATE", 200L, 200L, "ORG");
                        it.batchVariables(1, "SHOULD-NOT-WRITE", 201L, 201L, "ORG");
                    });
                    ctx.value("[ORG, Globex, GLOBEX-DUMB-UPDATE, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testUpdateSubtypeDumbBatchUsesOneByOneRootAcceptanceWhenDownstreamIsLoaded() {
        connectAndExpect(
                con -> {
                    Organization accepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setTaxCode("GLOBEX-DUMB-DOWNSTREAM");
                        organization.addIntoProjects(project -> {
                            project.setId(2313L);
                            project.setName("Accepted dumb project");
                        });
                    });
                    Organization rejected = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2314L);
                            project.setName("Rejected dumb project");
                        });
                    });
                    getSqlClient(it -> it.setDialect(new H2Dialect() {
                        @Override
                        public boolean isBatchDumb() {
                            return true;
                        }
                    }))
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(accepted, rejected))
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2313L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2314L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(201L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT " +
                                        "where JOINED_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                        );
                        it.variables("GLOBEX-DUMB-DOWNSTREAM", 200L, 200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2313L, "Accepted dumb project", 200L);
                    });
                    ctx.value("[ORG, Globex, GLOBEX-DUMB-DOWNSTREAM, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]; " +
                            "200; null");
                }
        );
    }

    @Test
    public void testUpsertSubtypeDumbBatchUsesOneByOneRootAcceptance() {
        connectAndExpect(
                con -> {
                    Organization accepted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setName("Globex Dumb Upsert");
                        organization.setTaxCode("GLOBEX-DUMB-UPSERT");
                    });
                    Organization rejected = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                    });
                    getSqlClient(it -> it.setDialect(new H2Dialect() {
                        @Override
                        public boolean isBatchDumb() {
                            return true;
                        }
                    }))
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(accepted, rejected))
                            .execute(con);
                    return joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(200L, "ORG", "Globex Dumb Upsert");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(201L, "ORG", "Should not update");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "key(ID) values(?, ?)"
                        );
                        it.variables(200L, "GLOBEX-DUMB-UPSERT");
                    });
                    ctx.value("[ORG, Globex Dumb Upsert, GLOBEX-DUMB-UPSERT, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testInsertIfAbsentSubtypeBatchRoutesOnlyInsertedRows() {
        connectAndExpect(
                con -> {
                    Organization inserted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(300L);
                        organization.setName("New Batch Org");
                        organization.setTaxCode("NEW-BATCH-001");
                        organization.addIntoProjects(project -> {
                            project.setId(2307L);
                            project.setName("Inserted batch project");
                        });
                    });
                    Organization existingSame = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2308L);
                            project.setName("Rejected same batch project");
                        });
                    });
                    Organization existingDifferent = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2309L);
                            project.setName("Rejected different batch project");
                        });
                    });
                    getSqlClient()
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(inserted, existingSame, existingDifferent))
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 300L) +
                            "; " +
                            joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2307L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2308L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2309L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.batchVariables(0, 300L, "ORG", "New Batch Org");
                        it.batchVariables(1, 200L, "ORG", "Should not update");
                        it.batchVariables(2, 201L, "ORG", "Should not update");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(300L, "NEW-BATCH-001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2307L, "Inserted batch project", 300L);
                    });
                    ctx.value("[ORG, New Batch Org, NEW-BATCH-001, null, null]; " +
                            "[ORG, Globex, GLOBEX-001, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]; " +
                            "300; null; null");
                }
        );
    }

    @Test
    public void testInsertIfAbsentSubtypeDumbBatchUsesOneByOneRootAcceptance() {
        connectAndExpect(
                con -> {
                    Organization inserted = OrganizationDraft.$.produce(organization -> {
                        organization.setId(301L);
                        organization.setName("New Dumb Org");
                        organization.setTaxCode("NEW-DUMB-001");
                        organization.addIntoProjects(project -> {
                            project.setId(2310L);
                            project.setName("Inserted dumb project");
                        });
                    });
                    Organization existingSame = OrganizationDraft.$.produce(organization -> {
                        organization.setId(200L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2311L);
                            project.setName("Rejected same dumb project");
                        });
                    });
                    Organization existingDifferent = OrganizationDraft.$.produce(organization -> {
                        organization.setId(201L);
                        organization.setName("Should not update");
                        organization.setTaxCode("SHOULD-NOT-WRITE");
                        organization.addIntoProjects(project -> {
                            project.setId(2312L);
                            project.setName("Rejected different dumb project");
                        });
                    });
                    getSqlClient(it -> it.setDialect(new H2Dialect() {
                        @Override
                        public boolean isBatchDumb() {
                            return true;
                        }
                    }))
                            .getEntities()
                            .saveEntitiesCommand(Arrays.asList(inserted, existingSame, existingDifferent))
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .setAssociatedMode(OrganizationProps.PROJECTS, AssociatedSaveMode.APPEND)
                            .execute(con);
                    return joinedClientRow(con, 301L) +
                            "; " +
                            joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2310L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2311L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2312L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(301L, "ORG", "New Dumb Org");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(200L, "ORG", "Should not update");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(201L, "ORG", "Should not update");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(301L, "NEW-DUMB-001");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_ORG_PROJECT(ID, NAME, ORGANIZATION_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(2310L, "Inserted dumb project", 301L);
                    });
                    ctx.value("[ORG, New Dumb Org, NEW-DUMB-001, null, null]; " +
                            "[ORG, Globex, GLOBEX-001, null, null]; " +
                            "[Person, Alice, null, Alice, Smith]; " +
                            "301; null; null");
                }
        );
    }

    @Test
    public void testDeleteSubtype() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 202L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return joinedClientRow(con, 202L) + "; " + joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_ORG_PROJECT tb_1_ " +
                                        "where tb_1_.ORGANIZATION_ID = ? limit ?"
                        );
                        it.variables(202L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(202L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_CLIENT_PROJECT tb_1_ " +
                                        "where tb_1_.CLIENT_ID = ? limit ?"
                        );
                        it.variables(202L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(202L, "ORG");
                    });
                    ctx.value("null; [Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testBatchDeleteSubtype() {
        connectAndExpect(
                con -> {
                    int affectedRowCount = getSqlClient()
                            .getEntities()
                            .deleteAllCommand(Organization.class, Arrays.asList(202L, 201L))
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    return affectedRowCount +
                            "; " +
                            joinedClientRow(con, 202L) +
                            "; " +
                            joinedClientRow(con, 201L) +
                            "; " +
                            joinedClientProjectTargetId(con, 2002L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_ORG_PROJECT tb_1_ " +
                                        "where tb_1_.ORGANIZATION_ID in (?, ?) limit ?"
                        );
                        it.variables(202L, 201L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.batchVariables(0, 202L);
                        it.batchVariables(1, 201L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_CLIENT_PROJECT tb_1_ " +
                                        "where tb_1_.CLIENT_ID = ? limit ?"
                        );
                        it.variables(202L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(202L, "ORG");
                    });
                    ctx.value("1; null; [Person, Alice, null, Alice, Smith]; 201");
                }
        );
    }

    @Test
    public void testDeleteSubtypeWithAssociationTargets() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 200L)
                            .setMode(DeleteMode.PHYSICAL)
                            .setDissociateAction(ClientProjectProps.CLIENT, DissociateAction.SET_NULL)
                            .setDissociateAction(OrganizationProjectProps.ORGANIZATION, DissociateAction.SET_NULL)
                            .execute(con);
                    return joinedClientProjectTargetId(con, 2000L) +
                            "; " +
                            joinedOrgProjectTargetId(con, 2001L) +
                            "; " +
                            joinedClientRow(con, 200L) +
                            "; " +
                            joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update JOINED_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("update JOINED_CLIENT_PROJECT set CLIENT_ID = null where CLIENT_ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(200L, "ORG");
                    });
                    ctx.value("null; null; null; [Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testDeleteSubtypeWithMismatchedDiscriminator() {
        connectAndExpect(
                con -> {
                    int affectedRowCount = getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 201L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    return affectedRowCount + "; " + joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_ORG_PROJECT tb_1_ " +
                                        "where tb_1_.ORGANIZATION_ID = ? limit ?"
                        );
                        it.variables(201L, 1);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(201L);
                    });
                    ctx.value("0; [Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testDeleteSubtypeWithMismatchedDiscriminatorDoesNotCleanBaseAssociations() {
        connectAndExpect(
                con -> {
                    int affectedRowCount = getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 201L)
                            .setMode(DeleteMode.PHYSICAL)
                            .setDissociateAction(ClientProjectProps.CLIENT, DissociateAction.SET_NULL)
                            .setDissociateAction(OrganizationProjectProps.ORGANIZATION, DissociateAction.SET_NULL)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    return affectedRowCount +
                            "; " +
                            joinedClientProjectTargetId(con, 2002L) +
                            "; " +
                            joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update JOINED_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?");
                        it.variables(201L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(201L);
                    });
                    ctx.value("0; 201; [Person, Alice, null, Alice, Smith]");
                }
        );
    }

    @Test
    public void testDeleteRoot() {
        connectAndExpect(
                con -> {
                    ExecutionException ex = assertThrows(
                            ExecutionException.class,
                            () -> getSqlClient()
                                    .getEntities()
                                    .deleteCommand(Client.class, 200L)
                                    .setMode(DeleteMode.PHYSICAL)
                                    .execute(con)
                    );
                    return ex.getMessage();
                },
                ctx -> {
                    ctx.value(
                            "Cannot delete inheritance entity type " +
                                    "\"org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client\" " +
                                    "exactly because it is abstract. Delete an instantiable subtype or enable polymorphic delete."
                    );
                }
        );
    }

    @Test
    public void testDeleteRootPolymorphically() {
        connectAndExpect(
                con -> {
                    ExecutionException ex = assertThrows(
                            ExecutionException.class,
                            () -> getSqlClient()
                                    .getEntities()
                                    .deleteCommand(Client.class, 200L)
                                    .setMode(DeleteMode.PHYSICAL)
                                    .setPolymorphic()
                                    .execute(con)
                    );
                    return ex.getMessage();
                },
                ctx -> {
                    ctx.value(
                            "Cannot physically delete joined inheritance rows polymorphically by type " +
                                    "\"org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client\" " +
                                    "when joinedTableDissociateAction is \"DELETE\". Delete exact concrete subtypes, " +
                                    "use joinedTableDissociateAction = LAX, or explicitly select concrete rows " +
                                    "and delete them as exact concrete subtypes."
                    );
                }
        );
    }
}
