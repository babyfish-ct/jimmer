package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class ComplexExprTest extends AbstractQueryTest {

    @Test
    public void testSqlExpression() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book,
                            Expression.numeric().sql(
                                    Integer.class,
                                    "rank() over(order by %e desc)",
                                    book.price()
                            ),
                            Expression.numeric().sql(
                                    Integer.class,
                                    "rank() over(partition by %e order by %e desc)",
                                    new Expression[] { book.store().id(), book.price() }
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, " +
                                    "rank() over(order by tb_1_.PRICE desc), " +
                                    "rank() over(partition by tb_1_.STORE_ID order by tb_1_.PRICE desc) " +
                                    "from BOOK as tb_1_"
                    );
                    ctx.variables();
                    ctx.rows(list -> {
                        Assertions.assertEquals(12, list.size());
                        Tuple3<Book, Integer, Integer> rank1 = list
                                .stream()
                                .filter(it -> it.get_1().price().intValue() == 88)
                                .findFirst()
                                .get();
                        Tuple3<Book, Integer, Integer> rank12 = list
                                .stream()
                                .filter(it -> it.get_1().price().intValue() == 45)
                                .findFirst()
                                .get();
                        Assertions.assertEquals(1, rank1.get_2());
                        Assertions.assertEquals(1, rank1.get_3());
                        Assertions.assertEquals(12, rank12.get_2());
                        Assertions.assertEquals(9, rank12.get_3());
                    });
                }
        );
    }

    @Test
    public void testSqlPredicate() {
        executeAndExpect(
                getLambdaClient().createQuery(AuthorTable.class, (q, author) -> {
                    q.where(
                            Predicate.sql(
                                    "regexp_like(%e, %v)",
                                    it -> it
                                            .expression(author.firstName())
                                            .value("^Ste(v|ph)en$")
                            )
                    );
                    return q.select(author);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER from AUTHOR as tb_1_ where regexp_like(tb_1_.FIRST_NAME, ?)"
                    );
                    ctx.variables("^Ste(v|ph)en$");
                }
        );
    }

    @Test
    public void testTupleInList() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            Expression.tuple(book.name(), book.edition()).in(
                                    Arrays.asList(
                                        new Tuple2<>("Learning GraphQL", 3),
                                        new Tuple2<>("Effective TypeScript", 2)
                                    )
                            )
                    );
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"
                    );
                    ctx.variables("Learning GraphQL", 3, "Effective TypeScript", 2);
                }
        );
    }

    @Test
    public void testSimpleCase() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store,
                            Expression.string()
                                    .caseBuilder(store.name())
                                    .when("O'REILLY", "Classic publishing house")
                                    .when("MANNING", "Classic publishing house")
                                    .otherwise("Other publishing house")
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION, " +
                                    "case tb_1_.NAME " +
                                    "when ? then ? " +
                                    "when ? then ? " +
                                    "else ? " +
                                    "end " +
                                    "from BOOK_STORE as tb_1_"
                    );
                    ctx.variables(
                            "O'REILLY",
                            "Classic publishing house",
                            "MANNING",
                            "Classic publishing house",
                            "Other publishing house"
                    );
                }
        );
    }

    @Test
    public void testCase() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    return q.select(
                            book,
                            Expression.string().caseBuilder()
                                    .when(book.price().gt(new BigDecimal(200)), "Expensive")
                                    .when(book.price().lt(new BigDecimal(100)), "Cheap")
                                    .otherwise("Fitting")
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, " +
                                    "case " +
                                    "when tb_1_.PRICE > ? then ? " +
                                    "when tb_1_.PRICE < ? then ? " +
                                    "else ? " +
                                    "end " +
                                    "from BOOK as tb_1_"
                    );
                }
        );
    }
}
