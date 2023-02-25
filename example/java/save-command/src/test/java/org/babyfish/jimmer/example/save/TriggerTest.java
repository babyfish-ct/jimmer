package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationWithTriggerTest;
import org.babyfish.jimmer.example.save.model.BookStoreDraft;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class TriggerTest extends AbstractMutationWithTriggerTest {

    @Test
    public void test() {

        jdbc("insert into book_store(name) values(?)", "MANNING");

        jdbc(
                "insert into book(name, edition, price, store_id) values" +
                        "(?, ?, ?, ?)," +
                        "(?, ?, ?, ?)",
                "Microservices Security in Action", 1, new BigDecimal("33.59"), 1L,
                "LINQ in Action", 1, new BigDecimal("21.59"), 1L
        );

        jdbc(
                "insert into author(first_name, last_name, gender) values" +
                        "(?, ?, ?), (?, ?, ?)," +
                        "(?, ?, ?), (?, ?, ?), (?, ?, ?)",

                "Prabath", "Siriwardena", "M",
                "Nuwan", "Dias", "M",

                "Fabrice", "Marguerie", "M",
                "Steve", "Eichert", "M",
                "Jim", "Wooley", "M"
        );

        jdbc(
                "insert into book_author_mapping(book_id, author_id) values" +
                        "(?, ?), (?, ?), " +
                        "(?, ?), (?, ?), (?, ?)",

                10L, 100L, 10L, 200L,
                20L, 300L, 20L, 400L, 20L, 500L
        );

        sql()
                .getEntities()
                .saveCommand(
                        BookStoreDraft.$.produce(turing -> {
                            turing.setName("TURING");
                            turing.addIntoBooks(security -> {
                                security.setName("Microservices Security in Action");
                                security.setEdition(1);
                                security.setPrice(new BigDecimal("43.59"));
                                security.addIntoAuthors(author -> author.setId(100L));
                                security.addIntoAuthors(author -> author.setId(200L));
                                security.addIntoAuthors(author -> author.setId(300L));
                            });
                            turing.addIntoBooks(linq -> {
                                linq.setName("LINQ in Action");
                                linq.setEdition(1);
                                linq.setPrice(new BigDecimal("31.59"));
                                linq.addIntoAuthors(author -> author.setId(400L));
                                linq.addIntoAuthors(author -> author.setId(500L));
                            });
                        })
                )
                .setAutoAttachingAll()
                .execute();
    }
}
