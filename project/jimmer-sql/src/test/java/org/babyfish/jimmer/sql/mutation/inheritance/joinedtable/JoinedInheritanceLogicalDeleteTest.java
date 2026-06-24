package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.logical.joinedtable.Organization;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinedInheritanceLogicalDeleteTest extends AbstractMutationTest {

    @Test
    public void testLogicalDeleteSubtype() {
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
