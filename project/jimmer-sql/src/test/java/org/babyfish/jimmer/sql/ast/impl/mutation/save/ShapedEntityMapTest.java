package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.junit.jupiter.api.Test;

public class ShapedEntityMapTest extends Tests {

    @Test
    public void test() {
        ShapedEntityMap<Book> bookMap = new ShapedEntityMap<>();
        bookMap.add(
                BookDraft.$.produce(book -> {
                    book.setId(Constants.graphQLInActionId1);
                    book.setName("GraphQL in Action");
                })
        );
        bookMap.add(
                BookDraft.$.produce(book -> {
                    book.setId(Constants.graphQLInActionId2);
                    book.setName("GraphQL in Action");
                })
        );
        bookMap.add(
                BookDraft.$.produce(book -> {
                    book.setName("Learning GraphQL");
                    book.setEdition(1);
                })
        );
        bookMap.add(
                BookDraft.$.produce(book -> {
                    book.setName("Learning GraphQL");
                    book.setEdition(2);
                })
        );
        assertContentEquals(
                "{" +
                        "--->[id, name]: [" +
                        "--->--->{\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\",\"name\":\"GraphQL in Action\"}, " +
                        "--->--->{\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\",\"name\":\"GraphQL in Action\"}" +
                        "--->], " +
                        "--->[name, edition]: [" +
                        "--->--->{\"name\":\"Learning GraphQL\",\"edition\":1}, " +
                        "--->--->{\"name\":\"Learning GraphQL\",\"edition\":2}" +
                        "--->]" +
                        "}",
                bookMap.toString()
        );
    }
}
