package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class PreHandlerTest extends AbstractQueryTest {

    @Test
    public void testInsertWithIdAndInterceptor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("6abc79f3-5e9c-4bfa-81f0-22967125be87"));
            draft.setName("SQL in Action");
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("a17b3c77-4c3d-4101-825f-dac736fcfd5a"));
            draft.setName("Java in Action");
        });
        execute(
                new Book[] { book1, book2 },
                builder -> {
                    builder.addDraftInterceptor(new DraftInterceptor<Book, BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft, @Nullable Book original) {
                            draft.setEdition(1);
                        }
                    });
                },
                PreHandlerTest::insertOnlySaveContextContext,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"6abc79f3-5e9c-4bfa-81f0-22967125be87\"," +
                                    "--->--->\"name\":\"SQL in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"a17b3c77-4c3d-4101-825f-dac736fcfd5a\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                }
        );
    }

    @Test
    public void testInsertWithIdAndTrigger() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("6abc79f3-5e9c-4bfa-81f0-22967125be87"));
            draft.setName("SQL in Action");
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("a17b3c77-4c3d-4101-825f-dac736fcfd5a"));
            draft.setName("Java in Action");
        });
        execute(
                new Book[] { book1, book2 },
                builder -> {
                    builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.setEdition(1);
                        }
                    });
                },
                PreHandlerTest::insertOnlySaveContextContext,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"6abc79f3-5e9c-4bfa-81f0-22967125be87\"," +
                                    "--->--->\"name\":\"SQL in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"a17b3c77-4c3d-4101-825f-dac736fcfd5a\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                }
        );
    }

    @Test
    public void testInsertWithKeyAndInterceptor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("SQL in Action");
            draft.setEdition(1);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
        });
        setAutoIds(
                Book.class,
                UUID.fromString("6abc79f3-5e9c-4bfa-81f0-22967125be87"),
                UUID.fromString("a17b3c77-4c3d-4101-825f-dac736fcfd5a")
        );
        execute(
                new Book[] { book1, book2 },
                builder -> {
                    builder.addDraftInterceptor(new DraftInterceptor<Book, BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft, @Nullable Book original) {
                            draft.setPrice(new BigDecimal("39.9"));
                        }
                    });
                },
                PreHandlerTest::insertOnlySaveContextContext,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"6abc79f3-5e9c-4bfa-81f0-22967125be87\"," +
                                    "--->--->\"name\":\"SQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":39.9" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"a17b3c77-4c3d-4101-825f-dac736fcfd5a\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":39.9" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                }
        );
    }

    @Override
    protected JSqlClient getSqlClient(Consumer<JSqlClient.Builder> block) {
        return super.getSqlClient(builder -> {
            UserIdGenerator<?> idGenerator = this::autoId;
            builder.setIdGenerator(Book.class, idGenerator);
            block.accept(builder);
        });
    }

    @Test
    public void testInsertWithKeyAndTrigger() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("SQL in Action");
            draft.setEdition(1);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
        });
        setAutoIds(
                Book.class,
                UUID.fromString("6abc79f3-5e9c-4bfa-81f0-22967125be87"),
                UUID.fromString("a17b3c77-4c3d-4101-825f-dac736fcfd5a")
        );
        execute(
                new Book[] { book1, book2 },
                builder -> {
                    builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.setPrice(new BigDecimal("39.9"));
                        }
                    });
                },
                PreHandlerTest::insertOnlySaveContextContext,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"6abc79f3-5e9c-4bfa-81f0-22967125be87\"," +
                                    "--->--->\"name\":\"SQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":39.9" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"a17b3c77-4c3d-4101-825f-dac736fcfd5a\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":39.9" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                }
        );
    }

    @Test
    public void testUpdateWithIdAndProcessor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setName("SQL in Action");
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setName("Java in Action");
        });
        execute(
                new Book[] { book1, book2 },
                builder -> {
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.setEdition(1);
                        }
                    });
                },
                PreHandlerTest::updateOnlySaveContextContext,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"SQL in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpdateWithIdAndInterceptor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("bbcdb5f0-8d48-4f31-87fe-90de54e3898a"));
            draft.setPrice(new BigDecimal("59.9"));
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.setDialect(new NoAnyEqualityDialect());
                    builder.addDraftInterceptor(new SetPriceInterceptor());
                },
                PreHandlerTest::updateOnlySaveContextContext,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(graphQLInActionId1, graphQLInActionId2, book3.id());
                },
                handler -> {
                    assertContentEquals(
                            "{[id, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"price\":180.00" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"price\":181.00" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpdateWithIdAndChildren() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setName("SQL in Action");
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setName("Java in Action");
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("bbcdb5f0-8d48-4f31-87fe-90de54e3898a"));
            draft.setName("Java in Action");
            draft.setPrice(new BigDecimal("59.9"));
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.setDialect(new NoAnyEqualityDialect());
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.addIntoAuthors(author -> author.setId(alexId));
                        }
                    });
                },
                PreHandlerTest::updateOnlySaveContextContext,
                ctx -> {
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"SQL in Action\"," +
                                    "--->--->\"price\":59.9," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"price\":59.9," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"bbcdb5f0-8d48-4f31-87fe-90de54e3898a\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"price\":59.9," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpdateWithIdAndTrigger() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setName("SQL in Action");
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setName("Java in Action");
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("bbcdb5f0-8d48-4f31-87fe-90de54e3898a"));
            draft.setName("Java in Action");
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.setDialect(new NoAnyEqualityDialect());
                    builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
                },
                PreHandlerTest::updateOnlySaveContextContext,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(graphQLInActionId1, graphQLInActionId2, book3.id());
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"SQL in Action\"" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"Java in Action\"" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpdateWithKeyAndProcessor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
        });
        execute(
                new Book[] { book1, book2 },
                builder -> {
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.setPrice(BigDecimal.TEN);
                        }
                    });
                },
                PreHandlerTest::updateOnlySaveContextContext,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{" +
                                    "--->[name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":10" +
                                    "--->}, {" +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":10" +
                                    "--->}]" +
                                    "}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpdateWithKeyAndInterceptor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
        });Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.addDraftInterceptor(new SetPriceInterceptor());
                },
                PreHandlerTest::updateOnlySaveContextContext,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?), (?, ?))"
                    ).variables(
                            "GraphQL in Action", 1,
                            "GraphQL in Action", 2,
                            "Java in Action", 1
                    );
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":180.00" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":181.00" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpdateWithKeyAndChildren() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.addIntoAuthors(author -> author.setId(alexId));
                        }
                    });
                },
                PreHandlerTest::updateOnlySaveContextContext,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{[name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":59.9," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}, {" +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":59.9," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}, {" +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":59.9," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpdateWithTrigger() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
        });Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
                },
                PreHandlerTest::updateOnlySaveContextContext,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?), (?, ?))"
                    ).variables(
                            "GraphQL in Action", 1,
                            "GraphQL in Action", 2,
                            "Java in Action", 1
                    );
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithIdAndProcessor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1"));
            draft.setPrice(new BigDecimal("59.9"));
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.setPrice(BigDecimal.TEN);
                        }
                    });
                },
                null,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{[id, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"price\":10" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"price\":10" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"price\":10" +
                                    "--->}" +
                                    "]}",
                            handler.mergedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithIdAndWeakDatabase() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setName("SQL in Action+");
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setName("GraphQL in Action+");
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1"));
            draft.setName("Java in Action");
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.setDialect(new WeakDatabaseDialect());
                },
                null,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(graphQLInActionId1, graphQLInActionId2, book3.id());
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"name\":\"Java in Action\"" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{[id, name]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"SQL in Action+\"" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action+\"" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithIdAndInterceptor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1"));
            draft.setPrice(new BigDecimal("59.9"));
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.setDialect(new NoAnyEqualityDialect());
                    builder.addDraftInterceptor(new SetPriceInterceptor());
                },
                null,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(graphQLInActionId1, graphQLInActionId2, book3.id());
                },
                handler -> {
                    assertContentEquals(
                            "{[id, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"price\":100" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{[id, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"price\":180.00" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"price\":181.00" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithIdAndChildren() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setName("SQL in Action+");
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setName("GraphQL in Action+");
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1"));
            draft.setName("Java in Action");
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {

                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.setAuthorIds(Collections.singletonList(alexId));
                        }
                    });
                },
                null,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{[id, name]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"SQL in Action+\"," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action+\"," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "}]}",
                            handler.mergedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithIdAndTrigger() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
            draft.setName("SQL in Action+");
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setName("GraphQL in Action+");
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1"));
            draft.setName("Java in Action");
        });
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.setDialect(new NoAnyEqualityDialect());
                    builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
                },
                null,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(graphQLInActionId1, graphQLInActionId2, book3.id());
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"name\":\"Java in Action\"" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{[id, name]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"SQL in Action+\"" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action+\"" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithKeyAndProcessor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
        });
        setAutoIds(
                Book.class,
                UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1"),
                UUID.fromString("87941f14-73ce-4a9f-afb5-a6b024bfb68a"),
                UUID.fromString("9dbc6982-fc41-4e18-a7b3-b347d04f6f11")
        );
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.setPrice(BigDecimal.TEN);
                        }
                    });
                },
                null,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?), (?, ?))"
                    );
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition, price]: [{" +
                                    "--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->\"name\":\"Java in Action\"," +
                                    "--->\"edition\":1," +
                                    "--->\"price\":10}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{[id, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":10" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":10" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithKeyAndChildren() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        setAutoIds(
                Book.class,
                UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1"),
                UUID.fromString("87941f14-73ce-4a9f-afb5-a6b024bfb68a"),
                UUID.fromString("9dbc6982-fc41-4e18-a7b3-b347d04f6f11")
        );
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.setAuthorIds(Collections.singletonList(alexId));
                        }
                    });
                },
                null,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?), (?, ?))"
                    );
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":59.9," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{" +
                                    "--->[id, price]: [" +
                                    "--->--->{\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":59.9," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":59.9,\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithKeyAndWeakDatabase() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
        });
        setAutoIds(
                Book.class,
                UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1")
        );
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.setDialect(new WeakDatabaseDialect());
                },
                null,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?), (?, ?))"
                    ).variables(
                            "GraphQL in Action", 1,
                            "GraphQL in Action", 2,
                            "Java in Action", 1
                    );
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{[id]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithKeyAndInterceptor() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        setAutoIds(
                Book.class,
                UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1")
        );
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.addDraftInterceptor(new SetPriceInterceptor());
                },
                null,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?), (?, ?))"
                    ).variables(
                            "GraphQL in Action", 1,
                            "GraphQL in Action", 2,
                            "Java in Action", 1
                    );
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":100" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{[id, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":180.00" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":181.00" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testUpsertWithKeyAndTrigger() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
            draft.setPrice(new BigDecimal("59.9"));
        });
        setAutoIds(
                Book.class,
                UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1")
        );
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {
                    builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
                },
                null,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?), (?, ?))"
                    ).variables(
                            "GraphQL in Action", 1,
                            "GraphQL in Action", 2,
                            "Java in Action", 1
                    );
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":59.9" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{[id, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":59.9" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":59.9" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    // If the `@OneToMany.isTargetTransferable` is false,
    // The back reference foreign key of child object must be fetched.
    @Test
    public void testLockedParent() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(1);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(1);
        });
        setAutoIds(
                Book.class,
                UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1")
        );
        execute(
                new Book[] { book1, book2, book3 },
                builder -> {},
                (sqlClient, con) -> {
                    SaveContext parent = new SaveContext(
                            new SaveOptionsImpl(sqlClient),
                            con,
                            ImmutableType.get(BookStore.class)
                    );
                    return parent.prop(BookStoreProps.BOOKS.unwrap());
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?), (?, ?))"
                    ).variables(
                            "GraphQL in Action", 1,
                            "GraphQL in Action", 2,
                            "Java in Action", 1
                    );
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{[id]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    @Test
    public void testMixedShape() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId1);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(UUID.fromString("7e152125-7b3e-4194-ad67-6de7aab4a7f9"));
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book3 = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        Book book4 = BookDraft.$.produce(draft -> {
            draft.setName("Java in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("59.9"));
        });
        setAutoIds(
                Book.class,
                UUID.fromString("3589bfb2-b44d-4b1c-b5d1-1572285b6dc1")
        );
        execute(
                new Book[] { book1, book2, book3, book4 },
                builder -> {
                    builder.setDialect(new NoAnyEqualityDialect());
                    builder.addDraftInterceptor(new SetPriceInterceptor());
                },
                null,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"
                    ).variables("GraphQL in Action", 2, "Java in Action", 2);
                },
                handler -> {
                    assertContentEquals(
                            "{[id, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"7e152125-7b3e-4194-ad67-6de7aab4a7f9\"," +
                                    "--->--->\"price\":100" +
                                    "--->}" +
                                    "], [id, name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"3589bfb2-b44d-4b1c-b5d1-1572285b6dc1\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":100" +
                                    "--->}" +
                                    "]}",
                            handler.insertedMap()
                    );
                    assertContentEquals(
                            "{[id, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":181.00" +
                                    "--->}" +
                                    "]}",
                            handler.updatedMap()
                    );
                }
        );
    }

    private <T> void execute(
            T[] entities,
            Consumer<JSqlClient.Builder> builderCfgBlock,
            BiFunction<JSqlClientImplementor, Connection, SaveContext> saveCtxCreator,
            Consumer<QueryTestContext<PreHandler>> ctxBlock,
            Consumer<PreHandler> handlerBlock
    ) {
        Internal.produceList(
                ImmutableType.get(entities.getClass().getComponentType()),
                Arrays.asList(entities),
                drafts -> {
                    connectAndExpect(
                            con  -> {
                                JSqlClient sqlClient = getSqlClient(builderCfgBlock);
                                SaveContext saveContext;
                                if (saveCtxCreator != null) {
                                    saveContext = saveCtxCreator.apply((JSqlClientImplementor) sqlClient, con);
                                } else {
                                    saveContext = new SaveContext(
                                            new SaveOptionsImpl((JSqlClientImplementor) sqlClient),
                                            con,
                                            ImmutableType.get(Book.class)
                                    );
                                }
                                PreHandler handler = PreHandler.of(saveContext);
                                for (Draft draft : drafts) {
                                    handler.add((DraftSpi) draft);
                                }
                                handler.insertedMap();
                                handler.updatedMap();
                                handler.mergedMap();
                                return handler;
                            },
                            ctx -> {
                                ctx.row(0, preHandler -> {
                                    ctxBlock.accept(ctx);
                                    handlerBlock.accept(preHandler);
                                });
                            }
                    );
                }
        );
    }
    
    private static SaveContext insertOnlySaveContextContext(JSqlClientImplementor sqlClient, Connection con) {
        SaveOptionsImpl options = new SaveOptionsImpl(sqlClient);
        options.mode = SaveMode.INSERT_ONLY;
        return new SaveContext(
                options,
                con,
                ImmutableType.get(Book.class)
        );
    }

    private static SaveContext updateOnlySaveContextContext(JSqlClientImplementor sqlClient, Connection con) {
        SaveOptionsImpl options = new SaveOptionsImpl(sqlClient);
        options.mode = SaveMode.UPDATE_ONLY;
        return new SaveContext(
                options,
                con,
                ImmutableType.get(Book.class)
        );
    }

    private static class NoAnyEqualityDialect extends H2Dialect {
        @Override
        public boolean isAnyEqualityOfArraySupported() {
            return false;
        }
    }

    private static class WeakDatabaseDialect extends NoAnyEqualityDialect {

        @Override
        public boolean isUpsertSupported() {
            return false;
        }
    }

    private static class SetPriceInterceptor implements DraftInterceptor<Book, BookDraft> {

        @Override
        public void beforeSave(@NotNull BookDraft draft, @Nullable Book original) {
            if (original == null) {
                draft.setPrice(new BigDecimal(100));
            } else {
                draft.setPrice(original.price().add(new BigDecimal(100)));
            }
        }

        @Override
        public Collection<TypedProp<Book, ?>> dependencies() {
            return Collections.singletonList(BookProps.PRICE);
        }
    }
}
