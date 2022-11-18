package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.babyfish.jimmer.sql.common.Constants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MutateCacheTest extends AbstractQueryTest {

    private LambdaClient lambdaClient;

    private List<CacheOpRecord> cacheOpRecords = new ArrayList<>();

    @BeforeEach
    public void initialize() {
        cacheOpRecords.clear();
        lambdaClient = getLambdaClient(builder -> {
            builder.setEntityManager(
                    new EntityManager(
                            BookStore.class,
                            Book.class,
                            Author.class,
                            Country.class,
                            TreeNode.class
                    )
            );
            builder.setCaches(cfg ->
                    cfg.setCacheFactory(
                            new CacheFactory() {
                                @Override
                                public Cache<?, ?> createObjectCache(ImmutableType type) {
                                    return new CacheImpl<>(type);
                                }

                                @Override
                                public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
                                    return new CacheImpl<>(prop);
                                }

                                @Override
                                public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                                    return new CacheImpl<>(prop);
                                }
                            }
                    ).setCacheOperator(
                            (cache, key, reason) -> {
                                cacheOpRecords.add(new CacheOpRecord(cache, key));
                                cache.delete(key);
                            }
                    )
            );
        });
    }

    @Test
    public void testChangeBook() {
        executeAndExpect(
                lambdaClient.createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .allScalarFields()
                                            .store(
                                                    BookStoreFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                }
        );
        executeAndExpect(
                lambdaClient.createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .allScalarFields()
                                            .books(
                                                    BookFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.STORE_ID, tb_1_.ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?)"
                    );
                    it.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                }
        );
        lambdaClient.getTriggers().fireEntityTableChange(
                BookDraft.$.produce(book -> {
                    book.setId(graphQLInActionId3).setStore(store -> store.setId(UUID.fromString("00000000-0000-0000-0000-000000000000")));
                }),
                BookDraft.$.produce(book -> {
                    book.setId(graphQLInActionId3).setStore(store -> store.setId(oreillyId));
                }),
                null
        );
        Assertions.assertEquals(
                String.format(
                        "[Book-" + graphQLInActionId3 +
                                ", Book.store-" + graphQLInActionId3 +
                                ", BookStore.books-00000000-0000-0000-0000-000000000000" +
                                ", BookStore.books-" + oreillyId +
                                "]"
                ),
                cacheOpRecords.toString()
        );
        /*
        [Book-780bdf07-05af-48bf-9be9-f8c65236fecc,
        Book.store-780bdf07-05af-48bf-9be9-f8c65236fecc,
        BookStore.books-00000000-0000-0000-0000-000000000000,
        BookStore.books-d38c10da-6be8-4924-b9b9-5e81899612a0
        ]
         */
        executeAndExpect(
                lambdaClient.createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .allScalarFields()
                                            .store(
                                                    BookStoreFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_"
                    );
                }
        );
        executeAndExpect(
                lambdaClient.createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .allScalarFields()
                                            .books(
                                                    BookFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION from BOOK_STORE as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.ID from BOOK as tb_1_ where tb_1_.STORE_ID = ?"
                    );
                    it.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                }
        );
    }

    @Test
    public void testInsertMiddleTable() {
        executeAndExpect(
                lambdaClient.createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .allScalarFields()
                                            .authors(
                                                    AuthorFetcher.$.allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE from BOOK as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.BOOK_ID, tb_1_.AUTHOR_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                    it.statement(2).sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?, ?, ?)"
                    );
                }
        );
        executeAndExpect(
                lambdaClient.createQuery(AuthorTable.class, (q, author) -> {
                    return q.select(
                            author.fetch(
                                    AuthorFetcher.$
                                            .allScalarFields()
                                            .books(
                                                    BookFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.AUTHOR_ID, tb_1_.BOOK_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.AUTHOR_ID in (?, ?, ?, ?, ?)"
                    );
                    it.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                }
        );
        lambdaClient.getTriggers().fireMiddleTableInsert(
                BookProps.AUTHORS.unwrap(),
                graphQLInActionId3,
                danId,
                null
        );
        Assertions.assertEquals(
                "[Book.authors-" +
                        graphQLInActionId3 +
                        ", Author.books-" +
                        danId +
                        "]",
                cacheOpRecords.toString()
        );
        executeAndExpect(
                lambdaClient.createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .allScalarFields()
                                            .authors(
                                                    AuthorFetcher.$.allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE from BOOK as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.AUTHOR_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.BOOK_ID = ?"
                    ).variables(graphQLInActionId3);
                }
        );
        executeAndExpect(
                lambdaClient.createQuery(AuthorTable.class, (q, author) -> {
                    return q.select(
                            author.fetch(
                                    AuthorFetcher.$
                                            .allScalarFields()
                                            .books(
                                                    BookFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.BOOK_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.AUTHOR_ID = ?"
                    ).variables(danId);
                }
        );
    }

    @Test
    public void testInsertInverseMiddleTable() {
        executeAndExpect(
                lambdaClient.createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .allScalarFields()
                                            .authors(
                                                    AuthorFetcher.$.allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE from BOOK as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.BOOK_ID, tb_1_.AUTHOR_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.BOOK_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                    it.statement(2).sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?, ?, ?)"
                    );
                }
        );
        executeAndExpect(
                lambdaClient.createQuery(AuthorTable.class, (q, author) -> {
                    return q.select(
                            author.fetch(
                                    AuthorFetcher.$
                                            .allScalarFields()
                                            .books(
                                                    BookFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.AUTHOR_ID, tb_1_.BOOK_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.AUTHOR_ID in (?, ?, ?, ?, ?)"
                    );
                    it.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                }
        );
        lambdaClient.getTriggers().fireMiddleTableInsert(
                AuthorProps.BOOKS.unwrap(),
                danId,
                graphQLInActionId3,
                null
        );
        Assertions.assertEquals(
                "[Book.authors-" +
                        graphQLInActionId3 +
                        ", Author.books-" +
                        danId +
                        "]",
                cacheOpRecords.toString()
        );
        executeAndExpect(
                lambdaClient.createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .allScalarFields()
                                            .authors(
                                                    AuthorFetcher.$.allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE from BOOK as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.AUTHOR_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.BOOK_ID = ?"
                    ).variables(graphQLInActionId3);
                }
        );
        executeAndExpect(
                lambdaClient.createQuery(AuthorTable.class, (q, author) -> {
                    return q.select(
                            author.fetch(
                                    AuthorFetcher.$
                                            .allScalarFields()
                                            .books(
                                                    BookFetcher.$
                                                            .allScalarFields()
                                            )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.BOOK_ID " +
                                    "from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.AUTHOR_ID = ?"
                    ).variables(danId);
                }
        );
    }

    @Test
    public void testChangeTreeNode() {
        executeAndExpect(
                lambdaClient.createQuery(TreeNodeTable.class, (q, treeNode) -> {
                    q.where(treeNode.parent().isNull());
                    return q.select(
                            treeNode.fetch(
                                    TreeNodeFetcher.$.childNodes(
                                            TreeNodeFetcher.$.allScalarFields()
                                    )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.NODE_ID " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID is null"
                    );
                    it.statement(1).sql(
                            "select tb_1_.NODE_ID " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID = ? " +
                                    "order by tb_1_.NODE_ID asc"
                    );
                    it.statement(2).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.NODE_ID in (?, ?)"
                    ).variables(2L, 9L);
                }
        );
        executeAndExpect(
                lambdaClient.createQuery(TreeNodeTable.class, (q, treeNode) -> {
                    q.where(treeNode.parent().isNull());
                    return q.select(
                            treeNode.fetch(
                                    TreeNodeFetcher.$.childNodes(
                                            TreeNodeFetcher.$.allScalarFields()
                                    )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.NODE_ID " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID is null"
                    );
                }
        );
        lambdaClient.getTriggers().fireEntityTableChange(
                TreeNodeDraft.$.produce(treeNode -> {
                    treeNode.setParent(parent -> {
                        parent.setId(1L);
                    });
                }),
                TreeNodeDraft.$.produce(treeNode -> {
                    treeNode.setId(9L).setParent((TreeNode) null);
                }),
                null
        );
        executeAndExpect(
                lambdaClient.createQuery(TreeNodeTable.class, (q, treeNode) -> {
                    q.where(treeNode.parent().isNull());
                    return q.select(
                            treeNode.fetch(
                                    TreeNodeFetcher.$.childNodes(
                                            TreeNodeFetcher.$.allScalarFields()
                                    )
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.NODE_ID " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID is null"
                    );
                    it.statement(1).sql(
                            "select tb_1_.NODE_ID " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID = ? " +
                                    "order by tb_1_.NODE_ID asc"
                    );
                    it.statement(2).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.NODE_ID = ?"
                    ).variables(9L);
                }
        );
    }

    private static class CacheOpRecord {

        final LocatedCache<?, ?> cache;

        private Object key;

        public CacheOpRecord(LocatedCache<?, ?> cache, Object key) {
            this.cache = cache;
            this.key = key;
        }

        @Override
        public String toString() {
            ImmutableType type = cache.getType();
            ImmutableProp prop = cache.getProp();
            if (type != null) {
                return type.getJavaClass().getSimpleName() + "-" + key;
            }
            return prop.getDeclaringType().getJavaClass().getSimpleName() + "." + prop.getName() + "-" + key;
        }
    }
}
