package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

public class IdOrKeyOnlyAssociationTest extends AbstractMutationTest {

    @Test
    public void testIdOnlyAsReference() {
        Book book = Immutables.createBook(draft -> {
            draft.setId(Constants.learningGraphQLId1);
            draft.setAuthorIds(Arrays.asList(Constants.alexId, Constants.danId));
        });
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(book),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID not in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "--->on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then " +
                                        "--->insert(BOOK_ID, AUTHOR_ID) " +
                                        "--->values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                    });
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testIdOnlyAsEntity() {
        Book book = Immutables.createBook(draft -> {
            draft.setId(Constants.learningGraphQLId1);
            draft.setAuthorIds(Arrays.asList(Constants.alexId, Constants.danId));
        });
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.addDraftPreProcessor(new DraftPreProcessor<AuthorDraft>() {
                        @Override
                        public void beforeSave(@NotNull AuthorDraft draft) {
                            if (draft.id().equals(Constants.alexId)) {
                                draft.setFirstName("Alex");
                                draft.setLastName("Banks");
                                draft.setGender(Gender.MALE);
                            } else if (draft.id().equals(Constants.danId)) {
                                draft.setFirstName("Dan");
                                draft.setLastName("Vanderkam");
                                draft.setGender(Gender.MALE);
                            }
                        }
                    });
                }).getEntities()
                        .saveCommand(book)
                        .setIdOnlyAsReference(BookProps.AUTHORS, false),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) " +
                                        "key(ID) " +
                                        "values(?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID not in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "--->on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then " +
                                        "--->insert(BOOK_ID, AUTHOR_ID) " +
                                        "--->values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                    });
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testRootIdOnlyAsEntity() {
        Book book = Immutables.createBook(draft -> {
            draft.setId(Constants.learningGraphQLId1);
        });
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.setName("Learning GraphQL");
                            draft.setEdition(1);
                            draft.setPrice(new BigDecimal("49.9"));
                        }
                    });
                }).getEntities()
                        .saveCommand(book)
                        .setIdOnlyAsReferenceAll(false),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK(ID, NAME, EDITION, PRICE) key(ID) values(?, ?, ?, ?)"
                        );
                    });
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testRecursiveIdOnlyAsEntity() {
        TreeNode treeNode = Immutables.createTreeNode(draft -> {
           draft.setId(1001L);
           draft.addIntoChildNodes(
                   child1 -> {
                       child1.setId(1002L);
                       child1.addIntoChildNodes(child11 -> {
                           child11.setId(1003L);
                       });
                       child1.addIntoChildNodes(child12 -> {
                           child12.setId(1004L);
                       });
                   }
           );
            draft.addIntoChildNodes(
                    child2 -> {
                        child2.setId(1005L);
                        child2.addIntoChildNodes(child21 -> {
                            child21.setId(1006L);
                        });
                        child2.addIntoChildNodes(child22 -> {
                            child22.setId(1007L);
                        });
                    }
            );
        });
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setTargetTransferable(true);
                    it.addDraftPreProcessor(new DraftPreProcessor<TreeNodeDraft>() {
                        @Override
                        public void beforeSave(@NotNull TreeNodeDraft draft) {
                            draft.setName("node-" + draft.id());
                        }
                    });
                }).getEntities()
                        .saveCommand(treeNode),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("merge into TREE_NODE(NODE_ID, NAME) key(NODE_ID) values(?, ?)");
                        it.variables(1001L, "node-1001");
                    });
                    ctx.statement(it -> {
                        it.sql("merge into TREE_NODE(NODE_ID, NAME, PARENT_ID) key(NODE_ID) values(?, ?, ?)");
                        it.batchVariables(0, 1002L, "node-1002", 1001L);
                        it.batchVariables(1, 1005L, "node-1005", 1001L);
                    });
                    ctx.statement(it -> {
                        it.sql("merge into TREE_NODE(NODE_ID, NAME, PARENT_ID) key(NODE_ID) values(?, ?, ?)");
                        it.batchVariables(0, 1003L, "node-1003", 1002L);
                        it.batchVariables(1, 1004L, "node-1004", 1002L);
                        it.batchVariables(2, 1006L, "node-1006", 1005L);
                        it.batchVariables(3, 1007L, "node-1007", 1005L);
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.TOO_DEEP);
                        it.sql(
                                "select tb_1_.NODE_ID from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID = any(?) " +
                                        "and (tb_3_.PARENT_ID, tb_2_.PARENT_ID) not in ((?, ?), (?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                new Object[] { 1002L, 1005L },
                                1002L, 1003L, 1002L, 1004L, 1005L, 1006L, 1005L, 1007L
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE tb_1_ " +
                                        "where exists(" +
                                        "--->select * from TREE_NODE tb_2_ " +
                                        "--->where tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and PARENT_ID = ? " +
                                        "--->and not (NODE_ID = any(?))" +
                                        ")"
                        );
                        it.batchVariables(0, 1002L, new Object[]{1003L, 1004L});
                        it.batchVariables(1, 1005L, new Object[]{1006L, 1007L});
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where PARENT_ID = ? and not (NODE_ID = any(?))");
                        it.batchVariables(0, 1002L, new Object[]{1003L, 1004L});
                        it.batchVariables(1, 1005L, new Object[]{1006L, 1007L});
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.TOO_DEEP);
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID = ? and not (tb_2_.PARENT_ID = any(?))"
                        );
                        it.variables(1001L, new Object[]{1002L, 1005L});
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE tb_1_ where exists(" +
                                        "--->select * from TREE_NODE tb_2_ " +
                                        "--->where tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and PARENT_ID = ? and not (NODE_ID = any(?)))"
                        );
                        it.variables(1001L, new Object[]{1002L, 1005L});
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where PARENT_ID = ? and not (NODE_ID = any(?))");
                        it.variables(1001L, new Object[]{1002L, 1005L});
                    });
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testKeyOnlyAsReference() {
        Book book = Immutables.createBook(draft -> {
            draft.setId(Constants.learningGraphQLId1);
            draft.addIntoAuthors(author -> {
                author.setFirstName("Alex");
                author.setLastName("Banks");
            });
            draft.addIntoAuthors(author -> {
                author.setFirstName("Dan");
                author.setLastName("Vanderkam");
            });
        });
        executeAndExpectResult(
                getSqlClient().getEntities()
                        .saveCommand(book)
                        .setKeyOnlyAsReferenceAll(),
                        // or `setKeyOnlyAsReference(BookProps.AUTHORS)`,
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.KEY_ONLY_AS_REFERENCE);
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) in ((?, ?), (?, ?))"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID not in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "--->on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then " +
                                        "--->insert(BOOK_ID, AUTHOR_ID) " +
                                        "--->values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                    });
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testKeyOnlyAsEntity() {
        Book book = Immutables.createBook(draft -> {
            draft.setId(Constants.learningGraphQLId1);
            draft.addIntoAuthors(author -> {
                author.setFirstName("Alex");
                author.setLastName("Banks");
            });
            draft.addIntoAuthors(author -> {
                author.setFirstName("Dan");
                author.setLastName("Vanderkam");
            });
        });
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.addDraftPreProcessor(new DraftPreProcessor<AuthorDraft>() {
                        @Override
                        public void beforeSave(@NotNull AuthorDraft draft) {
                            draft.setGender(Gender.MALE);
                        }
                    });
                }).getEntities()
                        .saveCommand(book),
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) in ((?, ?), (?, ?))"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("update AUTHOR set GENDER = ? where ID = ?");
                        it.batches(2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID not in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                        "--->on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                        "when not matched then " +
                                        "--->insert(BOOK_ID, AUTHOR_ID) " +
                                        "--->values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)"
                        );
                    });
                    ctx.entity(it -> {});
                }
        );
    }
}
