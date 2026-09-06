package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

public class VirtualPredicateTest extends AbstractQueryTest {

    @Test
    public void testAssociatedPredicateInSelectedSubQuery() {
        AuthorTable author = new AuthorTable();
        BookTableEx book = BookTableEx.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(author)
                        .orderBy(author.firstName())
                        .select(
                                author.firstName(),
                                getSqlClient()
                                        .createSubQuery(book)
                                        .where(book.authors(it -> it.id().eq(author.id())))
                                        .select(book.count())
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.FIRST_NAME, (" +
                                    "select count(tb_2_.ID) from BOOK tb_2_ " +
                                    "where exists(" +
                                    "select 1 from AUTHOR tb_3_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID " +
                                    "where tb_4_.BOOK_ID = tb_2_.ID and tb_3_.ID = tb_1_.ID" +
                                    ")) from AUTHOR tb_1_ order by tb_1_.FIRST_NAME asc"
                    );
                    ctx.rows("[" +
                            "{\"_1\":\"Alex\",\"_2\":3}," +
                            "{\"_1\":\"Boris\",\"_2\":3}," +
                            "{\"_1\":\"Dan\",\"_2\":3}," +
                            "{\"_1\":\"Eve\",\"_2\":3}," +
                            "{\"_1\":\"Samer\",\"_2\":3}" +
                            "]");
                }
        );
    }

    @Test
    public void testAssociatedPredicateInNestedSelectedSubQuery() {
        BookStoreTable store = BookStoreTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        BookTableEx book = BookTableEx.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(store)
                        .where(Expression.constant(3L).eq(
                                getSqlClient()
                                        .createSubQuery(author)
                                        .where(author.firstName().eq("Alex"))
                                        .select(
                                                getSqlClient()
                                                        .createSubQuery(book)
                                                        .where(book.authors(it -> it.firstName().eq("Alex")))
                                                        .select(book.count())
                                        )
                        ))
                        .orderBy(store.name())
                        .select(store.name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NAME from BOOK_STORE tb_1_ where 3 = (" +
                                    "select (select count(tb_3_.ID) from BOOK tb_3_ where exists(" +
                                    "select 1 from AUTHOR tb_4_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_4_.ID = tb_5_.AUTHOR_ID " +
                                    "where tb_5_.BOOK_ID = tb_3_.ID and tb_4_.FIRST_NAME = ?" +
                                    ")) from AUTHOR tb_2_ where tb_2_.FIRST_NAME = ?" +
                                    ") order by tb_1_.NAME asc"
                    );
                    ctx.variables("Alex", "Alex");
                    ctx.rows("[\"MANNING\",\"O'REILLY\"]");
                }
        );
    }

    @Test
    public void testEmptyAssociatedPredicateInSelectedSubQuery() {
        BookStoreTable store = BookStoreTable.$;
        BookTableEx book = BookTableEx.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(store)
                        .orderBy(store.name())
                        .select(
                                store.name(),
                                getSqlClient()
                                        .createSubQuery(book)
                                        .where(book.store().id().eq(store.id()))
                                        .where(book.authors(it -> it.firstName().eqIf(null)))
                                        .select(book.count())
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NAME, (select count(tb_2_.ID) from BOOK tb_2_ " +
                                    "where tb_2_.STORE_ID = tb_1_.ID) " +
                                    "from BOOK_STORE tb_1_ order by tb_1_.NAME asc"
                    );
                    ctx.rows("[{\"_1\":\"MANNING\",\"_2\":3},{\"_1\":\"O'REILLY\",\"_2\":9}]");
                }
        );
    }

    @Test
    public void testMergeAnd() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.authors(author -> author.firstName().eq("Alex")))
                        .where(table.authors(author -> author.gender().eq(Gender.MALE)))
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where exists(" +
                                    "--->select 1 " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where " +
                                    "--->--->tb_3_.BOOK_ID = tb_1_.ID " +
                                    "--->and " +
                                    "--->--->tb_2_.FIRST_NAME = ? " +
                                    "--->and " +
                                    "--->tb_2_.GENDER = ?" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testMergeOr() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                Predicate.or(
                                        table.authors(author -> author.firstName().ilike("a")),
                                        table.authors(author -> author.lastName().ilike("a"))
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where exists(" +
                                    "--->select 1 " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where " +
                                    "--->--->tb_3_.BOOK_ID = tb_1_.ID " +
                                    "--->and (" +
                                    "--->--->--->tb_2_.FIRST_NAME ilike ? " +
                                    "--->--->or " +
                                    "--->--->--->tb_2_.LAST_NAME ilike ?" +
                                    "--->)" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testMixed() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.authors(author -> author.gender().eq(Gender.MALE)))
                        .where(
                                Predicate.or(
                                        table.authors(author -> author.firstName().ilike("a")),
                                        table.authors(author -> author.lastName().ilike("a"))
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where exists(" +
                                    "--->select 1 " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_2_.ID = tb_4_.AUTHOR_ID " +
                                    "--->where " +
                                    "--->--->tb_4_.BOOK_ID = tb_1_.ID " +
                                    "--->and " +
                                    "--->--->tb_2_.GENDER = ?" +
                                    ") and (" +
                                    "--->exists(" +
                                    "--->--->select 1 " +
                                    "--->--->from AUTHOR tb_3_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_3_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->where " +
                                    "--->--->--->tb_5_.BOOK_ID = tb_1_.ID " +
                                    "--->--->and (" +
                                    "--->--->--->--->tb_3_.FIRST_NAME ilike ? " +
                                    "--->--->--->or " +
                                    "--->--->--->--->tb_3_.LAST_NAME ilike ?" +
                                    "--->--->)" +
                                    "--->)" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testDeep() {
        BookStoreTable table = BookStoreTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.books(book -> {
                                    return Predicate.and(
                                            book.name().ilike("GraphQL"),
                                            book.authors(author -> {
                                                return Predicate.or(
                                                        author.firstName().ilike("a"),
                                                        author.lastName().ilike("a")
                                                );
                                            })
                                    );
                                })
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where " +
                                    "--->exists(" +
                                    "--->--->select 1 from BOOK tb_2_ " +
                                    "--->--->where " +
                                    "--->--->--->tb_2_.STORE_ID = tb_1_.ID " +
                                    "--->--->and " +
                                    "--->--->--->tb_2_.NAME ilike ? " +
                                    "--->--->and exists(" +
                                    "--->--->--->select 1 " +
                                    "--->--->--->from AUTHOR tb_3_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_3_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->--->where " +
                                    "--->--->--->--->tb_5_.BOOK_ID = tb_2_.ID " +
                                    "--->--->--->and " +
                                    "--->--->--->--->(tb_3_.FIRST_NAME ilike ? or tb_3_.LAST_NAME ilike ?)" +
                                    "--->)" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testMixedDeep() {
        BookStoreTable table = BookStoreTable.$;
        BookTableEx book = BookTableEx.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                getSqlClient()
                                        .createSubQuery(book)
                                        .where(book.store().eq(table))
                                        .where(
                                                Predicate.and(
                                                        book.name().ilike("GraphQL"),
                                                        book.authors(author -> {
                                                            return Predicate.or(
                                                                    author.firstName().ilike("a"),
                                                                    author.lastName().ilike("a")
                                                            );
                                                        })
                                                )
                                        )
                                        .exists()
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where " +
                                    "--->exists(" +
                                    "--->--->select 1 from BOOK tb_2_ " +
                                    "--->--->where " +
                                    "--->--->--->tb_2_.STORE_ID = tb_1_.ID " +
                                    "--->--->and " +
                                    "--->--->--->tb_2_.NAME ilike ? " +
                                    "--->--->and exists(" +
                                    "--->--->--->select 1 " +
                                    "--->--->--->from AUTHOR tb_3_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_3_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->--->where " +
                                    "--->--->--->--->tb_5_.BOOK_ID = tb_2_.ID " +
                                    "--->--->--->and " +
                                    "--->--->--->--->(tb_3_.FIRST_NAME ilike ? or tb_3_.LAST_NAME ilike ?)" +
                                    "--->)" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testIgnoreEmpty() {
        BookStoreTable table = BookStoreTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.books(book -> book.name().eqIf(null))
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_"
                    );
                }
        );
    }

    @Test
    public void testTwo() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.authors(author ->
                                        Predicate.or(
                                                author.firstName().ilike("a"),
                                                author.lastName().ilike("a")
                                        )
                                )
                        )
                        .where(
                                table.authors(author -> author.gender().eq(Gender.MALE))
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ where exists(" +
                                    "--->select 1 " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ " +
                                    "--->--->on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where " +
                                    "--->--->tb_3_.BOOK_ID = tb_1_.ID " +
                                    "--->and (" +
                                    "--->--->tb_2_.FIRST_NAME ilike ? or " +
                                    "--->--->tb_2_.LAST_NAME ilike ?" +
                                    "--->) and tb_2_.GENDER = ?" +
                                    ")"
                    );
                }
        );
    }
}
