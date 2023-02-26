package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationWithTriggerTest;
import org.babyfish.jimmer.example.save.model.BookStoreDraft;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Recommended learning sequence: 7
 *
 * <p>SaveModeTest -> IncompleteObjectTest -> ManyToOneTest ->
 * OneToManyTest -> ManyToManyTest -> RecursiveTest -> [Current: TriggerTest]</p>
 */
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
                /*
                 * If you use jimmer-spring-starter, it is unecessary to
                 * do it because this switch is turned on.
                 */
                .setAutoAttachingAll()
                .execute();
        
        /*
         * This example focuses on triggers, so we don't assert SQL statements,
         * but directly assert events
         */
        assertEvents(

                "The entity \"org.babyfish.jimmer.example.save.model.BookStore\" is changed, " +
                        "old: null, " +
                        "new: {\"id\":2,\"name\":\"TURING\"}",

                "The entity \"org.babyfish.jimmer.example.save.model.Book\" is changed, " +
                        "old: {\"id\":10,\"name\":\"Microservices Security in Action\",\"edition\":1,\"price\":33.59,\"store\":{\"id\":1}}, " +
                        "new: {\"id\":10,\"name\":\"Microservices Security in Action\",\"edition\":1,\"price\":43.59,\"store\":{\"id\":2}}",

                "The association \"org.babyfish.jimmer.example.save.model.Book.store\" is changed, " +
                        "source id: 10, " +
                        "detached target id: 1, " +
                        "attached target id: 2",

                "The association \"org.babyfish.jimmer.example.save.model.BookStore.books\" is changed, " +
                        "source id: 1, " +
                        "detached target id: 10, " +
                        "attached target id: null",

                "The association \"org.babyfish.jimmer.example.save.model.BookStore.books\" is changed, " +
                        "source id: 2, " +
                        "detached target id: null, " +
                        "attached target id: 10",

                "The association \"org.babyfish.jimmer.example.save.model.Book.authors\" is changed, " +
                        "source id: 10, " +
                        "detached target id: null, " +
                        "attached target id: 300",

                "The association \"org.babyfish.jimmer.example.save.model.Author.books\" is changed, " +
                        "source id: 300, " +
                        "detached target id: null, " +
                        "attached target id: 10",

                "The entity \"org.babyfish.jimmer.example.save.model.Book\" is changed, " +
                        "old: {\"id\":20,\"name\":\"LINQ in Action\",\"edition\":1,\"price\":21.59,\"store\":{\"id\":1}}, " +
                        "new: {\"id\":20,\"name\":\"LINQ in Action\",\"edition\":1,\"price\":31.59,\"store\":{\"id\":2}}",

                "The association \"org.babyfish.jimmer.example.save.model.Book.store\" is changed, " +
                        "source id: 20, " +
                        "detached target id: 1, " +
                        "attached target id: 2",

                "The association \"org.babyfish.jimmer.example.save.model.BookStore.books\" is changed, " +
                        "source id: 1, " +
                        "detached target id: 20, " +
                        "attached target id: null",

                "The association \"org.babyfish.jimmer.example.save.model.BookStore.books\" is changed, " +
                        "source id: 2, " +
                        "detached target id: null, " +
                        "attached target id: 20",

                "The association \"org.babyfish.jimmer.example.save.model.Book.authors\" is changed, " +
                        "source id: 20, " +
                        "detached target id: 300, " +
                        "attached target id: null",

                "The association \"org.babyfish.jimmer.example.save.model.Author.books\" is changed, " +
                        "source id: 300, " +
                        "detached target id: 20, " +
                        "attached target id: null"
        );
    }
}
