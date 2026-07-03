package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable.instantiable;

import org.babyfish.jimmer.sql.ast.TypeMatchMode;

import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.Client;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.ClientDraft;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.ClientTable;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.dto.InstantiableClientDefaultInput;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void testUpdateRootBranchPolymorphically() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientDraft.$.produce(client -> {
                                        client.setId(601L);
                                        client.setName("Joined Root Polymorphic+");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setTypeMatchMode(TypeMatchMode.POLYMORPHIC)
                            .execute(con);
                    return joinedClientRow(con, 601L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_INST_CLIENT " +
                                        "set NAME = ? " +
                                        "where ID = ?"
                        );
                        it.variables("Joined Root Polymorphic+", 601L);
                    });
                    ctx.value("[ORG, Joined Root Polymorphic+, J-ORG-001, null, null]");
                }
        );
    }

    @Test
    public void testCreateUpdateRootBranchExactlyByAutoTypeMatchMode() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(ClientTable.class, (u, client) -> {
                    u.set(client.name(), "Joined Root Exact+");
                    u.where(client.id().in(Arrays.asList(600L, 601L)));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_INST_CLIENT tb_1_ " +
                                        "set NAME = ? " +
                                        "where tb_1_.ID in (?, ?) and tb_1_.CLIENT_TYPE = ?"
                        );
                        it.variables("Joined Root Exact+", 600L, 601L, "CLIENT");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testCreateUpdateRootBranchPolymorphically() {
        executeAndExpectRowCount(
                getLambdaClient().createUpdate(ClientTable.class, (u, client) -> {
                    u.setTypeMatchMode(TypeMatchMode.POLYMORPHIC);
                    u.set(client.name(), "Joined Root Polymorphic+");
                    u.where(client.id().in(Arrays.asList(600L, 601L)));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_INST_CLIENT tb_1_ " +
                                        "set NAME = ? " +
                                        "where tb_1_.ID in (?, ?)"
                        );
                        it.variables("Joined Root Polymorphic+", 600L, 601L);
                    });
                    ctx.rowCount(2);
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
    public void testUpsertRootBranchPolymorphically() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    ClientDraft.$.produce(client -> {
                                        client.setId(601L);
                                        client.setName("Joined Root Polymorphic+");
                                    })
                            )
                            .setTypeMatchMode(TypeMatchMode.POLYMORPHIC)
                            .execute(con);
                    return joinedClientRow(con, 601L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_INST_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                        it.variables(601L, "CLIENT", "Joined Root Polymorphic+");
                    });
                    ctx.value("[ORG, Joined Root Polymorphic+, J-ORG-001, null, null]");
                }
        );
    }

    @Test
    public void testUpdateRootBranchWithTypeChangeAllowed() {
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
                            .setTypeChangeAllowed(true)
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
    public void testUpdateRootBranchWithPolymorphicTypeMatchAndTypeChangeAllowedIsRejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient()
                        .getEntities()
                        .saveCommand(
                                ClientDraft.$.produce(client -> {
                                    client.setId(601L);
                                    client.setName("Joined Root Replacement");
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setTypeMatchMode(TypeMatchMode.POLYMORPHIC)
                        .setTypeChangeAllowed(true)
                        .execute()
        );
        assertEquals(
                "Cannot save inheritance entity type " +
                        "\"org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.Client\" " +
                        "with POLYMORPHIC type match mode because typeChangeAllowed is true",
                ex.getMessage()
        );
    }

    @Test
    public void testUpsertRootBranchWithTypeChangeAllowed() {
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
                            .setTypeChangeAllowed(true)
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
    public void testDeleteRootPolymorphically() {
        connectAndExpect(
                con -> {
                    int affectedRowCount = getSqlClient()
                            .getEntities()
                            .deleteCommand(Client.class, 600L)
                            .setMode(DeleteMode.PHYSICAL)
                            .setTypeMatchMode(TypeMatchMode.POLYMORPHIC)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    return affectedRowCount + "; " + joinedClientRow(con, 600L) + "; " + joinedClientRow(con, 601L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.RESOLVE_ACCEPTED_INHERITANCE_DELETE_TARGETS);
                        it.sql(
                                "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                                        "from JOINED_INST_CLIENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE in (?, ?, ?)"
                        );
                        it.variables(600L, "CLIENT", "ORG", "Person");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_INST_CLIENT where ID = ? and CLIENT_TYPE in (?, ?, ?)");
                        it.variables(600L, "CLIENT", "ORG", "Person");
                    });
                    ctx.value("1; null; [ORG, Joined Inst Org, J-ORG-001, null, null]");
                }
        );
    }

    @Test
    public void testCreateDeleteRootPolymorphicallyUsesAcceptedTargetFallback() {
        executeAndExpectRowCount(
                getLambdaClient().createDelete(ClientTable.class, (d, client) -> {
                    d.setMode(DeleteMode.PHYSICAL);
                    d.setTypeMatchMode(TypeMatchMode.POLYMORPHIC);
                    d.where(client.id().eq(601L));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select distinct tb_1_.ID " +
                                        "from JOINED_INST_CLIENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE in (?, ?, ?)"
                        );
                        it.variables(601L, "CLIENT", "ORG", "Person");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.RESOLVE_ACCEPTED_INHERITANCE_DELETE_TARGETS);
                        it.sql(
                                "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                                        "from JOINED_INST_CLIENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE in (?, ?, ?)"
                        );
                        it.variables(601L, "CLIENT", "ORG", "Person");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_INST_ORGANIZATION where ID = ?");
                        it.variables(601L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from JOINED_INST_CLIENT where ID = ? and CLIENT_TYPE in (?, ?, ?)");
                        it.variables(601L, "CLIENT", "ORG", "Person");
                    });
                    ctx.rowCount(1);
                }
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
