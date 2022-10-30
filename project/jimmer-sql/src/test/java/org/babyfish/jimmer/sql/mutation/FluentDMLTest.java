//package org.babyfish.jimmer.sql.mutation;
//
//import org.babyfish.jimmer.sql.common.AbstractMutationTest;
//import org.babyfish.jimmer.sql.model.AuthorTableEx;
//import org.babyfish.jimmer.sql.model.BookTable;
//import org.junit.jupiter.api.Test;
//
//import java.math.BigDecimal;
//
//public class FluentDMLTest extends AbstractMutationTest {
//
//    @Test
//    public void testUpdate() {
//        Fluent fluent = getSqlClient().createFluent();
//        BookTable book = new BookTable();
//        AuthorTableEx author = new AuthorTableEx();
//        executeAndExpectRowCount(
//                fluent
//                        .update(book)
//                        .set(book.price(), book.price().plus(BigDecimal.ONE))
//                        .where(
//                                book.id().in(
//                                        fluent
//                                                .subQuery(author)
//                                                .where(author.firstName().eq("Alex"))
//                                                .select(author.books().id())
//                                )
//                        ),
//                ctx -> {
//                    ctx.statement(it -> {
//                        it.sql(
//                                "update BOOK tb_1_ " +
//                                        "set PRICE = tb_1_.PRICE + ? " +
//                                        "where tb_1_.ID in (" +
//                                        "--->select tb_3_.BOOK_ID " +
//                                        "--->from AUTHOR as tb_2_ " +
//                                        "--->inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
//                                        "--->where tb_2_.FIRST_NAME = ?" +
//                                        ")");
//                    });
//                    ctx.rowCount(3);
//                }
//        );
//    }
//
//    @Test
//    public void testDelete() {
//        Fluent fluent = getSqlClient().createFluent();
//        BookTable book = new BookTable();
//        AuthorTableEx author = new AuthorTableEx();
//
//        executeAndExpectRowCount(
//                fluent
//                        .delete(book)
//                        .where(
//                                book.id().in(
//                                        fluent
//                                                .subQuery(author)
//                                                .where(author.firstName().eq("Alex"))
//                                                .select(author.books().id())
//                                )
//                        ),
//                ctx -> {
//                    ctx.statement(it -> {
//                        it.sql(
//                                "delete from BOOK as tb_1_ " +
//                                        "where tb_1_.ID in (" +
//                                        "--->select tb_3_.BOOK_ID " +
//                                        "--->from AUTHOR as tb_2_ " +
//                                        "--->inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
//                                        "--->where tb_2_.FIRST_NAME = ?" +
//                                        ")"
//                        );
//                    });
//                    ctx.rowCount(3);
//                }
//        );
//    }
//}
