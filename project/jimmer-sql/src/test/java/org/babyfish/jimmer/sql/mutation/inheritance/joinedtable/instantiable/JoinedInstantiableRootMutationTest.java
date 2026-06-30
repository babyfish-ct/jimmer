package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable.instantiable;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.Client;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.ClientDraft;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.dto.InstantiableClientDefaultInput;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class JoinedInstantiableRootMutationTest extends AbstractMutationTest {

    @Test
    public void testInsertRootBranch() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                ClientDraft.$.produce(client -> {
                                    client.setId(610L);
                                    client.setName("Joined Root New");
                                })
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_INST_CLIENT(ID, CLIENT_TYPE, NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(610L, "CLIENT", "Joined Root New");
                    });
                    ctx.rowCount(AffectedTable.of(Client.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":610,\"name\":\"Joined Root New\"}");
                        it.modified("{\"id\":610,\"name\":\"Joined Root New\"}");
                    });
                }
        );
    }

    @Test
    public void testInsertRootBranchByDefaultInput() {
        InstantiableClientDefaultInput.Base input = new InstantiableClientDefaultInput.Base();
        input.setId(611L);
        input.setName("Joined Root Input");
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(input)
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_INST_CLIENT(ID, CLIENT_TYPE, NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.variables(611L, "CLIENT", "Joined Root Input");
                    });
                    ctx.rowCount(AffectedTable.of(Client.class), 1);
                    ctx.entity(it -> {
                        it.original("{\"id\":611,\"name\":\"Joined Root Input\"}");
                        it.modified("{\"id\":611,\"name\":\"Joined Root Input\"}");
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
                                    ClientDraft.$.produce(client -> {
                                        client.setId(600L);
                                        client.setName("Joined Root+");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedClientRow(con, 600L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_INST_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Joined Root+", 600L, "CLIENT");
                    });
                    ctx.value("[CLIENT, Joined Root+, null, null, null]");
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
                                    ClientDraft.$.produce(client -> {
                                        client.setId(601L);
                                        client.setName("Should not update");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedClientRow(con, 601L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_INST_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Should not update", 601L, "CLIENT");
                    });
                    ctx.value("[ORG, Joined Inst Org, J-ORG-001, null, null]");
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
                                    ClientDraft.$.produce(client -> {
                                        client.setId(601L);
                                        client.setName("Should not update");
                                    })
                            )
                            .execute(con);
                    return joinedClientRow(con, 601L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_INST_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(601L, "CLIENT", "Should not update");
                    });
                    ctx.value("[ORG, Joined Inst Org, J-ORG-001, null, null]");
                }
        );
    }

    @Test
    public void testUpdateRootBranchWithSubtypeChangeAllowed() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientDraft.$.produce(client -> {
                                        client.setId(601L);
                                        client.setName("Joined Root Replacement");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 601L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_INST_CLIENT where ID = ? order by ID");
                        it.variables(601L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_INST_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("CLIENT", "Joined Root Replacement", 601L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_INST_ORGANIZATION where ID = ?");
                        it.variables(601L);
                    });
                    ctx.value("[CLIENT, Joined Root Replacement, null, null, null]");
                }
        );
    }

    @Test
    public void testUpsertRootBranchWithSubtypeChangeAllowed() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientDraft.$.produce(client -> {
                                        client.setId(601L);
                                        client.setName("Joined Root Replacement");
                                    })
                            )
                            .setSubtypeChangeAllowed(true)
                            .execute(con);
                    return joinedClientRow(con, 601L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_INST_CLIENT where ID = ? order by ID");
                        it.variables(601L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_INST_CLIENT " +
                                        "set CLIENT_TYPE = ?, NAME = ? " +
                                        "where ID = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("CLIENT", "Joined Root Replacement", 601L, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_INST_ORGANIZATION where ID = ?");
                        it.variables(601L);
                    });
                    ctx.value("[CLIENT, Joined Root Replacement, null, null, null]");
                }
        );
    }

    @Test
    public void testDeleteRootBranchExactly() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .deleteCommand(Client.class, 600L)
                            .setMode(DeleteMode.PHYSICAL)
                            .execute(con);
                    return joinedClientRow(con, 600L) + "; " + joinedClientRow(con, 601L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_INST_CLIENT where ID = ? and CLIENT_TYPE = ?");
                        it.variables(600L, "CLIENT");
                    });
                    ctx.value("null; [ORG, Joined Inst Org, J-ORG-001, null, null]");
                }
        );
    }

    @Test
    public void testDeleteRootPolymorphicallyIsRejectedByExplicitJoinedDeleteMode() {
        connectAndExpect(
                con -> {
                    ExecutionException ex = assertThrows(
                            ExecutionException.class,
                            () -> getSqlClient()
                                    .getEntities()
                                    .deleteCommand(Client.class, 600L)
                                    .setMode(DeleteMode.PHYSICAL)
                                    .setPolymorphic()
                                    .execute(con)
                    );
                    return ex.getMessage();
                },
                ctx -> ctx.value(
                        "Cannot physically delete joined inheritance rows polymorphically by type " +
                                "\"org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.Client\" " +
                                "when joinedTableDissociateAction is \"DELETE\". Delete exact concrete subtypes, " +
                                "use joinedTableDissociateAction = LAX, or explicitly select concrete rows " +
                                "and delete them as exact concrete subtypes."
                )
        );
    }

    private static String joinedClientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                        "from JOINED_INST_CLIENT c " +
                        "left join JOINED_INST_ORGANIZATION o on c.ID = o.ID " +
                        "left join JOINED_INST_PERSON p on c.ID = p.ID " +
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
