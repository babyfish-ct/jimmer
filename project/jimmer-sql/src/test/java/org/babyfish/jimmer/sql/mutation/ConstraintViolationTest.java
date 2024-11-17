package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.middle.Shop;
import org.babyfish.jimmer.sql.model.middle.ShopDraft;
import org.babyfish.jimmer.sql.model.middle.ShopProps;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

public class ConstraintViolationTest extends AbstractMutationTest {

    @Test
    public void testConflictId() {
        TreeNode treeNode1 = TreeNodeDraft.$.produce(draft -> {
            draft.setId(50L);
            draft.setName("Root2");
            draft.setParent(null);
        });
        TreeNode treeNode2 = TreeNodeDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.setName("Root3");
            draft.setParent(null);
        });
        executeAndExpectResult(
                getSqlClient().getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(treeNode1, treeNode2)
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into TREE_NODE(NODE_ID, NAME, PARENT_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.batchVariables(0, 50L, "Root2", new DbLiteral.DbNull(long.class));
                        it.batchVariables(1, 1L, "Root3", new DbLiteral.DbNull(long.class));
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID from TREE_NODE tb_1_ " +
                                        "where tb_1_.NODE_ID = ?"
                        );
                        it.variables(1L);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the id property " +
                                        "\"org.babyfish.jimmer.sql.model.TreeNode.id\" " +
                                        "is \"1\" which already exists"
                        );
                        SaveException.NotUnique ex = it.type(SaveException.NotUnique.class);
                        Assertions.assertTrue(ex.isMatched(TreeNodeProps.ID));
                    });
                }
        );
    }

    @Test
    public void testConflictKey() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(4);
            draft.setPrice(new BigDecimal("56.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(3);
            draft.setPrice(new BigDecimal("54.9"));
        });
        setAutoIds(Book.class, UUID.randomUUID(), UUID.randomUUID());
        executeAndExpectResult(
                getSqlClient().getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(book1, book2)
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(ID, NAME, EDITION, PRICE) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 4, new BigDecimal("56.9")
                        );
                        it.batchVariables(
                                1,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 3, new BigDecimal("54.9")
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID from BOOK tb_1_ where tb_1_.ID = ?");
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                        );
                        it.variables("GraphQL in Action", 3);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>\": Cannot save the entity, " +
                                        "the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.Book.name, " +
                                        "org.babyfish.jimmer.sql.model.Book.edition" +
                                        "]\" are \"Tuple2(_1=GraphQL in Action, _2=3)\" which already exists"
                        );
                        SaveException.NotUnique ex = it.type(SaveException.NotUnique.class);
                        Assertions.assertTrue(
                                ex.isMatched(
                                        BookProps.NAME,
                                        BookProps.EDITION
                                )
                        );
                        Assertions.assertTrue(
                                ex.isMatched(
                                        BookProps.EDITION,
                                        BookProps.NAME
                                )
                        );
                    });
                }
        );
    }

    @Test
    public void testConflictKeyWithGlobalExceptionTranslator() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(4);
            draft.setPrice(new BigDecimal("56.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(3);
            draft.setPrice(new BigDecimal("54.9"));
        });
        setAutoIds(Book.class, UUID.randomUUID(), UUID.randomUUID());
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.addExceptionTranslator(
                            new ExceptionTranslator<SaveException.NotUnique>() {
                                @Override
                                public @Nullable Exception translate(
                                        @NotNull SaveException.NotUnique exception,
                                        @NotNull Args args
                                ) {
                                    if (exception.isMatched(BookProps.NAME, BookProps.EDITION)) {
                                        return new IllegalArgumentException(
                                                "Illegal name and edition: " +
                                                        exception.getValueMap().values()
                                        );
                                    }
                                    return null;
                                }
                            }
                    );
                })
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(book1, book2)
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(ID, NAME, EDITION, PRICE) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 4, new BigDecimal("56.9")
                        );
                        it.batchVariables(
                                1,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 3, new BigDecimal("54.9")
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID from BOOK tb_1_ where tb_1_.ID = ?");
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                        );
                        it.variables("GraphQL in Action", 3);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Illegal name and edition: [GraphQL in Action, 3]"
                        );
                        it.type(IllegalArgumentException.class);
                    });
                }
        );
    }

    @Test
    public void testConflictKeyWithHierarchicalExceptionTranslators() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(4);
            draft.setPrice(new BigDecimal("56.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(3);
            draft.setPrice(new BigDecimal("54.9"));
        });
        setAutoIds(Book.class, UUID.randomUUID(), UUID.randomUUID());
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.addExceptionTranslator(
                            new ExceptionTranslator<SaveException.NotUnique>() {
                                @Override
                                public @Nullable Exception translate(
                                        @NotNull SaveException.NotUnique exception,
                                        @NotNull Args args
                                ) {
                                    if (exception.isMatched(BookProps.NAME, BookProps.EDITION)) {
                                        return new IllegalArgumentException(
                                                "Illegal name and edition: " +
                                                        exception.getValueMap().values()
                                        );
                                    }
                                    return null;
                                }
                            }
                    );
                })
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(book1, book2)
                        )
                        .setMode(SaveMode.INSERT_ONLY)
                        .addExceptionTranslator(
                                new ExceptionTranslator<SaveException.NotUnique>() {
                                    @Override
                                    public @Nullable Exception translate(
                                            @NotNull SaveException.NotUnique exception,
                                            @NotNull Args args
                                    ) {
                                        if (exception.isMatched(BookProps.NAME, BookProps.EDITION)) {
                                            return new IllegalArgumentException(
                                                    "The book whose name is \"" +
                                                            exception.getValue(BookProps.NAME) +
                                                            "\" and edition is \"" +
                                                            exception.getValue(BookProps.EDITION) +
                                                            "\" already exists"
                                            );
                                        }
                                        return null;
                                    }
                                }
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(ID, NAME, EDITION, PRICE) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 4, new BigDecimal("56.9")
                        );
                        it.batchVariables(
                                1,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 3, new BigDecimal("54.9")
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID from BOOK tb_1_ where tb_1_.ID = ?");
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                        );
                        it.variables("GraphQL in Action", 3);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "The book whose name is \"GraphQL in Action\" and edition is \"3\" already exists"
                        );
                        it.type(IllegalArgumentException.class);
                    });
                }
        );
    }

    @Test
    public void testIllegalForeignKey() {
        TreeNode treeNode1 = TreeNodeDraft.$.produce(draft -> {
            draft.setName("Pepsi");
            draft.setParentId(3L);
        });
        TreeNode treeNode2 = TreeNodeDraft.$.produce(draft -> {
            draft.setName("Nescafe");
            draft.setParentId(50L);
        });
        setAutoIds(TreeNode.class, 100L, 101L);
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(
                                        treeNode1,
                                        treeNode2
                                )
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into TREE_NODE(NODE_ID, NAME, PARENT_ID) values(?, ?, ?)");
                        it.batchVariables(0, 100L, "Pepsi", 3L);
                        it.batchVariables(1, 101L, "Nescafe", 50L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID from TREE_NODE tb_1_ " +
                                        "where tb_1_.NODE_ID = ?"
                        );
                        it.variables(101L);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID from TREE_NODE tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.PARENT_ID) = (?, ?)"
                        );
                        it.variables("Nescafe", 50L);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID from TREE_NODE tb_1_ " +
                                        "where tb_1_.NODE_ID = ?"
                        );
                        it.variables(50L);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>.parent\": " +
                                        "Cannot save the entity, the associated id of the reference property " +
                                        "\"org.babyfish.jimmer.sql.model.TreeNode.parent\" is \"50\" " +
                                        "but there is no corresponding associated object in the database"
                        );
                        SaveException.IllegalTargetId ex = it.type(SaveException.IllegalTargetId.class);
                        Assertions.assertEquals(TreeNodeProps.PARENT.unwrap(), ex.getProp());
                        Assertions.assertEquals("[50]", ex.getTargetIds().toString());
                    });
                }
        );
    }

    @Test
    public void testIllegalForeignKeyOfMiddleTable() {
        Shop shop = ShopDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.addIntoOrdinaryCustomers(customer -> customer.setId(2L));
            draft.addIntoOrdinaryCustomers(customer -> customer.setId(3L));
            draft.addIntoOrdinaryCustomers(customer -> customer.setId(999L));
        });
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(shop),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from shop_customer_mapping " +
                                        "where " +
                                        "--->shop_id = ? " +
                                        "and " +
                                        "--->customer_id not in (?, ?, ?) " +
                                        "and " +
                                        "--->type = ?"
                        );
                        it.variables(1L, 2L, 3L, 999L, "ORDINARY");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into shop_customer_mapping tb_1_ " +
                                        "using(values(?, ?, ?, ?)) tb_2_(shop_id, customer_id, deleted_millis, type) " +
                                        "on tb_1_.shop_id = tb_2_.shop_id and " +
                                        "--->tb_1_.customer_id = tb_2_.customer_id and " +
                                        "--->tb_1_.deleted_millis = tb_2_.deleted_millis and " +
                                        "--->tb_1_.type = tb_2_.type " +
                                        "when not matched then " +
                                        "--->insert(shop_id, customer_id, deleted_millis, type) " +
                                        "--->values(tb_2_.shop_id, tb_2_.customer_id, tb_2_.deleted_millis, tb_2_.type)"
                        );
                        it.batchVariables(0, 1L, 2L, 0L, "ORDINARY");
                        it.batchVariables(1, 1L, 3L, 0L, "ORDINARY");
                        it.batchVariables(2, 1L, 999L, 0L, "ORDINARY");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID from CUSTOMER tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables(999L);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>.ordinaryCustomers\": " +
                                        "Cannot save the entity, the associated id of the reference property " +
                                        "\"org.babyfish.jimmer.sql.model.middle.Shop.ordinaryCustomers\" is \"999\" " +
                                        "but there is no corresponding associated object in the database"
                        );
                        SaveException.IllegalTargetId ex = it.type(SaveException.IllegalTargetId.class);
                        Assertions.assertEquals(ShopProps.ORDINARY_CUSTOMERS.unwrap(), ex.getProp());
                        Assertions.assertEquals("[999]", ex.getTargetIds().toString());
                    });
                }
        );
    }

    @Test
    public void testConflictKeyByMySql() {

        NativeDatabases.assumeNativeDatabase();

        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(4);
            draft.setPrice(new BigDecimal("56.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(3);
            draft.setPrice(new BigDecimal("54.9"));
        });
        setAutoIds(Book.class, UUID.randomUUID(), UUID.randomUUID());
        executeAndExpectResult(
                NativeDatabases.MYSQL_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new MySqlDialect());
                    it.addScalarProvider(ScalarProvider.uuidByByteArray());
                })
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(book1, book2)
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(ID, NAME, EDITION, PRICE) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 4, new BigDecimal("56.9")
                        );
                        it.batchVariables(
                                1,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 3, new BigDecimal("54.9")
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID from BOOK tb_1_ where tb_1_.ID = ?");
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                        );
                        it.variables("GraphQL in Action", 3);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>\": Cannot save the entity, " +
                                        "the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.Book.name, " +
                                        "org.babyfish.jimmer.sql.model.Book.edition" +
                                        "]\" are \"Tuple2(_1=GraphQL in Action, _2=3)\" which already exists"
                        );
                        SaveException.NotUnique ex = it.type(SaveException.NotUnique.class);
                        Assertions.assertTrue(
                                ex.isMatched(
                                        BookProps.NAME,
                                        BookProps.EDITION
                                )
                        );
                        Assertions.assertTrue(
                                ex.isMatched(
                                        BookProps.EDITION,
                                        BookProps.NAME
                                )
                        );
                    });
                }
        );
    }

    @Test
    public void testConflictIdByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        TreeNode treeNode1 = TreeNodeDraft.$.produce(draft -> {
            draft.setId(50L);
            draft.setName("Root2");
            draft.setParent(null);
        });
        TreeNode treeNode2 = TreeNodeDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.setName("Root3");
            draft.setParent(null);
        });
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect())).getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(treeNode1, treeNode2)
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into TREE_NODE(NODE_ID, NAME, PARENT_ID) " +
                                        "values(?, ?, ?)"
                        );
                        it.batchVariables(0, 50L, "Root2", new DbLiteral.DbNull(long.class));
                        it.batchVariables(1, 1L, "Root3", new DbLiteral.DbNull(long.class));
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID from TREE_NODE tb_1_ " +
                                        "where tb_1_.NODE_ID = any(?)"
                        );
                        it.variables((Object) new Object[]{ 50L, 1L });
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot save the entity, the value of the id property " +
                                        "\"org.babyfish.jimmer.sql.model.TreeNode.id\" " +
                                        "is \"1\" which already exists"
                        );
                        SaveException.NotUnique ex = it.type(SaveException.NotUnique.class);
                        Assertions.assertTrue(ex.isMatched(TreeNodeProps.ID));
                    });
                }
        );
    }

    @Test
    public void testConflictKeyByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(4);
            draft.setPrice(new BigDecimal("56.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(3);
            draft.setPrice(new BigDecimal("54.9"));
        });
        setAutoIds(Book.class, UUID.randomUUID(), UUID.randomUUID());
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new PostgresDialect());
                })
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(book1, book2)
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(ID, NAME, EDITION, PRICE) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.batchVariables(
                                0,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 4, new BigDecimal("56.9")
                        );
                        it.batchVariables(
                                1,
                                UNKNOWN_VARIABLE, "GraphQL in Action", 3, new BigDecimal("54.9")
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID from BOOK tb_1_ where tb_1_.ID = any(?)");
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"
                        );
                        it.variables(
                                "GraphQL in Action", 4,
                                "GraphQL in Action", 3
                        );
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>\": Cannot save the entity, " +
                                        "the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.Book.name, " +
                                        "org.babyfish.jimmer.sql.model.Book.edition" +
                                        "]\" are \"Tuple2(_1=GraphQL in Action, _2=3)\" which already exists"
                        );
                        SaveException.NotUnique ex = it.type(SaveException.NotUnique.class);
                        Assertions.assertTrue(
                                ex.isMatched(
                                        BookProps.NAME,
                                        BookProps.EDITION
                                )
                        );
                        Assertions.assertTrue(
                                ex.isMatched(
                                        BookProps.EDITION,
                                        BookProps.NAME
                                )
                        );
                    });
                }
        );
    }

    @Test
    public void testConflictKeyWithIdByPostgres_Issue689() {

        NativeDatabases.assumeNativeDatabase();

        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(Constants.graphQLInActionId1);
            draft.setName("GraphQL in Action");
            draft.setEdition(4);
            draft.setPrice(new BigDecimal("56.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(Constants.graphQLInActionId2);
            draft.setName("GraphQL in Action");
            draft.setEdition(3);
            draft.setPrice(new BigDecimal("54.9"));
        });
        setAutoIds(Book.class, UUID.randomUUID(), UUID.randomUUID());
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new PostgresDialect());
                })
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(book1, book2)
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK set NAME = ?, EDITION = ?, PRICE = ? where ID = ?"
                        );
                        it.batchVariables(
                                0,
                                "GraphQL in Action", 4, new BigDecimal("56.9"), UNKNOWN_VARIABLE
                        );
                        it.batchVariables(
                                1,
                                "GraphQL in Action", 3, new BigDecimal("54.9"), UNKNOWN_VARIABLE
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"
                        );
                        it.variables("GraphQL in Action", 4, "GraphQL in Action", 3);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>\": Cannot save the entity, " +
                                        "the value of the key properties \"[" +
                                        "org.babyfish.jimmer.sql.model.Book.name, " +
                                        "org.babyfish.jimmer.sql.model.Book.edition" +
                                        "]\" are \"Tuple2(_1=GraphQL in Action, _2=3)\" which already exists"
                        );
                        SaveException.NotUnique ex = it.type(SaveException.NotUnique.class);
                        Assertions.assertTrue(
                                ex.isMatched(
                                        BookProps.NAME,
                                        BookProps.EDITION
                                )
                        );
                        Assertions.assertTrue(
                                ex.isMatched(
                                        BookProps.EDITION,
                                        BookProps.NAME
                                )
                        );
                    });
                }
        );
    }

    @Test
    public void testIllegalForeignKeyByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        TreeNode treeNode1 = TreeNodeDraft.$.produce(draft -> {
            draft.setName("Pepsi");
            draft.setParentId(3L);
        });
        TreeNode treeNode2 = TreeNodeDraft.$.produce(draft -> {
            draft.setName("Nescafe");
            draft.setParentId(50L);
        });
        setAutoIds(TreeNode.class, 100L, 101L);
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new PostgresDialect());
                    UserIdGenerator<?> idGenerator = this::autoId;
                    it.setIdGenerator(idGenerator);
                })
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(
                                        treeNode1,
                                        treeNode2
                                )
                        )
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into TREE_NODE(NODE_ID, NAME, PARENT_ID) values(?, ?, ?)");
                        it.batchVariables(0, 100L, "Pepsi", 3L);
                        it.batchVariables(1, 101L, "Nescafe", 50L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID from TREE_NODE tb_1_ " +
                                        "where tb_1_.NODE_ID = any(?)"
                        );
                        it.variables((Object)new Object[]{100L, 101L});
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.PARENT_ID) in ((?, ?), (?, ?))"
                        );
                        it.variables("Pepsi", 3L, "Nescafe", 50L);
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID from TREE_NODE tb_1_ " +
                                        "where tb_1_.NODE_ID = any(?)"
                        );
                        it.variables((Object)new Object[]{3L, 50L});
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>.parent\": " +
                                        "Cannot save the entity, the associated id of the reference property " +
                                        "\"org.babyfish.jimmer.sql.model.TreeNode.parent\" is \"50\" " +
                                        "but there is no corresponding associated object in the database"
                        );
                        SaveException.IllegalTargetId ex = it.type(SaveException.IllegalTargetId.class);
                        Assertions.assertEquals(TreeNodeProps.PARENT.unwrap(), ex.getProp());
                        Assertions.assertEquals("[50]", ex.getTargetIds().toString());
                    });
                }
        );
    }

    @Test
    public void testIllegalForeignKeyOfMiddleTableByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        Shop shop = ShopDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.addIntoOrdinaryCustomers(customer -> customer.setId(2L));
            draft.addIntoOrdinaryCustomers(customer -> customer.setId(3L));
            draft.addIntoOrdinaryCustomers(customer -> customer.setId(999L));
        });
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new PostgresDialect());
                })
                        .getEntities()
                        .saveCommand(shop),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from shop_customer_mapping " +
                                        "where " +
                                        "--->shop_id = ? " +
                                        "and " +
                                        "--->not (customer_id = any(?)) " +
                                        "and " +
                                        "--->type = ?"
                        );
                        it.variables(1L, new Object[]{2L, 3L, 999L}, "ORDINARY");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into shop_customer_mapping(" +
                                        "--->shop_id, customer_id, deleted_millis, type" +
                                        ") values(?, ?, ?, ?) on conflict(" +
                                        "--->shop_id, customer_id, deleted_millis, type" +
                                        ") do nothing"
                        );
                        it.batchVariables(0, 1L, 2L, 0L, "ORDINARY");
                        it.batchVariables(1, 1L, 3L, 0L, "ORDINARY");
                        it.batchVariables(2, 1L, 999L, 0L, "ORDINARY");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID from CUSTOMER tb_1_ " +
                                        "where tb_1_.ID = any(?)"
                        );
                        it.variables((Object) new Object[]{ 2L, 3L, 999L });
                        it.queryReason(QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR);
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>.ordinaryCustomers\": " +
                                        "Cannot save the entity, the associated id of the reference property " +
                                        "\"org.babyfish.jimmer.sql.model.middle.Shop.ordinaryCustomers\" is \"999\" " +
                                        "but there is no corresponding associated object in the database"
                        );
                        SaveException.IllegalTargetId ex = it.type(SaveException.IllegalTargetId.class);
                        Assertions.assertEquals(ShopProps.ORDINARY_CUSTOMERS.unwrap(), ex.getProp());
                        Assertions.assertEquals("[999]", ex.getTargetIds().toString());
                    });
                }
        );
    }
}
