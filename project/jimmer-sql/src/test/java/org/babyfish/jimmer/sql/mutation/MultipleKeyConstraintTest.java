package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.impl.mutation.QueryReason;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.SysUser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class MultipleKeyConstraintTest extends AbstractMutationTest {

    @Test
    public void testUpdateByAccount() {
        List<SysUser> users = Arrays.asList(
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_001");
                    draft.setDescription("Description_A");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_002");
                    draft.setDescription("Description_B");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_003");
                    draft.setDescription("Description_C");
                })
        );
        executeAndExpectResult(
                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .getEntities()
                        .saveEntitiesCommand(users),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(ACCOUNT, DESCRIPTION) " +
                                        "key(ACCOUNT) " +
                                        "values(?, ?)"
                        );
                        it.batchVariables(0, "sysusr_001", "Description_A");
                        it.batchVariables(1, "sysusr_002", "Description_B");
                        it.batchVariables(2, "sysusr_003", "Description_C");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":1,\"account\":\"sysusr_001\",\"description\":\"Description_A\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":2,\"account\":\"sysusr_002\",\"description\":\"Description_B\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":3,\"account\":\"sysusr_003\",\"description\":\"Description_C\"}");
                    });
                }
        );
    }

    @Test
    public void testUpdateByEmail() {
        List<SysUser> users = Arrays.asList(
                Immutables.createSysUser(draft -> {
                    draft.setEmail("tom.cook@gmail.com");
                    draft.setDescription("Description_A");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setEmail("linda.white@gmail.com");
                    draft.setDescription("Description_B");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setEmail("alex.brown@gmail.com");
                    draft.setDescription("Description_C");
                })
        );
        executeAndExpectResult(
                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .getEntities()
                        .saveEntitiesCommand(users),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(EMAIL, DESCRIPTION) " +
                                        "key(EMAIL) " +
                                        "values(?, ?)"
                        );
                        it.batchVariables(0, "tom.cook@gmail.com", "Description_A");
                        it.batchVariables(1, "linda.white@gmail.com", "Description_B");
                        it.batchVariables(2, "alex.brown@gmail.com", "Description_C");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":1,\"email\":\"tom.cook@gmail.com\",\"description\":\"Description_A\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":2,\"email\":\"linda.white@gmail.com\",\"description\":\"Description_B\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":3,\"email\":\"alex.brown@gmail.com\",\"description\":\"Description_C\"}");
                    });
                }
        );
    }

    @Test
    public void testUpdateByAreaAndNickName() {
        List<SysUser> users = Arrays.asList(
                Immutables.createSysUser(draft -> {
                    draft.setArea("north");
                    draft.setNickName("Tom");
                    draft.setDescription("Description_A");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setArea("south");
                    draft.setNickName("Linda");
                    draft.setDescription("Description_B");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setArea("east");
                    draft.setNickName("Alex");
                    draft.setDescription("Description_C");
                })
        );
        executeAndExpectResult(
                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .getEntities()
                        .saveEntitiesCommand(users),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(AREA, NICK_NAME, DESCRIPTION) " +
                                        "key(AREA, NICK_NAME) " +
                                        "values(?, ?, ?)"
                        );
                        it.batchVariables(0, "north", "Tom", "Description_A");
                        it.batchVariables(1, "south", "Linda", "Description_B");
                        it.batchVariables(2, "east", "Alex", "Description_C");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":1,\"area\":\"north\",\"nickName\":\"Tom\",\"description\":\"Description_A\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":2,\"area\":\"south\",\"nickName\":\"Linda\",\"description\":\"Description_B\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":3,\"area\":\"east\",\"nickName\":\"Alex\",\"description\":\"Description_C\"}");
                    });
                }
        );
    }

    @Test
    public void testMixedUpdateStatements() {
        List<SysUser> users = Arrays.asList(
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_001");
                    draft.setDescription("Description_A");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setEmail("linda.white@gmail.com");
                    draft.setDescription("Description_B");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setArea("east");
                    draft.setNickName("Alex");
                    draft.setDescription("Description_C");
                })
        );
        executeAndExpectResult(
                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .getEntities()
                        .saveEntitiesCommand(users),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("merge into SYS_USER(ACCOUNT, DESCRIPTION) key(ACCOUNT) values(?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("merge into SYS_USER(EMAIL, DESCRIPTION) key(EMAIL) values(?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("merge into SYS_USER(AREA, NICK_NAME, DESCRIPTION) key(AREA, NICK_NAME) values(?, ?, ?)");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":1,\"account\":\"sysusr_001\",\"description\":\"Description_A\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":2,\"email\":\"linda.white@gmail.com\",\"description\":\"Description_B\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":3,\"area\":\"east\",\"nickName\":\"Alex\",\"description\":\"Description_C\"}");
                    });
                }
        );
    }

    @Test
    public void testMixedUpdateAndQueryStatements() {
        List<SysUser> users = Arrays.asList(
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_001");
                    draft.setDescription("Description_A");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setEmail("linda.white@gmail.com");
                    draft.setDescription("Description_B");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setArea("east");
                    draft.setNickName("Alex");
                    draft.setDescription("Description_C");
                })
        );
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                })
                        .getEntities()
                        .saveEntitiesCommand(users)
                        .setInvestigateKeyBasedUpdate(),
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.PREPARE_TO_INVESTIGATE_KEY_BASED_UPDATE);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.EMAIL, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.PREPARE_TO_INVESTIGATE_KEY_BASED_UPDATE);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.EMAIL, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.EMAIL = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.PREPARE_TO_INVESTIGATE_KEY_BASED_UPDATE);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.EMAIL, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where (tb_1_.AREA, tb_1_.NICK_NAME) = (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("update SYS_USER set DESCRIPTION = ? where ID = ?");
                        it.batchVariables(0, "Description_A", 1L);
                        it.batchVariables(1, "Description_B", 2L);
                        it.batchVariables(2, "Description_C", 3L);
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":1,\"account\":\"sysusr_001\",\"description\":\"Description_A\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":2,\"email\":\"linda.white@gmail.com\",\"description\":\"Description_B\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":3,\"area\":\"east\",\"nickName\":\"Alex\",\"description\":\"Description_C\"}");
                    });
                }
        );
    }

    @Test
    public void translateDuplicateAccount() {
        List<SysUser> users = Arrays.asList(
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_004");
                    draft.setEmail("ada.wang@umbrella.com");
                    draft.setArea("north");
                    draft.setNickName("Ada");
                    draft.setDescription("Description_D");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_001");
                    draft.setEmail("albert.wesker@umbrella.com");
                    draft.setArea("west");
                    draft.setNickName("Wesker");
                    draft.setDescription("Description_E");
                })
        );
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                })
                        .getEntities()
                        .saveEntitiesCommand(users)
                        .setInvestigateKeyBasedUpdate(),
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.PREPARE_TO_INVESTIGATE_KEY_BASED_UPDATE);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.EMAIL, tb_1_.AREA, tb_1_.NICK_NAME from SYS_USER tb_1_ where (tb_1_.AREA, tb_1_.NICK_NAME) in ((?, ?), (?, ?))"
                        );
                        it.variables("north", "Ada", "west", "Wesker");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION) values(?, ?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                "sysusr_004",
                                "ada.wang@umbrella.com",
                                "north",
                                "Ada",
                                "Description_D"
                        );
                        it.batchVariables(
                                1,
                                "sysusr_001",
                                "albert.wesker@umbrella.com",
                                "west",
                                "Wesker",
                                "Description_E"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = ?"
                        );
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the key property " +
                                        "\"[org.babyfish.jimmer.sql.model.SysUser.account]\" " +
                                        "is \"sysusr_001\" which already exists"
                        );
                    });
                }
        );
    }

    @Test
    public void translateDuplicateEmail() {
        List<SysUser> users = Arrays.asList(
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_004");
                    draft.setEmail("ada.wang@umbrella.com");
                    draft.setArea("north");
                    draft.setNickName("Ada");
                    draft.setDescription("Description_D");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_005");
                    draft.setEmail("linda.white@gmail.com");
                    draft.setArea("west");
                    draft.setNickName("Wesker");
                    draft.setDescription("Description_E");
                })
        );
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                })
                        .getEntities()
                        .saveEntitiesCommand(users)
                        .setInvestigateKeyBasedUpdate(),
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.PREPARE_TO_INVESTIGATE_KEY_BASED_UPDATE);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.EMAIL, tb_1_.AREA, tb_1_.NICK_NAME from SYS_USER tb_1_ where (tb_1_.AREA, tb_1_.NICK_NAME) in ((?, ?), (?, ?))"
                        );
                        it.variables("north", "Ada", "west", "Wesker");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION) values(?, ?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                "sysusr_004",
                                "ada.wang@umbrella.com",
                                "north",
                                "Ada",
                                "Description_D"
                        );
                        it.batchVariables(
                                1,
                                "sysusr_005",
                                "linda.white@gmail.com",
                                "west",
                                "Wesker",
                                "Description_E"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.EMAIL = ?"
                        );
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, " +
                                        "the value of the key property \"[" +
                                        "org.babyfish.jimmer.sql.model.SysUser.email" +
                                        "]\" is \"linda.white@gmail.com\" which already exists"
                        );
                    });
                }
        );
    }
}
