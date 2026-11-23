package org.babyfish.jimmer;

import org.babyfish.jimmer.model.BookDraft;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

public class DraftToStringTest {

    @JacksonTest
    public void test(JacksonBackend backend) {
        StringBuilder builder = new StringBuilder();
        BookDraft.$.produce(draft -> {
            draft.setName("SQL in Action");
            draft.applyStore(store -> {
                store.setName("MANNING");
            });
            draft.addIntoAuthors(author -> {
                author.setName("James");
            });
            draft.addIntoAuthors(author -> {
                author.setName("Cramer");
            });
            builder.append(draft);
            Assertions.assertEquals(builder.toString(), backend.write(draft));
        });
        Assertions.assertEquals(
                "{" +
                        "\"name\":\"SQL in Action\"," +
                        "\"store\":{\"name\":\"MANNING\"}," +
                        "\"authors\":[" +
                        "{\"name\":\"James\"}," +
                        "{\"name\":\"Cramer\"}" +
                        "]}",
                builder.toString()
        );
    }
}
