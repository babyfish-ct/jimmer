package org.babyfish.jimmer;

import org.babyfish.jimmer.evaluation.Evaluators;
import org.babyfish.jimmer.model.Book;
import org.babyfish.jimmer.model.BookDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EvaluatorTest {

    @Test
    public void test() {
        Book book = BookDraft.$.produce(draft -> {
            draft
                    .setName("Book")
                    .setPrice(27)
                    .setStore(store -> {
                        store
                                .setName("manning")
                                .setWebsite("http://www.manning.com")
                                .setAvgPrice(new BigDecimal("33.3333"));
                    })
                    .addIntoAuthors(author -> {
                        author
                                .setName("Alex")
                                .setEmail("alex@gmail.com");
                    })
                    .addIntoAuthors(author -> {
                        author
                                .setName("Linda")
                                .setEmail("linda@gmail.com");
                    });
        });

        List<String> values = Evaluators.evaluate(
                book,
                new ArrayList<>(),
                (base, ctx) -> {
                    base.add(
                            "`" +
                                    ctx +
                                    (ctx.isLoaded() ?
                                            "` is loaded" :
                                            "` is not loaded"
                                    )
                    );
                    return base;
                }
        );

        Assertions.assertEquals(
                Arrays.asList(
                        "`<root>.name` is loaded",
                        "`<root>.store` is loaded",
                        "`<root>.store.name` is loaded",
                        "`<root>.store.website` is loaded",
                        "`<root>.store.books` is not loaded",
                        "`<root>.store.avgPrice` is loaded",
                        "`<root>.price` is loaded",
                        "`<root>.authors` is loaded",
                        "`<root>.authors[0].name` is loaded",
                        "`<root>.authors[0].books` is not loaded",
                        "`<root>.authors[0].email` is loaded",
                        "`<root>.authors[1].name` is loaded",
                        "`<root>.authors[1].books` is not loaded",
                        "`<root>.authors[1].email` is loaded"
                ),
                values
        );
    }
}
