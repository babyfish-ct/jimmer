package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

public class VirtualPredicateTest extends AbstractQueryTest {

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
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where " +
                                    "--->--->tb_3_.BOOK_ID = tb_1_.ID " +
                                    "--->and " +
                                    "--->--->tb_2_.GENDER = ?" +
                                    ") and (" +
                                    "--->exists(" +
                                    "--->--->select 1 " +
                                    "--->--->from AUTHOR tb_5_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_6_ on tb_5_.ID = tb_6_.AUTHOR_ID " +
                                    "--->--->where " +
                                    "--->--->--->tb_6_.BOOK_ID = tb_1_.ID " +
                                    "--->--->and (" +
                                    "--->--->--->--->tb_5_.FIRST_NAME ilike ? " +
                                    "--->--->--->or " +
                                    "--->--->--->--->tb_5_.LAST_NAME ilike ?" +
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
                                    "--->--->--->from AUTHOR tb_4_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_4_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->--->where " +
                                    "--->--->--->--->tb_5_.BOOK_ID = tb_2_.ID " +
                                    "--->--->--->and " +
                                    "--->--->--->--->(tb_4_.FIRST_NAME ilike ? or tb_4_.LAST_NAME ilike ?)" +
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
                                    "--->--->--->from AUTHOR tb_4_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_4_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->--->where " +
                                    "--->--->--->--->tb_5_.BOOK_ID = tb_2_.ID " +
                                    "--->--->--->and " +
                                    "--->--->--->--->(tb_4_.FIRST_NAME ilike ? or tb_4_.LAST_NAME ilike ?)" +
                                    "--->)" +
                                    ")"
                    );
                }
        );
    }
}
