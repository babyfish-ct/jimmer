package org.babyfish.jimmer.sql.formatter;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.Gender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class FormatterTest extends AbstractFormatterTest {

    @Test
    public void testQuery() {

        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;

        sqlClient().createQuery(table)
                .where(table.name().like("G"))
                .where(table.store().name().eq("O'REILLY"))
                .where(
                        sqlClient().createSubQuery(author)
                                .where(author.books().id().eq(table.id()))
                                .where(
                                        Predicate.or(
                                                author.firstName().like("e"),
                                                author.lastName().ilike("e")
                                        )
                                )
                                .exists()
                )
                .select(table)
                .execute();

        assertSqlStatements(
                "select\n" +
                        "    tb_1_.ID,\n" +
                        "    tb_1_.NAME,\n" +
                        "    tb_1_.EDITION,\n" +
                        "    tb_1_.PRICE,\n" +
                        "    tb_1_.STORE_ID\n" +
                        "from BOOK tb_1_\n" +
                        "inner join BOOK_STORE tb_2_\n" +
                        "    on tb_1_.STORE_ID = tb_2_.ID\n" +
                        "where\n" +
                        "        tb_1_.NAME like ?\n" +
                        "    and\n" +
                        "        tb_2_.NAME = ?\n" +
                        "    and\n" +
                        "        exists (\n" +
                        "            select\n" +
                        "                1\n" +
                        "            from AUTHOR tb_3_\n" +
                        "            inner join BOOK_AUTHOR_MAPPING tb_4_\n" +
                        "                on tb_3_.ID = tb_4_.AUTHOR_ID\n" +
                        "            where\n" +
                        "                    tb_4_.BOOK_ID = tb_1_.ID\n" +
                        "                and\n" +
                        "                    (\n" +
                        "                        tb_3_.FIRST_NAME like ?\n" +
                        "                    or\n" +
                        "                        lower(tb_3_.LAST_NAME) like ?\n" +
                        "                    )\n" +
                        "        )"
        );
    }

    @Test
    public void testUpdate() {

        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;

        sqlClient().createUpdate(table)
                .where(table.name().like("G"))
                .where(
                        sqlClient().createSubQuery(author)
                                .where(author.books().id().eq(table.id()))
                                .where(
                                        Predicate.or(
                                                author.firstName().like("e"),
                                                author.lastName().ilike("e")
                                        )
                                )
                                .exists()
                )
                .set(table.price(), table.price().plus(BigDecimal.ONE))
                .execute();

        assertSqlStatements(
                "update BOOK tb_1_\n" +
                        "set\n" +
                        "    PRICE = tb_1_.PRICE + ?\n" +
                        "where\n" +
                        "        tb_1_.NAME like ?\n" +
                        "    and\n" +
                        "        exists (\n" +
                        "            select\n" +
                        "                1\n" +
                        "            from AUTHOR tb_2_\n" +
                        "            inner join BOOK_AUTHOR_MAPPING tb_3_\n" +
                        "                on tb_2_.ID = tb_3_.AUTHOR_ID\n" +
                        "            where\n" +
                        "                    tb_3_.BOOK_ID = tb_1_.ID\n" +
                        "                and\n" +
                        "                    (\n" +
                        "                        tb_2_.FIRST_NAME like ?\n" +
                        "                    or\n" +
                        "                        lower(tb_2_.LAST_NAME) like ?\n" +
                        "                    )\n" +
                        "        )"
        );
    }

    @Test
    public void testDelete() {

        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;

        sqlClient().createDelete(table)
                .where(table.name().like("G"))
                .where(
                        sqlClient().createSubQuery(author)
                                .where(author.books().id().eq(table.id()))
                                .where(
                                        Predicate.or(
                                                author.firstName().like("e"),
                                                author.lastName().ilike("e")
                                        )
                                )
                                .exists()
                )
                .execute();

        assertSqlStatements(
                "delete\n" +
                        "from BOOK tb_1_\n" +
                        "where\n" +
                        "        tb_1_.NAME like ?\n" +
                        "    and\n" +
                        "        exists (\n" +
                        "            select\n" +
                        "                1\n" +
                        "            from AUTHOR tb_2_\n" +
                        "            inner join BOOK_AUTHOR_MAPPING tb_3_\n" +
                        "                on tb_2_.ID = tb_3_.AUTHOR_ID\n" +
                        "            where\n" +
                        "                    tb_3_.BOOK_ID = tb_1_.ID\n" +
                        "                and\n" +
                        "                    (\n" +
                        "                        tb_2_.FIRST_NAME like ?\n" +
                        "                    or\n" +
                        "                        lower(tb_2_.LAST_NAME) like ?\n" +
                        "                    )\n" +
                        "        )"
        );
    }

    @Test
    public void testTuple() {

        BookTable table = BookTable.$;

        sqlClient().createQuery(table)
                .where(
                        Expression.tuple(
                                table.name(),
                                table.edition()
                        ).in(
                                Arrays.asList(
                                        new Tuple2<>("GraphQL in Action", 1),
                                        new Tuple2<>("GraphQL in Action", 2),
                                        new Tuple2<>("Learning GraphQL", 1),
                                        new Tuple2<>("Learning GraphQL", 2),
                                        new Tuple2<>("Effective TypeScript", 1),
                                        new Tuple2<>("Effective TypeScript", 2)
                                )
                        )
                )
                .select(table)
                .execute();

        assertSqlStatements(
                "select\n" +
                        "    tb_1_.ID,\n" +
                        "    tb_1_.NAME,\n" +
                        "    tb_1_.EDITION,\n" +
                        "    tb_1_.PRICE,\n" +
                        "    tb_1_.STORE_ID\n" +
                        "from BOOK tb_1_\n" +
                        "where\n" +
                        "    (tb_1_.NAME, tb_1_.EDITION) in (\n" +
                        "        (?, ?), (?, ?), (?, ?),\n" +
                        "        (?, ?), (?, ?), (?, ?)\n" +
                        "    )"
        );
    }

    @Test
    public void testSave() {
        sqlClient().getEntities().save(
                BookDraft.$.produce(book -> {
                    book.setName("Learning GraphQL").setPrice(new BigDecimal(49)).setEdition(3)
                            .addIntoAuthors(author -> {
                                author.setFirstName("Dan").setLastName("Vanderkam").setGender(Gender.FEMALE);
                            })
                            .addIntoAuthors(author -> {
                                author.setFirstName("Boris").setLastName("Cherny").setGender(Gender.FEMALE);
                            });
                })
        );

        assertSqlStatements(
                "select\n" +
                        "    tb_1_.ID,\n" +
                        "    tb_1_.NAME,\n" +
                        "    tb_1_.EDITION\n" +
                        "from BOOK tb_1_\n" +
                        "where\n" +
                        "        tb_1_.NAME = ?\n" +
                        "    and\n" +
                        "        tb_1_.EDITION = ?",

                        "update BOOK\n" +
                        "set\n" +
                        "    PRICE = ?\n" +
                        "where\n" +
                        "    ID = ?",

                        "select\n" +
                        "    tb_1_.ID,\n" +
                        "    tb_1_.FIRST_NAME,\n" +
                        "    tb_1_.LAST_NAME\n" +
                        "from AUTHOR tb_1_\n" +
                        "where\n" +
                        "        tb_1_.FIRST_NAME = ?\n" +
                        "    and\n" +
                        "        tb_1_.LAST_NAME = ?",

                        "update AUTHOR\n" +
                        "set\n" +
                        "    GENDER = ?\n" +
                        "where\n" +
                        "    ID = ?",

                        "select\n" +
                        "    tb_1_.ID,\n" +
                        "    tb_1_.FIRST_NAME,\n" +
                        "    tb_1_.LAST_NAME\n" +
                        "from AUTHOR tb_1_\n" +
                        "where\n" +
                        "        tb_1_.FIRST_NAME = ?\n" +
                        "    and\n" +
                        "        tb_1_.LAST_NAME = ?",

                        "update AUTHOR\n" +
                        "set\n" +
                        "    GENDER = ?\n" +
                        "where\n" +
                        "    ID = ?",

                        "select\n" +
                        "    AUTHOR_ID\n" +
                        "from BOOK_AUTHOR_MAPPING\n" +
                        "where\n" +
                        "    BOOK_ID = ?",
                        
                        "delete from BOOK_AUTHOR_MAPPING\n" +
                        "where\n" +
                        "    (BOOK_ID, AUTHOR_ID) in (\n" +
                        "        (?, ?), (?, ?)\n" +
                        "    )",

                        "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID)\n" +
                        "values\n" +
                        "    (?, ?),\n" +
                        "    (?, ?)"
        );
    }
}
