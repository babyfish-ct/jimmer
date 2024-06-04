package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class OperatorTest extends AbstractMutationTest {

    private static final Set<ImmutableProp> BOOK_KEY_PROPS = new LinkedHashSet<>(
            Arrays.asList(
                    BookProps.NAME.unwrap(),
                    BookProps.EDITION.unwrap()
            )
    );

    private static final Set<ImmutableProp> BOOK_STORE_KEY_PROPS =
            Collections.singleton(
                    BookStoreProps.NAME.unwrap()
            );

    private static final Set<ImmutableProp> DEPARTMENT_KEY_PROPS =
            Collections.singleton(
                    DepartmentProps.NAME.unwrap()
            );

    private static final Set<ImmutableProp> TREE_NODE_KEY_PROPS =
            Collections.singleton(
                    TreeNodeProps.NAME.unwrap()
            );

    @Test
    public void testInsert() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("8c3c998b-f926-49ec-82c0-b2f6291715ea"));
            draft.setName("SQL in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("09615006-bfdc-45e1-bc65-8256c294dfb4"));
            draft.setName("Kotlin in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("49.9"));
        });
        execute(
                new Book[] { book1, book2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, Book.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(BOOK_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    return operator.insert(shapedEntityMap.iterator().next());
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)");
                        it.batchVariables(
                                0,
                                UUID.fromString("8c3c998b-f926-49ec-82c0-b2f6291715ea"),
                                "SQL in Action",
                                2,
                                new BigDecimal("59.9")
                        );
                        it.batchVariables(
                                1,
                                UUID.fromString("09615006-bfdc-45e1-bc65-8256c294dfb4"),
                                "Kotlin in Action",
                                1,
                                new BigDecimal("49.9")
                        );
                    });
                    ctx.value("2");
                }
        );
    }

    @Test
    public void insertByIdentity() {
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Engine");
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Wheel");
        });
        execute(
                new Department[] {department1, department2},
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, Department.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(DEPARTMENT_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    int rowCount = operator.insert(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(100L, drafts.get(0).__get(DepartmentProps.ID.unwrap().getId()));
                    Assertions.assertEquals(101L, drafts.get(1).__get(DepartmentProps.ID.unwrap().getId()));
                    return rowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into DEPARTMENT(NAME) values(?)");
                        it.batchVariables(0, "Engine");
                        it.batchVariables(1, "Wheel");
                    });
                    ctx.value("2");
                }
        );
    }

    @Test
    public void testInsertBySequence() {
        TreeNode treeNode1 = TreeNodeDraft.$.produce(draft -> {
            draft.setName("Car");
        });
        TreeNode treeNode2 = TreeNodeDraft.$.produce(draft -> {
            draft.setName("MotoBike");
        });
        execute(
                new TreeNode[] { treeNode1, treeNode2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, TreeNode.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(TREE_NODE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    int rowCount = operator.insert(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(100L, drafts.get(0).__get(DepartmentProps.ID.unwrap().getId()));
                    Assertions.assertEquals(101L, drafts.get(1).__get(DepartmentProps.ID.unwrap().getId()));
                    return rowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into TREE_NODE(NODE_ID, NAME) " +
                                        "values((select nextval('tree_node_id_seq')), ?)"
                        );
                        it.batchVariables(0, "Car");
                        it.batchVariables(1, "MotoBike");
                    });
                }
        );
    }

    @Test
    public void testUpdate() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setName("Kotlin in Action");
            draft.setEdition(4);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setName("Kotlin in Action");
            draft.setEdition(5);
        });
        execute(
                new Book[] { book1, book2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, Book.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(BOOK_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    return operator.update(shapedEntityMap.iterator().next());
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK set NAME = ?, EDITION = ? " +
                                        "where ID = ?"
                        );
                        it.batchVariables(0, "Kotlin in Action", 4, graphQLInActionId1);
                        it.batchVariables(1, "Kotlin in Action", 5, graphQLInActionId2);
                    });
                    ctx.value("2");
                }
        );
    }

    @Test
    public void testUpdateByVersion() {
        BookStore store1 = BookStoreDraft.$.produce(draft -> {
            draft.setId(oreillyId);
            draft.setWebsite("https://www.oreilly.com");
            draft.setVersion(0);
        });
        BookStore store2 = BookStoreDraft.$.produce(draft -> {
            draft.setId(manningId);
            draft.setWebsite("https://www.manning.com");
            draft.setVersion(0);
        });
        execute(
                new BookStore[] { store1, store2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, BookStore.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(BOOK_STORE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    int rowCount = operator.update(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(1, drafts.get(0).__get(BookStoreProps.VERSION.unwrap().getId()));
                    Assertions.assertEquals(1, drafts.get(1).__get(BookStoreProps.VERSION.unwrap().getId()));
                    return rowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK_STORE set WEBSITE = ?, VERSION = VERSION + 1 " +
                                        "where ID = ? and VERSION = ?"
                        );
                        it.batchVariables(0, "https://www.oreilly.com", oreillyId, 0);
                        it.batchVariables(1, "https://www.manning.com", manningId, 0);
                    });
                    ctx.value("2");
                }
        );
    }

    @Test
    public void testUpdateByVersionFailed() {
        BookStore store1 = BookStoreDraft.$.produce(draft -> {
            draft.setId(oreillyId);
            draft.setWebsite("https://www.oreilly.com");
            draft.setVersion(0);
        });
        BookStore store2 = BookStoreDraft.$.produce(draft -> {
            draft.setId(manningId);
            draft.setWebsite("https://www.manning.com");
            draft.setVersion(1);
        });
        execute(
                new BookStore[] { store1, store2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, BookStore.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(BOOK_STORE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    int rowCount = operator.update(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(1, drafts.get(0).__get(BookStoreProps.VERSION.unwrap().getId()));
                    Assertions.assertEquals(0, drafts.get(1).__get(BookStoreProps.VERSION.unwrap().getId()));
                    return rowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK_STORE set WEBSITE = ?, VERSION = VERSION + 1 " +
                                        "where ID = ? and VERSION = ?"
                        );
                        it.batchVariables(0, "https://www.oreilly.com", oreillyId, 0);
                        it.batchVariables(1, "https://www.manning.com", manningId, 1);
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.OptimisticLockError.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot update the entity whose type is " +
                                        "\"org.babyfish.jimmer.sql.model.BookStore\" and id is \"" +
                                        manningId +
                                        "\" because of optimistic lock error"
                        );
                    });
                }
        );
    }

    @Test
    public void testByUserOptimisticLock() {
        BookStore store1 = BookStoreDraft.$.produce(draft -> {
            draft.setId(oreillyId);
            draft.setWebsite("https://www.oreilly.com");
            draft.setVersion(2);
        });
        BookStore store2 = BookStoreDraft.$.produce(draft -> {
            draft.setId(manningId);
            draft.setWebsite("https://www.manning.com");
            draft.setVersion(4);
        });
        execute(
                new BookStore[] { store1, store2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, BookStore.class, options -> {
                        options.userOptimisticLock = (BookStoreTable table, UserOptimisticLock.ValueExpressionFactory<BookStore> f) -> {
                            return f.newValue(BookStoreProps.VERSION)
                                    .minus(table.version())
                                    .le(4);
                        };
                    });
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(BOOK_STORE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    return operator.update(shapedEntityMap.iterator().next());
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK_STORE set WEBSITE = ?, VERSION = ? " +
                                        "where ID = ? and ? - VERSION <= ?"
                        );
                        it.batchVariables(0, "https://www.oreilly.com", 2, oreillyId, 2, 4);
                        it.batchVariables(1, "https://www.manning.com", 4, manningId, 4, 4);
                    });
                    ctx.value("2");
                }
        );
    }

    @Test
    public void testByUserOptimisticLockFailed() {
        BookStore store1 = BookStoreDraft.$.produce(draft -> {
            draft.setId(oreillyId);
            draft.setWebsite("https://www.oreilly.com");
            draft.setVersion(2);
        });
        BookStore store2 = BookStoreDraft.$.produce(draft -> {
            draft.setId(manningId);
            draft.setWebsite("https://www.manning.com");
            draft.setVersion(5);
        });
        execute(
                new BookStore[] { store1, store2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, BookStore.class, options -> {
                        options.userOptimisticLock = (BookStoreTable table, UserOptimisticLock.ValueExpressionFactory<BookStore> f) -> {
                            return f.newValue(BookStoreProps.VERSION)
                                    .minus(table.version())
                                    .le(4);
                        };
                    });
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(BOOK_STORE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    return operator.update(shapedEntityMap.iterator().next());
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK_STORE set WEBSITE = ?, VERSION = ? " +
                                        "where ID = ? and ? - VERSION <= ?"
                        );
                        it.batchVariables(0, "https://www.oreilly.com", 2, oreillyId, 2, 4);
                        it.batchVariables(1, "https://www.manning.com", 5, manningId, 5, 4);
                    });
                    ctx.throwable(
                            it -> it.message(
                                    "Save error caused by the path: \"<root>\": " +
                                            "Cannot update the entity whose type is " +
                                            "\"org.babyfish.jimmer.sql.model.BookStore\" and id is " +
                                            "\"2fa3955e-3e83-49b9-902e-0465c109c779\" " +
                                            "because of optimistic lock error"
                            )
                    );
                }
        );
    }

    @Test
    public void upsertById() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("09615006-bfdc-45e1-bc65-8256c294dfb4"));
            draft.setName("Kotlin in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("49.9"));
        });
        execute(
                new Book[] { book1, book2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, Book.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = new ShapedEntityMap<>(BOOK_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    return operator.upsert(shapedEntityMap.iterator().next());
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("merge into BOOK(ID, NAME, EDITION, PRICE) key(ID) values(?, ?, ?, ?)");
                        it.batchVariables(
                                0,
                                graphQLInActionId2,
                                "GraphQL in Action",
                                2,
                                new BigDecimal("59.9")
                        );
                        it.batchVariables(
                                1,
                                UUID.fromString("09615006-bfdc-45e1-bc65-8256c294dfb4"),
                                "Kotlin in Action",
                                1,
                                new BigDecimal("49.9")
                        );
                    });
                    ctx.value("2");
                }
        );
    }

    @SuppressWarnings("unchecked")
    private <T, R> void execute(
            T[] entities,
            BiFunction<Connection, List<DraftSpi>, R> block,
            Consumer<AbstractMutationTest.ExpectDSLWithValue<R>> ctxBlock
    ) {
        Internal.produceList(
                ImmutableType.get(entities.getClass().getComponentType()),
                Arrays.asList(entities),
                drafts -> {
                    connectAndExpect(
                            con -> {
                                return block.apply(con, (List<DraftSpi>) drafts);
                            },
                            ctxBlock
                    );
                }
        );
    }

    private static Operator operator(
            JSqlClient sqlClient,
            Connection con,
            Class<?> entityType
    ) {
        return operator(sqlClient, con, entityType, null);
    }

    private static Operator operator(
            JSqlClient sqlClient,
            Connection con,
            Class<?> entityType,
            Consumer<SaveOptionsImpl> optionsBlock
    ) {
        SaveOptionsImpl options = new SaveOptionsImpl((JSqlClientImplementor) sqlClient);
        if (optionsBlock != null) {
            optionsBlock.accept(options);
        }
        return new Operator(
                new SaveContext(
                        options,
                        con,
                        ImmutableType.get(entityType)
                )
        );
    }

    @Override
    protected JSqlClient getSqlClient() {
        return super.getSqlClient(builder -> builder.setIdGenerator(null));
    }
}
