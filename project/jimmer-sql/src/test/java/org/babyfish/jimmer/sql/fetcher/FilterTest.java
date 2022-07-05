package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

public class FilterTest extends AbstractQueryTest {

    @Test
    public void testByOneToMany() {
        executeAndExpect(
                getSqlClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .name()
                                            .books(
                                                    BookFetcher.$.name().edition(),
                                                    it -> it.filter(args -> {
                                                        BookTable book = args.getTable();
                                                        args.where(
                                                                Expression.tuple(book.name(), book.edition()).in(
                                                                        args.createSubQuery(BookTable.class, (sq, book2) -> {
                                                                            sq.where(book2.store().id().in(args.getKeys()));
                                                                            sq.groupBy(book2.name());
                                                                            return sq.select(book2.name(), book2.edition().max());
                                                                        })
                                                                )
                                                        );
                                                    })
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_1_.STORE_ID, " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?) " +
                                    "and (tb_1_.NAME, tb_1_.EDITION) in (" +
                                        "select tb_3_.NAME, max(tb_3_.EDITION) " +
                                        "from BOOK as tb_3_ " +
                                        "where tb_3_.STORE_ID in (?, ?) " +
                                        "group by tb_3_.NAME" +
                                    ")");
                    ctx.rows(System.out::println);
                }
        );
    }

    @Test
    public void testByInverseManyToMany() {
        executeAndExpect(
                getSqlClient().createQuery(AuthorTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    AuthorFetcher.$
                                            .firstName()
                                            .lastName()
                                            .books(
                                                    BookFetcher.$.name().edition(),
                                                    it -> it.filter(args -> {
                                                        BookTable book = args.getTable();
                                                        args.where(
                                                                Expression.tuple(book.name(), book.edition()).in(
                                                                        args.createSubQuery(BookTableEx.class, (sq, book2) -> {
                                                                            sq.where(book2.authors().id().in(args.getKeys()));
                                                                            sq.groupBy(book2.name());
                                                                            return sq.select(book2.name(), book2.edition().max());
                                                                        })
                                                                )
                                                        );
                                                    })
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME from AUTHOR as tb_1_");
                    ctx.statement(1).sql(
                            "select tb_2_.AUTHOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where " +
                                    "--->tb_2_.AUTHOR_ID in (?, ?, ?, ?, ?) " +
                                    "and " +
                                    "(tb_1_.NAME, tb_1_.EDITION) in (" +
                                    "--->select tb_4_.NAME, max(tb_4_.EDITION) " +
                                    "--->from BOOK as tb_4_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING as tb_5_ on tb_4_.ID = tb_5_.BOOK_ID " +
                                    "--->where tb_5_.AUTHOR_ID in (?, ?, ?, ?, ?) " +
                                    "--->group by tb_4_.NAME" +
                                    ")"
                    );
                    ctx.rows(System.out::println);
                }
        );
    }
}
