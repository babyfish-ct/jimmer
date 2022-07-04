package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.fetcher.impl.DataLoader;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.babyfish.jimmer.sql.common.Constants.graphQLInActionId2;

public class DataLoaderWithCacheTest extends AbstractQueryTest {

    private SqlClient sqlClient;

    @BeforeEach
    public void initialize() {
        sqlClient = getSqlClient(builder -> {
            builder.setCaches(cfg -> {
                cfg.setCacheFactory(new CacheFactory() {
                    @Override
                    public Cache<?, ?> createObjectCache(ImmutableType type) {
                        return new CacheImpl<>();
                    }

                    @Override
                    public Cache<?, ?> createAssociatedIdCache(ImmutableProp type) {
                        return new CacheImpl<>();
                    }

                    @Override
                    public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp type) {
                        return new CacheImpl<>();
                    }
                });
            });
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void loadParentId() {
        Fetcher<Book> fetcher = BookFetcher.$.store();
        List<ImmutableSpi> books = (List<ImmutableSpi>) (List<?>) Arrays.asList(
                BookDraft.$.produce(book ->
                        book
                                .setId(learningGraphQLId1)
                                .setStore(store -> store.setId(oreillyId))
                ),
                BookDraft.$.produce(book ->
                        book.setId(learningGraphQLId2)
                ),
                BookDraft.$.produce(book ->
                        book
                                .setId(graphQLInActionId1)
                                .setStore(store -> store.setId(manningId))
                ),
                BookDraft.$.produce(book ->
                        book.setId(graphQLInActionId2)
                )
        );
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            connectAndExpect(
                    con -> new DataLoader(sqlClient, con, fetcher.getFieldMap().get("store"))
                            .load(books),
                    ctx -> {
                        if (useSql) {
                            ctx.sql(
                                    "select tb_1_.ID, tb_1_.STORE_ID " +
                                            "from BOOK as tb_1_ " +
                                            "where tb_1_.ID in (?, ?) and tb_1_.STORE_ID is not null"
                            );
                        }
                        ctx.rows(1);
                        ctx.row(0, map -> {
                            expect(
                                    "{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}",
                                    map.get(books.get(0))
                            );
                            expect(
                                    "{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}",
                                    map.get(books.get(1))
                            );
                            expect(
                                    "{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}",
                                    map.get(books.get(2))
                            );
                            expect(
                                    "{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}",
                                    map.get(books.get(3))
                            );
                        });
                    }
            );
        }
    }
}
