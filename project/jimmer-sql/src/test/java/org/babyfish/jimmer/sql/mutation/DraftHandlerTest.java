package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookProps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

public class DraftHandlerTest extends AbstractMutationTest {

    private JSqlClient sqlClient1 = getSqlClient(it -> {
        it.addDraftInterceptor(
                new DraftInterceptor<Book, BookDraft>() {

                    @Override
                    public void beforeSave(@NotNull BookDraft draft, @Nullable Book original) {

                    }

                    @Override
                    public Collection<TypedProp<Book, ?>> dependencies() {
                        return Collections.singleton(BookProps.EDITION);
                    }
                }
        );
    });

    private JSqlClient sqlClient2 = getSqlClient(it -> {
        it.addDraftInterceptor(
                new DraftInterceptor<Book, BookDraft>() {

                    @Override
                    public void beforeSave(@NotNull BookDraft draft, @Nullable Book original) {
                        if (original != null && !ImmutableObjects.isLoaded(draft, BookProps.PRICE)) {
                            draft.setPrice(original.price().add(new BigDecimal("7.77")));
                        }
                    }

                    @Override
                    public Collection<TypedProp<Book, ?>> dependencies() {
                        return Collections.singleton(BookProps.PRICE);
                    }
                }
        );
    });

    @Test
    public void testKeyOnlyDraftHandler() {
        executeAndExpectResult(
                sqlClient1
                        .getEntities()
                        .saveCommand(
                                BookDraft.$.produce(book -> {
                                    book.setId(Constants.graphQLInActionId3);
                                    book.setName("GraphQL in Action+");
                                })
                        ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables(Constants.graphQLInActionId3);
                        it.queryReason(QueryReason.INTERCEPTOR);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set NAME = ? where ID = ?");
                        it.variables("GraphQL in Action+", Constants.graphQLInActionId3);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                "--->\"name\":\"GraphQL in Action+\"" +
                                "}");
                        it.modified("{" +
                                "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                "--->\"name\":\"GraphQL in Action+\"" +
                                "}");
                    });
                }
        );
    }

    @Test
    public void testNonKeyOnlyDraftHandler() {
        executeAndExpectResult(
                sqlClient2
                        .getEntities()
                        .saveCommand(
                                BookDraft.$.produce(book -> {
                                    book.setId(Constants.graphQLInActionId3);
                                    book.setName("GraphQL in Action+");
                                })
                        ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                        "from BOOK tb_1_ where tb_1_.ID = ?"
                        );
                        it.variables(Constants.graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set NAME = ?, PRICE = ? where ID = ?");
                        it.variables("GraphQL in Action+", new BigDecimal("87.77"), Constants.graphQLInActionId3);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "--->\"name\":\"GraphQL in Action+\"" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "--->\"name\":\"GraphQL in Action+\"," +
                                        "--->\"price\":87.77" +
                                        "}"
                        );
                    });
                }
        );
    }
}
