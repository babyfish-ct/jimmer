package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookStoreProps;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class EntitySetTest extends Tests {

    @Test
    public void test() {
        EntitySet<Book> books = new EntitySet<>(
                new PropId[] {
                        BookStoreProps.ID.unwrap().getId()
                }
        );
        books.add(
                BookDraft.$.produce(draft -> {
                    draft.setId(Constants.graphQLInActionId1);
                    draft.setName("GraphQL in Action");
                })
        );
        books.add(
                BookDraft.$.produce(draft -> {
                    draft.setId(Constants.graphQLInActionId1);
                    draft.setPrice(new BigDecimal("49.99"));
                })
        );
        books.add(
                BookDraft.$.produce(draft -> {
                    draft.setId(Constants.effectiveTypeScriptId1);
                    draft.setName("Effective type script");
                })
        );
        books.add(
                BookDraft.$.produce(draft -> {
                    draft.setId(Constants.effectiveTypeScriptId1);
                    draft.setPrice(new BigDecimal("39.99"));
                })
        );
        assertContentEquals(
                "[{" +
                        "--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                        "--->\"name\":\"GraphQL in Action\"," +
                        "--->\"price\":49.99" +
                        "}, {" +
                        "--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                        "--->\"name\":\"Effective type script\"," +
                        "--->\"price\":39.99}" +
                        "]",
                books.toString()
        );
    }
}
