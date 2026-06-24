package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.*;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
                                "merge into JOINED_CLIENT(ID, CLIENT_TYPE, NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.variables(300L, "ORG", "New Org");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_PERSON where ID = ?");
                        it.variables(300L);
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
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
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
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
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
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ?"
                        );
                        it.variables("ORG", "Globex+", 200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_PERSON where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "key(ID) values(?, ?)"
                        );
                        it.variables(200L, "GLOBEX-002");
                    });
                    ctx.value("[ORG, Globex+, GLOBEX-002, null, null]");
                }
        );
    }

    @Test
    public void testDeleteSubtype() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 200L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ? order by ID for update");
                        it.variables(200L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(200L, "ORG");
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
                        it.sql(
                                "select ID " +
                                        "from JOINED_CLIENT " +
                                        "where ID = ? and CLIENT_TYPE = ? " +
                                        "order by ID for update"
                        );
                        it.variables(200L, "ORG");
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
                        it.sql("select ID from JOINED_CLIENT where ID = ? and CLIENT_TYPE = ? order by ID for update");
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
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Client.class, 200L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return joinedClientRow(con, 200L) + "; " + joinedClientRow(con, 201L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CLIENT where ID = ? order by ID for update");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_ORGANIZATION where ID = ?");
                        it.variables(200L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CLIENT where ID = ?");
                        it.variables(200L);
                    });
                    ctx.value("null; [Person, Alice, null, Alice, Smith]");
                }
        );
    }
}
