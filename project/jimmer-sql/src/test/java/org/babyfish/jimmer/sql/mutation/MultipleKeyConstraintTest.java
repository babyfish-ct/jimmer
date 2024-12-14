package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
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
                        .saveEntitiesCommand(users),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(" +
                                        "--->ACCOUNT, DESCRIPTION" +
                                        ") key(" +
                                        "--->ACCOUNT" +
                                        ") values(?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(" +
                                        "--->EMAIL, DESCRIPTION" +
                                        ") key(" +
                                        "--->EMAIL" +
                                        ") values(?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(" +
                                        "--->AREA, NICK_NAME, DESCRIPTION" +
                                        ") key(" +
                                        "--->AREA, NICK_NAME" +
                                        ") values(?, ?, ?)"
                        );
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
                    draft.setAccount("sysusr_005");
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
                        .saveEntitiesCommand(users),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(" +
                                        "--->ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION" +
                                        ") key(" +
                                        "--->AREA, NICK_NAME" +
                                        ") values(?, ?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                "sysusr_005",
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
                                "select tb_1_.ID, tb_1_.AREA, tb_1_.NICK_NAME " +
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
                    draft.setAccount("sysusr_005");
                    draft.setEmail("ada.wang@umbrella.com");
                    draft.setArea("north");
                    draft.setNickName("Ada");
                    draft.setDescription("Description_D");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_006");
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
                        .saveEntitiesCommand(users),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(" +
                                        "--->ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION" +
                                        ") key(" +
                                        "--->AREA, NICK_NAME" +
                                        ") values(?, ?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                "sysusr_005",
                                "ada.wang@umbrella.com",
                                "north",
                                "Ada",
                                "Description_D"
                        );
                        it.batchVariables(
                                1,
                                "sysusr_006",
                                "linda.white@gmail.com",
                                "west",
                                "Wesker",
                                "Description_E"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.AREA, tb_1_.NICK_NAME " +
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

    @Test
    public void testInsertKeyById() {
        executeAndExpectResult(
                getSqlClient().saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createSysUser(draft -> {
                                    draft.setId(21L);
                                    draft.setAccount("sysusr_021");
                                    draft.setEmail("pipi.lu@gmail.com");
                                    draft.setArea("east");
                                    draft.setNickName("Pipilu");
                                    draft.setDescription("Description_21");
                                }),
                                Immutables.createSysUser(draft -> {
                                    draft.setId(22L);
                                    draft.setAccount("sysusr_022");
                                    draft.setEmail("lu.xixi@gmail.com");
                                    draft.setArea("north");
                                    draft.setNickName("Tom");
                                    draft.setDescription("Description_22");
                                })
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(" +
                                        "--->ID, ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION" +
                                        ") key(" +
                                        "--->ID" +
                                        ") values(?, ?, ?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                21L,
                                "sysusr_021",
                                "pipi.lu@gmail.com",
                                "east",
                                "Pipilu",
                                "Description_21"
                        );
                        it.batchVariables(
                                1,
                                22L,
                                "sysusr_022",
                                "lu.xixi@gmail.com",
                                "north",
                                "Tom",
                                "Description_22"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql("select tb_1_.ID from SYS_USER tb_1_ where tb_1_.ACCOUNT = ?");
                        it.variables("sysusr_022");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql("select tb_1_.ID from SYS_USER tb_1_ where tb_1_.EMAIL = ?");
                        it.variables("lu.xixi@gmail.com");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID " +
                                        "from SYS_USER tb_1_ " +
                                        "where (tb_1_.AREA, tb_1_.NICK_NAME) = (?, ?)"
                        );
                        it.variables("north", "Tom");
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.SysUser.area, " +
                                        "org.babyfish.jimmer.sql.model.SysUser.nickName" +
                                        "]\" are \"Tuple2(_1=north, _2=Tom)\" which already exists"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdatePartialKeyById() {
        executeAndExpectResult(
                getSqlClient().saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createSysUser(draft -> {
                                    draft.setId(3L);
                                    draft.setNickName("Alex2");
                                }),
                                Immutables.createSysUser(draft -> {
                                    draft.setId(4L);
                                    draft.setNickName("Tom");
                                })
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(" +
                                        "--->ID, NICK_NAME" +
                                        ") key(" +
                                        "--->ID" +
                                        ") values(?, ?)"
                        );
                        it.batchVariables(0, 3L, "Alex2");
                        it.batchVariables(1, 4L, "Tom");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql("select tb_1_.ID, tb_1_.AREA from SYS_USER tb_1_ where tb_1_.ID = ?");
                        it.variables(4L);
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID " +
                                        "from SYS_USER tb_1_ " +
                                        "where (tb_1_.AREA, tb_1_.NICK_NAME) = (?, ?)"
                        );
                        it.variables("north", "Tom");
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.SysUser.area, " +
                                        "org.babyfish.jimmer.sql.model.SysUser.nickName" +
                                        "]\" are \"Tuple2(_1=north, _2=Tom)\" which already exists"
                        );
                    });
                }
        );
    }

    @Test
    public void testInsertKeyByKey() {
        executeAndExpectResult(
                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE)).saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("sysusr_021");
                                    draft.setEmail("pipi.lu@gmail.com");
                                    draft.setArea("east");
                                    draft.setNickName("Pipilu");
                                    draft.setDescription("Description_21");
                                }),
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("sysusr_001");
                                    draft.setEmail("linda.white@gmail.com");
                                    draft.setArea("north");
                                    draft.setNickName("Tom");
                                    draft.setDescription("Description_22");
                                })
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into SYS_USER(" +
                                        "--->ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION" +
                                        ") key(" +
                                        "--->AREA, NICK_NAME" +
                                        ") values(?, ?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                "sysusr_021",
                                "pipi.lu@gmail.com",
                                "east",
                                "Pipilu",
                                "Description_21"
                        );
                        it.batchVariables(
                                1,
                                "sysusr_001",
                                "linda.white@gmail.com",
                                "north",
                                "Tom",
                                "Description_22"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = ?"
                        );
                        it.variables("sysusr_001");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.EMAIL = ?"
                        );
                        it.variables("linda.white@gmail.com");
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the key property \"[" +
                                        "org.babyfish.jimmer.sql.model.SysUser.email" +
                                        "]\" is \"linda.white@gmail.com\" which already exists"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdatePartialKeyByKey() {
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("sysusr_003");
                                    draft.setNickName("Alex2");
                                }),
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("sysusr_004");
                                    draft.setNickName("Tom");
                                })
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("merge into SYS_USER(ACCOUNT, NICK_NAME) key(ACCOUNT) values(?, ?)");
                        it.batchVariables(0, "sysusr_003", "Alex2");
                        it.batchVariables(1, "sysusr_004", "Tom");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.AREA " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = ?"
                        );
                        it.variables("sysusr_004");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT " +
                                        "from SYS_USER tb_1_ " +
                                        "where (tb_1_.AREA, tb_1_.NICK_NAME) = (?, ?)"
                        );
                        it.variables("north", "Tom");
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.SysUser.area, " +
                                        "org.babyfish.jimmer.sql.model.SysUser.nickName" +
                                        "]\" are \"Tuple2(_1=north, _2=Tom)\" which already exists"
                        );
                    });
                }
        );
    }

    @Test
    public void translateDuplicateAccountByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        List<SysUser> users = Arrays.asList(
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_005");
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
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    it.setDialect(new PostgresDialect());
                })
                        .getEntities()
                        .saveEntitiesCommand(users),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(" +
                                        "--->ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION" +
                                        ") values(?, ?, ?, ?, ?) on conflict(" +
                                        "--->AREA, NICK_NAME" +
                                        ") do update set " +
                                        "--->ACCOUNT = excluded.ACCOUNT, " +
                                        "--->EMAIL = excluded.EMAIL, " +
                                        "--->DESCRIPTION = excluded.DESCRIPTION " +
                                        "returning ID"
                        );
                        it.batchVariables(
                                0,
                                "sysusr_005",
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
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = any(?)"
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
    public void translateDuplicateEmailByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        List<SysUser> users = Arrays.asList(
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_005");
                    draft.setEmail("ada.wang@umbrella.com");
                    draft.setArea("north");
                    draft.setNickName("Ada");
                    draft.setDescription("Description_D");
                }),
                Immutables.createSysUser(draft -> {
                    draft.setAccount("sysusr_006");
                    draft.setEmail("linda.white@gmail.com");
                    draft.setArea("west");
                    draft.setNickName("Wesker");
                    draft.setDescription("Description_E");
                })
        );
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    it.setDialect(new PostgresDialect());
                })
                        .getEntities()
                        .saveEntitiesCommand(users),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(" +
                                        "--->ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION" +
                                        ") values(?, ?, ?, ?, ?) on conflict(" +
                                        "--->AREA, NICK_NAME" +
                                        ") do update set " +
                                        "--->ACCOUNT = excluded.ACCOUNT, " +
                                        "--->EMAIL = excluded.EMAIL, " +
                                        "--->DESCRIPTION = excluded.DESCRIPTION " +
                                        "returning ID"
                        );
                        it.batchVariables(
                                0,
                                "sysusr_005",
                                "ada.wang@umbrella.com",
                                "north",
                                "Ada",
                                "Description_D"
                        );
                        it.batchVariables(
                                1,
                                "sysusr_006",
                                "linda.white@gmail.com",
                                "west",
                                "Wesker",
                                "Description_E"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = any(?)"
                        );
                        it.variables((Object) new Object[]{"sysusr_005", "sysusr_006"});
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.EMAIL, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.EMAIL = any(?)"
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

    @Test
    public void testInsertKeyByIdByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    it.setDialect(new PostgresDialect());
                }).saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createSysUser(draft -> {
                                    draft.setId(21L);
                                    draft.setAccount("sysusr_021");
                                    draft.setEmail("pipi.lu@gmail.com");
                                    draft.setArea("east");
                                    draft.setNickName("Pipilu");
                                    draft.setDescription("Description_21");
                                }),
                                Immutables.createSysUser(draft -> {
                                    draft.setId(22L);
                                    draft.setAccount("sysusr_022");
                                    draft.setEmail("lu.xixi@gmail.com");
                                    draft.setArea("north");
                                    draft.setNickName("Tom");
                                    draft.setDescription("Description_22");
                                })
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(" +
                                        "--->ID, ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION" +
                                        ") values(?, ?, ?, ?, ?, ?) on conflict(" +
                                        "--->ID" +
                                        ") do update set " +
                                        "--->ACCOUNT = excluded.ACCOUNT, " +
                                        "--->EMAIL = excluded.EMAIL, " +
                                        "--->AREA = excluded.AREA, " +
                                        "--->NICK_NAME = excluded.NICK_NAME, " +
                                        "--->DESCRIPTION = excluded.DESCRIPTION"
                        );
                        it.batchVariables(
                                0,
                                21L,
                                "sysusr_021",
                                "pipi.lu@gmail.com",
                                "east",
                                "Pipilu",
                                "Description_21"
                        );
                        it.batchVariables(
                                1,
                                22L,
                                "sysusr_022",
                                "lu.xixi@gmail.com",
                                "north",
                                "Tom",
                                "Description_22"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = any(?)"
                        );
                        it.variables((Object) new Object[]{"sysusr_021", "sysusr_022"});
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.EMAIL " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.EMAIL = any(?)"
                        );
                        it.variables((Object)new Object[]{"pipi.lu@gmail.com", "lu.xixi@gmail.com"});
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where (tb_1_.AREA, tb_1_.NICK_NAME) in ((?, ?), (?, ?))"
                        );
                        it.variables("east", "Pipilu", "north", "Tom");
                    });
                    ctx.throwable(it -> {
                        //it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.SysUser.area, " +
                                        "org.babyfish.jimmer.sql.model.SysUser.nickName" +
                                        "]\" are \"Tuple2(_1=north, _2=Tom)\" which already exists"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdatePartialKeyByIdByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    it.setDialect(new PostgresDialect());
                }).saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createSysUser(draft -> {
                                    draft.setId(3L);
                                    draft.setNickName("Alex2");
                                }),
                                Immutables.createSysUser(draft -> {
                                    draft.setId(4L);
                                    draft.setNickName("Tom");
                                })
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(" +
                                        "--->ID, NICK_NAME" +
                                        ") values(?, ?) on conflict(" +
                                        "--->ID" +
                                        ") do update set " +
                                        "--->NICK_NAME = excluded.NICK_NAME"
                        );
                        it.batchVariables(0, 3L, "Alex2");
                        it.batchVariables(1, 4L, "Tom");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql("select tb_1_.ID, tb_1_.AREA from SYS_USER tb_1_ where tb_1_.ID = any(?)");
                        it.variables((Object) new Object[] {3L, 4L});
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where (tb_1_.AREA, tb_1_.NICK_NAME) in ((?, ?), (?, ?))"
                        );
                        it.variables("east", "Alex2", "north", "Tom");
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.SysUser.area, " +
                                        "org.babyfish.jimmer.sql.model.SysUser.nickName" +
                                        "]\" are \"Tuple2(_1=north, _2=Tom)\" which already exists"
                        );
                    });
                }
        );
    }

    @Test
    public void testInsertKeyByKeyByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    it.setDialect(new PostgresDialect());
                }).saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("sysusr_021");
                                    draft.setEmail("pipi.lu@gmail.com");
                                    draft.setArea("east");
                                    draft.setNickName("Pipilu");
                                    draft.setDescription("Description_21");
                                }),
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("sysusr_001");
                                    draft.setEmail("linda.white@gmail.com");
                                    draft.setArea("north");
                                    draft.setNickName("Tom");
                                    draft.setDescription("Description_22");
                                })
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(" +
                                        "--->ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION" +
                                        ") values(?, ?, ?, ?, ?) on conflict(" +
                                        "--->AREA, NICK_NAME" +
                                        ") do update set " +
                                        "--->ACCOUNT = excluded.ACCOUNT, " +
                                        "--->EMAIL = excluded.EMAIL, " +
                                        "--->DESCRIPTION = excluded.DESCRIPTION " +
                                        "returning ID"
                        );
                        it.batchVariables(
                                0,
                                "sysusr_021",
                                "pipi.lu@gmail.com",
                                "east",
                                "Pipilu",
                                "Description_21"
                        );
                        it.batchVariables(
                                1,
                                "sysusr_001",
                                "linda.white@gmail.com",
                                "north",
                                "Tom",
                                "Description_22"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = any(?)"
                        );
                        it.variables((Object)new Object[]{"sysusr_021", "sysusr_001"});
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.EMAIL, tb_1_.AREA, tb_1_.NICK_NAME " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.EMAIL = any(?)"
                        );
                        it.variables((Object) new Object[]{"pipi.lu@gmail.com", "linda.white@gmail.com"});
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the key property \"[" +
                                        "org.babyfish.jimmer.sql.model.SysUser.email" +
                                        "]\" is \"linda.white@gmail.com\" which already exists"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdatePartialKeyByKeyByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    it.setDialect(new PostgresDialect());
                }).saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("sysusr_003");
                                    draft.setNickName("Alex2");
                                }),
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("sysusr_004");
                                    draft.setNickName("Tom");
                                })
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(" +
                                        "--->ACCOUNT, NICK_NAME" +
                                        ") values(?, ?) on conflict(" +
                                        "--->ACCOUNT" +
                                        ") do update set " +
                                        "--->NICK_NAME = excluded.NICK_NAME " +
                                        "returning ID"
                        );
                        it.batchVariables(0, "sysusr_003", "Alex2");
                        it.batchVariables(1, "sysusr_004", "Tom");
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.ACCOUNT, tb_1_.AREA " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ACCOUNT = any(?)"
                        );
                        it.variables((Object) new Object[]{"sysusr_003", "sysusr_004"});
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                        it.sql(
                                "select tb_1_.ID, tb_1_.AREA, tb_1_.NICK_NAME, tb_1_.ACCOUNT " +
                                        "from SYS_USER tb_1_ " +
                                        "where (tb_1_.AREA, tb_1_.NICK_NAME) in ((?, ?), (?, ?))"
                        );
                        it.variables("east", "Alex2", "north", "Tom");
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.NotUnique.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.SysUser.area, " +
                                        "org.babyfish.jimmer.sql.model.SysUser.nickName" +
                                        "]\" are \"Tuple2(_1=north, _2=Tom)\" which already exists"
                        );
                    });
                }
        );
    }
}
