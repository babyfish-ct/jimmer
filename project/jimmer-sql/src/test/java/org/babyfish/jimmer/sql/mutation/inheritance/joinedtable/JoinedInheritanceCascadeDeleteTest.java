package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.TypeMatchMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade.Client;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade.Organization;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade.OrganizationTable;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinedInheritanceCascadeDeleteTest extends AbstractMutationTest {

    private static String joinedClientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                        "from JOINED_CASCADE_CLIENT c " +
                        "left join JOINED_CASCADE_ORGANIZATION o on c.ID = o.ID " +
                        "left join JOINED_CASCADE_PERSON p on c.ID = p.ID " +
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

    private static Long joinedCascadeProjectTargetId(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_ID from JOINED_CASCADE_PROJECT where ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getObject(1, Long.class);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testDeleteDerivedTypeByDatabaseCascade() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Organization.class, 500L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return joinedClientRow(con, 500L) +
                            "; " +
                            joinedClientRow(con, 501L) +
                            "; " +
                            joinedCascadeProjectTargetId(con, 5000L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CASCADE_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(500L, "ORG");
                    });
                    ctx.value("null; [Person, Cascade Alice, null, Alice, Smith]; null");
                }
        );
    }

    @Test
    public void testCreateDeleteDerivedTypeByDatabaseCascade() {
        connectAndExpect(
                con -> {
                    int affectedRowCount = getLambdaClient().createDelete(OrganizationTable.class, (d, organization) -> {
                        d.setMode(DeleteMode.PHYSICAL);
                        d.where(organization.id().eq(500L));
                    }).execute(con);
                    return affectedRowCount +
                            "; " +
                            joinedClientRow(con, 500L) +
                            "; " +
                            joinedClientRow(con, 501L) +
                            "; " +
                            joinedCascadeProjectTargetId(con, 5000L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select distinct tb_1_.ID " +
                                        "from JOINED_CASCADE_CLIENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables(500L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CASCADE_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(500L, "ORG");
                    });
                    ctx.value("1; null; [Person, Cascade Alice, null, Alice, Smith]; null");
                }
        );
    }

    @Test
    public void testDeleteRootByDatabaseCascade() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Client.class, 500L)
                            .setMode(DeleteMode.PHYSICAL)
                            .setTypeMatchMode(TypeMatchMode.POLYMORPHIC)
                            .execute(con);
                    return joinedClientRow(con, 500L) + "; " + joinedClientRow(con, 501L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_CASCADE_CLIENT where ID = ? and CLIENT_TYPE in (?, ?)");
                        it.variables(500L, "ORG", "Person");
                    });
                    ctx.value("null; [Person, Cascade Alice, null, Alice, Smith]");
                }
        );
    }
}
