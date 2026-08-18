package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.logical.joinedtable.Client;
import org.babyfish.jimmer.sql.model.inheritance.logical.joinedtable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.logical.joinedtable.OrganizationDraft;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinedInheritanceLogicalDeleteTest extends AbstractMutationTest {

    @Test
    public void testInsertDerivedType() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(502L);
                                    organization.setName("Logical New Org");
                                    organization.setTaxCode("L-NEW-001");
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into LOGICAL_JOINED_CLIENT(ID, CLIENT_TYPE, NAME, DELETED) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables(502L, "ORG", "Logical New Org", false);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into LOGICAL_JOINED_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(502L, "L-NEW-001");
                    });
                    ctx.rowCount(AffectedTable.of(Client.class), 1);
                    ctx.rowCount(AffectedTable.of(Organization.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":502,\"name\":\"Logical New Org\",\"taxCode\":\"L-NEW-001\"}");
                        it.modified(
                                "{\"id\":502,\"name\":\"Logical New Org\"," +
                                        "\"deleted\":false,\"taxCode\":\"L-NEW-001\"}"
                        );
                    });
                }
        );
    }

    @Test
    public void testLogicalDeleteDerivedType() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 500L)
                            .execute(con);
                    return joinedClientRow(con, 500L) + "; " + joinedClientRow(con, 501L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update LOGICAL_JOINED_CLIENT set DELETED = ? where ID = ? and CLIENT_TYPE = ?");
                        it.variables(true, 500L, "ORG");
                    });
                    ctx.value("[ORG, Logical Globex, L-GLOBEX-001, null, null, true]; " +
                            "[Person, Logical Alice, null, Alice, Smith, false]");
                }
        );
    }

    private static String joinedClientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME, c.DELETED " +
                        "from LOGICAL_JOINED_CLIENT c " +
                        "left join LOGICAL_JOINED_ORGANIZATION o on c.ID = o.ID " +
                        "left join LOGICAL_JOINED_PERSON p on c.ID = p.ID " +
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
                        ", " +
                        rs.getBoolean(6) +
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
