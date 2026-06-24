package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationDraft;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.PersonDraft;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinedInheritanceMutationTest extends AbstractMutationTest {

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
}
