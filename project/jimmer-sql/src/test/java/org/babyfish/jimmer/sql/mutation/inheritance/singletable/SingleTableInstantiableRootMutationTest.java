package org.babyfish.jimmer.sql.mutation.inheritance.singletable;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumClient;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumClientDraft;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SingleTableInstantiableRootMutationTest extends AbstractMutationTest {

    @Test
    public void testInsertRootBranch() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                EnumClientDraft.$.produce(client -> {
                                    client.setId(320L);
                                    client.setName("Enum Root New");
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ENUM_CLIENT(ID, CLIENT_TYPE, NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(320L, "CLIENT", "Enum Root New");
                    });
                    ctx.rowCount(AffectedTable.of(EnumClient.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":320,\"name\":\"Enum Root New\"}");
                        it.modified("{\"id\":320,\"name\":\"Enum Root New\"}");
                    });
                }
        );
    }

    @Test
    public void testUpdateRootBranch() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    EnumClientDraft.$.produce(client -> {
                                        client.setId(111L);
                                        client.setName("Enum Root+");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return enumClientRow(con, 111L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update ENUM_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Enum Root+", 111L, "CLIENT");
                    });
                    ctx.value("[CLIENT, Enum Root+, null]");
                }
        );
    }

    @Test
    public void testUpdateRootBranchWithMismatchedDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    EnumClientDraft.$.produce(client -> {
                                        client.setId(110L);
                                        client.setName("Should not update");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return enumClientRow(con, 110L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update ENUM_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Should not update", 110L, "CLIENT");
                    });
                    ctx.value("[ORG, Enum Acme, null]");
                }
        );
    }

    @Test
    public void testUpsertRootBranchWithMismatchedDiscriminator() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    EnumClientDraft.$.produce(client -> {
                                        client.setId(110L);
                                        client.setName("Should not update");
                                    })
                            )
                            .execute(con);
                    return enumClientRow(con, 110L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ENUM_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(110L, "CLIENT", "Should not update");
                    });
                    ctx.value("[ORG, Enum Acme, null]");
                }
        );
    }

    @Test
    public void testDeleteRootBranchExactly() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(EnumClient.class, 111L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return enumClientRow(con, 111L) + "; " + enumClientRow(con, 110L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from ENUM_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(111L, "CLIENT");
                    });
                    ctx.value("null; [ORG, Enum Acme, null]");
                }
        );
    }

    private static String enumClientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_TYPE, NAME, FIRST_NAME from ENUM_CLIENT where ID = ?"
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
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
