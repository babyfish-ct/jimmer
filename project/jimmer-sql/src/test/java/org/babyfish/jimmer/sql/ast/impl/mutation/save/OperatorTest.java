package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.embedded.Machine;
import org.babyfish.jimmer.sql.model.embedded.MachineDraft;
import org.babyfish.jimmer.sql.model.embedded.MachineProps;
import org.babyfish.jimmer.sql.model.hr.*;
import org.babyfish.jimmer.sql.model.inheritance.Administrator;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorDraft;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorProps;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
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

    private static final Set<ImmutableProp> ADMINISTRATOR_KEY_PROPS =
            Collections.singleton(
                    AdministratorProps.NAME.unwrap()
            );

    private static final Set<ImmutableProp> MACHINE_KEY_PROPS =
            Collections.singleton(
                    MachineProps.LOCATION.unwrap()
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
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, BOOK_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.insert(shapedEntityMap.iterator().next());
                    return operator.ctx.affectedRowCountMap;
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
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Book.class)));
                    });
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
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, DEPARTMENT_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.insert(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(100L, drafts.get(0).__get(DepartmentProps.ID.unwrap().getId()));
                    Assertions.assertEquals(101L, drafts.get(1).__get(DepartmentProps.ID.unwrap().getId()));
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into DEPARTMENT(NAME) values(?)");
                        it.batchVariables(0, "Engine");
                        it.batchVariables(1, "Wheel");
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Department.class)));
                    });
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
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, TREE_NODE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                   operator.insert(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(100L, drafts.get(0).__get(DepartmentProps.ID.unwrap().getId()));
                    Assertions.assertEquals(101L, drafts.get(1).__get(DepartmentProps.ID.unwrap().getId()));
                    return operator.ctx.affectedRowCountMap;
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
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(TreeNode.class)));
                    });
                }
        );
    }

    @Test
    public void testInsertWithLogicalDeleted() {
        LocalDateTime time = LocalDateTime.of(2024, 6, 6, 22, 13);
        Administrator administrator1 = AdministratorDraft.$.produce(draft -> {
            draft.setName("Zeus");
            draft.setCreatedTime(time);
            draft.setModifiedTime(time);
        });
        Administrator administrator2 = AdministratorDraft.$.produce(draft -> {
            draft.setName("Hades");
            draft.setCreatedTime(time);
            draft.setModifiedTime(time);
        });
        execute(
                new Administrator[] { administrator1, administrator2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, Administrator.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, ADMINISTRATOR_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.insert(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(100L, drafts.get(0).__get(AdministratorProps.ID.unwrap().getId()));
                    Assertions.assertEquals(101L, drafts.get(1).__get(AdministratorProps.ID.unwrap().getId()));
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ADMINISTRATOR(" +
                                        "--->NAME, CREATED_TIME, MODIFIED_TIME, DELETED" +
                                        ") values(?, ?, ?, ?)"
                        );
                        it.batchVariables(0, "Zeus", time, time, false);
                        it.batchVariables(1, "Hades", time, time, false);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Administrator.class)));
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
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, BOOK_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.update(null, null, shapedEntityMap.iterator().next());
                    return operator.ctx.affectedRowCountMap;
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
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Book.class)));
                    });
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
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, BOOK_STORE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.update(null, null, shapedEntityMap.iterator().next());
                    Assertions.assertEquals(1, drafts.get(0).__get(BookStoreProps.VERSION.unwrap().getId()));
                    Assertions.assertEquals(1, drafts.get(1).__get(BookStoreProps.VERSION.unwrap().getId()));
                    return operator.ctx.affectedRowCountMap;
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
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(BookStore.class)));
                    });
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
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, BOOK_STORE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.update(null, null, shapedEntityMap.iterator().next());
                    Assertions.assertEquals(1, drafts.get(0).__get(BookStoreProps.VERSION.unwrap().getId()));
                    Assertions.assertEquals(0, drafts.get(1).__get(BookStoreProps.VERSION.unwrap().getId()));
                    return operator.ctx.affectedRowCountMap;
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
                            return f.newNumber(BookStoreProps.VERSION)
                                    .minus(table.version())
                                    .le(4);
                        };
                    });
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, BOOK_STORE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.update(null, null, shapedEntityMap.iterator().next());
                    return operator.ctx.affectedRowCountMap;
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
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(BookStore.class)));
                    });
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
                            return f.newNumber(BookStoreProps.VERSION)
                                    .minus(table.version())
                                    .le(4);
                        };
                    });
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, BOOK_STORE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.update(null, null, shapedEntityMap.iterator().next());
                    return operator.ctx.affectedRowCountMap;
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
    public void testUpsertById() {
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
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, BOOK_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.upsert(shapedEntityMap.iterator().next());
                    return operator.ctx.affectedRowCountMap;
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
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Book.class)));
                    });
                }
        );
    }

    @Test
    public void testUpsertByKey() {
        Machine machine1 = MachineDraft.$.produce(draft -> {
            draft.applyLocation(location -> location.setHost("localhost").setPort(8080));
            draft.setCpuFrequency(4);
            draft.setMemorySize(16);
            draft.setDiskSize(512);
            draft.applyDetail(detail -> {
                detail.setFactories(Collections.singletonMap("f-a", "factory-a"));
                detail.setPatents(Collections.singletonMap("p-b", "patent-b"));
            });
        });
        Machine machine2 = MachineDraft.$.produce(draft -> {
            draft.applyLocation(location -> location.setHost("localhost").setPort(443));
            draft.setCpuFrequency(2);
            draft.setMemorySize(8);
            draft.setDiskSize(256);
            draft.applyDetail(detail -> {
                detail.setFactories(Collections.singletonMap("f-x", "factory-x"));
                detail.setPatents(Collections.singletonMap("p-y", "patent-y"));
            });
        });
        execute(
                new Machine[] { machine1, machine2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(), con, Machine.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, MACHINE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.upsert(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(1L, drafts.get(0).__get(MachineProps.ID.unwrap().getId()));
                    Assertions.assertEquals(100L, drafts.get(1).__get(MachineProps.ID.unwrap().getId()));
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into MACHINE(" +
                                        "--->HOST, PORT, CPU_FREQUENCY, MEMORY_SIZE, DISK_SIZE, factory_map, patent_map" +
                                        ") key(HOST, PORT) values(" +
                                        "--->?, ?, ?, ?, ?, ? format json, ? format json" +
                                        ")"
                        );
                        it.batchVariables(
                                0,
                                "localhost",
                                8080,
                                4,
                                16,
                                512,
                                "{\"f-a\":\"factory-a\"}",
                                "{\"p-b\":\"patent-b\"}"
                        );
                        it.batchVariables(
                                1,
                                "localhost",
                                443,
                                2,
                                8,
                                256,
                                "{\"f-x\":\"factory-x\"}",
                                "{\"p-y\":\"patent-y\"}"
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Machine.class)));
                    });
                }
        );
    }

    @Test
    public void testUpsertByIdAndMySQL() {

        NativeDatabases.assumeNativeDatabase();

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
                NativeDatabases.MYSQL_DATA_SOURCE,
                new Book[] { book1, book2 },
                (con, drafts) -> {
                    Operator operator = operator(
                            getSqlClient(it -> {
                                it.setDialect(new MySqlDialect());
                                it.addScalarProvider(ScalarProvider.uuidByByteArray());
                            }),
                            con,
                            Book.class
                    );
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, BOOK_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.upsert(shapedEntityMap.iterator().next());
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(" +
                                        "--->ID, NAME, EDITION, PRICE" +
                                        ") values(" +
                                        "--->?, ?, ?, ?" +
                                        ") on duplicate key update " +
                                        "--->NAME = values(NAME), " +
                                        "--->EDITION = values(EDITION), " +
                                        "--->PRICE = values(PRICE)"
                        );
                        it.batchVariables(
                                0,
                                toByteArray(graphQLInActionId2),
                                "GraphQL in Action",
                                2,
                                new BigDecimal("59.9")
                        );
                        it.batchVariables(
                                1,
                                toByteArray(UUID.fromString("09615006-bfdc-45e1-bc65-8256c294dfb4")),
                                "Kotlin in Action",
                                1,
                                new BigDecimal("49.9")
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Book.class)));
                    });
                }
        );
    }

    @Test
    public void testUpsertByKeyAndMySql() {

        NativeDatabases.assumeNativeDatabase();

        Machine machine1 = MachineDraft.$.produce(draft -> {
            draft.applyLocation(location -> location.setHost("localhost").setPort(8080));
            draft.setCpuFrequency(4);
            draft.setMemorySize(16);
            draft.setDiskSize(512);
            draft.applyDetail(detail -> {
                detail.setFactories(Collections.singletonMap("f-a", "factory-a"));
                detail.setPatents(Collections.singletonMap("p-b", "patent-b"));
            });
        });
        Machine machine2 = MachineDraft.$.produce(draft -> {
            draft.applyLocation(location -> location.setHost("localhost").setPort(443));
            draft.setCpuFrequency(2);
            draft.setMemorySize(8);
            draft.setDiskSize(256);
            draft.applyDetail(detail -> {
                detail.setFactories(Collections.singletonMap("f-x", "factory-x"));
                detail.setPatents(Collections.singletonMap("p-y", "patent-y"));
            });
        });
        execute(
                NativeDatabases.MYSQL_DATA_SOURCE,
                new Machine[] { machine1, machine2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(it -> it.setDialect(new MySqlDialect())), con, Machine.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, MACHINE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.upsert(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(1L, drafts.get(0).__get(MachineProps.ID.unwrap().getId()));
                    Assertions.assertTrue((Long) drafts.get(1).__get(MachineProps.ID.unwrap().getId()) >= 100L);
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into MACHINE(" +
                                        "--->HOST, PORT, CPU_FREQUENCY, MEMORY_SIZE, DISK_SIZE, factory_map, patent_map" +
                                        ") values(" +
                                        "--->?, ?, ?, ?, ?, ?, ?" +
                                        ") on duplicate key update " +
                                        "--->CPU_FREQUENCY = values(CPU_FREQUENCY), " +
                                        "--->MEMORY_SIZE = values(MEMORY_SIZE), " +
                                        "--->DISK_SIZE = values(DISK_SIZE), " +
                                        "--->factory_map = values(factory_map), " +
                                        "--->patent_map = values(patent_map)"
                        );
                        it.batchVariables(
                                0,
                                "localhost",
                                8080,
                                4,
                                16,
                                512,
                                "{\"f-a\":\"factory-a\"}",
                                "{\"p-b\":\"patent-b\"}"
                        );
                        it.batchVariables(
                                1,
                                "localhost",
                                443,
                                2,
                                8,
                                256,
                                "{\"f-x\":\"factory-x\"}",
                                "{\"p-y\":\"patent-y\"}"
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Machine.class)));
                    });
                }
        );
    }

    @Test
    public void testUpsertByIdAndPostgres() {

        NativeDatabases.assumeNativeDatabase();

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
                NativeDatabases.POSTGRES_DATA_SOURCE,
                new Book[] { book1, book2 },
                (con, drafts) -> {
                    Operator operator = operator(
                            getSqlClient(it -> {
                                it.setDialect(new PostgresDialect());
                            }),
                            con,
                            Book.class
                    );
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, BOOK_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.upsert(shapedEntityMap.iterator().next());
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(" +
                                        "--->ID, NAME, EDITION, PRICE" +
                                        ") values(" +
                                        "--->?, ?, ?, ?" +
                                        ") on conflict(ID) " +
                                        "do update set " +
                                        "--->NAME = excluded.NAME, " +
                                        "--->EDITION = excluded.EDITION, " +
                                        "--->PRICE = excluded.PRICE"
                        );
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
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Book.class)));
                    });
                }
        );
    }

    @Test
    public void testUpsertByKeyAndMyPostgres() {

        NativeDatabases.assumeNativeDatabase();

        Machine machine1 = MachineDraft.$.produce(draft -> {
            draft.applyLocation(location -> location.setHost("localhost").setPort(8080));
            draft.setCpuFrequency(4);
            draft.setMemorySize(16);
            draft.setDiskSize(512);
            draft.applyDetail(detail -> {
                detail.setFactories(Collections.singletonMap("f-a", "factory-a"));
                detail.setPatents(Collections.singletonMap("p-b", "patent-b"));
            });
        });
        Machine machine2 = MachineDraft.$.produce(draft -> {
            draft.applyLocation(location -> location.setHost("localhost").setPort(443));
            draft.setCpuFrequency(2);
            draft.setMemorySize(8);
            draft.setDiskSize(256);
            draft.applyDetail(detail -> {
                detail.setFactories(Collections.singletonMap("f-x", "factory-x"));
                detail.setPatents(Collections.singletonMap("p-y", "patent-y"));
            });
        });
        execute(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                new Machine[] { machine1, machine2 },
                (con, drafts) -> {
                    Operator operator = operator(getSqlClient(it -> it.setDialect(new PostgresDialect())), con, Machine.class);
                    ShapedEntityMap<DraftSpi> shapedEntityMap = shapedEntityMap(operator, MACHINE_KEY_PROPS);
                    for (DraftSpi draft : drafts) {
                        shapedEntityMap.add(draft);
                    }
                    operator.upsert(shapedEntityMap.iterator().next());
                    Assertions.assertEquals(1L, drafts.get(0).__get(MachineProps.ID.unwrap().getId()));
                    Assertions.assertTrue((Long) drafts.get(1).__get(MachineProps.ID.unwrap().getId()) >= 100L);
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into MACHINE(" +
                                        "--->HOST, PORT, CPU_FREQUENCY, MEMORY_SIZE, DISK_SIZE, factory_map, patent_map" +
                                        ") values(" +
                                        "--->?, ?, ?, ?, ?, ?, ?" +
                                        ") on conflict(HOST, PORT) " +
                                        "do update set " +
                                        "--->CPU_FREQUENCY = excluded.CPU_FREQUENCY, " +
                                        "--->MEMORY_SIZE = excluded.MEMORY_SIZE, " +
                                        "--->DISK_SIZE = excluded.DISK_SIZE, " +
                                        "--->factory_map = excluded.factory_map, " +
                                        "--->patent_map = excluded.patent_map"
                        );
                        it.batchVariables(
                                0,
                                "localhost",
                                8080,
                                4,
                                16,
                                512,
                                toPgobject("{\"f-a\":\"factory-a\"}"),
                                toPgobject("{\"p-b\":\"patent-b\"}")
                        );
                        it.batchVariables(
                                1,
                                "localhost",
                                443,
                                2,
                                8,
                                256,
                                toPgobject("{\"f-x\":\"factory-x\"}"),
                                toPgobject("{\"p-y\":\"patent-y\"}")
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(2, map.get(AffectedTable.of(Machine.class)));
                    });
                }
        );
    }

    private <T, R> void execute(
            T[] entities,
            BiFunction<Connection, List<DraftSpi>, R> block,
            Consumer<AbstractMutationTest.ExpectDSLWithValue<R>> ctxBlock
    ) {
        execute(null, entities, block, ctxBlock);
    }

    @SuppressWarnings("unchecked")
    private <T, R> void execute(
            @Nullable DataSource dataSource,
            T[] entities,
            BiFunction<Connection, List<DraftSpi>, R> block,
            Consumer<AbstractMutationTest.ExpectDSLWithValue<R>> ctxBlock
    ) {
        Internal.produceList(
                ImmutableType.get(entities.getClass().getComponentType()),
                Arrays.asList(entities),
                drafts -> {
                    connectAndExpect(
                            dataSource,
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

    private static byte[] toByteArray(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    private static PGobject toPgobject(String value) {
        PGobject pgo = new PGobject();
        pgo.setType("jsonb");
        try {
            pgo.setValue(value);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return pgo;
    }

    @Override
    protected JSqlClient getSqlClient() {
        return super.getSqlClient(builder -> builder.setIdGenerator(null));
    }

    private ShapedEntityMap<DraftSpi> shapedEntityMap(Operator operator, Set<ImmutableProp> keyProps) {
        return new ShapedEntityMap<>(operator.ctx.options.getSqlClient(), keyProps, ImmutableProp::isColumnDefinition, SaveMode.UPSERT);
    }
}
