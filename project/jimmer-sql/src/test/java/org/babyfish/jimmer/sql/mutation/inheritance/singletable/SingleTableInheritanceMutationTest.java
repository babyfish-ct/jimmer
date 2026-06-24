package org.babyfish.jimmer.sql.mutation.inheritance.singletable;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.key.NaturalOrganizationDraft;
import org.babyfish.jimmer.sql.model.inheritance.singletable.*;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SingleTableInheritanceMutationTest extends AbstractMutationTest {

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
                                "insert into CLIENT(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables(300L, "ORG", "New Org", "NEW-001");
                    });
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
                                "merge into CLIENT(ID, CLIENT_TYPE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?, ?, null, null)"
                        );
                        it.variables(300L, "ORG", "New Org", "NEW-001");
                    });
                    ctx.rowCount(AffectedTable.of(Organization.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                        it.modified("{\"id\":300,\"name\":\"New Org\",\"taxCode\":\"NEW-001\"}");
                    });
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
                                        organization.setId(100L);
                                        organization.setName("Acme+");
                                        organization.setTaxCode("ACME-002");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update CLIENT " +
                                        "set CLIENT_TYPE = ?, FIRST_NAME = null, LAST_NAME = null, NAME = ?, TAX_CODE = ? " +
                                        "where ID = ?"
                        );
                        it.variables("ORG", "Acme+", "ACME-002", 100L);
                    });
                    ctx.value("[ORG, Acme+, ACME-002, null, null]");
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
                                        person.setId(100L);
                                        person.setName("Acme Person");
                                        person.setFirstName("Ann");
                                        person.setLastName("Smith");
                                    })
                            )
                            .execute(con);
                    return clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into CLIENT(ID, CLIENT_TYPE, NAME, FIRST_NAME, LAST_NAME, TAX_CODE) " +
                                        "key(ID) values(?, ?, ?, ?, ?, null)"
                        );
                        it.variables(100L, "Person", "Acme Person", "Ann", "Smith");
                    });
                    ctx.value("[Person, Acme Person, null, Ann, Smith]");
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
                                        person.setId(100L);
                                        person.setName("Acme Person");
                                        person.setFirstName("Ann");
                                        person.setLastName("Smith");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update CLIENT " +
                                        "set CLIENT_TYPE = ?, TAX_CODE = null, NAME = ?, FIRST_NAME = ?, LAST_NAME = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Person", "Acme Person", "Ann", "Smith", 100L);
                    });
                    ctx.value("[Person, Acme Person, null, Ann, Smith]");
                }
        );
    }

    @Test
    public void testUpdateByDiscriminatorKey() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    NaturalOrganizationDraft.$.produce(organization -> {
                                        organization.setCode("same-code");
                                        organization.setName("Acme Natural+");
                                        organization.setTaxCode("ACME-N-002");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return naturalClientRows(con);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update NATURAL_CLIENT " +
                                        "set CLIENT_TYPE = ?, FIRST_NAME = null, LAST_NAME = null, NAME = ?, TAX_CODE = ? " +
                                        "where CLIENT_TYPE = ? and CODE = ?"
                        );
                        it.variables("ORG", "Acme Natural+", "ACME-N-002", "ORG", "same-code");
                    });
                    ctx.value("[300, ORG, same-code, Acme Natural+, ACME-N-002, null, null]; " +
                            "[301, NaturalPerson, same-code, Bob Natural, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testDeleteSubtype() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 100L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return clientRow(con, 100L) + "; " + clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(100L, "ORG");
                    });
                    ctx.value("null; [Person, Bob, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testDeleteSubtypeWithAssociationTargets() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 100L)
                            .setMode(DeleteMode.PHYSICAL)
                            .setDissociateAction(ClientProjectProps.CLIENT, DissociateAction.SET_NULL)
                            .setDissociateAction(OrganizationProjectProps.ORGANIZATION, DissociateAction.SET_NULL)
                            .execute(con);
                    return singleClientProjectTargetId(con, 1000L) +
                            "; " +
                            singleOrgProjectTargetId(con, 1001L) +
                            "; " +
                            clientRow(con, 100L) +
                            "; " +
                            clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update SINGLE_CLIENT_PROJECT set CLIENT_ID = null where CLIENT_ID = ?");
                        it.variables(100L);
                    });
                    ctx.statement(it -> {
                        it.sql("update SINGLE_ORG_PROJECT set ORGANIZATION_ID = null where ORGANIZATION_ID = ?");
                        it.variables(100L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(100L, "ORG");
                    });
                    ctx.value("null; null; null; [Person, Bob, null, Bob, Brown]");
                }
        );
    }

    private static String clientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_TYPE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME from CLIENT where ID = ?"
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

    private static String naturalClientRows(Connection con) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select ID, CLIENT_TYPE, CODE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME " +
                        "from NATURAL_CLIENT where CODE = 'same-code' order by ID"
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                StringBuilder builder = new StringBuilder();
                while (rs.next()) {
                    if (builder.length() != 0) {
                        builder.append("; ");
                    }
                    builder
                            .append('[')
                            .append(rs.getLong(1))
                            .append(", ")
                            .append(rs.getString(2))
                            .append(", ")
                            .append(rs.getString(3))
                            .append(", ")
                            .append(rs.getString(4))
                            .append(", ")
                            .append(rs.getString(5))
                            .append(", ")
                            .append(rs.getString(6))
                            .append(", ")
                            .append(rs.getString(7))
                            .append(']');
                }
                return builder.toString();
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String singleClientProjectTargetId(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_ID from SINGLE_CLIENT_PROJECT where ID = ?"
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

    private static String singleOrgProjectTargetId(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select ORGANIZATION_ID from SINGLE_ORG_PROJECT where ID = ?"
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
}
