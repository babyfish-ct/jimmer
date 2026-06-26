package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
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
	                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID for update");
	                        it.variables(200L);
	                    });
	                    ctx.statement(it -> {
	                        it.sql(
	                                "merge into JOINED_CLIENT(ID, CLIENT_TYPE, NAME) " +
	                                        "key(ID) values(?, ?, ?)"
                        );
                        it.variables(200L, "Person", "Globex Person");
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
	                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID for update");
	                        it.variables(200L);
	                    });
	                    ctx.statement(it -> {
	                        it.sql(
	                                "update JOINED_CLIENT " +
	                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Person", "Globex Person", 200L);
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
                    ctx.entity(it -> {});
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
                    ctx.entity(it -> {});
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
	                                        "from JOINED_CLIENT_PROJECT tb_1_ " +
	                                        "where tb_1_.CLIENT_ID = ? limit ?"
	                        );
	                        it.variables(202L, 1);
	                    });
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
	                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?");
	                        it.variables(202L, "ORG");
	                    });
                    ctx.value("null; [Person, Alice, null, Alice, Smith]");
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
                        it.sql("update JOINED_CLIENT_PROJECT set CLIENT_ID = null where CLIENT_ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("update JOINED_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?");
                        it.variables(200L);
                    });
	                    ctx.statement(it -> {
	                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
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
	                                        "from JOINED_CLIENT_PROJECT tb_1_ " +
	                                        "where tb_1_.CLIENT_ID = ? limit ?"
	                        );
	                        it.variables(201L, 1);
	                    });
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
	                    ctx.statement(it -> {
	                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?");
	                        it.variables(201L, "ORG");
	                    });
	                    ctx.value("0; [Person, Alice, null, Alice, Smith]");
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
	                            "Cannot physically delete joined inheritance rows by root/base type " +
	                                    "\"org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client\" " +
	                                    "when joinedTableDeleteMode is \"EXPLICIT\". Delete concrete subtypes, " +
	                                    "use joinedTableDeleteMode = DB_CASCADE, or explicitly select/lock concrete rows " +
	                                    "and delete them as concrete subtypes."
	                    );
	                }
	        );
    }
}
