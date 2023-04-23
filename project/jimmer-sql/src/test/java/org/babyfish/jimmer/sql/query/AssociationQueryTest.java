package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class AssociationQueryTest extends AbstractQueryTest {

    @Test()
    public void test() {

        executeAndExpect(
                getLambdaClient().createAssociationQuery(BookTableEx.class, BookTableEx::authors, (q, t) -> {
                    q.where(t.source().name().eq("Learning GraphQL"));
                    q.where(t.target().firstName().eq("Alex"));
                    return q.select(t);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.BOOK_ID, tb_1_.AUTHOR_ID " +
                                    "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                    "inner join BOOK tb_2_ on tb_1_.BOOK_ID = tb_2_.ID " +
                                    "inner join AUTHOR tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_2_.NAME = ? " +
                                    "and tb_3_.FIRST_NAME = ?"
                    );
                    ctx.variables("Learning GraphQL", "Alex");
                    ctx.rows(list -> {
                        Assertions.assertEquals(
                                list.stream()
                                        .map(it -> new Tuple2<>(it.source().id(), it.target().id()))
                                        .collect(Collectors.toSet()),
                                new HashSet<>(
                                        Arrays.asList(
                                                new Tuple2<>(learningGraphQLId1, alexId),
                                                new Tuple2<>(learningGraphQLId2, alexId),
                                                new Tuple2<>(learningGraphQLId3, alexId)
                                        )
                                )
                        );
                    });
                }
        );
    }

    @Test
    public void testInverse() {
        executeAndExpect(
                getLambdaClient().createAssociationQuery(AuthorTableEx.class, AuthorTableEx::books, (q, t) -> {
                    q.where(t.source().firstName().eq("Alex"));
                    q.where(t.target().name().eq("Learning GraphQL"));
                    return q.select(t);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.AUTHOR_ID, tb_1_.BOOK_ID " +
                                    "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                    "inner join AUTHOR tb_2_ on tb_1_.AUTHOR_ID = tb_2_.ID " +
                                    "inner join BOOK tb_3_ on tb_1_.BOOK_ID = tb_3_.ID " +
                                    "where tb_2_.FIRST_NAME = ? " +
                                    "and tb_3_.NAME = ?"
                    );
                    ctx.variables("Alex", "Learning GraphQL");
                    ctx.rows(list -> {
                        Assertions.assertEquals(
                                list.stream()
                                        .map(it -> new Tuple2<>(it.source().id(), it.target().id()))
                                        .collect(Collectors.toSet()),
                                new HashSet<>(
                                        Arrays.asList(
                                                new Tuple2<>(alexId, learningGraphQLId1),
                                                new Tuple2<>(alexId, learningGraphQLId2),
                                                new Tuple2<>(alexId, learningGraphQLId3)
                                        )
                                )
                        );
                    });
                }
        );
    }

    @Test
    public void testSubQuery() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book.id().in(
                                    getLambdaClient().createAssociationSubQuery(
                                            q,
                                            BookTableEx.class,
                                            BookTableEx::authors,
                                            (sq, association) -> {
                                                sq.where(
                                                        association
                                                                .target()
                                                                .firstName()
                                                                .eq("Alex")
                                                );
                                                return sq.select(
                                                        association.source().id()
                                                );
                                            }
                                    )
                            )
                    );
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID in (" +
                                    "select tb_2_.BOOK_ID " +
                                    "from BOOK_AUTHOR_MAPPING tb_2_ " +
                                    "inner join AUTHOR tb_3_ " +
                                    "on tb_2_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_3_.FIRST_NAME = ?" +
                                    ")"
                    );
                }
        );
    }
}
