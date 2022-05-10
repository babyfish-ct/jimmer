package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class ComplexExprTest extends AbstractQueryTest {

    @Test
    public void testSqlExpression() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    return q.select(
                            book,
                            Expression.numeric().sql(
                                    Integer.class,
                                    "rank() over(order by %e desc)",
                                    it -> it.expression(book.price())
                            ),
                            Expression.numeric().sql(
                                    Integer.class,
                                    "rank() over(partition by %e order by %e desc)",
                                    it -> it
                                            .expression(book.store().id())
                                            .expression(book.price())
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID, " +
                                    "rank() over(order by tb_1.PRICE desc), " +
                                    "rank() over(partition by tb_1.STORE_ID order by tb_1.PRICE desc) " +
                                    "from BOOK as tb_1"
                    );
                    ctx.variables();
                    ctx.rows(list -> {
                        Assertions.assertEquals(12, list.size());
                        Tuple3<Book, Integer, Integer> rank1 = list
                                .stream()
                                .filter(it -> it._1().price().intValue() == 88)
                                .findFirst()
                                .get();
                        Tuple3<Book, Integer, Integer> rank12 = list
                                .stream()
                                .filter(it -> it._1().price().intValue() == 45)
                                .findFirst()
                                .get();
                        Assertions.assertEquals(1, rank1._2());
                        Assertions.assertEquals(1, rank1._3());
                        Assertions.assertEquals(12, rank12._2());
                        Assertions.assertEquals(9, rank12._3());
                    });
                }
        );
    }

    @Test
    public void testTupleInList() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
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
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 " +
                                    "where (tb_1.NAME, tb_1.EDITION) in ((?, ?), (?, ?))"
                    );
                    ctx.variables("Learning GraphQL", 3, "Effective TypeScript", 2);
                }
        );
    }

    @Test
    public void testSimpleCase() {
        executeAndExpect(
                BookStoreTable.createQuery(getSqlClient(), (q, store) -> {
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
                            "select tb_1.ID, tb_1.NAME, tb_1.WEBSITE, " +
                                    "case tb_1.NAME " +
                                    "when ? then ? " +
                                    "when ? then ? " +
                                    "else ? " +
                                    "end " +
                                    "from BOOK_STORE as tb_1"
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
                BookTable.createQuery(getSqlClient(), (q, book) -> {
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
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID, " +
                                    "case " +
                                    "when tb_1.PRICE > ? then ? " +
                                    "when tb_1.PRICE < ? then ? " +
                                    "else ? " +
                                    "end " +
                                    "from BOOK as tb_1"
                    );
                }
        );
        /*
        select {
                table then
                case()
                    .match(table.price gt BigDecimal(200), "Expensive")
                    .match(table.price lt BigDecimal(100), "Cheap")
                    .otherwise("Fitting")
            }
         */
    }
}
