package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.CascadeAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

public class DeleteTest extends AbstractMutationTest {

    @Test
    public void testDeleteBookStore() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select id from BOOK where STORE_ID in(?)");
                        it.variables(manningId);
                    });
                    ctx.throwable(it -> {
                        it.type(ExecutionException.class);
                        it.message(
                                "Cannot delete entities whose type are \"org.babyfish.jimmer.sql.model.BookStore\" " +
                                        "because there are some child entities whose type are \"org.babyfish.jimmer.sql.model.Book\", " +
                                        "these child entities use the association property \"org.babyfish.jimmer.sql.model.Book.store\" " +
                                        "to reference current entities."
                        );
                    });
                }
        );
    }

    @Test
    public void testDeleteBookStoreOnDeleteSetNull() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).configure(cfg -> {
                    cfg.setCascadeAction(
                            BookTable.class,
                            it -> it.store(),
                            CascadeAction.SET_NULL
                    );
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = null where STORE_ID in(?)");
                        it.variables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_STORE where ID in(?)");
                        it.variables(manningId);
                    });
                    ctx
                            .totalRowCount(4)
                            .rowCount(AffectedTable.of(Book.class), 3)
                            .rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
    }

    @Test
    public void testDeleteBookStoreOnDeleteCascade() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).configure(cfg -> {
                    cfg.setCascadeAction(
                            BookTable.class,
                            it -> it.store(),
                            CascadeAction.CASCADE
                    );
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select id from BOOK where STORE_ID in(?)");
                        it.variables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID in(?, ?, ?)");
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in(?, ?, ?)");
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_STORE where ID in(?)");
                        it.variables(manningId);
                    });
                    ctx.totalRowCount(7);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 3);
                    ctx.rowCount(AffectedTable.of(BookTableEx.class, BookTableEx::authors), 3);
                }
        );
    }

    @Test
    public void testBook() {
        UUID nonExistingId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        executeAndExpectResult(
                getSqlClient().getEntities().batchDeleteCommand(
                        Book.class,
                        Arrays.asList(
                            learningGraphQLId1,
                            learningGraphQLId2,
                            nonExistingId
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID in(?, ?, ?)");
                        it.variables(learningGraphQLId1, learningGraphQLId2, nonExistingId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in(?, ?, ?)");
                        it.variables(learningGraphQLId1, learningGraphQLId2, nonExistingId);
                    });
                    ctx.totalRowCount(6);
                    ctx.rowCount(AffectedTable.of(BookTableEx.class, BookTableEx::authors), 4);
                    ctx.rowCount(AffectedTable.of(Book.class), 2);
                }
        );
    }

    @Test
    public void testDeleteAuthor() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        Author.class,
                        alexId
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where AUTHOR_ID in(?)");
                        it.variables(alexId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from AUTHOR where ID in(?)");
                        it.variables(alexId);
                    });
                    ctx.totalRowCount(4);
                    ctx.rowCount(AffectedTable.of(Author.class), 1);
                    ctx.rowCount(AffectedTable.of(AuthorTableEx.class, AuthorTableEx::books), 3);
                }
        );
    }
}
