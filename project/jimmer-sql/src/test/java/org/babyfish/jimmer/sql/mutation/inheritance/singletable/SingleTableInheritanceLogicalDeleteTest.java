package org.babyfish.jimmer.sql.mutation.inheritance.singletable;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.logical.singletable.Organization;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SingleTableInheritanceLogicalDeleteTest extends AbstractMutationTest {

    @Test
    public void testLogicalDeleteDerivedType() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 400L)
                            .execute(con);
                    return clientRow(con, 400L) + "; " + clientRow(con, 401L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update LOGICAL_CLIENT set DELETED = ? where ID = ? and CLIENT_TYPE = ?");
                        it.variables(true, 400L, "ORG");
                    });
                    ctx.value("[ORG, Logical Acme, L-ACME-001, null, null, true]; " +
                            "[Person, Logical Bob, null, Bob, Brown, false]");
                }
        );
    }

    private static String clientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_TYPE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME, DELETED " +
                        "from LOGICAL_CLIENT where ID = ?"
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
