package org.babyfish.jimmer.sql.ast.impl.mutation.save;

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
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
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
                saveOptions -> saveOptions.mode = SaveMode.INSERT_ONLY,
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
                saveOptions -> saveOptions.mode = SaveMode.INSERT_ONLY,
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
                saveOptions -> saveOptions.mode = SaveMode.INSERT_ONLY,
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

    private <T> void execute(
            T[] entities,
            Consumer<JSqlClient.Builder> builderCfgBlock,
            Consumer<SaveOptionsImpl> optionsCfgBlock,
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
                                SaveOptionsImpl options = new SaveOptionsImpl((JSqlClientImplementor) sqlClient);
                                optionsCfgBlock.accept(options);
                                PreHandler handler = PreHandler.of(
                                        new SaveContext(
                                                options,
                                                con,
                                                ImmutableType.get(Book.class)
                                        )
                                );
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
                saveOptions -> saveOptions.mode = SaveMode.INSERT_ONLY,
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
                saveOptions -> saveOptions.mode = SaveMode.UPDATE_ONLY,
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
                    builder.addDraftInterceptor(new DraftInterceptor<Book, BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft, @Nullable Book original) {
                            draft.setEdition(1);
                            if (original != null) {
                                draft.setPrice(original.price().add(new BigDecimal(100)));
                            }
                        }

                        @Override
                        public Collection<TypedProp<Book, ?>> dependencies() {
                            return Collections.singletonList(BookProps.PRICE);
                        }
                    });
                },
                saveOptions -> saveOptions.mode = SaveMode.UPDATE_ONLY,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(graphQLInActionId1, graphQLInActionId2, book3.id());
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"SQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":180.00" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
                                    "--->--->\"edition\":1," +
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
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.addIntoAuthors(author -> author.setId(alexId));
                        }
                    });
                },
                saveOptions -> saveOptions.mode = SaveMode.UPDATE_ONLY,
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(graphQLInActionId1, graphQLInActionId2, book3.id());
                },
                handler -> {
                    assertContentEquals(
                            "{[id, name, authors]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"SQL in Action\"," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"Java in Action\"," +
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
                    builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
                },
                saveOptions -> saveOptions.mode = SaveMode.UPDATE_ONLY,
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
                saveOptions -> saveOptions.mode = SaveMode.UPDATE_ONLY,
                ctx -> {},
                handler -> {
                    assertContentEquals(
                            "{[name, edition, price]: [" +
                                    "--->{" +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":10" +
                                    "--->}, {" +
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
                    builder.addDraftInterceptor(new DraftInterceptor<Book, BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft, @Nullable Book original) {
                            if (original != null) {
                                draft.setPrice(original.price().add(new BigDecimal(100)));
                            }
                        }

                        @Override
                        public Collection<TypedProp<Book, ?>> dependencies() {
                            return Collections.singletonList(BookProps.PRICE);
                        }
                    });
                },
                saveOptions -> saveOptions.mode = SaveMode.UPDATE_ONLY,
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
                    builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                        @Override
                        public void beforeSave(@NotNull BookDraft draft) {
                            draft.addIntoAuthors(author -> author.setId(alexId));
                        }
                    });
                },
                saveOptions -> saveOptions.mode = SaveMode.UPDATE_ONLY,
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
                            "{[id, name, edition, authors]: [" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"authors\":[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}]" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
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
                saveOptions -> saveOptions.mode = SaveMode.UPDATE_ONLY,
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
}
